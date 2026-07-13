package com.jekael.adoel.ui

import android.content.Context
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.jekael.adoel.data.Estimasi
import com.jekael.adoel.data.ProsesResult
import com.jekael.adoel.data.formatDeltaMin
import com.jekael.adoel.data.nowAbsMin
import com.jekael.adoel.notification.NotificationHelper
import com.jekael.adoel.viewmodel.DoffViewModel
import com.jekael.adoel.viewmodel.UIViewModel

/** Console/radar/doffing action handlers, hoisted out of MainScreen's composable body and held via
 * `remember { MainScreenHandlers(...) }` so this instance's identity is stable across recomposition
 * (letting callbacks passed to children stay stable too, instead of being redefined every time
 * MainScreen recomposes). Reads [doffVm]'s state fresh inside every call via `.state.value` — never
 * captures a snapshot at construction time — since the instance itself is not recreated when the
 * app state changes. */
internal class MainScreenHandlers(
    private val context: Context,
    private val doffVm: DoffViewModel,
    private val uiVm: UIViewModel,
    private val haptic: HapticFeedback,
    private val sendPulse: SendPulseState,
    private val errorFlash: ErrorFlashState,
    private val shiftFinished: ShiftFinishedState,
) {
    private fun flashError(msg: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        errorFlash.key++
        uiVm.showToast("⚠ $msg")
    }

    private fun submitEstimasi(cmd: String, onCleared: () -> Unit) {
        val result = doffVm.prosesBarisKondisiMesin(cmd, nowAbsMin())
        when (result) {
            is ProsesResult.Ok -> {
                sendPulse.key++
                uiVm.showToast(result.msg)
                onCleared()
                result.estAbs?.let { NotificationHelper.scheduleNotif(context, result.mcNo, it) }
            }
            is ProsesResult.Err -> flashError(result.msg)
        }
    }

    fun handleCommand(mode: Mode, input: String, onCleared: () -> Unit) {
        val cmd = input.trim().uppercase()
        if (cmd.isEmpty()) return

        when (mode) {
            Mode.ESTIMASI -> {
                val mcNo = cmd.substringBefore(' ')
                val existing = doffVm.state.value.estimasi[mcNo]
                val remaining = existing?.let { it.estAbsMin - nowAbsMin() }
                // Salah masuk mode ESTIMASI lalu ngetik cepat bisa nggak sadar menimpa timer yang
                // masih jalan — prosesBarisKondisiMesin tidak punya undo, jadi khusus estimasi yang
                // masih aktif & mepet (<10 menit lagi), minta konfirmasi dulu, bukan menimpa diam-diam.
                if (existing != null && remaining != null && remaining in 0 until 10) {
                    uiVm.showConfirm("Mc $mcNo sudah diestimasi ${formatDeltaMin(remaining)} lagi. Timpa dengan estimasi baru?") {
                        submitEstimasi(cmd, onCleared)
                    }
                } else {
                    submitEstimasi(cmd, onCleared)
                }
            }
            Mode.AKTUAL -> {
                val result = doffVm.prosesBarisUmum(cmd)
                when (result) {
                    is ProsesResult.Ok -> {
                        sendPulse.key++
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        NotificationHelper.cancelNotif(context, result.mcNo)
                        uiVm.showToast(result.msg, undo = {
                            result.undoFn?.invoke()
                            rescheduleEstimasi(result.prevEst)
                        })
                        onCleared()
                    }
                    is ProsesResult.Err -> flashError(result.msg)
                }
            }
        }
    }

    fun handleDoff(mcNo: String, keterangan: String? = null) {
        val cmd = if (keterangan != null) "$mcNo $keterangan" else mcNo
        val result = doffVm.prosesBarisUmum(cmd)
        when (result) {
            is ProsesResult.Ok -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                NotificationHelper.cancelNotif(context, result.mcNo)
                uiVm.showToast(result.msg, undo = {
                    result.undoFn?.invoke()
                    rescheduleEstimasi(result.prevEst)
                })
            }
            is ProsesResult.Err -> uiVm.showToast("⚠ ${result.msg}")
        }
    }

    fun handleHapusEst(mcNo: String) {
        uiVm.showConfirm("Hapus estimasi Mc $mcNo?") {
            val prevEst = doffVm.state.value.estimasi[mcNo]
            doffVm.hapusEstimasi(mcNo)
            NotificationHelper.cancelNotif(context, mcNo)
            uiVm.showToast("Mc $mcNo dihapus", undo = {
                if (prevEst != null) {
                    doffVm.restoreEstimasi(prevEst)
                    rescheduleEstimasi(prevEst)
                }
            })
        }
    }

    fun handleHapusAktual(id: Int, onCleared: () -> Unit) {
        val entry = doffVm.state.value.aktual.find { it.id == id } ?: return
        uiVm.showConfirm("Hapus riwayat Mc ${entry.mcNo}?") {
            doffVm.hapusAktualById(id)
            onCleared()
            uiVm.showToast("Mc ${entry.mcNo} dihapus", undo = {
                doffVm.restoreAktual(entry)
            })
        }
    }

    fun handleFinishShift() {
        val state = doffVm.state.value
        // finishShift() itself already no-ops on an empty console — mirror that here so an
        // accidental/duplicate tap doesn't show a confirm dialog and checkmark celebration for
        // archiving "0 doff & 0 estimasi".
        if (state.aktual.isEmpty() && state.estimasi.isEmpty()) {
            uiVm.showToast("Tidak ada yang perlu diarsipkan")
            return
        }
        uiVm.showConfirm("Akhiri shift? ${state.aktual.size} doff & ${state.estimasi.size} estimasi akan diarsipkan ke Riwayat, lalu konsol dikosongkan untuk shift baru.") {
            NotificationHelper.cancelAll(context, state.estimasi.keys.toList())
            doffVm.finishShift()
            shiftFinished.key++
        }
    }

    private fun rescheduleEstimasi(est: Estimasi?) {
        est?.let { NotificationHelper.scheduleNotif(context, it.mcNo, it.estAbsMin) }
    }
}
