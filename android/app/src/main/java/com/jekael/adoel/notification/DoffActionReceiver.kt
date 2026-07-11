package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jekael.adoel.data.DoffRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoffActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mcNo = intent.getStringExtra("mcNo") ?: return
        NotificationHelper.cancelNotif(context, mcNo)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DoffRepository.getInstance(context).quickDoff(mcNo)
            } catch (e: Exception) {
                // Operator menekan "Doff" dari notifikasi dan mengira tercatat — kalau tulisnya
                // gagal, minimal harus ada jejaknya, bukan crash proses senyap.
                Log.e("DoffActionReceiver", "Gagal mencatat quick doff Mc $mcNo", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
