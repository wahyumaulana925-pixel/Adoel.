package com.jekael.adoel.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class UrgencyStyle(
    val accent: Color,
    val barColor: Color,
    val textColor: Color,
    val labelColor: Color,
    val pulse: Boolean,
    val icon: ImageVector?,
)

private fun urgency(remaining: Long): UrgencyStyle = when (urgencyLevel(remaining)) {
    UrgencyLevel.CALM -> UrgencyStyle(Cyan500, Cyan500, Cyan400, Cyan700, false, null)
    UrgencyLevel.SOON -> UrgencyStyle(Amber500, Amber400, Amber400, Amber700, false, Icons.Outlined.Schedule)
    UrgencyLevel.IMMINENT -> UrgencyStyle(Orange500, Orange500, Orange400, Orange700, false, Icons.Outlined.Warning)
    UrgencyLevel.OVERDUE -> UrgencyStyle(Red500, Red500, Red400, Red700, true, Icons.Filled.Warning)
}

@Composable
fun RadarCard(
    est: Estimasi,
    mesin: MesinData?,
    nowAbs: Long,
    onDoff: () -> Unit,
    onHapus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val remaining = est.estAbsMin - nowAbs
    val clr = urgency(remaining)
    val totalDur = est.estAbsMin - est.startAbsMin
    val elapsed = nowAbs - est.startAbsMin
    val progress = if (totalDur > 0) (elapsed.toFloat() / totalDur).coerceIn(0f, 1f) else 0f
    val remStr = formatDeltaMin(remaining)
    val corak = est.corakOverride ?: mesin?.corak ?: "—"
    val standardYard = est.yardOverride ?: mesin?.targetYard
    val corakLine = if (standardYard != null) "$corak · ${formatYard(standardYard)}y" else corak
    val tipe = mesin?.tipe?.name ?: "?"
    val showDot = remaining <= 5
    val colors = LocalAppColors.current

    val criticalPulse = rememberInfiniteTransition(label = "criticalPulse")
    val pulseFraction by criticalPulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseFraction",
    )
    val faceBg = if (clr.pulse) lerp(colors.bgElevated, colors.criticalPulseTarget, pulseFraction) else colors.bgElevated

    // Celebrate completion — card slides out + checkmark pops before the state is actually
    // mutated. Hapus deliberately does NOT get this treatment: it's gated by a ConfirmDialog
    // (see handleHapusEst in MainScreen.kt), so animating the card away on tap — before the user
    // has even confirmed — would hide it during the dialog and leave it stuck gone after Batal,
    // since nothing would ever reset the animation. Hapus instead relies on the LazyColumn's own
    // Modifier.animateItem() to reflow smoothly once the deletion is actually confirmed.
    var completing by remember(est.mcNo) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val exitProgress by animateFloatAsState(
        targetValue = if (completing) 1f else 0f,
        animationSpec = tween(420, easing = FastOutSlowInEasing),
        label = "exitProgress",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (completing) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )

    fun triggerDoff() {
        if (completing) return
        completing = true
        scope.launch {
            delay(420)
            onDoff()
        }
    }

    // Swipe right = doff, swipe left = hapus — the only way to act on a card now that the
    // always-visible buttons are gone (see SwipeActionBackground for the reveal panel).
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 88.dp.toPx() }
    val maxSwipePx = with(density) { 132.dp.toPx() }
    val offsetX = remember(est.mcNo) { Animatable(0f) }

    fun settleSwipe() {
        val value = offsetX.value
        when {
            value >= swipeThresholdPx -> triggerDoff() // exitProgress takes over the slide-out from here
            value <= -swipeThresholdPx -> {
                onHapus()
                scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
            }
            else -> scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeActionBackground(offsetX = offsetX.value, thresholdPx = swipeThresholdPx)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = offsetX.value + exitProgress * size.width
                    alpha = 1f - exitProgress
                }
                .shadow(elevation = 5.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.35f))
                .clip(RoundedCornerShape(14.dp))
                .background(faceBg)
                .pointerInput(completing) {
                    if (completing) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = { settleSwipe() },
                        onDragCancel = {
                            scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx))
                            }
                        },
                    )
                },
        ) {
            // Decorative full-height overlays — wrapped in matchParentSize() so they resolve
            // against the height the Column below actually ends up with (LazyColumn gives
            // this Box unbounded height, so a bare fillMaxHeight() here would collapse to 0).
            Box(modifier = Modifier.matchParentSize()) {
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
            }

            // Content — swipe right to doff, swipe left to hapus (see SwipeActionBackground above).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
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
                                color = colors.textPrimary,
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
                        if (clr.icon != null) {
                            Icon(
                                imageVector = clr.icon,
                                contentDescription = null,
                                tint = clr.labelColor,
                                modifier = Modifier.size(12.dp).padding(bottom = 4.dp),
                            )
                        }
                    }
                    Text(
                        text = corakLine,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = colors.textMuted,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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

            // Celebrate completion — checkmark pops in while the card slides/fades out
            if (checkScale > 0f) {
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(Emerald500.copy(alpha = 0.14f)),
                    )
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Emerald500,
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { scaleX = checkScale; scaleY = checkScale },
                    )
                }
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeActionBackground(offsetX: Float, thresholdPx: Float) {
    if (abs(offsetX) < 1f) return
    val isDoff = offsetX > 0
    val progress = (abs(offsetX) / thresholdPx).coerceIn(0f, 1f)
    val bg = if (isDoff) Emerald500 else Red500
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bg.copy(alpha = 0.18f + 0.6f * progress)),
        contentAlignment = if (isDoff) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Icon(
            imageVector = if (isDoff) Icons.Outlined.Check else Icons.Outlined.Delete,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .size(26.dp)
                .graphicsLayer {
                    scaleX = 0.6f + 0.4f * progress
                    scaleY = 0.6f + 0.4f * progress
                },
        )
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
