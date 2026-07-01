package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jekael.adoel.data.DoffRepository
import com.jekael.adoel.data.nowAbsMin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = DoffRepository(context)
        CoroutineScope(Dispatchers.IO).launch {
            val state = repo.load()
            val now = nowAbsMin()
            state.estimasi.values
                .filter { it.estAbsMin > now }
                .forEach { NotificationHelper.scheduleNotif(context, it.mcNo, it.estAbsMin) }
        }
    }
}
