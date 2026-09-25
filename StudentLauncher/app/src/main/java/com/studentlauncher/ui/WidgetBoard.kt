package com.studentlauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.studentlauncher.data.WT
import com.studentlauncher.data.WidgetItem

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

/** Row structure as flat indices — same pairing rules as buildRows. */
internal fun boardRowIndexRows(items: List<WidgetItem>): List<List<Int>> {
    val rows = mutableListOf<List<Int>>()
    var i = 0
    while (i < items.size) {
        val a = items[i]
        val b = items.getOrNull(i + 1)
        if (a.effectiveHalf && b != null && b.effectiveHalf) {
            rows.add(listOf(i, i + 1)); i += 2
        } else {
            rows.add(listOf(i)); i += 1
        }
    }
    return rows
}

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

    Box(modifier) {
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
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
                Txt("Long-press a widget to edit", 11f, c.fg2, modifier = Modifier.padding(top = 4.dp))
            }
        }
        if (vm.boardWidgets.isEmpty()) item {
            Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(c.tile), contentAlignment = Alignment.Center) {
                PillButton("Add your first widget", { vm.widgetTargetHome = false; vm.widgetCenter = true }, filled = true)
            }
        }
        items(buildRows(vm.boardWidgets.toList()), key = { row -> row.joinToString { it.id } }) { row ->
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
                            .weight(1f)
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
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
        }
        ArrangeBar(vm, selOnBoard, Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
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
