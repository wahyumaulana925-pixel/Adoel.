package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.jekael.adoel.data.DoffRepository
import com.jekael.adoel.widget.AdoelWidget
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
                DoffRepository(context).quickDoff(mcNo)
                AdoelWidget().updateAll(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
