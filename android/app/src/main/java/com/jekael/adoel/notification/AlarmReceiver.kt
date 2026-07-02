package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mcNo = intent.getStringExtra("mcNo") ?: return
        val isReminder = intent.getBooleanExtra("isReminder", false)
        NotificationHelper.showNotification(context, mcNo, isReminder)
    }
}
