package com.jekael.adoel.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Two-option pill toggle with an animated sliding indicator that also follows a
 * drag/tap anywhere across its width, like a physical slider.
 */
@Composable
fun SlidingToggle(
    labelLeft: String,
    labelRight: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    containerColor: Color,
    activeColorLeft: Color,
    activeColorRight: Color,
    activeTextColorLeft: Color,
    activeTextColorRight: Color,
    inactiveTextColor: Color,
    modifier: Modifier = Modifier,
    height: Dp = 44.dp,
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val position = remember { Animatable(selectedIndex.toFloat()) }
    var dragging by remember { mutableStateOf(false) }
    val currentSelected = rememberUpdatedState(selectedIndex)
    val currentOnSelect = rememberUpdatedState(onSelect)

    LaunchedEffect(selectedIndex) {
        if (!dragging) {
            position.animateTo(selectedIndex.toFloat(), animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(50.dp))
            .padding(4.dp),
    ) {
        val segmentWidth = maxWidth / 2
        val widthPx = with(density) { maxWidth.toPx() }
        val pos = position.value

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .pointerInput(widthPx) {
                    if (widthPx <= 0f) return@pointerInput
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        dragging = true
                        val pointerId = down.id
                        scope.launch { position.snapTo((down.position.x / widthPx).coerceIn(0f, 1f)) }
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId } ?: break
                            if (!change.pressed) break
                            change.consume()
                            val frac = (change.position.x / widthPx).coerceIn(0f, 1f)
                            scope.launch { position.snapTo(frac) }
                        }
                        dragging = false
                        val nearest = position.value.roundToInt().coerceIn(0, 1)
                        scope.launch {
                            position.animateTo(nearest.toFloat(), animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        }
                        if (nearest != currentSelected.value) currentOnSelect.value(nearest)
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .offset(x = segmentWidth * pos)
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50.dp))
                    .background(if (pos < 0.5f) activeColorLeft else activeColorRight),
            )
            Row(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        labelLeft,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pos < 0.5f) activeTextColorLeft else inactiveTextColor,
                        ),
                    )
                }
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        labelRight,
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (pos >= 0.5f) activeTextColorRight else inactiveTextColor,
                        ),
                    )
                }
            }
        }
    }
}
