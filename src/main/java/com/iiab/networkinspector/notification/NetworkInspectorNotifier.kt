package com.iiab.networkinspector.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.iiab.networkinspector.domain.NetworkCallDetails
import com.iiab.networkinspector.ui.NetworkInspectorActivity

private const val CHANNEL_ID = "network_inspector_debug"
private const val NOTIFICATION_ID = 10_001

class NetworkInspectorNotifier(
    private val context: Context
) {

    private val notificationManager: NotificationManagerCompat =
        NotificationManagerCompat.from(context)

    init {
        ensureChannel()
    }

    fun notifyNewCall(details: NetworkCallDetails) {
        val intent = Intent(context, NetworkInspectorActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Network inspector"
        val content = "${details.method} ${details.path} (${details.statusCode ?: "-"} )"

        val appIcon = context.applicationInfo?.icon ?: android.R.drawable.stat_notify_sync

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(appIcon)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        if (notificationManager.areNotificationsEnabled()) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel() {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Network inspector (debug)",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recent HTTP calls for debug builds."
                enableLights(false)
                enableVibration(false)
                lightColor = Color.BLUE
            }
            manager.createNotificationChannel(channel)
        }
    }
}

