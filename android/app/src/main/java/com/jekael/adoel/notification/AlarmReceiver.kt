package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
                val state = DoffRepository(context).load()
                val mesin = state.db[mcNo]
                val est = state.estimasi[mcNo]
                val corak = est?.corakOverride ?: mesin?.corak
                val targetYard = est?.yardOverride ?: mesin?.targetYard
                NotificationHelper.showNotification(context, mcNo, isReminder, corak, targetYard)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
