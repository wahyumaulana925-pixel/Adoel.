package com.jekael.adoel.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * A track-and-fill linear progress bar — the pill used for the console's shift-progress
 * indicator, pulled out here so the Statistik screen can reuse the exact same look.
 *
 * Grows in from empty on first composition (rather than snapping straight to [fraction]), and
 * animates smoothly between later fraction changes too. The track stays a plain straight line
 * (calm, at rest); the fill itself is a continuously flowing sine wave, like water moving through
 * a flume, with the roll indicator at its leading edge bobbing up and down to track the wave's own
 * crest instead of just sliding sideways.
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
    // RadarCard's own per-machine bar animates continuously (it's the one thing on that card that
    // keeps moving); the header's shift-progress bar and Statistik's archived-shift rows share the
    // exact same wave shape/thickness for visual consistency but stay still — a whole screen of
    // rows all flowing at once would be much busier than one card's bar, and an archived shift
    // isn't "in progress" anymore anyway.
    animated: Boolean = true,
) {
    var grown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { grown = true }
    val animatedFraction by animateFloatAsState(
        targetValue = if (grown) fraction else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "progressFraction",
    )

    val phase = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "waveFlow")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2f * Math.PI).toFloat(),
            animationSpec = infiniteRepeatable(animation = tween(1200, easing = LinearEasing)),
            label = "wavePhase",
        )
        animatedPhase
    } else {
        0f
    }
    val density = LocalDensity.current

    BoxWithConstraints(
        // No .clip() on this shared outer container — it hosts the roll indicator below, which
        // deliberately overflows past [height] via requiredSize(), and the wave fill's own
        // amplitude also swings past [height]; clipping here would just crop both straight back
        // off. Track/fill/roll each clip themselves individually instead.
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

        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { height.toPx() }
        // Gentler than the bar's full height/a tight 12dp pitch — that combination read as a sharp
        // zigzag rather than a smooth flowing curve, especially right at the leading edge where the
        // path used to jump straight from a fixed anchor to the first phase-shifted point.
        val amplitudePx = heightPx * 0.5f
        val wavelengthPx = with(density) { 18.dp.toPx() }
        val stepPx = with(density) { 1.dp.toPx() }
        val strokeWidthPx = heightPx * 0.6f
        val progressWidthPx = widthPx * animatedFraction.coerceIn(0f, 1f)

        fun waveY(midY: Float, x: Float) = midY + amplitudePx * sin((2.0 * Math.PI * x / wavelengthPx) - phase).toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            if (progressWidthPx > 0f) {
                val midY = size.height / 2f
                val wavePath = Path().apply {
                    // Starts at the actual first phase-shifted point (not a fixed midY anchor) —
                    // moveTo-ing to midY then immediately lineTo-ing to the real value drew a
                    // visible vertical spike at x=0 whenever phase wasn't a multiple of π.
                    moveTo(0f, waveY(midY, 0f))
                    var x = stepPx
                    while (x <= progressWidthPx) {
                        lineTo(x, waveY(midY, x))
                        x += stepPx
                    }
                }
                drawPath(
                    path = wavePath,
                    color = fillColor,
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                )
            }
        }

        // A small "roll" at the leading edge — the fabric-being-produced metaphor behind this bar
        // (it tracks yard progress toward a doff) had no visual cue that it's cloth accumulating,
        // just a bar filling like any generic loader. Its Y position now tracks the wave's own
        // crest at that X instead of sitting on a fixed centerline, so it visibly bobs as the wave
        // flows underneath it — like a spool riding the surface of moving water. Its diameter still
        // grows with [animatedFraction] too (1.6x up to 3.0x the bar's height) so the roll itself
        // visibly thickens as more cloth accumulates, not just travels further right.
        if (animatedFraction > 0.02f) {
            val rollYPx = waveY(heightPx / 2f, progressWidthPx)
            val rollDiameter = height * (1.6f + 1.4f * animatedFraction.coerceIn(0f, 1f))
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { progressWidthPx.toDp() } - rollDiameter / 2,
                        y = with(density) { rollYPx.toDp() } - rollDiameter / 2,
                    )
                    // requiredSize (not size) — the roll is deliberately taller than the thin
                    // track it sits on, like a spool poking above/below a belt; size() would have
                    // Compose coerce it back down to fit the track's own tight height constraint,
                    // squashing it down to invisible.
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
