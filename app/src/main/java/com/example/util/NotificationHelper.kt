package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.time.LocalDateTime
import java.time.ZoneId

object NotificationHelper {

    private const val PREFS_NAME = "app_notification_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_NOTIF_ENABLED = "notifications_enabled"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    const val CHANNEL_ID_SOUND = "schedule_event_channel_sound"
    const val CHANNEL_ID_SILENT = "schedule_event_channel_silent"

    fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunch(context: Context, isFirst: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_FIRST_LAUNCH, isFirst).apply()
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_NOTIF_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_NOTIF_ENABLED, enabled).apply()
    }

    fun isSoundEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SOUND_ENABLED, true)
    }

    fun setSoundEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply()
    }

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            // 1. Channel with Sound & Vibration
            val soundChannel = NotificationChannel(
                CHANNEL_ID_SOUND,
                "Nhắc nhở sự kiện (Có âm thanh)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo nhắc nhở lịch trình sự kiện kèm âm thanh và rung"
                enableLights(true)
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            // 2. Channel Silent
            val silentChannel = NotificationChannel(
                CHANNEL_ID_SILENT,
                "Nhắc nhở sự kiện (Im lặng)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Thông báo nhắc nhở lịch trình sự kiện không có âm thanh"
                enableLights(true)
                enableVibration(false)
                setSound(null, null)
            }

            notificationManager.createNotificationChannel(soundChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        isTest: Boolean = false
    ) {
        if (!isNotificationsEnabled(context) && !isTest) return

        createNotificationChannels(context)

        val soundEnabled = isSoundEnabled(context)
        val channelId = if (soundEnabled) CHANNEL_ID_SOUND else CHANNEL_ID_SILENT

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (soundEnabled) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (soundEnabled) {
            builder.setSound(soundUri)
            builder.setVibrate(longArrayOf(0, 350, 200, 350))
            builder.setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
        } else {
            builder.setSilent(true)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(id, builder.build())

            // Play notification sound when sound is enabled (for both scheduled reminders and test notifications)
            if (soundEnabled) {
                try {
                    val ringtone = RingtoneManager.getRingtone(context.applicationContext, soundUri)
                    ringtone?.play()
                } catch (_: Exception) {}
            }
        } catch (e: SecurityException) {
            // Permission not granted
        }
    }

    fun scheduleEventReminder(
        context: Context,
        eventId: Int,
        title: String,
        note: String,
        startTime: LocalDateTime,
        offsetMinutes: Int
    ) {
        if (!isNotificationsEnabled(context)) return

        val reminderTime = startTime.minusMinutes(offsetMinutes.toLong())
        val epochMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()

        if (epochMillis <= nowMillis) return // Event/Reminder already in the past

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_EVENT_ID", eventId)
            putExtra("EXTRA_TITLE", "Nhắc nhở: $title")
            putExtra("EXTRA_MESSAGE", if (note.isNotBlank()) note else "Sự kiện diễn ra lúc ${startTime.toLocalTime()}")
            putExtra("EXTRA_SOUND_ENABLED", isSoundEnabled(context))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, epochMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    fun cancelEventReminder(context: Context, eventId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            eventId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
