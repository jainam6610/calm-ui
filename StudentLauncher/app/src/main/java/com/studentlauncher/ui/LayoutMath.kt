package com.studentlauncher.ui

/**
 * Home becomes compact when the person asks for it, or automatically on short screens.
 * Kept pure so the rule is testable without Compose.
 */
internal fun isCompactHome(preferred: Boolean, screenHeightDp: Float): Boolean =
    preferred || screenHeightDp < 640f
