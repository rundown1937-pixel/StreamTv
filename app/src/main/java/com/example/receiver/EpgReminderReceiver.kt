package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class EpgReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "epg_program_reminders"
        const val EXTRA_PROGRAM_ID = "extra_program_id"
        const val EXTRA_PROGRAM_TITLE = "extra_program_title"
        const val EXTRA_CHANNEL_ID = "extra_channel_id"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
        const val EXTRA_START_TIME = "extra_start_time"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val programId = intent.getStringExtra(EXTRA_PROGRAM_ID) ?: return
        val programTitle = intent.getStringExtra(EXTRA_PROGRAM_TITLE) ?: "Scheduled Program"
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: ""
        val channelName = intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "Live TV"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create high-importance notification channel on Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EPG Program Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts 5 minutes before scheduled TV programs start"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tap notification to open app and play channel directly
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("auto_play_channel_id", channelId)
            putExtra("notification_program_title", programTitle)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            programId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Starts in 5 Mins: $programTitle")
            .setContentText("Coming up on $channelName. Tap to tune in live!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("'$programTitle' starts in 5 minutes on $channelName. Tap to open StreamPulse TV in direct landscape mode.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(programId.hashCode(), notification)
    }
}
