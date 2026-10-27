package com.studentlauncher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.time.LocalDate
import kotlin.math.max

class WallpaperSet(val full: ImageBitmap, val blurred: ImageBitmap, val lum: Float)

/** User adjustments: blur 0..1, dim 0..0.7, zoom 1..3, pan -1..1 (photo zoom is centred, then panned). */
class WpAdjust(
    val blur: Float = 0f,
    val dim: Float = 0f,
    val zoom: Float = 1f,
    val panX: Float = 0f,
    val panY: Float = 0f
) {
    companion object { val None = WpAdjust() }
}

object Wallpapers {
    val presets = listOf("dune", "mist", "meadow", "plain")
    const val MAX_SHUFFLE = 5

    private class Blob(val cx: Float, val cy: Float, val r: Float, val color: Int)
    private class Spec(val base: Int, val blobs: List<Blob>)

    private fun spec(id: String, dark: Boolean): Spec = when (id) {
        "dune" -> if (dark) Spec(0xFF15100C.toInt(), listOf(Blob(.15f, .08f, .9f, 0xFF4A3526.toInt()), Blob(.92f, .55f, .8f, 0xFF7A3F2B.toInt()), Blob(.3f, 1f, 1f, 0xFF33261C.toInt())))
        else Spec(0xFFEFE2D3.toInt(), listOf(Blob(.15f, .08f, .9f, 0xFFFFF3E0.toInt()), Blob(.92f, .55f, .8f, 0xFFF0B79A.toInt()), Blob(.3f, 1f, 1f, 0xFFE4C9A8.toInt())))
        "mist" -> if (dark) Spec(0xFF0D1219.toInt(), listOf(Blob(.2f, .15f, .9f, 0xFF1D2C47.toInt()), Blob(.88f, .5f, .8f, 0xFF2B2147.toInt()), Blob(.4f, 1f, 1f, 0xFF14202B.toInt())))
        else Spec(0xFFE4EBF3.toInt(), listOf(Blob(.2f, .15f, .9f, 0xFFC6D6F0.toInt()), Blob(.88f, .5f, .8f, 0xFFEDDFF5.toInt()), Blob(.4f, 1f, 1f, 0xFFD0E2E8.toInt())))
        "meadow" -> if (dark) Spec(0xFF0B120C.toInt(), listOf(Blob(.2f, .1f, .9f, 0xFF1F2F22.toInt()), Blob(.85f, .6f, .8f, 0xFF2E4A26.toInt()), Blob(.4f, 1f, 1f, 0xFF3A5530.toInt())))
        else Spec(0xFFDBE6CF.toInt(), listOf(Blob(.2f, .1f, .9f, 0xFFEEF5DF.toInt()), Blob(.85f, .6f, .8f, 0xFFA9C98A.toInt()), Blob(.4f, 1f, 1f, 0xFF7FA06C.toInt())))
        else -> Spec(if (dark) 0xFF000000.toInt() else 0xFFF2F2F4.toInt(), emptyList())
    }

    fun preset(id: String, dark: Boolean, w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val s = spec(id, dark)
        c.drawColor(s.base)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        for (b in s.blobs) {
            p.shader = RadialGradient(b.cx * w, b.cy * h, b.r * w, b.color, b.color and 0x00FFFFFF, Shader.TileMode.CLAMP)
            c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        }
        return bmp
    }

    // ponytail: slot 0 keeps the legacy wallpaper.jpg name so existing users keep their photo
    private fun photoFile(ctx: Context, slot: Int): File =
        File(ctx.filesDir, if (slot == 0) "wallpaper.jpg" else "wallpaper$slot.jpg")

    /**
     * Pure: the smallest power-of-two [BitmapFactory] sample that still covers [target] on the
     * long edge. Lets [build] avoid decoding a full-resolution photo just to crop it to screen
     * size, which is the single largest allocation in the wallpaper path.
     */
    fun sampleSizeFor(srcW: Int, srcH: Int, target: Int): Int {
        if (srcW <= 0 || srcH <= 0 || target <= 0) return 1
        var s = 1
        while (max(srcW, srcH) / (s * 2) >= target) s *= 2
        return s
    }

    /** Which shuffle slot to show for this local day. Pure: unit-testable via epochDay. */
    fun slotForDay(count: Int, epochDay: Long = LocalDate.now().toEpochDay()): Int {
        if (count <= 1) return 0
        return (epochDay % count).toInt().coerceIn(0, count - 1)
    }

    fun hasPhoto(ctx: Context, slot: Int): Boolean = photoFile(ctx, slot).exists()

    fun savedCount(ctx: Context): Int =
        (MAX_SHUFFLE downTo 1).firstOrNull { photoFile(ctx, it - 1).exists() } ?: 0

    /** Decoded photo for widget drawing, sampled ~4x down to keep memory small. */
    fun photoBitmap(ctx: Context, slot: Int): Bitmap? = runCatching {
        val f = photoFile(ctx, slot)
        if (!f.exists()) null
        else BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = 4 })
    }.getOrNull()

    /** Copies the picked image (downscaled) into app storage so it survives permission loss. */
    fun savePhoto(ctx: Context, uri: Uri, slot: Int = 0): Boolean = runCatching {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1
        while (max(opts.outWidth, opts.outHeight) / sample > 2400) sample *= 2
        val bmp = ctx.contentResolver.openInputStream(uri)!!.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }!!
        photoFile(ctx, slot).outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        true
    }.getOrDefault(false)

    fun removePhoto(ctx: Context, slot: Int): Boolean =
        photoFile(ctx, slot).delete() || !photoFile(ctx, slot).exists()

    /**
     * Move photos into dense slots 0..n-1 after a hole (middle remove).
     * Safe: renames remaining files down; deletes any trailing leftovers.
     */
    fun compactPhotos(ctx: Context) {
        var write = 0
        for (read in 0 until MAX_SHUFFLE) {
            if (!photoFile(ctx, read).exists()) continue
            if (write != read) {
                val from = photoFile(ctx, read)
                val to = photoFile(ctx, write)
                from.renameTo(to)
            }
            write++
        }
        for (slot in write until MAX_SHUFFLE) removePhoto(ctx, slot)
    }

    /** Copy bundled preload WebPs to filesDir slots 0..n-1 only if those slots are empty. */
    fun seedPreloads(ctx: Context) {
        val seeds = listOf(com.studentlauncher.R.raw.preload_sky, com.studentlauncher.R.raw.preload_forest)
        seeds.forEachIndexed { i, rawId ->
            val dest = photoFile(ctx, i)
            if (!dest.exists()) {
                runCatching {
                    ctx.resources.openRawResource(rawId).use { src ->
                        dest.outputStream().use { src.copyTo(it) }
                    }
                }
            }
        }
    }

    /** Samples dominant hue from the wallpaper bitmap to produce an adaptive accent colour. */
    fun dominantColor(bmp: Bitmap): androidx.compose.ui.graphics.Color {
        return runCatching {
            val small = Bitmap.createScaledBitmap(bmp, 16, 16, false)
            val buckets = FloatArray(16)
            val hsv = FloatArray(3)
            var maxCount = 0f
            var domHue = 0f
            for (x in 0 until 16 step 2) {
                for (y in 0 until 16 step 2) {
                    val p = small.getPixel(x, y)
                    Color.colorToHSV(p, hsv)
                    if (hsv[1] >= 0.15f && hsv[2] in 0.15f..0.95f) {
                        val idx = ((hsv[0] / 360f) * 16).toInt().coerceIn(0, 15)
                        buckets[idx] += 1f
                        if (buckets[idx] > maxCount) {
                            maxCount = buckets[idx]
                            domHue = (idx + 0.5f) * (360f / 16f)
                        }
                    }
                }
            }
            if (small != bmp) small.recycle()
            if (maxCount > 0f) {
                val rgb = Color.HSVToColor(floatArrayOf(domHue, 0.75f, 0.9f))
                androidx.compose.ui.graphics.Color(rgb)
            } else {
                androidx.compose.ui.graphics.Color(0xFFE50914)
            }
        }.getOrDefault(androidx.compose.ui.graphics.Color(0xFFE50914))
    }

    private fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val s = max(w / src.width.toFloat(), h / src.height.toFloat())
        val dw = src.width * s
        val dh = src.height * s
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(src, null, RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f), Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    fun build(ctx: Context, id: String, dark: Boolean, w: Int, h: Int, adj: WpAdjust = WpAdjust.None, slot: Int = 0): WallpaperSet {
        var base: Bitmap? = null
        if (id == "photo" || id == "shuffle") {
            val f = photoFile(ctx, if (id == "shuffle") slot else 0)
            if (f.exists()) {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(f.path, bounds)
                val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight, max(w, h))
                val src = BitmapFactory.decodeFile(f.path, BitmapFactory.Options().apply { inSampleSize = sample })
                if (src != null) {
                    val cropped = centerCrop(src, w, h)
                    Canvas(cropped).drawColor(if (dark) 0x66000000 else 0x22FFFFFF)
                    base = cropped
                }
            }
        }
        val fallback = if (id == "shuffle" || id == "photo") "dune" else id
        var full: Bitmap = base ?: preset(fallback, dark, w, h)
        if (adj.zoom > 1.001f) full = zoomPan(full, adj)
        if (adj.blur > 0.01f) full = softBlur(full, adj.blur)
        if (adj.dim > 0.01f) Canvas(full).drawColor((adj.dim.coerceIn(0f, 0.8f) * 255).toInt() shl 24)
        val soft = blur(full)
        return WallpaperSet(full.asImageBitmap(), soft.asImageBitmap(), luminance(soft))
    }

    private fun zoomPan(src: Bitmap, a: WpAdjust): Bitmap {
        val w = src.width
        val h = src.height
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val m = Matrix()
        m.postScale(a.zoom, a.zoom, w / 2f, h / 2f)
        m.postTranslate(a.panX * (a.zoom - 1f) * w / 2f, a.panY * (a.zoom - 1f) * h / 2f)
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }

    /** The wallpaper the user sees, softened by [amount] (0..1). */
    private fun softBlur(src: Bitmap, amount: Float): Bitmap {
        val a = amount.coerceIn(0f, 1f)
        val sw = (src.width / 2f + (28f - src.width / 2f) * a).toInt().coerceAtLeast(24)
        return blurTo(src, sw, 2, 2)
    }

    /**
     * Frosted copy of the wallpaper, kept at 200px wide. Every glass surface samples it, so it is
     * the one blur the launcher pays for; 200px keeps a little of the photo's shape at low cost.
     */
    fun blur(src: Bitmap): Bitmap = blurTo(src, 200, 3, 3, upscale = false)

    /** Mean perceived luminance of the wallpaper, sampled sparsely. */
    private fun luminance(src: Bitmap): Float {
        val w = src.width
        val h = src.height
        if (w <= 0 || h <= 0) return 0.5f
        val step = max(1, minOf(w, h) / 24)
        var sum = 0.0
        var n = 0
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val c = src.getPixel(x, y)
                val a = ((c ushr 24) and 255) / 255f
                val r = ((c shr 16) and 255) / 255f
                val g = ((c shr 8) and 255) / 255f
                val b = (c and 255) / 255f
                sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) * a
                n++
                x += step
            }
            y += step
        }
        return if (n == 0) 0.5f else (sum / n).toFloat().coerceIn(0f, 1f)
    }

    private fun blurTo(src: Bitmap, sw: Int, r: Int, passes: Int, upscale: Boolean = true): Bitmap {
        var b = src
        while (b.width / 2 >= sw) b = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
        val sh = max(1, (src.height * sw / src.width.toFloat()).toInt())
        val small = Bitmap.createScaledBitmap(b, sw, sh, true).copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(sw * sh)
        small.getPixels(px, 0, sw, 0, 0, sw, sh)
        repeat(passes) { boxBlur(px, sw, sh, r) }
        small.setPixels(px, 0, sw, 0, 0, sw, sh)
        if (!upscale) return small
        return Bitmap.createScaledBitmap(small, src.width, src.height, true).copy(Bitmap.Config.ARGB_8888, true)
    }

    private fun boxBlur(p: IntArray, w: Int, h: Int, r: Int) {
        val tmp = IntArray(p.size)
        for (y in 0 until h) for (x in 0 until w) {
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (k in -r..r) {
                val c = p[y * w + (x + k).coerceIn(0, w - 1)]
                a += c ushr 24; rr += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
            }
            val n = 2 * r + 1
            tmp[y * w + x] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
        }
        for (y in 0 until h) for (x in 0 until w) {
            var a = 0; var rr = 0; var g = 0; var b = 0
            for (k in -r..r) {
                val c = tmp[(y + k).coerceIn(0, h - 1) * w + x]
                a += c ushr 24; rr += (c shr 16) and 255; g += (c shr 8) and 255; b += c and 255
            }
            val n = 2 * r + 1
            p[y * w + x] = ((a / n) shl 24) or ((rr / n) shl 16) or ((g / n) shl 8) or (b / n)
        }
    }
}
