package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StudyStatsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
            } finally {
                pendingResult?.finish()
            }
        }
    }

    internal fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val stats = ExamWidgetUpdater.loadWeeklyStudyStats(context)
        val palette = ExamWidgetUpdater.resolveWidgetTheme(context)
        val isTr = UserPreferences.resolveSystemLanguage() == "TR"

        val headerText = if (isTr) "Bu Hafta İstatistiklerim" else "Weekly Study Stats"
        val activeDaysText = if (isTr) "${stats.activeDaysCount} Aktif Gün" else "${stats.activeDaysCount} Active Days"
        val avgLabel = if (isTr) "Günlük Ort." else "Daily Avg."
        val todayLabel = if (isTr) "Bugün" else "Today"
        val testsLabel = if (isTr) "Haftalık Test" else "Weekly Tests"
        val avgVal = ExamWidgetUpdater.formatMinutesCompact(stats.averageDailyMinutes, isTr)
        val todayVal = ExamWidgetUpdater.formatMinutesCompact(stats.todayTotalMinutes, isTr)
        val totalVal = ExamWidgetUpdater.formatMinutesCompact(stats.totalAllTimeMinutes, isTr)
        val totalFooter = if (isTr) "Bu Hafta Toplam: $totalVal" else "This Week Total: $totalVal"

        appWidgetIds.forEach { widgetId ->
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120)

            val views = RemoteViews(context.packageName, R.layout.widget_study_stats)
            ExamWidgetUpdater.applyBaseSurfaceTheme(views, palette)

            views.setInt(R.id.widget_stats_icon_flame, "setColorFilter", palette.primaryColor)
            views.setTextViewText(R.id.widget_stats_header, headerText)
            views.setTextColor(R.id.widget_stats_header, palette.onSurfaceColor)

            views.setInt(R.id.widget_stats_active_days_bg, "setColorFilter", palette.pillBackgroundColor)
            views.setTextViewText(R.id.widget_stats_active_days, activeDaysText)
            views.setTextColor(R.id.widget_stats_active_days, palette.pillTextColor)

            views.setInt(R.id.widget_stats_avg_bg, "setColorFilter", palette.pillBackgroundColor)
            views.setTextViewText(R.id.widget_stats_avg_value, avgVal)
            views.setTextColor(R.id.widget_stats_avg_value, palette.onSurfaceColor)
            views.setTextViewText(R.id.widget_stats_avg_label, avgLabel)
            views.setTextColor(R.id.widget_stats_avg_label, palette.pillTextColor)

            views.setInt(R.id.widget_stats_today_bg, "setColorFilter", palette.rowBackgroundColor)
            views.setTextViewText(R.id.widget_stats_today_value, todayVal)
            views.setTextColor(R.id.widget_stats_today_value, palette.onSurfaceColor)
            views.setTextViewText(R.id.widget_stats_today_label, todayLabel)
            views.setTextColor(R.id.widget_stats_today_label, palette.onSurfaceVariantColor)

            views.setInt(R.id.widget_stats_tests_bg, "setColorFilter", palette.rowBackgroundColor)
            views.setTextViewText(R.id.widget_stats_tests_value, "${stats.totalTestsSolved}")
            views.setTextColor(R.id.widget_stats_tests_value, palette.onSurfaceColor)
            views.setTextViewText(R.id.widget_stats_tests_label, testsLabel)
            views.setTextColor(R.id.widget_stats_tests_label, palette.onSurfaceVariantColor)

            views.setTextViewText(R.id.widget_stats_total_footer, totalFooter)
            views.setTextColor(R.id.widget_stats_total_footer, palette.onSurfaceVariantColor)
            views.setViewVisibility(
                R.id.widget_stats_total_footer,
                if (maxHeight in 1..95) View.GONE else View.VISIBLE
            )

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context,
                7000 + widgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_stats_root, openPi)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
