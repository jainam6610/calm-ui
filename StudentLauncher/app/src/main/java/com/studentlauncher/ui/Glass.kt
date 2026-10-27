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
import androidx.compose.ui.geometry.Size
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
 * Two depths, one material.
 *
 * [Content] is the quieter card widgets read through; [Chrome] is the floating layer (dock,
 * sheets, search, handle, menus, toasts) that bends light at its rim and casts a shadow.
 * Keeping the distinction explicit stops glass-on-glass stacks from looking like mush.
 */
enum class GlassLevel { Content, Chrome }

/** The blurred wallpaper every glass surface samples, plus the theme flag and its luminance. */
@Stable
class GlassEnv(
    val blurred: ImageBitmap?,
    val dark: Boolean,
    screenW: Int = 0,
    /** Average luminance of the wallpaper behind the glass, 0..1. */
    val lum: Float = if (dark) 0.18f else 0.82f
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
 * Liquid Glass, held back from becoming coloured glass.
 *
 * The backdrop is the blurred wallpaper, so the surface is genuinely translucent. Three things
 * keep it from tinting the interface: the sample is desaturated towards its own luminance, a
 * neutral veil is laid over it, and the veil follows the theme rather than the photo. The rim
 * bends and very slightly splits colour like thick glass, which reads as a material instead of a
 * flat tint. Everything is static — no animation, no noise over time.
 */
private const val GLASS_AGSL = """
uniform shader image;
uniform float2 size;
uniform float2 origin;
uniform float radius;
uniform float refr;
uniform float ss;
uniform float sat;

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 p = coord - halfSize;
    float2 q = abs(p) - (halfSize - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float depth = clamp(-d / (radius * 1.2 + 1.0), 0.0, 1.0);
    float bend = pow(1.0 - depth, 2.0) * refr;
    float2 dir = p / (length(p) + 0.001);
    float2 uv = coord + origin - dir * bend * radius * 0.7;

    half4 c = image.eval(uv * ss);
    // A hair of dispersion, only where the rim is actually bending.
    float ca = bend * 1.8;
    if (ca > 0.05) {
        float2 off = dir * ca;
        c.r = image.eval((uv + off) * ss).r;
        c.b = image.eval((uv - off) * ss).b;
    }

    float3 col = float3(c.rgb);
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(lum), col, sat);
    // Glass adds a little vibrancy of its own instead of washing the scene out.
    col = clamp((col - 0.5) * 1.08 + 0.52, 0.0, 1.0);
    // A whisper of static grain keeps large sheets from banding.
    float grain = fract(sin(dot(coord, float2(12.9898, 78.233))) * 43758.5453);
    col += (grain - 0.5) * 0.006;
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
    ss: Float,
    sat: Float
) {
    shader.setInputShader("image", bitmapShader)
    shader.setFloatUniform("size", size.width, size.height)
    shader.setFloatUniform("origin", origin.x, origin.y)
    shader.setFloatUniform("radius", r)
    shader.setFloatUniform("refr", 0.9f)
    shader.setFloatUniform("ss", ss)
    shader.setFloatUniform("sat", sat)
    drawRect(ShaderBrush(shader))
}

/**
 * How much of the wallpaper's colour survives into the glass. Low enough that a photo cannot
 * recolour the chrome, high enough that the surface still sits in the scene.
 */
private const val GLASS_SAT = 0.68f

/**
 * Liquid Glass.
 *
 * - A blurred wallpaper sample, not a live blur, so the cost is one bitmap per wallpaper.
 * - Chrome bends light at its edge with a hint of dispersion on Android 13+; it also gets a soft
 *   shadow. Widgets and cards take the cheaper plain crop, since a board can hold twenty of them.
 * - A neutral veil that follows the theme, which is what stops a photo from tinting panels.
 * - A specular rim and an inner bevel: light at the top-left, shade at the bottom-right.
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
    // Only the handful of chrome surfaces bend light; widgets and cards take the cheap crop path,
    // because there can be twenty of them on the board at once.
    val agsl = remember(chrome) {
        if (chrome && Build.VERSION.SDK_INT >= 33) newGlassShader() else null
    }
    val shadowC = Color.Black.copy(alpha = if (dark) 0.46f else 0.16f)
    val lum = env.lum.coerceIn(0f, 1f)

    // The veil is the theme's own surface, not the photo's colour. It is heavier under text.
    val veilBase = if (dark) Color(0xFF17171C) else Color(0xFFF7F7FA)
    val veil = if (dark) {
        (if (chrome) 0.24f else 0.28f) + 0.10f * lum
    } else {
        (if (chrome) 0.38f else 0.44f) + 0.08f * (1f - lum)
    }
    // Lift the surface a touch so dark glass reads as glass rather than a hole.
    val lift = if (dark) {
        (if (chrome) 0.11f else 0.08f) + 0.03f * (1f - lum)
    } else {
        (if (chrome) 0.26f else 0.22f) - 0.06f * lum
    }
    val topSheen = if (dark) 0.11f else 0.22f

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
                    refract(agsl, bitmapShader, origin, r, env.scale, GLASS_SAT)
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
            } else {
                drawRect(veilBase)
            }
            drawRect(veilBase.copy(alpha = veil))
            drawRect(Color.White.copy(alpha = lift))
            // Specular streak: the light the material catches, strongest along the top-left edge.
            drawRect(
                Brush.linearGradient(
                    0f to Color.White.copy(alpha = topSheen),
                    0.42f to Color.White.copy(alpha = topSheen * 0.22f),
                    0.72f to Color.Transparent,
                    1f to Color.White.copy(alpha = topSheen * 0.30f),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
            // Thickness: glass darkens towards its lower edge, which is what makes it a slab.
            drawRect(
                Brush.verticalGradient(
                    0.62f to Color.Transparent,
                    1f to Color.Black.copy(alpha = if (dark) 0.16f else 0.07f),
                    endY = h
                )
            )
        }
        .drawWithContent {
            drawContent()
            val r = radius.toPx()
            if (chrome) {
                drawRoundRect(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.38f else 0.95f),
                        0.32f to Color.White.copy(alpha = if (dark) 0.10f else 0.24f),
                        0.66f to Color.White.copy(alpha = if (dark) 0.03f else 0.10f),
                        1f to if (dark) Color.White.copy(alpha = 0.07f)
                        else Color.Black.copy(alpha = 0.14f),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.2.dp.toPx())
                )
            } else {
                drawRoundRect(
                    Brush.linearGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.32f else 0.85f),
                        0.38f to Color.White.copy(alpha = if (dark) 0.08f else 0.20f),
                        0.72f to Color.White.copy(alpha = if (dark) 0.03f else 0.06f),
                        1f to if (dark) Color.White.copy(alpha = 0.06f)
                        else Color.Black.copy(alpha = 0.10f),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height)
                    ),
                    cornerRadius = CornerRadius(r),
                    style = Stroke(1.dp.toPx())
                )
            }
            // Inner bevel one pixel in: bright along the top, shaded along the bottom. This is the
            // cue that reads as thickness even when the wallpaper behind the panel is flat.
            val inset = 1.dp.toPx()
            drawRoundRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (dark) 0.20f else 0.55f),
                    0.45f to Color.Transparent,
                    1f to Color.Black.copy(alpha = if (dark) 0.26f else 0.07f)
                ),
                topLeft = Offset(inset, inset),
                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                cornerRadius = CornerRadius((r - inset).coerceAtLeast(1f)),
                style = Stroke(1.dp.toPx())
            )
        }
}
