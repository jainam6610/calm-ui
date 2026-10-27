package com.studentlauncher.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Two depths, one material.
 *
 * [Content] is the quiet card widgets read through; [Chrome] is the floating layer (dock,
 * sheets, search, handle, menus, toasts) that lifts off the wallpaper with a shadow.
 * Keeping the distinction explicit stops stacked surfaces from looking like mush.
 */
enum class GlassLevel { Content, Chrome }

/**
 * The launcher's panel material: a plain neutral surface, not wallpaper glass.
 *
 * Earlier builds sampled a blurred copy of the wallpaper and bent it at the edges, which made
 * every panel pick up the photo's colour and fight the accent. This version is deliberately
 * flat: one neutral fill, a hairline rim, a soft top sheen on chrome and a shadow so floating
 * surfaces still read as floating. Nothing here depends on the wallpaper, so panels stay
 * legible over photos, presets and plain white alike.
 */
fun Modifier.glass(
    radius: Dp = 26.dp,
    level: GlassLevel = GlassLevel.Content
): Modifier = composed {
    val dark = LocalColors.current.dark
    val chrome = level == GlassLevel.Chrome
    val shape = RoundedCornerShape(radius)

    val fill = if (dark) {
        Color(0xFF141417).copy(alpha = if (chrome) 0.94f else 0.90f)
    } else {
        Color(0xFFFBFBFD).copy(alpha = if (chrome) 0.95f else 0.92f)
    }
    val rim = if (dark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
    val sheen = if (dark) 0.05f else 0.32f
    val shadowC = Color.Black.copy(alpha = if (dark) 0.46f else 0.16f)

    val base = if (chrome) {
        this.shadow(
            elevation = if (dark) 18.dp else 12.dp,
            shape = shape,
            clip = false,
            ambientColor = shadowC,
            spotColor = shadowC
        )
    } else this

    base
        .clip(shape)
        .drawBehind {
            drawRect(fill)
            if (chrome) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = sheen),
                        1f to Color.Transparent,
                        endY = size.height * 0.55f
                    )
                )
            }
        }
        .drawWithContent {
            drawContent()
            drawRoundRect(
                color = rim,
                cornerRadius = CornerRadius(radius.toPx()),
                style = Stroke(1.dp.toPx())
            )
        }
}
