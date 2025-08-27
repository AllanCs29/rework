package com.example.routineapp.util

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.routineapp.R

private const val CH_ID = "routine_channel"

fun scheduleReminder(ctx: Context, title: String, timeHHmm: String) {
    ensureChannel(ctx)
    val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notif = NotificationCompat.Builder(ctx, CH_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Recordatorio")
        .setContentText(title + " @ " + timeHHmm)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .build()
    nm.notify((title + timeHHmm).hashCode(), notif)
}

private fun ensureChannel(ctx: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CH_ID, "Routine", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) { /* no-op */ }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) { /* no-op */ }
}