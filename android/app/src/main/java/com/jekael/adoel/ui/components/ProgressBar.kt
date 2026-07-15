package com.jekael.adoel.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
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
        // No .clip() on this shared outer container — it hosts the roll indicator below, which
        // deliberately overflows past [height] via requiredSize(); clipping here would just crop
        // that overflow straight back off. Track/fill each clip themselves individually instead.
        modifier = modifier
            .then(if (fillMaxWidth) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(height),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(cornerRadius))
                .background(trackColor),
        )
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(cornerRadius))
                .background(fillColor)
                // Coiled yarn texture (Master Blueprint §3C): diagonal strokes across the fill,
                // growing both denser and thicker toward the leading edge nearest the roll —
                // reads as yarn winding up thicker as more cloth accumulates, rather than a
                // uniform dashed cut at a fixed pitch regardless of position.
                .drawWithContent {
                    drawContent()
                    val slant = size.height * 0.8f
                    val unit = 1.dp.toPx()
                    var x = -slant
                    while (x < size.width) {
                        // 0 at the fill's trailing edge, 1 at its leading edge (near the roll).
                        val t = (x / size.width).coerceIn(0f, 1f)
                        drawLine(
                            color = trackColor.copy(alpha = 0.55f),
                            start = Offset(x, 0f),
                            end = Offset(x + slant, size.height),
                            strokeWidth = (1f + 2f * t) * unit,
                        )
                        x += (11f - 5f * t) * unit
                    }
                },
        )
        // A small "roll" at the leading edge — the fabric-being-produced metaphor behind this bar
        // (it tracks yard progress toward a doff) had no visual cue that it's cloth accumulating,
        // just a bar filling like any generic loader. A shaded circle (dark-light-dark horizontal
        // gradient, like a cylinder's cross-section) reads as a small roll of cloth growing at the
        // point production has reached, without becoming a literal fabric illustration. Its own
        // diameter grows with [animatedFraction] too (1.6x up to 3.0x the bar's height) so the roll
        // itself visibly thickens as more cloth accumulates, not just travels further right.
        // requiredSize (not size) — the roll is deliberately taller than the thin track it sits
        // on, like a spool poking above/below a belt; size() would have Compose coerce it back
        // down to fit the track's own tight height constraint, squashing it down to invisible.
        if (animatedFraction > 0.02f) {
            val rollDiameter = height * (1.6f + 1.4f * animatedFraction.coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction.coerceIn(0f, 1f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    modifier = Modifier
                        .requiredSize(rollDiameter)
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
