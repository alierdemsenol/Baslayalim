package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import com.example.R
import com.example.data.database.AppDatabase
import com.example.data.model.DashboardStats
import com.example.data.model.StudyStatsCalculator
import com.example.data.preferences.UserPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WidgetExamItem(
    val title: String,
    val daysRemaining: Long,
    val formattedDate: String
)

data class WidgetThemePalette(
    val isDark: Boolean,
    val isPitchBlack: Boolean,
    val accentColor: Int,
    val backgroundColor: Int,
    val borderColor: Int,
    val pillBackgroundColor: Int,
    val pillTextColor: Int,
    val rowBackgroundColor: Int,
    val onSurfaceColor: Int,
    val onSurfaceVariantColor: Int,
    val primaryColor: Int,
    val onPrimaryColor: Int
)

object ExamWidgetUpdater {

    fun resolveWidgetTheme(context: Context): WidgetThemePalette {
        val prefs = UserPreferences(context)
        val systemDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val isDark = when (prefs.appThemeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> systemDark
        }
        val isPitchBlack = isDark && prefs.isPitchBlackDarkEnabled
        val accentColor = try {
            Color.parseColor(prefs.appAccentColorHex)
        } catch (_: Exception) {
            0xFFFF2D75.toInt()
        }
        val onPrimaryColor = if (ColorUtils.calculateLuminance(accentColor) > 0.55) {
            Color.BLACK
        } else {
            Color.WHITE
        }

        return when {
            isPitchBlack -> {
                val bg = Color.BLACK
                WidgetThemePalette(
                    isDark = true,
                    isPitchBlack = true,
                    accentColor = accentColor,
                    backgroundColor = bg,
                    borderColor = ColorUtils.blendARGB(bg, accentColor, 0.72f),
                    pillBackgroundColor = ColorUtils.blendARGB(bg, accentColor, 0.24f),
                    pillTextColor = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.20f),
                    rowBackgroundColor = ColorUtils.blendARGB(0xFF0B0B0E.toInt(), accentColor, 0.08f),
                    onSurfaceColor = Color.WHITE,
                    onSurfaceVariantColor = 0xFFC8C5D0.toInt(),
                    primaryColor = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.14f),
                    onPrimaryColor = onPrimaryColor
                )
            }
            isDark -> {
                val bg = 0xFF1C1B22.toInt()
                WidgetThemePalette(
                    isDark = true,
                    isPitchBlack = false,
                    accentColor = accentColor,
                    backgroundColor = bg,
                    borderColor = 0xFF33303C.toInt(),
                    pillBackgroundColor = ColorUtils.blendARGB(bg, accentColor, 0.26f),
                    pillTextColor = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.20f),
                    rowBackgroundColor = ColorUtils.blendARGB(0xFF292731.toInt(), accentColor, 0.06f),
                    onSurfaceColor = 0xFFE6E0E9.toInt(),
                    onSurfaceVariantColor = 0xFFCAC4D0.toInt(),
                    primaryColor = ColorUtils.blendARGB(accentColor, Color.WHITE, 0.12f),
                    onPrimaryColor = onPrimaryColor
                )
            }
            else -> {
                val bg = 0xFFFEF7FF.toInt()
                WidgetThemePalette(
                    isDark = false,
                    isPitchBlack = false,
                    accentColor = accentColor,
                    backgroundColor = bg,
                    borderColor = 0xFFE7E0EC.toInt(),
                    pillBackgroundColor = ColorUtils.blendARGB(Color.WHITE, accentColor, 0.16f),
                    pillTextColor = ColorUtils.blendARGB(accentColor, 0xFF1D1B20.toInt(), 0.14f),
                    rowBackgroundColor = ColorUtils.blendARGB(0xFFF3EDF7.toInt(), accentColor, 0.07f),
                    onSurfaceColor = 0xFF1D1B20.toInt(),
                    onSurfaceVariantColor = 0xFF49454F.toInt(),
                    primaryColor = accentColor,
                    onPrimaryColor = onPrimaryColor
                )
            }
        }
    }

    private val pitchBlackGradientCache = java.util.concurrent.ConcurrentHashMap<Int, Bitmap>()

    fun getOrCreatePitchBlackGradientBitmap(accentColor: Int): Bitmap {
        return pitchBlackGradientCache.getOrPut(accentColor) {
            val width = 420
            val height = 260
            val cornerRadius = 38f
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val pixels = IntArray(width * height)
            val cx = width / 2f
            val cy = height / 2f
            val halfW = width / 2f
            val halfH = height / 2f
            val accentR = Color.red(accentColor)
            val accentG = Color.green(accentColor)
            val accentB = Color.blue(accentColor)

            for (y in 0 until height) {
                val dyNorm = ((y + 0.5f) - cy) / halfH
                val dyCorner = kotlin.math.abs((y + 0.5f) - cy) - (halfH - cornerRadius)
                val rowOffset = y * width
                for (x in 0 until width) {
                    val dxCorner = kotlin.math.abs((x + 0.5f) - cx) - (halfW - cornerRadius)
                    val cornerAlpha = if (dxCorner > 0f && dyCorner > 0f) {
                        val dist = kotlin.math.hypot(dxCorner.toDouble(), dyCorner.toDouble()).toFloat() - cornerRadius
                        ((0.5f - dist).coerceIn(0f, 1f) * 255f).toInt()
                    } else {
                        255
                    }
                    if (cornerAlpha <= 0) {
                        pixels[rowOffset + x] = 0
                        continue
                    }

                    val dxNorm = ((x + 0.5f) - cx) / halfW
                    val normR = kotlin.math.sqrt((dxNorm * dxNorm + dyNorm * dyNorm).toDouble()).toFloat()
                    // Keep inner text area pure #000000, then smoothly fade to accent past the text towards edges
                    val accentRatio = when {
                        normR <= 0.56f -> 0f
                        normR <= 0.78f -> ((normR - 0.56f) / 0.22f) * 0.24f
                        normR <= 0.94f -> 0.24f + ((normR - 0.78f) / 0.16f) * 0.26f
                        else -> (0.50f + ((normR - 0.94f) / 0.28f) * 0.22f).coerceAtMost(0.72f)
                    }
                    val r = (accentR * accentRatio).toInt().coerceIn(0, 255)
                    val g = (accentG * accentRatio).toInt().coerceIn(0, 255)
                    val b = (accentB * accentRatio).toInt().coerceIn(0, 255)
                    pixels[rowOffset + x] = (cornerAlpha shl 24) or (r shl 16) or (g shl 8) or b
                }
            }
            bmp.setPixels(pixels, 0, width, 0, 0, width, height)
            bmp
        }
    }

    fun applyBaseSurfaceTheme(views: RemoteViews, palette: WidgetThemePalette) {
        if (palette.isPitchBlack) {
            views.setInt(R.id.widget_bg_fill_layer, "setColorFilter", 0)
            views.setImageViewBitmap(
                R.id.widget_bg_fill_layer,
                getOrCreatePitchBlackGradientBitmap(palette.accentColor)
            )
        } else {
            views.setImageViewResource(R.id.widget_bg_fill_layer, R.drawable.widget_shape_fill)
            views.setInt(R.id.widget_bg_fill_layer, "setColorFilter", palette.backgroundColor)
        }
        views.setInt(R.id.widget_bg_border_layer, "setColorFilter", palette.borderColor)
        views.setInt(R.id.widget_bg_border_layer, "setImageAlpha", 255)
    }

    fun formatCountdownText(examTitleOrKey: String, daysRemaining: Long, isTr: Boolean): String {
        val isMsu = examTitleOrKey.contains("MSÜ", ignoreCase = true) ||
            examTitleOrKey.contains("MSU", ignoreCase = true)
        return if (isMsu) {
            if (isTr) "Şafak $daysRemaining" else "$daysRemaining days left"
        } else {
            if (isTr) "$daysRemaining gün kaldı" else "$daysRemaining days left"
        }
    }

    fun formatMinutesCompact(totalMinutes: Int, isTr: Boolean): String {
        val hrS = if (isTr) "sa" else "h"
        val minS = if (isTr) "dk" else "min"
        if (totalMinutes <= 0) return "0 $minS"
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "$h $hrS $m $minS"
            h > 0 -> "$h $hrS"
            else -> "$m $minS"
        }
    }

    private fun loadParentSnapshotIfActive(prefs: UserPreferences): com.example.data.model.ParentStudentSnapshot? {
        if (prefs.userRole != UserPreferences.ROLE_PARENT) return null
        val raw = prefs.parentSyncedPayloadJson
        if (raw.isBlank()) return null
        return com.example.util.BackupHelper.parseParentStudentSnapshot(raw)
    }

    fun loadExamItems(context: Context): List<WidgetExamItem> {
        val prefs = UserPreferences(context)
        val isTr = UserPreferences.resolveSystemLanguage() == "TR"
        val now = System.currentTimeMillis()
        val dateFmt = SimpleDateFormat("dd.MM.yyyy", Locale.US)
        val parentSnap = loadParentSnapshotIfActive(prefs)

        fun buildItem(title: String, targetMs: Long): WidgetExamItem {
            val diff = (targetMs - now).coerceAtLeast(0L)
            val days = diff / (24L * 3600L * 1000L)
            return WidgetExamItem(
                title = title,
                daysRemaining = days,
                formattedDate = dateFmt.format(Date(targetMs))
            )
        }

        val target = parentSnap?.examTarget ?: prefs.examTarget
        val track = parentSnap?.userTrack ?: prefs.userTrack
        val tytDate = parentSnap?.customTytExamDate?.takeIf { it > 0L } ?: prefs.customTytExamDate
        val aytDate = parentSnap?.customAytExamDate?.takeIf { it > 0L } ?: prefs.customAytExamDate
        val lgsDate = parentSnap?.customLgsExamDate?.takeIf { it > 0L } ?: prefs.customLgsExamDate
        val kpssDate = parentSnap?.customKpssExamDate?.takeIf { it > 0L } ?: prefs.customKpssExamDate
        val msuDate = parentSnap?.customMsuExamDate?.takeIf { it > 0L } ?: prefs.customMsuExamDate

        val items = mutableListOf<WidgetExamItem>()
        when (target) {
            "LGS" -> {
                items.add(buildItem(if (isTr) "LGS 2027" else "LGS 2027 Exam", lgsDate))
            }
            "KPSS" -> {
                items.add(buildItem(if (isTr) "KPSS Lisans 2027" else "KPSS 2027 Exam", kpssDate))
            }
            else -> {
                items.add(buildItem("TYT 2027", tytDate))
                items.add(buildItem("AYT 2027", aytDate))
                if (track.trim().equals("Dil", ignoreCase = true)) {
                    items.add(buildItem("YDT 2027", aytDate))
                }
                items.add(buildItem("MSÜ 2027", msuDate))
            }
        }

        if (parentSnap != null) {
            parentSnap.customTimers.forEach { timer ->
                items.add(buildItem(timer.title, timer.targetTimestamp))
            }
        } else {
            try {
                val db = AppDatabase.getDatabase(context)
                val timers = kotlinx.coroutines.runBlocking {
                    db.customTimerDao().getAllTimersOnce()
                }
                timers.forEach { timer ->
                    items.add(buildItem(timer.title, timer.targetTimestamp))
                }
            } catch (_: Throwable) {
            }
        }

        return items
    }

    fun loadStudyStats(context: Context): DashboardStats {
        return try {
            val prefs = UserPreferences(context)
            val parentSnap = loadParentSnapshotIfActive(prefs)
            if (parentSnap != null) {
                return StudyStatsCalculator.compute(
                    sessions = parentSnap.sessions,
                    notes = parentSnap.dailyNotes,
                    firstUsageTimestampMillis = parentSnap.firstUsageTimestamp.takeIf { it > 0L }
                )
            }
            val db = AppDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                val sessions = db.studySessionDao().getAllSessionsOnce()
                val notes = db.dailyNoteDao().getAllNotesOnce()
                StudyStatsCalculator.compute(
                    sessions = sessions,
                    notes = notes,
                    firstUsageTimestampMillis = prefs.firstUsageTimestamp
                )
            }
        } catch (_: Throwable) {
            DashboardStats()
        }
    }

    fun loadWeeklyStudyStats(context: Context): DashboardStats {
        return try {
            val prefs = UserPreferences(context)
            val parentSnap = loadParentSnapshotIfActive(prefs)
            if (parentSnap != null) {
                return StudyStatsCalculator.computeWeekly(
                    sessions = parentSnap.sessions,
                    notes = parentSnap.dailyNotes,
                    firstUsageTimestampMillis = parentSnap.firstUsageTimestamp.takeIf { it > 0L }
                )
            }
            val db = AppDatabase.getDatabase(context)
            kotlinx.coroutines.runBlocking {
                val sessions = db.studySessionDao().getAllSessionsOnce()
                val notes = db.dailyNoteDao().getAllNotesOnce()
                StudyStatsCalculator.computeWeekly(
                    sessions = sessions,
                    notes = notes,
                    firstUsageTimestampMillis = prefs.firstUsageTimestamp
                )
            }
        } catch (_: Throwable) {
            DashboardStats()
        }
    }

    fun updateStopwatchWidgets(context: Context) {
        try {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)
            val swIds = manager.getAppWidgetIds(
                ComponentName(appContext, StopwatchWidgetProvider::class.java)
            )
            if (swIds.isNotEmpty()) {
                val intent = Intent(appContext, StopwatchWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, swIds)
                }
                appContext.sendBroadcast(intent)
            }
        } catch (_: Throwable) {
        }
    }

    fun updateAllWidgets(context: Context) {
        try {
            val appContext = context.applicationContext
            val manager = AppWidgetManager.getInstance(appContext)

            val singleIds = manager.getAppWidgetIds(
                ComponentName(appContext, ExamCountdownWidgetProvider::class.java)
            )
            if (singleIds.isNotEmpty()) {
                val intent = Intent(appContext, ExamCountdownWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, singleIds)
                }
                appContext.sendBroadcast(intent)
            }

            val allIds = manager.getAppWidgetIds(
                ComponentName(appContext, AllExamsWidgetProvider::class.java)
            )
            if (allIds.isNotEmpty()) {
                val intent = Intent(appContext, AllExamsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allIds)
                }
                appContext.sendBroadcast(intent)
            }

            val statsIds = manager.getAppWidgetIds(
                ComponentName(appContext, StudyStatsWidgetProvider::class.java)
            )
            if (statsIds.isNotEmpty()) {
                val intent = Intent(appContext, StudyStatsWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, statsIds)
                }
                appContext.sendBroadcast(intent)
            }

            updateStopwatchWidgets(appContext)
        } catch (_: Throwable) {
        }
    }
}
