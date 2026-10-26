package com.studentlauncher.ui

import android.graphics.BitmapShader
import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Two materials, one system.
 *
 * [Content] is the flat frosted card widgets read through; [Chrome] is the floating layer
 * (dock, sheets, search, handle, menus, toasts) that may bend light and cast a shadow.
 * Keeping the distinction explicit stops glass-on-glass stacks.
 */
enum class GlassLevel { Content, Chrome }

/** The blurred wallpaper every glass surface samples, plus the theme flag and its luminance. */
@Stable
class GlassEnv(
    val blurred: ImageBitmap?,
    val dark: Boolean,
    screenW: Int = 0,
    /** Average luminance of the wallpaper behind the glass, 0..1. */
    val lum: Float = if (dark) 0.12f else 0.82f
) {
    /** Blurred-bitmap px per screen px; the frosted copy is ~160px wide on a phone. */
    val scale: Float =
        if (blurred != null && screenW > 0) blurred.width.toFloat() / screenW else 1f

    /** One backdrop sampler per wallpaper; the runtime shader reads it. */
    val shader: Shader? by lazy {
        blurred?.let {
            BitmapShader(
                it.asAndroidBitmap(),
                android.graphics.Shader.TileMode.CLAMP,
                android.graphics.Shader.TileMode.CLAMP
            )
        }
    }
}

val LocalGlass = compositionLocalOf { GlassEnv(null, false) }

/**
 * Edge lensing only: the centre is untouched and the outer band bends the blurred wallpaper a
 * little, so the rim reads as a thick material instead of a flat tint. No chromatic fringe and
 * no animated noise; the effect is static and cheap enough for a handful of chrome surfaces.
 */
private const val GLASS_AGSL = """
uniform shader image;
uniform float2 size;
uniform float2 origin;
uniform float radius;
uniform float refr;
uniform float ss;

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float2 q = abs(p) - (halfSize - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float depth = clamp(-d / (radius * 1.2 + 1.0), 0.0, 1.0);
    float bend = pow(1.0 - depth, 2.0) * refr;
    float2 dir = p / (length(p) + 0.001);
    float2 uv = (coord + origin - dir * bend * radius * 0.7) * ss;
    half4 c = image.eval(uv);
    float3 col = float3(c.rgb);
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(lum), col, 1.12);
    // A whisper of static grain keeps large sheets from banding.
    float grain = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
    col += (grain - 0.5) * 0.012;
    return half4(col, 1.0);
}
"""

@RequiresApi(33)
private fun newGlassShader(): RuntimeShader = RuntimeShader(GLASS_AGSL)

@RequiresApi(33)
private fun DrawScope.refract(
    shader: RuntimeShader,
    bitmapShader: Shader,
    origin: Offset,
    r: Float,
    ss: Float
) {
    shader.setInputShader("image", bitmapShader)
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("origin", origin.x, origin.y)
    shader.setFloatUniform("radius", r)
    shader.setFloatUniform("refr", 0.9f)
    shader.setFloatUniform("ss", ss)
    drawRect(ShaderBrush(shader))
}

/**
 * Liquid Glass, restrained.
 *
 * - A high-resolution blurred wallpaper sample (not a live blur).
 * - Edge lensing on Android 13+; every other version keeps the same tint, rim and shadow.
 * - A specular rim: light at the top-left, shade at the bottom-right.
 * - An adaptive tint driven by the wallpaper's luminance.
 * - A soft shadow for chrome only, so content never floats.
 */
fun Modifier.glass(
    radius: Dp = 26.dp,
    level: GlassLevel = GlassLevel.Content
): Modifier = composed {
    val env = LocalGlass.current
    val dark = env.dark
    val chrome = level == GlassLevel.Chrome
    var origin by remember { mutableStateOf(Offset.Zero) }
    val shape = RoundedCornerShape(radius)
    // One shader per surface: uniforms are per-instance, and Compose records draws for later.
    val agsl = remember(chrome) {
        if (chrome && Build.VERSION.SDK_INT >= 33) newGlassShader() else null
    }
    val shadowC = Color.Black.copy(alpha = if (dark) 0.46f else 0.16f)

    val white = if (dark) {
        (if (chrome) 0.07f else 0.045f) + 0.04f * (1f - env.lum.coerceIn(0f, 1f))
    } else {
        (if (chrome) 0.31f else 0.28f) - 0.09f * env.lum.coerceIn(0f, 1f)
    }
    // A dark scrim only when a bright wallpaper sits under a dark theme.
    val scrim = if (dark) (0.04f + 0.20f * env.lum.coerceIn(0f, 1f)) else 0f
    val topSheen = if (dark) 0.05f else 0.18f

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
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val bmp = env.blurred
            val w = size.width
            val h = size.height
            val r = radius.toPx()
            if (bmp != null && w > 0f && h > 0f) {
                val bitmapShader = env.shader
                if (Build.VERSION.SDK_INT >= 33 && agsl != null && bitmapShader != null) {
                    refract(agsl, bitmapShader, origin, r, env.scale)
                } else {
                    val sw = (w * env.scale).roundToInt().coerceAtMost(bmp.width)
                    val sh = (h * env.scale).roundToInt().coerceAtMost(bmp.height)
                    val sx = (origin.x * env.scale).roundToInt().coerceIn(0, max(0, bmp.width - sw))
                    val sy = (origin.y * env.scale).roundToInt().coerceIn(0, max(0, bmp.height - sh))
                    drawImage(
                        bmp,
                        srcOffset = IntOffset(sx, sy),
                        srcSize = IntSize(sw, sh),
                        dstSize = IntSize(w.roundToInt(), h.roundToInt())
                    )
                }
            }
            if (scrim > 0f) drawRect(Color.Black.copy(alpha = scrim))
            drawRect(Color.White.copy(alpha = white))
            if (chrome) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = topSheen),
                        1f to Color.Transparent,
                        endY = h * 0.55f
                    )
                )
            }
        }
        .drawWithContent {
            drawContent()
            val r = radius.toPx()
            if (chrome) {
                drawRoundRect(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.34f else 0.92f),
                        0.35f to Color.White.copy(alpha = if (dark) 0.07f else 0.20f),
                        0.68f to Color.White.copy(alpha = if (dark) 0.02f else 0.08f),
                        1f to if (dark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.12f),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.2.dp.toPx())
                )
            } else {
                drawRoundRect(
                    color = if (dark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.09f),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.dp.toPx())
                )
            }
        }
}
