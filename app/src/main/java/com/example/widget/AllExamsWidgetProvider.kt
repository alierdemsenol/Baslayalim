package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AllExamsWidgetProvider : AppWidgetProvider() {

    private data class ExamRowViews(
        val containerId: Int,
        val bgId: Int,
        val titleId: Int,
        val daysId: Int
    )

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
        newOptions: android.os.Bundle
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
        val items = ExamWidgetUpdater.loadExamItems(context)
        val prefs = UserPreferences(context)
        val palette = ExamWidgetUpdater.resolveWidgetTheme(context)
        val isTr = UserPreferences.resolveSystemLanguage() == "TR"
        val headerText = if (isTr) "Başlayalım • Sınavlar" else "Başlayalım • Exams"
        val targetBadgeText = if (prefs.examTarget == "YKS") {
            if (prefs.userTrack.equals("Dil", ignoreCase = true)) "TYT • YDT" else "TYT • AYT"
        } else {
            prefs.examTarget
        }

        val rowIds = listOf(
            ExamRowViews(R.id.widget_row_1, R.id.widget_row_1_bg, R.id.widget_row_1_title, R.id.widget_row_1_days),
            ExamRowViews(R.id.widget_row_2, R.id.widget_row_2_bg, R.id.widget_row_2_title, R.id.widget_row_2_days),
            ExamRowViews(R.id.widget_row_3, R.id.widget_row_3_bg, R.id.widget_row_3_title, R.id.widget_row_3_days),
            ExamRowViews(R.id.widget_row_4, R.id.widget_row_4_bg, R.id.widget_row_4_title, R.id.widget_row_4_days)
        )

        appWidgetIds.forEach { widgetId ->
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 160)
            val maxVisibleRows = when {
                maxHeight in 1..95 -> 2
                maxHeight in 1..130 -> 3
                else -> 4
            }

            val views = RemoteViews(context.packageName, R.layout.widget_all_exams)
            ExamWidgetUpdater.applyBaseSurfaceTheme(views, palette)

            views.setInt(R.id.widget_all_icon_flame, "setColorFilter", palette.primaryColor)
            views.setTextViewText(R.id.widget_all_header, headerText)
            views.setTextColor(R.id.widget_all_header, palette.onSurfaceColor)

            views.setInt(R.id.widget_all_target_badge_bg, "setColorFilter", palette.pillBackgroundColor)
            views.setTextViewText(R.id.widget_all_target_badge, targetBadgeText)
            views.setTextColor(R.id.widget_all_target_badge, palette.pillTextColor)

            rowIds.forEachIndexed { index, row ->
                val item = items.getOrNull(index)
                if (item != null && index < maxVisibleRows) {
                    views.setViewVisibility(row.containerId, View.VISIBLE)
                    views.setInt(row.bgId, "setColorFilter", palette.rowBackgroundColor)
                    views.setTextViewText(row.titleId, item.title)
                    views.setTextColor(row.titleId, palette.onSurfaceColor)
                    val badge = ExamWidgetUpdater.formatCountdownText(item.title, item.daysRemaining, isTr)
                    views.setTextViewText(row.daysId, badge)
                    views.setTextColor(row.daysId, palette.primaryColor)
                } else {
                    views.setViewVisibility(row.containerId, View.GONE)
                }
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPi = PendingIntent.getActivity(
                context,
                5000 + widgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_all_root, openPi)
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
