package com.jekael.adoel.ui

import com.jekael.adoel.data.Estimasi

internal enum class Mode { ESTIMASI, AKTUAL }

/** Gaps at or above this many minutes between two upcoming doffs are worth flagging as a
 * break window — short enough to still be actionable, long enough to actually leave the floor. */
internal const val BREAK_GAP_THRESHOLD_MIN = 30L

internal sealed class MenungguRow {
    data class CardRow(val est: Estimasi) : MenungguRow()
    data class GapRow(val afterMcNo: String, val nextMcNo: String, val gapMin: Long, val nextAbsMin: Long) : MenungguRow()
}

/** The overlay panels/sheets MainScreen can show are mutually exclusive in practice (each opens
 * over a full-screen backdrop that blocks interaction with what triggers the others), so modeling
 * them as one sealed type instead of 4 independent flags makes that invariant structural. */
internal sealed interface ActiveOverlay {
    data object None : ActiveOverlay
    data object Settings : ActiveOverlay
    data object Statistik : ActiveOverlay
    data class EditAkt(val id: Int) : ActiveOverlay
    data class QuickEditMesin(val mcNo: String) : ActiveOverlay
}
