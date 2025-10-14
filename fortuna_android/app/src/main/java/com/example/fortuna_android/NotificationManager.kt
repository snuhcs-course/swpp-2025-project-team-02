package com.example.fortuna_android

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import java.util.*

class NotificationManager {

    companion object {
        private const val TAG = "NotificationManager"
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001

        fun requestNotificationPermission(activity: Activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity,
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        NOTIFICATION_PERMISSION_REQUEST_CODE
                    )
                }
            }
        }

        fun generateFCMToken(context: Context, onTokenReceived: (String?) -> Unit) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    onTokenReceived(null)
                    return@addOnCompleteListener
                }

                val token = task.result
                Log.d(TAG, "FCM Registration Token: $token")
                onTokenReceived(token)
            }
        }

        fun subscribeToTopic(topic: String) {
            FirebaseMessaging.getInstance().subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "Subscribed to topic: $topic"
                    if (!task.isSuccessful) {
                        msg = "Failed to subscribe to topic: $topic"
                    }
                    Log.d(TAG, msg)
                }
        }

        fun unsubscribeFromTopic(topic: String) {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "Unsubscribed from topic: $topic"
                    if (!task.isSuccessful) {
                        msg = "Failed to unsubscribe from topic: $topic"
                    }
                    Log.d(TAG, msg)
                }
        }

        // Android Internal Notification Logic
        fun scheduleNotification(
            context: Context,
            title: String,
            message: String,
            hour: Int,
            minute: Int,
            notificationId: Int = 0
        ) {
            val calendar = Calendar.getInstance().apply {
                val currentTime = Calendar.getInstance()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (before(currentTime)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("title", title)
                putExtra("message", message)
                putExtra("notificationId", notificationId)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )

            Log.d(TAG, "Scheduled notification for ${hour}:${minute} daily")
        }

        fun fortuneGeneratedNotification(
            context: Context,
            title: String,
            message: String,
            notificationId: Int = 0
        ) {
            val locale = Locale.getDefault()
            // 10:00PM
            val (hour, minute) = Pair(22, 0)

            Log.d(TAG, "Scheduling notification for locale: ${locale.country} at ${hour}:${minute}")
            scheduleNotification(context, title, message, hour, minute, notificationId)
        }

        fun cancelScheduledNotification(context: Context, notificationId: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Cancelled scheduled notification with ID: $notificationId")
        }
    }

    class NotificationReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val title = intent.getStringExtra("title") ?: "Fortuna"
            val message = intent.getStringExtra("message") ?: "Time for your daily fortune!"
            val notificationId = intent.getIntExtra("notificationId", 0)

            val notificationIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE
            )

            val channelId = "scheduled_notifications"
            val notificationBuilder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setShowWhen(true)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Scheduled Notifications",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications scheduled at specific times based on locale"
                    enableVibration(true)
                    enableLights(true)
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                notificationManager.createNotificationChannel(channel)
            }

            notificationManager.notify(notificationId, notificationBuilder.build())
            Log.d("NotificationReceiver", "Scheduled notification displayed: $title")
        }
    }
}