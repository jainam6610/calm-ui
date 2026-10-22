package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.WidgetItem
import kotlin.math.roundToInt

/** A regular Android app widget (weather, calendar, ...) hosted inside a glass frame. */
@Composable
fun HostedWidget(vm: LauncherViewModel, item: WidgetItem, compact: Boolean, modifier: Modifier = Modifier) {
    val appId = item.get("appWidgetId").toIntOrNull() ?: return
    val info = remember(appId) { vm.hostMgr.info(appId) } ?: return
    val c = LocalColors.current
    val density = LocalDensity.current
    val selected = vm.selectedWidgetId == item.id
    val settingsCd = stringResource(R.string.widget_settings)
    val h = widgetFrameHeight(item.rowSpan, compact)
    Box(modifier.fillMaxWidth().height(h).glass(26.dp).clip(RoundedCornerShape(26.dp))) {
        AndroidView(
            factory = { ctx -> vm.hostMgr.host.createView(ctx, appId, info) },
            modifier = Modifier.fillMaxSize().onSizeChanged { size ->
                vm.updateHostedWidgetOptions(
                    item.id,
                    with(density) { size.width.toDp().value }.roundToInt(),
                    with(density) { size.height.toDp().value }.roundToInt()
                )
            }
        )
        if (selected) {
            Box(
                Modifier.matchParentSize()
                    .border(1.dp, c.accent.copy(alpha = 0.6f), RoundedCornerShape(26.dp))
            )
            WidgetResizeHandle(
                w = item,
                vm = vm,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp)
            )
        }
        // Android widgets swallow touches, so settings live behind this small handle.
        Box(
            Modifier.align(Alignment.TopEnd).padding(4.dp).size(48.dp)
                .semantics { contentDescription = settingsCd }
                .tap {
                    vm.selectedWidgetId = item.id
                    vm.editWidgetId = item.id
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(28.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.28f)),
                contentAlignment = Alignment.Center
            ) { Txt("...", 13f, Color.White) }
        }
    }
}
