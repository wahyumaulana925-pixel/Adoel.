package com.jekael.adoel.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for the pure parsing/formatting helpers in Models.kt — the logic most prone to
 * silent regressions (e.g. the historical "+1.25" formatting bug). Time-dependent helpers
 * (jamKeShiftAbs, nowTimeStr, absMinToTimeStr) are intentionally excluded to keep the suite
 * deterministic.
 */
class ModelsTest {

    // ---- parseDurasi: "jam.menit", plain minutes, "Nm/Nmenit" ----

    @Test
    fun parseDurasi_jamMenit() {
        assertEquals(85, parseDurasi("1.25"))   // 1 jam 25 menit
        assertEquals(65, parseDurasi("1.05"))
        assertEquals(0, parseDurasi("0.00"))
    }

    @Test
    fun parseDurasi_plainMinutes() {
        assertEquals(45, parseDurasi("45"))
        assertEquals(90, parseDurasi("90"))
        assertEquals(1234, parseDurasi("1234"))
    }

    @Test
    fun parseDurasi_minuteSuffix() {
        assertEquals(30, parseDurasi("30m"))
        assertEquals(30, parseDurasi("30menit"))
        assertEquals(30, parseDurasi("30MENIT"))
    }

    @Test
    fun parseDurasi_commaAsDecimal() {
        assertEquals(85, parseDurasi("1,25"))
    }

    @Test
    fun parseDurasi_invalid() {
        assertNull(parseDurasi("1.60"))  // menit 60 di luar 0..59
        assertNull(parseDurasi("abc"))
        assertNull(parseDurasi(""))
    }

    // ---- parseJam: "HH.MM", "HH:MM", "HHMM" ----

    @Test
    fun parseJam_withSeparator() {
        assertEquals(450, parseJam("07.30"))
        assertEquals(450, parseJam("7:30"))
        assertEquals(1439, parseJam("23.59"))
        assertEquals(0, parseJam("00.00"))
    }

    @Test
    fun parseJam_fourDigit() {
        assertEquals(450, parseJam("0730"))
        assertEquals(1439, parseJam("2359"))
    }

    @Test
    fun parseJam_invalid() {
        assertNull(parseJam("24.00"))  // jam 24 tidak valid
        assertNull(parseJam("07.60"))  // menit 60 tidak valid
        assertNull(parseJam("730"))    // 3 digit, bukan HHMM
        assertNull(parseJam("abc"))
    }

    // ---- formatDeltaMin: menit tersisa → "Nm" / "NjMm", overdue diberi tanda minus ----

    @Test
    fun formatDeltaMin_pending() {
        assertEquals("0m", formatDeltaMin(0))
        assertEquals("5m", formatDeltaMin(5))
        assertEquals("1j0m", formatDeltaMin(60))
        assertEquals("1j25m", formatDeltaMin(85))
        assertEquals("2j5m", formatDeltaMin(125))
    }

    @Test
    fun formatDeltaMin_overdue() {
        // Tanda minus di sini adalah U+2212 (−), bukan hyphen ASCII.
        assertEquals("−5m", formatDeltaMin(-5))
        assertEquals("−1j25m", formatDeltaMin(-85))
    }

    // ---- formatYard: bulat tampil tanpa ".0", pecahan dipertahankan ----

    @Test
    fun formatYard_whole() {
        assertEquals("303", formatYard(303.0))
        assertEquals("165", formatYard(165.0))
    }

    @Test
    fun formatYard_fractional() {
        assertEquals("1.25", formatYard(1.25))
        assertEquals("0.158", formatYard(0.158))
    }

    // ---- standarisasiKeterangan: alias singkat → bentuk baku ----

    @Test
    fun standarisasiKeterangan_aliases() {
        assertEquals("HB", standarisasiKeterangan("hb"))
        assertEquals("HB", standarisasiKeterangan("HB"))
        assertEquals("P.LP", standarisasiKeterangan("lp"))
        assertEquals("P.SN", standarisasiKeterangan("p.sn"))
        assertEquals("P.SN", standarisasiKeterangan("snarling"))
        assertEquals("P.OH", standarisasiKeterangan("overhaul"))
        assertEquals("P.EL", standarisasiKeterangan("elektrik"))
        assertEquals("P.Sel", standarisasiKeterangan("sel"))
    }

    @Test
    fun standarisasiKeterangan_passthroughAndTrim() {
        assertEquals("teks bebas", standarisasiKeterangan("teks bebas"))
        assertEquals("catatan", standarisasiKeterangan("  catatan  "))
    }
}
