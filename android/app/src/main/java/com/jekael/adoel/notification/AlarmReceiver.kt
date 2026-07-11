package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.jekael.adoel.data.DoffRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mcNo = intent.getStringExtra("mcNo") ?: return
        val isReminder = intent.getBooleanExtra("isReminder", false)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = DoffRepository.getInstance(context).load()
                val mesin = state.db[mcNo]
                val est = state.estimasi[mcNo]
                val corak = est?.corakOverride ?: mesin?.corak
                val targetYard = est?.yardOverride ?: mesin?.targetYard
                NotificationHelper.showNotification(context, mcNo, isReminder, corak, targetYard)
            } catch (e: Exception) {
                // Tanpa catch, exception dari coroutine ini merobohkan proses dan notifikasi
                // doff hilang tanpa jejak — kegagalan alarm harus tercatat, bukan jadi crash.
                Log.e("AlarmReceiver", "Gagal menampilkan notifikasi doff Mc $mcNo", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
