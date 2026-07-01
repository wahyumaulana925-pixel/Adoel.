package com.jekael.adoel.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.jekael.adoel.MainActivity
import com.jekael.adoel.R

object NotificationHelper {
    const val CHANNEL_ID = "doff_alerts"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Doff Alerts",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi waktu doff mesin"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun scheduleNotif(context: Context, mcNo: String, estAbsMin: Long) {
        val alarmTime = estAbsMin * 60000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("mcNo", mcNo)
        }
        val notifId = notifIdFor(mcNo)
        val pi = PendingIntent.getBroadcast(
            context, notifId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val am = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
            am.set(AlarmManager.RTC_WAKEUP, alarmTime, pi)
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarmTime, pi)
        }
    }

    fun cancelNotif(context: Context, mcNo: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val notifId = notifIdFor(mcNo)
        val pi = PendingIntent.getBroadcast(
            context, notifId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        context.getSystemService(NotificationManager::class.java).cancel(notifId)
    }

    fun cancelAll(context: Context, mcNos: List<String>) {
        mcNos.forEach { cancelNotif(context, it) }
    }

    fun showNotification(context: Context, mcNo: String) {
        val notifId = notifIdFor(mcNo)
        val tapIntent = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Mc $mcNo — siap doff")
            .setContentText("Estimasi waktu telah tiba")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notifId, notif)
    }

    private fun notifIdFor(mcNo: String): Int = mcNo.toIntOrNull() ?: mcNo.hashCode()
}
