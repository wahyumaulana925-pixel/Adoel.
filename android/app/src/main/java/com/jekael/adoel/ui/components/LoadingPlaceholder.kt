package com.jekael.adoel.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors

private val ShuttleTrackWidth = 96.dp
private val ShuttleSize = 14.dp

/** Cold-start placeholder shown until [com.jekael.adoel.viewmodel.DoffViewModel]'s first persisted
 * state emission arrives, so the 174-unconfigured-machine in-memory default never flashes on screen.
 *
 * Replaces a generic spinner with two weaving-themed cues: a shuttle sliding back and forth along
 * its track, and three short "thread" rows fading in one after another underneath it. */
// Continuous ambient loading loop (the brief's "loading bernuansa weaving"), not a one-shot
// micro-interaction — outside the 150-250ms range on purpose, same reasoning as RadarCard's
// PingDot/criticalPulse: a shuttle sweep or thread-fade that fast would flicker instead of read
// as a calm, continuous motion.
@Composable
internal fun LoadingPlaceholder() {
    val colors = LocalAppColors.current
    val transition = rememberInfiniteTransition(label = "loomLoading")
    val shuttleX by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "shuttleX",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.Space20),
        ) {
            Box(modifier = Modifier.width(ShuttleTrackWidth).height(ShuttleSize), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(colors.border))
                val halfRange = (ShuttleTrackWidth - ShuttleSize) / 2
                Box(
                    modifier = Modifier
                        .offset(x = halfRange * shuttleX)
                        .size(ShuttleSize)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Cyan500),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.Space8)) {
                ThreadRow(transition, width = 64.dp, delayMillis = 0)
                ThreadRow(transition, width = 96.dp, delayMillis = 150)
                ThreadRow(transition, width = 48.dp, delayMillis = 300)
            }
        }
    }
}

@Composable
private fun ThreadRow(transition: InfiniteTransition, width: Dp, delayMillis: Int) {
    val colors = LocalAppColors.current
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis),
        ),
        label = "threadAlpha",
    )
    Box(
        modifier = Modifier
            .width(width)
            .height(3.dp)
            .clip(RoundedCornerShape(1.5.dp))
            .background(colors.border.copy(alpha = alpha)),
    )
}
