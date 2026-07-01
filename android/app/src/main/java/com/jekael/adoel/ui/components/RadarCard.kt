package com.jekael.adoel.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val SWIPE_SENSITIVITY = 1.8f
private const val SNAP_OPEN_FRACTION = 0.32f

private data class UrgencyStyle(
    val accent: Color,
    val barColor: Color,
    val textColor: Color,
    val labelColor: Color,
    val pulse: Boolean,
)

private fun urgency(remaining: Long): UrgencyStyle = when {
    remaining > 30 -> UrgencyStyle(Cyan500, Cyan500, Cyan400, Cyan700, false)
    remaining > 10 -> UrgencyStyle(Amber500, Amber400, Amber400, Amber700, false)
    remaining > 0  -> UrgencyStyle(Orange500, Orange500, Orange400, Orange700, false)
    else           -> UrgencyStyle(Red500, Red500, Red400, Red700, true)
}

@Composable
fun RadarCard(
    est: Estimasi,
    mesin: MesinData?,
    nowAbs: Long,
    onDoff: () -> Unit,
    onHapus: () -> Unit,
    onLongPress: () -> Unit,
    onTap: () -> Unit,
) {
    val remaining = est.estAbsMin - nowAbs
    val clr = urgency(remaining)
    val totalDur = est.estAbsMin - est.startAbsMin
    val elapsed = nowAbs - est.startAbsMin
    val progress = if (totalDur > 0) (elapsed.toFloat() / totalDur).coerceIn(0f, 1f) else 0f
    val remStr = formatDeltaMin(remaining)
    val corak = est.corakOverride ?: mesin?.corak ?: "—"
    val tipe = mesin?.tipe?.name ?: "?"
    val showDot = remaining <= 5

    val revealDp = 180.dp
    val density = LocalDensity.current
    val revealPx = with(density) { revealDp.toPx() }

    val animOffset = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val currentOffset = if (isDragging) dragOffset else animOffset.value
    val scope = rememberCoroutineScope()

    fun snapTo(open: Boolean) = scope.launch {
        animOffset.animateTo(
            if (open) revealPx else 0f,
            animationSpec = tween(220, easing = FastOutSlowInEasing),
        )
    }

    val criticalPulse = rememberInfiniteTransition(label = "criticalPulse")
    val pulseFraction by criticalPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseFraction",
    )
    val faceBg = if (clr.pulse) lerp(Zinc900, Color(0xFF3A1414), pulseFraction) else Zinc900

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Zinc900),
    ) {
        // Action buttons strip (behind card face)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(revealDp)
                .fillMaxHeight()
                .padding(end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActionButton(
                label = "HAPUS",
                bgColor = Zinc700,
                modifier = Modifier
                    .width(72.dp)
                    .fillMaxHeight(0.8f),
                onClick = { snapTo(false); onHapus() },
            ) {
                TrashIcon()
            }
            ActionButton(
                label = "DOFF",
                bgColor = Cyan600,
                modifier = Modifier
                    .width(88.dp)
                    .fillMaxHeight(0.8f),
                onClick = { snapTo(false); onDoff() },
            ) {
                CheckIcon()
            }
        }

        // Card face
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -currentOffset
                }
                .background(faceBg)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        isDragging = false
                        var longPressTriggered = false
                        var longPressJob = scope.launch {
                            delay(480)
                            if (!isDragging) {
                                longPressTriggered = true
                                onLongPress()
                            }
                        }
                        val startOffset = animOffset.value

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull() ?: break
                                if (!change.pressed) {
                                    longPressJob.cancel()
                                    if (isDragging) {
                                        isDragging = false
                                        val open = dragOffset > revealPx * SNAP_OPEN_FRACTION
                                        scope.launch {
                                            animOffset.snapTo(dragOffset)
                                            animOffset.animateTo(
                                                if (open) revealPx else 0f,
                                                animationSpec = tween(220, easing = FastOutSlowInEasing),
                                            )
                                        }
                                    } else if (!longPressTriggered) {
                                        if (animOffset.value > 1f) {
                                            snapTo(false)
                                        } else {
                                            onTap()
                                        }
                                    }
                                    break
                                }
                                val dx = down.position.x - change.position.x
                                if (!isDragging && abs(dx) > 6f) {
                                    isDragging = true
                                    longPressJob.cancel()
                                }
                                if (isDragging) {
                                    change.consume()
                                    dragOffset = (startOffset + dx * SWIPE_SENSITIVITY).coerceIn(0f, revealPx)
                                }
                            }
                        } finally {
                            longPressJob.cancel()
                        }
                    }
                },
        ) {
            // Left accent border
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .background(clr.accent),
            )

            // Progress fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(clr.barColor.copy(alpha = 0.12f)),
            )

            // Content
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: mc number + type + corak
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = est.mcNo,
                            style = TextStyle(
                                fontSize = 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp,
                                color = Zinc100,
                            ),
                        )
                        Text(
                            text = tipe,
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = clr.labelColor,
                            ),
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                    Text(
                        text = corak,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = Zinc500,
                        ),
                        maxLines = 1,
                    )
                }

                // Right: ping dot + estimated time + remaining
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (showDot) {
                        PingDot(color = if (remaining < 0) Red500 else Emerald500)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = absMinToTimeStr(est.estAbsMin),
                            style = TextStyle(
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-1).sp,
                                fontFamily = FontFamily.Monospace,
                                color = clr.textColor,
                            ),
                        )
                        Text(
                            text = remStr,
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (remaining < 0) Red400 else clr.textColor,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PingDot(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "ping")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 2.2f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "pingScale",
    )
    val pingAlpha by infiniteTransition.animateFloat(
        initialValue = 0.75f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Restart),
        label = "pingAlpha",
    )
    Box(modifier = Modifier.size(12.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale; alpha = pingAlpha }
                .clip(CircleShape)
                .background(color),
        )
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}
