package com.jekael.adoel.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.jekael.adoel.MainActivity
import com.jekael.adoel.R
import com.jekael.adoel.data.formatYard

// Brand accent (Cyan600 #0891B2) — tints the small icon & app name in the notification shade.
private const val BRAND_COLOR = 0xFF0891B2.toInt()

object NotificationHelper {
    const val CHANNEL_ID = "doff_alerts"

    // A heads-up alert fires this many minutes before the estimated doff time, in addition
    // to the alert at the estimate itself, so the operator has time to walk over.
    private const val REMINDER_LEAD_MIN = 5L
    private const val REMINDER_ID_OFFSET = 100_000

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
        val now = System.currentTimeMillis() / 60000L
        val reminderAt = estAbsMin - REMINDER_LEAD_MIN
        if (reminderAt > now) {
            scheduleAt(context, mcNo, reminderAt, isReminder = true)
        }
        // Only schedule the "siap doff" alarm if the estimate is still in the future.
        // For an already-past estimate, AlarmManager would fire it instantly — but the operator
        // is looking at the screen when they enter it and the RadarCard already shows it as
        // overdue, so an immediate notification would just be redundant noise.
        if (estAbsMin > now) {
            scheduleAt(context, mcNo, estAbsMin, isReminder = false)
        }
    }

    private fun scheduleAt(context: Context, mcNo: String, atAbsMin: Long, isReminder: Boolean) {
        val alarmTime = atAbsMin * 60000L
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("mcNo", mcNo)
            putExtra("isReminder", isReminder)
        }
        val notifId = notifIdFor(mcNo, isReminder)
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
        cancelOne(context, mcNo, isReminder = true)
        cancelOne(context, mcNo, isReminder = false)
    }

    private fun cancelOne(context: Context, mcNo: String, isReminder: Boolean) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val notifId = notifIdFor(mcNo, isReminder)
        val pi = PendingIntent.getBroadcast(
            context, notifId, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (pi != null) context.getSystemService(AlarmManager::class.java).cancel(pi)
        context.getSystemService(NotificationManager::class.java).cancel(notifId)
    }

    fun cancelAll(context: Context, mcNos: List<String>) {
        mcNos.forEach { cancelNotif(context, it) }
    }

    fun showNotification(context: Context, mcNo: String, isReminder: Boolean, corak: String? = null, targetYard: Double? = null) {
        val notifId = notifIdFor(mcNo, isReminder)
        val label = buildString {
            append("Mc $mcNo")
            if (!corak.isNullOrBlank() && corak != "-") append(" · $corak")
            if (targetYard != null) append(" · ${formatYard(targetYard)}y")
        }
        val tapIntent = PendingIntent.getActivity(
            context, notifId,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val doffIntent = PendingIntent.getBroadcast(
            context, notifId,
            Intent(context, DoffActionReceiver::class.java).apply {
                putExtra("mcNo", mcNo)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Two icon elements, same as every other app's notifications (WhatsApp, Chrome, etc.):
        // the small icon (status bar, forced monochrome by Android) and the large icon (shown
        // prominently in the notification shade, full color — the actual launcher "A." bitmap).
        val largeIcon = runCatching {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)?.toBitmap(width = 128, height = 128)
        }.getOrNull()

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(BRAND_COLOR)
            .apply { if (largeIcon != null) setLargeIcon(largeIcon) }
            .setContentTitle(if (isReminder) "$label — $REMINDER_LEAD_MIN menit lagi" else "$label — siap doff")
            .setContentText(if (isReminder) "Bersiap, estimasi hampir tiba" else "Estimasi waktu telah tiba")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(tapIntent)
            .addAction(0, "Doff", doffIntent)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(notifId, notif)
    }

    private fun notifIdFor(mcNo: String, isReminder: Boolean = false): Int {
        val base = mcNo.toIntOrNull() ?: mcNo.hashCode()
        return if (isReminder) base + REMINDER_ID_OFFSET else base
    }
}
