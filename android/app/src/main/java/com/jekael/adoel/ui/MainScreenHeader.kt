package com.jekael.adoel.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.shiftNumberForEpochMin
import com.jekael.adoel.ui.components.GearIcon
import com.jekael.adoel.ui.components.LinearProgressBar
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.launch
import java.util.Calendar

/** Floating header — branding (with a tap-pulse micro-interaction), shift progress, and the
 * settings gear. Reports its own measured height via [onHeightMeasured] so the scrollable list
 * behind it can pad itself to avoid sitting under the card. */
@Composable
fun MainScreenHeader(
    nowAbs: Long,
    totalMc: Int,
    doffCount: Int,
    showRemaining: Boolean,
    onToggleShowRemaining: () -> Unit,
    onGearClick: () -> Unit,
    // Shift-wide actions (not tied to whichever mode/list is currently on screen) — expanded via
    // a dedicated chevron rather than overloading the doff-count column's existing tap (which
    // already toggles selesai/sisa display), so neither gesture has to give way to the other.
    actionsExpanded: Boolean,
    onToggleActionsExpanded: () -> Unit,
    onShare: () -> Unit,
    onStatistik: () -> Unit,
    onFinish: () -> Unit,
    onHeightMeasured: (Dp) -> Unit,
    haptic: HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                onHeightMeasured(with(density) { coords.size.height.toDp() })
            }
            .padding(horizontal = 12.dp)
            .padding(top = 12.dp)
            .floatingHeaderCard(),
    ) {
      Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Branding — a one-shot ~320ms tap pulse: logo settles to 0.97, the dot hops up
            // 7dp and a thin glow blooms from it, all finishing on their own (not tied to how
            // long the finger stays down) so repeated daily taps stay quick and subtle.
            var brandPulseKey by remember { mutableStateOf(0) }
            val brandScale = remember { Animatable(1f) }
            val dotOffsetY = remember { Animatable(0f) }
            val glowAlpha = remember { Animatable(0f) }
            val glowScale = remember { Animatable(0.6f) }
            LaunchedEffect(brandPulseKey) {
                if (brandPulseKey == 0) return@LaunchedEffect
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                launch {
                    brandScale.animateTo(0.97f, tween(90, easing = FastOutSlowInEasing))
                    brandScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing))
                }
                launch {
                    dotOffsetY.animateTo(-7f, tween(120, easing = FastOutSlowInEasing))
                    dotOffsetY.animateTo(0f, tween(180, easing = FastOutSlowInEasing))
                }
                launch {
                    glowScale.snapTo(0.6f)
                    glowAlpha.snapTo(0.4f)
                    launch { glowAlpha.animateTo(0f, tween(300, easing = LinearOutSlowInEasing)) }
                    glowScale.animateTo(2.2f, tween(320, easing = LinearOutSlowInEasing))
                }
            }
            val shiftLabel = remember(nowAbs) {
                val cal = Calendar.getInstance().apply { timeInMillis = nowAbs * 60000L }
                "Shift ${shiftNumberForEpochMin(nowAbs)} · %02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
            }
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .graphicsLayer { scaleX = brandScale.value; scaleY = brandScale.value }
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClickLabel = "Animasi logo",
                        ) { brandPulseKey++ },
                ) {
                    Text(
                        text = "Adoel",
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = colors.textPrimary, letterSpacing = (-0.5).sp),
                    )
                    Text(
                        text = ".",
                        modifier = Modifier
                            .offset(y = dotOffsetY.value.dp)
                            .drawBehind {
                                drawCircle(
                                    color = Amber500.copy(alpha = glowAlpha.value),
                                    radius = (size.minDimension.coerceAtLeast(20f)) * glowScale.value,
                                    center = Offset(size.width / 2f, size.height / 2f),
                                )
                            },
                        style = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Black, color = Amber500),
                    )
                }
                Text(
                    text = shiftLabel,
                    style = AppType.Caption.copy(color = colors.textFaint),
                )
            }

            // Shift progress — centered between branding and the menu icon
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (totalMc > 0) {
                    val remainingMc = totalMc - doffCount
                    val shiftFraction = doffCount.toFloat() / totalMc
                    val animatedFraction by animateFloatAsState(
                        targetValue = shiftFraction.coerceIn(0f, 1f),
                        animationSpec = tween(400),
                        label = "shiftProgress",
                    )
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClickLabel = "Ganti tampilan jumlah selesai/sisa") {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onToggleShowRemaining()
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = when {
                                !showRemaining -> "$doffCount/$totalMc"
                                remainingMc <= 0 -> "Selesai"
                                else -> "$remainingMc lagi"
                            },
                            style = AppType.LabelSmallBold.copy(color = Cyan400),
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressBar(
                            fraction = animatedFraction,
                            trackColor = colors.bgElevated2,
                            fillColor = Cyan500,
                        )
                    }
                }
            }

            // Expand/collapse the shift-actions row below (Bagikan/Statistik/Selesai Shift) —
            // separate from the gear (full Pengaturan) and from the doff-count column's own tap.
            IconButton(onClick = onToggleActionsExpanded) {
                Icon(
                    imageVector = if (actionsExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (actionsExpanded) "Sembunyikan aksi shift" else "Tampilkan aksi shift",
                    tint = colors.textMuted,
                )
            }

            // Gear button
            IconButton(
                onClick = onGearClick,
            ) {
                GearIcon()
            }
        }

        AnimatedVisibility(
            visible = actionsExpanded,
            enter = expandVertically(tween(200)) + fadeIn(tween(160)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
        ) {
            ShiftActionsRow(
                onShare = onShare,
                onStatistik = onStatistik,
                onFinish = onFinish,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
            )
        }
      }
    }
}

/** Bagikan/Statistik/Selesai Shift — shift-wide actions, available regardless of whether
 * Estimasi or Doffing is the currently-selected list (moved out of doffingSection, which only
 * rendered — and scrolled away — while in Doffing mode). */
@Composable
private fun ShiftActionsRow(onShare: () -> Unit, onStatistik: () -> Unit, onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onShare,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
            contentPadding = PaddingValues(0.dp),
        ) { Icon(imageVector = Icons.Outlined.Share, contentDescription = "Bagikan", modifier = Modifier.size(20.dp)) }
        OutlinedButton(
            onClick = onStatistik,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
            contentPadding = PaddingValues(0.dp),
        ) { Icon(imageVector = Icons.Outlined.BarChart, contentDescription = "Statistik", modifier = Modifier.size(20.dp)) }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            border = BorderStroke(1.dp, Red700.copy(alpha = 0.5f)),
            contentPadding = PaddingValues(0.dp),
        ) { Icon(imageVector = Icons.Outlined.Flag, contentDescription = "Selesai Shift", modifier = Modifier.size(20.dp)) }
    }
}
