package com.example.routineapp.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.routineapp.R
import java.time.LocalTime

private const val CHANNEL_ID = "reminders"

fun ensureChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val ch = NotificationChannel(CHANNEL_ID, "Recordatorios", NotificationManager.IMPORTANCE_DEFAULT)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(ch)
    }
}

fun scheduleReminder(ctx: Context, title: String, time: String) {
    ensureChannel(ctx)
    val t = runCatching { LocalTime.parse(time) }.getOrNull() ?: return
    val now = java.time.ZonedDateTime.now()
    var target = now.withHour(t.hour).withMinute(t.minute).withSecond(0)
    if (target.isBefore(now)) target = target.plusDays(1)
    val triggerAt = target.toInstant().toEpochMilli()

    val i = Intent(ctx, ReminderReceiver::class.java).apply { putExtra("title", title) }
    val pi = PendingIntent.getBroadcast(ctx, title.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
}

class ReminderReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Recordatorio"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("RoutineApp")
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        nm.notify(title.hashCode(), notif)
    }
}

class BootReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { /* reprogram if needed */ }
}
