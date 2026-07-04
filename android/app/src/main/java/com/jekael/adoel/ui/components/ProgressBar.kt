package com.jekael.adoel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A track-and-fill linear progress bar — the pill used for the console's shift-progress
 * indicator, pulled out here so the Statistik screen can reuse the exact same look.
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
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(fillColor),
        )
    }
}
