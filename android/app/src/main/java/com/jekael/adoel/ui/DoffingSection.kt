package com.jekael.adoel.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.components.EmptyState
import com.jekael.adoel.ui.components.SectionHeader
import com.jekael.adoel.ui.components.SwipeableCard
import com.jekael.adoel.ui.components.mesinTipeColor
import com.jekael.adoel.ui.theme.*

/** DOFFING mode's list content: header, share/statistik/finish actions, empty state or the
 * recorded-doff rows (newest first). */
fun LazyListScope.doffingSection(
    state: DoffState,
    aktualReversed: List<AktualEntry>,
    doffFilter: String,
    onDoffFilterChange: (String) -> Unit,
    onShare: () -> Unit,
    onStatistik: () -> Unit,
    onFinish: () -> Unit,
    onEntryClick: (Int) -> Unit,
    onHapusEntry: (Int) -> Unit,
) {
    item(key = "doff_header") {
        SectionHeader(title = "Doffing", count = state.aktual.size)
    }
    // Always shown — Statistik reads state.history, which survives even when the live aktual
    // list is empty right after "Selesai Shift".
    item(key = "doff_actions") {
        DoffingActions(onShare = onShare, onStatistik = onStatistik, onFinish = onFinish)
    }
    if (state.aktual.isEmpty()) {
        item(key = "doff_empty") {
            EmptyState(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                title = "Belum ada doff",
                subtitle = "Doff akan muncul di sini setelah kamu proses baris di ESTIMASI/AKTUAL",
            )
        }
        return
    }
    // Only worth showing once there's more than a handful to scan through — mirrors the same
    // threshold used for the radar list's filter.
    if (aktualReversed.size > 4) {
        item(key = "doff_filter") {
            ListFilterField(
                value = doffFilter,
                onValueChange = onDoffFilterChange,
                placeholder = "Cari nomor mesin, corak, atau keterangan",
                modifier = Modifier.fillMaxWidth().animateItem(),
            )
        }
    }
    // Filtered via withIndex() (not a fresh 1..N over the filtered subset) so the displayed
    // number still reflects each entry's true position in this shift's doff order, not its
    // position among just the search matches.
    val filteredIndexed = if (doffFilter.isBlank()) {
        aktualReversed.withIndex().toList()
    } else {
        aktualReversed.withIndex().filter { (_, entry) ->
            val corak = entry.corakOverride ?: state.db[entry.mcNo]?.corak ?: ""
            entry.mcNo.contains(doffFilter, ignoreCase = true) ||
                corak.contains(doffFilter, ignoreCase = true) ||
                entry.ket.contains(doffFilter, ignoreCase = true)
        }
    }
    if (filteredIndexed.isEmpty()) {
        item(key = "doff_filter_empty") {
            EmptyState(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                title = "Tidak ditemukan",
                subtitle = "Coba kata kunci lain — cari berdasarkan nomor mesin, corak, atau keterangan",
            )
        }
        return
    }
    items(filteredIndexed, key = { (_, entry) -> entry.id }) { (idx, entry) ->
        DoffingRow(
            entry = entry,
            mesin = state.db[entry.mcNo],
            num = idx + 1,
            onEdit = { onEntryClick(entry.id) },
            onHapus = { onHapusEntry(entry.id) },
            modifier = Modifier.animateItem(),
        )
    }
}

@Composable
private fun DoffingActions(onShare: () -> Unit, onStatistik: () -> Unit, onFinish: () -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
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

@Composable
private fun DoffingRow(
    entry: AktualEntry,
    mesin: MesinData?,
    num: Int,
    onEdit: () -> Unit,
    onHapus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    val sub = when {
        entry.customYard != null -> "$corak · ${formatYard(entry.customYard)}y"
        mesin?.targetYard != null -> "$corak · ${formatYard(mesin.targetYard)}y"
        else -> corak
    }
    val dotColor = mesin?.tipe?.let(::mesinTipeColor) ?: colors.textFaint
    // Swipe right = edit (same as tapping the card), swipe left = hapus — matches RadarCard's
    // swipe model. Tap still opens edit too, both as a fallback for anyone who doesn't swipe and
    // because TalkBack can't perform the drag gesture at all (see customActions below for that).
    SwipeableCard(
        modifier = modifier.fillMaxWidth(),
        onSwipeRight = onEdit,
        onSwipeLeft = onHapus,
        rightIcon = Icons.Outlined.Edit,
        leftIcon = Icons.Outlined.Delete,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .elevatedListCard(elevation = 5.dp, backgroundColor = colors.bgElevated)
                .clickable(onClick = onEdit)
                .semantics(mergeDescendants = true) {
                    customActions = listOf(
                        CustomAccessibilityAction("Edit riwayat Mc ${entry.mcNo}") { onEdit(); true },
                        CustomAccessibilityAction("Hapus riwayat Mc ${entry.mcNo}") { onHapus(); true },
                    )
                }
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "$num",
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Black, color = colors.textMuted),
                modifier = Modifier.width(22.dp),
            )
            Box(Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.mcNo,
                    style = AppType.NumberLarge.copy(color = Cyan500, letterSpacing = (-1).sp),
                )
                Text(
                    text = sub,
                    style = AppType.LabelBold.copy(color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = entry.ket,
                style = AppType.TabLabel.copy(color = colors.textPrimary),
            )
        }
    }
}

internal fun shareHistory(context: Context, state: DoffState) {
    shareIntent(context, buildShareHistoryText(state), "Bagikan riwayat")
}
