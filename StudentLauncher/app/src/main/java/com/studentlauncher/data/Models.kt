package com.studentlauncher.data

import android.content.ComponentName
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(val pkg: String, val label: String, val component: ComponentName)

/** [mono] is the themed (monochrome) layer when the app ships one (Android 13+). */
class AppIcons(val original: ImageBitmap, val mono: ImageBitmap?)

enum class ThemeMode { Light, Dark, System }
enum class IconShape { None, Circle, Squircle }
enum class IconStyle { Mono, Original, MinimalBubble, MinimalOutline }
enum class HomeLayout { List, Grid }
enum class AlphaSide { Right, Left, Off }
enum class DockStyle { Bottom, Rail, Off }
enum class ViewMode { Home, All }
enum class Chip { All, Study }
enum class InternalDestination { Home, Plan, Settings }

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
