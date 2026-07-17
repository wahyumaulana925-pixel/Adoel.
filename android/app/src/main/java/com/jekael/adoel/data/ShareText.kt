package com.jekael.adoel.data

import android.content.Context
import android.content.Intent
import java.util.Calendar
import java.util.TimeZone

/**
 * Shared "Bravo!!!"-toned WhatsApp share text — used both for the currently-running shift
 * (MainScreen/DoffingSection) and for re-sharing a single archived shift (StatistikScreen), kept
 * in one place so the two formats can't drift apart by editing only one call site. Covered by
 * golden-fixture tests (ShareTextTest) since this text is read by other people, not just exercised
 * internally.
 */

/** [nowMillis]/[zone] hanya untuk unit test — call site produksi memakai waktu & zona perangkat
 * saat ini, perilaku tidak berubah. */
fun buildShareHistoryText(
    state: DoffState,
    nowMillis: Long = System.currentTimeMillis(),
    zone: TimeZone = TimeZone.getDefault(),
): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = nowMillis }
    val dateStr = "%02d/%02d/%04d".format(
        cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.YEAR),
    )
    val lines = state.aktual.asReversed().mapIndexed { i, a ->
        val mesin = state.db[a.mcNo]
        val corak = a.corakOverride ?: mesin?.corak ?: "—"
        val yard = a.customYard ?: mesin?.targetYard
        val suffix = if (yard != null) " · ${formatYard(yard)}y" else ""
        "${i + 1}. Mc${a.mcNo} · $corak$suffix · ${a.ket}"
    }
    // Doff selesai saja tidak cukup buat rekan yang baca pesan ini di lantai produksi — mereka
    // juga perlu tahu mesin mana yang masih ditimer sekarang dan kapan harus di-doff, jadi bagian
    // ini ikut disertakan alih-alih cuma riwayat yang sudah selesai.
    val berjalan = sortedByNearest(state.estimasi).map { est ->
        val mesin = state.db[est.mcNo]
        val corak = est.corakOverride ?: mesin?.corak ?: "—"
        val yard = est.yardOverride ?: mesin?.targetYard
        val suffix = if (yard != null) " · ${formatYard(yard)}y" else ""
        "• Mc${est.mcNo} · $corak$suffix · est. ${absMinToTimeStr(est.estAbsMin, zone)}"
    }
    val selesaiCount = state.aktual.size
    val berjalanCount = berjalan.size
    // Spelling the sum out explicitly (bukan cuma "Total: N doff") — tanpa ini, rekan yang baca
    // sering salah jumlah sendiri antara yang sudah selesai vs. yang masih berjalan (lihat contoh
    // nyata: seseorang nanya "jadi total 23 mc?" padahal "Total" di pesan cuma menghitung selesai).
    val totalLine = if (berjalanCount > 0) {
        "Total: $selesaiCount selesai + $berjalanCount berjalan = ${selesaiCount + berjalanCount} mc"
    } else {
        "Total: $selesaiCount doff"
    }
    val berjalanBlock = if (berjalan.isNotEmpty()) {
        "\n\n*Sedang Berjalan ($berjalanCount)*\n${berjalan.joinToString("\n")}"
    } else {
        ""
    }
    return "Bravo!!!\n$dateStr\n\n*Selesai ($selesaiCount doff)*\n${lines.joinToString("\n")}$berjalanBlock\n\n$totalLine"
}

/** Re-share a single archived shift — mirrors [buildShareHistoryText]'s format/tone exactly (same
 * casual "Bravo!!!" register, same audience: rekan kerja), for whenever an operator needs to
 * resend a specific day's record instead of the whole running total.
 *
 * [zone] hanya untuk unit test — call site produksi memakai default zona perangkat. */
fun buildShareShiftText(shift: ShiftRecord, db: Map<String, MesinData>, zone: TimeZone = TimeZone.getDefault()): String {
    val shiftNo = shiftNumberForEpochMin(shift.startedAtEpochMin, zone)
    val dateStr = formatShiftDate(shift.startedAtEpochMin, zone)
    val lines = shift.aktual.asReversed().mapIndexed { i, a ->
        val mesin = db[a.mcNo]
        val corak = a.corakOverride ?: mesin?.corak ?: "—"
        val yard = a.customYard ?: mesin?.targetYard
        val suffix = if (yard != null) " · ${formatYard(yard)}y" else ""
        "${i + 1}. Mc${a.mcNo} · $corak$suffix · ${a.ket}"
    }
    return "Bravo!!!\nShift $shiftNo · $dateStr\n\n*Selesai (${shift.aktual.size} doff)*\n${lines.joinToString("\n")}\n\nTotal: ${shift.aktual.size} doff"
}

/** Launches the system share sheet with [text] — shared by both callers above. Returns false
 * (instead of swallowing the failure silently) when no share-capable app could be launched, so
 * callers can let the operator know instead of the tap silently doing nothing. */
fun shareIntent(context: Context, text: String, chooserTitle: String): Boolean {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return runCatching { context.startActivity(Intent.createChooser(intent, chooserTitle)) }.isSuccess
}
