package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.YKSAppShell
import com.example.ui.theme.YKSOdakTheme
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.ActiveTimerManager
import com.example.util.AppActionLogger
import com.example.util.NotificationHelper
import com.example.widget.ExamWidgetUpdater

class MainActivity : ComponentActivity() {

    private val studyViewModel: StudyViewModel by viewModels()
    private var openAgendaTrigger by mutableIntStateOf(0)
    private var openAgendaOnLaunch by mutableStateOf(false)
    private var openStopwatchTrigger by mutableIntStateOf(0)
    private var openStopwatchOnLaunch by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppActionLogger.logAction(this, "LIFECYCLE", "MainActivity_onCreate")
        NotificationHelper.scheduleDailyReminders(this)
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_AGENDA, false) == true) {
            openAgendaOnLaunch = true
            openAgendaTrigger++
        }
        if (intent?.getBooleanExtra(NotificationHelper.EXTRA_OPEN_STOPWATCH, false) == true) {
            openStopwatchOnLaunch = true
            openStopwatchTrigger++
        }
        setContent {
            val themeMode by studyViewModel.themeMode.collectAsStateWithLifecycle()
            val accentColorHex by studyViewModel.accentColorHex.collectAsStateWithLifecycle()
            val isPitchBlackDarkEnabled by studyViewModel.isPitchBlackDarkEnabled.collectAsStateWithLifecycle()

            YKSOdakTheme(
                themeMode = themeMode,
                accentColorHex = accentColorHex,
                pitchBlackDark = isPitchBlackDarkEnabled
            ) {
                YKSAppShell(
                    viewModel = studyViewModel,
                    openAgendaTrigger = openAgendaTrigger,
                    initialOpenAgenda = openAgendaOnLaunch,
                    openStopwatchTrigger = openStopwatchTrigger,
                    initialOpenStopwatch = openStopwatchOnLaunch
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_AGENDA, false)) {
            openAgendaOnLaunch = true
            openAgendaTrigger++
        }
        if (intent.getBooleanExtra(NotificationHelper.EXTRA_OPEN_STOPWATCH, false)) {
            openStopwatchOnLaunch = true
            openStopwatchTrigger++
        }
    }

    override fun onResume() {
        super.onResume()
        studyViewModel.syncSystemLanguage()
        studyViewModel.refreshExamCountdowns()
        ActiveTimerManager.checkAndHandleExpirations(this)
        ExamWidgetUpdater.updateAllWidgets(this)
    }
}
