package com.example.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.example.util.ActiveTimerManager
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val snap = ActiveTimerManager.ensureInitialized(applicationContext)
        val spec = ActiveTimerManager.buildCurrentNotificationContent(applicationContext, snap)

        if (!ActiveTimerManager.isAnyTimerRunning(snap) || spec == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val initialNotif = NotificationHelper.buildTimerNotification(
            context = applicationContext,
            title = spec.title,
            content = spec.content,
            mode = spec.mode,
            isRunning = spec.isRunning,
            isPaused = spec.isPaused
        )

        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(
            this,
            NotificationHelper.NOTIFICATION_ID,
            initialNotif,
            fgsType
        )

        startTickerLoop()
        return START_STICKY
    }

    private fun startTickerLoop() {
        if (tickJob?.isActive == true) return
        tickJob = serviceScope.launch {
            var lastMinuteAlerted = -1
            while (isActive) {
                val snap = ActiveTimerManager.checkAndHandleExpirations(applicationContext)
                if (!ActiveTimerManager.isAnyTimerRunning(snap)) {
                    stopSelf()
                    break
                }

                val spec = ActiveTimerManager.buildCurrentNotificationContent(applicationContext, snap)
                if (spec != null) {
                    NotificationHelper.showTimerNotification(
                        context = applicationContext,
                        title = spec.title,
                        content = spec.content,
                        mode = spec.mode,
                        isRunning = spec.isRunning,
                        isPaused = spec.isPaused
                    )
                }

                // Trigger 60-second vibration cue for Pomodoro or Custom Timer
                val now = System.currentTimeMillis()
                val rem60Hit = when (snap.activeNotificationMode) {
                    "TIMER" -> snap.customTimer.isRunning && snap.customTimer.currentRemainingSeconds(now) == 60
                    "POMODORO" -> (snap.pomodoro.stateName == "WORKING" || snap.pomodoro.stateName == "BREAKING") &&
                        snap.pomodoro.currentRemainingSeconds(now) == 60
                    else -> false
                }
                if (rem60Hit && lastMinuteAlerted != 60) {
                    lastMinuteAlerted = 60
                    NotificationHelper.triggerVibrationWarning(applicationContext)
                } else if (!rem60Hit) {
                    lastMinuteAlerted = -1
                }

                delay(1000L)
            }
        }
    }

    override fun onDestroy() {
        tickJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        fun stop(context: Context) {
            try {
                context.applicationContext.stopService(
                    Intent(context.applicationContext, TimerForegroundService::class.java)
                )
            } catch (_: Throwable) {
            }
        }
    }
}
