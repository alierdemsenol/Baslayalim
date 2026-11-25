package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaSubjectBullet
import com.example.data.preferences.UserPreferences
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.LocalSyncHelper
import com.example.util.TranslationHelper
import com.example.widget.ExamWidgetUpdater
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParentDashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToSettings: () -> Unit
) {
    val lang by viewModel.userLanguage.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val deviceUniqueCode by viewModel.deviceUniqueCode.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val userTrack by viewModel.userTrack.collectAsState()
    val tytDays by viewModel.tytDaysRemaining.collectAsState()
    val aytDays by viewModel.aytDaysRemaining.collectAsState()
    val lgsDays by viewModel.lgsDaysRemaining.collectAsState()
    val kpssDays by viewModel.kpssDaysRemaining.collectAsState()
    val msuDays by viewModel.msuDaysRemaining.collectAsState()
    val currentSyncKey by viewModel.syncKey.collectAsState()

    val parentSnapshot by viewModel.parentSnapshot.collectAsState()
    val parentStats by viewModel.parentStats.collectAsState()
    val parentWeeklyStats by viewModel.parentWeeklyStats.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val lanTransferStatus by viewModel.lanTransferStatus.collectAsState()

    val context = LocalContext.current
    val myHardware = remember { LocalSyncHelper.getDeviceBrandAndModel() }
    var showMenu by remember { mutableStateOf(false) }
    var showLegacyDialog by remember { mutableStateOf(false) }
    var selectedStageFilter by remember { mutableStateOf("ALL") }
    var isAgendaExpanded by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        viewModel.startLanDiscovery(mode = "PARENT_PAIRING")
        onDispose {
            viewModel.stopLanDiscovery()
        }
    }

    val hrS = TranslationHelper.getTranslation("hr_short", lang)
    val minS = TranslationHelper.getTranslation("min_short", lang)
    fun formatMins(mins: Int): String {
        if (mins <= 0) return "0 $minS"
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "$h $hrS $m $minS"
            h > 0 -> "$h $hrS"
            else -> "$m $minS"
        }
    }

    val snap = parentSnapshot
    val activeTarget = snap?.examTarget ?: examTarget
    val activeTrack = snap?.userTrack ?: userTrack

    val countdownItems = remember(
        snap, activeTarget, activeTrack, tytDays, aytDays, lgsDays, kpssDays, msuDays, lang
    ) {
        val now = System.currentTimeMillis()
        fun daysFor(ts: Long, fallback: Long): Long {
            return if (ts > 0L) ((ts - now).coerceAtLeast(0L) / (24L * 3600L * 1000L)) else fallback
        }
        val list = mutableListOf<Pair<String, Long>>()
        val isTr = lang == "TR"
        when (activeTarget) {
            "LGS" -> list.add((if (isTr) "LGS 2027" else "LGS 2027 Exam") to daysFor(snap?.customLgsExamDate ?: 0L, lgsDays))
            "KPSS" -> list.add((if (isTr) "KPSS Lisans 2027" else "KPSS 2027 Exam") to daysFor(snap?.customKpssExamDate ?: 0L, kpssDays))
            else -> {
                list.add("YKS (TYT) 2027" to daysFor(snap?.customTytExamDate ?: 0L, tytDays))
                list.add("YKS (AYT) 2027" to daysFor(snap?.customAytExamDate ?: 0L, aytDays))
                if (activeTrack.trim().equals("Dil", ignoreCase = true)) {
                    list.add("YKS (YDT) 2027" to daysFor(snap?.customAytExamDate ?: 0L, aytDays))
                }
                list.add("MSÜ 2027" to daysFor(snap?.customMsuExamDate ?: 0L, msuDays))
            }
        }
        snap?.customTimers?.forEach { t ->
            list.add(t.title to daysFor(t.targetTimestamp, 0L))
        }
        list
    }
    var examIdx by remember { mutableIntStateOf(0) }
    val safeExamIdx = if (countdownItems.isNotEmpty()) examIdx.coerceIn(0, countdownItems.lastIndex) else 0
    val currentExam = countdownItems.getOrElse(safeExamIdx) { "YKS (TYT) 2027" to tytDays }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("parent_dashboard_screen"),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item(key = "pd_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${TranslationHelper.getTranslation("welcome_user", lang)}, ${userName.ifBlank { TranslationHelper.getTranslation("role_parent", lang) }}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = TranslationHelper.getTranslation("parent_panel_title", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("parent_more_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = TranslationHelper.getTranslation("more_options", lang),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(TranslationHelper.getTranslation("legacy_sync_menu_item", lang)) },
                                leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                onClick = {
                                    showMenu = false
                                    showLegacyDialog = true
                                },
                                modifier = Modifier.testTag("parent_legacy_sync_menu")
                            )
                            DropdownMenuItem(
                                text = { Text(TranslationHelper.getTranslation("settings_title", lang)) },
                                leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                },
                                modifier = Modifier.testTag("parent_settings_menu")
                            )
                        }
                    }
                }
            }

            // Exam Countdowns Ticker (parent can always view countdowns + MSÜ Şafak rule)
            item(key = "pd_countdown") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("parent_countdown_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                examIdx = if (safeExamIdx > 0) safeExamIdx - 1 else countdownItems.lastIndex
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = TranslationHelper.getTranslation("prev_exam", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Whatshot,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ExamWidgetUpdater.formatCountdownText(
                                        examTitleOrKey = currentExam.first,
                                        daysRemaining = currentExam.second,
                                        isTr = lang == "TR"
                                    ),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = "${currentExam.first} • ${safeExamIdx + 1}/${countdownItems.size}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(
                            onClick = {
                                examIdx = if (safeExamIdx < countdownItems.lastIndex) safeExamIdx + 1 else 0
                            },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = TranslationHelper.getTranslation("next_exam", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Wi-Fi Peer Presence & Listener Status Card
            item(key = "pd_wifi_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Wifi,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "$myHardware • #$deviceUniqueCode",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = TranslationHelper.getTranslation("parent_wifi_listen_active", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        if (lanTransferStatus.isNotBlank()) {
                            Text(
                                text = lanTransferStatus,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (discoveredPeers.isNotEmpty()) {
                            Text(
                                text = TranslationHelper.getTranslation("nearby_devices_title", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            discoveredPeers.forEach { peer ->
                                val badge = if (peer.role == UserPreferences.ROLE_PARENT) {
                                    TranslationHelper.getTranslation("role_badge_parent", lang)
                                } else {
                                    TranslationHelper.getTranslation("role_badge_student", lang)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = peer.compactSummary(),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (snap == null) {
                item(key = "pd_no_data") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_empty_snapshot_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SupervisorAccount,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = TranslationHelper.getTranslation("parent_no_data_yet", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = TranslationHelper.getTranslation("parent_no_data_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Student Summary Card
                item(key = "pd_student_summary") {
                    val loc = if (lang == "TR") Locale("tr", "TR") else Locale.ENGLISH
                    val syncTimeText = if (snap.syncedAtMillis > 0L) {
                        SimpleDateFormat("dd MMM yyyy • HH:mm", loc).format(Date(snap.syncedAtMillis))
                    } else "-"
                    val trackInfo = if (snap.examTarget == "YKS") {
                        "${snap.examTarget} • ${TranslationHelper.localizeTrack(snap.userTrack, lang)}"
                    } else snap.examTarget
                    val studentTitle = snap.studentName.ifBlank { TranslationHelper.getTranslation("role_student", lang) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("parent_student_summary_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = studentTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = trackInfo,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (snap.studentBrand.isNotBlank() || snap.studentDeviceCode.isNotBlank()) {
                                    Text(
                                        text = snap.compactDeviceInfo(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Text(
                                text = String.format(
                                    Locale.US,
                                    TranslationHelper.getTranslation("parent_last_sync_fmt", lang),
                                    syncTimeText
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                ParentMiniKpi(
                                    label = TranslationHelper.getTranslation("stats_weekly_badge", lang),
                                    value = formatMins(parentWeeklyStats.totalAllTimeMinutes),
                                    modifier = Modifier.weight(1f)
                                )
                                ParentMiniKpi(
                                    label = TranslationHelper.getTranslation("stats_total_time", lang),
                                    value = formatMins(parentStats.totalAllTimeMinutes),
                                    modifier = Modifier.weight(1f)
                                )
                                ParentMiniKpi(
                                    label = TranslationHelper.getTranslation("stats_total_tests", lang),
                                    value = "${parentStats.totalTestsSolved}",
                                    modifier = Modifier.weight(1f)
                                )
                                ParentMiniKpi(
                                    label = TranslationHelper.getTranslation("stats_total_denemes", lang),
                                    value = "${parentStats.totalDenemesSolved}",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                // TYT / AYT / YDT Stage Breakdown & Filter
                item(key = "pd_stage_breakdown") {
                    val stageOptions = remember(activeTrack, lang) {
                        val second = if (activeTrack.equals("Dil", ignoreCase = true)) "YDT" else "AYT"
                        listOf(
                            "ALL" to TranslationHelper.getTranslation("stats_filter_all", lang),
                            "TYT" to "TYT",
                            second to second,
                            "GENEL" to TranslationHelper.getTranslation("stage_general", lang)
                        )
                    }
                    val filteredTime = remember(parentStats.timeBySubjectAndStage, selectedStageFilter) {
                        if (selectedStageFilter == "ALL") parentStats.timeBySubjectAndStage
                        else parentStats.timeBySubjectAndStage.filter { it.examStage == selectedStageFilter }
                    }
                    val filteredPractice = remember(parentStats.practiceBySubjectAndStage, selectedStageFilter) {
                        if (selectedStageFilter == "ALL") parentStats.practiceBySubjectAndStage
                        else parentStats.practiceBySubjectAndStage.filter { it.examStage == selectedStageFilter }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("stats_stage_breakdown_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stageOptions, key = { it.first }) { (code, label) ->
                                    val isSel = selectedStageFilter == code
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { selectedStageFilter = code }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (filteredTime.isNotEmpty()) {
                                val maxSec = filteredTime.maxOf { it.totalSeconds }.toFloat().coerceAtLeast(1f)
                                filteredTime.take(8).forEach { st ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "[${st.examStage}] ${TranslationHelper.localizeSubject(st.subject, lang)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatMins((st.totalSeconds / 60L).toInt()),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (st.totalSeconds / maxSec).coerceIn(0.04f, 1f) },
                                        modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                                    )
                                }
                            }
                            if (filteredPractice.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("stats_practice_by_subject", lang),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                filteredPractice.take(8).forEach { ps ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "[${ps.examStage}] ${TranslationHelper.localizeSubject(ps.subject, lang)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${ps.totalTests} ${TranslationHelper.getTranslation("agenda_test_unit", lang)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Deneme Results & Nets
                val denemeBullets = snap.dailyNotes.flatMap { note ->
                    note.getSubjectBullets()
                        .filter { it.studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME && it.totalQuestions > 0 }
                        .map { note.dateStr to it }
                }
                if (denemeBullets.isNotEmpty()) {
                    item(key = "pd_denemes") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("parent_deneme_results", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                                denemeBullets.take(12).forEach { (dateStr, b) ->
                                    val netStr = String.format(Locale.US, "%.2f", b.computeNet())
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "$dateStr • ${b.trialType.ifBlank { b.examStage }} • ${TranslationHelper.localizeSubject(b.subject, lang)}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "${b.totalQuestions}S • ${b.correctCount}D / ${b.wrongCount}Y / ${b.blankCount}B",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "$netStr Net",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Read-only Student Agenda Entries (Expandable)
                item(key = "pd_agenda_toggle") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { isAgendaExpanded = !isAgendaExpanded },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("parent_student_agenda", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Icon(
                                imageVector = if (isAgendaExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (isAgendaExpanded) {
                    itemsIndexed(snap.dailyNotes, key = { idx, n -> "pn_${idx}_${n.dateStr}" }) { _, note ->
                        val bullets = remember(note.subjectBulletsJson) { note.getSubjectBullets() }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = note.dateStr,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (note.studyDurationMinutes > 0) {
                                        Text(
                                            text = formatMins(note.studyDurationMinutes),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                                bullets.forEach { b ->
                                    val st = if (b.examStage.isNotBlank()) "[${b.examStage}] " else ""
                                    val bk = if (b.sourceBook.isNotBlank()) " • ${b.sourceBook}" else ""
                                    Text(
                                        text = "• $st${TranslationHelper.localizeSubject(b.subject, lang)}: ${b.topic}$bk",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                if (note.noteText.isNotBlank()) {
                                    Text(
                                        text = note.noteText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLegacyDialog) {
        ParentLegacyWifiModal(
            lang = lang,
            currentSyncKey = currentSyncKey,
            onRefreshKey = {
                viewModel.updateSyncKey(LocalSyncHelper.generate4DigitKey())
            },
            onDismiss = { showLegacyDialog = false },
            onPullFromStudent = { ip, pin ->
                viewModel.syncWithPeerAuto(ip)
            },
            onConnectWithPin = { ip, pin, onResult ->
                LocalSyncHelper.connectAndSyncClient(context, ip, pin) { status ->
                    onResult(status)
                }
            },
            onRefreshPrefs = { viewModel.refreshPreferencesState() }
        )
    }
}

@Composable
private fun ParentMiniKpi(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ParentLegacyWifiModal(
    lang: String,
    currentSyncKey: String,
    onRefreshKey: () -> Unit,
    onDismiss: () -> Unit,
    onPullFromStudent: (String, String) -> Unit,
    onConnectWithPin: suspend (String, String, (String) -> Unit) -> Unit,
    onRefreshPrefs: () -> Unit
) {
    val localIp = remember { LocalSyncHelper.getLocalIpAddress() }
    var ipInput by remember { mutableStateOf(localIp) }
    var pinInput by remember { mutableStateOf("") }
    var statusText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    BackHandler { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = TranslationHelper.getTranslation("legacy_sync_menu_item", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(TranslationHelper.getTranslation("sync_key_label", lang), style = MaterialTheme.typography.labelSmall)
                        Text(
                            text = currentSyncKey,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text("${TranslationHelper.getTranslation("ip_label", lang)} $localIp", style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(onClick = onRefreshKey) {
                        Text(TranslationHelper.getTranslation("refresh_key", lang))
                    }
                }
                OutlinedTextField(
                    value = ipInput,
                    onValueChange = { ipInput = it },
                    label = { Text(TranslationHelper.getTranslation("ip_hint", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { pinInput = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(TranslationHelper.getTranslation("key_hint", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (statusText.isNotBlank()) {
                    Text(statusText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(TranslationHelper.getTranslation("close", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = ipInput.isNotBlank() && pinInput.length == 4,
                        onClick = {
                            scope.launch {
                                onConnectWithPin(ipInput.trim(), pinInput.trim()) { st ->
                                    statusText = st
                                    onRefreshPrefs()
                                    Toast.makeText(context, st, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    ) {
                        Text(TranslationHelper.getTranslation("pair_sync_btn", lang))
                    }
                }
            }
        }
    }
}
