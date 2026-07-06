package com.jekael.adoel.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
    if (segeraList.isNotEmpty()) {
        item(key = "segera_head") {
            UrgencyBandHeader(label = "Segera", color = Red400, modifier = Modifier.animateItem())
        }
        items(segeraList, key = { it.mcNo }) { est ->
            RadarCard(
                est = est,
                mesin = db[est.mcNo],
                nowAbs = nowAbs,
                onDoff = { onDoff(est.mcNo) },
                onHapus = { onHapus(est.mcNo) },
                onQuickEdit = { onQuickEdit(est.mcNo) },
                modifier = Modifier.animateItem(),
            )
        }
    }
    if (menungguList.isNotEmpty()) {
        item(key = "menunggu_head") {
            UrgencyBandHeader(label = "Menunggu", color = menungguAccent, modifier = Modifier.animateItem())
        }
        items(
            menungguRows,
            key = { row ->
                when (row) {
                    is MenungguRow.CardRow -> row.est.mcNo
                    is MenungguRow.GapRow -> "gap_after_${row.afterMcNo}"
                }
            },
        ) { row ->
            when (row) {
                is MenungguRow.CardRow -> RadarCard(
                    est = row.est,
                    mesin = db[row.est.mcNo],
                    nowAbs = nowAbs,
                    onDoff = { onDoff(row.est.mcNo) },
                    onHapus = { onHapus(row.est.mcNo) },
                    onQuickEdit = { onQuickEdit(row.est.mcNo) },
                    modifier = Modifier.animateItem(),
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
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = animatedColor),
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
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, color = colors.textSecondary),
            )
            Text(
                text = "Sebelum Mc $nextMcNo · ${absMinToTimeStr(nextAbsMin)}",
                style = TextStyle(fontSize = 12.sp, color = colors.textFaint),
            )
        }
    }
}
