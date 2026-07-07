package com.jekael.adoel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FreeBreakfast
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.components.*
import com.jekael.adoel.ui.theme.*

/** ESTIMASI mode's list content: header + empty state, or the Segera/Menunggu urgency bands. */
internal fun LazyListScope.estimasiSection(
    radarList: List<Estimasi>,
    segeraList: List<Estimasi>,
    menungguList: List<Estimasi>,
    menungguRows: List<MenungguRow>,
    menungguAccent: Color,
    db: Map<String, MesinData>,
    nowAbs: Long,
    radarFilter: String,
    onRadarFilterChange: (String) -> Unit,
    onDoff: (String) -> Unit,
    onHapus: (String) -> Unit,
    onQuickEdit: (String) -> Unit,
) {
    item(key = "est_header") {
        SectionHeader(title = "Estimasi", count = radarList.size)
    }
    if (radarList.isEmpty()) {
        item(key = "est_empty") {
            EmptyState(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp))
        }
        return
    }
    // Only worth showing once there's more than a handful to scan through — for a couple of
    // machines a filter field is just clutter above the very thing it's meant to help find.
    if (radarList.size > 4) {
        item(key = "est_filter") {
            RadarFilterField(
                value = radarFilter,
                onValueChange = onRadarFilterChange,
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
    }
    if (segeraList.isEmpty() && menungguList.isEmpty()) {
        item(key = "est_filter_empty") {
            EmptyState(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                title = "Tidak ditemukan",
                subtitle = "Coba kata kunci lain — cari berdasarkan nomor mesin atau corak",
            )
        }
        return
    }
    if (segeraList.isNotEmpty()) {
        item(key = "segera_head") {
            UrgencyBandHeader(label = "Segera", color = Red400, modifier = Modifier.animateItem())
        }
        itemsIndexed(segeraList, key = { _, est -> est.mcNo }) { index, est ->
            RadarCard(
                est = est,
                mesin = db[est.mcNo],
                nowAbs = nowAbs,
                onDoff = { onDoff(est.mcNo) },
                onHapus = { onHapus(est.mcNo) },
                onQuickEdit = { onQuickEdit(est.mcNo) },
                modifier = Modifier.animateItem(),
                entranceDelayMs = (index * 35L).coerceAtMost(400L),
            )
        }
    }
    if (menungguList.isNotEmpty()) {
        item(key = "menunggu_head") {
            UrgencyBandHeader(label = "Menunggu", color = menungguAccent, modifier = Modifier.animateItem())
        }
        itemsIndexed(
            menungguRows,
            key = { _, row ->
                when (row) {
                    is MenungguRow.CardRow -> row.est.mcNo
                    is MenungguRow.GapRow -> "gap_after_${row.afterMcNo}"
                }
            },
        ) { index, row ->
            when (row) {
                is MenungguRow.CardRow -> RadarCard(
                    est = row.est,
                    mesin = db[row.est.mcNo],
                    nowAbs = nowAbs,
                    onDoff = { onDoff(row.est.mcNo) },
                    onHapus = { onHapus(row.est.mcNo) },
                    onQuickEdit = { onQuickEdit(row.est.mcNo) },
                    modifier = Modifier.animateItem(),
                    entranceDelayMs = (index * 35L).coerceAtMost(400L),
                )
                is MenungguRow.GapRow -> BreakGapCard(
                    gapMin = row.gapMin,
                    nextMcNo = row.nextMcNo,
                    nextAbsMin = row.nextAbsMin,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun UrgencyBandHeader(label: String, color: Color, modifier: Modifier = Modifier) {
    val animatedColor by animateColorAsState(color, animationSpec = tween(300), label = "urgencyBandColor")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(animatedColor),
            )
            Text(
                text = label,
                style = AppType.LabelBold.copy(color = animatedColor),
            )
        }
    }
}

/** Sits between two RadarCards in the Menunggu band when the gap to the next doff is long
 * enough to actually step away — tells the operator how long, and what's next when they're back. */
@Composable
private fun BreakGapCard(gapMin: Long, nextMcNo: String, nextAbsMin: Long, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgElevated2.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.FreeBreakfast,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(18.dp),
        )
        Column {
            Text(
                text = "Jeda ${formatDeltaMin(gapMin)} — waktu istirahat",
                style = AppType.LabelSmallBold.copy(color = colors.textSecondary),
            )
            Text(
                text = "Sebelum Mc $nextMcNo · ${absMinToTimeStr(nextAbsMin)}",
                style = AppType.Caption.copy(color = colors.textFaint),
            )
        }
    }
}

/** Filters the Segera/Menunggu bands by mc number or corak — lets an operator jump straight to a
 * machine instead of scanning past everything else when a lot of machines are running at once. */
@Composable
private fun RadarFilterField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text("Cari nomor mesin atau corak", style = AppType.Caption.copy(color = colors.textFaint)) },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(50.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        singleLine = true,
    )
}
