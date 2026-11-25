package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.util.ActiveTimerManager
import com.example.util.NotificationHelper

class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val mode = intent.getStringExtra(NotificationHelper.EXTRA_TIMER_MODE)
        ActiveTimerManager.ensureInitialized(context)

        when (action) {
            NotificationHelper.ACTION_TIMER_PAUSE -> {
                ActiveTimerManager.handleNotificationPause(context, mode)
            }
            NotificationHelper.ACTION_TIMER_RESUME -> {
                ActiveTimerManager.handleNotificationResume(context, mode)
            }
            NotificationHelper.ACTION_TIMER_DISMISS -> {
                ActiveTimerManager.handleNotificationDismiss(context, mode)
            }
        }
    }
}
