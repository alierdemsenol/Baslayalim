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
import com.example.util.AppActionLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ExamCountdownWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_PREV_EXAM = "com.example.widget.ACTION_PREV_EXAM"
        const val ACTION_NEXT_EXAM = "com.example.widget.ACTION_NEXT_EXAM"
        private const val WIDGET_PREFS = "baslayalim_widget_prefs"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult: PendingResult? = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val items = ExamWidgetUpdater.loadExamItems(context)
                appWidgetIds.forEach { widgetId ->
                    updateSingleWidget(context, appWidgetManager, widgetId, items)
                }
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
                val items = ExamWidgetUpdater.loadExamItems(context)
                updateSingleWidget(context, appWidgetManager, widgetId = appWidgetId, items = items)
            } finally {
                pendingResult?.finish()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return
        if (action == ACTION_PREV_EXAM || action == ACTION_NEXT_EXAM) {
            val widgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            val pendingResult: PendingResult? = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val items = ExamWidgetUpdater.loadExamItems(context)
                    if (items.isNotEmpty()) {
                        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
                        val curIdx = prefs.getInt("idx_$widgetId", 0).coerceIn(0, items.lastIndex)
                        val nextIdx = if (action == ACTION_PREV_EXAM) {
                            if (curIdx > 0) curIdx - 1 else items.lastIndex
                        } else {
                            if (curIdx < items.lastIndex) curIdx + 1 else 0
                        }
                        prefs.edit().putInt("idx_$widgetId", nextIdx).apply()
                        AppActionLogger.logAction(context, "WIDGET", "switch_exam")
                        updateSingleWidget(context, AppWidgetManager.getInstance(context), widgetId, items)
                    }
                } finally {
                    pendingResult?.finish()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE).edit()
        appWidgetIds.forEach { prefs.remove("idx_$it") }
        prefs.apply()
    }

    internal fun updateSingleWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        widgetId: Int,
        items: List<WidgetExamItem>
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_exam_countdown)
        val palette = ExamWidgetUpdater.resolveWidgetTheme(context)
        ExamWidgetUpdater.applyBaseSurfaceTheme(views, palette)

        views.setInt(R.id.widget_icon_flame, "setColorFilter", palette.primaryColor)
        views.setInt(R.id.widget_btn_prev, "setColorFilter", palette.onSurfaceColor)
        views.setInt(R.id.widget_btn_next, "setColorFilter", palette.onSurfaceColor)
        views.setTextColor(R.id.widget_days_text, palette.onSurfaceColor)
        views.setTextColor(R.id.widget_exam_title, palette.primaryColor)
        views.setTextColor(R.id.widget_exam_date, palette.onSurfaceVariantColor)

        val isTr = UserPreferences.resolveSystemLanguage() == "TR"
        val prefs = context.getSharedPreferences(WIDGET_PREFS, Context.MODE_PRIVATE)
        val options = appWidgetManager.getAppWidgetOptions(widgetId)
        val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 80)
        val maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 200)
        val isCompactHeight = maxHeight in 1..62
        val isUltraNarrow = maxWidth in 1..155

        views.setViewVisibility(R.id.widget_icon_flame, if (isUltraNarrow) View.GONE else View.VISIBLE)

        if (items.isNotEmpty()) {
            val idx = prefs.getInt("idx_$widgetId", 0).coerceIn(0, items.lastIndex)
            val current = items[idx]
            val daysText = ExamWidgetUpdater.formatCountdownText(current.title, current.daysRemaining, isTr)
            views.setTextViewText(R.id.widget_days_text, daysText)
            views.setTextViewText(R.id.widget_exam_title, "${current.title} • ${idx + 1}/${items.size}")
            views.setTextViewText(R.id.widget_exam_date, current.formattedDate)
            views.setViewVisibility(R.id.widget_exam_date, if (isCompactHeight) View.GONE else View.VISIBLE)
        } else {
            views.setTextViewText(R.id.widget_days_text, if (isTr) "-- gün kaldı" else "-- Days")
            views.setTextViewText(R.id.widget_exam_title, "Başlayalım")
            views.setTextViewText(R.id.widget_exam_date, "")
            views.setViewVisibility(R.id.widget_exam_date, View.GONE)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(
            context,
            widgetId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_content_area, openPi)

        val prevIntent = Intent(context, ExamCountdownWidgetProvider::class.java).apply {
            action = ACTION_PREV_EXAM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val prevPi = PendingIntent.getBroadcast(
            context,
            widgetId * 10 + 1,
            prevIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_prev, prevPi)

        val nextIntent = Intent(context, ExamCountdownWidgetProvider::class.java).apply {
            action = ACTION_NEXT_EXAM
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        val nextPi = PendingIntent.getBroadcast(
            context,
            widgetId * 10 + 2,
            nextIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_next, nextPi)

        appWidgetManager.updateAppWidget(widgetId, views)
    }
}
