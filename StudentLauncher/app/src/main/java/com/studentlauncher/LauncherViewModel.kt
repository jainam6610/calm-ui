package com.studentlauncher

import android.app.ActivityOptions
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studentlauncher.data.*
import com.studentlauncher.detox.DetoxPolicy
import com.studentlauncher.detox.DetoxStore
import com.studentlauncher.mindful.MindfulEngine
import com.studentlauncher.mindful.MindfulHost
import com.studentlauncher.mindful.MindfulStore
import com.studentlauncher.mindful.TaskRef
import com.studentlauncher.reactions.Flash
import com.studentlauncher.reactions.TorchConditions
import com.studentlauncher.reactions.TorchController
import com.studentlauncher.reactions.TorchPatterns
import com.studentlauncher.security.PinLock
import com.studentlauncher.signals.Flip
import com.studentlauncher.signals.NotificationMeta
import com.studentlauncher.signals.SignalStore
import com.studentlauncher.signals.SignalsMonitor
import com.studentlauncher.signals.notificationAccessGranted
import com.studentlauncher.signals.shouldReact
import com.studentlauncher.ui.GlyphModes
import com.studentlauncher.ui.PinMode
import com.studentlauncher.ui.PinRequest
import com.studentlauncher.ui.WpAdjust
import com.studentlauncher.ui.Wallpapers
import com.studentlauncher.ui.encodeTasks
import com.studentlauncher.ui.parseTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    private val repo = AppRepository(ctx)
    private val prefs = ctx.getSharedPreferences("student_launcher", Context.MODE_PRIVATE)
    val hostMgr = WidgetHostManager(ctx)

    // ---- data ----
    var apps by mutableStateOf<List<AppInfo>>(emptyList()); private set
    private var byPkg by mutableStateOf<Map<String, AppInfo>>(emptyMap())
    val icons = mutableStateMapOf<String, AppIcons>()

    // ---- settings (persisted) ----
    var themeMode by mutableStateOf(enumPref("theme", ThemeMode.Light)); private set
    var wallpaper by mutableStateOf(prefs.getString("wallpaper", "dune") ?: "dune"); private set
    var wallpaperVersion by mutableStateOf(0); private set
    var wpBlur by mutableStateOf(prefs.getFloat("wpBlur", 0f)); private set
    var wpDim by mutableStateOf(prefs.getFloat("wpDim", 0f)); private set
    var wpZoom by mutableStateOf(prefs.getFloat("wpZoom", 1f)); private set
    var wpPanX by mutableStateOf(prefs.getFloat("wpPanX", 0f)); private set
    var wpPanY by mutableStateOf(prefs.getFloat("wpPanY", 0f)); private set
    var iconPack by mutableStateOf(loadIconPack()); private set
    var iconShape by mutableStateOf(enumPref("iconShape", IconShape.Squircle)); private set
    var layout by mutableStateOf(enumPref("layout", HomeLayout.List)); private set
    var alphaSide by mutableStateOf(enumPref("alphaSide", AlphaSide.Right)); private set
    var normalScroll by mutableStateOf(prefs.getBoolean("normalScroll", false)); private set
    var dockStyle by mutableStateOf(enumPref("dockStyle", DockStyle.Bottom)); private set
    var showNames by mutableStateOf(prefs.getBoolean("showNames", true)); private set
    var studyMode by mutableStateOf(prefs.getBoolean("studyMode", false)); private set
    var pauseSec by mutableStateOf(prefs.getInt("pauseSec", 5)); private set
    var reminderMin by mutableStateOf(prefs.getInt("reminderMin", 10)); private set
    var motionIntensity by mutableStateOf(prefs.getFloat("motionIntensity", 1f)); private set
    var compactDensity by mutableStateOf(prefs.getBoolean("compactDensity", false)); private set
    var accentId by mutableStateOf(prefs.getString("accentId", "red") ?: "red"); private set
    var wpAccent by mutableStateOf(prefs.getBoolean("wpAccent", false)); private set
    var uiFont by mutableStateOf(prefs.getString("uiFont", "system") ?: "system"); private set
    /** One global clock style for every clock widget (no per-widget override). */
    var clockStyle by mutableStateOf(prefs.getString("clockStyle", "Dot") ?: "Dot"); private set

    // ---- torch reactions: opt-in, off by default, rate limited in code ----
    val torch = TorchController(ctx, viewModelScope)
    var torchEnabled by mutableStateOf(prefs.getBoolean("torchEnabled", false)); private set
    var torchNotice by mutableStateOf<String?>(null); private set

    fun applyTorchEnabled(v: Boolean) {
        torchEnabled = v
        putB("torchEnabled", v)
        if (!v) {
            torch.allOff()
            torchNotice = null
        }
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) = applyBattery(i)
    }
    private var batteryRegistered = false

    /**
     * Called when the activity starts: the light is never left on across a resume, and battery
     * state starts tracking through the sticky broadcast (an event, not a poll). A Detoxify
     * session that ran out, or time that passed while backgrounded, is caught up once here.
     */
    fun onActivityStart() {
        torch.beginSession()
        if (!batteryRegistered) {
            batteryRegistered = true
            runCatching {
                applyBattery(ctx.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
            }
        }
        detoxHeartbeat()
        ensureDetoxLoop()
    }

    fun onActivityStop() {
        torchOff()
        // Freeze the anchors so the session resumes exactly where it was, however long it was away.
        detox?.let {
            detoxStore.save(DetoxPolicy.snapshot(it, SystemClock.elapsedRealtime(), System.currentTimeMillis()))
        }
        detoxJob?.cancel()
        detoxJob = null
        if (batteryRegistered) {
            batteryRegistered = false
            runCatching { ctx.unregisterReceiver(batteryReceiver) }
        }
    }

    private fun applyBattery(i: Intent?) {
        if (i == null) return
        val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (l >= 0 && s > 0) batteryPct = l * 100 / s
        val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        batteryCharging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        recomputeMatrixState()
    }

    fun torchOff() = torch.allOff()

    fun testTorch() {
        val reason = torch.flash(TorchPatterns.TAP, torchConditions())
        torchNotice = reason ?: "Test flash sent"
        if (reason == null) {
            viewModelScope.launch {
                delay(3000)
                if (torchNotice == "Test flash sent") torchNotice = null
            }
        }
    }

    private fun torchConditions(): TorchConditions = TorchConditions(
        optedIn = torchEnabled,
        reducedMotion = runCatching {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f) <= 0f,
        powerSave = runCatching {
            (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isPowerSaveMode
        }.getOrDefault(false),
        thermalOk = runCatching {
            if (Build.VERSION.SDK_INT >= 29) {
                (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).currentThermalStatus < PowerManager.THERMAL_STATUS_MODERATE
            } else true
        }.getOrDefault(true),
        batteryPct = batteryInfo().first,
        charging = batteryInfo().second,
        quietNow = quietNow()
    )

    /** True while Do Not Disturb or a quiet profile is suppressing interruptions. */
    private fun quietNow(): Boolean = runCatching {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
    }.getOrDefault(false)

    private fun batteryInfo(): Pair<Int, Boolean> = runCatching {
        val i = ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        var pct = 100
        var charging = false
        if (i != null) {
            val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && s > 0) pct = l * 100 / s
            val st = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            charging = st == BatteryManager.BATTERY_STATUS_CHARGING || st == BatteryManager.BATTERY_STATUS_FULL
        }
        pct to charging
    }.getOrDefault(100 to false)

    // ---- live glyph matrix state ----
    /** Battery, kept live by a sticky BATTERY_CHANGED receiver while the activity is started. */
    var batteryPct by mutableIntStateOf(100); private set
    var batteryCharging by mutableStateOf(false); private set

    /** What the glyph matrix shows right now; the widget renders this rather than a fixed mode. */
    var matrixState by mutableStateOf(GlyphModes.CLOCK); private set
    var matrixTick by mutableIntStateOf(0); private set

    private var reactionMode: String? = null
    private var lastNotifReactAt = 0L

    /**
     * The ambient state the matrix falls back to when nothing is happening:
     * charging > low battery > clock. All three render statically, so an idle matrix costs
     * no frames at all.
     */
    private fun sustainedState(): String = when {
        batteryCharging -> GlyphModes.CHARGING
        batteryPct <= 15 -> GlyphModes.LOW
        else -> GlyphModes.CLOCK
    }

    private fun pushMatrixState(next: String) {
        if (next != matrixState) {
            matrixState = next
            matrixTick++
        }
    }

    private fun recomputeMatrixState() = pushMatrixState(reactionMode ?: sustainedState())

    /**
     * Plays a matrix reaction and, when opted in, the matching torch pattern. Only non-touch app
     * events call this; touch, breathing and low-battery states never reach the torch. After the
     * burst the matrix settles back into the live state (charging, low battery or clock).
     */
    fun fireReaction(mode: String, torchPattern: List<Flash>? = null) {
        reactionMode = mode
        // Always bump the tick: two reactions of the same kind in a row must still replay.
        matrixState = mode
        matrixTick++
        if (torchPattern != null && torchEnabled) {
            torchNotice = torch.flash(torchPattern, torchConditions())
        }
        val tick = matrixTick
        viewModelScope.launch {
            delay(2200)
            if (matrixTick == tick) {
                reactionMode = null
                recomputeMatrixState()
            }
        }
    }

    /** A finished focus session: matrix check plus an optional two-flash torch. */
    fun reactionDone() = fireReaction(GlyphModes.DONE, TorchPatterns.DONE)

    /** A priority / important signal: three-flash torch pattern at 2.5 Hz. */
    fun reactionAlert() = fireReaction(GlyphModes.PULSE, TorchPatterns.ALERT)

    // ---- Tier 0 signals: calls only, from AudioManager; no caller identity, no permissions ----
    var signalsEnabled by mutableStateOf(prefs.getBoolean("signalsEnabled", false)); private set
    private val signals = SignalsMonitor(ctx) { flip -> onCallFlip(flip) }

    /** True when the platform can report audio-mode changes (API 31+). */
    val signalsAvailable: Boolean get() = signals.available

    fun applySignalsEnabled(v: Boolean) {
        signalsEnabled = v
        putB("signalsEnabled", v)
        if (v) signals.start() else signals.stop()
    }

    private val onNotification: (NotificationMeta) -> Unit = { meta ->
        viewModelScope.launch { handleNotification(meta) }
    }

    fun startSignals() {
        if (signalsEnabled) signals.start()
        if (notifSignalsEnabled) {
            SignalStore.addListener(onNotification)
        }
    }

    fun stopSignals() {
        signals.stop()
        SignalStore.removeListener(onNotification)
    }

    /**
     * One accelerometer reading decides how a call is surfaced. Face up lights the matrix and
     * never the torch; face down uses the torch only and keeps the matrix hidden; unknown means
     * no reaction at all. No caller name, number or message content is ever read.
     */
    private fun onCallFlip(flip: Flip) {
        if (!signalsEnabled) return
        when (flip) {
            Flip.FaceUp -> fireReaction(GlyphModes.PULSE, null)
            Flip.FaceDown -> if (torchEnabled) {
                torchNotice = torch.flash(TorchPatterns.ALERT, torchConditions())
            }
            Flip.Unknown -> Unit
        }
    }

    // ---- Tier 1 signals: notification metadata only, opt-in, in-memory counts ----
    var notifSignalsEnabled by mutableStateOf(prefs.getBoolean("notifSignalsEnabled", false)); private set
    var signalNotice by mutableStateOf<String?>(null); private set
    var notifAccess by mutableStateOf(false); private set

    fun refreshNotifAccess() {
        notifAccess = notificationAccessGranted(ctx)
    }

    fun applyNotifSignals(v: Boolean) {
        notifSignalsEnabled = v
        putB("notifSignalsEnabled", v)
        refreshNotifAccess()
        if (v) {
            signalNotice = if (notifAccess) null else "Grant notification access below to start"
            startSignals()
        } else {
            stopSignals()
            signalNotice = null
        }
    }

    fun openNotificationAccess() {
        runCatching {
            ctx.startActivity(
                Intent("android.settings.NOTIFICATION_LISTENER_SETTINGS")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** (total since start, last hour) — metadata counts only, never content. */
    fun signalCounts(): Pair<Int, Int> = SignalStore.counts(SystemClock.elapsedRealtime())

    /**
     * A notification arrived. Metadata only: the category, the group-summary flag and how loud
     * the notification is decide the reaction. No title, body, sender, icon or extras is read.
     */
    private fun handleNotification(meta: NotificationMeta) {
        if (!notifSignalsEnabled) return
        if (quietNow()) return
        val now = SystemClock.elapsedRealtime()
        if (!shouldReact(lastNotifReactAt, now)) return
        lastNotifReactAt = now
        when {
            // A bundled digest: a wide, calm sweep, never the torch.
            meta.summary -> fireReaction(GlyphModes.WAVE, null)
            // A loud notification: priority pulse, plus the torch pattern if opted in.
            meta.important -> fireReaction(GlyphModes.PULSE, TorchPatterns.ALERT)
            // Anything else: one quiet pulse.
            else -> fireReaction(GlyphModes.TAP, null)
        }
    }

    /** Home grid columns (3..5). Kept as a real setting instead of the old fixed 4. */
    var homeColumns by mutableIntStateOf(prefs.getInt("homeColumns", 4).coerceIn(3, 5)); private set
    /** Always a whole number of rows for [homeColumns] (see data/HomeGrid.kt). */
    var homeAppLimit by mutableStateOf(snapHomeLimit(prefs.getInt("homeAppLimit", 8), homeColumns)); private set
    var shuffleCount by mutableStateOf(prefs.getInt("shuffleCount", 0)); private set
    var shuffleSlot by mutableStateOf(0); private set
    // ---- onboarding (v4 key, migrated from v2) ----
    var onboardingComplete by mutableStateOf(
        prefs.getBoolean("onboarding_done", false) || prefs.getBoolean("onboardingComplete", false)
    ); private set

    // ---- simple mindful toggles (v2 Settings / PauseSheet) ----
    var mindfulEnabled by mutableStateOf(prefs.getBoolean("mindful_enabled", false)); private set
    var escalatingEnabled by mutableStateOf(prefs.getBoolean("escalating_enabled", true)); private set
    var trackingEnabled by mutableStateOf(prefs.getBoolean("tracking_enabled", true)); private set
    var earnScrollEnabled by mutableStateOf(prefs.getBoolean("earn_scroll_enabled", false)); private set
    var dailyBudgetMin by mutableStateOf(prefs.getInt("daily_budget_min", 60)); private set
    var sessionPkg by mutableStateOf(prefs.getString("session_pkg", "") ?: ""); private set
    var sessionStart by mutableStateOf(prefs.getLong("session_start", 0L)); private set
    var unlockUntil by mutableStateOf(prefs.getLong("unlock_until", 0L)); private set

    var destination by mutableStateOf(InternalDestination.Home)
    /** Active sub-page of the Settings app; [SettingsPage.Root] is the top-level list. */
    var settingsPage by mutableStateOf(SettingsPage.Root)

    val pinned = mutableStateListOf<String>().apply { addAll(csv("pinned")) }
    val dock = mutableStateListOf<String>().apply { addAll(csv("dock")) }
    var flagged by mutableStateOf(csv("flagged").toSet()); private set
    var studyApps by mutableStateOf(csv("study").toSet()); private set

    // ---- widgets: home + board (v2 layout, student/mindful types from v4) ----
    val homeWidgets = mutableStateListOf<WidgetItem>().apply {
        addAll(parseWidgets(prefs.getString("home_widgets_v2", null))
            ?: parseWidgets(prefs.getString("widgets_v2", null))
            ?: defaultWidgets())
    }
    val boardWidgets = mutableStateListOf<WidgetItem>().apply {
        addAll(parseWidgets(prefs.getString("board_widgets_v2", null)) ?: emptyList())
    }
    var widgetTargetHome by mutableStateOf(true)
    var pendingWidgetId = -1

    // ---- widget delete undo: keep hosted ids alive until the window closes ----
    private val widgetUndo = UndoBuffer<WidgetItem>()
    var undoLabel by mutableStateOf<String?>(null); private set
    private var undoJob: Job? = null

    // ---- PIN (v4) ----
    val pin = PinLock(prefs)
    var pinSet by mutableStateOf(pin.isSet); private set
    var protectStudy by mutableStateOf(pin.protectStudyMode); private set
    var pinRequest by mutableStateOf<PinRequest?>(null)

    // ---- Detoxify (v6): a timed quiet home, allow-listed, PIN-gated on the way out ----
    private val detoxStore = DetoxStore(prefs)
    var detox by mutableStateOf<DetoxPolicy.Session?>(detoxStore.load()); private set
    var detoxLeft by mutableStateOf(initialDetoxLeft()); private set
    /** Snapshot of the store value for UI; the store is authoritative. */
    var detoxReleaseAt by mutableStateOf(detoxStore.releaseAt); private set
    var detoxSheetOpen by mutableStateOf(false)
    var detoxBlocked by mutableStateOf<String?>(null); private set
    var detoxSummary by mutableStateOf<DetoxPolicy.Summary?>(DetoxPolicy.decodeSummary(detoxStore.summary)); private set

    private var detoxJob: Job? = null
    private var detoxBeat = 0

    /** Phone, camera and clock are never blocked. Resolved once from the system's own answers. */
    val detoxAlways: Set<String> = resolveDetoxAlways()

    /** Remaining time right now, without waiting for the first heartbeat. */
    private fun initialDetoxLeft(): Long =
        detox?.let { DetoxPolicy.remainingMs(it, SystemClock.elapsedRealtime(), System.currentTimeMillis()) } ?: 0L

    /**
     * Phone/camera/clock packages: the system's resident handlers plus well-known vendor
     * packages. Phone and camera also open through direct intents, so this list is mainly for
     * showing them as always usable and for launching their packages. No new permissions.
     */
    private fun resolveDetoxAlways(): Set<String> {
        val out = HashSet<String>()
        val pm = ctx.packageManager
        fun add(i: Intent) {
            runCatching { pm.resolveActivity(i, 0)?.activityInfo?.packageName }.getOrNull()?.let { out.add(it) }
        }
        add(Intent(Intent.ACTION_DIAL))
        runCatching { add(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) }
        add(Intent(AlarmClock.ACTION_SHOW_ALARMS))
        out.addAll(listOf(
            "com.android.phone", "com.android.systemui",
            "com.android.dialer", "com.google.android.dialer", "com.samsung.android.dialer",
            "com.android.camera", "com.android.camera2", "com.google.android.GoogleCamera",
            "com.sec.android.app.camera", "com.samsung.android.camera",
            "com.android.deskclock", "com.google.android.deskclock", "com.sec.android.app.clockpackage"
        ))
        return out
    }

    /** All intents a session may open directly (dialer, camera shortcuts, alarms). */
    fun openSystemIntent(i: Intent) {
        try {
            ctx.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            toast = "not available on this phone"
        }
    }

    /** Starts a session. [selected] is what the user ticked on the start sheet. */
    fun startDetox(lengthMs: Long, selected: Set<String>) {
        if (detox != null) return
        val nowMono = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val s = DetoxPolicy.Session(nowWall, lengthMs, DetoxPolicy.startAllowed(selected, detoxAlways), nowMono, lengthMs)
        detox = s
        detoxLeft = lengthMs
        detoxReleaseAt = 0L
        detoxStore.releaseAt = 0L
        detoxBlocked = null
        detoxStore.summary = ""
        detoxSummary = null
        detoxSheetOpen = false
        goHome()
        toast = "Detoxify is on"
        ensureDetoxLoop()
    }

    private fun ensureDetoxLoop() {
        if (detox == null) return
        if (detoxJob?.isActive == true) return
        detoxJob = viewModelScope.launch {
            while (isActive && detox != null) {
                delay(1000L)
                detoxHeartbeat()
            }
        }
    }

    /**
     * One tick: catch the remaining time up, auto-end at zero, run an overdue early-exit wait.
     * Anchors are persisted about every half minute, and again whenever the activity stops.
     */
    fun detoxHeartbeat() {
        val s = detox ?: return
        val nowMono = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val left = DetoxPolicy.remainingMs(s, nowMono, nowWall)
        if (left <= 0L) { endDetox(endedByTimer = true); return }
        if (DetoxPolicy.releaseDue(detoxReleaseAt, nowWall)) { endDetox(endedByTimer = false); return }
        detoxLeft = left
        detoxReleaseAt = detoxStore.releaseAt
        if (++detoxBeat % 30 == 0) {
            val snap = DetoxPolicy.snapshot(s, nowMono, nowWall)
            detox = snap
            detoxStore.save(snap)
        }
    }

    private fun endDetox(endedByTimer: Boolean) {
        val s = detox ?: return
        val summary = DetoxPolicy.Summary(s.startedWall, s.lengthMs, s.allowed.size, endedByTimer)
        detox = null
        detoxLeft = 0L
        detoxReleaseAt = 0L
        detoxBlocked = null
        detoxJob?.cancel()
        detoxJob = null
        detoxStore.clear()
        detoxStore.summary = DetoxPolicy.encodeSummary(summary)
        detoxSummary = summary
        toast = if (endedByTimer) "Detoxify complete" else "Detoxify ended"
        notifyDetoxDone(summary)
    }

    /** Clears the block explanation once the sheet is dismissed. */
    fun clearDetoxBlock() { detoxBlocked = null }

    fun dismissDetoxSummary() {
        detoxSummary = null
        detoxStore.summary = ""
    }

    // ---- Detoxify early exit: right PIN, then a 10-minute cooling-off ----

    /** Opens the PIN pad (create if the PIN was removed mid-session), then starts the wait. */
    fun requestDetoxEnd() {
        if (detox == null) return
        if (detoxReleaseAt != 0L) { toast = "The exit wait is already running"; return }
        if (pinSet) {
            pinRequest = PinRequest("Enter your PIN", PinMode.Verify) { beginDetoxRelease() }
        } else {
            pinRequest = PinRequest("Set a PIN to end early", PinMode.Create) { beginDetoxRelease() }
        }
    }

    private fun beginDetoxRelease() {
        if (detox == null) return
        val at = System.currentTimeMillis() + DetoxPolicy.RELEASE_WAIT_MS
        detoxReleaseAt = at
        detoxStore.releaseAt = at
        toast = "Detoxify ends in 10 minutes"
    }

    /** Withdraw the pending exit — the safe direction, so no PIN for cancelling. */
    fun cancelDetoxRelease() {
        detoxReleaseAt = 0L
        detoxStore.releaseAt = 0L
        toast = "Withdrawn — the session keeps running"
    }

    /** A quiet "back to normal" nudge; the notification permission is respected by wrapping. */
    private fun notifyDetoxDone(summary: DetoxPolicy.Summary) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        runCatching {
            if (nm.getNotificationChannel(DETOX_CHANNEL) == null) {
                val ch = NotificationChannel(DETOX_CHANNEL, "Detoxify", NotificationManager.IMPORTANCE_DEFAULT)
                ch.description = "When a Detoxify session ends"
                nm.createNotificationChannel(ch)
            }
            val open = PendingIntent.getActivity(
                ctx, 0,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = Notification.Builder(ctx, DETOX_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_focus)
                .setContentTitle(if (summary.endedByTimer) "Detoxify complete" else "Detoxify ended")
                .setContentText("Your phone is back to normal.")
                .setContentIntent(open)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .build()
            nm.notify(DETOX_NOTIF_ID, n)
        }
    }

    private companion object {
        const val DETOX_CHANNEL = "detox"
        const val DETOX_NOTIF_ID = 4343
    }

    // ---- Mindful engine (v4) ----
    val mindful = MindfulEngine(ctx, MindfulStore(prefs), MindfulHostAdapter())

    // ---- transient UI state ----
    var view by mutableStateOf(ViewMode.Home)
    var letter by mutableStateOf('A')
    var letterDir by mutableStateOf(1)
    /** Bumps on every scrubber letter press so re-tapping the current letter still re-scrolls. */
    var scrubTick by mutableStateOf(0)
    var query by mutableStateOf("")
    var chip by mutableStateOf(Chip.All)
    var ctxApp by mutableStateOf<AppInfo?>(null)
    var ctxRect by mutableStateOf(Rect.Zero)
    var pauseApp by mutableStateOf<AppInfo?>(null)
    var pauseRect by mutableStateOf(Rect.Zero)
    var toast by mutableStateOf<String?>(null)
    var widgetCenter by mutableStateOf(false)
    var widgetTab by mutableStateOf(0)
    var editWidgetId by mutableStateOf<String?>(null)
    var selectedWidgetId by mutableStateOf<String?>(null)
    /** Root-space bottom of the selected widget's frame — Widgets() uses it to reveal a clipped corner. */
    var frameBottomPx by mutableStateOf(0f)
    /** Session-only: one resize-hint toast per app run, not persisted. */
    var resizeHintShown = false
    var wallpaperEditor by mutableStateOf(false)
    var pickHomeApps by mutableStateOf(false)
    var arrangingApps by mutableStateOf(false)
    var aboutOpen by mutableStateOf(false)
    var activeBrainTask by mutableStateOf<String?>(null)
    val planTasks = mutableStateListOf<PlanTask>().apply { addAll(parsePlanTasks(prefs.getString("plan_tasks", "") ?: "")) }
    val planEvents = mutableStateListOf<PlanEvent>().apply { addAll(parsePlanEvents(prefs.getString("plan_events", "") ?: "")) }

    init {
        restorePendingWidgetDelete()
        refresh()
        refreshMotionScale()
        refreshShuffleSlot()
        viewModelScope.launch(Dispatchers.IO) {
            Wallpapers.seedPreloads(ctx)
            if (shuffleCount == 0) {
                val sc = Wallpapers.savedCount(ctx)
                if (sc > 0) withContext(Dispatchers.Main) { applyShuffleCount(sc) }
            }
        }
    }

    // ---- loading ----
    fun refresh() {
        viewModelScope.launch(Dispatchers.Default) {
            val list = repo.loadApps()
            val installed = list.map { it.pkg }.toSet()
            withContext(Dispatchers.Main) {
                apps = list
                byPkg = list.associateBy { it.pkg }
                if (!prefs.contains("pinned")) { pinned.clear(); pinned.addAll(repo.defaultHome(installed)); saveCsv("pinned", pinned) }
                if (!prefs.contains("dock")) { dock.clear(); dock.addAll(repo.defaultDock(installed)); saveCsv("dock", dock) }
                if (!prefs.contains("flagged")) {
                    flagged = AppRepository.DISTRACTING_DEFAULTS.filter { it in installed }.toSet(); saveCsv("flagged", flagged)
                }
                if (!prefs.contains("study")) {
                    studyApps = AppRepository.STUDY_DEFAULTS.filter { it in installed }.toSet(); saveCsv("study", studyApps)
                }
                pinned.removeAll { it !in installed }
                dock.removeAll { it !in installed }
                icons.keys.filter { it !in installed }.forEach { icons.remove(it) }
            }
            val px = (ctx.resources.displayMetrics.density * 56f).toInt().coerceAtLeast(96)
            // Insert icons in batches: one state write per icon would trigger a recomposition
            // storm while the app list is still being populated on the main thread.
            val pending = ArrayList<Pair<String, AppIcons>>(16)
            suspend fun flush() {
                if (pending.isEmpty()) return
                val batch = pending.toList()
                pending.clear()
                withContext(Dispatchers.Main) { batch.forEach { (k, v) -> icons[k] = v } }
            }
            list.forEach { a ->
                if (icons[a.pkg] == null) repo.loadIcon(a, px)?.let { pending.add(a.pkg to it) }
                if (pending.size >= 8) flush()
            }
            flush()
        }
    }

    fun appFor(pkg: String): AppInfo? = byPkg[pkg]
    fun isVisible(a: AppInfo) = !((studyMode || mindful.hidesFlagged) && a.pkg in flagged)

    // ---- launcher panel data ----
    fun letterOf(label: String): Char {
        val c = label.trim().firstOrNull()?.uppercaseChar() ?: '#'
        return if (c in 'A'..'Z') c else '#'
    }

    private fun pool(): List<AppInfo> = apps.filter {
        it.pkg !in pinned && isVisible(it) && (chip != Chip.Study || it.pkg in studyApps)
    }

    fun availableLetters(): Set<Char> = pool().map { letterOf(it.label) }.toSet()

    fun groupedApps(): List<Pair<Char, List<AppInfo>>> =
        pool().groupBy { letterOf(it.label) }.toSortedMap().toList()

    fun appsForKey(key: String): List<AppInfo> {
        val q = query.trim().lowercase()
        return if (key == "q") pool().filter { it.label.lowercase().contains(q) }
        else pool().filter { letterOf(it.label) == key[0] }
    }

    // ---- navigation ----
    fun onScrub(i: Int) {
        when (i) {
            0 -> goHome()
            27 -> Unit
            else -> {
                val l = 'A' + (i - 1)
                scrubTick++
                if (view != ViewMode.All || l != letter || query.isNotEmpty()) {
                    letterDir = if (l >= letter) 1 else -1
                    letter = l
                    query = ""
                    view = ViewMode.All
                }
            }
        }
    }

    fun onScrubRelease(i: Int) { if (i == 27) { settingsPage = SettingsPage.Root; destination = InternalDestination.Settings } }

    fun goHome() { view = ViewMode.Home; query = ""; ctxApp = null; settingsPage = SettingsPage.Root }

    fun onHomePressed() {
        goHome()
        pauseApp = null
        editWidgetId = null
        widgetCenter = false
        aboutOpen = false
        wallpaperEditor = false
        pickHomeApps = false
        selectedWidgetId = null
        activeBrainTask = null
        destination = InternalDestination.Home
        settingsPage = SettingsPage.Root
        mindful.cancelDoor(false)
        mindful.settingsOpen = false
        mindful.appLimitsOpen = false
        pinRequest = null
        refreshShuffleSlot()
    }

    fun back() {
        when {
            settingsPage != SettingsPage.Root -> settingsPage = SettingsPage.Root
            wallpaperEditor -> wallpaperEditor = false
            pickHomeApps -> pickHomeApps = false
            aboutOpen -> aboutOpen = false
            editWidgetId != null -> editWidgetId = null
            widgetCenter -> widgetCenter = false
            selectedWidgetId != null -> selectedWidgetId = null
            activeBrainTask != null -> activeBrainTask = null
            ctxApp != null -> ctxApp = null
            pinRequest != null -> pinRequest = null
            mindful.appLimitsOpen -> mindful.appLimitsOpen = false
            mindful.settingsOpen -> mindful.settingsOpen = false
            mindful.door != null -> mindful.cancelDoor(true)
            pauseApp != null -> pauseApp = null
            destination != InternalDestination.Home -> destination = InternalDestination.Home
            view == ViewMode.All -> goHome()
        }
    }

    // ---- app actions ----
    fun togglePin(pkg: String) {
        if (pkg in pinned) pinned.remove(pkg)
        else if (pinned.size >= homeAppLimit) toast = "Home holds $homeAppLimit apps"
        else pinned.add(pkg)
        saveCsv("pinned", pinned)
    }

    /** Drag-rearrange: move [dragged] next to [target] in pinned order (display order mirrors pinned among visible apps). */
    fun movePinned(dragged: String, target: String, after: Boolean) {
        val from = pinned.indexOf(dragged)
        if (from < 0) return
        pinned.removeAt(from)
        val to = pinned.indexOf(target)
        if (to < 0) pinned.add(if (after) pinned.size else 0, dragged)
        else pinned.add(if (after) to + 1 else to, dragged)
        saveCsv("pinned", pinned)
    }

    fun toggleDock(pkg: String) {
        if (pkg in dock) dock.remove(pkg)
        else if (dock.size >= 5) toast = "Dock holds 5 apps"
        else dock.add(pkg)
        saveCsv("dock", dock)
    }

    fun toggleFlag(pkg: String) {
        if (pkg in flagged) guarded("Remove from distracting") { applyToggleFlag(pkg) } else applyToggleFlag(pkg)
    }

    private fun applyToggleFlag(pkg: String) {
        flagged = if (pkg in flagged) flagged - pkg else flagged + pkg
        saveCsv("flagged", flagged)
        toast = if (pkg in flagged) "Marked as distracting" else "Removed from distracting"
    }

    fun setFlaggedDirect(pkg: String, on: Boolean) {
        flagged = if (on) flagged + pkg else flagged - pkg
        saveCsv("flagged", flagged)
    }

    fun toggleStudyApp(pkg: String) {
        studyApps = if (pkg in studyApps) studyApps - pkg else studyApps + pkg
        saveCsv("study", studyApps)
        toast = if (pkg in studyApps) "Marked as study app" else "Removed from study apps"
    }

    fun startApp(pkg: String) {
        // Detoxify: blocked before anything else, whatever screen the tap came from.
        val d = detox
        if (d != null && !DetoxPolicy.isAllowed(pkg, d, detoxAlways)) {
            detoxBlocked = pkg
            return
        }
        val i = ctx.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        if (pkg in flagged && reminderMin > 0) {
            FocusReminders.start(ctx, pkg, appFor(pkg)?.label ?: "this app", reminderMin, reminderMin)
        }
        if (mindfulEnabled && trackingEnabled && pkg in flagged) beginSession(pkg)
        val o = ActivityOptions.makeCustomAnimation(ctx, R.anim.app_in, R.anim.hold)
        ctx.startActivity(i, o.toBundle())
    }

    // ---- simple session tracking (v2 Today widget) ----
    private fun beginSession(pkg: String) {
        sessionPkg = pkg
        sessionStart = System.currentTimeMillis()
        putS("session_pkg", pkg)
        prefs.edit().putLong("session_start", sessionStart).apply()
    }

    fun endSession() {
        val pkg = sessionPkg
        if (pkg.isNotBlank() && sessionStart > 0L) {
            val elapsed = System.currentTimeMillis() - sessionStart
            val mins = (elapsed / 60_000).toInt().coerceIn(0, 300)
            if (mins > 0 && trackingEnabled) recordUsage(pkg, mins)
            sessionPkg = ""; sessionStart = 0L
            prefs.edit().remove("session_pkg").remove("session_start").apply()
        }
        mindful.endSession()
    }

    fun effectivePauseSec(): Int {
        if (!mindfulEnabled || !escalatingEnabled) return pauseSec
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("pause_date", "") ?: ""
        val count = if (savedDate == today) prefs.getInt("pause_count_today", 0) else 0
        if (count == 0) return pauseSec
        val escalated = (5 * (1 shl (count - 1))).coerceAtMost(60)
        return escalated.coerceAtLeast(pauseSec)
    }

    fun incrementPauseCount() {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("pause_date", "") ?: ""
        val count = if (savedDate == today) prefs.getInt("pause_count_today", 0) else 0
        prefs.edit().putString("pause_date", today).putInt("pause_count_today", count + 1).apply()
    }

    fun getPauseCount(): Int {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("pause_date", "") ?: ""
        return if (savedDate == today) prefs.getInt("pause_count_today", 0) else 0
    }

    // ponytail: called per widget recomposition — cache the prefs parse per day,
    // invalidated by recordUsage writes and date rollover.
    private var usageDay = ""
    private var usageList: List<DailyUsage> = emptyList()

    fun getTodayUsage(): List<DailyUsage> {
        val today = LocalDate.now().toString()
        if (usageDay == today) return usageList
        val savedDate = prefs.getString("today_date", "") ?: ""
        usageDay = today
        usageList = if (savedDate != today) {
            emptyList()
        } else {
            val raw = prefs.getString("today_usage", "") ?: ""
            if (raw.isBlank()) emptyList()
            else raw.split("|").mapNotNull { part ->
                val p = part.split(":")
                if (p.size == 2) DailyUsage(p[0], (p[1].toIntOrNull() ?: 0)) else null
            }
        }
        return usageList
    }

    private fun recordUsage(pkg: String, mins: Int) {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("today_date", "") ?: ""
        val list = if (savedDate == today) getTodayUsage().toMutableList() else mutableListOf()
        val idx = list.indexOfFirst { it.pkg == pkg }
        if (idx >= 0) list[idx] = DailyUsage(pkg, list[idx].minutes + mins) else list.add(DailyUsage(pkg, mins))
        val encoded = list.joinToString("|") { "${it.pkg}:${it.minutes}" }
        prefs.edit().putString("today_date", today).putString("today_usage", encoded).apply()
        usageDay = "" // invalidate so the next read re-parses the written value
    }

    fun unlockApp(pkg: String, minutes: Int) {
        unlockUntil = System.currentTimeMillis() + minutes * 60_000L
        prefs.edit().putLong("unlock_until", unlockUntil).apply()
        toast = "Unlocked for $minutes minutes"
    }

    fun isUnlocked(pkg: String): Boolean = System.currentTimeMillis() < unlockUntil

    fun completeBrainTask() {
        val pkg = pauseApp?.pkg ?: return
        unlockApp(pkg, 10)
        pauseApp = null
        activeBrainTask = null
        reactionDone()
    }

    // ---- mindful simple setters (v2 Settings) ----
    fun applyMindful(v: Boolean) { mindfulEnabled = v; putB("mindful_enabled", v) }
    fun applyEscalating(v: Boolean) { escalatingEnabled = v; putB("escalating_enabled", v) }
    fun applyTracking(v: Boolean) { trackingEnabled = v; putB("tracking_enabled", v) }
    fun applyEarnScroll(v: Boolean) { earnScrollEnabled = v; putB("earn_scroll_enabled", v) }
    fun applyDailyBudget(v: Int) { dailyBudgetMin = v; prefs.edit().putInt("daily_budget_min", v).apply() }

    fun openAppInfo(pkg: String) {
        val i = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(i)
    }

    // ---- widgets ----
    private fun saveHomeWidgets() = putS("home_widgets_v2", homeWidgets.toList().toJson())
    private fun saveBoardWidgets() = putS("board_widgets_v2", boardWidgets.toList().toJson())

    private fun activeWidgets() = if (widgetTargetHome) homeWidgets else boardWidgets
    private fun saveActive() { if (widgetTargetHome) saveHomeWidgets() else saveBoardWidgets() }

    fun addWidget(type: String) {
        val list = activeWidgets()
        list.add(newWidget(type))
        saveActive()
        toast = "${WT.title(type)} added to ${if (widgetTargetHome) "Home" else "Board"}"
    }

    fun commitAndroidWidget(appId: Int) {
        activeWidgets().add(WidgetItem(UUID.randomUUID().toString(), WT.ANDROID, cfg = mapOf("appWidgetId" to appId.toString())))
        saveActive()
        pendingWidgetId = -1
        widgetCenter = false
    }

    fun cancelPendingWidget() {
        if (pendingWidgetId >= 0) hostMgr.release(pendingWidgetId)
        pendingWidgetId = -1
    }

    fun removeWidget(id: String) {
        val onHome = homeWidgets.indexOfFirst { it.id == id }
        val onBoard = boardWidgets.indexOfFirst { it.id == id }
        if (onHome < 0 && onBoard < 0) return
        val isHome = onHome >= 0
        val list = if (isHome) homeWidgets else boardWidgets
        val index = if (isHome) onHome else onBoard
        val w = list[index]

        // A second delete supersedes the first; commit that one before staging this one.
        commitPendingWidgetDelete()

        list.removeAt(index)
        if (editWidgetId == id) editWidgetId = null
        if (selectedWidgetId == id) selectedWidgetId = null
        if (isHome) saveHomeWidgets() else saveBoardWidgets()

        widgetUndo.stage(w, index, if (isHome) "home" else "board", System.currentTimeMillis())
        undoLabel = WT.title(w.type)
        persistPendingWidgetDelete()
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(5_200)
            finalizeWidgetUndo()
        }
    }

    /** Restores the last removed widget at its original index, if the window is still open. */
    fun undoRemoveWidget() {
        // Capture before undo(): an expired entry is dropped by undo() and still needs its
        // hosted id released, otherwise the delete window would leak the AppWidgetHost id.
        val pendingBefore = widgetUndo.pending
        val entry = widgetUndo.undo(System.currentTimeMillis()) ?: run {
            if (pendingBefore != null) {
                widgetUndo.restore(pendingBefore)
                commitPendingWidgetDelete()
            }
            undoLabel = null
            return
        }
        val list = if (entry.listKey == "home") homeWidgets else boardWidgets
        list.add(entry.index.coerceIn(0, list.size), entry.item)
        if (entry.listKey == "home") saveHomeWidgets() else saveBoardWidgets()
        undoJob?.cancel()
        undoJob = null
        undoLabel = null
        prefs.edit { remove("pending_widget_delete") }
    }

    private fun finalizeWidgetUndo() {
        val entry = widgetUndo.takeExpired(System.currentTimeMillis()) ?: return
        releaseHosted(entry.item)
        undoLabel = null
        prefs.edit { remove("pending_widget_delete") }
    }

    /** Ends the undo window immediately (used when a newer delete arrives). */
    private fun commitPendingWidgetDelete() {
        val entry = widgetUndo.pending ?: return
        widgetUndo.clear()
        releaseHosted(entry.item)
        undoLabel = null
        undoJob?.cancel()
        undoJob = null
        prefs.edit { remove("pending_widget_delete") }
    }

    private fun releaseHosted(item: WidgetItem) {
        if (item.type != WT.ANDROID) return
        item.get("appWidgetId").toIntOrNull()?.let { runCatching { hostMgr.release(it) } }
    }

    private fun persistPendingWidgetDelete() {
        val entry = widgetUndo.pending ?: return
        val o = JSONObject()
            .put("item", listOf(entry.item).toJson())
            .put("index", entry.index)
            .put("list", entry.listKey)
            .put("expires", entry.expiresAt)
        putS("pending_widget_delete", o.toString())
    }

    /** A process kill during the undo window must not leak an Android widget host id. */
    private fun restorePendingWidgetDelete() {
        val raw = prefs.getString("pending_widget_delete", null) ?: return
        val o = runCatching { JSONObject(raw) }.getOrNull()
        val item = o?.optString("item")?.let { parseWidgets(it)?.firstOrNull() }
        if (o == null || item == null) {
            prefs.edit { remove("pending_widget_delete") }
            return
        }
        val expires = o.optLong("expires", 0L)
        val now = System.currentTimeMillis()
        val listKey = o.optString("list", "home")
        if (expires <= now) {
            releaseHosted(item)
            prefs.edit { remove("pending_widget_delete") }
            return
        }
        val entry = UndoEntry(item, o.optInt("index", 0), listKey, expires)
        widgetUndo.restore(entry)
        undoLabel = WT.title(item.type)
        undoJob = viewModelScope.launch {
            delay((expires - System.currentTimeMillis()).coerceAtLeast(0L) + 200L)
            finalizeWidgetUndo()
        }
    }

    fun moveWidget(id: String, delta: Int) {
        val inHome = homeWidgets.indexOfFirst { it.id == id }
        val list: MutableList<WidgetItem>
        val i: Int
        if (inHome >= 0) {
            list = homeWidgets; i = inHome
        } else {
            val inBoard = boardWidgets.indexOfFirst { it.id == id }
            if (inBoard < 0) return
            list = boardWidgets; i = inBoard
        }
        val j = i + delta
        if (j !in list.indices) return
        val w = list.removeAt(i)
        list.add(j, w)
        if (list === homeWidgets) saveHomeWidgets() else saveBoardWidgets()
    }

    fun reorderBoardWidget(from: Int, to: Int) {
        if (from !in boardWidgets.indices || to !in boardWidgets.indices || from == to) return
        val w = boardWidgets.removeAt(from)
        boardWidgets.add(to, w)
        saveBoardWidgets()
    }

    fun setWidgetHeight(id: String, h: Int) {
        val clamped = h.coerceIn(1, 3)
        updateWidget(id) { it.copy(height = clamped) }
    }

    fun updateWidget(id: String, f: (WidgetItem) -> WidgetItem) {
        val inHome = homeWidgets.indexOfFirst { it.id == id }
        if (inHome >= 0) {
            homeWidgets[inHome] = f(homeWidgets[inHome])
            saveHomeWidgets()
            return
        }
        val inBoard = boardWidgets.indexOfFirst { it.id == id }
        if (inBoard >= 0) {
            boardWidgets[inBoard] = f(boardWidgets[inBoard])
            saveBoardWidgets()
        }
    }

    fun setCfg(id: String, key: String, value: String) = updateWidget(id) { it.put(key, value) }

    fun findWidget(id: String): WidgetItem? =
        homeWidgets.firstOrNull { it.id == id } ?: boardWidgets.firstOrNull { it.id == id }

    // ---- wallpaper ----
    fun saveWallpaperAdjust(blur: Float, dim: Float, zoom: Float, panX: Float, panY: Float) {
        if (wallpaper == "shuffle") {
            val i = shuffleSlot
            prefs.edit().putFloat("wpBlur_$i", blur).putFloat("wpDim_$i", dim).putFloat("wpZoom_$i", zoom)
                .putFloat("wpPanX_$i", panX).putFloat("wpPanY_$i", panY).apply()
        } else {
            wpBlur = blur; wpDim = dim; wpZoom = zoom; wpPanX = panX; wpPanY = panY
            prefs.edit().putFloat("wpBlur", blur).putFloat("wpDim", dim).putFloat("wpZoom", zoom)
                .putFloat("wpPanX", panX).putFloat("wpPanY", panY).apply()
        }
        wallpaperVersion++
    }

    /** Adjusts for the photo currently on screen (per-slot when shuffle is on). */
    fun currentWpAdjust(): WpAdjust =
        if (wallpaper == "shuffle") WpAdjust(
            prefs.getFloat("wpBlur_$shuffleSlot", 0f),
            prefs.getFloat("wpDim_$shuffleSlot", 0f),
            prefs.getFloat("wpZoom_$shuffleSlot", 1f),
            prefs.getFloat("wpPanX_$shuffleSlot", 0f),
            prefs.getFloat("wpPanY_$shuffleSlot", 0f)
        ) else WpAdjust(wpBlur, wpDim, wpZoom, wpPanX, wpPanY)

    fun setPhotoWallpaper(uri: Uri, slot: Int = 0) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = Wallpapers.savePhoto(ctx, uri, slot)
            withContext(Dispatchers.Main) {
                if (ok) {
                    wallpaperVersion++
                    if (wallpaper != "shuffle") {
                        applyWallpaper(if (slot == 0) "photo" else "shuffle")
                        wpZoom = 1f; wpPanX = 0f; wpPanY = 0f
                        wallpaperEditor = true
                    } else {
                        applyShuffleCount(Wallpapers.savedCount(ctx))
                        toast = "Photo ${slot + 1} saved"
                    }
                } else toast = "Couldn't load that photo"
            }
        }
    }

    /** Batch-save shuffle photos (one IO pass, one toast). Clears leftover higher slots. */
    fun setShufflePhotos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            var ok = 0
            uris.take(Wallpapers.MAX_SHUFFLE).forEachIndexed { i, u ->
                if (Wallpapers.savePhoto(ctx, u, i)) ok++
            }
            // Delete slots beyond the new batch so stale photos don't linger
            for (slot in ok until Wallpapers.MAX_SHUFFLE) {
                Wallpapers.removePhoto(ctx, slot)
            }
            withContext(Dispatchers.Main) {
                if (ok > 0) {
                    wallpaperVersion++
                    applyWallpaper("shuffle")
                    applyShuffleCount(ok)
                    toast = "Shuffle set with $ok photo${if (ok == 1) "" else "s"}"
                } else toast = "Couldn't load those photos"
            }
        }
    }

    fun applyShuffleCount(n: Int) {
        shuffleCount = n.coerceIn(0, Wallpapers.MAX_SHUFFLE)
        prefs.edit().putInt("shuffleCount", shuffleCount).apply()
        refreshShuffleSlot()
    }

    fun refreshShuffleSlot() {
        if (wallpaper != "shuffle" || shuffleCount <= 0) { shuffleSlot = 0; return }
        val s = Wallpapers.slotForDay(shuffleCount)
        if (s != shuffleSlot) {
            shuffleSlot = s
            wallpaperVersion++
        }
    }

    fun removeShuffleSlot(slot: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            Wallpapers.removePhoto(ctx, slot)
            // Compact remaining photos to dense 0..n-1 so slotForDay never hits a hole
            Wallpapers.compactPhotos(ctx)
            withContext(Dispatchers.Main) {
                val n = Wallpapers.savedCount(ctx)
                applyShuffleCount(n)
                wallpaperVersion++
                if (n == 0) applyWallpaper("dune")
                toast = "Photo removed"
            }
        }
    }

    fun applyHomeAppLimit(v: Int) { homeAppLimit = v.coerceIn(1, 16); prefs.edit().putInt("homeAppLimit", homeAppLimit).apply() }

    /** Changing columns keeps the same number of rows, so the last row is never ragged. */
    fun applyHomeColumns(v: Int) {
        homeColumns = v.coerceIn(3, 5)
        prefs.edit().putInt("homeColumns", homeColumns).apply()
        val snapped = snapHomeLimit(homeAppLimit, homeColumns)
        if (snapped != homeAppLimit) applyHomeAppLimit(snapped)
    }

    // ---- optional PIN ----
    fun guarded(title: String, action: () -> Unit) {
        if (pinSet) pinRequest = PinRequest(title, PinMode.Verify, action) else action()
    }

    fun requestCreatePin(onDone: () -> Unit = {}) { pinRequest = PinRequest("Create a PIN", PinMode.Create, onDone) }
    fun requestChangePin() { pinRequest = PinRequest("Choose a new PIN", PinMode.Create) { } }
    fun requestRemovePin() {
        pin.clear()
        onPinChanged()
        toast = "PIN removed"
    }
    fun applyProtectStudy(v: Boolean) { pin.protectStudyMode = v; protectStudy = v }
    fun onPinChanged() { pinSet = pin.isSet; protectStudy = pin.protectStudyMode }

    // ---- settings setters (v2 names + v4 aliases for Menus) ----
    private fun putS(k: String, v: String) = prefs.edit().putString(k, v).apply()
    private fun putB(k: String, v: Boolean) = prefs.edit().putBoolean(k, v).apply()

    fun applyTheme(v: ThemeMode) { themeMode = v; putS("theme", v.name) }
    fun applyWallpaper(id: String) { wallpaper = id; putS("wallpaper", id) }
    fun applyIconPack(v: IconPack) { iconPack = v; putS("iconPack", v.name) }

    /** Reads the icon pack, migrating the pre-3.1 `iconStyle` values to the new pack names. */
    private fun loadIconPack(): IconPack =
        iconPackFromStored(prefs.getString("iconPack", null) ?: prefs.getString("iconStyle", null))
    fun applyIconShape(v: IconShape) { iconShape = v; putS("iconShape", v.name) }
    fun applyLayout(v: HomeLayout) { layout = v; putS("layout", v.name) }
    fun applyAlphaSide(v: AlphaSide) { alphaSide = v; putS("alphaSide", v.name) }
    fun applyNormalScroll(v: Boolean) { normalScroll = v; putB("normalScroll", v) }
    fun applyDockStyle(v: DockStyle) { dockStyle = v; putS("dockStyle", v.name) }
    fun applyShowNames(v: Boolean) { showNames = v; putB("showNames", v) }
    fun applyStudyMode(v: Boolean) { studyMode = v; putB("studyMode", v) }
    fun applyPauseSec(v: Int) { pauseSec = v; prefs.edit().putInt("pauseSec", v).apply() }
    fun applyReminderMin(v: Int) { reminderMin = v; prefs.edit().putInt("reminderMin", v).apply() }
    // ponytail: Settings.Global IPC on every read; cached until motionIntensity changes
    private var sysAnimScale = 1f
    fun refreshMotionScale() {
        sysAnimScale = runCatching {
            android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    fun applyMotionIntensity(v: Float) {
        motionIntensity = v
        prefs.edit().putFloat("motionIntensity", v).apply()
        refreshMotionScale()
    }
    fun applyCompactDensity(v: Boolean) { compactDensity = v; putB("compactDensity", v) }
    fun applyAccent(id: String) { accentId = id; putS("accentId", id) }
    fun applyWpAccent(v: Boolean) { wpAccent = v; putB("wpAccent", v) }
    fun applyUiFont(f: String) { uiFont = f; putS("uiFont", f) }
    fun applyClockStyle(s: String) { clockStyle = s; putS("clockStyle", s) }
    fun effectiveMotion(): Float =
        if (sysAnimScale == 0f) 0f else (motionIntensity * sysAnimScale).coerceIn(0f, 1f)

    fun finishOnboarding() { onboardingComplete = true; putB("onboarding_done", true); putB("onboardingComplete", true) }
    fun completeOnboarding() = finishOnboarding()
    fun resetOnboarding() { onboardingComplete = false; putB("onboarding_done", false); putB("onboardingComplete", false) }

    // ---- plan ----
    fun addPlanTask(title: String, due: String = "", priority: Int = 1, className: String = "") {
        if (title.isBlank()) return
        if (due.isNotBlank() && runCatching { LocalDate.parse(due) }.isFailure) { toast = "Use a valid date: YYYY-MM-DD"; return }
        planTasks.add(PlanTask(UUID.randomUUID().toString(), title.trim(), due, priority, false, className.trim()))
        savePlanTasks()
    }
    fun updatePlanTask(task: PlanTask) {
        if (task.due.isNotBlank() && runCatching { LocalDate.parse(task.due) }.isFailure) { toast = "Use a valid date: YYYY-MM-DD"; return }
        val i = planTasks.indexOfFirst { it.id == task.id }
        if (i >= 0) { planTasks[i] = task; savePlanTasks() }
    }
    fun removePlanTask(id: String) { planTasks.removeAll { it.id == id }; savePlanTasks() }
    fun startFocusFor(task: PlanTask) {
        val timer = homeWidgets.firstOrNull { it.type == WT.TIMER } ?: boardWidgets.firstOrNull { it.type == WT.TIMER }
        if (timer != null) setCfg(timer.id, "start", System.currentTimeMillis().toString())
        else { toast = "Add a Study timer widget to begin focus"; return }
        toast = "Focus: ${task.title}"; destination = InternalDestination.Home
    }
    fun addPlanEvent(title: String, date: String, time: String = "", course: String = "") {
        if (title.isNotBlank() && runCatching { LocalDate.parse(date) }.isSuccess) {
            planEvents.add(PlanEvent(UUID.randomUUID().toString(), title.trim(), date, time, course.trim())); savePlanEvents()
        } else toast = "Use a valid date: YYYY-MM-DD"
    }
    fun removePlanEvent(id: String) { planEvents.removeAll { it.id == id }; savePlanEvents() }
    private fun savePlanTasks() = putS("plan_tasks", planTasks.joinToString("\n") { listOf(it.id, it.title.replace("|", " "), it.due, it.priority, it.completed, it.className.replace("|", " ")).joinToString("|") })
    private fun parsePlanTasks(raw: String) = raw.lineSequence().mapNotNull { line ->
        val p = line.split("|"); if (p.size < 6) null else PlanTask(p[0], p[1], p[2], p[3].toIntOrNull() ?: 1, p[4].toBoolean(), p[5])
    }.toList()
    private fun savePlanEvents() = putS("plan_events", planEvents.joinToString("\n") { listOf(it.id, it.title.replace("|", " "), it.date, it.time, it.course.replace("|", " ")).joinToString("|") })
    private fun parsePlanEvents(raw: String) = raw.lineSequence().mapNotNull { line ->
        val p = line.split("|"); if (p.size < 5) null else PlanEvent(p[0], p[1], p[2], p[3], p[4])
    }.toList()

    // ---- prefs helpers ----
    private inline fun <reified E : Enum<E>> enumPref(key: String, def: E): E =
        runCatching { enumValueOf<E>(prefs.getString(key, def.name) ?: def.name) }.getOrDefault(def)

    private fun csv(k: String): List<String> =
        (prefs.getString(k, "") ?: "").split(",").filter { it.isNotBlank() }
    private fun saveCsv(k: String, l: Collection<String>) =
        prefs.edit().putString(k, l.joinToString(",")).apply()

    /** Lets Mindful Mode read exam countdown and tasks without knowing about widgets. */
    private inner class MindfulHostAdapter : MindfulHost {
        private val allWidgets: List<WidgetItem> get() = homeWidgets + boardWidgets

        override fun examDaysLeft(): Int? = allWidgets
            .filter { it.type == WT.COUNTDOWN }
            .mapNotNull { w ->
                runCatching { ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(w.get("date"))).toInt() }.getOrNull()
            }
            .filter { it >= 0 }
            .minOrNull()

        override fun basePauseSec(): Int = pauseSec
        override fun reminderRepeatMin(): Int = reminderMin

        override fun openTasks(): List<TaskRef> = allWidgets
            .filter { it.type == WT.TASKS }
            .flatMap { w ->
                parseTasks(w.get("items")).mapIndexedNotNull { i, t ->
                    if (!t.first) TaskRef(w.id, i, t.second) else null
                }
            }

        override fun completeTask(ref: TaskRef) {
            updateWidget(ref.widgetId) { w ->
                val list = parseTasks(w.get("items")).toMutableList()
                if (ref.index in list.indices) list[ref.index] = true to list[ref.index].second
                w.put("items", encodeTasks(list))
            }
        }
    }
}
