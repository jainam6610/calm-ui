package com.studentlauncher.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.AppIcons
import com.studentlauncher.data.AppInfo
import com.studentlauncher.data.IconPack
import com.studentlauncher.data.IconShape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Resolution of the Dot pack grid. */
internal const val DOT_GRID = 12

private val PACK_GRAY = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

/**
 * Icon rendering for the built-in packs. Everything derives from the app's monochrome/themed
 * layer when it ships one; apps without a themed layer fall back to [IconPack.Original].
 */
@Composable
fun AppIconView(
    app: AppInfo,
    vm: LauncherViewModel,
    size: Dp,
    modifier: Modifier = Modifier,
    packOverride: IconPack? = null,
    shapeOverride: IconShape? = null
) {
    val c = LocalColors.current
    val ic = vm.icons[app.pkg]
    val requested = packOverride ?: vm.iconPack
    val shapeKind = shapeOverride ?: vm.iconShape
    val glyph = ic?.mono
    // The stroke/grid packs need a themed glyph; without one they render like Original.
    val pack = when {
        requested == IconPack.Hairline || requested == IconPack.Bold || requested == IconPack.Dot ->
            if (glyph == null) IconPack.Original else requested
        else -> requested
    }

    val shape: Shape = when (shapeKind) {
        IconShape.Circle -> CircleShape
        IconShape.Squircle -> RoundedCornerShape(size * 0.32f)
        IconShape.None -> RectangleShape
    }
    val tileShape = RoundedCornerShape(size * 0.28f)
    val chip = if (shapeKind == IconShape.None) Modifier else Modifier
        .clip(shape)
        .background(c.tile)
        .border(
            0.8.dp,
            Brush.linearGradient(listOf(Color.White.copy(alpha = if (c.dark) 0.35f else 0.9f), Color.White.copy(alpha = 0.08f))),
            shape
        )

    val container = when (pack) {
        IconPack.Bubble -> Modifier
            .clip(CircleShape)
            .background(c.tile)
            .border(1.2.dp, c.fg2.copy(alpha = 0.35f), CircleShape)
        IconPack.Tile -> Modifier.clip(tileShape).background(c.accent.copy(alpha = 0.22f))
        IconPack.Hairline, IconPack.Bold, IconPack.Dot -> Modifier
        else -> chip
    }

    Box(modifier.size(size).then(container), contentAlignment = Alignment.Center) {
        if (ic == null) return@Box
        when (pack) {
            IconPack.Original ->
                Image(ic.original, null, Modifier.fillMaxSize().padding(originalPad(shapeKind, size)))
            IconPack.Mono ->
                TintedGlyph(ic, c.fg, originalPad(shapeKind, size))
            IconPack.Bubble ->
                TintedGlyph(ic, c.fg, size * 0.26f)
            IconPack.Tile ->
                TintedGlyph(ic, c.accent, size * 0.26f)
            IconPack.Hairline ->
                OutlineGlyph(glyph!!, c.fg, 1.5.dp, Modifier.fillMaxSize().padding(size * 0.14f))
            IconPack.Bold ->
                OutlineGlyph(glyph!!, c.fg, 2.6.dp, Modifier.fillMaxSize().padding(size * 0.14f))
            IconPack.Dot ->
                DotGlyph(ic, c.fg, Modifier.fillMaxSize().padding(size * 0.16f))
        }
    }
}

private fun originalPad(shape: IconShape, size: Dp): Dp =
    if (shape == IconShape.None) 0.dp else size * 0.2f

/** Mono layer tinted with [tint]; falls back to a grayscale original when no themed layer exists. */
@Composable
private fun TintedGlyph(ic: AppIcons, tint: Color, pad: Dp) {
    val mono = ic.mono
    val m = Modifier.fillMaxSize().padding(pad)
    if (mono != null) {
        Image(mono, null, m, colorFilter = ColorFilter.tint(tint))
    } else {
        Image(ic.original, null, m.clip(RoundedCornerShape(pad)), colorFilter = PACK_GRAY)
    }
}

/** 12x12 dot grid sampled from the themed glyph's coverage. */
@Composable
private fun DotGlyph(ic: AppIcons, color: Color, modifier: Modifier) {
    val grid = DOT_GRID
    val mask = ic.dotMask(grid)
    Canvas(modifier) {
        val cell = size.minDimension / grid
        val ox = (size.width - cell * grid) / 2f
        val oy = (size.height - cell * grid) / 2f
        val r = cell * 0.30f
        for (y in 0 until grid) {
            for (x in 0 until grid) {
                if (mask[y * grid + x]) {
                    drawCircle(color, r, Offset(ox + (x + 0.5f) * cell, oy + (y + 0.5f) * cell))
                }
            }
        }
    }
}

/**
 * Smooth outline of the themed glyph: the silhouette is dilated by drawing copies around a small
 * circle, then the original is punched out with [BlendMode.DstOut] inside an offscreen layer so
 * only a ring of thickness [strokeWidth] remains.
 */
@Composable
private fun OutlineGlyph(glyph: ImageBitmap, color: Color, strokeWidth: Dp, modifier: Modifier) {
    val sw = with(LocalDensity.current) { strokeWidth.toPx() }
    Canvas(modifier.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
        val steps = 12
        for (i in 0 until steps) {
            val a = (i.toFloat() / steps) * 2f * PI.toFloat()
            drawImage(
                image = glyph,
                topLeft = Offset((cos(a) * sw), (sin(a) * sw)),
                colorFilter = ColorFilter.tint(color)
            )
        }
        drawImage(image = glyph, topLeft = Offset.Zero, blendMode = BlendMode.DstOut)
    }
}
