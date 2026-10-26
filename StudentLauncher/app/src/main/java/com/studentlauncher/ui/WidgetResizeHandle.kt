package com.studentlauncher.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.MIN_WIDGET_COL_SPAN
import com.studentlauncher.data.WIDGET_COLUMNS
import com.studentlauncher.data.WidgetItem
import kotlin.math.abs

/**
 * The white corner grip from Control Center / Nothing OS. The visible dot is small, the
 * touch target is 56 dp, and the drag is measured against the grip's root origin, so the
 * grip moving under the finger never feeds back into the gesture.
 */
@Composable
internal fun WidgetResizeHandle(
    w: WidgetItem,
    vm: LauncherViewModel,
    compact: Boolean,
    cellWidth: Dp? = null,
    gridRow: Int? = null,
    gridCol: Int? = null,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val resizeCd = stringResource(R.string.resize_widget)
    val gap = WIDGET_GRID_GAP_DP.dp
    val rowStepPx = with(density) { (widgetRowUnit(compact) + gap).toPx() }
    val cellStepPx = with(density) {
        val cell = cellWidth ?: widgetCellWidth((screenWidth - 72).coerceAtLeast(200).dp)
        (cell + gap).toPx()
    }
    var dragging by remember { mutableStateOf(false) }
    var label by remember { mutableStateOf("") }
    // The grip is bottom-end aligned, so it moves as the tile grows. Track its root
    // origin and add the local pointer position to get a coordinate that stays put.
    val origin = remember { mutableStateOf(Offset.Zero) }
    val gripScale by animateFloatAsState(if (dragging) 1.18f else 1f, Motion.Micro, label = "grip")

    Box(
        modifier
            .size(56.dp)
            .onGloballyPositioned { origin.value = it.boundsInRoot().topLeft }
            .semantics { contentDescription = resizeCd }
            .pointerInput(w.id, rowStepPx, cellStepPx) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val start = vm.findWidget(w.id) ?: return@awaitEachGesture
                    val startX = origin.value.x + down.position.x
                    val startY = origin.value.y + down.position.y
                    val startCols = start.colSpan
                    val startRows = start.rowSpan
                    var cols = startCols
                    var rows = startRows
                    var claimed = false
                    // Claim the down so the pager/scroll cannot steal a diagonal drag.
                    down.consume()
                    if (gridRow != null && gridCol != null) vm.anchorWidget(w.id, gridRow, gridCol)
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.firstOrNull { it.id == down.id } ?: ev.changes.firstOrNull() ?: break
                        if (!ch.pressed) break
                        val dx = origin.value.x + ch.position.x - startX
                        val dy = origin.value.y + ch.position.y - startY
                        if (!claimed) {
                            if (abs(dx) <= viewConfiguration.touchSlop && abs(dy) <= viewConfiguration.touchSlop) continue
                            claimed = true
                            dragging = true
                            vm.widgetResizing = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        ch.consume()
                        val nextCols = snappedSpan(
                            startCols, dx, cellStepPx, MIN_WIDGET_COL_SPAN, WIDGET_COLUMNS
                        )
                        val nextRows = snappedSpan(startRows, dy, rowStepPx)
                        if (nextCols != cols || nextRows != rows) {
                            cols = nextCols
                            rows = nextRows
                            label = "$nextCols × $nextRows"
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            vm.resizeWidget(w.id, nextCols, nextRows)
                        }
                    }
                    dragging = false
                    vm.widgetResizing = false
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = dragging && label.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 34.dp)
        ) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) { Txt(label, 10f, Color.White, FontWeight.Medium) }
        }
        Box(
            Modifier
                .padding(8.dp)
                .size(24.dp)
                .graphicsLayer {
                    scaleX = gripScale
                    scaleY = gripScale
                }
                .shadow(6.dp, CircleShape)
                .clip(CircleShape)
                .background(c.fg)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val side = size.width
                drawArc(
                    color = c.knobOn.copy(alpha = 0.88f),
                    startAngle = 0f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(side * 0.30f, side * 0.30f),
                    size = Size(side * 0.52f, side * 0.52f),
                    style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

/** Top-left `−` badge shown while a tile is selected, mirroring iOS/Control Center. */
@Composable
internal fun WidgetRemoveBadge(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val cd = stringResource(R.string.remove_widget)
    Box(
        modifier
            .size(44.dp)
            .semantics { contentDescription = cd }
            .tap {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            Modifier
                .padding(6.dp)
                .size(22.dp)
                .shadow(5.dp, CircleShape)
                .clip(CircleShape)
                .background(c.fg),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(width = 10.dp, height = 2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(c.knobOn)
            )
        }
    }
}
