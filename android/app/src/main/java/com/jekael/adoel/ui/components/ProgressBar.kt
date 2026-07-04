package com.jekael.adoel.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
    height: Dp = 4.dp,
    cornerRadius: Dp = 2.dp,
) {
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (grown) fraction else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "progressFraction",
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .background(trackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(fillColor),
        )
    }
}
