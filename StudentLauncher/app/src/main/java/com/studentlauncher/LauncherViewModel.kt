package com.studentlauncher

import android.app.ActivityOptions
import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studentlauncher.data.*
import com.studentlauncher.mindful.MindfulEngine
import com.studentlauncher.mindful.MindfulHost
import com.studentlauncher.mindful.MindfulStore
import com.studentlauncher.mindful.TaskRef
import com.studentlauncher.security.PinLock
import com.studentlauncher.ui.PinMode
import com.studentlauncher.ui.PinRequest
import com.studentlauncher.ui.WpAdjust
import com.studentlauncher.ui.Wallpapers
import com.studentlauncher.ui.encodeTasks
import com.studentlauncher.ui.parseTasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var iconStyle by mutableStateOf(enumPref("iconStyle", IconStyle.Mono)); private set
    var iconShape by mutableStateOf(enumPref("iconShape", IconShape.Squircle)); private set
    var layout by mutableStateOf(enumPref("layout", HomeLayout.List)); private set
    var alphaSide by mutableStateOf(enumPref("alphaSide", AlphaSide.Right)); private set
    var dockStyle by mutableStateOf(enumPref("dockStyle", DockStyle.Bottom)); private set
    var showNames by mutableStateOf(prefs.getBoolean("showNames", true)); private set
    var studyMode by mutableStateOf(prefs.getBoolean("studyMode", false)); private set
    var pauseSec by mutableStateOf(prefs.getInt("pauseSec", 5)); private set
    var reminderMin by mutableStateOf(prefs.getInt("reminderMin", 10)); private set
    var motionIntensity by mutableStateOf(prefs.getFloat("motionIntensity", 1f)); private set
    var compactDensity by mutableStateOf(prefs.getBoolean("compactDensity", false)); private set
    var accentId by mutableStateOf(prefs.getString("accentId", "red") ?: "red"); private set
    var uiFont by mutableStateOf(prefs.getString("uiFont", "system") ?: "system"); private set
    var homeAppLimit by mutableStateOf(prefs.getInt("homeAppLimit", 8)); private set
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

    // ---- PIN (v4) ----
    val pin = PinLock(prefs)
    var pinSet by mutableStateOf(pin.isSet); private set
    var protectStudy by mutableStateOf(pin.protectStudyMode); private set
    var pinRequest by mutableStateOf<PinRequest?>(null)

    // ---- Mindful engine (v4) ----
    val mindful = MindfulEngine(ctx, MindfulStore(prefs), MindfulHostAdapter())

    // ---- transient UI state ----
    var view by mutableStateOf(ViewMode.Home)
    var letter by mutableStateOf('A')
    var letterDir by mutableStateOf(1)
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
    var wallpaperEditor by mutableStateOf(false)
    var pickHomeApps by mutableStateOf(false)
    var aboutOpen by mutableStateOf(false)
    var activeBrainTask by mutableStateOf<String?>(null)
    val planTasks = mutableStateListOf<PlanTask>().apply { addAll(parsePlanTasks(prefs.getString("plan_tasks", "") ?: "")) }
    val planEvents = mutableStateListOf<PlanEvent>().apply { addAll(parsePlanEvents(prefs.getString("plan_events", "") ?: "")) }

    init { refresh(); refreshMotionScale(); refreshShuffleSlot() }

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
            list.forEach { a ->
                if (icons[a.pkg] == null) repo.loadIcon(a, px)?.let { ic ->
                    withContext(Dispatchers.Main) { icons[a.pkg] = ic }
                }
            }
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
                if (view != ViewMode.All || l != letter || query.isNotEmpty()) {
                    letterDir = if (l >= letter) 1 else -1
                    letter = l
                    query = ""
                    view = ViewMode.All
                }
            }
        }
    }

    fun onScrubRelease(i: Int) { if (i == 27) destination = InternalDestination.Settings }

    fun goHome() { view = ViewMode.Home; query = ""; ctxApp = null }

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
        mindful.cancelDoor(false)
        mindful.settingsOpen = false
        mindful.appLimitsOpen = false
        pinRequest = null
        refreshShuffleSlot()
    }

    fun back() {
        when {
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

    fun getTodayUsage(): List<DailyUsage> {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("today_date", "") ?: ""
        if (savedDate != today) return emptyList()
        val raw = prefs.getString("today_usage", "") ?: ""
        if (raw.isBlank()) return emptyList()
        return raw.split("|").mapNotNull { part ->
            val p = part.split(":")
            if (p.size == 2) DailyUsage(p[0], (p[1].toIntOrNull() ?: 0)) else null
        }
    }

    private fun recordUsage(pkg: String, mins: Int) {
        val today = LocalDate.now().toString()
        val savedDate = prefs.getString("today_date", "") ?: ""
        val list = if (savedDate == today) getTodayUsage().toMutableList() else mutableListOf()
        val idx = list.indexOfFirst { it.pkg == pkg }
        if (idx >= 0) list[idx] = DailyUsage(pkg, list[idx].minutes + mins) else list.add(DailyUsage(pkg, mins))
        val encoded = list.joinToString("|") { "${it.pkg}:${it.minutes}" }
        prefs.edit().putString("today_date", today).putString("today_usage", encoded).apply()
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
        val w = homeWidgets.firstOrNull { it.id == id } ?: boardWidgets.firstOrNull { it.id == id } ?: return
        val list = if (homeWidgets.contains(w)) homeWidgets else boardWidgets
        if (w.type == WT.ANDROID) w.get("appWidgetId").toIntOrNull()?.let { hostMgr.release(it) }
        list.remove(w)
        if (editWidgetId == id) editWidgetId = null
        if (selectedWidgetId == id) selectedWidgetId = null
        if (list === homeWidgets) saveHomeWidgets() else saveBoardWidgets()
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
    fun applyIconStyle(v: IconStyle) { iconStyle = v; putS("iconStyle", v.name) }
    fun applyIconShape(v: IconShape) { iconShape = v; putS("iconShape", v.name) }
    fun applyLayout(v: HomeLayout) { layout = v; putS("layout", v.name) }
    fun applyAlphaSide(v: AlphaSide) { alphaSide = v; putS("alphaSide", v.name) }
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
    fun applyUiFont(f: String) { uiFont = f; putS("uiFont", f) }
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
