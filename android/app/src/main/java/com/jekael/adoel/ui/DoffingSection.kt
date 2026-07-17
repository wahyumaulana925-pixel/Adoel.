package com.jekael.adoel.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Texture
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jekael.adoel.ui.components.SwipeableCard
import com.jekael.adoel.ui.components.mesinTipeColor
import com.jekael.adoel.ui.components.mesinTipeIcon
import com.jekael.adoel.ui.theme.*

/** RIWAYAT page's list content: empty state, or the recorded-doff rows (newest first). Bagikan
 * WA / Selesai Shift render as two permanent, high-contrast buttons directly above the list
 * (Master Blueprint §4E) instead of behind a header chevron — shift-closing actions belong right
 * where the operator is already looking once the history page is the thing on screen. */
fun LazyListScope.doffingSection(
    state: DoffState,
    aktualReversed: List<AktualEntry>,
    doffFilter: String,
    onDoffFilterChange: (String) -> Unit,
    onEntryClick: (Int) -> Unit,
    onHapusEntry: (Int) -> Unit,
    onShare: () -> Unit,
    onFinish: () -> Unit,
) {
    item(key = "doff_shift_actions") {
        val colors = LocalAppColors.current
        Row(
            modifier = Modifier.fillMaxWidth().animateItem(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onShare,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
                border = BorderStroke(1.dp, colors.border),
            ) {
                Icon(imageVector = Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Bagikan WA", style = AppType.TabLabel)
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(Dimens.RadiusControl),
                colors = ButtonDefaults.buttonColors(containerColor = Red500),
            ) {
                Icon(imageVector = Icons.Outlined.Flag, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text("Selesai Shift", style = AppType.TabLabel.copy(color = Color.White))
            }
        }
    }
    if (state.aktual.isEmpty()) {
        item(key = "doff_empty") {
            EmptyState(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                title = "Belum Ada Riwayat Doffing",
                subtitle = "Geser kartu mesin di layar Radar untuk mencatat doff, atau ketuk ikon gunting di konsol bawah untuk mencatat doff langsung.",
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
                placeholder = "Cari nomor mesin",
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
            entry.mcNo.contains(doffFilter, ignoreCase = true)
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
    val tipeColor = mesin?.tipe?.let(::mesinTipeColor) ?: colors.textFaint
    val tipeIcon = mesin?.tipe?.let(::mesinTipeIcon) ?: Icons.Outlined.Circle
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
                .elevatedListCard(backgroundColor = colors.bgElevated)
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
            Icon(
                imageVector = tipeIcon,
                contentDescription = null,
                tint = tipeColor,
                modifier = Modifier.size(14.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.mcNo,
                    style = AppType.NumberLarge.copy(color = Cyan500, letterSpacing = (-1).sp),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Same kain marker as RadarCard's corak/yard line (see Icons.Outlined.Texture
                    // there) — this history row shows the identical corak/yard pairing.
                    Icon(
                        imageVector = Icons.Outlined.Texture,
                        contentDescription = null,
                        tint = colors.textFaint,
                        modifier = Modifier.size(11.dp),
                    )
                    Text(
                        text = sub,
                        style = AppType.LabelBold.copy(color = colors.textMuted),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Text(
                text = entry.ket,
                style = AppType.TabLabel.copy(color = colors.textPrimary),
            )
        }
    }
}

internal fun shareHistory(context: Context, state: DoffState, onFailure: () -> Unit = {}) {
    if (!shareIntent(context, buildShareHistoryText(state), "Bagikan riwayat")) onFailure()
}
