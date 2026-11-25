package com.example.ui.screens

import android.Manifest
import android.app.Activity
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AvTimer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.preferences.UserPreferences
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
    val isAmoledModeEnabled by viewModel.isAmoledModeEnabled.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val timerIsRunning by viewModel.timerIsRunning.collectAsState()

    var selectedTabItem by remember { mutableIntStateOf(0) }
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

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { bottomTabs.size })
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
            pagerState.scrollToPage(2)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage in bottomTabs.indices) {
            selectedTabItem = pagerState.currentPage
        }
    }

    val isCountdownActive = (
        pomodoroState == PomodoroState.WORKING ||
            pomodoroState == PomodoroState.BREAKING ||
            timerIsRunning
        ) && selectedTabItem == 1 && !isSettingsOpen && !isAgendaOpen && !isStatsOpen && !isParentPairingOpen &&
        isOnboarded && userRole == UserPreferences.ROLE_STUDENT

    LaunchedEffect(isAmoledModeEnabled, isCountdownActive, darkFocusResetKey) {
        if (isAmoledModeEnabled && isCountdownActive) {
            delay(15000L)
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

    val activity = context as? Activity
    DisposableEffect(showDarkFocusOverlay) {
        val window = activity?.window
        if (window != null) {
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
            BackHandler(enabled = showDarkFocusOverlay || isSettingsOpen || isAgendaOpen || isStatsOpen || isParentPairingOpen) {
                when {
                    showDarkFocusOverlay -> {
                        showDarkFocusOverlay = false
                        darkFocusResetKey++
                    }
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

        // Fullscreen Dark Focus Mode ("Koyu Odak Modu"): hides NavigationBar/Rail & tabs completely,
        // centered on pure #000000 showing ONLY remaining time in exact normal countdown typography.
        if (showDarkFocusOverlay && userRole == UserPreferences.ROLE_STUDENT) {
            DarkFocusFullscreenOverlay(
                viewModel = viewModel,
                isPomodoroRunning = (pomodoroState == PomodoroState.WORKING || pomodoroState == PomodoroState.BREAKING),
                onDismiss = {
                    showDarkFocusOverlay = false
                    darkFocusResetKey++
                }
            )
        }
    }
}

@Composable
private fun DarkFocusFullscreenOverlay(
    viewModel: StudyViewModel,
    isPomodoroRunning: Boolean,
    onDismiss: () -> Unit
) {
    val pomoRemaining by viewModel.pomoRemainingSeconds.collectAsState()
    val timerRemaining by viewModel.timerRemainingSeconds.collectAsState()
    val remainingSecs = if (isPomodoroRunning) pomoRemaining else timerRemaining

    val h = remainingSecs / 3600
    val m = (remainingSecs % 3600) / 60
    val s = remainingSecs % 60
    val timeText = if (h > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.US, "%02d:%02d", m, s)
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onDismiss
                )
                .testTag("dark_focus_overlay")
        )
        Text(
            text = timeText,
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.Light,
                fontSize = if (h > 0) 36.sp else 46.sp
            ),
            color = Color.White,
            modifier = Modifier.testTag("dark_focus_time_text")
        )
    }
}
