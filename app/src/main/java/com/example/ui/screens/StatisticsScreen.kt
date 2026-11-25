package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun StatisticsScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit = {}
) {
    BackHandler { onBack() }

    val lang by viewModel.userLanguage.collectAsStateWithLifecycle()
    val userTrack by viewModel.userTrack.collectAsStateWithLifecycle()
    val examTarget by viewModel.examTarget.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val allNotes by viewModel.allNotes.collectAsStateWithLifecycle()
    val allTimeStats by viewModel.dashboardStats.collectAsStateWithLifecycle()

    var showAddPastSessionDialog by remember { mutableStateOf(false) }
    var selectedStageFilter by remember { mutableStateOf("ALL") }
    var isNotesExpanded by remember { mutableStateOf(false) }

    val hrS = TranslationHelper.getTranslation("hr_short", lang)
    val minS = TranslationHelper.getTranslation("min_short", lang)

    fun formatMinutes(mins: Int): String {
        if (mins <= 0) return "0 $minS"
        val h = mins / 60
        val m = mins % 60
        return when {
            h > 0 && m > 0 -> "$h $hrS $m $minS"
            h > 0 -> "$h $hrS"
            else -> "$m $minS"
        }
    }

    val totalPomodoros = remember(allSessions) {
        allSessions.filter { it.sessionType == "POMODORO" }.sumOf { it.blocksCompleted }
    }

    val filteredTimeStats = remember(allTimeStats.timeBySubjectAndStage, selectedStageFilter) {
        if (selectedStageFilter == "ALL") {
            allTimeStats.timeBySubjectAndStage
        } else {
            allTimeStats.timeBySubjectAndStage.filter { it.examStage == selectedStageFilter }
        }
    }
    val filteredTotalSessionSeconds = remember(filteredTimeStats) {
        filteredTimeStats.sumOf { it.totalSeconds }
    }

    val filteredPracticeStats = remember(allTimeStats.practiceBySubjectAndStage, selectedStageFilter) {
        if (selectedStageFilter == "ALL") {
            allTimeStats.practiceBySubjectAndStage
        } else {
            allTimeStats.practiceBySubjectAndStage.filter { it.examStage == selectedStageFilter }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .testTag("detailed_statistics_screen"),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item(key = "stats_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("stats_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = TranslationHelper.getTranslation("back", lang),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = TranslationHelper.getTranslation("stats_detailed_title", lang),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = TranslationHelper.getTranslation("stats_all_time_badge", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { showAddPastSessionDialog = true },
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("stats_add_past_btn", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // All-Time Primary KPI Cards
            item(key = "stats_kpi_row_1") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.AvTimer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatMinutes(allTimeStats.totalAllTimeMinutes),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = TranslationHelper.getTranslation("stats_total_time", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "$totalPomodoros ${TranslationHelper.getTranslation("badge_pomodoro", lang)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${allTimeStats.activeDaysCount} ${TranslationHelper.getTranslation("stats_active_days", lang)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Secondary All-Time KPIs (Daily Average, Tests, Reviewed Topics, Denemes)
            item(key = "stats_kpi_row_2") {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DetailMiniStatBox(
                            label = TranslationHelper.getTranslation("stats_avg_daily", lang),
                            value = formatMinutes(allTimeStats.averageDailyMinutes),
                            modifier = Modifier.weight(1f)
                        )
                        DetailMiniStatBox(
                            label = TranslationHelper.getTranslation("stats_total_tests", lang),
                            value = "${allTimeStats.totalTestsSolved}",
                            modifier = Modifier.weight(1f)
                        )
                        DetailMiniStatBox(
                            label = TranslationHelper.getTranslation("stats_topics_reviewed", lang),
                            value = "${allTimeStats.totalTopicsReviewed}",
                            modifier = Modifier.weight(1f)
                        )
                        DetailMiniStatBox(
                            label = TranslationHelper.getTranslation("stats_total_denemes", lang),
                            value = "${allTimeStats.totalDenemesSolved}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = TranslationHelper.getTranslation("stats_avg_zero_hint", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // TYT / AYT / YDT Stage Breakdown Card & Filter Chips
            if (examTarget == "YKS" || allTimeStats.stageStats.isNotEmpty()) {
                item(key = "stats_stage_breakdown_card") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stats_stage_breakdown_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = TranslationHelper.getTranslation("stats_stage_breakdown_title", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            val stageOptions = remember(userTrack, lang) {
                                val second = if (userTrack.equals("Dil", ignoreCase = true)) "YDT" else "AYT"
                                listOf(
                                    "ALL" to TranslationHelper.getTranslation("stats_filter_all", lang),
                                    "TYT" to "TYT",
                                    second to second,
                                    "GENEL" to TranslationHelper.getTranslation("stage_general", lang)
                                )
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(stageOptions, key = { it.first }) { (code, label) ->
                                    val isSel = selectedStageFilter == code
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .clickable { selectedStageFilter = code }
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                            .testTag("stats_stage_filter_$code")
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            if (allTimeStats.stageStats.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                allTimeStats.stageStats.forEach { st ->
                                    val stLabel = if (st.stage == "GENEL") TranslationHelper.getTranslation("stage_general", lang) else st.stage
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    when (st.stage) {
                                                        "TYT" -> MaterialTheme.colorScheme.primaryContainer
                                                        "AYT", "YDT" -> MaterialTheme.colorScheme.tertiaryContainer
                                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                                    }
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = stLabel,
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = when (st.stage) {
                                                    "TYT" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                    "AYT", "YDT" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                    else -> MaterialTheme.colorScheme.onSecondaryContainer
                                                }
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = formatMinutes(st.totalMinutes),
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${st.testsSolved} ${TranslationHelper.getTranslation("agenda_test_unit", lang)} • ${st.topicsReviewed} ${TranslationHelper.getTranslation("agenda_type_review", lang)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Subject Time Distribution Card
            item(key = "stats_subject_time_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stats_subject_time_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = TranslationHelper.getTranslation("stats_subject_distribution", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (filteredTimeStats.isEmpty()) {
                            Text(
                                text = TranslationHelper.getTranslation("stats_no_sessions_yet", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxSecs = filteredTimeStats.maxOf { it.totalSeconds }.toFloat().coerceAtLeast(1f)
                            filteredTimeStats.forEach { stat ->
                                val mins = stat.totalSeconds / 60
                                val pct = if (filteredTotalSessionSeconds > 0) {
                                    stat.totalSeconds.toFloat() / filteredTotalSessionSeconds.toFloat()
                                } else 0f
                                val stageBadge = if (stat.examStage == "GENEL") {
                                    TranslationHelper.getTranslation("stage_general", lang)
                                } else stat.examStage

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        when (stat.examStage) {
                                                            "TYT" -> MaterialTheme.colorScheme.primaryContainer
                                                            "AYT", "YDT" -> MaterialTheme.colorScheme.tertiaryContainer
                                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = stageBadge,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                    color = when (stat.examStage) {
                                                        "TYT" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                        "AYT", "YDT" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = TranslationHelper.localizeSubject(stat.subject, lang),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "$mins $minS (%${(pct * 100).toInt()})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { (stat.totalSeconds.toFloat() / maxSecs).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // All-Time Question Practice by Subject Card (TYT/AYT separated)
            item(key = "stats_subject_practice_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Quiz,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = TranslationHelper.getTranslation("stats_practice_by_subject", lang),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredPracticeStats.isEmpty()) {
                            Text(
                                text = TranslationHelper.getTranslation("stats_no_practice_yet", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxTests = filteredPracticeStats.maxOf { it.totalTests }.coerceAtLeast(1)
                            filteredPracticeStats.forEach { subjStat ->
                                val ratio = (subjStat.totalTests.toFloat() / maxTests.toFloat()).coerceIn(0.05f, 1f)
                                val stLabel = if (subjStat.examStage == "GENEL") {
                                    TranslationHelper.getTranslation("stage_general", lang)
                                } else subjStat.examStage
                                Column(modifier = Modifier.padding(vertical = 5.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (stLabel.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(
                                                            when (subjStat.examStage) {
                                                                "TYT" -> MaterialTheme.colorScheme.primaryContainer
                                                                "AYT", "YDT" -> MaterialTheme.colorScheme.tertiaryContainer
                                                                else -> MaterialTheme.colorScheme.surfaceVariant
                                                            }
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                                        color = when (subjStat.examStage) {
                                                            "TYT" -> MaterialTheme.colorScheme.onPrimaryContainer
                                                            "AYT", "YDT" -> MaterialTheme.colorScheme.onTertiaryContainer
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        }
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(
                                                text = TranslationHelper.localizeSubject(subjStat.subject, lang),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = String.format(
                                                Locale.US,
                                                TranslationHelper.getTranslation("stats_tests_fmt", lang),
                                                subjStat.totalTests
                                            ),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { ratio },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = MaterialTheme.colorScheme.tertiary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Sessions History Header
            item(key = "stats_sessions_header") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = TranslationHelper.getTranslation("stats_session_history", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (allSessions.isEmpty()) {
                item(key = "stats_sessions_empty") {
                    Text(
                        text = TranslationHelper.getTranslation("stats_no_sessions_yet", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(allSessions, key = { "sess_${it.id}_${it.timestamp}" }) { session ->
                    val mins = session.durationSeconds / 60
                    val dateLoc = if (lang == "TR") Locale("tr", "TR") else Locale.ENGLISH
                    val formattedDate = SimpleDateFormat("dd MMM yyyy - HH:mm", dateLoc).format(Date(session.timestamp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (session.sessionType == "POMODORO") MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = session.sessionType,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (session.sessionType == "POMODORO") MaterialTheme.colorScheme.onPrimaryContainer
                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = TranslationHelper.localizeSubject(session.subject, lang),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val stageSuffix = if (session.examStage.isNotBlank()) " • ${session.examStage}" else ""
                                Text(
                                    text = "$formattedDate • $mins $minS$stageSuffix",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(onClick = { viewModel.deleteSession(session) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = TranslationHelper.getTranslation("delete", lang),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }

            // Daily Notes Collapsible Header ("Paydos & Gün Sonu Notları")
            item(key = "stats_notes_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isNotesExpanded = !isNotesExpanded }
                        .testTag("stats_notes_toggle"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${TranslationHelper.getTranslation("stats_notes_title", lang)} (${allNotes.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            imageVector = if (isNotesExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = TranslationHelper.getTranslation("stats_notes_title", lang),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isNotesExpanded) {
                if (allNotes.isEmpty()) {
                    item(key = "stats_notes_empty") {
                        Text(
                            text = TranslationHelper.getTranslation("stats_no_notes_yet", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(allNotes, key = { "note_${it.dateStr}" }) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = note.dateStr,
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (note.mood.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = TranslationHelper.localizeMood(note.mood, lang),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                    }
                                    if (note.noteText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = note.noteText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(onClick = { viewModel.deleteNote(note) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = TranslationHelper.getTranslation("delete", lang),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPastSessionDialog) {
        var subjectInput by remember { mutableStateOf("Matematik") }
        var durationInput by remember { mutableStateOf("30") }
        val context = LocalContext.current
        val todayStr = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
        }
        var dateStrInput by remember { mutableStateOf(todayStr) }

        AlertDialog(
            onDismissRequest = { showAddPastSessionDialog = false },
            title = { Text(TranslationHelper.getTranslation("stats_add_past_title", lang)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        label = { Text(TranslationHelper.getTranslation("agenda_subject_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = durationInput,
                        onValueChange = { durationInput = it.filter { c -> c.isDigit() } },
                        label = { Text(TranslationHelper.getTranslation("stats_duration_min_label", lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dateStrInput,
                        onValueChange = { dateStrInput = it },
                        label = { Text(TranslationHelper.getTranslation("stats_date_format_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val dur = durationInput.toLongOrNull() ?: 0L
                        if (subjectInput.isNotBlank() && dur > 0) {
                            viewModel.insertPastSession(subjectInput.trim(), dur, dateStrInput)
                            showAddPastSessionDialog = false
                        } else {
                            Toast.makeText(
                                context,
                                TranslationHelper.getTranslation("stats_past_invalid_warn", lang),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(TranslationHelper.getTranslation("add", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPastSessionDialog = false }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }
}

@Composable
private fun DetailMiniStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
