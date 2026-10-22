package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WidgetItem

/** A regular Android app widget (weather, calendar, ...) hosted inside a glass frame. */
@Composable
fun HostedWidget(vm: LauncherViewModel, item: WidgetItem, compact: Boolean, modifier: Modifier = Modifier) {
    val appId = item.get("appWidgetId").toIntOrNull() ?: return
    val info = remember(appId) { vm.hostMgr.info(appId) } ?: return
    val h = widgetFrameHeight(item.rowSpan, compact)
    Box(modifier.fillMaxWidth().height(h).glass(26.dp).clip(RoundedCornerShape(26.dp))) {
        AndroidView(
            factory = { c -> vm.hostMgr.host.createView(c, appId, info) },
            modifier = Modifier.fillMaxSize()
        )
        // Android widgets swallow touches, so settings live behind this small handle.
        Box(
            Modifier.align(Alignment.TopEnd).padding(8.dp).size(28.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.28f)).tap { vm.editWidgetId = item.id },
            contentAlignment = Alignment.Center
        ) { Txt("...", 13f, Color.White) }
    }
}
