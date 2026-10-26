package com.studentlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The clean Settings scaffold: a centred title, a quiet back arrow, and one scrolling column.
 * No eyebrow, no card chrome; the rows do the work.
 */
@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val c = LocalColors.current
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Box(Modifier.fillMaxWidth().height(64.dp)) {
            if (onBack != null) {
                Box(
                    Modifier.align(Alignment.CenterStart).size(48.dp).tap(onBack),
                    contentAlignment = Alignment.Center
                ) { BackArrow(c.fg) }
            }
            Txt(
                title, 22f, c.fg, FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            content = content
        )
    }
}

@Composable
private fun BackArrow(tint: Color) {
    Canvas(Modifier.size(24.dp)) {
        val s = 2.dp.toPx()
        val mid = size.height / 2f
        drawLine(tint, Offset(size.width * 0.72f, mid), Offset(size.width * 0.28f, mid), strokeWidth = s)
        drawLine(tint, Offset(size.width * 0.28f, mid), Offset(size.width * 0.48f, size.height * 0.3f), strokeWidth = s)
        drawLine(tint, Offset(size.width * 0.28f, mid), Offset(size.width * 0.48f, size.height * 0.7f), strokeWidth = s)
    }
}

/** A section header and its rows, with no card background. */
@Composable
internal fun SettingsGroup(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalColors.current
    Column(Modifier.fillMaxWidth().padding(top = if (title == null) 8.dp else 22.dp)) {
        if (title != null) {
            Txt(
                title.uppercase(), 10.5f, c.fg2, FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
                tracking = 1.3f
            )
        }
        content()
    }
}

/** Icon + title + optional subtitle + chevron. The primary Settings row. */
@Composable
internal fun SettingsLink(
    title: String,
    sub: String? = null,
    glyph: String,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(14.dp))
            .tap(onClick)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsGlyph(glyph, 40.dp, c.fg)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 17f, c.fg)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Txt(sub, 12f, c.fg2, maxLines = 1)
            }
        }
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(8.dp))
        }
        Txt("\u203A", 24f, c.fg2.copy(alpha = 0.7f))
    }
}

/** Icon + title + subtitle + switch. */
@Composable
internal fun SettingsToggle(
    title: String,
    sub: String?,
    glyph: String,
    on: Boolean,
    onChange: (Boolean) -> Unit
) {
    val c = LocalColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsGlyph(glyph, 40.dp, c.fg)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Txt(title, 17f, c.fg)
            if (sub != null) {
                Spacer(Modifier.height(2.dp))
                Txt(sub, 12f, c.fg2, maxLines = 2)
            }
        }
        Spacer(Modifier.width(12.dp))
        GlassSwitch(on, onChange)
    }
}

/** A compact action row inside a sub-page (same rhythm as SettingsLink, no chevron). */
@Composable
internal fun SettingsAction(
    label: String,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    val c = LocalColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (filled) c.fg else c.track)
            .tap(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Txt(label, 14f, if (filled) c.knobOn else c.fg)
    }
}

/** Thin line icon, matching the reference's outline glyphs. No decorative ring. */
@Composable
internal fun SettingsGlyph(kind: String, size: Dp, tint: Color) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val s = 1.7.dp.toPx()
        val g = w * 0.19f
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
            drawLine(tint, Offset(cx + x1, cy + y1), Offset(cx + x2, cy + y2), strokeWidth = s)
        fun circle(r: Float, fill: Boolean = false) =
            drawCircle(tint, r, Offset(cx, cy), style = if (fill) Fill else Stroke(s))
        when (kind) {
            "personal" -> {
                circle(g * 0.55f)
                line(-g * 0.9f, -g * 0.2f, -g * 0.9f, g * 0.9f)
                line(g * 0.9f, -g * 0.2f, g * 0.9f, g * 0.9f)
                drawArc(tint, 200f, 140f, false, Offset(cx - g * 0.9f, cy - g * 0.9f), Size(g * 1.8f, g * 1.8f), style = Stroke(s))
            }
            "home" -> {
                val p = Path().apply {
                    moveTo(cx - g, cy)
                    lineTo(cx, cy - g)
                    lineTo(cx + g, cy)
                    lineTo(cx + g, cy + g)
                    lineTo(cx - g, cy + g)
                    close()
                }
                drawPath(p, tint, style = Stroke(s))
                line(0f, g * 0.35f, 0f, g)
            }
            "focus" -> {
                circle(g)
                circle(g * 0.55f)
                circle(g * 0.16f, fill = true)
            }
            "detox", "clock" -> {
                circle(g)
                line(0f, 0f, 0f, -g * 0.6f)
                line(0f, 0f, g * 0.45f, 0f)
            }
            "signals" -> {
                drawArc(tint, -50f, 100f, false, Offset(cx - g, cy - g), Size(g * 2f, g * 2f), style = Stroke(s))
                drawArc(tint, -50f, 100f, false, Offset(cx - g * 0.6f, cy - g * 0.6f), Size(g * 1.2f, g * 1.2f), style = Stroke(s))
                circle(g * 0.18f, fill = true)
            }
            "privacy" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - g * 0.8f, cy - g * 0.05f),
                    size = Size(g * 1.6f, g * 1.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(g * 0.2f),
                    style = Stroke(s)
                )
                drawArc(tint, 180f, 180f, false, Offset(cx - g * 0.5f, cy - g * 0.9f), Size(g, g), style = Stroke(s))
            }
            "advanced" -> {
                listOf(-0.55f, 0f, 0.55f).forEachIndexed { i, y ->
                    line(-g, y * g, g, y * g)
                    val x = when (i) { 0 -> -g * 0.35f; 1 -> g * 0.45f; else -> 0f }
                    drawCircle(tint, g * 0.18f, Offset(cx + x, cy + y * g), style = Fill)
                }
            }
            "icons", "layout", "apps" -> {
                val d = g * 0.62f
                listOf(-1 to -1, 1 to -1, -1 to 1, 1 to 1).forEach { (x, y) ->
                    drawRoundRect(
                        tint,
                        topLeft = Offset(cx + x * d - d * 0.5f, cy + y * d - d * 0.5f),
                        size = Size(d, d),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(d * 0.2f),
                        style = Stroke(s)
                    )
                }
            }
            "theme" -> {
                drawCircle(tint, g, Offset(cx, cy), style = Stroke(s))
                drawArc(tint, -90f, 180f, true, Offset(cx - g, cy - g), Size(g * 2f, g * 2f), style = Fill)
            }
            "wallpaper" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - g, cy - g * 0.75f),
                    size = Size(g * 2f, g * 1.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(g * 0.16f),
                    style = Stroke(s)
                )
                drawCircle(tint, g * 0.18f, Offset(cx - g * 0.35f, cy - g * 0.3f), style = Fill)
                val p = Path().apply {
                    moveTo(cx - g * 0.75f, cy + g * 0.55f)
                    lineTo(cx - g * 0.1f, cy - g * 0.1f)
                    lineTo(cx + g * 0.35f, cy + g * 0.35f)
                    lineTo(cx + g * 0.7f, cy + g * 0.05f)
                    lineTo(cx + g * 0.85f, cy + g * 0.55f)
                    close()
                }
                drawPath(p, tint, style = Stroke(s))
            }
            "dock" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - g, cy - g * 0.35f),
                    size = Size(g * 2f, g * 0.7f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(g * 0.35f),
                    style = Stroke(s)
                )
                listOf(-0.45f, 0f, 0.45f).forEach { x ->
                    drawCircle(tint, g * 0.11f, Offset(cx + x * g, cy), style = Fill)
                }
            }
            "widgets" -> {
                drawRoundRect(
                    tint,
                    topLeft = Offset(cx - g, cy - g * 0.75f),
                    size = Size(g * 2f, g * 1.5f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(g * 0.16f),
                    style = Stroke(s)
                )
                line(-g * 0.5f, -g * 0.2f, g * 0.5f, -g * 0.2f)
                line(-g * 0.5f, g * 0.2f, g * 0.1f, g * 0.2f)
            }
            "restart" -> {
                drawArc(tint, 30f, 280f, false, Offset(cx - g, cy - g), Size(g * 2f, g * 2f), style = Stroke(s))
                line(g * 0.55f, -g * 0.9f, g * 0.95f, -g * 0.45f)
                line(g * 0.95f, -g * 0.45f, g * 0.4f, -g * 0.4f)
            }
            "info" -> {
                circle(g)
                drawCircle(tint, g * 0.12f, Offset(cx, cy - g * 0.45f), style = Fill)
                line(0f, -g * 0.05f, 0f, g * 0.55f)
            }
            "bell" -> {
                val p = Path().apply {
                    moveTo(cx - g * 0.7f, cy + g * 0.45f)
                    lineTo(cx - g * 0.55f, cy + g * 0.1f)
                    lineTo(cx - g * 0.55f, cy - g * 0.25f)
                    quadraticBezierTo(cx, cy - g * 0.95f, cx + g * 0.55f, cy - g * 0.25f)
                    lineTo(cx + g * 0.55f, cy + g * 0.1f)
                    lineTo(cx + g * 0.7f, cy + g * 0.45f)
                    close()
                }
                drawPath(p, tint, style = Stroke(s))
                drawArc(tint, 0f, 180f, false, Offset(cx - g * 0.25f, cy + g * 0.4f), Size(g * 0.5f, g * 0.45f), style = Stroke(s))
            }
            "bolt" -> {
                val p = Path().apply {
                    moveTo(cx + g * 0.15f, cy - g)
                    lineTo(cx - g * 0.55f, cy + g * 0.1f)
                    lineTo(cx - g * 0.05f, cy + g * 0.1f)
                    lineTo(cx - g * 0.15f, cy + g)
                    lineTo(cx + g * 0.55f, cy - g * 0.1f)
                    lineTo(cx + g * 0.05f, cy - g * 0.1f)
                    close()
                }
                drawPath(p, tint, style = Stroke(s))
            }
            "shield" -> {
                val p = Path().apply {
                    moveTo(cx, cy - g)
                    lineTo(cx + g * 0.75f, cy - g * 0.65f)
                    lineTo(cx + g * 0.65f, cy + g * 0.2f)
                    quadraticBezierTo(cx, cy + g, cx - g * 0.65f, cy + g * 0.2f)
                    lineTo(cx - g * 0.75f, cy - g * 0.65f)
                    close()
                }
                drawPath(p, tint, style = Stroke(s))
            }
            else -> circle(g)
        }
    }
}

/** A small round accent dot for theme rows. */
@Composable
internal fun AccentDot(color: Color, size: Dp = 18.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(color))
}
