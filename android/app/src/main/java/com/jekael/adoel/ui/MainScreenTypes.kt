package com.jekael.adoel.ui

import androidx.compose.runtime.saveable.Saver
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

/** Custom Saver for rememberSaveable — ActiveOverlay's variants aren't natively Bundle-storable
 * (no Parcelable/Serializable), so this encodes each as a (tag, payload) pair. Restoring
 * EditAkt/QuickEditMesin across process death can point at data that no longer exists (the entry
 * was deleted, e.g. via a backup restore, while the app was gone) — MainScreen validates that
 * against the freshly-loaded state before trusting the restored overlay. */
internal val ActiveOverlaySaver = Saver<ActiveOverlay, List<Any?>>(
    save = { overlay ->
        when (overlay) {
            is ActiveOverlay.None -> listOf("None", null)
            is ActiveOverlay.Settings -> listOf("Settings", null)
            is ActiveOverlay.Statistik -> listOf("Statistik", null)
            is ActiveOverlay.EditAkt -> listOf("EditAkt", overlay.id)
            is ActiveOverlay.QuickEditMesin -> listOf("QuickEditMesin", overlay.mcNo)
        }
    },
    restore = { saved ->
        when (saved.getOrNull(0) as? String) {
            "Settings" -> ActiveOverlay.Settings
            "Statistik" -> ActiveOverlay.Statistik
            "EditAkt" -> (saved.getOrNull(1) as? Int)?.let { ActiveOverlay.EditAkt(it) } ?: ActiveOverlay.None
            "QuickEditMesin" -> (saved.getOrNull(1) as? String)?.let { ActiveOverlay.QuickEditMesin(it) } ?: ActiveOverlay.None
            else -> ActiveOverlay.None
        }
    },
)
