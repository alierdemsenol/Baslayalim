package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.preferences.UserPreferences
import com.example.util.AppActionLogger
import com.example.util.MotivationalQuotes
import com.example.util.NotificationHelper

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val lang = UserPreferences.resolveSystemLanguage()
        val isTr = lang == "TR"
        val prefs = UserPreferences(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                NotificationHelper.scheduleDailyReminders(context)
            }
            NotificationHelper.ACTION_DAILY_MOTIVATION -> {
                val quote = MotivationalQuotes.getQuoteForUsageDay(prefs.usageDaysCount, lang)
                val title = if (isTr) "Günün Motivasyonu" else "Daily Motivation"
                val body = "\"${quote.text}\" — ${quote.author}"
                NotificationHelper.showDailyReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.MOTIVATION_NOTIFICATION_ID,
                    title = title,
                    content = body,
                    openAgenda = false
                )
                AppActionLogger.logAction(context, "NOTIF", "daily_motivation")
                NotificationHelper.scheduleDailyReminders(context)
            }
            NotificationHelper.ACTION_DAILY_AGENDA_REMINDER -> {
                val title = if (isTr) "Bugünkü Çalışmalarını Ajandaya İşle" else "Log Today's Study to Your Agenda"
                val body = if (isTr) {
                    "Günü kapatmadan çözdüğün testleri, tekrarları ve çalışma süreni Ajandam'a kaydetmeyi unutma!"
                } else {
                    "Before ending the day, remember to log your solved tests, topics, and study duration in My Agenda!"
                }
                NotificationHelper.showDailyReminderNotification(
                    context = context,
                    notificationId = NotificationHelper.AGENDA_REMINDER_NOTIFICATION_ID,
                    title = title,
                    content = body,
                    openAgenda = true
                )
                AppActionLogger.logAction(context, "NOTIF", "daily_agenda_reminder")
                NotificationHelper.scheduleDailyReminders(context)
            }
        }
    }
}
