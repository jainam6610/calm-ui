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

/** The blurred wallpaper every glass surface samples, plus the theme flag. */
@Stable
class GlassEnv(val blurred: ImageBitmap?, val dark: Boolean, screenW: Int = 0) {
    /** Blurred-bitmap px per screen px; the frosted copy lives at ~72/1080, full-res gives 1. */
    val scale: Float =
        if (blurred != null && screenW > 0) blurred.width.toFloat() / screenW else 1f

    val shader: Shader? = blurred?.let {
        BitmapShader(it.asAndroidBitmap(), android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP)
    }
}

val LocalGlass = compositionLocalOf { GlassEnv(null, false) }

private const val GLASS_AGSL = """
uniform shader image;
uniform float2 size;
uniform float2 origin;
uniform float radius;
uniform float refr;
uniform float ss;

half4 main(float2 coord) {
    float2 p = coord - size * 0.5;
    float2 q = abs(p) - (size * 0.5 - float2(radius));
    float d = length(max(q, float2(0.0))) + min(max(q.x, q.y), 0.0) - radius;
    float depth = clamp(-d / (radius * 1.1 + 1.0), 0.0, 1.0);
    float bend = pow(1.0 - depth, 2.4) * refr;
    float2 dir = p / (length(p) + 0.001);
    float2 uv = (coord + origin - dir * bend * radius * 0.85) * ss;
    float ca = bend * 3.0 * ss;
    float r = image.eval(uv + dir * ca).r;
    float g = image.eval(uv).g;
    float b = image.eval(uv - dir * ca).b;
    float3 col = float3(r, g, b);
    float lum = dot(col, float3(0.299, 0.587, 0.114));
    col = mix(float3(lum), col, 1.25);
    return half4(half3(col), 1.0);
}
"""

@RequiresApi(33)
private fun newGlassShader(): Any = RuntimeShader(GLASS_AGSL)

// ponytail: one shared RuntimeShader for every glass surface (UI thread draws are sequential)
private val sharedGlassShader: Any? by lazy {
    if (Build.VERSION.SDK_INT >= 33) newGlassShader() else null
}

@RequiresApi(33)
private fun DrawScope.refract(shaderAny: Any, bitmapShader: Shader, origin: Offset, r: Float, ss: Float) {
    val s = shaderAny as RuntimeShader
    s.setInputShader("image", bitmapShader)
    s.setFloatUniform("size", size.width, size.height)
    s.setFloatUniform("origin", origin.x, origin.y)
    s.setFloatUniform("radius", r)
    s.setFloatUniform("refr", 0.9f)
    s.setFloatUniform("ss", ss)
    drawRect(ShaderBrush(s))
}

/**
 * Liquid glass: frosted blurred wallpaper behind the surface, edge refraction and chromatic
 * fringe (AGSL, Android 13+), a soft tint, a diagonal sheen and a specular rim.
 * Older devices fall back to the frosted copy without refraction.
 */
fun Modifier.glass(radius: Dp = 26.dp): Modifier = composed {
    val env = LocalGlass.current
    val dark = env.dark
    var origin by remember { mutableStateOf(Offset.Zero) }
    val agsl: Any? = if (Build.VERSION.SDK_INT >= 33) sharedGlassShader else null
    val shape = RoundedCornerShape(radius)
    val shadowC = Color.Black.copy(alpha = if (dark) 0.55f else 0.14f)

    this
        .shadow(12.dp, shape, clip = false, ambientColor = shadowC, spotColor = shadowC)
        .onGloballyPositioned { origin = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            val bmp = env.blurred
            val r = radius.toPx()
            val w = size.width
            val h = size.height
            if (bmp != null && w > 0f && h > 0f) {
                val bs = env.shader
                if (Build.VERSION.SDK_INT >= 33 && agsl != null && bs != null) {
                    refract(agsl, bs, origin, r, env.scale)
                } else {
                    val sw = (w * env.scale).roundToInt().coerceAtMost(bmp.width)
                    val sh = (h * env.scale).roundToInt().coerceAtMost(bmp.height)
                    val sx = (origin.x * env.scale).roundToInt().coerceIn(0, max(0, bmp.width - sw))
                    val sy = (origin.y * env.scale).roundToInt().coerceIn(0, max(0, bmp.height - sh))
                    drawImage(bmp, srcOffset = IntOffset(sx, sy), srcSize = IntSize(sw, sh), dstSize = IntSize(w.roundToInt(), h.roundToInt()))
                }
            }
            drawRect(Color.White.copy(alpha = if (dark) 0.06f else 0.26f))
            drawRect(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = if (dark) 0.16f else 0.5f),
                        Color.Transparent,
                        Color.Transparent,
                        Color.White.copy(alpha = if (dark) 0.05f else 0.2f)
                    ),
                    start = Offset.Zero,
                    end = Offset(w, h)
                )
            )
        }
        .drawWithContent {
            drawContent()
            val r = radius.toPx()
            val a = if (dark) 0.42f else 0.95f
            drawRoundRect(
                Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = a),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = 0.05f),
                        Color.White.copy(alpha = a * 0.6f)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                ),
                cornerRadius = CornerRadius(r),
                style = Stroke(1.3.dp.toPx())
            )
            drawRoundRect(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (dark) 0.10f else 0.35f), Color.Transparent),
                    endY = size.height * 0.4f
                ),
                cornerRadius = CornerRadius(r),
                style = Stroke(6.dp.toPx()),
                alpha = 0.5f
            )
        }
}
