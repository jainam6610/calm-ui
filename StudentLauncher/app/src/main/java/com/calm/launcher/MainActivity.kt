package com.calm.launcher

import android.app.Activity
import android.app.Dialog
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.Uri
import android.os.*
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.*
import android.view.inputmethod.EditorInfo
import android.webkit.*
import com.studentlauncher.detox.DetoxPolicy
import com.studentlauncher.security.PinLock
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), SensorEventListener {

    // themes: 0 plain white (default, also forced during a session), 1 paper, 2 amber.
    // fonts: 0 sans, 1 mono, 2 serif
    private val themes = listOf(
        intArrayOf(Color.WHITE, Color.parseColor("#111111"), Color.parseColor("#767676"), Color.parseColor("#E4E4E4")),
        intArrayOf(Color.parseColor("#F2F0E9"), Color.parseColor("#141414"), Color.parseColor("#77746C"), Color.parseColor("#D6D3CA")),
        intArrayOf(Color.parseColor("#0A0600"), Color.parseColor("#FFB000"), Color.parseColor("#9A6A00"), Color.parseColor("#3A2800")))
    private val themeNames = listOf("white", "paper", "amber")
    private val fontNames = listOf("sans", "mono", "serif")
    /** A running session always uses the plain white palette, whatever the saved theme is. */
    private val themeIndex get() = if (sessionActive) 0 else prefs.getInt("theme", 0).coerceIn(0, 2)
    private val T get() = themes[themeIndex]
    private val BLACK get() = T[0]
    private val WHITE get() = T[1]
    private val DIM get() = T[2]
    private val LINE get() = T[3]
    private val font get() = prefs.getInt("font", 0)
    private val light: Typeface get() = when (font) { 1 -> Typeface.MONOSPACE; 2 -> Typeface.SERIF; else -> Typeface.create("sans-serif-light", Typeface.NORMAL) }
    private val reg: Typeface get() = when (font) { 1 -> Typeface.MONOSPACE; 2 -> Typeface.SERIF; else -> Typeface.create("sans-serif", Typeface.NORMAL) }
    private val ease = PathInterpolator(0.2f, 0f, 0f, 1f)
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("calm", MODE_PRIVATE) }
    /** The launcher's PIN. Same prefs file, so one PIN guards leaving and Calm. */
    private val lock by lazy { PinLock(getSharedPreferences("student_launcher", MODE_PRIVATE)) }
    private class Entry(val key: String, val dir: Int, val build: () -> View)

    private val slots: List<Slot>
        get() = CalmConfig.slots(gridCount())

    private fun gridCount(): Int = CalmConfig.normalizeCount(prefs.getString(CalmConfig.COUNT_KEY, "9"))

    private lateinit var root: FrameLayout
    private var current: View? = null
    private val stack = ArrayList<Entry>()
    private var timeTv: TextView? = null
    private var dateTv: TextView? = null
    private var npTv: TextView? = null
    private var homeList: ScrollView? = null
    private var homeAtBottom = true
    private var musicTitle: TextView? = null
    private var musicArtist: TextView? = null
    private var musicFill: View? = null
    private var musicPlay: TextView? = null
    private var focusTv: TextView? = null
    private var openedAt = 0L
    private var fdActive = false
    private var fdPending = false
    private var fdStart = 0L
    private var noType = false
    private var resumed = false
    private var browserSince = 0L
    private var web: WebView? = null
    private var urlBar: EditText? = null
    private var progressV: View? = null
    private var pendingUrl: String? = null
    private var pendingLink: String? = null
    private var customView: View? = null
    private var customCb: WebChromeClient.CustomViewCallback? = null
    private val browserTick = object : Runnable { override fun run() { browserCheck(); handler.postDelayed(this, 5000) } }

    // While a Detoxify session runs the countdown ticks every second; otherwise Calm's clock keeps its 3s beat.
    private val tick = object : Runnable {
        override fun run() {
            if (sessionActive && sessionLeftMs() <= 0L) { endSessionNow(); return }
            updateHome()
            handler.postDelayed(this, if (sessionActive) 1000L else 3000L)
        }
    }
    private val musicTick = object : Runnable { override fun run() { updateMusic(); handler.postDelayed(this, 1000) } }
    private val focusTick = object : Runnable {
        override fun run() {
            val s = ((SystemClock.elapsedRealtime() - fdStart) / 1000).toInt()
            focusTv?.text = "%d:%02d".format(s / 60, s % 60); handler.postDelayed(this, 1000)
        }
    }
    private val fdStartRun = Runnable { fdPending = false; startFocus() }

    // ---------- Detoxify session timer (the one addition on top of stock Calm) ----------
    // The launcher hands this activity an absolute deadline; the home clock becomes a countdown and
    // the activity finishes itself when it reaches zero, handing control back to the launcher.
    private var sessionEndWall = 0L
    private var sessionEndMono = 0L
    private var sessionLeftAtStart = 0L
    private var sessionActive = false

    // ---------- helpers ----------
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    private fun dayKey(k: String) = "${k}_${today()}"
    private fun dayInt(k: String) = prefs.getInt(dayKey(k), 0)
    private fun addDay(k: String, n: Int) { prefs.edit().putInt(dayKey(k), dayInt(k) + n).apply() }
    private fun lp(top: Int, w: Int = -2) = LinearLayout.LayoutParams(w, -2).apply { topMargin = dp(top) }
    private fun tv(t: String, sp: Float, c: Int = WHITE, f: Typeface = reg) =
        TextView(this).apply { text = t; textSize = sp; setTextColor(c); typeface = f; includeFontPadding = false }

    // Settings that loosen your rules wait 24 hours; stricter ones apply now
    private fun getS(k: String, d: String): String {
        val at = prefs.getLong("pat_$k", 0)
        if (at != 0L && System.currentTimeMillis() >= at)
            prefs.edit().putString(k, prefs.getString("p_$k", d)).putLong("pat_$k", 0).apply()
        return prefs.getString(k, d)!!
    }
    private fun setS(k: String, v: String, loosening: Boolean) {
        if (loosening) {
            prefs.edit().putString("p_$k", v).putLong("pat_$k", System.currentTimeMillis() + CalmConfig.LOOSEN_WAIT_MS).apply()
            toast("this change applies in 24 hours")
        } else prefs.edit().putString(k, v).putLong("pat_$k", 0).apply()
    }

    private fun log(e: String) {
        val l = prefs.getString("log", "")!!.split("\n").filter { it.isNotBlank() } + "${today().substring(5)} | ${e.replace("\n", " ")}"
        prefs.edit().putString("log", l.takeLast(50).joinToString("\n")).apply()
    }

    private fun press(v: View, onLong: (() -> Unit)?, onClick: () -> Unit) {
        v.setOnTouchListener { x, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> x.animate().alpha(0.45f).scaleX(0.97f).scaleY(0.97f).setDuration(90).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> x.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(180).start()
            }
            false
        }
        v.setOnClickListener { x -> x.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK); onClick() }
        if (onLong != null) v.setOnLongClickListener { x ->
            x.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(120).start()
            onLong(); true
        }
    }

    private fun row(label: String, onLong: (() -> Unit)? = null, onClick: () -> Unit): TextView {
        val t = tv(label, 25f, WHITE, light).apply { setPadding(0, dp(9), 0, dp(9)); pivotX = 0f }
        press(t, onLong, onClick); return t
    }

    private fun btn(label: String, filled: Boolean, onClick: () -> Unit): TextView {
        val t = tv(label, 16f, if (filled) BLACK else WHITE).apply {
            gravity = Gravity.CENTER; setPadding(dp(26), dp(13), dp(26), dp(13))
            background = GradientDrawable().apply {
                cornerRadius = dp(40).toFloat()
                if (filled) setColor(WHITE) else { setColor(Color.TRANSPARENT); setStroke(dp(1), WHITE) }
            }
        }
        press(t, null, onClick); return t
    }

    // ---------- owned dialog: no AlertDialog, no One UI chrome ----------

    private class CalmDialogHandle(
        val dialog: Dialog,
        val title: TextView?,
        val positive: TextView?,
        val negative: TextView?,
        val neutral: TextView?
    )

    private fun calmInput(hint: String, numeric: Boolean = false, password: Boolean = false): EditText =
        EditText(this).apply {
            this.hint = hint
            setTextColor(WHITE); setHintTextColor(DIM)
            typeface = reg; textSize = 15f; setSingleLine()
            background = GradientDrawable().apply { setColor(LINE); cornerRadius = dp(24).toFloat() }
            setPadding(dp(18), dp(12), dp(18), dp(12))
            inputType = when {
                numeric && password -> InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
                numeric -> InputType.TYPE_CLASS_NUMBER
                else -> InputType.TYPE_CLASS_TEXT
            }
        }

    private fun calmDialog(
        title: String? = null,
        message: String? = null,
        bodyView: View? = null,
        items: List<String>? = null,
        onItem: ((Int) -> Unit)? = null,
        positive: String? = null,
        onPositive: (() -> Unit)? = null,
        negative: String? = null,
        onNegative: (() -> Unit)? = null,
        neutral: String? = null,
        onNeutral: (() -> Unit)? = null,
        dismissOnPositive: Boolean = true,
        dismissOnNegative: Boolean = true,
        cancelable: Boolean = true
    ): CalmDialogHandle {
        val d = Dialog(this)
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(12))
            background = GradientDrawable().apply {
                setColor(BLACK)
                cornerRadius = dp(28).toFloat()
                setStroke(dp(1), LINE)
            }
        }
        val titleTv = title?.let {
            tv(it, 20f, WHITE, light).apply { setPadding(dp(4), 0, dp(4), 6) }
                .also { t -> col.addView(t) }
        }
        message?.let {
            col.addView(tv(it, 13f, DIM).apply { setPadding(dp(4), 0, dp(4), dp(10)) })
        }
        bodyView?.let {
            col.addView(it, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(8) })
        }
        items?.forEachIndexed { i, label ->
            val r = tv(label, 16f, WHITE).apply { setPadding(dp(4), dp(15), dp(4), dp(15)) }
            press(r, null) { d.dismiss(); onItem?.invoke(i) }
            col.addView(r, LinearLayout.LayoutParams(-1, -2))
        }
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            setPadding(0, dp(10), 0, 0)
        }
        val neutralTv = neutral?.let { label ->
            val v = tv(label, 14f, DIM).apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
            press(v, null) { d.dismiss(); onNeutral?.invoke() }
            bar.addView(v); v
        }
        val negativeTv = negative?.let { label ->
            val v = tv(label, 14f, DIM).apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
            press(v, null) { if (dismissOnNegative) d.dismiss(); onNegative?.invoke() }
            bar.addView(v); v
        }
        val positiveTv = positive?.let { label ->
            val v = tv(label, 14f, WHITE, light).apply { setPadding(dp(12), dp(12), dp(12), dp(12)) }
            press(v, null) { if (dismissOnPositive) { d.dismiss(); onPositive?.invoke() } }
            bar.addView(v); v
        }
        if (bar.childCount > 0) col.addView(bar, LinearLayout.LayoutParams(-1, -2))
        d.setContentView(col)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        d.window?.setLayout((resources.displayMetrics.widthPixels * 0.88f).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        d.setCanceledOnTouchOutside(cancelable)
        d.show()
        return CalmDialogHandle(d, titleTv, positiveTv, negativeTv, neutralTv)
    }

    private fun column(title: String? = null): LinearLayout {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BLACK); setPadding(dp(28), dp(56), dp(28), dp(28)) }
        if (title != null) {
            val h = tv("←  $title", 15f, DIM).apply { setPadding(0, 0, dp(24), dp(12)) }
            press(h, null) { pop() }; c.addView(h)
        }
        return c
    }

    private fun infoRow(label: String, value: String): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, dp(10)) }
        r.addView(tv(label, 17f, WHITE, light), LinearLayout.LayoutParams(0, -2, 1f)); r.addView(tv(value, 14f, DIM))
        return r
    }

    private fun safe(i: Intent) { try { startActivity(i) } catch (e: Exception) { toast("not available on this phone") } }

    // ---------- navigation and animation ----------
    private fun cancelLive() {
        if (stack.lastOrNull()?.key == "browser") closeBrowser()
        handler.removeCallbacks(musicTick); handler.removeCallbacks(focusTick)
    }

    private fun present(key: String, dir: Int, build: () -> View) {
        cancelLive(); stack.add(Entry(key, dir, build)); transition(build(), dir, true)
    }

    private fun pop(): Boolean {
        if (stack.size <= 1) return false
        cancelLive(); val e = stack.removeAt(stack.size - 1)
        transition(stack.last().build(), e.dir, false); return true
    }

    private fun goHome() {
        cancelLive(); stack.clear()
        val e = Entry("home", 0) { homeView() }; stack.add(e); transition(e.build(), 0, true)
    }

    private fun rebuildCurrent() {
        val e = stack.lastOrNull() ?: return
        val old = current; noType = true; val nv = e.build(); noType = false
        root.addView(nv, FrameLayout.LayoutParams(-1, -1)); current = nv
        if (old != null) root.removeView(old)
    }

    // dir: 0 fade, 1 slide from right, 2 slide from bottom
    private fun transition(nv: View, dir: Int, enter: Boolean) {
        val old = current; current = nv
        root.addView(nv, FrameLayout.LayoutParams(-1, -1))
        if (prefs.getBoolean("eink", false)) { if (old != null) root.removeView(old); flash(); return }
        val ex = if (dir == 1) root.width * 0.22f else 0f
        val ey = if (dir == 2) root.height * 0.10f else 0f
        val sg = if (enter) 1f else -1f
        nv.alpha = 0f; nv.translationX = sg * ex; nv.translationY = sg * ey
        nv.animate().alpha(1f).translationX(0f).translationY(0f).setDuration(300).setInterpolator(ease).start()
        if (old != null) {
            old.animate().alpha(0f).translationX(-sg * ex * 0.4f).translationY(-sg * ey * 0.5f)
                .setDuration(240).withEndAction { root.removeView(old) }.start()
        }
        staggerIn(nv)
    }

    private fun staggerIn(v: View) {
        val g = v.findViewWithTag<View>("stagger") as? ViewGroup ?: return
        for (i in 0 until g.childCount) {
            val c = g.getChildAt(i); c.alpha = 0f; c.translationY = dp(18).toFloat()
            c.animate().alpha(1f).translationY(0f).setStartDelay(60L + 45L * i).setDuration(340).setInterpolator(ease).start()
        }
    }

    private val gd by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                homeAtBottom = homeList?.canScrollVertically(1) != true
                return false
            }
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                val k = stack.lastOrNull()?.key
                if (Math.abs(vy) > Math.abs(vx) * 1.5f) {
                    // A long 15-app list scrolls first; tools only open when it was already at the bottom.
                    if (vy < -1200f && k == "home" && homeAtBottom) present("tools", 2) { toolsView() }
                    else if (vy > 1200f && k == "tools") pop()
                }
                return false
            }
        })
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean { gd.onTouchEvent(ev); return super.dispatchTouchEvent(ev) }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val k = stack.lastOrNull()?.key ?: return
        if (customView != null) exitFull()
        else if (k == "browser") { if (wv.canGoBack()) wv.goBack() else pop() }
        else if (k.startsWith("ob")) { val n = k.substring(2).toInt(); if (n > 0) obGo(n - 1) } else pop()
    }

    // ---------- lifecycle ----------
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        setContentView(root)
        val hadSession = intent?.hasExtra(EXTRA_END_WALL) == true
        readSessionTimer()
        applyTheme()
        if (hadSession && !sessionActive) { endSessionNow(); return }
        root.post {
            if (prefs.getBoolean("onboarded", false)) { goHome(); intent?.dataString?.let { openLink(it) } }
            else { pendingLink = intent?.dataString; obGo(0) }
        }
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i)
        if (i.hasExtra(EXTRA_END_WALL)) {
            readSessionTimer()
            applyTheme()
            if (!sessionActive) { endSessionNow(); return }
            if (prefs.getBoolean("onboarded", false)) goHome()
            return
        }
        if (i.action == Intent.ACTION_MAIN && i.hasCategory(Intent.CATEGORY_HOME)) goHome()
        else i.dataString?.let { if (prefs.getBoolean("onboarded", false)) openLink(it) else pendingLink = it }
    }

    override fun onResume() {
        super.onResume()
        if (sessionActive && sessionLeftMs() <= 0L) { endSessionNow(); return }
        if (openedAt != 0L) {
            addDay("sec", ((SystemClock.elapsedRealtime() - openedAt) / 1000).toInt().coerceIn(0, 7200)); openedAt = 0L
        }
        resumed = true
        if (stack.lastOrNull()?.key == "browser" && browserSince == 0L) browserSince = SystemClock.elapsedRealtime()
        handler.post(tick)
        if (stack.lastOrNull()?.key == "music") handler.post(musicTick)
        if (stack.lastOrNull()?.key == "browser") handler.postDelayed(browserTick, 5000)
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onPause() {
        super.onPause()
        persistSessionTimer()
        handler.removeCallbacks(tick); handler.removeCallbacks(fdStartRun); fdPending = false; resumed = false; accrueBrowser()
        handler.removeCallbacks(musicTick); handler.removeCallbacks(browserTick)
        (getSystemService(SENSOR_SERVICE) as SensorManager).unregisterListener(this)
        if (fdActive) endFocus()
    }

    // ---------- face-down focus ----------
    override fun onSensorChanged(e: SensorEvent) {
        val down = e.values[2] < -7.5f
        if (down && !fdActive && !fdPending && stack.lastOrNull()?.key == "home") { fdPending = true; handler.postDelayed(fdStartRun, 2500) }
        if (!down) {
            if (fdPending) { handler.removeCallbacks(fdStartRun); fdPending = false }
            if (fdActive) endFocus()
        }
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}

    private fun setBrightness(b: Float) { val a = window.attributes; a.screenBrightness = b; window.attributes = a }

    private fun startFocus() {
        fdActive = true; fdStart = SystemClock.elapsedRealtime()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); setBrightness(0.02f)
        present("focus", 0) { focusView() }
    }

    private fun endFocus() {
        val s = ((SystemClock.elapsedRealtime() - fdStart) / 1000).toInt()
        fdActive = false; addDay("fdsec", s)
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); setBrightness(WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE)
        if (stack.lastOrNull()?.key == "focus") pop()
    }

    private fun focusView(): View {
        val col = column().apply { gravity = Gravity.CENTER }
        col.addView(tv("face-down focus", 14f, DIM))
        focusTv = tv("0:00", 64f, WHITE, light); col.addView(focusTv, lp(8))
        col.addView(tv("lift the phone to end", 13f, DIM), lp(16))
        handler.post(focusTick); return col
    }

    // ---------- rules: limit, taper, quiet hours ----------
    private fun taperDay(): Int {
        var t = prefs.getLong("taper_start", 0)
        if (t == 0L) { t = System.currentTimeMillis(); prefs.edit().putLong("taper_start", t).apply() }
        return ((System.currentTimeMillis() - t) / 86_400_000L).toInt()
    }
    private fun limitNow(): Int {
        val base = getS("limit", "60").toInt(); val wk = minOf(taperDay() / 7, 2)
        return maxOf(20, (base * (1 - 0.1 * wk)).toInt()) + dayInt("extra")
    }
    private fun usedMin() = dayInt("sec") / 60
    private fun isQuiet(): Boolean {
        val s = getS("qs", "22").toInt(); val e = getS("qe", "7").toInt()
        if (s == e) return false
        val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        return if (s < e) h in s until e else h >= s || h < e
    }

    // ---------- apps ----------
    private fun installed(): List<Pair<String, String>> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(i, 0)
            .map { Pair(it.loadLabel(packageManager).toString().lowercase(), it.activityInfo.packageName) }
            .filter { it.second != packageName }.distinctBy { it.second }.sortedBy { it.first }
    }
    private fun appLabel(pkg: String): String = try {
        packageManager.getApplicationInfo(pkg, 0).loadLabel(packageManager).toString().lowercase()
    } catch (e: Exception) { pkg }

    private fun pkgFor(s: Slot): String? {
        val chosen = getS("slot_${s.id}", "")
        if (chosen.isNotEmpty() && packageManager.getLaunchIntentForPackage(chosen) != null) return chosen
        return s.pkgs.firstOrNull { packageManager.getLaunchIntentForPackage(it) != null }
    }

    private fun fallbackIntent(s: Slot): Intent? = when (s.id) {
        "messages" -> Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MESSAGING)
        "clock" -> Intent(AlarmClock.ACTION_SHOW_ALARMS)
        "browser" -> Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
        else -> null
    }

    private fun launch(s: Slot) {
        if (s.counts && isQuiet()) present("quiet", 0) { blockView("quiet hours", "back at ${getS("qe", "7")}:00") }
        else if (s.counts && usedMin() >= limitNow()) present("limit", 0) { limitView() }
        else if (s.friction) present("pause", 0) { pauseView(s) }
        else openSlot(s)
    }

    private fun openSlot(s: Slot) {
        if (s.id == "browser") { openBrowser(null); return }
        if (s.id == "phone") { safe(Intent(Intent.ACTION_DIAL)); return }
        if (s.id == "camera") { safe(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)); return }
        val pkg = pkgFor(s)
        val i = if (pkg != null) packageManager.getLaunchIntentForPackage(pkg) else fallbackIntent(s)
        if (i == null) { toast("no app for ${s.label}. settings, then apps"); return }
        if (s.counts) openedAt = SystemClock.elapsedRealtime()
        safe(i)
    }

    // ---------- built-in browser ----------
    private val bBlocked = setOf("instagram.com", "facebook.com", "twitter.com", "x.com", "tiktok.com", "snapchat.com",
        "reddit.com", "youtube.com", "youtu.be", "twitch.tv", "pinterest.com", "9gag.com", "netflix.com", "threads.net",
        "translate.goog", "translate.google.com", "webcache.googleusercontent.com")
    private val adHosts = setOf("doubleclick.net", "googlesyndication.com", "googleadservices.com", "googletagservices.com",
        "adservice.google.com", "google-analytics.com", "adnxs.com", "taboola.com", "outbrain.com", "criteo.com", "criteo.net",
        "pubmatic.com", "rubiconproject.com", "openx.net", "adsrvr.org", "amazon-adsystem.com", "moatads.com", "scorecardresearch.com",
        "quantserve.com", "hotjar.com", "mc.yandex.ru", "ads-twitter.com", "popads.net", "propellerads.com", "exoclick.com",
        "juicyads.com", "adsterra.com", "mgid.com", "revcontent.com", "zedo.com", "smartadserver.com", "casalemedia.com",
        "adform.net", "bidswitch.net", "contextweb.com", "yieldmo.com", "teads.tv", "inmobi.com", "applovin.com",
        "advertising.com", "adcolony.com", "2mdn.net", "serving-sys.com", "adroll.com", "media.net", "tapad.com", "3lift.com",
        "sharethrough.com", "lijit.com", "sovrn.com", "onetag-sys.com", "adition.com", "smaato.net")
    private val adultHosts = setOf("pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com", "youporn.com",
        "spankbang.com", "chaturbate.com", "stripchat.com", "bongacams.com", "livejasmin.com", "onlyfans.com", "brazzers.com",
        "rule34.xxx", "erome.com", "fapello.com", "eporner.com", "camsoda.com", "cam4.com", "tnaflix.com", "motherless.com",
        "nhentai.net", "e-hentai.org", "sex.com", "porn.com", "thumbzilla.com", "beeg.com", "tube8.com", "porntrex.com",
        "hqporner.com", "fansly.com")
    private val adultWords = listOf("porn", "xxx", "hentai", "xvideo", "xnxx", "xhamster", "sexcam", "camgirl", "nsfw")

    private val wv: WebView get() { if (web == null) web = makeWeb(); return web!! }
    private fun flag(k: String, d: Boolean) = prefs.getBoolean(k, d)
    private fun hex(c: Int) = String.format("#%06X", c and 0xFFFFFF)

    private val bChrome = object : WebChromeClient() {
        override fun onProgressChanged(v: WebView, p: Int) {
            val b = progressV ?: return
            if (p < 100) { b.alpha = 1f; b.animate().scaleX(p / 100f).setDuration(180).start() }
            else b.animate().scaleX(1f).setDuration(120).withEndAction { b.animate().alpha(0f).setDuration(250).withEndAction { b.scaleX = 0f }.start() }.start()
        }
        override fun onShowCustomView(v: View, cb: CustomViewCallback) { customView = v; customCb = cb; root.addView(v, FrameLayout.LayoutParams(-1, -1)) }
        override fun onHideCustomView() { exitFull() }
    }

    private val bClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(v: WebView, r: WebResourceRequest): Boolean {
            val u = r.url
            if (u.scheme == "calm") { if (u.host == "go") bGo(u.getQueryParameter("q") ?: ""); return true }
            if (u.scheme != "http" && u.scheme != "https") return true
            if (isAdult(u)) { bBlock("adult content is blocked"); return true }
            if (bIsBlocked(u)) { bBlock("this site is off"); return true }
            safeRewrite(u)?.let { v.loadUrl(it.toString()); return true }
            return false
        }
        override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest): WebResourceResponse? {
            val u = r.url
            if (isAdult(u)) { if (r.isForMainFrame) handler.post { bBlock("adult content is blocked") }; return empty() }
            if (isAd(u)) return empty()
            if (bIsBlocked(u)) { if (r.isForMainFrame) handler.post { bBlock("this site is off") }; return empty() }
            return null
        }
        override fun onReceivedError(v: WebView, r: WebResourceRequest, e: WebResourceError) {
            if (r.isForMainFrame) bPage("no internet", listOf("turn your phone away", "find a spot to reflect", "breathe"))
        }
        override fun doUpdateVisitedHistory(v: WebView, url: String, isReload: Boolean) {
            urlBar?.let { if (!it.hasFocus()) it.setText(if (url.startsWith("http")) url else "") }
        }
        override fun onPageFinished(v: WebView, url: String) { v.evaluateJavascript(injectJs(), null) }
    }

    private fun makeWeb(): WebView {
        val w = WebView(this)
        w.settings.apply {
            javaScriptEnabled = true; domStorageEnabled = true; mediaPlaybackRequiresUserGesture = true
            useWideViewPort = true; setSupportZoom(true); builtInZoomControls = true; displayZoomControls = false
        }
        applyUA(w)
        CookieManager.getInstance().setAcceptThirdPartyCookies(w, true)
        w.webChromeClient = bChrome; w.webViewClient = bClient
        w.setDownloadListener { _, _, _, _, _ -> toast("downloads aren't supported") }
        return w
    }

    private fun applyUA(w: WebView) {
        val desk = flag("desktop", false)
        w.settings.userAgentString = if (desk) "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            else WebSettings.getDefaultUserAgent(this).replace("; wv", "").replace("Version/4.0 ", "")
        w.settings.loadWithOverviewMode = desk
    }

    private fun exitFull() {
        customView?.let { root.removeView(it) }; customView = null
        customCb?.onCustomViewHidden(); customCb = null
    }

    private fun inSet(h: String, set: Set<String>): Boolean {
        var x = h
        while (true) { if (x in set) return true; val i = x.indexOf('.'); if (i < 0) return false; x = x.substring(i + 1) }
    }
    private fun empty() = WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    private fun adultAllowed(): Boolean { val at = prefs.getLong("adult_off_at", 0); return at != 0L && System.currentTimeMillis() >= at }
    private fun isAd(u: Uri): Boolean {
        val h = (u.host ?: return false).lowercase()
        return flag("ads", true) && inSet(h, adHosts)
    }
    private fun isAdult(u: Uri): Boolean {
        if (adultAllowed()) return false
        val h = (u.host ?: return false).lowercase()
        return inSet(h, adultHosts) || adultWords.any { h.contains(it) }
    }
    private fun bIsBlocked(u: Uri): Boolean {
        val h = (u.host ?: return false).lowercase()
        return inSet(h, bBlocked) || Regex("^[0-9.:]+$").matches(h)
    }
    private fun safeRewrite(u: Uri): Uri? {
        if (adultAllowed()) return null
        val h = u.host ?: return null
        if (h.contains("google.") && u.path == "/search" && u.getQueryParameter("safe") == null)
            return u.buildUpon().appendQueryParameter("safe", "active").build()
        return null
    }

    private fun injectJs(): String {
        val css = StringBuilder()
        if (flag("gray", true)) css.append("html{filter:grayscale(1)!important}")
        if (flag("ads", true)) css.append("ins.adsbygoogle,[id^=\"google_ads\"],[id*=\"ad-container\"],[class*=\"ad-banner\"],[data-ad-slot],iframe[src*=\"doubleclick\"],iframe[src*=\"googlesyndication\"]{display:none!important}")
        css.append("#onetrust-banner-sdk,.fc-consent-root,[id*=\"cookie-banner\"],[class*=\"cookie-banner\"],[class*=\"consent-banner\"]{display:none!important}")
        return "(function(){if(window.__c)return;window.__c=1;var s=document.createElement('style');s.textContent=${JSONObject.quote(css.toString())};(document.head||document.documentElement).appendChild(s);" +
            "var t=false;addEventListener('touchstart',function(){t=true;setTimeout(function(){t=false},1500)},true);addEventListener('play',function(e){if(!t&&e.target&&e.target.pause)e.target.pause()},true);})();"
    }

    private fun bToUrl(t: String) = when {
        t.startsWith("http://") || t.startsWith("https://") -> t
        t.contains(" ") || !t.contains(".") -> "https://www.google.com/search?q=${Uri.encode(t)}&safe=active"
        else -> "https://$t"
    }

    private fun bGo(input: String) {
        val t = input.trim(); if (t.isEmpty()) return
        val url = bToUrl(t); val u = Uri.parse(url)
        if (isAdult(u)) { bBlock("adult content is blocked"); return }
        if (bIsBlocked(u)) { bBlock("this site is off"); return }
        urlBar?.clearFocus(); wv.loadUrl(url)
    }

    private fun bBlock(msg: String) {
        wv.stopLoading(); wv.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS); addDay("blocks", 1)
        bPage("not now", listOf(msg, "go back to what you came for", "breathe"))
    }

    private val bCss get() = "body{margin:0;min-height:100vh;background:${hex(BLACK)};color:${hex(WHITE)};font-family:sans-serif}" +
        ".c{padding:14vh 9vw 0}h1{font-weight:300;font-size:34px;margin:0 0 18px}p,li{color:${hex(DIM)};font-size:16px;margin:12px 0}ul{padding-left:18px}"

    private fun bPage(title: String, lines: List<String>) = wv.loadDataWithBaseURL(null,
        "<html><head><meta name=viewport content='width=device-width,initial-scale=1'><style>$bCss</style></head><body><div class=c><h1>$title</h1><ul>" +
            lines.joinToString("") { "<li>${TextUtils.htmlEncode(it)}</li>" } + "</ul></div></body></html>", "text/html", "utf-8", null)

    private fun bStart() = wv.loadDataWithBaseURL(null,
        """<html><head><meta name=viewport content='width=device-width,initial-scale=1'><style>$bCss
        input{width:100%;box-sizing:border-box;background:${hex(LINE)};color:${hex(WHITE)};border:0;border-radius:32px;padding:20px 24px;font:300 20px sans-serif;margin-top:8px;outline:none}
        </style></head><body><div class=c><h1>search</h1>
        <form action='calm://go' method='get'><input name=q placeholder='ask google' autocomplete=off autofocus></form>
        <p style='margin-top:28px'>one tab. no feeds. no social.</p></div></body></html>""", "text/html", "utf-8", null)

    private fun bMenu() {
        val desk = flag("desktop", false); val at = prefs.getLong("adult_off_at", 0); val now = System.currentTimeMillis()
        val adult = when { at == 0L -> "adult sites: blocked"; at > now -> "adult block turning off (tap to cancel)"; else -> "adult sites: allowed (tap to block)" }
        val items = listOf("desktop site: ${if (desk) "on" else "off"}", "ad blocker: ${if (flag("ads", true)) "on" else "off"}",
            "grayscale: ${if (flag("gray", true)) "on" else "off"}", adult, "reader view", "close browser")
        calmDialog(items = items, onItem = { i ->
            when (i) {
                0 -> { prefs.edit().putBoolean("desktop", !desk).apply(); applyUA(wv); wv.reload() }
                1 -> { prefs.edit().putBoolean("ads", !flag("ads", true)).apply(); wv.reload() }
                2 -> { prefs.edit().putBoolean("gray", !flag("gray", true)).apply(); wv.reload() }
                3 -> if (at == 0L) { prefs.edit().putLong("adult_off_at", now + 24 * 3600_000L).apply(); toast("adult block turns off in 24 hours. tap again to cancel.") }
                     else { prefs.edit().putLong("adult_off_at", 0).apply(); toast("adult block is on") }
                4 -> wv.evaluateJavascript("(function(){var a=document.querySelector('article')||document.querySelector('main');if(a)document.body.innerHTML='<div style=\"max-width:680px;margin:auto;padding:20px;font:18px/1.7 Georgia;background:#fff;color:#111\">'+a.innerHTML+'</div>'})()", null)
                5 -> pop()
            }
        })
    }

    private fun browserView(): View {
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BLACK) }
        val bar0 = EditText(this).apply {
            hint = "search google"; setSingleLine(); setTextColor(WHITE); setHintTextColor(DIM); typeface = reg; textSize = 15f
            background = GradientDrawable().apply { setColor(LINE); cornerRadius = dp(30).toFloat() }
            setPadding(dp(20), dp(13), dp(20), dp(13)); setSelectAllOnFocus(true); imeOptions = EditorInfo.IME_ACTION_GO
            setOnEditorActionListener { v, _, _ -> bGo(v.text.toString()); true }
        }
        urlBar = bar0
        col.addView(bar0, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(14), dp(44), dp(14), dp(6)) })
        val prog = View(this).apply { setBackgroundColor(WHITE); pivotX = 0f; scaleX = 0f; alpha = 0f }
        progressV = prog; col.addView(prog, LinearLayout.LayoutParams(-1, dp(2)))
        val w = wv; (w.parent as? ViewGroup)?.removeView(w); w.setBackgroundColor(BLACK)
        col.addView(w, LinearLayout.LayoutParams(-1, 0, 1f))
        val bar = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        fun item(t: String, a: () -> Unit) {
            val v = tv(t, 15f, WHITE, light).apply { gravity = Gravity.CENTER; setPadding(0, dp(15), 0, dp(15)) }
            press(v, null, a); bar.addView(v, LinearLayout.LayoutParams(0, -2, 1f))
        }
        item("back") { if (w.canGoBack()) w.goBack() }
        item("forward") { if (w.canGoForward()) w.goForward() }
        item("home") { bStart() }
        item("menu") { bMenu() }
        col.addView(bar, LinearLayout.LayoutParams(-1, -2))
        if (browserSince == 0L) browserSince = SystemClock.elapsedRealtime()
        handler.removeCallbacks(browserTick); handler.postDelayed(browserTick, 5000)
        val u = pendingUrl; pendingUrl = null
        if (u != null) bGo(u) else if (w.url == null || w.url == "about:blank") bStart()
        return col
    }

    private fun openBrowser(u: String?) { pendingUrl = u; present("browser", 2) { browserView() } }

    // links from other apps: same limits as tapping the browser
    private fun openLink(u: String) {
        if (stack.lastOrNull()?.key == "browser") { bGo(u); return }
        if (isQuiet()) present("quiet", 0) { blockView("quiet hours", "back at ${getS("qe", "7")}:00") }
        else if (usedMin() >= limitNow()) present("limit", 0) { limitView() }
        else openBrowser(u)
    }

    private fun accrueBrowser() {
        if (browserSince != 0L) { addDay("sec", ((SystemClock.elapsedRealtime() - browserSince) / 1000).toInt().coerceIn(0, 7200)); browserSince = 0L }
    }

    private fun closeBrowser() {
        accrueBrowser(); handler.removeCallbacks(browserTick)
        web?.let { (it.parent as? ViewGroup)?.removeView(it); it.loadUrl("about:blank"); it.clearHistory() }
        if (customView != null) exitFull()
    }

    private fun browserCheck() {
        if (!resumed || stack.lastOrNull()?.key != "browser") return
        accrueBrowser(); browserSince = SystemClock.elapsedRealtime()
        if (isQuiet()) { pop(); present("quiet", 0) { blockView("quiet hours", "back at ${getS("qe", "7")}:00") } }
        else if (usedMin() >= limitNow()) { pop(); present("limit", 0) { limitView() } }
    }

    private fun slotValue(s: Slot): String = if (s.id == "browser") "built in" else (pkgFor(s)?.let { appLabel(it) } ?: "choose")

    // ---------- weekly reflection ----------
    private fun dayAgo(k: String, n: Int) =
        prefs.getInt("${k}_${SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - n * 86_400_000L))}", 0)

    private fun reflectView(): View {
        val col = column("reflect")
        val sv = ScrollView(this); val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val lim = limitNow().coerceAtLeast(1)
        var week = 0; var prev = 0
        for (n in 0..13) { val m = dayAgo("sec", n) / 60; if (n < 7) week += m else prev += m }
        inner.addView(tv("last 7 days", 12f, DIM), lp(14))
        inner.addView(tv("$week min", 34f, WHITE, light), lp(4))
        val diff = week - prev
        inner.addView(tv(if (prev == 0) "your first week" else if (diff <= 0) "${-diff} min less than the week before" else "$diff min more than the week before", 13f, DIM), lp(4))
        val fmt = SimpleDateFormat("EEE", Locale.getDefault())
        for (n in 6 downTo 0) {
            val m = dayAgo("sec", n) / 60
            val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            r.addView(tv(fmt.format(Date(System.currentTimeMillis() - n * 86_400_000L)).lowercase(), 13f, DIM), LinearLayout.LayoutParams(dp(44), -2))
            val box = FrameLayout(this).apply { setBackgroundColor(LINE) }
            val fill = View(this).apply { setBackgroundColor(WHITE); pivotX = 0f; scaleX = 0f }
            box.addView(fill, FrameLayout.LayoutParams(-1, -1))
            r.addView(box, LinearLayout.LayoutParams(0, dp(2), 1f))
            r.addView(tv("$m", 13f, DIM).apply { gravity = Gravity.END }, LinearLayout.LayoutParams(dp(40), -2))
            inner.addView(r, lp(16, -1))
            fill.animate().scaleX((m.toFloat() / lim).coerceIn(0f, 1f)).setStartDelay(200L + 70L * (6 - n)).setDuration(700).setInterpolator(ease).start()
        }
        var fd = 0; var ints = 0
        for (n in 0..6) { fd += dayAgo("fdsec", n) / 60; ints += dayAgo("intents", n) }
        inner.addView(infoRow("phone-free", "$fd min"), lp(22, -1))
        inner.addView(infoRow("intentions set", "$ints"))
        inner.addView(tv("what did you use your phone for that mattered?", 16f, WHITE, light), lp(26))
        val ans = EditText(this).apply { hint = "one line"; setHintTextColor(DIM); setTextColor(WHITE); typeface = reg; textSize = 16f; background = null; setSingleLine() }
        inner.addView(ans, lp(8, -1)); inner.addView(View(this).apply { setBackgroundColor(LINE) }, LinearLayout.LayoutParams(-1, dp(1)))
        inner.addView(btn("save", false) { if (ans.text.isNotBlank()) { log("reflect: ${ans.text}"); toast("saved"); ans.setText("") } }, lp(16, -1))
        sv.addView(inner); col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f)); return col
    }

    // ---------- look, retro touches ----------
    private fun applyTheme() {
        window.statusBarColor = BLACK; window.navigationBarColor = BLACK
        root.setBackgroundColor(BLACK)
        // Themes 0 and 1 are light canvases; only amber is dark.
        val lightCanvas = themeIndex != 2
        window.decorView.systemUiVisibility =
            if (lightCanvas) View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR else 0
    }

    // e-ink style refresh: the screen flashes once instead of sliding
    private fun flash() {
        val f = View(this).apply { setBackgroundColor(Color.WHITE) }
        root.addView(f, FrameLayout.LayoutParams(-1, -1))
        f.animate().setStartDelay(50).alpha(0f).setDuration(160).withEndAction { root.removeView(f) }.start()
    }

    private fun typeText(t: TextView, full: String, step: Long = 38L) {
        t.text = ""
        for (i in 1..full.length) handler.postDelayed({ t.text = full.substring(0, i) }, step * i)
    }

    private fun pillRow(labels: List<String>, sel: Int, onSel: (Int) -> Unit): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        labels.forEachIndexed { i, l ->
            val t = tv(l, 14f, if (i == sel) BLACK else WHITE).apply {
                gravity = Gravity.CENTER; setPadding(dp(18), dp(10), dp(18), dp(10))
                background = GradientDrawable().apply {
                    cornerRadius = dp(40).toFloat()
                    if (i == sel) setColor(WHITE) else { setColor(Color.TRANSPARENT); setStroke(dp(1), WHITE) }
                }
            }
            press(t, null) { onSel(i) }
            r.addView(t, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = dp(8) })
        }
        return r
    }

    // ---------- onboarding ----------
    private fun obGo(step: Int) {
        cancelLive(); stack.clear()
        val e = Entry("ob$step", 1) { onboardView(step) }; stack.add(e); transition(e.build(), 1, true)
    }

    private fun finishOnboarding() {
        prefs.edit().putBoolean("onboarded", true).putLong("taper_start", System.currentTimeMillis()).apply(); goHome()
        pendingLink?.let { openLink(it); pendingLink = null }
    }

    private fun onboardView(step: Int): View {
        val col = column()
        col.addView(tv("${step + 1} / 6", 12f, DIM))
        val title = tv("", 32f, WHITE, light); col.addView(title, lp(18))
        val body = tv("", 15f, DIM); col.addView(body, lp(10))
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stagger" }
        var t = ""; var b = ""
        when (step) {
            0 -> {
                t = "calm"; b = "a quieter phone. ${gridCount()} apps, no feeds. first, how should it look?"
                content.addView(tv("theme", 12f, DIM), lp(26))
                content.addView(pillRow(themeNames, prefs.getInt("theme", 0)) { prefs.edit().putInt("theme", it).apply(); applyTheme(); rebuildCurrent() }, lp(8))
                content.addView(tv("typeface", 12f, DIM), lp(22))
                content.addView(pillRow(fontNames, prefs.getInt("font", 0)) { prefs.edit().putInt("font", it).apply(); rebuildCurrent() }, lp(8))
                content.addView(tv("paper is a light mode. amber is a retro terminal.", 12f, DIM), lp(18))
            }
            1 -> {
                t = "your apps"; b = "tap one to choose which app opens. hold a row to change it later. phone and camera are always there."
                content.addView(settingsRow("apps on home", "${gridCount()}") { chooseGridCount() })
                slots.forEach { s ->
                    val v = if (s.id == "phone" || s.id == "camera") "always" else slotValue(s)
                    content.addView(settingsRow(s.label, v) { chooseApp(s, setup = true) })
                }
            }
            2 -> {
                t = "your limits"; b = "minutes a day for whatsapp and the browser. it shrinks a little each week."
                val lims = listOf(30, 45, 60, 90)
                content.addView(tv("daily minutes", 12f, DIM), lp(24))
                content.addView(pillRow(lims.map { "${it}m" }, lims.indexOf(getS("limit", "60").toInt()).coerceAtLeast(0)) { setS("limit", lims[it].toString(), false); rebuildCurrent() }, lp(8))
                val qs = listOf(Pair(22, 7), Pair(23, 6), Pair(0, 0))
                val curQ = qs.indexOfFirst { it.first == getS("qs", "22").toInt() && it.second == getS("qe", "7").toInt() }.coerceAtLeast(0)
                content.addView(tv("quiet hours", 12f, DIM), lp(22))
                content.addView(pillRow(listOf("22 to 7", "23 to 6", "off"), curQ) { setS("qs", qs[it].first.toString(), false); setS("qe", qs[it].second.toString(), false); rebuildCurrent() }, lp(8))
            }
            3 -> {
                t = "the browser"; b = "built in. google search only. no social, no video feeds, no ads. adult sites blocked."
                content.addView(tv("the browser opens from its row on the home screen.", 12f, DIM), lp(24))
            }
            4 -> {
                t = "permissions"; b = "optional. the digest and the music screen need notification access."
                content.addView(settingsRow("notification access", if (listenerEnabled()) "on" else "allow") { safe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, lp(18, -1))
            }
            else -> { t = "you're set"; b = "phone and camera sit at the end. swipe up for tools. ${gridCount()} apps, that's all." }
        }
        if (noType) { title.text = t; body.text = b }
        else { typeText(title, t); body.text = b; body.alpha = 0f; body.animate().alpha(1f).setStartDelay(40L * t.length + 100).setDuration(400).start() }
        val sv = ScrollView(this); sv.addView(content)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        if (step > 0) { val back = tv("back", 15f, DIM).apply { setPadding(0, dp(12), dp(24), dp(12)) }; press(back, null) { obGo(step - 1) }; nav.addView(back) }
        nav.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        nav.addView(btn(if (step == 5) "start" else "next", true) { if (step == 5) finishOnboarding() else obGo(step + 1) })
        col.addView(nav, lp(10, -1))
        return col
    }

    // ---------- home ----------
    private fun homeView(): View {
        val col = column()
        timeTv = tv("", 72f, WHITE, light); dateTv = tv("", 15f, DIM)
        col.addView(timeTv, lp(10)); col.addView(dateTv, lp(6))
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stagger" }
        slots.forEach { s ->
            val fixed = s.id == "phone" || s.id == "camera" || s.id == "browser"
            val chosen = getS("slot_${s.id}", "")
            val label = when {
                s.id.startsWith("extra") -> pkgFor(s)?.let { appLabel(it) } ?: "choose"
                fixed -> s.label
                chosen.isNotEmpty() -> appLabel(chosen)
                else -> s.label
            }
            val long: (() -> Unit)? = if (fixed) null else ({ chooseApp(s) })
            val r = row(label, long) {
                when {
                    s.id == "music" -> present("music", 1) { musicView() }
                    fixed -> launch(s)
                    pkgFor(s) == null && fallbackIntent(s) == null -> chooseApp(s)
                    else -> launch(s)
                }
            }
            if (!fixed && pkgFor(s) == null) r.setTextColor(DIM)
            list.addView(r)
        }
        val sv = ScrollView(this).apply { isFillViewport = true }
        sv.addView(list)
        homeList = sv
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(22) })
        val foot = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        npTv = tv("", 13f, DIM).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }
        foot.addView(npTv, LinearLayout.LayoutParams(0, -2, 1f))
        val tools = tv("tools ↑", 12f, DIM).apply { setPadding(dp(14), dp(10), 0, dp(10)) }
        press(tools, null) { present("tools", 2) { toolsView() } }
        foot.addView(tools)
        col.addView(foot, lp(8, -1))
        updateHome(); return col
    }

    private fun updateHome() {
        if (stack.lastOrNull()?.key != "home") return
        if (sessionActive) {
            val left = sessionLeftMs()
            if (left <= 0L) { endSessionNow(); return }
            timeTv?.text = sessionLabel(left)
            dateTv?.text = "detoxify · " + SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()).lowercase()
        } else {
            val pat = if (android.text.format.DateFormat.is24HourFormat(this)) "H:mm" else "h:mm"
            timeTv?.text = SimpleDateFormat(pat, Locale.getDefault()).format(Date())
            dateTv?.text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()).lowercase()
        }
        val t = activeController()?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        npTv?.text = if (t != null) "playing · ${t.lowercase()}" else ""
    }

    // ---------- Detoxify session timer ----------

    /** Takes the deadline from a new launch intent, or restores the persisted anchors. */
    private fun readSessionTimer() {
        val i = intent
        val token = i?.getLongExtra(EXTRA_SESSION, 0L) ?: 0L
        val seen = prefs.getLong("session_id", 0L)
        // A recreation (dark mode, font size, process death) still hands us the original launch
        // intent, so the extras may only be trusted once per session id. startDetox() always
        // sends a fresh id (its start time), and a completed session keeps the old one, so a
        // stale intent can never resurrect a countdown.
        if (token != 0L && token != seen && i != null && i.hasExtra(EXTRA_END_WALL)) {
            sessionEndWall = i.getLongExtra(EXTRA_END_WALL, 0L)
            sessionLeftAtStart = i.getLongExtra(EXTRA_END_LEFT, 0L)
            sessionEndMono = SystemClock.elapsedRealtime()
            prefs.edit().putLong("session_id", token).apply()
            persistSessionTimer()
        } else {
            sessionEndWall = prefs.getLong("session_end_wall", 0L)
            sessionEndMono = prefs.getLong("session_end_mono", SystemClock.elapsedRealtime())
            sessionLeftAtStart = prefs.getLong("session_left", 0L)
        }
        sessionActive = sessionEndWall != 0L && sessionLeftMs() > 0L
        if (sessionEndWall != 0L && !sessionActive) clearSessionTimer()
    }

    /** Freezes the current remaining time so a recreation resumes instead of restarting. */
    private fun persistSessionTimer() {
        if (sessionEndWall == 0L) return
        prefs.edit().putLong("session_end_wall", sessionEndWall)
            .putLong("session_end_mono", SystemClock.elapsedRealtime())
            .putLong("session_left", sessionLeftMs()).apply()
    }

    /**
     * Remaining session ms. The monotonic clock wins while it kept moving; after a reboot
     * ([endMono] from before boot is larger than now) it falls back to the wall-clock deadline.
     */
    private fun sessionLeftMs(): Long {
        if (sessionEndWall == 0L) return 0L
        val nowMono = SystemClock.elapsedRealtime()
        val left = if (nowMono >= sessionEndMono) sessionLeftAtStart - (nowMono - sessionEndMono)
                   else minOf(sessionEndWall - System.currentTimeMillis(), sessionLeftAtStart)
        return left.coerceAtLeast(0L)
    }

    private fun sessionLabel(ms: Long): String {
        val h = ms / 3_600_000L
        return when {
            ms >= 86_400_000L -> "${ms / 86_400_000L}d ${(ms % 86_400_000L) / 3_600_000L}h"
            h >= 1 -> "%d:%02d:%02d".format(h, (ms % 3_600_000L) / 60_000L, (ms % 60_000L) / 1000L)
            else -> "%02d:%02d".format(ms / 60_000L, (ms % 60_000L) / 1000L)
        }
    }

    private fun clearSessionTimer() {
        prefs.edit().remove("session_end_wall").remove("session_end_mono").remove("session_left").apply()
        sessionEndWall = 0L; sessionEndMono = 0L; sessionLeftAtStart = 0L; sessionActive = false
    }

    /** The timer is up: forget it and hand the screen back to the launcher. */
    private fun endSessionNow() {
        clearSessionTimer()
        setResult(RESULT_OK, Intent().putExtra(EXTRA_DONE, true))
        finish()
    }

    // ---------- pause screen ----------
    private fun pauseView(s: Slot): View {
        val col = column().apply { gravity = Gravity.CENTER_HORIZONTAL }
        col.addView(tv("opening", 13f, DIM))
        col.addView(tv(s.label, 34f, WHITE, light), lp(4))
        val circle = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.TRANSPARENT); setStroke(dp(1), DIM) }
        }
        col.addView(circle, LinearLayout.LayoutParams(dp(110), dp(110)).apply { topMargin = dp(44) })
        col.addView(tv("wait", 15f, DIM), lp(22))
        val what = EditText(this).apply {
            hint = "what for?"; setHintTextColor(DIM); setTextColor(WHITE); typeface = reg; textSize = 18f
            setSingleLine(); background = null; gravity = Gravity.CENTER
        }
        col.addView(what, lp(34, -1))
        col.addView(View(this).apply { setBackgroundColor(WHITE) }, LinearLayout.LayoutParams(-1, dp(1)))
        val box = FrameLayout(this).apply { setBackgroundColor(LINE) }
        val bar = View(this).apply { setBackgroundColor(WHITE); pivotX = 0f; scaleX = 0f }
        box.addView(bar, FrameLayout.LayoutParams(-1, -1))
        col.addView(box, LinearLayout.LayoutParams(-1, dp(2)).apply { topMargin = dp(26) })
        col.addView(tv("3 seconds", 12f, DIM), lp(8))
        col.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        var ready = false
        val go = btn("go", true) {
            if (ready) {
                val w = what.text.toString().trim()
                log("${s.label}: ${if (w.isEmpty()) "-" else w}"); addDay("intents", 1)
                pop(); handler.postDelayed({ openSlot(s) }, 220)
            }
        }
        go.alpha = 0.25f
        col.addView(go, lp(0, -1))
        col.addView(btn("not now", false) { pop() }, lp(10, -1))
        bar.animate().scaleX(1f).setDuration(3000).setInterpolator(LinearInterpolator()).withEndAction {
            ready = true; go.animate().alpha(1f).setDuration(250).start()
        }.start()
        return col
    }

    private fun blockView(title: String, sub: String): View {
        val col = column().apply { gravity = Gravity.CENTER_HORIZONTAL }
        col.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(tv(title, 32f, WHITE, light)); col.addView(tv(sub, 15f, DIM), lp(8))
        col.addView(tv("find a spot to reflect.\nturn your phone away.", 14f, DIM).apply { gravity = Gravity.CENTER }, lp(28))
        col.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(btn("ok", false) { pop() }, lp(0, -1))
        return col
    }

    private fun limitView(): View {
        val col = blockView("limit reached", "${usedMin()} of ${limitNow()} min used today") as LinearLayout
        val more = tv("need 5 more minutes", 13f, DIM).apply { gravity = Gravity.CENTER; setPadding(0, dp(14), 0, 0) }
        press(more, null) { askExtra() }; col.addView(more, lp(0, -1))
        return col
    }

    private fun askExtra() {
        val input = calmInput("why 5 more minutes? (10+ characters)")
        val h = calmDialog(
            title = "need more time?",
            bodyView = input,
            positive = "add 5 min",
            onPositive = { log("+5 min: ${input.text}"); addDay("extra", 5); pop() },
            negative = "never mind"
        )
        val ok = h.positive ?: return
        ok.isEnabled = false
        ok.alpha = 0.4f
        var waited = false
        val refresh = {
            ok.isEnabled = waited && input.text.length >= 10
            ok.alpha = if (ok.isEnabled) 1f else 0.4f
        }
        val cd = object : CountDownTimer(20_000, 1000) {
            override fun onTick(t: Long) { h.title?.text = "wait ${t / 1000}s" }
            override fun onFinish() { waited = true; h.title?.text = "need more time?"; refresh() }
        }.start()
        h.dialog.setOnDismissListener { cd.cancel() }
        input.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { refresh() }
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
        })
    }

    // ---------- tools ----------
    private fun digestLines() = prefs.getString("digest", "")!!.split("\n").filter { it.isNotBlank() }
        .map { it.split("\u0001") }.filter { it.size >= 3 }

    /**
     * Tools launch other apps through implicit intents. Not every vendor clock answers
     * SHOW_TIMERS (and not every calculator answers CATEGORY_APP_CALCULATOR), so fall back to
     * the known packages before giving up.
     */
    private fun openClock(action: String) {
        if (tryStart(Intent(action))) return
        if (action != AlarmClock.ACTION_SHOW_ALARMS && tryStart(Intent(AlarmClock.ACTION_SHOW_ALARMS))) return
        if (tryStartPackage(CLOCK_PACKAGES)) return
        toast("no clock app found")
    }

    private fun openCalculator() {
        if (tryStart(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR))) return
        if (tryStartPackage(CALCULATOR_PACKAGES)) return
        toast("no calculator app found")
    }

    private fun tryStart(i: Intent): Boolean = try { startActivity(i); true } catch (e: Exception) { false }

    private fun tryStartPackage(pkgs: List<String>): Boolean {
        for (pkg in pkgs) {
            val i = packageManager.getLaunchIntentForPackage(pkg) ?: continue
            if (tryStart(i)) return true
        }
        return false
    }

    private fun toolsView(): View {
        val col = column("tools")
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stagger" }
        val n = digestLines().size
        list.addView(row("notes") { present("notes", 1) { notesView() } })
        list.addView(row(if (n > 0) "digest  ·  $n" else "digest") { present("digest", 1) { digestView() } })
        list.addView(row("alarm") { openClock(AlarmClock.ACTION_SHOW_ALARMS) })
        list.addView(row("timer") { openClock(AlarmClock.ACTION_SHOW_TIMERS) })
        list.addView(row("calculator") { openCalculator() })
        list.addView(row("reflect") { present("reflect", 1) { reflectView() } })
        list.addView(row("detox") { present("detox", 1) { detoxView() } })
        list.addView(row("settings") { present("settings", 1) { settingsView() } })
        list.addView(row("leave dumb mode") { leave() })
        val sv = ScrollView(this); sv.addView(list); col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8) }); return col
    }

    private fun notesView(): View {
        val col = column("notes")
        val e = EditText(this).apply {
            setText(prefs.getString("notes", "")); setTextColor(WHITE); textSize = 18f; typeface = reg; background = null
            gravity = Gravity.TOP; hint = "write it down. park the thought."; setHintTextColor(DIM)
            addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) { prefs.edit().putString("notes", s.toString()).apply() }
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            })
        }
        col.addView(e, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(12) }); return col
    }

    private fun digestView(): View {
        val col = column("digest")
        val items = digestLines()
        if (items.isEmpty()) { col.addView(tv("nothing waiting", 22f, DIM, light), lp(24)); return col }
        val sv = ScrollView(this); val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stagger" }
        items.groupBy { it[0] }.forEach { (pkg, list) ->
            inner.addView(tv("${appLabel(pkg)}  ·  ${list.size}", 19f, WHITE, light), lp(20))
            list.takeLast(4).forEach {
                inner.addView(tv("${it[1]}: ${it[2]}".trim(), 13f, DIM).apply { maxLines = 2; ellipsize = TextUtils.TruncateAt.END }, lp(4))
            }
        }
        sv.addView(inner)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f).apply { topMargin = dp(8) })
        col.addView(btn("clear", false) { prefs.edit().putString("digest", "").apply(); pop() }, lp(12, -1))
        return col
    }

    private fun detoxView(): View {
        val col = column("detox")
        val used = usedMin(); val lim = limitNow()
        col.addView(tv("day ${minOf(taperDay() + 1, 21)} of 21", 13f, DIM), lp(20))
        col.addView(tv("$used of $lim min", 32f, WHITE, light), lp(4))
        val box = FrameLayout(this).apply { setBackgroundColor(LINE) }
        val fill = View(this).apply { setBackgroundColor(WHITE); pivotX = 0f; scaleX = 0f }
        box.addView(fill, FrameLayout.LayoutParams(-1, -1))
        col.addView(box, LinearLayout.LayoutParams(-1, dp(2)).apply { topMargin = dp(16); bottomMargin = dp(18) })
        fill.animate().scaleX((used.toFloat() / lim).coerceIn(0f, 1f)).setStartDelay(250).setDuration(900).setInterpolator(ease).start()
        col.addView(infoRow("phone-free today", "${dayInt("fdsec") / 60} min"))
        col.addView(infoRow("intentions today", "${dayInt("intents")}"))
        col.addView(infoRow("quiet hours", "${getS("qs", "22")}:00 to ${getS("qe", "7")}:00"))
        col.addView(infoRow("digest waiting", "${digestLines().size}"))
        val recent = prefs.getString("log", "")!!.split("\n").filter { it.isNotBlank() }.takeLast(4).reversed()
        if (recent.isNotEmpty()) {
            col.addView(tv("recent", 12f, DIM), lp(22))
            recent.forEach { col.addView(tv(it, 13f, DIM).apply { maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, lp(6)) }
        }
        return col
    }

    // ---------- settings ----------
    private fun settingsRow(label: String, value: String, onClick: () -> Unit): View {
        val r = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(12), 0, dp(12)) }
        r.addView(tv(label, 20f, WHITE, light), LinearLayout.LayoutParams(0, -2, 1f)); r.addView(tv(value, 13f, DIM))
        press(r, null, onClick); return r
    }

    private fun settingsView(): View {
        val col = column("settings")
        val sv = ScrollView(this); val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        inner.addView(settingsRow("apps", "${gridCount()}") { present("apps", 1) { appsView() } })
        inner.addView(settingsRow("daily limit", "${getS("limit", "60")} min") { editLimit() })
        inner.addView(settingsRow("quiet hours", "${getS("qs", "22")} to ${getS("qe", "7")}") { editQuiet() })
        val live = prefs.getBoolean("wa_live", true)
        inner.addView(settingsRow("whatsapp alerts", if (live) "live" else "digest") { prefs.edit().putBoolean("wa_live", !live).apply(); rebuildCurrent() })
        inner.addView(settingsRow("notification access", if (listenerEnabled()) "on" else "off") { safe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) })
        inner.addView(settingsRow("theme", themeNames[prefs.getInt("theme", 0)]) { cycle("theme") })
        inner.addView(settingsRow("typeface", fontNames[prefs.getInt("font", 0)]) { cycle("font") })
        val ink = prefs.getBoolean("eink", false)
        inner.addView(settingsRow("e-ink refresh", if (ink) "on" else "off") { prefs.edit().putBoolean("eink", !ink).apply(); rebuildCurrent() })
        inner.addView(settingsRow("run setup again", "") { obGo(0) })
        sv.addView(inner); col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f)); return col
    }

    private fun cycle(key: String) { prefs.edit().putInt(key, (prefs.getInt(key, 0) + 1) % 3).apply(); applyTheme(); rebuildCurrent() }

    private fun appsView(): View {
        val col = column("apps")
        val sv = ScrollView(this); val inner = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        inner.addView(settingsRow("apps on home", "${gridCount()}") { chooseGridCount() })
        slots.forEach { s ->
            if (s.id == "browser") return@forEach
            getS("slot_${s.id}", "")
            val pending = prefs.getLong("pat_slot_${s.id}", 0) != 0L
            val value = when {
                s.id == "phone" || s.id == "camera" -> "always"
                pending -> "changing in 24h"
                else -> slotValue(s)
            }
            inner.addView(settingsRow(s.label, value) { chooseApp(s) })
        }
        sv.addView(inner)
        col.addView(sv, LinearLayout.LayoutParams(-1, 0, 1f))
        return col
    }

    private fun chooseGridCount() {
        val opts = listOf("9 apps · short list", "15 apps · long list")
        calmDialog(title = "apps on home", items = opts, onItem = { i ->
            prefs.edit().putString("grid_count", if (i == 0) "9" else "15").apply()
            rebuildCurrent()
        })
    }

    private fun chooseApp(s: Slot, setup: Boolean = false) {
        if (s.id == "browser") { toast("the browser is built in"); return }
        if (s.id == "phone" || s.id == "camera") { toast("${s.label} is always here"); return }
        val apps = installed()
        val extra = s.id.startsWith("extra")
        val names = ArrayList<String>()
        val actions = ArrayList<() -> Unit>()
        if (extra) {
            names.add("leave empty")
            actions.add { setS("slot_${s.id}", "", false); rebuildCurrent() }
        } else {
            names.add("use the default app")
            actions.add { setS("slot_${s.id}", "", false); rebuildCurrent() }
        }
        val current = pkgFor(s)
        apps.forEach { (label, pkg) ->
            names.add(if (pkg == current) "$label  ✓" else label)
            // Changing a default slot to another app is a loosening, so it waits 24 hours. The
            // onboarding picker is the first setup, not a change, so it applies immediately.
            actions.add { setS("slot_${s.id}", pkg, !setup && !extra); rebuildCurrent() }
        }
        calmDialog(title = s.label, items = names, onItem = { i -> actions[i]() })
    }

    private fun numField(v: String) = calmInput("", numeric = true).apply { setText(v) }

    private fun editLimit() {
        val f = numField(getS("limit", "60"))
        calmDialog(
            title = "daily limit (minutes)",
            bodyView = f,
            positive = "save",
            onPositive = {
                val n = (f.text.toString().toIntOrNull() ?: 60).coerceIn(10, 480)
                setS("limit", n.toString(), n > getS("limit", "60").toInt()); rebuildCurrent()
            },
            negative = "cancel"
        )
    }

    private fun editQuiet() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val a = numField(getS("qs", "22")); val b = numField(getS("qe", "7"))
        lay.addView(tv("starts (hour, 0-23)", 13f, DIM), lp(4))
        lay.addView(a, lp(4))
        lay.addView(tv("ends (hour, 0-23)", 13f, DIM), lp(10))
        lay.addView(b, lp(4))
        calmDialog(
            title = "quiet hours",
            bodyView = lay,
            positive = "save",
            onPositive = {
                val ns = (a.text.toString().toIntOrNull() ?: 22).coerceIn(0, 23); val ne = (b.text.toString().toIntOrNull() ?: 7).coerceIn(0, 23)
                fun len(s: Int, e: Int) = (e - s + 24) % 24
                val loosen = len(ns, ne) < len(getS("qs", "22").toInt(), getS("qe", "7").toInt())
                setS("qs", ns.toString(), loosen); setS("qe", ne.toString(), loosen); rebuildCurrent()
            },
            negative = "cancel"
        )
    }

    // ---------- leave dumb mode: wait, PIN, phrase, reason ----------

    /** Remaining wait from the monotonic anchor; the wall anchor is only the post-reboot fallback. */
    private fun leaveLeftMs(): Long = DetoxPolicy.waitLeftMs(
        prefs.getLong("leave_mono", 0L),
        prefs.getLong("leave_at", 0L),
        SystemClock.elapsedRealtime(),
        System.currentTimeMillis()
    )

    private fun startLeaveWait() {
        prefs.edit().putLong("leave_at", System.currentTimeMillis())
            .putLong("leave_mono", SystemClock.elapsedRealtime()).apply()
        toast("come back in 10 minutes")
    }

    private fun cancelLeave() {
        prefs.edit().remove("leave_at").remove("leave_mono").apply()
    }

    /** The launcher PIN. A session started before the PIN existed simply proceeds. */
    private fun askPin(onOk: () -> Unit) {
        if (lock.finishResetIfDue()) { onOk(); return }
        if (!lock.isSet) { onOk(); return }
        val locked = lock.lockedForSec()
        if (locked > 0) { toast("try again in ${locked / 60 + 1} min"); return }
        val f = calmInput("PIN", numeric = true, password = true)
        val h = calmDialog(
            title = "enter your PIN",
            message = "the same PIN that protects your distracting apps.",
            bodyView = f,
            positive = "ok",
            dismissOnPositive = false,
            neutral = "forgot PIN",
            onNeutral = { lock.startReset(); toast("PIN can be removed in 24 hours") },
            negative = "stay"
        )
        // Keep the dialog open on a wrong PIN so the lockout stays visible.
        h.positive?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val wait = lock.lockedForSec()
            when {
                wait > 0 -> { f.setText(""); f.error = "too many tries, wait ${wait}s" }
                lock.verify(f.text.toString()) -> { h.dialog.dismiss(); onOk() }
                else -> {
                    f.setText("")
                    val next = lock.lockedForSec()
                    f.error = if (next > 0) "too many tries, wait ${next}s" else "wrong PIN"
                }
            }
        }
    }

    private fun leave() {
        val pending = prefs.getLong("leave_at", 0L) != 0L
        if (!pending) {
            calmDialog(
                title = "leave dumb mode",
                message = "first a 10 minute wait, then your PIN, a phrase and a reason.",
                positive = "start the wait",
                onPositive = { startLeaveWait() },
                negative = "stay"
            )
        } else if (leaveLeftMs() > 0L) {
            val minLeft = (leaveLeftMs() + 59_999L) / 60_000L
            calmDialog(
                title = "$minLeft min left",
                message = "still want to leave?",
                positive = "keep waiting",
                negative = "cancel leaving",
                onNegative = { cancelLeave() }
            )
        } else {
            askPin { confirmLeave() }
        }
    }

    private fun confirmLeave() {
        val lay = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val phrase = calmInput("type: i choose to leave")
        val reason = calmInput("why? (10+ characters)")
        lay.addView(phrase, lp(4)); lay.addView(reason, lp(8))
        val h = calmDialog(
            title = "leave dumb mode",
            bodyView = lay,
            positive = if (sessionActive) "leave" else "open home settings",
            dismissOnPositive = false,
            negative = "stay"
        )
        // Keep the dialog open on a bad phrase/reason so the reason is visible instead of a
        // toast behind the dialog; the wait and PIN stay valid, so a retry is immediate.
        h.positive?.setOnClickListener {
            it.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            val typedPhrase = phrase.text.toString().trim().lowercase(Locale.getDefault())
            val typedReason = reason.text.toString().trim()
            val phraseOk = typedPhrase == "i choose to leave"
            val reasonOk = typedReason.length >= 10
            phrase.error = if (phraseOk) null else "type exactly: i choose to leave"
            reason.error = if (reasonOk) null else "say why, at least 10 characters"
            if (!phraseOk || !reasonOk) return@setOnClickListener
            log("left dumb mode: $typedReason")
            cancelLeave()
            h.dialog.dismiss()
            if (sessionActive) {
                // Detoxify integration: the launcher owns the session. Signal it in-process
                // (so it is already off when the launcher resumes) and finish.
                CalmSessionBridge.notifyLeft(this)
                endSessionNow()
            } else {
                safe(Intent(Settings.ACTION_HOME_SETTINGS))
            }
        }
    }

    // ---------- music ----------
    private fun listenerEnabled() = (Settings.Secure.getString(contentResolver, "enabled_notification_listeners") ?: "").contains(packageName)

    private fun activeController(): MediaController? = try {
        val m = getSystemService(MediaSessionManager::class.java)
        val l = m.getActiveSessions(ComponentName(this, NotifListener::class.java))
        l.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: l.firstOrNull()
    } catch (e: Exception) { null }

    private fun updateMusic() {
        val c = activeController(); val md = c?.metadata; val st = c?.playbackState
        musicTitle?.text = md?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "nothing playing"
        musicArtist?.text = md?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        val dur = md?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        var frac = 0f
        if (st != null && dur > 0) {
            var pos = st.position
            if (st.state == PlaybackState.STATE_PLAYING) pos += ((SystemClock.elapsedRealtime() - st.lastPositionUpdateTime) * st.playbackSpeed).toLong()
            frac = (pos.toFloat() / dur).coerceIn(0f, 1f)
        }
        musicFill?.scaleX = frac
        musicPlay?.text = if (st?.state == PlaybackState.STATE_PLAYING) "pause" else "play"
    }

    private fun musicView(): View {
        val col = column("music")
        if (!listenerEnabled()) {
            col.addView(tv("to show what's playing, allow notification access.", 14f, DIM), lp(8))
            col.addView(btn("allow access", false) { safe(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, lp(14))
        }
        col.addView(tv("now playing", 12f, DIM), lp(22))
        musicTitle = tv("nothing playing", 28f, WHITE, light); col.addView(musicTitle, lp(4))
        musicArtist = tv("", 15f, DIM); col.addView(musicArtist, lp(4))
        val box = FrameLayout(this).apply { setBackgroundColor(LINE) }
        musicFill = View(this).apply { setBackgroundColor(WHITE); pivotX = 0f; scaleX = 0f }
        box.addView(musicFill, FrameLayout.LayoutParams(-1, -1))
        col.addView(box, LinearLayout.LayoutParams(-1, dp(2)).apply { topMargin = dp(18); bottomMargin = dp(18) })
        val ctl = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        musicPlay = btn("play", true) {
            val c = activeController()
            if (c != null) { if (c.playbackState?.state == PlaybackState.STATE_PLAYING) c.transportControls.pause() else c.transportControls.play() }
        }
        ctl.addView(btn("prev", false) { activeController()?.transportControls?.skipToPrevious() })
        ctl.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)); ctl.addView(musicPlay)
        ctl.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f))
        ctl.addView(btn("next", false) { activeController()?.transportControls?.skipToNext() })
        col.addView(ctl, lp(0, -1))
        val pl = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; tag = "stagger" }
        listOf("focus", "walk", "sleep").forEach { n ->
            pl.addView(row(n, { setPlaylist(n) }) {
                val link = prefs.getString("pl_$n", "")!!
                if (link.isEmpty()) toast("hold to set a playlist link") else safe(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
            })
        }
        col.addView(pl, lp(22, -1))
        col.addView(tv("hold a playlist to set its link", 12f, DIM), lp(4))
        col.addView(View(this), LinearLayout.LayoutParams(-1, 0, 1f))
        col.addView(btn("open music app", false) { musicSlot()?.let { launch(it) } }, lp(0, -1))
        handler.post(musicTick); return col
    }

    private fun musicSlot() = slots.firstOrNull { it.id == "music" }

    private fun setPlaylist(n: String) {
        val f = calmInput("paste a spotify or youtube music link").apply { setText(prefs.getString("pl_$n", "")) }
        calmDialog(
            title = "$n playlist",
            bodyView = f,
            positive = "save",
            onPositive = { prefs.edit().putString("pl_$n", f.text.toString().trim()).apply() },
            negative = "cancel"
        )
    }

    companion object {
        /** Deadline extras the launcher passes in, and the result it gets back. */
        const val EXTRA_SESSION = "detox_session_id"
        const val EXTRA_END_WALL = "detox_end_wall"
        const val EXTRA_END_LEFT = "detox_end_left"
        const val EXTRA_DONE = "detox_session_done"

        /** Fallbacks for tools when the vendor app does not answer the standard intent. */
        private val CLOCK_PACKAGES = DetoxPolicy.clockPackages.toList()
        private val CALCULATOR_PACKAGES = DetoxPolicy.calculatorPackages.toList()
        // The leave signal lives in CalmSessionBridge, which owns the in-process handoff and the
        // cold-start prefs fallback.
    }
}
