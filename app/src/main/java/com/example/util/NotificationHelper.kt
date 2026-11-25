package com.example.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.service.TimerActionReceiver
import java.util.Locale

object NotificationHelper {

    const val CHANNEL_ID = "baslayabilirsin_timer_channel"
    const val DAILY_CHANNEL_ID = "baslayalim_daily_reminders"
    const val NOTIFICATION_ID = 1001
    const val MOTIVATION_NOTIFICATION_ID = 1002
    const val AGENDA_REMINDER_NOTIFICATION_ID = 1003

    const val ACTION_TIMER_PAUSE = "com.example.ACTION_TIMER_PAUSE"
    const val ACTION_TIMER_RESUME = "com.example.ACTION_TIMER_RESUME"
    const val ACTION_TIMER_DISMISS = "com.example.ACTION_TIMER_DISMISS"
    const val ACTION_DAILY_MOTIVATION = "com.example.ACTION_DAILY_MOTIVATION"
    const val ACTION_DAILY_AGENDA_REMINDER = "com.example.ACTION_DAILY_AGENDA_REMINDER"
    const val EXTRA_TIMER_MODE = "extra_timer_mode"
    const val EXTRA_OPEN_AGENDA = "extra_open_agenda"
    const val EXTRA_OPEN_STOPWATCH = "extra_open_stopwatch"
    const val EXTRA_OPEN_STOPWATCH_SAVE_DIALOG = "extra_open_stopwatch_save_dialog"

    fun createNotificationChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val isTr = Locale.getDefault().language.equals("tr", ignoreCase = true)
                val channelName = if (isTr) "Başlayalım Sayaç Bildirimi" else "Başlayalım Timer Notification"
                val channelDesc = if (isTr) {
                    "Aktif Pomodoro, Sayaç ve Kronometre sürelerini duraklatma kontrolü ile gösterir."
                } else {
                    "Shows active Pomodoro, Timer, and Stopwatch progress with pause controls."
                }
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    channelName,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = channelDesc
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    setSound(null, null)
                    enableVibration(false)
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error creating channel: ${t.message}")
        }
    }

    fun buildTimerNotification(
        context: Context,
        title: String,
        content: String,
        mode: String = "NONE",
        isRunning: Boolean = false,
        isPaused: Boolean = false
    ): Notification {
        createNotificationChannel(context)
        val cleanTitle = removeEmojis(title)
        val cleanContent = removeEmojis(content)
        val isTr = Locale.getDefault().language.equals("tr", ignoreCase = true)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(cleanTitle)
            .setContentText(cleanContent)
            .setContentIntent(contentPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(isRunning)
            .setAutoCancel(!isRunning && !isPaused)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)

        if (isRunning && mode != "NONE") {
            val pauseIntent = Intent(context, TimerActionReceiver::class.java).apply {
                action = ACTION_TIMER_PAUSE
                putExtra(EXTRA_TIMER_MODE, mode)
            }
            val pausePi = PendingIntent.getBroadcast(
                context,
                201,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                if (isTr) "Duraklat" else "Pause",
                pausePi
            )
        } else if (isPaused && mode != "NONE") {
            val resumeIntent = Intent(context, TimerActionReceiver::class.java).apply {
                action = ACTION_TIMER_RESUME
                putExtra(EXTRA_TIMER_MODE, mode)
            }
            val resumePi = PendingIntent.getBroadcast(
                context,
                202,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                if (isTr) "Devam" else "Resume",
                resumePi
            )

            val dismissIntent = Intent(context, TimerActionReceiver::class.java).apply {
                action = ACTION_TIMER_DISMISS
                putExtra(EXTRA_TIMER_MODE, mode)
            }
            val dismissPi = PendingIntent.getBroadcast(
                context,
                203,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                if (isTr) "Kapat" else "Dismiss",
                dismissPi
            )
        }

        return builder.build()
    }

    fun showTimerNotification(
        context: Context,
        title: String,
        content: String,
        mode: String = "NONE",
        isRunning: Boolean = false,
        isPaused: Boolean = false
    ) {
        try {
            val notification = buildTimerNotification(context, title, content, mode, isRunning, isPaused)
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error showing timer notification: ${t.message}")
        }
    }

    fun triggerVibrationWarning(context: Context) {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
            }
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        android.os.VibrationEffect.createWaveform(
                            longArrayOf(0, 400, 200, 400),
                            -1
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(longArrayOf(0, 400, 200, 400), -1)
                }
            }
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error vibrating: ${t.message}")
        }
    }

    private fun removeEmojis(str: String): String {
        return str.replace(Regex("[\\p{So}\\p{Cn}\\u200D]"), "").trim()
    }

    fun cancelTimerNotification(context: Context) {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancel(NOTIFICATION_ID)
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error canceling notification: ${t.message}")
        }
    }

    fun createDailyReminderChannel(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val isTr = Locale.getDefault().language.equals("tr", ignoreCase = true)
                val name = if (isTr) "Günlük Motivasyon ve Ajanda Hatırlatıcısı" else "Daily Motivation & Agenda Reminders"
                val desc = if (isTr) {
                    "Her gün saat 06:00'da motivasyon sözü ve 22:45'te ajanda hatırlatması gönderir."
                } else {
                    "Sends daily motivational quote at 06:00 and agenda reminder at 22:45."
                }
                val channel = NotificationChannel(
                    DAILY_CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = desc
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                }
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
            }
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error creating daily channel: ${t.message}")
        }
    }

    fun showDailyReminderNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        openAgenda: Boolean
    ) {
        try {
            createDailyReminderChannel(context)
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (openAgenda) {
                    putExtra(EXTRA_OPEN_AGENDA, true)
                }
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val builder = NotificationCompat.Builder(context, DAILY_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(removeEmojis(title))
                .setContentText(removeEmojis(content))
                .setStyle(NotificationCompat.BigTextStyle().bigText(removeEmojis(content)))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.notify(notificationId, builder.build())
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error showing daily reminder: ${t.message}")
        }
    }

    fun computeNextDailyTriggerMillis(
        hourOfDay: Int,
        minute: Int,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val cal = java.util.Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
        }
        return cal.timeInMillis
    }

    fun scheduleDailyReminders(context: Context) {
        try {
            val appCtx = context.applicationContext
            createDailyReminderChannel(appCtx)
            val alarmManager = appCtx.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager ?: return
            val now = System.currentTimeMillis()

            // 1. Morning 06:00 Motivation Quote
            val morningTrigger = computeNextDailyTriggerMillis(6, 0, now)
            val morningIntent = Intent(appCtx, com.example.service.DailyReminderReceiver::class.java).apply {
                action = ACTION_DAILY_MOTIVATION
            }
            val morningPi = PendingIntent.getBroadcast(
                appCtx,
                3001,
                morningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAllowWhileIdle(alarmManager, morningTrigger, morningPi)

            // 2. Evening 22:45 Agenda Reminder
            val eveningTrigger = computeNextDailyTriggerMillis(22, 45, now)
            val eveningIntent = Intent(appCtx, com.example.service.DailyReminderReceiver::class.java).apply {
                action = ACTION_DAILY_AGENDA_REMINDER
            }
            val eveningPi = PendingIntent.getBroadcast(
                appCtx,
                3002,
                eveningIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            scheduleAllowWhileIdle(alarmManager, eveningTrigger, eveningPi)
        } catch (t: Throwable) {
            Log.e("NotificationHelper", "Error scheduling daily reminders: ${t.message}")
        }
    }

    private fun scheduleAllowWhileIdle(
        alarmManager: android.app.AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
