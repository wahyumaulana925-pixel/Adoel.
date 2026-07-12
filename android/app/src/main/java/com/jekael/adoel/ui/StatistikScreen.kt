package com.jekael.adoel.ui

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.ShiftRecord
import com.jekael.adoel.data.buildShareShiftText
import com.jekael.adoel.data.formatDeltaMin
import com.jekael.adoel.data.formatShiftDate
import com.jekael.adoel.data.formatShiftShortDate
import com.jekael.adoel.data.formatShiftTime
import com.jekael.adoel.data.formatYard
import com.jekael.adoel.data.shareIntent
import com.jekael.adoel.data.shiftNumberForEpochMin
import com.jekael.adoel.ui.components.CloseIcon
import com.jekael.adoel.ui.components.EmptyState
import com.jekael.adoel.ui.components.LinearProgressBar
import com.jekael.adoel.ui.components.SlidePanel
import com.jekael.adoel.ui.components.SwipeableCard
import com.jekael.adoel.ui.theme.AppType
import com.jekael.adoel.ui.theme.Cyan400
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.Dimens
import com.jekael.adoel.ui.theme.LocalAppColors
import com.jekael.adoel.ui.theme.Motion
import com.jekael.adoel.ui.theme.elevatedListCard
import com.jekael.adoel.ui.theme.floatingHeaderCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen panel listing archived shifts (see DoffViewModel.finishShift) with simple
 * aggregate productivity stats. Mirrors SettingsDrawer's slide-in shell (no drag-to-dismiss,
 * kept simpler since this is a read-only view).
 */
@Composable
fun StatistikScreen(
    history: List<ShiftRecord>,
    db: Map<String, MesinData>,
    onClose: () -> Unit,
    onDeleteShift: (Int) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
) {
    val colors = LocalAppColors.current
    var expandedShiftId by remember { mutableStateOf<Int?>(null) }
    var headerHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    SlidePanel(onClose = onClose) { requestClose ->
        // Same "floating header overlays a full-bleed scrollable list" concept as MainScreen —
        // the list is measured/laid out from the very top and scrolls behind the header, instead
        // of just sitting in a Column below it.
        Box(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            if (history.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState(
                        title = "Belum ada riwayat shift",
                        subtitle = "Riwayat akan tersimpan otomatis setiap kali kamu tekan Selesai Shift",
                    )
                }
            } else {
                val totalDoff = history.sumOf { it.aktual.size }
                val avgPerShift = totalDoff.toFloat() / history.size
                val maxDoffCount = (history.maxOfOrNull { it.aktual.size } ?: 1).coerceAtLeast(1)
                val listState = rememberLazyListState()
                val scope = rememberCoroutineScope()

                fun jumpToShift(shift: ShiftRecord) {
                    expandedShiftId = shift.id
                    val index = history.indexOfFirst { it.id == shift.id }
                    if (index >= 0) scope.launch { listState.animateScrollToItem(index + 1) }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 10.dp + headerHeight + 16.dp,
                        bottom = 20.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        AggregateStatsCard(
                            history = history,
                            totalDoff = totalDoff,
                            avgPerShift = avgPerShift,
                            selectedShiftId = expandedShiftId,
                            onBarClick = { jumpToShift(it) },
                        )
                    }
                    items(history, key = { it.id }) { shift ->
                        ShiftRow(
                            shift = shift,
                            db = db,
                            maxDoffCount = maxDoffCount,
                            expanded = expandedShiftId == shift.id,
                            onToggle = { expandedShiftId = if (expandedShiftId == shift.id) null else shift.id },
                            onDeleteShift = onDeleteShift,
                            showConfirm = showConfirm,
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }

            // Floating header — overlays the list (list scrolls behind it), matching
            // MainScreen's header/console bar look: shadow + rounded corners + a subtle border
            // (shadows alone barely read on a near-black dark background).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        headerHeight = with(density) { coords.size.height.toDp() }
                    }
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .floatingHeaderCard(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Statistik", style = AppType.DialogTitle.copy(color = colors.textPrimary))
                    IconButton(onClick = { requestClose() }) {
                        CloseIcon()
                    }
                }
            }
        }
    }
}

@Composable
private fun AggregateStatsCard(
    history: List<ShiftRecord>,
    totalDoff: Int,
    avgPerShift: Float,
    selectedShiftId: Int?,
    onBarClick: (ShiftRecord) -> Unit,
) {
    val colors = LocalAppColors.current
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedTotal by animateIntAsState(
        targetValue = if (started) totalDoff else 0,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "totalDoff",
    )
    val animatedShifts by animateIntAsState(
        targetValue = if (started) history.size else 0,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "shiftCount",
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.bgElevated)
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            StatFigure(label = "Total doff", value = "$animatedTotal")
            StatFigure(label = "Shift", value = "$animatedShifts")
            StatFigure(label = "Rata-rata/shift", value = "%.1f".format(avgPerShift))
        }
        Spacer(Modifier.height(12.dp))
        DoffCountChart(history = history, selectedShiftId = selectedShiftId, onBarClick = onBarClick)
    }
}

@Composable
private fun StatFigure(label: String, value: String) {
    val colors = LocalAppColors.current
    Column {
        Text(value, style = AppType.NumberLarge.copy(color = colors.textPrimary))
        Text(label, style = TextStyle(fontSize = 11.sp, color = colors.textFaint))
    }
}

/** Bar chart of doff count for the most recent shifts, oldest on the left — each bar carries its
 * own count label and a short date underneath, with a baseline so heights read unambiguously.
 * Tapping a bar jumps the list below to that shift's row and expands it, bridging chart and detail. */
@Composable
private fun DoffCountChart(history: List<ShiftRecord>, selectedShiftId: Int?, onBarClick: (ShiftRecord) -> Unit) {
    val colors = LocalAppColors.current
    val recent = history.take(10).asReversed()
    if (recent.isEmpty()) return
    val maxCount = (recent.maxOfOrNull { it.aktual.size } ?: 1).coerceAtLeast(1)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(60.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            recent.forEachIndexed { index, shift ->
                // key(shift.id) (not just positional remember) — without it, deleting an earlier
                // shift shifts every later shift's position in `recent`, and a plain positional
                // remember would then hand a DIFFERENT shift's already-settled Animatable to a bar
                // that just moved into that slot, misanimating it as if its own count had changed.
                key(shift.id) {
                    val targetFraction = shift.aktual.size.toFloat() / maxCount
                    val animatedFraction = remember { Animatable(0f) }
                    LaunchedEffect(shift.id, targetFraction) {
                        delay(index * Motion.CHART_STAGGER_STEP_MS)
                        animatedFraction.animateTo(targetFraction, animationSpec = tween(450, easing = FastOutSlowInEasing))
                    }
                    val selected = shift.id == selectedShiftId
                    val barColor by animateColorAsState(
                        if (selected) Cyan400 else Cyan500.copy(alpha = 0.75f),
                        label = "barColor",
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(
                                onClickLabel = "Lihat detail Shift ${shiftNumberForEpochMin(shift.startedAtEpochMin)} · ${shift.aktual.size} doff",
                                onClick = { onBarClick(shift) },
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            "${shift.aktual.size}",
                            style = TextStyle(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Cyan400 else colors.textSecondary,
                            ),
                        )
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((44.dp * animatedFraction.value).coerceAtLeast(4.dp))
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor),
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = colors.border, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            recent.forEach { shift ->
                Text(
                    text = formatShiftShortDate(shift.startedAtEpochMin),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = TextStyle(fontSize = 9.sp, color = colors.textFaint),
                )
            }
        }
    }
}

@Composable
private fun ShiftRow(
    shift: ShiftRecord,
    db: Map<String, MesinData>,
    maxDoffCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onDeleteShift: (Int) -> Unit,
    showConfirm: (String, () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val shiftNo = remember(shift.startedAtEpochMin) { shiftNumberForEpochMin(shift.startedAtEpochMin) }
    val dateStr = remember(shift.startedAtEpochMin) { formatShiftDate(shift.startedAtEpochMin) }
    val timeRange = remember(shift.startedAtEpochMin, shift.endedAtEpochMin) {
        "${formatShiftTime(shift.startedAtEpochMin)}–${formatShiftTime(shift.endedAtEpochMin)}"
    }
    val chronological = remember(shift.aktual) { shift.aktual.asReversed() }
    val avgGapMin = remember(chronological) {
        val stamped = chronological.mapNotNull { it.tsEpochMin }
        if (stamped.size >= 2) stamped.zipWithNext { a, b -> b - a }.average() else null
    }

    // Bagikan langsung — bukan salin, supaya tidak perlu ganti aplikasi lalu tempel manual.
    // Tidak berarti apa-apa untuk shift tanpa doff (mis. diarsipkan dengan estimasi yang belum
    // sempat diselesaikan), jadi swipe-kanan pada shift kosong tidak melakukan apa-apa.
    fun requestShare() {
        if (shift.aktual.isNotEmpty()) shareShift(context, shift, db)
    }
    fun requestDelete() {
        showConfirm("Hapus arsip Shift $shiftNo · $dateStr? Data ini tidak bisa dikembalikan.") {
            onDeleteShift(shift.id)
        }
    }

    SwipeableCard(
        modifier = modifier,
        onSwipeRight = { requestShare() },
        onSwipeLeft = { requestDelete() },
        rightIcon = Icons.Outlined.Share,
        leftIcon = Icons.Outlined.Delete,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedListCard(elevation = 5.dp, backgroundColor = colors.bgElevated)
                .clickable { onToggle() }
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction("Bagikan Shift $shiftNo") { requestShare(); true },
                        CustomAccessibilityAction("Hapus Shift $shiftNo") { requestDelete(); true },
                    )
                }
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "Shift $shiftNo · $dateStr",
                        style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
                    )
                    Text(timeRange, style = AppType.Caption.copy(color = colors.textFaint))
                    Spacer(Modifier.height(6.dp))
                    LinearProgressBar(
                        fraction = shift.aktual.size.toFloat() / maxDoffCount,
                        trackColor = colors.bgElevated2,
                        fillColor = Cyan500,
                        width = 60.dp,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${shift.aktual.size} doff", style = AppType.TabLabel.copy(color = Cyan400))
                    if (avgGapMin != null) {
                        Text(
                            "±${formatDeltaMin(avgGapMin.toLong())}/doff",
                            style = TextStyle(fontSize = 11.sp, color = colors.textFaint),
                        )
                    }
                }
            }

            if (expanded) {
                Spacer(Modifier.height(10.dp))
                chronological.forEach { entry ->
                    val corak = entry.corakOverride ?: db[entry.mcNo]?.corak ?: "—"
                    // Yard sudah terlihat di layar Doffing sebelum "Selesai Shift" mengarsipkannya ke
                    // sini — datanya tetap tersimpan di AktualEntry, jadi riwayat semestinya tetap
                    // menunjukkannya alih-alih diam-diam menghilang begitu shift diarsipkan.
                    val yard = entry.customYard ?: db[entry.mcNo]?.targetYard
                    val corakLine = if (yard != null) "$corak · ${formatYard(yard)}y" else corak
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Mc ${entry.mcNo} · $corakLine · ${entry.ket}", style = AppType.Caption.copy(color = colors.textSecondary))
                        Text(entry.jam, style = AppType.Caption.copy(color = colors.textFaint))
                    }
                }
            }
        }
    }
}

/** Re-share a single archived shift — mirrors [buildShareHistoryText]'s format/tone exactly (same
 * "Bravo!!!" casual register, same audience: rekan kerja), for whenever an operator needs to
 * resend a specific day's record instead of the whole running total. Opens the share-sheet
 * directly instead of a copy-then-paste round trip. */
private fun shareShift(context: Context, shift: ShiftRecord, db: Map<String, MesinData>) {
    shareIntent(context, buildShareShiftText(shift, db), "Bagikan shift")
}
