package com.jekael.adoel.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

/**
 * Matematika shift & waktu di Models.kt, diuji lewat seam zona/clock yang bisa disuntik — zona
 * dikunci ke WIB (UTC+7, tanpa DST) supaya hasilnya deterministik di mesin CI mana pun.
 */
class ShiftMathTest {

    private val wib = TimeZone.getTimeZone("Asia/Jakarta")

    /** Epoch-minute untuk tanggal/jam tertentu di WIB. */
    private fun epochMin(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(wib).apply {
            clear()
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis / 60000L

    // ---- shiftNumberForEpochMin: Shift 1 06-14, Shift 2 14-22, Shift 3 22-06 ----

    @Test
    fun shiftNumberBoundaries() {
        assertEquals(1, shiftNumberForEpochMin(epochMin(2026, 1, 15, 6, 0), wib))
        assertEquals(1, shiftNumberForEpochMin(epochMin(2026, 1, 15, 13, 59), wib))
        assertEquals(2, shiftNumberForEpochMin(epochMin(2026, 1, 15, 14, 0), wib))
        assertEquals(2, shiftNumberForEpochMin(epochMin(2026, 1, 15, 21, 59), wib))
        assertEquals(3, shiftNumberForEpochMin(epochMin(2026, 1, 15, 22, 0), wib))
        assertEquals(3, shiftNumberForEpochMin(epochMin(2026, 1, 15, 23, 59), wib))
        assertEquals(3, shiftNumberForEpochMin(epochMin(2026, 1, 16, 0, 0), wib))
        assertEquals(3, shiftNumberForEpochMin(epochMin(2026, 1, 16, 5, 59), wib))
    }

    // ---- currentShiftStartAbsMin: batas 06.00/14.00/22.00 terdekat sebelum/pas epochMin ----

    @Test
    fun currentShiftStartWithinShift1() {
        assertEquals(
            epochMin(2026, 1, 15, 6, 0),
            currentShiftStartAbsMin(epochMin(2026, 1, 15, 10, 0), wib),
        )
    }

    @Test
    fun currentShiftStartExactlyAtBoundary() {
        assertEquals(
            epochMin(2026, 1, 15, 14, 0),
            currentShiftStartAbsMin(epochMin(2026, 1, 15, 14, 0), wib),
        )
    }

    @Test
    fun currentShiftStartLateEvening() {
        assertEquals(
            epochMin(2026, 1, 15, 22, 0),
            currentShiftStartAbsMin(epochMin(2026, 1, 15, 23, 10), wib),
        )
    }

    @Test
    fun currentShiftStartAfterMidnightBelongsToPreviousDay() {
        // Jam 02.00 masih bagian dari Shift 3 yang MULAI jam 22.00 kemarin — kasus lintas
        // tengah malam yang menjadi dasar banner "shift sebelumnya belum diarsipkan".
        assertEquals(
            epochMin(2026, 1, 15, 22, 0),
            currentShiftStartAbsMin(epochMin(2026, 1, 16, 2, 0), wib),
        )
    }

    // ---- jamKeShiftAbs: bacaan jam dinding tanpa tanggal → hari kalender terdekat dari now ----

    @Test
    fun jamKeShiftAbsSameDayWhenWithinTwelveHours() {
        val now = epochMin(2026, 1, 15, 12, 0)
        assertEquals(
            epochMin(2026, 1, 15, 14, 0),
            jamKeShiftAbs(14 * 60, now, wib),
        )
    }

    @Test
    fun jamKeShiftAbsWrapsToNextDayAcrossMidnight() {
        // Sekarang 23.30, counter menunjukkan 00.15 — itu 00.15 BESOK, bukan tadi pagi.
        val now = epochMin(2026, 1, 15, 23, 30)
        assertEquals(
            epochMin(2026, 1, 16, 0, 15),
            jamKeShiftAbs(15, now, wib),
        )
    }

    @Test
    fun jamKeShiftAbsWrapsToPreviousDayAcrossMidnight() {
        // Sekarang 00.30, counter menunjukkan 23.45 — itu 23.45 KEMARIN.
        val now = epochMin(2026, 1, 16, 0, 30)
        assertEquals(
            epochMin(2026, 1, 15, 23, 45),
            jamKeShiftAbs(23 * 60 + 45, now, wib),
        )
    }

    @Test
    fun jamKeShiftAbsExactlyTwelveHoursAheadStaysSameDay() {
        // diff == +720 persis tidak di-wrap (kondisi wrap adalah > 720) — kunci perilaku batas.
        val now = epochMin(2026, 1, 15, 0, 0)
        assertEquals(
            epochMin(2026, 1, 15, 12, 0),
            jamKeShiftAbs(12 * 60, now, wib),
        )
    }
}
