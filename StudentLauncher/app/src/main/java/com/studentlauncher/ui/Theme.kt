package com.studentlauncher.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

val NothingRed = Color(0xFFD71921)

// ponytail: centralised motion/spacing/type so every call site stays consistent
object Motion {
    /** Sheets, panels, overlays — calm, not bouncy. */
    val Macro = spring<Float>(dampingRatio = 0.85f, stiffness = 320f)
    /** Press, scrub, toggle — snappy micro-feedback. */
    val Micro = spring<Float>(dampingRatio = 0.7f, stiffness = 500f)
    /** Size animations (animateContentSize). */
    val Size = spring<androidx.compose.ui.unit.IntSize>(dampingRatio = 0.85f, stiffness = 320f)
    /** Offset animations (slideIn/slideOut). */
    val Offset = spring<androidx.compose.ui.unit.IntOffset>(dampingRatio = 0.85f, stiffness = 320f)
}

object Dim {
    val PaddingXs = 6.dp; val PaddingSm = 8.dp; val PaddingMd = 14.dp
    val PaddingLg = 20.dp; val PaddingXl = 24.dp
    val CardRadius = 12.dp; val SheetRadius = 24.dp; val PillRadius = 24.dp
    val WidgetGap = 10.dp
}

object Type {
    val Kicker = 11f; val Caption = 12f; val Body = 14f
    val Subhead = 15f; val Title = 20f; val Large = 25f
}

@Immutable
class LauncherColors(
    val dark: Boolean,
    val fg: Color,
    val fg2: Color,
    val tile: Color,
    val track: Color,
    val knob: Color,
    val knobOn: Color,
    val accent: Color
)

fun launcherColors(dark: Boolean, accent: Color = NothingRed) = if (dark) {
    LauncherColors(true, Color(0xFFF5F5F7), Color(0x99F5F5F7), Color(0x1AFFFFFF), Color(0x2EFFFFFF), Color.White, Color(0xFF111111), accent)
} else {
    LauncherColors(false, Color(0xFF1C1C1E), Color(0x8C1C1C1E), Color(0x66FFFFFF), Color(0x291C1C1E), Color.White, Color.White, accent)
}

fun accentColor(id: String): Color = when (id) {
    "blue" -> Color(0xFF2E7CF6)
    "green" -> Color(0xFF34C759)
    "purple" -> Color(0xFFAF52DE)
    "orange" -> Color(0xFFFF9500)
    "teal" -> Color(0xFF30B0C7)
    "pink" -> Color(0xFFFF2D55)
    else -> NothingRed
}

fun fontFamilyOf(id: String): FontFamily = when (id) {
    "serif" -> FontFamily.Serif
    "mono" -> FontFamily.Monospace
    // OFL fonts bundled in res/font (see app/OFL-POPPINS.txt)
    "poppins" -> FontFamily(
        Font(com.studentlauncher.R.font.poppins_light, FontWeight.Light),
        Font(com.studentlauncher.R.font.poppins_regular, FontWeight.Normal),
        Font(com.studentlauncher.R.font.poppins_medium, FontWeight.Medium)
    )
    // OFL fonts bundled in res/font (see app/OFL-GEIST.txt)
    "geist" -> FontFamily(
        Font(com.studentlauncher.R.font.geist_light, FontWeight.Light),
        Font(com.studentlauncher.R.font.geist_regular, FontWeight.Normal),
        Font(com.studentlauncher.R.font.geist_medium, FontWeight.Medium)
    )
    else -> FontFamily.Default
}

val LocalFontFamily = staticCompositionLocalOf<FontFamily> { FontFamily.Default }

val LocalColors = staticCompositionLocalOf { launcherColors(false) }
/** 0 means Android reduced motion is enabled; 1 is the normal calm motion profile. */
val LocalMotionScale = staticCompositionLocalOf { 1f }

/** Text colour choice for a widget: theme, always light, always dark, or an accent name. */
fun toneColor(tone: String, c: LauncherColors): Color = when (tone) {
    "light" -> Color.White
    "dark" -> Color(0xFF1C1C1E)
    "red" -> c.accent
    "blue" -> Color(0xFF2E7CF6)
    "green" -> Color(0xFF34C759)
    "purple" -> Color(0xFFAF52DE)
    else -> c.fg
}

@Composable
fun Txt(
    text: String,
    size: Float = 14f,
    color: Color = LocalColors.current.fg,
    weight: FontWeight = FontWeight.Normal,
    modifier: Modifier = Modifier,
    maxLines: Int = 1,
    align: TextAlign = TextAlign.Unspecified,
    strike: Boolean = false
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            color = color,
            fontSize = size.sp,
            fontWeight = weight,
            fontFamily = LocalFontFamily.current,
            textAlign = align,
            textDecoration = if (strike) TextDecoration.LineThrough else TextDecoration.None
        ),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

/** Click without a ripple; the glass UI uses springs instead. */
fun Modifier.tap(onClick: () -> Unit): Modifier = composed {
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Button, onClick = onClick)
}

/** 0 -> 1 spring used for enter animations of overlays. */
@Composable
fun enterProgress(spec: AnimationSpec<Float> = Motion.Macro): State<Float> {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, spec) }
    return a.asState()
}

@Composable
fun <T> Segmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val idx = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val pos by animateFloatAsState(idx.toFloat(), Motion.Micro, label = "seg")
    // 48 dp touch target; the visible track stays 32 dp tall.
    BoxWithConstraints(modifier.fillMaxWidth().height(48.dp)) {
        val segW = maxWidth / options.size
        Box(
            Modifier.align(Alignment.Center).fillMaxWidth().height(32.dp)
                .clip(RoundedCornerShape(10.dp)).background(c.track)
        )
        Box(
            Modifier.align(Alignment.CenterStart)
                .offset { IntOffset((segW * pos + 2.dp).roundToPx(), 0) }
                .width((segW - 4.dp).coerceAtLeast(0.dp))
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp)).background(c.knob)
        )
        Row(Modifier.fillMaxSize().selectableGroup()) {
            options.forEach { (v, label) ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(
                        selected = v == selected,
                        role = Role.RadioButton,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(v)
                        }
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Txt(label, 12f, if (v == selected) Color(0xFF1C1C1E) else c.fg)
                }
            }
        }
    }
}

/** A 34 dp visual chip inside a 48 dp touch target. */
@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    Box(
        modifier.height(48.dp).selectable(
            selected = selected,
            role = Role.RadioButton,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(8.dp))
                .background(if (selected) c.knob else c.track),
            contentAlignment = Alignment.Center
        ) {
            Txt(label, 11f, if (selected) Color(0xFF1C1C1E) else c.fg, maxLines = 1)
        }
    }
}

@Composable
fun GlassSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val x by animateDpAsState(if (on) 20.dp else 2.dp, spring(dampingRatio = 0.7f, stiffness = 500f), label = "sw")
    val bg by animateColorAsState(if (on) c.fg else c.track, label = "swbg")
    // Invisible 48 dp touch box around the unchanged 44 x 26 visual.
    Box(Modifier.size(48.dp).toggleable(
        value = on,
        role = Role.Switch,
        onValueChange = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange(it)
        }
    ), contentAlignment = Alignment.Center) {
        Box(Modifier.width(44.dp).height(26.dp).clip(CircleShape).background(bg)) {
            Box(Modifier.offset { IntOffset(x.roundToPx(), 2.dp.roundToPx()) }.size(22.dp).clip(CircleShape).background(if (on) c.knobOn else c.knob))
        }
    }
}

@Composable
fun GlassSlider(
    value: Float,
    onChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    range: ClosedFloatingPointRange<Float> = 0f..1f
) {
    val c = LocalColors.current
    var w by remember { mutableIntStateOf(1) }
    val cb by rememberUpdatedState(onChange)
    val frac = ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0f, 1f)
    Box(
        modifier.fillMaxWidth().height(48.dp).onSizeChanged { w = it.width }.pointerInput(range) {
            while (true) {
                awaitEachGesture {
                    val d = awaitFirstDown(requireUnconsumed = false)
                    d.consume()
                    fun set(x: Float) {
                        cb(range.start + (range.endInclusive - range.start) * (x / w).coerceIn(0f, 1f))
                    }
                    set(d.position.x)
                    do {
                        val e = awaitPointerEvent()
                        val ch = e.changes.first()
                        set(ch.position.x)
                        ch.consume()
                    } while (e.changes.any { it.pressed })
                }
            }
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(c.track))
        Box(Modifier.fillMaxWidth(frac).height(6.dp).clip(CircleShape).background(c.fg))
        Box(
            Modifier
                .offset { IntOffset(((w - 26.dp.roundToPx()) * frac).roundToInt(), 0) }
                .size(26.dp).clip(CircleShape).background(Color.White)
                .border(0.5.dp, c.fg2.copy(alpha = 0.3f), CircleShape)
        )
    }
}

@Composable
fun GlassField(
    value: String,
    hint: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    numeric: Boolean = false
) {
    val c = LocalColors.current
    Box(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.track)
            .padding(horizontal = 16.dp, vertical = 13.dp)
    ) {
        if (value.isEmpty()) Txt(hint, 15f, c.fg2)
        BasicTextField(
            value, onChange, singleLine = true,
            textStyle = TextStyle(color = c.fg, fontSize = 15.sp),
            cursorBrush = SolidColor(c.fg),
            keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    danger: Boolean = false
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val bg = if (filled) c.fg else c.track
    val fg = if (filled) c.knobOn else if (danger) c.accent else c.fg
    Box(
        modifier.height(48.dp).clip(CircleShape).background(bg).tap {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        }.padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) { Txt(label, 13f, fg) }
}
