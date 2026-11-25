package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.StudySession
import com.example.data.preferences.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.util.ActiveTimerManager
import com.example.util.AppActionLogger
import com.example.util.NotificationHelper
import com.example.util.TranslationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class StopwatchWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SW_START = "com.example.widget.ACTION_SW_START"
        const val ACTION_SW_PAUSE = "com.example.widget.ACTION_SW_PAUSE"
        const val ACTION_SW_RESET = "com.example.widget.ACTION_SW_RESET"
        const val ACTION_SW_SAVE = "com.example.widget.ACTION_SW_SAVE"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgets(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        when (intent.action) {
            ACTION_SW_START -> {
                val snap = ActiveTimerManager.ensureInitialized(context)
                ActiveTimerManager.startStopwatch(context, snap.stopwatch.subject)
                AppActionLogger.logAction(context, "WIDGET", "sw_start")
                refreshAllInstances(context)
            }
            ACTION_SW_PAUSE -> {
                ActiveTimerManager.pauseStopwatch(context)
                AppActionLogger.logAction(context, "WIDGET", "sw_pause")
                refreshAllInstances(context)
            }
            ACTION_SW_RESET -> {
                ActiveTimerManager.resetStopwatch(context)
                AppActionLogger.logAction(context, "WIDGET", "sw_reset")
                refreshAllInstances(context)
            }
            ACTION_SW_SAVE -> {
                val pendingResult: PendingResult? = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        saveCurrentStopwatchSession(context)
                    } finally {
                        pendingResult?.finish()
                    }
                }
            }
        }
    }

    private fun refreshAllInstances(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, StopwatchWidgetProvider::class.java))
        if (ids.isNotEmpty()) {
            updateWidgets(context, manager, ids)
        }
    }

    internal suspend fun saveCurrentStopwatchSession(context: Context) {
        val snap = ActiveTimerManager.ensureInitialized(context)
        val sw = snap.stopwatch
        val durationSec = sw.currentElapsedMillis() / 1000L
        if (durationSec > 0L) {
            val db = AppDatabase.getDatabase(context)
            val prefs = UserPreferences(context)
            val stage = if (prefs.examTarget == "YKS") {
                val raw = sw.examStage.ifBlank { "TYT" }
                if (raw.equals("KARISIK", ignoreCase = true)) "TYT" else raw
            } else ""
            val session = StudySession(
                subject = sw.subject,
                durationSeconds = durationSec,
                sessionType = "KRONOMETRE",
                blocksCompleted = 1,
                emergencyCount = 0,
                examStage = stage
            )
            db.studySessionDao().insertSession(session)
            StudyRepository.appendSessionToAgenda(db.studySessionDao(), db.dailyNoteDao(), session)
        }
        ActiveTimerManager.resetStopwatch(context)
        ExamWidgetUpdater.updateAllWidgets(context)
        refreshAllInstances(context)
        AppActionLogger.logAction(context, "WIDGET", "sw_save dur=${durationSec}s")
    }

    internal fun updateWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val snap = ActiveTimerManager.ensureInitialized(context)
        val sw = snap.stopwatch
        val now = System.currentTimeMillis()
        val elapsedMs = sw.currentElapsedMillis(now)
        val isPausedWithTime = !sw.isRunning && elapsedMs > 0L

        val palette = ExamWidgetUpdater.resolveWidgetTheme(context)
        val lang = UserPreferences.resolveSystemLanguage()
        val isTr = lang == "TR"
        val prefs = UserPreferences(context)

        val localizedSubject = TranslationHelper.localizeSubject(sw.subject, lang)
        val subjectBadge = if (prefs.examTarget == "YKS" && sw.examStage.isNotBlank()) {
            val stageLabel = when (sw.examStage) {
                "KARISIK" -> if (isTr) "Karışık" else "Mixed"
                else -> sw.examStage
            }
            "$stageLabel • $localizedSubject"
        } else {
            localizedSubject
        }

        val titleText = if (isTr) "Kronometre" else "Stopwatch"
        val statusText = when {
            sw.isRunning -> if (isTr) "Çalışıyor" else "Running"
            isPausedWithTime -> if (isTr) "Duraklatıldı" else "Paused"
            else -> if (isTr) "Hazır" else "Ready"
        }
        val startOrResumeLabel = when {
            isPausedWithTime -> if (isTr) "Devam Et" else "Resume"
            else -> if (isTr) "Başlat" else "Start"
        }
        val pauseLabel = if (isTr) "Duraklat" else "Pause"
        val saveLabel = if (isTr) "Kaydet" else "Save"

        val baseTime = SystemClock.elapsedRealtime() - elapsedMs

        appWidgetIds.forEach { widgetId ->
            val options = appWidgetManager.getAppWidgetOptions(widgetId)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 120)
            val isCompactHeight = maxHeight in 1..92

            val views = RemoteViews(context.packageName, R.layout.widget_stopwatch)
            ExamWidgetUpdater.applyBaseSurfaceTheme(views, palette)

            // Header & Subject pill theme
            views.setInt(R.id.widget_sw_icon, "setColorFilter", palette.primaryColor)
            views.setTextViewText(R.id.widget_sw_title, titleText)
            views.setTextColor(R.id.widget_sw_title, palette.onSurfaceColor)

            views.setInt(R.id.widget_sw_subject_bg, "setColorFilter", palette.pillBackgroundColor)
            views.setTextViewText(R.id.widget_sw_subject_badge, subjectBadge)
            views.setTextColor(R.id.widget_sw_subject_badge, palette.pillTextColor)

            // Live synchronized Chronometer
            views.setChronometer(R.id.widget_sw_chronometer, baseTime, null, sw.isRunning)
            views.setTextColor(R.id.widget_sw_chronometer, palette.onSurfaceColor)

            views.setTextViewText(R.id.widget_sw_status_text, statusText)
            views.setTextColor(
                R.id.widget_sw_status_text,
                if (sw.isRunning || isPausedWithTime) palette.primaryColor else palette.onSurfaceVariantColor
            )
            views.setViewVisibility(R.id.widget_sw_status_text, if (isCompactHeight) View.GONE else View.VISIBLE)

            // Action button colors
            views.setInt(R.id.widget_sw_btn_start_bg, "setColorFilter", palette.primaryColor)
            views.setInt(R.id.widget_sw_btn_start_icon, "setColorFilter", palette.onPrimaryColor)
            views.setTextViewText(R.id.widget_sw_btn_start_label, startOrResumeLabel)
            views.setTextColor(R.id.widget_sw_btn_start_label, palette.onPrimaryColor)

            views.setInt(R.id.widget_sw_btn_pause_bg, "setColorFilter", palette.primaryColor)
            views.setInt(R.id.widget_sw_btn_pause_icon, "setColorFilter", palette.onPrimaryColor)
            views.setTextViewText(R.id.widget_sw_btn_pause_label, pauseLabel)
            views.setTextColor(R.id.widget_sw_btn_pause_label, palette.onPrimaryColor)

            views.setInt(R.id.widget_sw_btn_save_bg, "setColorFilter", palette.pillBackgroundColor)
            views.setInt(R.id.widget_sw_btn_save_icon, "setColorFilter", palette.pillTextColor)
            views.setTextViewText(R.id.widget_sw_btn_save_label, saveLabel)
            views.setTextColor(R.id.widget_sw_btn_save_label, palette.pillTextColor)

            views.setInt(R.id.widget_sw_btn_reset_bg, "setColorFilter", palette.rowBackgroundColor)
            views.setInt(R.id.widget_sw_btn_reset_icon, "setColorFilter", palette.onSurfaceVariantColor)

            // Control visibility per state
            views.setViewVisibility(R.id.widget_sw_btn_start, if (sw.isRunning) View.GONE else View.VISIBLE)
            views.setViewVisibility(R.id.widget_sw_btn_pause, if (sw.isRunning) View.VISIBLE else View.GONE)
            views.setViewVisibility(
                R.id.widget_sw_btn_save,
                if (sw.isRunning || isPausedWithTime) View.VISIBLE else View.GONE
            )
            views.setViewVisibility(
                R.id.widget_sw_btn_reset,
                if (isPausedWithTime) View.VISIBLE else View.GONE
            )

            // Open Stopwatch screen when tapping header or clock area
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(NotificationHelper.EXTRA_OPEN_STOPWATCH, true)
            }
            val openPi = PendingIntent.getActivity(
                context,
                9000 + widgetId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_sw_header_area, openPi)
            views.setOnClickPendingIntent(R.id.widget_sw_clock_area, openPi)

            // Action broadcasts
            views.setOnClickPendingIntent(
                R.id.widget_sw_btn_start,
                buildBroadcastPi(context, ACTION_SW_START, 9100 + widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.widget_sw_btn_pause,
                buildBroadcastPi(context, ACTION_SW_PAUSE, 9200 + widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.widget_sw_btn_save,
                buildBroadcastPi(context, ACTION_SW_SAVE, 9300 + widgetId)
            )
            views.setOnClickPendingIntent(
                R.id.widget_sw_btn_reset,
                buildBroadcastPi(context, ACTION_SW_RESET, 9400 + widgetId)
            )

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    private fun buildBroadcastPi(context: Context, actionName: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, StopwatchWidgetProvider::class.java).apply {
            action = actionName
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
