package com.example.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.data.database.AppDatabase
import com.example.data.model.StudySession
import com.example.data.preferences.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.service.TimerForegroundService
import com.example.widget.ExamWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

data class PersistentStopwatchLap(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long
)

data class PersistentStopwatchState(
    val active: Boolean = false,
    val isRunning: Boolean = false,
    val startEpochMs: Long = 0L,
    val accumulatedMs: Long = 0L,
    val subject: String = "Matematik",
    val examStage: String = "TYT",
    val laps: List<PersistentStopwatchLap> = emptyList()
) {
    fun currentElapsedMillis(now: Long = System.currentTimeMillis()): Long {
        return when {
            isRunning && startEpochMs > 0L -> (now - startEpochMs).coerceAtLeast(0L)
            active -> accumulatedMs.coerceAtLeast(0L)
            else -> 0L
        }
    }
}

data class PersistentCustomTimerState(
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val endEpochMs: Long = 0L,
    val remainingSec: Int = 45 * 60,
    val totalSec: Int = 45 * 60,
    val durationMinutes: Int = 45,
    val subject: String = "Matematik",
    val examStage: String = "TYT",
    val isDenemeMode: Boolean = false,
    val denemeType: String = "TYT Deneme Sınavı (165 dk)"
) {
    fun currentRemainingSeconds(now: Long = System.currentTimeMillis()): Int {
        return if (isRunning && endEpochMs > 0L) {
            ((endEpochMs - now + 999L) / 1000L).toInt().coerceIn(0, totalSec.coerceAtLeast(1))
        } else {
            remainingSec.coerceAtLeast(0)
        }
    }
}

data class PersistentPomodoroState(
    val stateName: String = "IDLE",
    val endEpochMs: Long = 0L,
    val remainingSec: Int = 25 * 60,
    val totalSec: Int = 25 * 60,
    val subject: String = "Matematik",
    val examStage: String = "TYT",
    val workMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val longBreakMinutes: Int = 45,
    val completedBlocks: Int = 0,
    val baseSessionWorkSeconds: Long = 0L,
    val emergencyCount: Int = 0,
    val isLongBreak: Boolean = false
) {
    fun currentRemainingSeconds(now: Long = System.currentTimeMillis()): Int {
        return if ((stateName == "WORKING" || stateName == "BREAKING") && endEpochMs > 0L) {
            ((endEpochMs - now + 999L) / 1000L).toInt().coerceIn(0, totalSec.coerceAtLeast(1))
        } else {
            remainingSec.coerceAtLeast(0)
        }
    }

    fun currentTotalWorkSeconds(now: Long = System.currentTimeMillis()): Long {
        return if (stateName == "WORKING" || stateName == "WORK_PAUSED") {
            val elapsedInBlock = (totalSec - currentRemainingSeconds(now)).coerceAtLeast(0)
            baseSessionWorkSeconds + elapsedInBlock
        } else {
            baseSessionWorkSeconds
        }
    }
}

data class ActiveTimerSnapshot(
    val activeNotificationMode: String = "NONE", // "POMODORO", "TIMER", "STOPWATCH", "NONE"
    val stopwatch: PersistentStopwatchState = PersistentStopwatchState(),
    val customTimer: PersistentCustomTimerState = PersistentCustomTimerState(),
    val pomodoro: PersistentPomodoroState = PersistentPomodoroState(),
    val eventVersion: Long = 0L
)

object ActiveTimerManager {

    private const val PREFS_NAME = "baslayalim_timer_state"
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()

    @Volatile
    private var initialized = false

    private val _snapshot = MutableStateFlow(ActiveTimerSnapshot())
    val snapshot: StateFlow<ActiveTimerSnapshot> = _snapshot.asStateFlow()

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun ensureInitialized(context: Context): ActiveTimerSnapshot {
        synchronized(lock) {
            if (!initialized) {
                val loaded = loadFromPrefs(context.applicationContext)
                _snapshot.value = loaded
                initialized = true
                // Clean up stale notification if nothing is actually running or paused
                if (loaded.activeNotificationMode == "NONE") {
                    NotificationHelper.cancelTimerNotification(context.applicationContext)
                }
            }
            return _snapshot.value
        }
    }

    fun getActiveMode(context: Context): String {
        return ensureInitialized(context).activeNotificationMode
    }

    private fun loadFromPrefs(context: Context): ActiveTimerSnapshot {
        val p = prefs(context)
        val userPrefs = UserPreferences(context)
        val defaultSubject = when (userPrefs.examTarget) {
            "LGS" -> "LGS Türkçe"
            "KPSS" -> "KPSS Türkçe"
            else -> "TYT Türkçe"
        }
        val defaultStage = if (userPrefs.examTarget == "YKS") "TYT" else ""

        val swLaps = decodeLaps(p.getString("sw_laps_json", "[]") ?: "[]")
        val rawSwActive = p.getBoolean("sw_active", false)
        val rawSwRunning = p.getBoolean("sw_running", false)
        val rawSwStart = if (rawSwRunning) p.getLong("sw_start_epoch_ms", 0L) else 0L
        val rawSwAcc = if (rawSwActive || rawSwRunning) p.getLong("sw_accumulated_ms", 0L) else 0L
        val sw = PersistentStopwatchState(
            active = rawSwActive,
            isRunning = rawSwRunning,
            startEpochMs = rawSwStart,
            accumulatedMs = rawSwAcc,
            subject = p.getString("sw_subject", defaultSubject) ?: defaultSubject,
            examStage = p.getString("sw_exam_stage", defaultStage) ?: defaultStage,
            laps = if (rawSwActive || rawSwRunning) swLaps else emptyList()
        )

        val tm = PersistentCustomTimerState(
            isRunning = p.getBoolean("tm_running", false),
            isPaused = p.getBoolean("tm_paused", false),
            endEpochMs = p.getLong("tm_end_epoch_ms", 0L),
            remainingSec = p.getInt("tm_remaining_sec", 45 * 60),
            totalSec = p.getInt("tm_total_sec", 45 * 60),
            durationMinutes = p.getInt("tm_duration_min", 45),
            subject = p.getString("tm_subject", defaultSubject) ?: defaultSubject,
            examStage = p.getString("tm_exam_stage", defaultStage) ?: defaultStage,
            isDenemeMode = p.getBoolean("tm_is_deneme", false),
            denemeType = p.getString("tm_deneme_type", "TYT Deneme Sınavı (165 dk)") ?: "TYT Deneme Sınavı (165 dk)"
        )

        val pm = PersistentPomodoroState(
            stateName = p.getString("pm_state", "IDLE") ?: "IDLE",
            endEpochMs = p.getLong("pm_end_epoch_ms", 0L),
            remainingSec = p.getInt("pm_remaining_sec", 25 * 60),
            totalSec = p.getInt("pm_total_sec", 25 * 60),
            subject = p.getString("pm_subject", defaultSubject) ?: defaultSubject,
            examStage = p.getString("pm_exam_stage", defaultStage) ?: defaultStage,
            workMinutes = p.getInt("pm_work_min", 25),
            breakMinutes = p.getInt("pm_break_min", 5),
            longBreakMinutes = p.getInt("pm_long_break_min", 45),
            completedBlocks = p.getInt("pm_blocks_done", 0),
            baseSessionWorkSeconds = p.getLong("pm_base_work_sec", 0L),
            emergencyCount = p.getInt("pm_emergency_count", 0),
            isLongBreak = p.getBoolean("pm_is_long_break", false)
        )

        val mode = p.getString("active_notif_mode", "NONE") ?: "NONE"
        return ActiveTimerSnapshot(
            activeNotificationMode = mode,
            stopwatch = sw,
            customTimer = tm,
            pomodoro = pm,
            eventVersion = p.getLong("event_version", 1L)
        )
    }

    private fun saveToPrefs(context: Context, snap: ActiveTimerSnapshot) {
        val p = prefs(context).edit()
        p.putString("active_notif_mode", snap.activeNotificationMode)
        p.putLong("event_version", snap.eventVersion)

        // Stopwatch
        p.putBoolean("sw_active", snap.stopwatch.active)
        p.putBoolean("sw_running", snap.stopwatch.isRunning)
        p.putLong("sw_start_epoch_ms", snap.stopwatch.startEpochMs)
        p.putLong("sw_accumulated_ms", snap.stopwatch.accumulatedMs)
        p.putString("sw_subject", snap.stopwatch.subject)
        p.putString("sw_exam_stage", snap.stopwatch.examStage)
        p.putString("sw_laps_json", encodeLaps(snap.stopwatch.laps))

        // Custom Timer
        p.putBoolean("tm_running", snap.customTimer.isRunning)
        p.putBoolean("tm_paused", snap.customTimer.isPaused)
        p.putLong("tm_end_epoch_ms", snap.customTimer.endEpochMs)
        p.putInt("tm_remaining_sec", snap.customTimer.remainingSec)
        p.putInt("tm_total_sec", snap.customTimer.totalSec)
        p.putInt("tm_duration_min", snap.customTimer.durationMinutes)
        p.putString("tm_subject", snap.customTimer.subject)
        p.putString("tm_exam_stage", snap.customTimer.examStage)
        p.putBoolean("tm_is_deneme", snap.customTimer.isDenemeMode)
        p.putString("tm_deneme_type", snap.customTimer.denemeType)

        // Pomodoro
        p.putString("pm_state", snap.pomodoro.stateName)
        p.putLong("pm_end_epoch_ms", snap.pomodoro.endEpochMs)
        p.putInt("pm_remaining_sec", snap.pomodoro.remainingSec)
        p.putInt("pm_total_sec", snap.pomodoro.totalSec)
        p.putString("pm_subject", snap.pomodoro.subject)
        p.putString("pm_exam_stage", snap.pomodoro.examStage)
        p.putInt("pm_work_min", snap.pomodoro.workMinutes)
        p.putInt("pm_break_min", snap.pomodoro.breakMinutes)
        p.putInt("pm_long_break_min", snap.pomodoro.longBreakMinutes)
        p.putInt("pm_blocks_done", snap.pomodoro.completedBlocks)
        p.putLong("pm_base_work_sec", snap.pomodoro.baseSessionWorkSeconds)
        p.putInt("pm_emergency_count", snap.pomodoro.emergencyCount)
        p.putBoolean("pm_is_long_break", snap.pomodoro.isLongBreak)

        p.apply()
    }

    private fun updateSnapshot(context: Context, transform: (ActiveTimerSnapshot) -> ActiveTimerSnapshot): ActiveTimerSnapshot {
        synchronized(lock) {
            val current = ensureInitialized(context)
            val next = transform(current).copy(eventVersion = current.eventVersion + 1L)
            _snapshot.value = next
            saveToPrefs(context, next)
            return next
        }
    }

    // ====================================================
    // STOPWATCH ACTIONS
    // ====================================================
    fun startStopwatch(context: Context, subject: String) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            if (cur.stopwatch.isRunning) return@updateSnapshot cur
            val startEpoch = now - cur.stopwatch.accumulatedMs.coerceAtLeast(0L)
            cur.copy(
                activeNotificationMode = "STOPWATCH",
                stopwatch = cur.stopwatch.copy(
                    active = true,
                    isRunning = true,
                    startEpochMs = startEpoch,
                    subject = subject
                )
            )
        }
        AppActionLogger.logAction(context, "STOPWATCH", "start")
        syncServiceAndNotification(context, snap)
        ExamWidgetUpdater.updateStopwatchWidgets(context)
    }

    fun pauseStopwatch(context: Context) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val elapsed = cur.stopwatch.currentElapsedMillis(now)
            cur.copy(
                activeNotificationMode = if (elapsed > 0L || cur.stopwatch.active) "STOPWATCH" else "NONE",
                stopwatch = cur.stopwatch.copy(
                    active = elapsed > 0L || cur.stopwatch.active,
                    isRunning = false,
                    startEpochMs = 0L,
                    accumulatedMs = elapsed
                )
            )
        }
        AppActionLogger.logAction(context, "STOPWATCH", "pause elapsed=${snap.stopwatch.accumulatedMs / 1000}s")
        syncServiceAndNotification(context, snap)
        ExamWidgetUpdater.updateStopwatchWidgets(context)
    }

    fun setStopwatchSubject(context: Context, subject: String) {
        updateSnapshot(context) { cur ->
            cur.copy(stopwatch = cur.stopwatch.copy(subject = subject))
        }
        ExamWidgetUpdater.updateStopwatchWidgets(context)
    }

    fun setStopwatchExamStage(context: Context, examStage: String) {
        updateSnapshot(context) { cur ->
            cur.copy(stopwatch = cur.stopwatch.copy(examStage = examStage))
        }
        ExamWidgetUpdater.updateStopwatchWidgets(context)
    }

    fun addStopwatchLap(context: Context) {
        val now = System.currentTimeMillis()
        updateSnapshot(context) { cur ->
            val currentTotal = cur.stopwatch.currentElapsedMillis(now)
            val lapNumber = cur.stopwatch.laps.size + 1
            val lastTotal = cur.stopwatch.laps.lastOrNull()?.totalTimeMillis ?: 0L
            val lapTime = (currentTotal - lastTotal).coerceAtLeast(0L)
            cur.copy(
                stopwatch = cur.stopwatch.copy(
                    laps = cur.stopwatch.laps + PersistentStopwatchLap(lapNumber, lapTime, currentTotal)
                )
            )
        }
        AppActionLogger.logAction(context, "STOPWATCH", "lap_added")
    }

    fun resetStopwatch(context: Context) {
        val snap = updateSnapshot(context) { cur ->
            val nextMode = fallbackNotificationMode(cur, clearing = "STOPWATCH")
            cur.copy(
                activeNotificationMode = nextMode,
                stopwatch = cur.stopwatch.copy(
                    active = false,
                    isRunning = false,
                    startEpochMs = 0L,
                    accumulatedMs = 0L,
                    laps = emptyList()
                )
            )
        }
        AppActionLogger.logAction(context, "STOPWATCH", "reset")
        syncServiceAndNotification(context, snap)
        ExamWidgetUpdater.updateStopwatchWidgets(context)
    }

    // ====================================================
    // CUSTOM TIMER ACTIONS
    // ====================================================
    fun updateCustomTimerConfig(
        context: Context,
        subject: String,
        isDenemeMode: Boolean,
        denemeType: String,
        durationMinutes: Int,
        examStage: String? = null
    ) {
        val totalSec = durationMinutes * 60
        updateSnapshot(context) { cur ->
            if (cur.customTimer.isRunning) return@updateSnapshot cur
            cur.copy(
                customTimer = cur.customTimer.copy(
                    subject = subject,
                    examStage = examStage ?: cur.customTimer.examStage,
                    isDenemeMode = isDenemeMode,
                    denemeType = denemeType,
                    durationMinutes = durationMinutes,
                    totalSec = totalSec,
                    remainingSec = totalSec,
                    isPaused = false
                )
            )
        }
    }

    fun startOrResumeCustomTimer(context: Context) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            if (cur.customTimer.isRunning) return@updateSnapshot cur
            val rem = cur.customTimer.remainingSec.coerceAtLeast(1)
            cur.copy(
                activeNotificationMode = "TIMER",
                customTimer = cur.customTimer.copy(
                    isRunning = true,
                    isPaused = false,
                    endEpochMs = now + rem * 1000L
                )
            )
        }
        AppActionLogger.logAction(context, "TIMER", "start_or_resume rem=${snap.customTimer.remainingSec}s deneme=${snap.customTimer.isDenemeMode}")
        syncServiceAndNotification(context, snap)
    }

    fun pauseCustomTimer(context: Context) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val rem = cur.customTimer.currentRemainingSeconds(now)
            cur.copy(
                activeNotificationMode = if (rem > 0) "TIMER" else fallbackNotificationMode(cur, "TIMER"),
                customTimer = cur.customTimer.copy(
                    isRunning = false,
                    isPaused = rem > 0,
                    endEpochMs = 0L,
                    remainingSec = rem
                )
            )
        }
        AppActionLogger.logAction(context, "TIMER", "pause rem=${snap.customTimer.remainingSec}s")
        syncServiceAndNotification(context, snap)
    }

    fun resetCustomTimer(context: Context) {
        val snap = updateSnapshot(context) { cur ->
            val totalSec = cur.customTimer.durationMinutes * 60
            cur.copy(
                activeNotificationMode = fallbackNotificationMode(cur, "TIMER"),
                customTimer = cur.customTimer.copy(
                    isRunning = false,
                    isPaused = false,
                    endEpochMs = 0L,
                    totalSec = totalSec,
                    remainingSec = totalSec
                )
            )
        }
        AppActionLogger.logAction(context, "TIMER", "reset")
        syncServiceAndNotification(context, snap)
    }

    // ====================================================
    // POMODORO ACTIONS
    // ====================================================
    fun updatePomodoroConfig(
        context: Context,
        subject: String? = null,
        examStage: String? = null,
        workMinutes: Int? = null,
        breakMinutes: Int? = null,
        longBreakMinutes: Int? = null
    ) {
        updateSnapshot(context) { cur ->
            val pm = cur.pomodoro
            val newWork = workMinutes ?: pm.workMinutes
            val newBreak = breakMinutes ?: pm.breakMinutes
            val newLong = longBreakMinutes ?: pm.longBreakMinutes
            val newSubj = subject ?: pm.subject
            val newStage = examStage ?: pm.examStage
            val resetRem = if (pm.stateName == "IDLE" && workMinutes != null) newWork * 60 else pm.remainingSec
            val resetTotal = if (pm.stateName == "IDLE" && workMinutes != null) newWork * 60 else pm.totalSec
            cur.copy(
                pomodoro = pm.copy(
                    subject = newSubj,
                    examStage = newStage,
                    workMinutes = newWork,
                    breakMinutes = newBreak,
                    longBreakMinutes = newLong,
                    remainingSec = resetRem,
                    totalSec = resetTotal
                )
            )
        }
    }

    fun startPomodoroWork(context: Context, subject: String, workMinutes: Int, breakMinutes: Int, examStage: String? = null) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val pm = cur.pomodoro
            val isFreshIdle = pm.stateName == "IDLE"
            val totalSec = if (isFreshIdle) workMinutes * 60 else pm.totalSec
            val remSec = if (isFreshIdle) workMinutes * 60 else pm.remainingSec.coerceAtLeast(1)
            cur.copy(
                activeNotificationMode = "POMODORO",
                pomodoro = pm.copy(
                    stateName = "WORKING",
                    subject = subject,
                    examStage = examStage ?: pm.examStage,
                    workMinutes = workMinutes,
                    breakMinutes = breakMinutes,
                    totalSec = totalSec,
                    remainingSec = remSec,
                    endEpochMs = now + remSec * 1000L,
                    emergencyCount = if (isFreshIdle) 0 else pm.emergencyCount,
                    isLongBreak = false
                )
            )
        }
        AppActionLogger.logAction(context, "POMODORO", "start_work min=$workMinutes")
        syncServiceAndNotification(context, snap)
    }

    fun resumePomodoroWork(context: Context) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val pm = cur.pomodoro
            if (pm.stateName != "WORK_PAUSED") return@updateSnapshot cur
            val rem = pm.remainingSec.coerceAtLeast(1)
            cur.copy(
                activeNotificationMode = "POMODORO",
                pomodoro = pm.copy(
                    stateName = "WORKING",
                    endEpochMs = now + rem * 1000L,
                    isLongBreak = false
                )
            )
        }
        AppActionLogger.logAction(context, "POMODORO", "resume_work rem=${snap.pomodoro.remainingSec}s")
        syncServiceAndNotification(context, snap)
    }

    fun pausePomodoro(context: Context, incrementEmergency: Boolean) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val pm = cur.pomodoro
            when (pm.stateName) {
                "WORKING" -> {
                    val rem = pm.currentRemainingSeconds(now)
                    cur.copy(
                        activeNotificationMode = "POMODORO",
                        pomodoro = pm.copy(
                            stateName = "WORK_PAUSED",
                            endEpochMs = 0L,
                            remainingSec = rem,
                            emergencyCount = if (incrementEmergency) pm.emergencyCount + 1 else pm.emergencyCount
                        )
                    )
                }
                "BREAKING" -> {
                    val rem = pm.currentRemainingSeconds(now)
                    cur.copy(
                        activeNotificationMode = "POMODORO",
                        pomodoro = pm.copy(
                            stateName = "BREAK_PAUSED",
                            endEpochMs = 0L,
                            remainingSec = rem,
                            emergencyCount = if (incrementEmergency) pm.emergencyCount + 1 else pm.emergencyCount
                        )
                    )
                }
                else -> cur
            }
        }
        // Normalize totalSec cleanly for progress ring if needed: wait — let's keep original work totalSec and track blockBaseWorkSeconds instead!
        AppActionLogger.logAction(context, "POMODORO", "pause state=${snap.pomodoro.stateName}")
        syncServiceAndNotification(context, snap)
    }

    fun startPomodoroBreak(context: Context, isLongBreak: Boolean, durationMinutes: Int) {
        val now = System.currentTimeMillis()
        val breakSec = durationMinutes * 60
        val snap = updateSnapshot(context) { cur ->
            cur.copy(
                activeNotificationMode = "POMODORO",
                pomodoro = cur.pomodoro.copy(
                    stateName = "BREAKING",
                    totalSec = breakSec,
                    remainingSec = breakSec,
                    endEpochMs = now + breakSec * 1000L,
                    isLongBreak = isLongBreak,
                    longBreakMinutes = if (isLongBreak) durationMinutes else cur.pomodoro.longBreakMinutes
                )
            )
        }
        AppActionLogger.logAction(context, "POMODORO", "start_break long=$isLongBreak min=$durationMinutes")
        syncServiceAndNotification(context, snap)
    }

    fun resumePomodoroBreak(context: Context) {
        val now = System.currentTimeMillis()
        val snap = updateSnapshot(context) { cur ->
            val pm = cur.pomodoro
            if (pm.stateName != "BREAK_PAUSED") return@updateSnapshot cur
            val rem = pm.remainingSec.coerceAtLeast(1)
            cur.copy(
                activeNotificationMode = "POMODORO",
                pomodoro = pm.copy(
                    stateName = "BREAKING",
                    endEpochMs = now + rem * 1000L
                )
            )
        }
        AppActionLogger.logAction(context, "POMODORO", "resume_break")
        syncServiceAndNotification(context, snap)
    }

    fun finishPomodoroToDailyStats(context: Context) {
        val snap = updateSnapshot(context) { cur ->
            cur.copy(
                activeNotificationMode = fallbackNotificationMode(cur, "POMODORO"),
                pomodoro = cur.pomodoro.copy(
                    stateName = "SESSION_FINISHED_DAILY_STATS",
                    endEpochMs = 0L
                )
            )
        }
        syncServiceAndNotification(context, snap)
    }

    fun resetPomodoro(context: Context) {
        val snap = updateSnapshot(context) { cur ->
            val workSec = cur.pomodoro.workMinutes * 60
            cur.copy(
                activeNotificationMode = fallbackNotificationMode(cur, "POMODORO"),
                pomodoro = cur.pomodoro.copy(
                    stateName = "IDLE",
                    endEpochMs = 0L,
                    remainingSec = workSec,
                    totalSec = workSec,
                    completedBlocks = 0,
                    baseSessionWorkSeconds = 0L,
                    emergencyCount = 0,
                    isLongBreak = false
                )
            )
        }
        AppActionLogger.logAction(context, "POMODORO", "reset")
        syncServiceAndNotification(context, snap)
    }

    // ====================================================
    // NOTIFICATION BUTTON HANDLERS (WORK EVEN WHEN APP CLOSED)
    // ====================================================
    fun handleNotificationPause(context: Context, requestedMode: String?) {
        val snap = ensureInitialized(context)
        val mode = requestedMode ?: snap.activeNotificationMode
        when (mode) {
            "STOPWATCH" -> pauseStopwatch(context)
            "TIMER" -> pauseCustomTimer(context)
            "POMODORO" -> pausePomodoro(context, incrementEmergency = false)
        }
    }

    fun handleNotificationResume(context: Context, requestedMode: String?) {
        val snap = ensureInitialized(context)
        val mode = requestedMode ?: snap.activeNotificationMode
        when (mode) {
            "STOPWATCH" -> startStopwatch(context, snap.stopwatch.subject)
            "TIMER" -> startOrResumeCustomTimer(context)
            "POMODORO" -> {
                if (snap.pomodoro.stateName == "BREAK_PAUSED") {
                    resumePomodoroBreak(context)
                } else if (snap.pomodoro.stateName == "WORK_PAUSED") {
                    resumePomodoroWork(context)
                }
            }
        }
    }

    fun handleNotificationDismiss(context: Context, requestedMode: String?) {
        val snap = ensureInitialized(context)
        val mode = requestedMode ?: snap.activeNotificationMode
        val next = updateSnapshot(context) { cur ->
            cur.copy(activeNotificationMode = fallbackNotificationMode(cur, mode))
        }
        AppActionLogger.logAction(context, "NOTIF", "dismiss mode=$mode")
        syncServiceAndNotification(context, next)
    }

    // ====================================================
    // BACKGROUND COMPLETION & TICK EVALUATION
    // ====================================================
    fun checkAndHandleExpirations(context: Context): ActiveTimerSnapshot {
        val now = System.currentTimeMillis()
        val cur = ensureInitialized(context)
        val lang = UserPreferences.resolveSystemLanguage()

        // 1. Check Custom Timer expiration
        if (cur.customTimer.isRunning && cur.customTimer.currentRemainingSeconds(now) <= 0) {
            val tm = cur.customTimer
            val durationSec = tm.totalSec.toLong()
            val sessionType = if (tm.isDenemeMode) "DENEME" else "SAYAÇ"
            val subjName = if (tm.isDenemeMode) tm.denemeType else tm.subject
            val label = if (tm.isDenemeMode) {
                "${TranslationHelper.getTranslation("notif_deneme_prefix", lang)}: ${TranslationHelper.localizeDenemeOption(tm.denemeType, lang)}"
            } else {
                "${TranslationHelper.getTranslation("notif_timer_prefix", lang)}: ${TranslationHelper.localizeSubject(tm.subject, lang)}"
            }
            ioScope.launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    val session = StudySession(
                        subject = subjName,
                        durationSeconds = durationSec,
                        sessionType = sessionType,
                        blocksCompleted = 1,
                        emergencyCount = 0,
                        examStage = tm.examStage
                    )
                    db.studySessionDao().insertSession(session)
                    StudyRepository.appendSessionToAgenda(db.studySessionDao(), db.dailyNoteDao(), session)
                    ExamWidgetUpdater.updateAllWidgets(context)
                } catch (e: Exception) {
                    AppActionLogger.logError(context, "TIMER", "save_fail:${e.javaClass.simpleName}")
                }
            }
            AppActionLogger.logAction(context, "TIMER", "completed dur=${durationSec}s")
            val totalSec = tm.durationMinutes * 60
            val next = updateSnapshot(context) { s ->
                s.copy(
                    activeNotificationMode = fallbackNotificationMode(s, "TIMER"),
                    customTimer = s.customTimer.copy(
                        isRunning = false,
                        isPaused = false,
                        endEpochMs = 0L,
                        totalSec = totalSec,
                        remainingSec = totalSec
                    )
                )
            }
            NotificationHelper.triggerVibrationWarning(context)
            NotificationHelper.showTimerNotification(
                context = context,
                title = TranslationHelper.getTranslation("notif_timer_finished", lang),
                content = "$label ${TranslationHelper.getTranslation("notif_timer_finished_desc", lang)}",
                mode = "NONE",
                isRunning = false,
                isPaused = false
            )
            if (next.activeNotificationMode == "NONE") {
                TimerForegroundService.stop(context)
            }
            return next
        }

        // 2. Check Pomodoro expiration
        val pm = cur.pomodoro
        if (pm.stateName == "WORKING" && pm.currentRemainingSeconds(now) <= 0) {
            val newBlocks = pm.completedBlocks + 1
            val newWorkSec = pm.baseSessionWorkSeconds + pm.totalSec
            val nextState = if (newBlocks > 0 && newBlocks % 4 == 0) {
                "LONG_BREAK_PROMPT"
            } else {
                "BLOCK_FINISHED_PROMPT"
            }
            val next = updateSnapshot(context) { s ->
                s.copy(
                    activeNotificationMode = fallbackNotificationMode(s, "POMODORO"),
                    pomodoro = s.pomodoro.copy(
                        stateName = nextState,
                        endEpochMs = 0L,
                        remainingSec = 0,
                        completedBlocks = newBlocks,
                        baseSessionWorkSeconds = newWorkSec,
                        longBreakMinutes = if (nextState == "LONG_BREAK_PROMPT") 45 else s.pomodoro.longBreakMinutes
                    )
                )
            }
            AppActionLogger.logAction(context, "POMODORO", "block_done blocks=$newBlocks")
            val locSubj = TranslationHelper.localizeSubject(pm.subject, lang)
            NotificationHelper.triggerVibrationWarning(context)
            NotificationHelper.showTimerNotification(
                context = context,
                title = TranslationHelper.getTranslation("notif_block_done", lang),
                content = String.format(Locale.US, TranslationHelper.getTranslation("notif_block_done_desc_fmt", lang), locSubj, pm.workMinutes),
                mode = "NONE",
                isRunning = false,
                isPaused = false
            )
            if (next.activeNotificationMode == "NONE") {
                TimerForegroundService.stop(context)
            }
            return next
        } else if (pm.stateName == "BREAKING" && pm.currentRemainingSeconds(now) <= 0) {
            if (pm.isLongBreak) {
                val savedWorkSec = pm.baseSessionWorkSeconds
                val savedBlocks = pm.completedBlocks
                val savedEmerg = pm.emergencyCount
                val savedSubj = pm.subject
                if (savedWorkSec > 0L) {
                    ioScope.launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val session = StudySession(
                                subject = savedSubj,
                                durationSeconds = savedWorkSec,
                                sessionType = "POMODORO",
                                blocksCompleted = savedBlocks.coerceAtLeast(1),
                                emergencyCount = savedEmerg,
                                examStage = pm.examStage
                            )
                            db.studySessionDao().insertSession(session)
                            StudyRepository.appendSessionToAgenda(db.studySessionDao(), db.dailyNoteDao(), session)
                            ExamWidgetUpdater.updateAllWidgets(context)
                        } catch (e: Exception) {
                            AppActionLogger.logError(context, "POMODORO", "bg_save_fail:${e.javaClass.simpleName}")
                        }
                    }
                }
                val workSec = pm.workMinutes * 60
                val next = updateSnapshot(context) { s ->
                    s.copy(
                        activeNotificationMode = fallbackNotificationMode(s, "POMODORO"),
                        pomodoro = s.pomodoro.copy(
                            stateName = "IDLE",
                            endEpochMs = 0L,
                            totalSec = workSec,
                            remainingSec = workSec,
                            completedBlocks = 0,
                            baseSessionWorkSeconds = 0L,
                            emergencyCount = 0,
                            isLongBreak = false
                        )
                    )
                }
                NotificationHelper.triggerVibrationWarning(context)
                NotificationHelper.showTimerNotification(
                    context = context,
                    title = TranslationHelper.getTranslation("notif_long_break_done", lang),
                    content = TranslationHelper.getTranslation("notif_long_break_done_desc", lang),
                    mode = "NONE",
                    isRunning = false,
                    isPaused = false
                )
                if (next.activeNotificationMode == "NONE") {
                    TimerForegroundService.stop(context)
                }
                return next
            } else {
                val workSec = pm.workMinutes * 60
                val next = updateSnapshot(context) { s ->
                    s.copy(
                        activeNotificationMode = "POMODORO",
                        pomodoro = s.pomodoro.copy(
                            stateName = "WORKING",
                            totalSec = workSec,
                            remainingSec = workSec,
                            endEpochMs = now + workSec * 1000L,
                            isLongBreak = false
                        )
                    )
                }
                NotificationHelper.triggerVibrationWarning(context)
                syncServiceAndNotification(context, next)
                return next
            }
        }

        return cur
    }

    fun isAnyTimerRunning(snap: ActiveTimerSnapshot): Boolean {
        return snap.stopwatch.isRunning ||
            snap.customTimer.isRunning ||
            snap.pomodoro.stateName == "WORKING" ||
            snap.pomodoro.stateName == "BREAKING"
    }

    fun buildCurrentNotificationContent(context: Context, snap: ActiveTimerSnapshot): NotificationSpec? {
        val now = System.currentTimeMillis()
        val lang = UserPreferences.resolveSystemLanguage()
        return when (snap.activeNotificationMode) {
            "STOPWATCH" -> {
                val sw = snap.stopwatch
                if (!sw.active && !sw.isRunning) return null
                val secs = (sw.currentElapsedMillis(now) / 1000L).toInt()
                val locSubj = TranslationHelper.localizeSubject(sw.subject, lang)
                val title = if (sw.isRunning) {
                    TranslationHelper.getTranslation("notif_sw_running", lang)
                } else {
                    TranslationHelper.getTranslation("notif_sw_paused", lang)
                }
                NotificationSpec(
                    title = title,
                    content = "$locSubj - ${TranslationHelper.getTranslation("notif_sw_elapsed", lang)}: ${formatTime(secs)}",
                    mode = "STOPWATCH",
                    isRunning = sw.isRunning,
                    isPaused = !sw.isRunning && sw.active
                )
            }
            "TIMER" -> {
                val tm = snap.customTimer
                if (!tm.isRunning && !tm.isPaused) return null
                val rem = tm.currentRemainingSeconds(now)
                val baseLabel = if (tm.isDenemeMode) {
                    "${TranslationHelper.getTranslation("notif_deneme_prefix", lang)}: ${TranslationHelper.localizeDenemeOption(tm.denemeType, lang)}"
                } else {
                    "${TranslationHelper.getTranslation("notif_timer_prefix", lang)}: ${TranslationHelper.localizeSubject(tm.subject, lang)}"
                }
                val title = if (tm.isRunning) baseLabel else "$baseLabel (${TranslationHelper.getTranslation("pomo_state_paused", lang)})"
                NotificationSpec(
                    title = title,
                    content = "${TranslationHelper.getTranslation("notif_remaining_time", lang)}: ${formatTime(rem)}",
                    mode = "TIMER",
                    isRunning = tm.isRunning,
                    isPaused = tm.isPaused
                )
            }
            "POMODORO" -> {
                val pm = snap.pomodoro
                val rem = pm.currentRemainingSeconds(now)
                val locSubj = TranslationHelper.localizeSubject(pm.subject, lang)
                when (pm.stateName) {
                    "WORKING" -> NotificationSpec(
                        title = "${TranslationHelper.getTranslation("notif_pomo_running", lang)}: $locSubj",
                        content = "${TranslationHelper.getTranslation("notif_remaining_time", lang)}: ${formatTime(rem)}",
                        mode = "POMODORO",
                        isRunning = true,
                        isPaused = false
                    )
                    "WORK_PAUSED" -> NotificationSpec(
                        title = TranslationHelper.getTranslation("notif_pomo_paused", lang),
                        content = "$locSubj - ${TranslationHelper.getTranslation("notif_remaining", lang)}: ${formatTime(rem)}",
                        mode = "POMODORO",
                        isRunning = false,
                        isPaused = true
                    )
                    "BREAKING" -> NotificationSpec(
                        title = TranslationHelper.getTranslation(if (pm.isLongBreak) "notif_long_break" else "notif_break", lang),
                        content = "${TranslationHelper.getTranslation(if (pm.isLongBreak) "notif_long_break_rem" else "notif_break_rem", lang)}: ${formatTime(rem)}",
                        mode = "POMODORO",
                        isRunning = true,
                        isPaused = false
                    )
                    "BREAK_PAUSED" -> NotificationSpec(
                        title = TranslationHelper.getTranslation("pomo_state_break_paused", lang),
                        content = "${TranslationHelper.getTranslation("notif_remaining", lang)}: ${formatTime(rem)}",
                        mode = "POMODORO",
                        isRunning = false,
                        isPaused = true
                    )
                    else -> null
                }
            }
            else -> null
        }
    }

    fun syncServiceAndNotification(context: Context, snap: ActiveTimerSnapshot) {
        val running = isAnyTimerRunning(snap)
        val spec = buildCurrentNotificationContent(context, snap)
        if (running && spec != null) {
            try {
                val intent = Intent(context.applicationContext, TimerForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ContextCompat.startForegroundService(context.applicationContext, intent)
                } else {
                    context.applicationContext.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w("ActiveTimerManager", "FGS start fallback: ${t.message}")
                NotificationHelper.showTimerNotification(
                    context,
                    spec.title,
                    spec.content,
                    spec.mode,
                    spec.isRunning,
                    spec.isPaused
                )
            }
        } else {
            TimerForegroundService.stop(context)
            if (spec != null && spec.isPaused) {
                NotificationHelper.showTimerNotification(
                    context,
                    spec.title,
                    spec.content,
                    spec.mode,
                    isRunning = false,
                    isPaused = true
                )
            } else {
                NotificationHelper.cancelTimerNotification(context)
            }
        }
    }

    private fun fallbackNotificationMode(cur: ActiveTimerSnapshot, clearing: String): String {
        if (clearing != "POMODORO" && (cur.pomodoro.stateName == "WORKING" || cur.pomodoro.stateName == "BREAKING" || cur.pomodoro.stateName == "WORK_PAUSED" || cur.pomodoro.stateName == "BREAK_PAUSED")) {
            return "POMODORO"
        }
        if (clearing != "TIMER" && (cur.customTimer.isRunning || cur.customTimer.isPaused)) {
            return "TIMER"
        }
        if (clearing != "STOPWATCH" && (cur.stopwatch.isRunning || cur.stopwatch.active)) {
            return "STOPWATCH"
        }
        return "NONE"
    }

    private fun encodeLaps(laps: List<PersistentStopwatchLap>): String {
        val arr = JSONArray()
        laps.forEach { lap ->
            arr.put(
                JSONObject().apply {
                    put("n", lap.lapNumber)
                    put("l", lap.lapTimeMillis)
                    put("t", lap.totalTimeMillis)
                }
            )
        }
        return arr.toString()
    }

    private fun decodeLaps(json: String): List<PersistentStopwatchLap> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        PersistentStopwatchLap(
                            lapNumber = obj.optInt("n", i + 1),
                            lapTimeMillis = obj.optLong("l", 0L),
                            totalTimeMillis = obj.optLong("t", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun formatTime(seconds: Int): String {
        val safeSec = seconds.coerceAtLeast(0)
        val h = safeSec / 3600
        val m = (safeSec % 3600) / 60
        val s = safeSec % 60
        return if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }
}

data class NotificationSpec(
    val title: String,
    val content: String,
    val mode: String,
    val isRunning: Boolean,
    val isPaused: Boolean
)
