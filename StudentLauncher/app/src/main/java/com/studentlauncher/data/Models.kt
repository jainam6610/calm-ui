package com.studentlauncher.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap

data class AppInfo(val pkg: String, val label: String, val component: ComponentName)

/** [mono] is the themed (monochrome) layer when the app ships one (Android 13+). */
class AppIcons(val original: ImageBitmap, val mono: ImageBitmap?) {
    private val maskCache = HashMap<Int, BooleanArray>()

    /**
     * Cached coverage mask of the monochrome glyph at [grid] x [grid], used by the Dot and
     * Hairline/Bold pack renderers. Returns an empty mask when there is no mono layer.
     */
    fun dotMask(grid: Int): BooleanArray = maskCache.getOrPut(grid) {
        val src = mono ?: return@getOrPut BooleanArray(grid * grid)
        val bmp = src.asAndroidBitmap()
        val w = bmp.width
        val h = bmp.height
        if (w <= 0 || h <= 0) return@getOrPut BooleanArray(grid * grid)
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val out = BooleanArray(grid * grid)
        for (gy in 0 until grid) {
            val y0 = gy * h / grid
            val y1 = ((gy + 1) * h / grid).coerceIn(y0 + 1, h)
            for (gx in 0 until grid) {
                val x0 = gx * w / grid
                val x1 = ((gx + 1) * w / grid).coerceIn(x0 + 1, w)
                var sum = 0L
                var n = 0
                for (y in y0 until y1) {
                    val row = y * w
                    for (x in x0 until x1) { sum += px[row + x] ushr 24; n++ }
                }
                out[gy * grid + gx] = n > 0 && sum / n > 64
            }
        }
        out
    }
}

enum class ThemeMode { Light, Dark, System }
enum class IconShape { None, Circle, Squircle }

/**
 * Built-in icon packs (one monochrome glyph, several treatments).
 * Original keeps the app's own artwork; the rest derive from its themed layer.
 */
enum class IconPack { Original, Mono, Hairline, Bold, Dot, Bubble, Tile }

/**
 * Maps a persisted icon-pack value to an [IconPack]. Handles the pre-3.1 `IconStyle`
 * names (Mono, Original, MinimalBubble, MinimalOutline) so upgrades keep their look.
 */
fun iconPackFromStored(raw: String?): IconPack = when (raw) {
    null -> IconPack.Original
    "MinimalBubble" -> IconPack.Bubble
    "MinimalOutline" -> IconPack.Tile
    else -> runCatching { IconPack.valueOf(raw) }.getOrDefault(IconPack.Original)
}
enum class HomeLayout { List, Grid }
enum class AlphaSide { Right, Left, Off }
enum class DockStyle { Bottom, Rail, Off }
enum class ViewMode { Home, All }
enum class Chip { All, Study }
enum class InternalDestination { Home, Plan, Settings }

/** Sub-page shown inside the Settings app. [Root] is the top-level settings list. */
enum class SettingsPage { Root, Personalization, Icons, Theme, Wallpaper, Home, Clock }

/** Lightweight, local-only study plan item.  Dates are ISO-8601 strings for stable persistence. */
data class PlanTask(
    val id: String,
    val title: String,
    val due: String = "",
    val priority: Int = 1,
    val completed: Boolean = false,
    val className: String = ""
)
data class PlanEvent(val id: String, val title: String, val date: String, val time: String = "", val course: String = "")

data class DailyUsage(val pkg: String, val minutes: Int)
