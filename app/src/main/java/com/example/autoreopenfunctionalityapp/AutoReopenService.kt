package com.example.autoreopenfunctionalityapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import java.util.*

class AutoReopenService : Service() {
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (Intent.ACTION_SCREEN_OFF == intent?.action) {
                val currentTime = System.currentTimeMillis()
                val editor: SharedPreferences.Editor = getSharedPreferences("AutoReopenPrefs", MODE_PRIVATE).edit()
                editor.putLong("lastClosedTime", currentTime)
                editor.apply()
                scheduleAutoReopen(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(broadcastReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadcastReceiver)
    }

    private fun scheduleAutoReopen(context: Context?) {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun run() {
                // Retrieve the last closed time
                val prefs = context?.getSharedPreferences("AutoReopenPrefs", MODE_PRIVATE)
                val lastClosedTime = prefs?.getLong("lastClosedTime", 0)
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - (lastClosedTime ?: 0)
                if (elapsedTime >= 2000) {
                    showNotification(context)
                }
            }
        }, 2000) // 1 minute delay
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showNotification(context: Context?) {
        val channelId = "auto_reopen_channel"
        val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(channelId, "Auto Reopen Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        val intent = context?.packageManager?.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("App Reopened")
            .setContentText("Your app has been automatically reopened.")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(0, notification)
    }



    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

