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
import com.example.data.EventRepository
import com.example.widget.WidgetUpdateHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object NotificationHelper {

    private const val PREFS_NAME = "app_notification_prefs"
    private const val KEY_FIRST_LAUNCH = "is_first_launch"
    private const val KEY_NOTIF_ENABLED = "notifications_enabled"
    private const val KEY_SOUND_ENABLED = "sound_enabled"

    const val CHANNEL_ID_SOUND = "schedule_event_channel_sound"
    const val CHANNEL_ID_SILENT = "schedule_event_channel_silent"
    /** Kênh sự kiện quan trọng: luôn kêu/rung, kể cả khi tắt chuông trong Cài đặt. */
    const val CHANNEL_ID_IMPORTANT = "schedule_event_channel_important"

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

            // 3. Channel for important events: always loud, shown on lock screen
            val importantChannel = NotificationChannel(
                CHANNEL_ID_IMPORTANT,
                "Sự kiện quan trọng",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Sự kiện đánh dấu quan trọng: nổi lên toàn màn hình, luôn có âm thanh và rung"
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannel(soundChannel)
            notificationManager.createNotificationChannel(importantChannel)
            notificationManager.createNotificationChannel(silentChannel)
        }
    }

    fun showNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        isTest: Boolean = false,
        isImportant: Boolean = false,
        eventId: String? = null,
        startDate: String? = null,
        startTime: String? = null
    ) {
        if (!isNotificationsEnabled(context) && !isTest) return

        createNotificationChannels(context)

        if (isImportant) {
            showImportantNotification(context, id, title, message, eventId, startDate, startTime)
            return
        }

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

    /** Android 14+ có thể chặn full-screen intent cho tới khi người dùng cấp quyền trong Cài đặt. */
    fun canUseFullScreenIntent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return true
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.canUseFullScreenIntent()
    }

    fun openFullScreenIntentSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return
        try {
            context.startActivity(
                Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {}
    }

    private fun showImportantNotification(
        context: Context,
        id: Int,
        title: String,
        message: String,
        eventId: String?,
        startDate: String?,
        startTime: String?
    ) {
        val popupIntent = Intent(context, ImportantEventActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ImportantEventActivity.EXTRA_NOTIF_ID, id)
            putExtra(ImportantEventActivity.EXTRA_EVENT_ID, eventId)
            putExtra(ImportantEventActivity.EXTRA_TITLE, title)
            putExtra(ImportantEventActivity.EXTRA_MESSAGE, message)
            putExtra(ImportantEventActivity.EXTRA_DATE, startDate)
            putExtra(ImportantEventActivity.EXTRA_TIME, startTime)
        }
        val popupPending = PendingIntent.getActivity(
            context, id, popupIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_IMPORTANT)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(popupPending)
            .setAutoCancel(true)
            // Lock screen/off: bật màn hình đầy đủ. Đang dùng máy: Android chỉ hiện banner nổi.
            .setFullScreenIntent(popupPending, true)

        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
            // Permission POST_NOTIFICATIONS not granted
        }
    }

    fun scheduleEventReminder(
        context: Context,
        eventId: String,
        title: String,
        note: String,
        startTime: LocalDateTime,
        offsetMinutes: Int,
        isImportant: Boolean = false
    ) {
        if (!isNotificationsEnabled(context)) return

        val reminderTime = startTime.minusMinutes(offsetMinutes.toLong())
        val epochMillis = reminderTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()

        if (epochMillis <= nowMillis) return // Event/Reminder already in the past

        val notifIntId = (eventId.hashCode() and 0x7FFFFFFF)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("EXTRA_EVENT_ID", eventId)
            putExtra("EXTRA_NOTIF_INT_ID", notifIntId)
            putExtra("EXTRA_TITLE", "Nhắc nhở: $title")
            putExtra("EXTRA_MESSAGE", if (note.isNotBlank()) note else "Sự kiện diễn ra lúc ${startTime.toLocalTime()}")
            putExtra("EXTRA_SOUND_ENABLED", isSoundEnabled(context))
            putExtra("EXTRA_IS_IMPORTANT", isImportant)
            putExtra("EXTRA_START_DATE", startTime.toLocalDate().toString())
            putExtra("EXTRA_START_TIME", startTime.toLocalTime().toString().take(5))
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifIntId,
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

    fun cancelEventReminder(context: Context, eventId: String) {
        val notifIntId = (eventId.hashCode() and 0x7FFFFFFF)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notifIntId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Reschedules all alarms for active events/reminders from Room, cancels alarms for soft-deleted events,
     * and refreshes all 3 widgets. Called after event edit/delete and once upon app startup after migration.
     */
    suspend fun rescheduleAllAlarmsAndRefreshWidgets(
        context: Context,
        repository: EventRepository
    ) = withContext(Dispatchers.IO) {
        // 1. Cancel alarms for all soft-deleted events
        val deletedEventIds = try {
            repository.getAllSoftDeletedEventIds()
        } catch (e: Exception) {
            emptyList()
        }
        for (deletedId in deletedEventIds) {
            cancelEventReminder(context, deletedId)
        }

        // 2. Schedule reminders for all active events that have reminders enabled and are not completed
        val activeEvents = try {
            repository.getAllActiveEventsWithReminders()
        } catch (e: Exception) {
            emptyList()
        }
        for (event in activeEvents) {
            val reminders = try {
                repository.getRemindersForEvent(event.id)
            } catch (e: Exception) {
                emptyList()
            }
            val offsetMinutes = reminders.firstOrNull()?.remindBeforeMinutes ?: 15
            val startTime: LocalDateTime? = try {
                val date = LocalDate.parse(event.startDate)
                val time = event.startTime?.let { LocalTime.parse(it) } ?: LocalTime.of(8, 0)
                date.atTime(time)
            } catch (e: Exception) {
                null
            }

            if (startTime != null) {
                scheduleEventReminder(
                    context = context,
                    eventId = event.id,
                    title = event.title,
                    note = event.description ?: "",
                    startTime = startTime,
                    offsetMinutes = offsetMinutes,
                    isImportant = event.isImportant
                )
            }
        }

        // 3. Refresh all 3 widgets (2x2, 4x2, 2x1)
        try {
            WidgetUpdateHelper.updateAllWidgets(context)
        } catch (_: Exception) {}
    }
}
