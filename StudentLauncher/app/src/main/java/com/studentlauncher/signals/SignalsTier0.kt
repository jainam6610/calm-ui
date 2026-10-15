package com.studentlauncher.signals

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat

/** Which way the phone is lying when a call arrives. */
enum class Flip { FaceUp, FaceDown, Unknown }

/**
 * Pure orientation decision from a single accelerometer sample. Screen up reads about +9.8 on z,
 * screen down about -9.8; anything in between is treated as unknown (in a pocket, in hand).
 */
fun flipFrom(z: Float, threshold: Float = 7f): Flip = when {
    z >= threshold -> Flip.FaceUp
    z <= -threshold -> Flip.FaceDown
    else -> Flip.Unknown
}

/**
 * Tier 0 signals: calls only, and only from [AudioManager]'s audio mode.
 *
 * This deliberately reads no caller information — no phone number, no contact lookup, no
 * TelephonyManager and no READ_PHONE_STATE — and keeps its counters in memory only.
 * The accelerometer is sampled once per event and then released; there is no continuous sensor.
 */
class SignalsMonitor(
    private val ctx: Context,
    private val onCall: (Flip) -> Unit
) {
    private val audio = ctx.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val sensors = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accel = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mainExecutor = ContextCompat.getMainExecutor(ctx)
    private val handler = Handler(Looper.getMainLooper())

    private var registered = false
    private var lastMode = AudioManager.MODE_NORMAL

    /** How many call transitions we saw. In memory only; never persisted. */
    var callEvents = 0
        private set

    /** True when this device can report audio-mode changes (API 31+). */
    val available: Boolean get() = Build.VERSION.SDK_INT >= 31

    private var modeListener: Any? = null

    fun start() {
        if (registered || !available) return
        startApi31()
        registered = true
    }

    fun stop() {
        if (!registered) return
        if (Build.VERSION.SDK_INT >= 31) stopApi31()
        registered = false
    }

    @RequiresApi(31)
    private fun startApi31() {
        val l = AudioManager.OnModeChangedListener { mode -> handleMode(mode) }
        modeListener = l
        audio?.addOnModeChangedListener(mainExecutor, l)
        lastMode = runCatching { audio?.mode ?: AudioManager.MODE_NORMAL }.getOrDefault(AudioManager.MODE_NORMAL)
    }

    @RequiresApi(31)
    private fun stopApi31() {
        (modeListener as? AudioManager.OnModeChangedListener)?.let {
            runCatching { audio?.removeOnModeChangedListener(it) }
        }
        modeListener = null
    }

    private fun isCallMode(mode: Int) = mode == AudioManager.MODE_RINGTONE ||
        mode == AudioManager.MODE_IN_CALL ||
        mode == AudioManager.MODE_IN_COMMUNICATION

    private fun handleMode(mode: Int) {
        val wasCall = isCallMode(lastMode)
        lastMode = mode
        if (isCallMode(mode) && !wasCall) onCallEvent()
    }

    private fun onCallEvent() {
        callEvents++
        val sensor = accel
        if (sensor == null) {
            onCall(Flip.Unknown)
            return
        }
        val listener = object : SensorEventListener {
            private var done = false
            override fun onSensorChanged(event: SensorEvent) {
                if (done) return
                done = true
                sensors?.unregisterListener(this)
                onCall(flipFrom(event.values.getOrElse(2) { 0f }))
            }

            override fun onAccuracyChanged(s: Sensor?, accuracy: Int) = Unit
        }
        // Safety timeout: never leave a sensor listener registered if no sample arrives.
        handler.postDelayed({ runCatching { sensors?.unregisterListener(listener) } }, 800L)
        runCatching { sensors?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_FASTEST) }
            .onFailure { onCall(Flip.Unknown) }
    }
}
