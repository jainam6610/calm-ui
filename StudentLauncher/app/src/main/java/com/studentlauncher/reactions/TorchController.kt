package com.studentlauncher.reactions

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Owns the camera torch for reactive feedback.
 *
 * Uses [CameraManager.setTorchMode], which needs no runtime permission and no manifest entry,
 * so this adds nothing to the app's permission surface. Every play is gated by the pure rules
 * in [TorchPolicy] and is additionally bounded by a watchdog, so the light can never stay on
 * even if the coroutine driving it is cancelled midway.
 */
class TorchController(private val ctx: Context, private val scope: CoroutineScope) {

    private val cameraManager = ctx.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val torchId: String? = runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id ->
            cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        }
    }.getOrNull()

    /** False when the device has no flash unit or the camera service rejected us. */
    var available: Boolean = torchId != null
        private set

    /** True when the device has a flash unit, even if the camera service is busy right now. */
    val hasFlash: Boolean get() = torchId != null

    /** Human-readable state for the settings screens. */
    val status: String get() = when {
        torchId == null -> "No torch found"
        available -> "On"
        else -> "Camera busy"
    }

    private val recent = ArrayDeque<Long>()
    private val hour = ArrayDeque<Long>()
    private var lastSessionEnd = 0L
    private var sessionOnMs = 0L
    private var job: Job? = null
    private var watchdog: Job? = null

    /** When a transient failure may be retried; 0 means the failure was permanent. */
    private var retryAt = 0L

    private fun setTorch(on: Boolean) {
        val id = torchId ?: return
        runCatching { cameraManager?.setTorchMode(id, on) }.onFailure { e ->
            // Only an invalid camera id means the flash is really gone. A camera that is
            // busy (video call, another torch app) or blocked by policy is temporary, so
            // reactions must not stay off until the process restarts.
            available = false
            retryAt = if (e is IllegalArgumentException) 0L
            else SystemClock.elapsedRealtime() + TorchLimits.RETRY_AFTER_MS
        }
    }

    /** Forces the light off and cancels anything in flight. Safe to call from any lifecycle event. */
    fun allOff() {
        watchdog?.cancel()
        watchdog = null
        job?.cancel()
        job = null
        setTorch(false)
        lastSessionEnd = SystemClock.elapsedRealtime()
    }

    /** Forces the light off and clears the per-session budget (app returned to the foreground). */
    fun beginSession() {
        sessionOnMs = 0L
        lastSessionEnd = 0L
        // A camera that was busy last time may be free now; re-probe instead of staying
        // "No torch found" for the life of the process.
        if (torchId != null) available = true
        watchdog?.cancel()
        watchdog = null
        job?.cancel()
        job = null
        setTorch(false)
    }

    /** Attempts [pattern]; returns null when it started, or a reason when it was blocked. */
    fun flash(pattern: List<Flash>, conditions: TorchConditions): String? {
        val now = SystemClock.elapsedRealtime()
        if (!available) {
            if (torchId == null) return "No torch on this device"
            if (retryAt == 0L || now < retryAt) return "The camera is busy"
            available = true
        }
        prune(now)
        torchBlockReason(conditions, now, recent.toList(), lastSessionEnd, hour.size, sessionOnMs)
            ?.let { return it }
        patternBlockReason(pattern, now, recent.toList(), hour.size, sessionOnMs)
            ?.let { return it }

        job?.cancel()
        job = scope.launch {
            val start = SystemClock.elapsedRealtime()
            for (f in pattern) {
                if (SystemClock.elapsedRealtime() - start >= TorchLimits.MAX_TOTAL_ON_MS) break
                val t = SystemClock.elapsedRealtime()
                recent.addLast(t)
                hour.addLast(t)
                sessionOnMs += f.onMs
                setTorch(true)
                delay(f.onMs)
                setTorch(false)
                if (f.gapMs > 0) delay(f.gapMs)
            }
            setTorch(false)
            lastSessionEnd = SystemClock.elapsedRealtime()
        }
        // Watchdog: whatever happens to the coroutine, force the light off.
        watchdog?.cancel()
        watchdog = scope.launch {
            delay(TorchLimits.WATCHDOG_MS + 250)
            job?.cancel()
            job = null
            setTorch(false)
            lastSessionEnd = SystemClock.elapsedRealtime()
        }
        return null
    }

    private fun prune(now: Long) {
        while (recent.isNotEmpty() && now - recent.first() >= TorchLimits.WINDOW_MS) recent.removeFirst()
        while (hour.isNotEmpty() && now - hour.first() >= TorchLimits.HOUR_MS) hour.removeFirst()
        if (now - lastSessionEnd > TorchLimits.COOL_DOWN_MS) sessionOnMs = 0L
    }
}
