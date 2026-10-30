package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.util.KarateChopDetector

class KarateChopService : Service() {

    private var detector: KarateChopDetector? = null

    companion object {
        private const val TAG = "KarateChopService"
        private const val CHANNEL_ID = "karate_chop_gesture_channel"
        private const val NOTIFICATION_ID = 20261

        const val ACTION_START = "com.example.service.START_KARATE_CHOP"
        const val ACTION_STOP = "com.example.service.STOP_KARATE_CHOP"

        fun startService(context: Context) {
            val intent = Intent(context, KarateChopService::class.java).apply {
                action = ACTION_START
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start KarateChopService: ${e.message}")
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KarateChopService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to stop KarateChopService: ${e.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            detector?.stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences("stream_pulse_settings", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("karate_chop_enabled", true)
        if (!isEnabled) {
            detector?.stopListening()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        val sensitivityStr = prefs.getString("karate_chop_sensitivity", "NORMAL") ?: "NORMAL"
        val sensitivity = try {
            KarateChopDetector.Sensitivity.valueOf(sensitivityStr)
        } catch (e: Exception) {
            KarateChopDetector.Sensitivity.NORMAL
        }

        if (detector == null) {
            detector = KarateChopDetector(
                context = this,
                sensitivity = sensitivity,
                onDoubleChopDetected = {
                    handleDoubleChopAction()
                }
            )
        }
        detector?.startListening()

        return START_STICKY
    }

    private fun handleDoubleChopAction() {
        Log.d(TAG, "Karate Chop Action triggered from background service! Opening app...")
        val openIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("karate_chop_launched", true)
        }
        startActivity(openIntent)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Karate Chop Quick Gesture",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors device accelerometer for double karate chop motion to quickly open StreamPulse TV"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🥋 Karate Chop Quick Launch Active")
            .setContentText("Do 2 quick chops with your phone anytime to open StreamPulse TV")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        detector?.stopListening()
        detector = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
