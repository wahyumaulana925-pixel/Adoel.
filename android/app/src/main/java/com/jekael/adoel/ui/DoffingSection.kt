package com.jekael.adoel.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.jekael.adoel.data.*
import com.jekael.adoel.ui.theme.*
import java.util.Calendar

/** DOFFING mode's list content: header, share/statistik/finish actions, empty state or the
 * recorded-doff rows (newest first). */
fun LazyListScope.doffingSection(
    state: DoffState,
    aktualReversed: List<AktualEntry>,
    onShare: () -> Unit,
    onStatistik: () -> Unit,
    onFinish: () -> Unit,
    onEntryClick: (Int) -> Unit,
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
    itemsIndexed(aktualReversed, key = { _, e -> e.id }) { idx, entry ->
        DoffingRow(
            entry = entry,
            mesin = state.db[entry.mcNo],
            num = idx + 1,
            onClick = { onEntryClick(entry.id) },
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
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Bagikan", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
        OutlinedButton(
            onClick = onStatistik,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textSecondary),
            border = BorderStroke(1.dp, colors.border),
        ) { Text("Statistik", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Red400),
            border = BorderStroke(1.dp, Red700.copy(alpha = 0.5f)),
        ) { Text("Selesai Shift", style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.SemiBold)) }
    }
}

@Composable
private fun DoffingRow(
    entry: AktualEntry,
    mesin: MesinData?,
    num: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalAppColors.current
    val corak = entry.corakOverride ?: mesin?.corak ?: "—"
    val sub = when {
        entry.customYard != null -> "$corak · ${formatYard(entry.customYard)}y"
        mesin?.targetYard != null -> "$corak · ${formatYard(mesin.targetYard)}y"
        else -> corak
    }
    val dotColor = when (mesin?.tipe) {
        MesinTipe.TAPPET -> Teal500
        MesinTipe.CAM -> Violet500
        MesinTipe.D405 -> Amber500
        MesinTipe.D408 -> Sky500
        null -> colors.textFaint
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(14.dp), ambientColor = Color.Black.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(14.dp))
            .background(colors.bgElevated)
            .clickable(onClick = onClick)
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
                style = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Black, color = Cyan500, letterSpacing = (-1).sp),
            )
            Text(
                text = sub,
                style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = colors.textMuted),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = entry.ket,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary),
        )
    }
}

internal fun shareHistory(context: Context, state: DoffState) {
    val cal = Calendar.getInstance()
    val dateStr = "%02d/%02d/%04d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR),
    )
    val lines = state.aktual.asReversed().mapIndexed { i, a ->
        val mesin = state.db[a.mcNo]
        val corak = a.corakOverride ?: mesin?.corak ?: "—"
        val yard = a.customYard ?: mesin?.targetYard
        val suffix = if (yard != null) " [${formatYard(yard)}y]" else ""
        "${i + 1}. Mc${a.mcNo} - $corak$suffix - ${a.ket}"
    }
    // Doff selesai saja tidak cukup buat rekan yang baca pesan ini di lantai produksi — mereka
    // juga perlu tahu mesin mana yang masih ditimer sekarang dan kapan harus di-doff, jadi bagian
    // ini ikut disertakan alih-alih cuma riwayat yang sudah selesai.
    val berjalan = sortedByNearest(state.estimasi).map { est ->
        val mesin = state.db[est.mcNo]
        val corak = est.corakOverride ?: mesin?.corak ?: "—"
        val yard = est.yardOverride ?: mesin?.targetYard
        val suffix = if (yard != null) " [${formatYard(yard)}y]" else ""
        "• Mc${est.mcNo} - $corak$suffix - est. ${absMinToTimeStr(est.estAbsMin)}"
    }
    val berjalanBlock = if (berjalan.isNotEmpty()) {
        "\n\nSedang Berjalan:\n${berjalan.joinToString("\n")}"
    } else {
        ""
    }
    val text = "Bravo!!!\n$dateStr\n\n${lines.joinToString("\n")}\n\nTotal: ${state.aktual.size} doff$berjalanBlock"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    runCatching { context.startActivity(Intent.createChooser(intent, "Bagikan riwayat")) }
}
