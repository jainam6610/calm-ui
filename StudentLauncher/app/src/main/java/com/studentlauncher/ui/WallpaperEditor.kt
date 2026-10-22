package com.studentlauncher.ui

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Full-screen wallpaper editor: blur, dim and (for photos) zoom and drag. Saved on "Save". */
@Composable
fun WallpaperEditor(vm: LauncherViewModel) {
    val c = LocalColors.current
    val ctx = LocalContext.current
    val dark = c.dark
    val isPhoto = vm.wallpaper == "photo" || vm.wallpaper == "shuffle"
    val startAdj = vm.currentWpAdjust()
    var blur by remember { mutableFloatStateOf(startAdj.blur) }
    var dim by remember { mutableFloatStateOf(startAdj.dim) }
    var zoom by remember { mutableFloatStateOf(startAdj.zoom) }
    var panX by remember { mutableFloatStateOf(startAdj.panX) }
    var panY by remember { mutableFloatStateOf(startAdj.panY) }
    var base by remember { mutableStateOf<ImageBitmap?>(null) }
    val e = enterProgress()

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val wPx = constraints.maxWidth
        val hPx = constraints.maxHeight
        LaunchedEffect(vm.wallpaper, vm.wallpaperVersion, dark, wPx, hPx) {
            val id = vm.wallpaper
            val slot = vm.shuffleSlot
            base = withContext(Dispatchers.Default) { Wallpapers.build(ctx, id, dark, wPx, hPx, WpAdjust.None, slot).full }
        }

        base?.let { b ->
            Image(
                b, null,
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = panX * (zoom - 1f) * size.width / 2f
                        translationY = panY * (zoom - 1f) * size.height / 2f
                        if (Build.VERSION.SDK_INT >= 31 && blur > 0.01f) renderEffect = BlurEffect(blur * 60f, blur * 60f)
                    }
                    .pointerInput(isPhoto) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            if (isPhoto && zoom > 1.001f) {
                                panX = (panX + drag.x / ((zoom - 1f) * size.width / 2f)).coerceIn(-1f, 1f)
                                panY = (panY + drag.y / ((zoom - 1f) * size.height / 2f)).coerceIn(-1f, 1f)
                            }
                        }
                    },
                contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.fillMaxSize().graphicsLayer { alpha = dim }.background(Color.Black))

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(14.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - e.value) * size.height * 1.2f }
                .glass(32.dp)
                .padding(20.dp)
        ) {
            Txt("Adjust wallpaper", 17f, c.fg, FontWeight.Medium)
            Txt("Blur", 11f, c.fg2, modifier = Modifier.padding(top = 12.dp))
            GlassSlider(blur, { blur = it })
            Txt("Dim", 11f, c.fg2)
            GlassSlider(dim, { dim = it }, range = 0f..0.7f)
            if (isPhoto) {
                Txt("Zoom (drag the image to move it)", 11f, c.fg2)
                GlassSlider(zoom, {
                    zoom = it
                    if (it <= 1.001f) { panX = 0f; panY = 0f }
                }, range = 1f..3f)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Reset", { blur = 0f; dim = 0f; zoom = 1f; panX = 0f; panY = 0f })
                PillButton("Cancel", { vm.wallpaperEditor = false })
                PillButton("Save", {
                    vm.saveWallpaperAdjust(blur, dim, zoom, panX, panY)
                    vm.wallpaperEditor = false
                }, filled = true)
            }
        }
    }
}
