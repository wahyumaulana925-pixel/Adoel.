package com.jekael.adoel.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Makes a LazyColumn item recede (shrink/fade/tilt) as it scrolls past the top edge of the
 * viewport, like a card folding into a stack behind the header — an iOS notification-stack
 * cue applied to an ordinary flat list. Items still below the top edge render untouched
 * (recede = 0), so a normal scroll doesn't shrink the whole list, only the one item currently
 * exiting through the top.
 *
 * Reads [listState] inside the `graphicsLayer` draw-phase lambda (not composition), so this
 * recomputes per frame for the item(s) near the edge without recomposing anything.
 */
fun Modifier.stackRecede(listState: LazyListState, itemKey: Any): Modifier = this.graphicsLayer {
    val info = listState.layoutInfo.visibleItemsInfo.find { it.key == itemKey } ?: return@graphicsLayer
    val heightPx = info.size.toFloat().coerceAtLeast(1f)
    val recede = (-info.offset.toFloat() / heightPx).coerceIn(0f, 1f)
    scaleX = 1f - recede * 0.12f
    scaleY = scaleX
    alpha = 1f - recede * 0.55f
    translationY = -recede * heightPx * 0.08f
    rotationX = recede * 6f
    cameraDistance = 12f * density
    transformOrigin = TransformOrigin(0.5f, 0f)
}
