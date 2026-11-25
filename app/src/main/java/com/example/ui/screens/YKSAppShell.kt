package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.preferences.UserPreferences
import com.example.ui.viewmodel.FocusModeTarget
import com.example.ui.viewmodel.PomodoroState
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.AppActionLogger
import com.example.util.TranslationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

data class AppTabItem(
    val route: String,
    val titleKey: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun YKSAppShell(
    viewModel: StudyViewModel,
    openAgendaTrigger: Int = 0,
    initialOpenAgenda: Boolean = false,
    openStopwatchTrigger: Int = 0,
    initialOpenStopwatch: Boolean = false
) {
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val userLanguage by viewModel.userLanguage.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val isParentPairingEnabled by viewModel.isParentPairingEnabled.collectAsState()
    val savedMainTab by viewModel.selectedMainTab.collectAsState()
    val focusLockedTarget by viewModel.focusLockedTarget.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val timerIsRunning by viewModel.timerIsRunning.collectAsState()
    val timerIsPaused by viewModel.timerIsPaused.collectAsState()
    val stopwatchIsRunning by viewModel.stopwatchIsRunning.collectAsState()
    val stopwatchElapsedMillis by viewModel.stopwatchElapsedMillis.collectAsState()

    val initialTab = remember { if (initialOpenStopwatch) 2 else savedMainTab.coerceIn(0, 2) }
    var selectedTabItem by remember { mutableIntStateOf(initialTab) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAgendaOpen by remember { mutableStateOf(false) }
    var isStatsOpen by remember { mutableStateOf(false) }
    var isParentPairingOpen by remember { mutableStateOf(false) }

    LaunchedEffect(userRole, isParentPairingEnabled) {
        if (userRole == UserPreferences.ROLE_PARENT) {
            isAgendaOpen = false
            isStatsOpen = false
            isParentPairingOpen = false
        } else if (!isParentPairingEnabled) {
            isParentPairingOpen = false
        }
    }

    var lastHandledAgendaTrigger by remember { mutableIntStateOf(if (initialOpenAgenda) -1 else openAgendaTrigger) }
    LaunchedEffect(openAgendaTrigger, initialOpenAgenda, isOnboarded, userRole) {
        if (isOnboarded && userRole == UserPreferences.ROLE_STUDENT &&
            (openAgendaTrigger > lastHandledAgendaTrigger || (initialOpenAgenda && lastHandledAgendaTrigger == -1))
        ) {
            lastHandledAgendaTrigger = openAgendaTrigger
            isSettingsOpen = false
            isStatsOpen = false
            isParentPairingOpen = false
            isAgendaOpen = true
        }
    }

    var showDarkFocusOverlay by remember { mutableStateOf(false) }
    var darkFocusResetKey by remember { mutableIntStateOf(0) }

    val bottomTabs = remember {
        listOf(
            AppTabItem("main", "tab_main", Icons.Filled.Home, Icons.Outlined.Home),
            AppTabItem("timer_hub", "tab_timer", Icons.Filled.Timer, Icons.Outlined.Timer),
            AppTabItem("stopwatch", "tab_stopwatch", Icons.Filled.AvTimer, Icons.Outlined.AvTimer)
        )
    }

    val pagerState = rememberPagerState(initialPage = initialTab, pageCount = { bottomTabs.size })
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var lastHandledStopwatchTrigger by remember { mutableIntStateOf(if (initialOpenStopwatch) -1 else openStopwatchTrigger) }
    LaunchedEffect(openStopwatchTrigger, initialOpenStopwatch, isOnboarded, userRole) {
        if (isOnboarded && userRole == UserPreferences.ROLE_STUDENT &&
            (openStopwatchTrigger > lastHandledStopwatchTrigger || (initialOpenStopwatch && lastHandledStopwatchTrigger == -1))
        ) {
            lastHandledStopwatchTrigger = openStopwatchTrigger
            isSettingsOpen = false
            isAgendaOpen = false
            isStatsOpen = false
            isParentPairingOpen = false
            selectedTabItem = 2
            viewModel.selectMainTab(2)
            pagerState.scrollToPage(2)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in bottomTabs.indices) {
            selectedTabItem = pagerState.currentPage
            viewModel.selectMainTab(pagerState.currentPage)
        }
    }

    val isLockActive = isOnboarded &&
        userRole == UserPreferences.ROLE_STUDENT &&
        !isSettingsOpen && !isAgendaOpen && !isStatsOpen && !isParentPairingOpen &&
        when (focusLockedTarget) {
            FocusModeTarget.TIMER -> selectedTabItem == 1 && (timerIsRunning || timerIsPaused)
            FocusModeTarget.POMODORO -> selectedTabItem == 1 && (
                pomodoroState == PomodoroState.WORKING ||
                    pomodoroState == PomodoroState.WORK_PAUSED ||
                    pomodoroState == PomodoroState.BREAKING ||
                    pomodoroState == PomodoroState.BREAK_PAUSED
                )
            FocusModeTarget.STOPWATCH -> selectedTabItem == 2 && (stopwatchIsRunning || stopwatchElapsedMillis > 0L)
            null -> false
        }

    LaunchedEffect(focusLockedTarget, isLockActive, darkFocusResetKey) {
        if (focusLockedTarget != null && isLockActive) {
            delay(5000L)
            showDarkFocusOverlay = true
        } else {
            showDarkFocusOverlay = false
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled gracefully */ }
    )

    LaunchedEffect(Unit) {
        viewModel.syncSystemLanguage()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val shouldKeepScreenOn = (focusLockedTarget != null && isLockActive) || showDarkFocusOverlay
    val activity = context as? Activity
    DisposableEffect(showDarkFocusOverlay, shouldKeepScreenOn) {
        val window = activity?.window
        if (window != null) {
            if (shouldKeepScreenOn) {
                window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            val lp = window.attributes
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (showDarkFocusOverlay) {
                lp.screenBrightness = 0.02f
                window.attributes = lp
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            } else {
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = lp
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val win = activity?.window
            if (win != null) {
                win.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                val lp = win.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                win.attributes = lp
                WindowCompat.getInsetsController(win, win.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isOnboarded) {
            OnboardingScreen(viewModel = viewModel)
        } else if (userRole == UserPreferences.ROLE_PARENT) {
            BackHandler(enabled = isSettingsOpen) {
                isSettingsOpen = false
            }
            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isSettingsOpen) {
                        SettingsScreen(
                            viewModel = viewModel,
                            onBack = { isSettingsOpen = false }
                        )
                    } else {
                        ParentDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToSettings = {
                                AppActionLogger.logAction(context, "NAV", "open_parent_settings")
                                isSettingsOpen = true
                            }
                        )
                    }
                }
            }
        } else {
            BackHandler(enabled = !showDarkFocusOverlay && (isSettingsOpen || isAgendaOpen || isStatsOpen || isParentPairingOpen)) {
                when {
                    isSettingsOpen -> isSettingsOpen = false
                    isAgendaOpen -> isAgendaOpen = false
                    isStatsOpen -> isStatsOpen = false
                    isParentPairingOpen -> isParentPairingOpen = false
                }
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (showDarkFocusOverlay) Modifier.clearAndSetSemantics {} else Modifier)
            ) {
                val useNavigationRail = maxWidth >= 600.dp &&
                    !isSettingsOpen && !isAgendaOpen && !isStatsOpen && !isParentPairingOpen

                if (useNavigationRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.safeDrawing)
                    ) {
                        if (!showDarkFocusOverlay) {
                            NavigationRail(
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                bottomTabs.forEachIndexed { index, tab ->
                                    val isSelected = selectedTabItem == index
                                    val titleText = TranslationHelper.getTranslation(tab.titleKey, userLanguage)
                                    NavigationRailItem(
                                        selected = isSelected,
                                        onClick = {
                                            selectedTabItem = index
                                            AppActionLogger.logAction(context, "NAV", "tab=${tab.route}")
                                            coroutineScope.launch {
                                                pagerState.scrollToPage(index)
                                            }
                                        },
                                        label = { Text(titleText) },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = titleText
                                            )
                                        },
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_${tab.route}")
                                    )
                                }
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                beyondViewportPageCount = 0,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 720.dp)
                            ) { page ->
                                when (page) {
                                    0 -> MainDashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToSettings = {
                                            AppActionLogger.logAction(context, "NAV", "open_settings")
                                            isSettingsOpen = true
                                        },
                                        onNavigateToAgenda = {
                                            AppActionLogger.logAction(context, "NAV", "open_agenda")
                                            isAgendaOpen = true
                                        },
                                        onNavigateToStats = {
                                            AppActionLogger.logAction(context, "NAV", "open_stats")
                                            isStatsOpen = true
                                        },
                                        isParentPairingEnabled = isParentPairingEnabled,
                                        onNavigateToParentPairing = {
                                            if (isParentPairingEnabled) {
                                                AppActionLogger.logAction(context, "NAV", "open_parent_pairing")
                                                isParentPairingOpen = true
                                            }
                                        }
                                    )
                                    1 -> TimerHubScreen(viewModel = viewModel)
                                    2 -> StopwatchScreen(viewModel = viewModel)
                                }
                            }
                        }
                    }
                } else {
                    Scaffold(
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            if (!isSettingsOpen && !isAgendaOpen && !isStatsOpen && !isParentPairingOpen && !showDarkFocusOverlay) {
                                NavigationBar(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 2.dp
                                ) {
                                    bottomTabs.forEachIndexed { index, tab ->
                                        val isSelected = selectedTabItem == index
                                        val titleText = TranslationHelper.getTranslation(tab.titleKey, userLanguage)
                                        NavigationBarItem(
                                            selected = isSelected,
                                            onClick = {
                                                selectedTabItem = index
                                                AppActionLogger.logAction(context, "NAV", "tab=${tab.route}")
                                                coroutineScope.launch {
                                                    pagerState.scrollToPage(index)
                                                }
                                            },
                                            label = { Text(titleText) },
                                            icon = {
                                                Icon(
                                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                    contentDescription = titleText
                                                )
                                            },
                                            colors = NavigationBarItemDefaults.colors(
                                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                                indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                            ),
                                            modifier = Modifier.testTag("nav_tab_${tab.route}")
                                        )
                                    }
                                }
                            }
                        }
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            when {
                                isSettingsOpen -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        onBack = { isSettingsOpen = false }
                                    )
                                }
                                isAgendaOpen -> {
                                    AgendaScreen(
                                        viewModel = viewModel,
                                        onBack = { isAgendaOpen = false }
                                    )
                                }
                                isStatsOpen -> {
                                    StatisticsScreen(
                                        viewModel = viewModel,
                                        onBack = { isStatsOpen = false }
                                    )
                                }
                                isParentPairingOpen && isParentPairingEnabled -> {
                                    ParentPairingScreen(
                                        viewModel = viewModel,
                                        onBack = { isParentPairingOpen = false }
                                    )
                                }
                                else -> {
                                    HorizontalPager(
                                        state = pagerState,
                                        beyondViewportPageCount = 0,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        when (page) {
                                            0 -> MainDashboardScreen(
                                                viewModel = viewModel,
                                                onNavigateToSettings = {
                                                    AppActionLogger.logAction(context, "NAV", "open_settings")
                                                    isSettingsOpen = true
                                                },
                                                onNavigateToAgenda = {
                                                    AppActionLogger.logAction(context, "NAV", "open_agenda")
                                                    isAgendaOpen = true
                                                },
                                                onNavigateToStats = {
                                                    AppActionLogger.logAction(context, "NAV", "open_stats")
                                                    isStatsOpen = true
                                                },
                                                isParentPairingEnabled = isParentPairingEnabled,
                                                onNavigateToParentPairing = {
                                                    if (isParentPairingEnabled) {
                                                        AppActionLogger.logAction(context, "NAV", "open_parent_pairing")
                                                        isParentPairingOpen = true
                                                    }
                                                }
                                            )
                                            1 -> TimerHubScreen(viewModel = viewModel)
                                            2 -> StopwatchScreen(viewModel = viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen AMOLED Focus Mode ("Odak Modu"): pure #000000 showing ONLY active counter
        // styled to match the active timer mode + its stats, with zero clickable buttons.
        if (showDarkFocusOverlay && userRole == UserPreferences.ROLE_STUDENT && focusLockedTarget != null) {
            DarkFocusFullscreenOverlay(
                viewModel = viewModel,
                target = focusLockedTarget ?: FocusModeTarget.POMODORO,
                onDismiss = {
                    showDarkFocusOverlay = false
                    darkFocusResetKey++
                    viewModel.clearFocusLock()
                }
            )
        }
    }
}

@Composable
private fun DarkFocusFullscreenOverlay(
    viewModel: StudyViewModel,
    target: FocusModeTarget,
    onDismiss: () -> Unit
) {
    val lang by viewModel.userLanguage.collectAsState()
    val interactionSource = remember { MutableInteractionSource() }
    // 0 = pure black with zero buttons; 1 = Lock button revealed on tap; 2 = Slide-to-unlock slider visible
    var unlockUiStage by remember { mutableIntStateOf(0) }

    LaunchedEffect(unlockUiStage) {
        if (unlockUiStage > 0) {
            delay(8000L)
            unlockUiStage = 0
        }
    }

    BackHandler(enabled = true) {
        if (unlockUiStage == 0) {
            unlockUiStage = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("dark_focus_overlay"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        if (unlockUiStage == 0) {
                            unlockUiStage = 1
                        }
                    }
                )
        )
        when (target) {
            FocusModeTarget.TIMER -> {
                val remainingSecs by viewModel.timerRemainingSeconds.collectAsState()
                val totalSecs by viewModel.timerTotalSeconds.collectAsState()
                val isDenemeMode by viewModel.isDenemeMode.collectAsState()
                val denemeType by viewModel.denemeType.collectAsState()
                val timerSubject by viewModel.timerSubject.collectAsState()
                val timerStage by viewModel.timerExamStage.collectAsState()

                val progress = if (totalSecs > 0) remainingSecs.toFloat() / totalSecs.toFloat() else 1f
                val elapsedSecs = (totalSecs - remainingSecs).coerceAtLeast(0)
                val pctDone = if (totalSecs > 0) ((elapsedSecs.toFloat() / totalSecs.toFloat()) * 100f).toInt() else 0

                val h = remainingSecs / 3600
                val m = (remainingSecs % 3600) / 60
                val s = remainingSecs % 60
                val timeText = if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                else String.format(Locale.US, "%02d:%02d", m, s)

                val baseLabel = if (isDenemeMode) {
                    TranslationHelper.localizeDenemeOption(denemeType, lang)
                } else {
                    TranslationHelper.localizeSubject(timerSubject, lang)
                }
                val fullLabel = if (timerStage.isNotBlank() && !isDenemeMode) "$baseLabel • $timerStage" else baseLabel
                val ringColor = if (isDenemeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(modifier = Modifier.size(230.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF1F1F1F),
                            strokeWidth = 12.dp
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = ringColor,
                            strokeWidth = 12.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = if (h > 0) 36.sp else 46.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("dark_focus_time_text")
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = fullLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = ringColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    FocusStatPillRow(
                        items = listOf(
                            "${elapsedSecs / 60} ${TranslationHelper.getTranslation("min_short", lang)}" to TranslationHelper.getTranslation("notif_sw_elapsed", lang),
                            "${totalSecs / 60} ${TranslationHelper.getTranslation("min_short", lang)}" to TranslationHelper.getTranslation("timer_duration_label", lang),
                            "%$pctDone" to TranslationHelper.getTranslation("seg_timer_deneme", lang)
                        )
                    )
                }
            }

            FocusModeTarget.POMODORO -> {
                val remainingSecs by viewModel.pomoRemainingSeconds.collectAsState()
                val totalSecs by viewModel.pomoTotalSeconds.collectAsState()
                val pomodoroState by viewModel.pomodoroState.collectAsState()
                val pomoStage by viewModel.pomoExamStage.collectAsState()
                val selectedSubject by viewModel.selectedSubject.collectAsState()
                val completedBlocks by viewModel.currentSessionCompletedBlocks.collectAsState()
                val totalWorkSecs by viewModel.currentSessionTotalWorkSeconds.collectAsState()

                val progress = if (totalSecs > 0) remainingSecs.toFloat() / totalSecs.toFloat() else 1f
                val isBreak = pomodoroState == PomodoroState.BREAKING || pomodoroState == PomodoroState.BREAK_PAUSED
                val ringColor = if (isBreak) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary

                val h = remainingSecs / 3600
                val m = (remainingSecs % 3600) / 60
                val s = remainingSecs % 60
                val timeText = if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
                else String.format(Locale.US, "%02d:%02d", m, s)

                val stateKey = when (pomodoroState) {
                    PomodoroState.IDLE -> "pomo_state_idle"
                    PomodoroState.WORKING -> "pomo_state_working"
                    PomodoroState.WORK_PAUSED -> "pomo_state_paused"
                    PomodoroState.BREAKING -> "pomo_state_breaking"
                    PomodoroState.BREAK_PAUSED -> "pomo_state_break_paused"
                    PomodoroState.BLOCK_FINISHED_PROMPT -> "pomo_state_block_done"
                    PomodoroState.LONG_BREAK_PROMPT -> "pomo_state_long_break"
                    PomodoroState.SESSION_FINISHED_DAILY_STATS -> "pomo_state_finished"
                }
                val stageSuffix = when {
                    pomoStage.equals("KARISIK", ignoreCase = true) -> " • ${TranslationHelper.getTranslation("stage_karisik", lang)}"
                    pomoStage.isNotBlank() -> " • $pomoStage"
                    else -> ""
                }
                val subjectDisplay = TranslationHelper.localizeSubject(selectedSubject, lang)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF1F1F1F),
                            strokeWidth = 10.dp
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = ringColor,
                            strokeWidth = 10.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = timeText,
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontWeight = FontWeight.Light,
                                    fontSize = if (h > 0) 36.sp else 46.sp
                                ),
                                color = Color.White,
                                modifier = Modifier.testTag("dark_focus_time_text")
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TranslationHelper.getTranslation(stateKey, lang) + stageSuffix,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = ringColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = subjectDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FocusStatPillRow(
                        items = listOf(
                            "$completedBlocks" to TranslationHelper.getTranslation("badge_pomodoro", lang),
                            "${totalWorkSecs / 60} ${TranslationHelper.getTranslation("min_short", lang)}" to TranslationHelper.getTranslation("stats_today_total", lang)
                        )
                    )
                }
            }

            FocusModeTarget.STOPWATCH -> {
                val elapsedMillis by viewModel.stopwatchElapsedMillis.collectAsState()
                val isRunning by viewModel.stopwatchIsRunning.collectAsState()
                val subject by viewModel.stopwatchSubject.collectAsState()
                val stage by viewModel.stopwatchExamStage.collectAsState()
                val laps by viewModel.stopwatchLaps.collectAsState()

                val totalSeconds = elapsedMillis / 1000
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val seconds = totalSeconds % 60
                val millis = (elapsedMillis % 1000) / 10
                val timeText = if (hours > 0) {
                    String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis)
                }
                val badgeText = when {
                    stage.equals("KARISIK", ignoreCase = true) -> TranslationHelper.getTranslation("stage_karisik", lang)
                    stage.isNotBlank() -> "$stage • ${TranslationHelper.localizeSubject(subject, lang)}"
                    else -> TranslationHelper.localizeSubject(subject, lang)
                }
                val lastLapStr = laps.lastOrNull()?.let { lap ->
                    val ls = lap.lapTimeMillis / 1000
                    String.format(Locale.US, "+%02d:%02d", ls / 60, ls % 60)
                } ?: "--:--"

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF0E0E0E))
                            .padding(horizontal = 32.dp, vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Light,
                                fontSize = if (hours > 0) 38.sp else 48.sp
                            ),
                            color = Color.White,
                            modifier = Modifier.testTag("dark_focus_time_text")
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = TranslationHelper.getTranslation(
                            if (isRunning) "notif_sw_running" else "notif_sw_paused",
                            lang
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    FocusStatPillRow(
                        items = listOf(
                            "${laps.size}" to TranslationHelper.getTranslation("stopwatch_laps_title", lang),
                            lastLapStr to TranslationHelper.getTranslation("stopwatch_lap", lang)
                        )
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = unlockUiStage == 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Button(
                onClick = { unlockUiStage = 2 },
                modifier = Modifier
                    .height(48.dp)
                    .testTag("dark_focus_unlock_btn"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1A1A20),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = TranslationHelper.getTranslation("focus_unlock_tap_btn", lang),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TranslationHelper.getTranslation("focus_unlock_tap_btn", lang),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }

        AnimatedVisibility(
            visible = unlockUiStage == 2,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            FocusSlideToUnlockBar(
                labelText = TranslationHelper.getTranslation("focus_slide_to_unlock", lang),
                onUnlocked = onDismiss
            )
        }
    }
}

@Composable
private fun FocusSlideToUnlockBar(
    labelText: String,
    onUnlocked: () -> Unit
) {
    val thumbSize = 48.dp
    val horizontalPad = 4.dp
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp)
            .height(56.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0xFF141418))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), RoundedCornerShape(50))
            .padding(horizontal = horizontalPad)
            .testTag("dark_focus_unlock_slider"),
        contentAlignment = Alignment.CenterStart
    ) {
        val maxDragPx = with(density) {
            (maxWidth - (horizontalPad * 2) - thumbSize).toPx().coerceAtLeast(1f)
        }
        var dragOffsetPx by remember { mutableFloatStateOf(0f) }
        var unlockedTriggered by remember { mutableStateOf(false) }

        val progress = (dragOffsetPx / maxDragPx).coerceIn(0f, 1f)

        Text(
            text = labelText,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = (0.85f * (1f - progress * 1.15f)).coerceIn(0f, 0.85f)),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = thumbSize)
                .alpha((1f - progress).coerceIn(0f, 1f))
        )

        val dragState = rememberDraggableState { delta ->
            if (!unlockedTriggered) {
                val next = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
                dragOffsetPx = next
                if (next >= maxDragPx * 0.82f) {
                    unlockedTriggered = true
                    onUnlocked()
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(dragOffsetPx.roundToInt(), 0) }
                .size(thumbSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (!unlockedTriggered) {
                            if (dragOffsetPx >= maxDragPx * 0.82f) {
                                unlockedTriggered = true
                                onUnlocked()
                            } else {
                                dragOffsetPx = 0f
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (progress > 0.55f) Icons.Outlined.LockOpen else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = labelText,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FocusStatPillRow(items: List<Pair<String, String>>) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEach { (value, label) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF121212))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }
    }
}
