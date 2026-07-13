package com.jekael.adoel.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val mcNo = intent.getStringExtra("mcNo") ?: return
        val isReminder = intent.getBooleanExtra("isReminder", false)
        try {
            NotificationHelper.showNotification(context, mcNo, isReminder)
        } catch (e: Exception) {
            // Notifikasi gagal tampil tidak boleh merobohkan proses — cukup dicatat.
            Log.e("AlarmReceiver", "Gagal menampilkan notifikasi doff Mc $mcNo", e)
        }
    }
}
