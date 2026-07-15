package com.jekael.adoel.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Which swipe direction triggered a doff completion — drives the celebration icon/color/exit
 * direction in RadarCard (see completingKind below). */
private enum class DoffCompletionKind { NORMAL, MATCHING }

private data class UrgencyStyle(
    val accent: Color,
    val barColor: Color,
    val textColor: Color,
    val labelColor: Color,
    val pulse: Boolean,
    val icon: ImageVector?,
    // Tonal elevation, not shadow: how far the card's face tints from bgElevated toward its own
    // accent color climbs with urgency — the flat/minimal "raised = urgent" cue, but as a color mix
    // (no Modifier.shadow RenderNode, so it can never visibly lag a frame behind the card's content
    // the way a shadow can on a freshly-composed LazyColumn item). OVERDUE ignores this and pulses
    // instead (see faceBg in RadarCard below).
    val tintFraction: Float,
)

private fun urgency(remaining: Long): UrgencyStyle = when (urgencyLevel(remaining)) {
    UrgencyLevel.CALM -> UrgencyStyle(Cyan500, Cyan500, Cyan400, Cyan700, false, null, 0f)
    // tintFraction kept modest — Amber600 already drives both this background wash AND the
    // progress bar fill below; stacking a strong wash on top of that read as too dominant/orange
    // for a card that isn't overdue yet (see clr.barColor/clr.accent usage further down).
    UrgencyLevel.SOON -> UrgencyStyle(Amber500, Amber400, Amber400, Amber700, false, Icons.Outlined.Schedule, 0.06f)
    UrgencyLevel.IMMINENT -> UrgencyStyle(Amber600, Amber600, Amber500, Amber700, false, Icons.Outlined.Warning, 0.10f)
    UrgencyLevel.OVERDUE -> UrgencyStyle(Red500, Red500, Red400, Red700, true, Icons.Filled.Warning, 0f)
}

@Composable
fun RadarCard(
    est: Estimasi,
    mesin: MesinData?,
    nowAbs: Long,
    // Swipe right: doffing normal. Swipe left: doffing dengan keterangan "Matching" (cek kualitas
    // kain dari beam baru) — same instant-commit shape as onDoff, just a different keterangan
    // token, so Teks and Terpandu behave identically here (no confirmation sheet either way; the
    // full Ada-keterangan/kendala flow is only reached from the Doffing console's own entry point).
    onDoff: () -> Unit,
    onDoffMatching: () -> Unit,
    // Hapus moved off the swipe gesture (which now means Matching, not delete) onto long-press.
    onHapus: () -> Unit,
    onQuickEdit: () -> Unit,
    modifier: Modifier = Modifier,
    entranceDelayMs: Long = 0L,
    // True when grid-paired half-width in the Menunggu band (see MenungguGridSlot) — the smaller
    // column width shrinks every label on the card, so the type/corak text bumps up the Inter
    // variable font's weight axis (Bold→ExtraBold) to hold contrast at the smaller size instead of
    // just reading fainter (Master Blueprint §2A). The mcNo hero number is already at the top of
    // the axis (Black) regardless of layout, so it needs no further compensation here.
    isCompact: Boolean = false,
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
    val haptic = LocalHapticFeedback.current
    // Doffing before a machine is actually near due doesn't make sense operationally — swipe (and
    // its screen-reader equivalent) only turns on once the card's within the same lead time as the
    // reminder notification/physical warning light. The topmost card in Menunggu always renders
    // wide regardless of this (see groupMenungguRowsForGrid), so wide-ness alone can no longer be
    // used as a stand-in for "actionable" the way it once could.
    val swipeEnabled = remaining <= REMINDER_LEAD_MIN

    // Only OVERDUE cards actually render the pulse, so only they should pay for it — an
    // unconditional rememberInfiniteTransition here would tick a frame-by-frame animation for
    // every card on screen (CALM/SOON/IMMINENT included) for the entire shift, for no visible effect.
    // Ambient alert breathing, not a micro-interaction — deliberately outside the 150-250ms range
    // (see PingDot's comment above for why a loop this fast would read as flickering, not calm).
    val faceBg = if (clr.pulse) {
        val criticalPulse = rememberInfiniteTransition(label = "criticalPulse")
        val pulseFraction by criticalPulse.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulseFraction",
        )
        lerp(colors.bgElevated, colors.criticalPulseTarget, pulseFraction)
    } else {
        lerp(colors.bgElevated, clr.accent, clr.tintFraction)
    }

    // Celebrate completion — card slides out + an icon pops before the state is actually mutated.
    // Normal and Matching get different icon/color/exit-direction so the two feel distinguishable
    // at a glance (Emerald checkmark sliding right vs Sky "verified" badge sliding left). Hapus
    // deliberately does NOT get this treatment: it's gated by a ConfirmDialog (see handleHapusEst
    // in MainScreen.kt), so animating the card away before the user has even confirmed would hide
    // it during the dialog and leave it stuck gone after Batal, since nothing would reset it.
    // Hapus instead relies on the LazyColumn's own Modifier.animateItem() to reflow once the
    // deletion is actually confirmed, plus its own long-press "charge" animation below.
    var completingKind by remember(est.mcNo) { mutableStateOf<DoffCompletionKind?>(null) }
    val completing = completingKind != null
    val scope = rememberCoroutineScope()
    val exitProgress by animateFloatAsState(
        targetValue = if (completing) 1f else 0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "exitProgress",
    )
    val checkScale by animateFloatAsState(
        targetValue = if (completing) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "checkScale",
    )
    // One-shot diagonal "thread" sweep across the completion tint, echoing the twill-weave
    // texture direction (see Texture.kt) instead of a generic flash.
    val weaveSweep by animateFloatAsState(
        targetValue = if (completing) 1f else 0f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "weaveSweep",
    )
    val completionColor = if (completingKind == DoffCompletionKind.MATCHING) Sky500 else Emerald500
    val completionIcon = if (completingKind == DoffCompletionKind.MATCHING) Icons.Filled.Verified else Icons.Filled.CheckCircle
    val exitDirection = if (completingKind == DoffCompletionKind.MATCHING) -1f else 1f

    // Long-press to hapus: a "charge up" red tint/scale while held (distinct from the swipe-driven
    // completions above, which slide the card away) so an operator gets feedback during the hold
    // itself, not just a sudden confirm dialog. Cancelled/reset if released or dragged before the
    // system long-press timeout fires.
    val pressCharge = remember(est.mcNo) { Animatable(0f) }

    // Staggered fade+rise entrance when a batch of cards first appears (e.g. switching into
    // ESTIMASI mode from empty), instead of every card popping in at once — keyed to mcNo so it
    // only plays once per card, not on every recomposition (nowAbs ticks every 5s).
    val entranceAlpha = remember(est.mcNo) { Animatable(0f) }
    val entranceOffsetY = remember(est.mcNo) { Animatable(16f) }
    LaunchedEffect(est.mcNo) {
        delay(entranceDelayMs)
        launch { entranceAlpha.animateTo(1f, tween(220)) }
        entranceOffsetY.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
    }

    fun triggerDoff(kind: DoffCompletionKind) {
        if (completing) return
        completingKind = kind
        // Distinct haptic per kind (Master Blueprint §3A/§3B): Normal gets one deep tap — the
        // cutter closing on a taut roll of finished cloth; Matching gets two sharp ones — a
        // scissor snipping a quick quality-check sample.
        when (kind) {
            DoffCompletionKind.NORMAL -> haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            DoffCompletionKind.MATCHING -> scope.launch {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                delay(90)
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
        }
        scope.launch {
            delay(420)
            when (kind) {
                DoffCompletionKind.NORMAL -> onDoff()
                DoffCompletionKind.MATCHING -> onDoffMatching()
            }
        }
    }

    // Swipe right = doffing normal, swipe left = doffing dengan keterangan Matching, long-press =
    // hapus — the only ways to act on a card now that the always-visible buttons are gone (see
    // SwipeActionBackground for the swipe reveal panel).
    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { Dimens.SwipeThreshold.toPx() }
    val maxSwipePx = with(density) { Dimens.SwipeMax.toPx() }
    val offsetX = remember(est.mcNo) { Animatable(0f) }

    fun settleSwipe() {
        val value = offsetX.value
        when {
            value >= swipeThresholdPx -> triggerDoff(DoffCompletionKind.NORMAL) // exitProgress takes over from here
            value <= -swipeThresholdPx -> triggerDoff(DoffCompletionKind.MATCHING)
            else -> scope.launch { offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeActionBackground(
            offsetX = offsetX.value,
            thresholdPx = swipeThresholdPx,
            rightIcon = Icons.Outlined.Check,
            leftIcon = Icons.Filled.Verified,
            rightColor = Emerald500,
            leftColor = Sky500,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                // Stretches to match a taller sibling when grid-paired (see MenungguGridSlot),
                // which relies on the outer Box above actually being given that height via
                // Modifier.weight(1f).fillMaxHeight() at the call site — a no-op otherwise.
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = offsetX.value + exitProgress * exitDirection * size.width
                    translationY = entranceOffsetY.value
                    scaleX = 1f - 0.03f * pressCharge.value
                    scaleY = 1f - 0.03f * pressCharge.value
                    alpha = (1f - exitProgress) * entranceAlpha.value
                }
                .elevatedListCard(backgroundColor = lerp(faceBg, Red500, 0.16f * pressCharge.value))
                // Swipe is the fast path, but TalkBack intercepts swipe gestures for its own
                // navigation before they ever reach this card — without this, a screen-reader
                // user would have no way at all to doff or delete. These custom actions surface
                // in TalkBack's local context menu as a non-gesture alternative to the swipe above.
                .semantics(mergeDescendants = true) {
                    customActions = buildList {
                        if (swipeEnabled) {
                            add(CustomAccessibilityAction("Doff mesin ${est.mcNo}") { triggerDoff(DoffCompletionKind.NORMAL); true })
                            add(
                                CustomAccessibilityAction("Doff mesin ${est.mcNo} dengan keterangan Matching") {
                                    triggerDoff(DoffCompletionKind.MATCHING)
                                    true
                                },
                            )
                        }
                        add(CustomAccessibilityAction("Hapus estimasi Mc ${est.mcNo}") { onHapus(); true })
                    }
                }
                .pointerInput(completing, swipeEnabled) {
                    if (completing || !swipeEnabled) return@pointerInput
                    detectHorizontalDragGestures(
                        onDragEnd = { settleSwipe() },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            scope.launch {
                                offsetX.snapTo((offsetX.value + dragAmount).coerceIn(-maxSwipePx, maxSwipePx))
                            }
                        },
                    )
                }
                .pointerInput(completing) {
                    if (completing) return@pointerInput
                    detectTapGestures(
                        onLongPress = {
                            scope.launch { pressCharge.snapTo(0f) }
                            onHapus()
                        },
                        onPress = {
                            // Tied to the system long-press threshold, not a fixed micro-interaction
                            // duration — this must visually track how long the finger is actually
                            // down, so it's exempt from the 150-250ms range other animations use.
                            val chargeJob = scope.launch { pressCharge.animateTo(1f, tween(450, easing = LinearEasing)) }
                            // OVERDUE-only: a rhythmic pulsing haptic while held, mimicking the
                            // loom's own mechanical vibration at low RPM (Master Blueprint §3E) —
                            // a physical cue the operator can feel without having to keep reading
                            // the screen. ~300 RPM = 5 rev/s, so a pulse roughly every 200ms.
                            val vibrationJob = if (clr.pulse) {
                                scope.launch {
                                    while (true) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        delay(200)
                                    }
                                }
                            } else null
                            tryAwaitRelease()
                            chargeJob.cancel()
                            vibrationJob?.cancel()
                            scope.launch { pressCharge.animateTo(0f, tween(150)) }
                        },
                    )
                },
            // Centers the Row below when this card is stretched taller than its own content to
            // match a grid-paired sibling (see MenungguGridSlot) — a no-op when the card's own
            // height already equals its content's, i.e. everywhere outside the grid pairing.
            contentAlignment = Alignment.Center,
        ) {
            // Decorative full-height overlay — wrapped in matchParentSize() so it resolves
            // against the height the Column below actually ends up with (LazyColumn gives
            // this Box unbounded height, so a bare fillMaxHeight() here would collapse to 0).
            Box(modifier = Modifier.matchParentSize()) {
                // Left accent border — a twisted-thread look (alternating shadow bands down the
                // solid accent fill) rather than a flat color bar, since this 3dp strip runs down
                // every single card on Radar and is the one element guaranteed to be on screen at
                // all times; giving it texture does more for the "woven" identity here than
                // anywhere else. Shadow bands (not gap-cutting into the card's own background)
                // so this doesn't need to track the card's dynamically tinted fill color.
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(4.dp)
                        .background(clr.accent)
                        .drawWithContent {
                            drawContent()
                            // Alternating dark/light bands (not just a periodic shadow) so this
                            // reads as a twisted cord's highlight/shadow rotation rather than a
                            // faint smudge — widened bar + stronger contrast after the first pass
                            // at this (3dp, one shadow tone, 22% alpha) turned out too subtle to
                            // confirm by eye on-device.
                            val pitch = 6.dp.toPx()
                            val band = 2.5.dp.toPx()
                            var y = 0f
                            var dark = true
                            while (y < size.height) {
                                drawRect(
                                    color = if (dark) Color.Black.copy(alpha = 0.38f) else Color.White.copy(alpha = 0.30f),
                                    topLeft = Offset(0f, y),
                                    size = Size(size.width, band),
                                )
                                dark = !dark
                                y += pitch
                            }
                        },
                )
            }

            // Content — swipe right = doff, swipe left = doff+Matching, long-press = hapus.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: mc number + type + corak — tappable on its own (separate from the
                // swipe-the-whole-card drag above) as a fast path to correct corak/target yard,
                // the two fields that actually change often on the floor, without leaving Radar
                // for the full Pengaturan > Mesin flow.
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClickLabel = "Ubah corak dan target yard Mc ${est.mcNo}", onClick = onQuickEdit),
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = est.mcNo,
                            // A 3-digit mcNo at the 2-digit size wraps mid-number in a half-width
                            // grid card (e.g. "104" breaking into "10"/"4") — shrink it instead of
                            // letting it wrap, since maxLines=1 alone would just clip a digit.
                            style = TextStyle(
                                fontSize = if (est.mcNo.length >= 3) 30.sp else 40.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-2).sp,
                                color = colors.textPrimary,
                            ),
                            maxLines = 1,
                            softWrap = false,
                        )
                        if (mesin != null) {
                            Icon(
                                imageVector = mesinTipeIcon(mesin.tipe),
                                contentDescription = null,
                                tint = mesinTipeColor(mesin.tipe),
                                modifier = Modifier.size(12.dp).padding(bottom = 4.dp),
                            )
                        }
                        Text(
                            text = tipe,
                            style = TextStyle(
                                fontSize = 12.sp,
                                fontWeight = if (isCompact) FontWeight.ExtraBold else FontWeight.Bold,
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
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Small standard Material icon marking "this line is about the fabric
                        // itself" (corak/yard) — not a fabric illustration, just a modest visual
                        // anchor next to the one line of text that's actually about the kain,
                        // as opposed to the machine/timing info the rest of the card shows.
                        Icon(
                            imageVector = Icons.Outlined.Texture,
                            contentDescription = null,
                            tint = colors.textFaint,
                            modifier = Modifier.size(12.dp),
                        )
                        Text(
                            text = corakLine,
                            style = AppType.LabelBold.copy(
                                color = colors.textMuted,
                                fontWeight = if (isCompact) FontWeight.ExtraBold else FontWeight.Bold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    LinearProgressBar(
                        fraction = progress,
                        trackColor = colors.bgElevated2,
                        fillColor = clr.barColor,
                        modifier = Modifier.fillMaxWidth(),
                        fillMaxWidth = true,
                        height = 3.dp,
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp,
                                color = if (remaining < 0) Red400 else clr.textColor,
                            ),
                        )
                    }
                }
            }

            // Celebrate completion — the card visibly "splits" as the tint sweeps in behind a
            // clipped boundary (straight diagonal for Normal, a jagged swatch-clip edge for
            // Matching — Master Blueprint §3A/§3B), then the icon pops/spins in. Which shape,
            // icon, and direction depends on completingKind (see its assignment above).
            if (checkScale > 0f) {
                val isMatching = completingKind == DoffCompletionKind.MATCHING
                Box(
                    modifier = Modifier.matchParentSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .drawWithContent {
                                drawContent()
                                val splitPath = if (isMatching) {
                                    jaggedSplitPath(size, weaveSweep)
                                } else {
                                    diagonalSplitPath(size, weaveSweep)
                                }
                                clipPath(splitPath) {
                                    drawRect(completionColor.copy(alpha = 0.30f))
                                }
                                // Bright flash racing along the split boundary as it sweeps across.
                                rotate(if (isMatching) -12f else 20f) {
                                    val bandWidth = size.width * 0.18f
                                    val travel = size.width * 1.6f
                                    val x = -bandWidth + weaveSweep * travel
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.5f * (1f - weaveSweep)),
                                        topLeft = Offset(x, -size.height),
                                        size = Size(bandWidth, size.height * 3f),
                                    )
                                }
                            },
                    )
                    Icon(
                        imageVector = completionIcon,
                        contentDescription = null,
                        tint = completionColor,
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer {
                                scaleX = checkScale
                                scaleY = checkScale
                                // Verified spins in (Matching); CheckCircle just grows in place.
                                rotationZ = if (isMatching) (1f - checkScale) * -180f else 0f
                            },
                    )
                }
            }
        }
    }
}

/** Straight diagonal boundary sweeping left-to-right as [progress] goes 0→1 — everything left of
 * the boundary is inside the returned path (see [clipPath] call site). Slanted like a single
 * cutter pass through taut cloth, for the routine "Potong Normal" completion. */
private fun diagonalSplitPath(size: Size, progress: Float): Path {
    val slant = size.height * 0.55f
    val edgeX = size.width * progress
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(edgeX, 0f)
        lineTo(edgeX - slant, size.height)
        lineTo(0f, size.height)
        close()
    }
}

/** Zigzag boundary sweeping left-to-right as [progress] goes 0→1 — a "swatch clip" sampling snip
 * rather than a clean blade pass, for the "Potong Matching" quality-check completion. */
private fun jaggedSplitPath(size: Size, progress: Float): Path {
    val edgeX = size.width * progress
    val teeth = 8
    val toothH = size.height / teeth
    val toothDepth = size.minDimension * 0.03f
    return Path().apply {
        moveTo(0f, 0f)
        lineTo(edgeX, 0f)
        for (i in 1..teeth) {
            val y = (toothH * i).coerceAtMost(size.height)
            val x = edgeX + (if (i % 2 == 0) toothDepth else -toothDepth)
            lineTo(x, y)
        }
        lineTo(0f, size.height)
        close()
    }
}

// Continuous ambient indicator, not a one-shot micro-interaction — the 150-250ms range (see
// Motion.kt) is for animations that respond to something happening; a breathing/ping loop that
// fast would read as flickering rather than "halus", so it's intentionally left outside that range.
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
