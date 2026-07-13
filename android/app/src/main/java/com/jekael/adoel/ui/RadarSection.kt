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
import androidx.compose.ui.graphics.lerp
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
            ListFilterField(
                value = radarFilter,
                onValueChange = onRadarFilterChange,
                placeholder = "Cari nomor mesin atau corak",
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
                entranceDelayMs = (index * Motion.LIST_STAGGER_STEP_MS).coerceAtMost(Motion.LIST_STAGGER_MAX_MS),
            )
        }
    }
    if (menungguList.isNotEmpty()) {
        item(key = "menunggu_head") {
            UrgencyBandHeader(label = "Menunggu", color = menungguAccent, modifier = Modifier.animateItem())
        }
        // Cards within REMINDER_LEAD_MIN of their estimate (same threshold as the reminder
        // notification and the machines' own physical warning light) render full width, same as
        // the always-overdue Segera band above — everything further out is paired two-per-row
        // into a compact grid instead. GapRow (break-time card) always stays full width alone,
        // since it's about a time gap, not any single machine's urgency.
        val renderGroups = groupMenungguRowsForGrid(menungguRows, nowAbs)
        itemsIndexed(renderGroups, key = { _, group -> group.key }) { index, group ->
            val entranceDelayMs = (index * Motion.LIST_STAGGER_STEP_MS).coerceAtMost(Motion.LIST_STAGGER_MAX_MS)
            when {
                // A GapRow, or a CardRow within REMINDER_LEAD_MIN/overdue — genuinely wide, full row.
                group.rows.size == 1 && group.isWide -> when (val row = group.rows[0]) {
                    is MenungguRow.CardRow -> RadarCard(
                        est = row.est,
                        mesin = db[row.est.mcNo],
                        nowAbs = nowAbs,
                        onDoff = { onDoff(row.est.mcNo) },
                        onHapus = { onHapus(row.est.mcNo) },
                        onQuickEdit = { onQuickEdit(row.est.mcNo) },
                        modifier = Modifier.animateItem(),
                        entranceDelayMs = entranceDelayMs,
                    )
                    is MenungguRow.GapRow -> BreakGapCard(
                        gapMin = row.gapMin,
                        nextMcNo = row.nextMcNo,
                        nextAbsMin = row.nextAbsMin,
                        nowAbs = nowAbs,
                        modifier = Modifier.animateItem(),
                    )
                }
                // Grid-eligible but left without a partner (e.g. a GapRow or an odd count broke the
                // pairing) — stays half-width with an empty second slot instead of expanding to
                // full width, so it still reads as "not urgent" like its paired siblings.
                group.rows.size == 1 -> Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val row = group.rows[0] as MenungguRow.CardRow
                    RadarCard(
                        est = row.est,
                        mesin = db[row.est.mcNo],
                        nowAbs = nowAbs,
                        onDoff = { onDoff(row.est.mcNo) },
                        onHapus = { onHapus(row.est.mcNo) },
                        onQuickEdit = { onQuickEdit(row.est.mcNo) },
                        modifier = Modifier.weight(1f),
                        entranceDelayMs = entranceDelayMs,
                    )
                    Spacer(Modifier.weight(1f))
                }
                else -> Row(
                    modifier = Modifier.fillMaxWidth().animateItem(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    group.rows.forEach { row ->
                        row as MenungguRow.CardRow
                        RadarCard(
                            est = row.est,
                            mesin = db[row.est.mcNo],
                            nowAbs = nowAbs,
                            onDoff = { onDoff(row.est.mcNo) },
                            onHapus = { onHapus(row.est.mcNo) },
                            onQuickEdit = { onQuickEdit(row.est.mcNo) },
                            modifier = Modifier.weight(1f),
                            entranceDelayMs = entranceDelayMs,
                        )
                    }
                }
            }
        }
    }
}

/** One render slot in the Menunggu band. [isWide] distinguishes a genuinely urgent/gap full-width
 * row from a grid-eligible CardRow that just didn't find a pairing partner (e.g. a GapRow forced
 * a flush, or an odd count) — the latter still renders half-width so it doesn't misleadingly look
 * as urgent as a real wide card. */
private class MenungguRenderGroup(val key: String, val rows: List<MenungguRow>, val isWide: Boolean)

private fun groupMenungguRowsForGrid(menungguRows: List<MenungguRow>, nowAbs: Long): List<MenungguRenderGroup> {
    val groups = mutableListOf<MenungguRenderGroup>()
    var pending: MenungguRow.CardRow? = null
    fun flushPending() {
        pending?.let { groups += MenungguRenderGroup("grid_${it.est.mcNo}", listOf(it), isWide = false) }
        pending = null
    }
    for (row in menungguRows) {
        val isWide = when (row) {
            is MenungguRow.GapRow -> true
            is MenungguRow.CardRow -> (row.est.estAbsMin - nowAbs) <= REMINDER_LEAD_MIN
        }
        if (isWide) {
            // A GapRow must flush any pending grid card first — it can't be reordered to appear
            // after the gap just to find a pairing partner, since that would render it later than
            // its actual chronological position in the list.
            flushPending()
            val key = when (row) {
                is MenungguRow.CardRow -> row.est.mcNo
                is MenungguRow.GapRow -> "gap_after_${row.afterMcNo}"
            }
            groups += MenungguRenderGroup(key, listOf(row), isWide = true)
        } else {
            val cardRow = row as MenungguRow.CardRow
            val prev = pending
            if (prev == null) {
                pending = cardRow
            } else {
                groups += MenungguRenderGroup("grid_${prev.est.mcNo}_${cardRow.est.mcNo}", listOf(prev, cardRow), isWide = false)
                pending = null
            }
        }
    }
    flushPending()
    return groups
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

/** Sits between two RadarCards in the Menunggu band when the gap to the next doff is long enough
 * to actually step away. The remaining minutes are the headline (same big/bold treatment as a
 * RadarCard's countdown) so an operator reads "how long do I have" at a glance instead of doing
 * the subtraction themselves between the two neighboring cards' times — the whole point of this
 * card existing. Emerald is used nowhere in the urgency scale (Cyan/Amber/Orange/Red), so this
 * reads as "good news" rather than competing with any urgency color. */
@Composable
private fun BreakGapCard(gapMin: Long, nextMcNo: String, nextAbsMin: Long, nowAbs: Long, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    val remainingMin = (nextAbsMin - nowAbs).coerceAtLeast(0)
    val elapsedFraction = if (gapMin > 0) (1f - remainingMin.toFloat() / gapMin).coerceIn(0f, 1f) else 1f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .elevatedListCard(backgroundColor = lerp(colors.bgElevated, Emerald500, 0.08f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = Icons.Outlined.FreeBreakfast,
                contentDescription = null,
                tint = Emerald500,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "WAKTU ISTIRAHAT",
                style = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Emerald500),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatDeltaMin(remainingMin),
            style = TextStyle(
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = colors.textPrimary,
            ),
        )
        Text(
            text = "Istirahat sampai ${absMinToTimeStr(nextAbsMin)} — sebelum Mc $nextMcNo",
            style = AppType.Caption.copy(color = colors.textFaint),
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.bgElevated2),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(elapsedFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(2.dp))
                    .background(Emerald500),
            )
        }
    }
}

/** Shared search box for both the radar (ESTIMASI) and doffing (AKTUAL) lists — lets an operator
 * jump straight to a machine instead of scanning past everything else when a lot are on screen. */
@Composable
internal fun ListFilterField(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, style = AppType.Caption.copy(color = colors.textFaint)) },
        colors = outlinedFieldColors(),
        shape = RoundedCornerShape(50.dp),
        textStyle = AppType.FieldText.copy(color = colors.textPrimary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        singleLine = true,
    )
}
