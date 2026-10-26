package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studentlauncher.LauncherViewModel
import com.studentlauncher.data.WIDGET_COLUMNS
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetItem
import com.studentlauncher.data.flowRows
import com.studentlauncher.data.flowRowsOfSpans
import com.studentlauncher.data.rowUsedColumns
import kotlinx.coroutines.flow.first

/**
 * Pure swap step for board drag. Returns (indexDelta, newOffset) when the dragged
 * card's center has moved past half the neighbor's measured height, else null.
 * Defaults keep single-column ±1 behavior; board passes row-aware deltas.
 * Height threshold uses (hDragged + hNeighbor) / 2 so unequal rows don't jump early.
 */
internal fun boardSwap(
    offset: Float,
    heightUp: Float?,
    heightDown: Float?,
    deltaUp: Int = -1,
    deltaDown: Int = 1,
    heightDragged: Float? = null
): Pair<Int, Float>? {
    fun threshold(neighbor: Float): Float {
        val dragged = heightDragged
        return if (dragged == null) neighbor / 2f else (dragged + neighbor) / 2f
    }
    if (offset < 0f && heightUp != null && -offset > threshold(heightUp)) {
        return deltaUp to (offset + heightUp)
    }
    if (offset > 0f && heightDown != null && offset > threshold(heightDown)) {
        return deltaDown to (offset - heightDown)
    }
    return null
}

/** Row structure as flat indices — same span packing as the Home grid. */
internal fun boardRowIndexRows(items: List<WidgetItem>): List<List<Int>> =
    flowRowsOfSpans(items.map { it.colSpan })

/** Vertical neighbors (flat indices) skipping horizontal pair partners. */
internal fun boardVerticalNeighbors(items: List<WidgetItem>, idx: Int): Pair<Int?, Int?> {
    if (idx !in items.indices) return null to null
    val rows = boardRowIndexRows(items)
    val r = rows.indexOfFirst { idx in it }
    if (r < 0) return null to null
    val c = rows[r].indexOf(idx)
    val up = rows.getOrNull(r - 1)?.let { it.getOrNull(c) ?: it.last() }
    val down = rows.getOrNull(r + 1)?.let { it.getOrNull(c) ?: it.last() }
    return up to down
}

/** Max measured height of the row containing memberIdx (for drag thresholds). */
internal fun boardRowHeight(items: List<WidgetItem>, heights: Map<String, Float>, memberIdx: Int): Float? {
    if (memberIdx !in items.indices) return null
    val row = boardRowIndexRows(items).firstOrNull { memberIdx in it } ?: return null
    return row.mapNotNull { heights[items[it].id] }.maxOrNull()
}

@Composable
fun WidgetBoard(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val c = LocalColors.current
    val haptic = LocalHapticFeedback.current
    val itemHeights = remember { mutableStateMapOf<String, Float>() }
    var dragId by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val editingId = vm.selectedWidgetId
    // The selected widget may live on Home while this page is visible — only lock scroll
    // when the selection actually belongs to this board.
    val selOnBoard = editingId != null && vm.boardWidgets.any { it.id == editingId }
    val listState = rememberLazyListState()
    // Selection first reveals: a selected frame whose corner handle would land under the
    // list's bottom edge scrolls into view. Scroll is locked during selection, so without
    // this a widget selected near the bottom edge could keep its handle unreachable. The
    // loop re-arms on itemHeights changes — a resize can push the handle past the edge.
    LaunchedEffect(editingId, selOnBoard, listState) {
        val sel = editingId ?: return@LaunchedEffect
        if (!selOnBoard) return@LaunchedEffect
        val rowIndex = flowRows(vm.boardWidgets.toList()).indexOfFirst { row -> row.any { it.id == sel } }
        if (rowIndex < 0) return@LaunchedEffect
        val lazyIndex = 1 + rowIndex // header item sits at 0 while widgets exist
        var blocked = false
        var retries = 0
        while (editingId == sel && retries < 60) {
            withFrameNanos { } // let the selection/resize layout settle before measuring
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == lazyIndex }
            if (item == null) { retries++; continue }
            val frameBottom = item.offset + (itemHeights[sel] ?: item.size.toFloat())
            val limit = info.viewportEndOffset - 20
            if (!blocked && frameBottom > limit) {
                val before = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                listState.animateScrollBy(frameBottom - limit)
                val after = listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
                if (after == before) blocked = true
                continue
            }
            val lastH = itemHeights[sel]
            snapshotFlow { itemHeights[sel] }.first { it != lastH }
            blocked = false // a frame height changed (resize): re-evaluate the reveal
        }
    }

    Box(modifier) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            userScrollEnabled = !selOnBoard && dragId == null
        ) {
        item {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Txt("WIDGET BOARD", 11f, c.fg2, FontWeight.Bold); Txt("Arrange your home", 20f, c.fg, FontWeight.SemiBold) }
                PillButton("Add", { vm.widgetTargetHome = false; vm.widgetTab = 0; vm.widgetCenter = true }, Modifier.semantics { contentDescription = "Add widget" })
            }
            if (editingId != null) {
                Txt("Arrange mode — drag to reorder, Done to finish", 11f, c.accent, modifier = Modifier.padding(top = 4.dp))
            } else {
                Txt("Long-press a widget to resize · drag to arrange", 11f, c.fg2, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (vm.boardWidgets.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(c.tile), contentAlignment = Alignment.Center) {
                PillButton("Add your first widget", { vm.widgetTargetHome = false; vm.widgetCenter = true }, filled = true)
            }
        }
        items(flowRows(vm.boardWidgets.toList()), key = { row -> row.joinToString { it.id } }) { row ->
            Row(
                Modifier.fillMaxWidth().animateItem(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                row.forEach { item ->
                    val isEditing = editingId == item.id
                    val isDragging = dragId == item.id
                    Box(
                        Modifier
                            .weight(item.colSpan.toFloat())
                            .onSizeChanged { itemHeights[item.id] = it.height.toFloat() }
                    ) {
                        BoardWidget(
                            item = item,
                            vm = vm,
                            isEditing = isEditing,
                            isDragging = isDragging,
                            dragOffset = if (isDragging) dragOffset else 0f,
                            onDragStart = { dragId = item.id; dragOffset = 0f },
                            onDrag = { dy ->
                                dragOffset += dy
                                var swapped = true
                                var guard = 0
                                while (swapped && guard++ < 32) {
                                    swapped = false
                                    val list = vm.boardWidgets.toList()
                                    val idx = list.indexOfFirst { it.id == dragId }
                                    if (idx < 0) break
                                    val (upIdx, downIdx) = boardVerticalNeighbors(list, idx)
                                    val upH = upIdx?.let { boardRowHeight(list, itemHeights, it) }
                                    val downH = downIdx?.let { boardRowHeight(list, itemHeights, it) }
                                    val dragH = boardRowHeight(list, itemHeights, idx)
                                    val step = boardSwap(
                                        dragOffset, upH, downH,
                                        deltaUp = (upIdx ?: idx) - idx,
                                        deltaDown = (downIdx ?: idx) - idx,
                                        heightDragged = dragH
                                    ) ?: break
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.reorderBoardWidget(idx, idx + step.first)
                                    dragOffset = step.second
                                    swapped = true
                                }
                            },
                            onDragEnd = { dragId = null; dragOffset = 0f }
                        )
                    }
                }
                val used = rowUsedColumns(row)
                if (used < WIDGET_COLUMNS) {
                    Spacer(Modifier.weight((WIDGET_COLUMNS - used).toFloat()))
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        }
        // The Arrange bar is a screen-level overlay (Home.kt) so it never covers the
        // list's bottom edge where a selected frame's corner resize handle lives.
    }
}

@Composable
private fun BoardWidget(
    item: WidgetItem,
    vm: LauncherViewModel,
    isEditing: Boolean,
    isDragging: Boolean,
    dragOffset: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val c = LocalColors.current

    Column(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                if (isDragging) {
                    scaleY = 1.03f
                    translationY = dragOffset
                    shadowElevation = 8.dp.toPx()
                    alpha = 0.9f
                }
            }
            .then(
                // No border here — WidgetFrame is the single border source for both pages.
                if (isEditing) Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.tile)
                else Modifier
            )
            .pointerInput(item.id, isEditing) {
                if (isEditing) {
                    detectDragGestures(
                        onDragStart = { onDragStart() },
                        onDragEnd = { onDragEnd() },
                        onDragCancel = { onDragEnd() },
                        onDrag = { _, drag -> onDrag(drag.y) }
                    )
                }
                // Not selected → no drag node here: WidgetFrame's long-press selects,
                // so scroll/tap/Pager pass through untouched.
            }
            .padding(if (isEditing) 8.dp else 0.dp)
    ) {
        WidgetBody(item, vm, compact = vm.compactDensity)
    }
}
