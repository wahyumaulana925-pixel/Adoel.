package com.jekael.adoel.data

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

enum class MesinTipe { TAPPET, CAM, D405, D408 }

data class MesinData(
    val tipe: MesinTipe = MesinTipe.TAPPET,
    val corak: String = "-",
    val targetYard: Double? = null,
    val speed: Double? = null,
    val koreksi: Double? = null,
)

data class Estimasi(
    val mcNo: String,
    val estAbsMin: Long,
    val startAbsMin: Long,
    val corakOverride: String? = null,
    val yardOverride: Double? = null,
)

data class AktualEntry(
    val id: Int,
    val mcNo: String,
    val jam: String,
    val ket: String,
    val corakOverride: String? = null,
    val customYard: Double? = null,
    // Null for entries persisted before this field existed (Gson leaves it null on old data).
    // "jam" is only a display string ("HH.mm") that's ambiguous across a midnight-crossing
    // shift; this absolute-minute timestamp lets shift-history stats sort/measure durations
    // correctly regardless of when the entry was recorded relative to midnight.
    val tsEpochMin: Long? = null,
)

/** One archived shift, created when "Selesai Shift" is confirmed (see DoffViewModel.finishShift). */
data class ShiftRecord(
    val id: Int,
    val startedAtEpochMin: Long,
    val endedAtEpochMin: Long,
    val aktual: List<AktualEntry> = emptyList(),
    val estimasiRemaining: Map<String, Estimasi> = emptyMap(),
)

data class DoffState(
    val db: Map<String, MesinData> = emptyMap(),
    val estimasi: Map<String, Estimasi> = emptyMap(),
    val aktual: List<AktualEntry> = emptyList(),
    val nextId: Int = 1,
    val themeMode: String = "SYSTEM",
    val history: List<ShiftRecord> = emptyList(),
    val nextShiftId: Int = 1,
    // Defaults true (already-seen) so existing users upgrading from a version that predates this
    // field don't suddenly get the first-run tutorial — it's only explicitly set false in
    // DoffRepository.parseState()'s genuinely-fresh-install fallback (no persisted state at all).
    val onboardingSeen: Boolean = true,
)

sealed class ProsesResult {
    data class Ok(
        val msg: String,
        val mcNo: String,
        val estAbs: Long? = null,
        val prevEst: Estimasi? = null,
        val undoFn: (() -> Unit)? = null,
    ) : ProsesResult()
    data class Err(val msg: String) : ProsesResult()
}

fun nowAbsMin(): Long = System.currentTimeMillis() / 60000L

/** [zone] hanya untuk unit test — call site produksi memakai default zona perangkat. */
fun absMinToTimeStr(absMin: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = absMin * 60000L }
    return "%02d.%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

/** [zone] hanya untuk unit test — call site produksi memakai default zona perangkat. */
fun formatShiftDate(epochMin: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = epochMin * 60000L }
    return "%02d/%02d/%04d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR))
}

/** [zone] hanya untuk unit test — call site produksi memakai default zona perangkat. */
fun formatShiftShortDate(epochMin: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = epochMin * 60000L }
    return "%02d/%02d".format(cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.MONTH) + 1)
}

/** [zone] hanya untuk unit test — call site produksi memakai default zona perangkat. */
fun formatShiftTime(epochMin: Long, zone: TimeZone = TimeZone.getDefault()): String {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = epochMin * 60000L }
    return "%02d.%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

fun nowTimeStr(): String {
    val cal = Calendar.getInstance()
    return "%02d.%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
}

fun formatDeltaMin(deltaMin: Long): String {
    val sign = if (deltaMin < 0) "−" else ""
    val mag = abs(deltaMin)
    return if (mag >= 60) "$sign${mag / 60}j${mag % 60}m" else "$sign${mag}m"
}

fun formatYard(y: Double): String =
    if (y == y.toLong().toDouble()) y.toLong().toString() else y.toString()

/** Fixed 3-shift schedule: Shift 1 06.00–14.00, Shift 2 14.00–22.00, Shift 3 22.00–06.00
 * (crosses midnight). Classified by the hour-of-day the shift started. [zone] hanya untuk unit
 * test — call site produksi memakai default zona perangkat, perilaku tidak berubah. */
fun shiftNumberForEpochMin(epochMin: Long, zone: TimeZone = TimeZone.getDefault()): Int {
    val hour = Calendar.getInstance(zone).apply { timeInMillis = epochMin * 60000L }.get(Calendar.HOUR_OF_DAY)
    return when {
        hour in 6 until 14 -> 1
        hour in 14 until 22 -> 2
        else -> 3
    }
}

/** Epoch-minute the *current* shift period began (the most recent 06.00/14.00/22.00 boundary at
 * or before [epochMin]) — used to tell whether an already-recorded entry belongs to a shift that
 * hasn't been archived yet via "Selesai Shift" (see MainScreen's staleShiftBanner). */
fun currentShiftStartAbsMin(epochMin: Long, zone: TimeZone = TimeZone.getDefault()): Long {
    val cal = Calendar.getInstance(zone).apply { timeInMillis = epochMin * 60000L }
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val boundaryHour = if (hour in 6 until 22) (if (hour < 14) 6 else 14) else 22
    if (hour < 6) cal.add(Calendar.DAY_OF_YEAR, -1)
    cal.set(Calendar.HOUR_OF_DAY, boundaryHour)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis / 60000L
}

/** One day / half a day in minutes — used by [jamKeShiftAbs] to snap an ambiguous wall-clock
 * reading (which carries no date) onto whichever calendar day puts it within 12 hours of now.
 * Assumes fixed-offset days of exactly 24h; fine for WIB (no DST), not portable to DST zones. */
private const val DAY_MIN = 1440L
private const val HALF_DAY_MIN = 720L

/** [nowEpochMin]/[zone] hanya untuk unit test — "hari ini" diturunkan dari [nowEpochMin] di
 * [zone], sehingga test bisa memilih momen tetap; default-nya identik dengan perilaku lama. */
fun jamKeShiftAbs(
    jamMin: Int,
    nowEpochMin: Long = nowAbsMin(),
    zone: TimeZone = TimeZone.getDefault(),
): Long {
    val startOfDay = Calendar.getInstance(zone).apply {
        timeInMillis = nowEpochMin * 60000L
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val epochMinToday = startOfDay / 60000L + jamMin
    val diff = epochMinToday - nowEpochMin
    return when {
        diff < -HALF_DAY_MIN -> epochMinToday + DAY_MIN
        diff > HALF_DAY_MIN -> epochMinToday - DAY_MIN
        else -> epochMinToday
    }
}

fun parseJam(str: String): Int? {
    val s = str.trim().replace(',', '.')
    val m1 = Regex("""^(\d{1,2})[.:](\d{2})$""").matchEntire(s)
    if (m1 != null) {
        val h = m1.groupValues[1].toInt()
        val mi = m1.groupValues[2].toInt()
        if (h in 0..23 && mi in 0..59) return h * 60 + mi
        return null
    }
    val m2 = Regex("""^(\d{2})(\d{2})$""").matchEntire(s)
    if (m2 != null) {
        val h = m2.groupValues[1].toInt()
        val mi = m2.groupValues[2].toInt()
        if (h in 0..23 && mi in 0..59) return h * 60 + mi
        return null
    }
    return null
}

fun parseDurasi(str: String): Int? {
    val s = str.trim().replace(',', '.')
    val m1 = Regex("""^(\d{1,2})\.(\d{2})$""").matchEntire(s)
    if (m1 != null) {
        val h = m1.groupValues[1].toInt()
        val mi = m1.groupValues[2].toInt()
        if (mi in 0..59) return h * 60 + mi
        return null
    }
    val m2 = Regex("""^(\d{1,4})m(?:enit)?$""", RegexOption.IGNORE_CASE).matchEntire(s)
    if (m2 != null) return m2.groupValues[1].toInt()
    val m3 = Regex("""^(\d{1,4})$""").matchEntire(s)
    if (m3 != null) return m3.groupValues[1].toInt()
    return null
}

fun standarisasiKeterangan(raw: String): String {
    val t = raw.trim().lowercase()
    return when {
        t == "hb" -> "HB"
        t in listOf("lp", "p.lp", "p. lp", "p lp") -> "P.LP"
        t in listOf("sn", "p.sn", "p. sn", "p sn", "snarling") -> "P.SN"
        t in listOf("oh", "p.oh", "p. oh", "p oh", "overhaul") -> "P.OH"
        t in listOf("el", "p.el", "p. el", "p el", "elektrik") -> "P.EL"
        t in listOf("sel", "selvedge", "p.sel", "p. sel", "p sel") -> "P.Sel"
        else -> raw.trim()
    }
}
