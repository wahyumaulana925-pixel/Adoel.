package com.jekael.adoel.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A track-and-fill linear progress bar — the pill used for the console's shift-progress
 * indicator, pulled out here so the Statistik screen can reuse the exact same look.
 *
 * Grows in from empty on first composition (rather than snapping straight to [fraction]), and
 * animates smoothly between later fraction changes too.
 */
@Composable
fun LinearProgressBar(
    fraction: Float,
    trackColor: Color,
    fillColor: Color,
    modifier: Modifier = Modifier,
    width: Dp = 90.dp,
    // When true, the bar spans whatever width its container gives it instead of the fixed [width]
    // — RadarCard uses this so the bar reaches the edge of its column instead of reading as a
    // short stub next to a bunch of empty space.
    fillMaxWidth: Boolean = false,
    height: Dp = 4.dp,
    cornerRadius: Dp = 2.dp,
) {
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (grown) fraction else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "progressFraction",
    )

    Box(
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(fillColor)
                // Woven-thread look: cut thin gaps into the fill at a fixed pitch (independent of
                // the bar's own width) so a short RadarCard bar and a wide header bar both read as
                // the same "thread" texture rather than one solid block.
                .drawWithContent {
                    drawContent()
                    val pitch = 8.dp.toPx()
                    val gap = 2.dp.toPx()
                    var x = pitch
                    while (x < size.width) {
                        drawRect(
                            color = trackColor,
                            topLeft = Offset(x, 0f),
                            size = Size(gap, size.height),
                        )
                        x += pitch
                    }
                },
        )
        // A small "roll" at the leading edge — the fabric-being-produced metaphor behind this bar
        // (it tracks yard progress toward a doff) had no visual cue that it's cloth accumulating,
        // just a bar filling like any generic loader. A shaded circle (dark-light-dark horizontal
        // gradient, like a cylinder's cross-section) reads as a small roll of cloth growing at the
        // point production has reached, without becoming a literal fabric illustration.
        if (animatedFraction > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.coerceIn(0f, 1f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(height * 2.4f)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    lerp(fillColor, Color.Black, 0.35f),
                                    lerp(fillColor, Color.White, 0.4f),
                                    lerp(fillColor, Color.Black, 0.35f),
                                ),
                            ),
                        ),
                )
            }
        }
    }
}
