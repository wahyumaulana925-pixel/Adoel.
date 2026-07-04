package com.jekael.adoel.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.MesinData
import com.jekael.adoel.data.ShiftRecord
import com.jekael.adoel.data.formatDeltaMin
import com.jekael.adoel.data.shiftNumberForEpochMin
import com.jekael.adoel.ui.components.CloseIcon
import com.jekael.adoel.ui.components.LinearProgressBar
import com.jekael.adoel.ui.components.TrashIcon
import com.jekael.adoel.ui.theme.Cyan400
import com.jekael.adoel.ui.theme.Cyan500
import com.jekael.adoel.ui.theme.LocalAppColors
import kotlinx.coroutines.delay
import java.util.Calendar

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
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun requestClose() {
        visible = false
    }
    LaunchedEffect(visible) {
        if (!visible) {
            delay(220)
            onClose()
        }
    }
    BackHandler(enabled = visible) { requestClose() }

    var expandedShiftId by remember { mutableStateOf<Int?>(null) }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(260)),
        exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(220)),
    ) {
        Column(modifier = Modifier.fillMaxSize().background(colors.bg)) {
            // Floating header — matches the header/console bar's shadow + rounded-corner look
            // instead of sitting flat directly on colors.bg like the rest of this full-bleed panel.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 12.dp)
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp))
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.bgElevated),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Statistik", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary))
                    IconButton(onClick = { requestClose() }) {
                        CloseIcon()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        AggregateStatsCard(history = history, totalDoff = totalDoff, avgPerShift = avgPerShift)
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
        }
    }
}

@Composable
private fun AggregateStatsCard(history: List<ShiftRecord>, totalDoff: Int, avgPerShift: Float) {
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
        DoffCountChart(history)
    }
}

@Composable
private fun StatFigure(label: String, value: String) {
    val colors = LocalAppColors.current
    Column {
        Text(value, style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = colors.textPrimary))
        Text(label, style = TextStyle(fontSize = 11.sp, color = colors.textFaint))
    }
}

/** Bar chart of doff count for the most recent shifts, oldest on the left — each bar carries its
 * own count label and a short date underneath, with a baseline so heights read unambiguously. */
@Composable
private fun DoffCountChart(history: List<ShiftRecord>) {
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
                val targetFraction = shift.aktual.size.toFloat() / maxCount
                val animatedFraction = remember { Animatable(0f) }
                LaunchedEffect(shift.id, targetFraction) {
                    delay(index * 50L)
                    animatedFraction.animateTo(targetFraction, animationSpec = tween(450, easing = FastOutSlowInEasing))
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Text(
                        "${shift.aktual.size}",
                        style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary),
                    )
                    Spacer(Modifier.height(2.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((44.dp * animatedFraction.value).coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(3.dp))
                            .background(Cyan500),
                    )
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgElevated)
            .clickable { onToggle() }
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
                Text(timeRange, style = TextStyle(fontSize = 12.sp, color = colors.textFaint))
                Spacer(Modifier.height(6.dp))
                LinearProgressBar(
                    fraction = shift.aktual.size.toFloat() / maxDoffCount,
                    trackColor = colors.bgElevated2,
                    fillColor = Cyan500,
                    width = 60.dp,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("${shift.aktual.size} doff", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Cyan400))
                    if (avgGapMin != null) {
                        Text(
                            "±${formatDeltaMin(avgGapMin.toLong())}/doff",
                            style = TextStyle(fontSize = 11.sp, color = colors.textFaint),
                        )
                    }
                }
                IconButton(onClick = {
                    showConfirm("Hapus arsip Shift $shiftNo · $dateStr? Data ini tidak bisa dikembalikan.") {
                        onDeleteShift(shift.id)
                    }
                }) {
                    TrashIcon()
                }
            }
        }

        if (expanded) {
            Spacer(Modifier.height(10.dp))
            chronological.forEach { entry ->
                val corak = entry.corakOverride ?: db[entry.mcNo]?.corak ?: "—"
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Mc ${entry.mcNo} · $corak · ${entry.ket}", style = TextStyle(fontSize = 12.sp, color = colors.textSecondary))
                    Text(entry.jam, style = TextStyle(fontSize = 12.sp, color = colors.textFaint))
                }
            }
        }
    }
}

private fun formatShiftDate(epochMin: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMin * 60000L }
    return "%02d/%02d/%04d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
}

private fun formatShiftShortDate(epochMin: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMin * 60000L }
    return "%02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
}

private fun formatShiftTime(epochMin: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = epochMin * 60000L }
    return "%02d.%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}
