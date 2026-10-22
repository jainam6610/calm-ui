package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.R
import com.studentlauncher.data.MAX_WIDGET_ROW_SPAN
import com.studentlauncher.data.WIDGET_COLUMNS
import com.studentlauncher.data.WidgetItem
import kotlin.math.abs

/** Accent corner grip shared by framed and hosted widgets. One gesture resizes both axes. */
@Composable
internal fun WidgetResizeHandle(
    w: WidgetItem,
    vm: LauncherViewModel,
    modifier: Modifier = Modifier
) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val resizeCd = stringResource(R.string.resize_widget)
    Box(
        modifier
            .size(48.dp)
            .semantics { contentDescription = resizeCd }
            .pointerInput(w.id) { // w.id ONLY — a span change must not cancel mid-drag
                awaitEachGesture {
                    var accumV = 0f
                    var accumH = 0f
                    var markV = 0
                    var claimed = false
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val slop = viewConfiguration.touchSlop
                    val startX = down.position.x
                    val startY = down.position.y
                    val stepPx = 36.dp.toPx()
                    val flipPx = 40.dp.toPx()
                    while (true) {
                        val ev = awaitPointerEvent()
                        val ch = ev.changes.first()
                        if (!ch.pressed) break
                        if (!claimed) {
                            if (ch.isConsumed) continue // another gesture took it
                            val dx = ch.position.x - startX
                            val dy = ch.position.y - startY
                            if (abs(dx) <= slop && abs(dy) <= slop) continue
                            accumV = if (dy > 0f) dy - slop else dy + slop
                            accumH = if (dx > 0f) dx - slop else dx + slop
                            claimed = true
                            ch.consume()
                        } else {
                            accumV += ch.position.y - ch.previousPosition.y
                            accumH += ch.position.x - ch.previousPosition.x
                            ch.consume()
                        }

                        // Live commit: one per step, so each step haptics and the running
                        // gesture must survive the recomposition it causes.
                        val cur = vm.findWidget(w.id) ?: break
                        val steps = (accumV / stepPx).toInt()
                        if (steps != markV) {
                            val newH = (cur.rowSpan + steps - markV).coerceIn(1, MAX_WIDGET_ROW_SPAN)
                            if (newH != cur.rowSpan) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.setWidgetRowSpan(w.id, newH)
                            }
                            markV = steps // advance even at the clamp, else drag-back never applies
                        }
                        if (abs(accumH) > flipPx) {
                            val want = if (accumH < 0f) 2 else WIDGET_COLUMNS // left = half, right = full
                            if (want != cur.colSpan) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                vm.updateWidget(w.id) { it.copy(colSpan = want) }
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.BottomEnd
    ) {
        // Corner bracket — fuses with the accent border, legible at any widget height.
        Box(Modifier.size(18.dp)) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(c.accent)
            )
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .width(3.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(c.accent)
            )
        }
    }
}
