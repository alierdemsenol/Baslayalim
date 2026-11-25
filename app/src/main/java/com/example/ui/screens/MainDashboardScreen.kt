package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaSubjectBullet
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import com.example.widget.ExamWidgetUpdater
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainDashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToAgenda: () -> Unit,
    onNavigateToStats: () -> Unit = {},
    isParentPairingEnabled: Boolean = false,
    onNavigateToParentPairing: () -> Unit = {}
) {
    val lang by viewModel.userLanguage.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val todayQuote by viewModel.todayQuote.collectAsState()
    val usageDaysCount by viewModel.usageDaysCount.collectAsState()
    val toastEvent by viewModel.toastEvent.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(toastEvent) {
        toastEvent?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastEvent()
        }
    }

    // Lightweight day-level StateFlows (ZERO per-second recomposition!)
    val tytDays by viewModel.tytDaysRemaining.collectAsState()
    val aytDays by viewModel.aytDaysRemaining.collectAsState()
    val lgsDays by viewModel.lgsDaysRemaining.collectAsState()
    val kpssDays by viewModel.kpssDaysRemaining.collectAsState()
    val msuDays by viewModel.msuDaysRemaining.collectAsState()
    val customTimers by viewModel.customTimers.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val weeklyDashboardStats by viewModel.weeklyDashboardStats.collectAsState()

    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCalculatorDialog by remember { mutableStateOf(false) }

    val latestAgendaPreview = remember(allNotes, lang) {
        val isTr = lang == "TR"
        val latestNote = allNotes
            .sortedByDescending { it.dateStr }
            .firstOrNull { n ->
                n.getSubjectBullets().isNotEmpty() || n.noteText.isNotBlank() || n.studyDurationMinutes > 0
            }
        if (latestNote == null) {
            null
        } else {
            val bullets = latestNote.getSubjectBullets()
            val lastBullet = bullets.lastOrNull()
            when {
                lastBullet != null -> {
                    val stagePrefix = if (lastBullet.examStage.isNotBlank()) "[${lastBullet.examStage}] " else ""
                    val subj = TranslationHelper.localizeSubject(lastBullet.subject, lang)
                    val suffix = when (lastBullet.studyType) {
                        AgendaSubjectBullet.STUDY_TYPE_PRACTICE ->
                            " (${lastBullet.testCount.coerceAtLeast(1)} ${TranslationHelper.getTranslation("agenda_test_unit", lang)})"
                        AgendaSubjectBullet.STUDY_TYPE_DENEME ->
                            if (lastBullet.totalQuestions > 0) " (${String.format(Locale.US, "%.2f", lastBullet.computeNet())} Net)" else ""
                        else -> ""
                    }
                    "$stagePrefix$subj: ${lastBullet.topic}$suffix"
                }
                latestNote.noteText.isNotBlank() -> latestNote.noteText.trim()
                else -> ExamWidgetUpdater.formatMinutesCompact(latestNote.studyDurationMinutes, isTr)
            }
        }
    }

    val examList = remember(examTarget, userTrack, tytDays, aytDays, lgsDays, kpssDays, msuDays, customTimers, lang) {
        val list = mutableListOf<ExamTickerItem>()
        val isTr = lang == "TR"
        when (examTarget) {
            "LGS" -> {
                list.add(ExamTickerItem(if (isTr) "LGS 2027" else "LGS 2027 Exam", lgsDays, "LGS"))
            }
            "KPSS" -> {
                list.add(ExamTickerItem(if (isTr) "KPSS Lisans 2027" else "KPSS 2027 Exam", kpssDays, "KPSS"))
            }
            else -> {
                list.add(ExamTickerItem("YKS (TYT) 2027", tytDays, "YKS"))
                list.add(ExamTickerItem("YKS (AYT) 2027", aytDays, "YKS"))
                if (userTrack.trim().equals("Dil", ignoreCase = true)) {
                    list.add(ExamTickerItem("YKS (YDT) 2027", aytDays, "YKS"))
                }
                list.add(ExamTickerItem("MSÜ 2027", msuDays, "MSÜ"))
            }
        }

        val now = System.currentTimeMillis()
        customTimers.forEach { timer ->
            val diff = (timer.targetTimestamp - now).coerceAtLeast(0)
            val d = diff / (24L * 3600L * 1000L)
            list.add(ExamTickerItem(timer.title, d, "CUSTOM"))
        }
        list
    }

    var selectedExamIndex by remember { mutableIntStateOf(0) }
    val currentExamIndex = if (examList.isNotEmpty()) selectedExamIndex.coerceIn(0, examList.lastIndex) else 0
    val currentExam = examList.getOrElse(currentExamIndex) { ExamTickerItem("YKS (TYT) 2027", tytDays, "YKS") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 0. TOP GREETING & 3-DOT MENU
            item(key = "header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        val greetingPrefix = TranslationHelper.getTranslation("welcome_user", lang)
                        val studentFallback = TranslationHelper.getTranslation("default_student", lang)
                        Text(
                            text = "$greetingPrefix, ${userName.ifBlank { studentFallback }}",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val subtitle = when (examTarget) {
                            "LGS" -> TranslationHelper.getTranslation("prep_lgs", lang)
                            "KPSS" -> TranslationHelper.getTranslation("prep_kpss", lang)
                            else -> "${TranslationHelper.localizeTrack(userTrack, lang)} • ${TranslationHelper.getTranslation("prep_yks_suffix", lang)}"
                        }
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showOptionsMenu = true }
                                .testTag("dashboard_more_menu_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = TranslationHelper.getTranslation("more_options", lang),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(TranslationHelper.getTranslation("calculator", lang), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Calculate,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    showCalculatorDialog = true
                                }
                            )
                            if (isParentPairingEnabled) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            TranslationHelper.getTranslation("parent_pairing_title", lang),
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.SupervisorAccount,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary
                                        )
                                    },
                                    onClick = {
                                        showOptionsMenu = false
                                        onNavigateToParentPairing()
                                    },
                                    modifier = Modifier.testTag("menu_parent_pairing")
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(TranslationHelper.getTranslation("settings_title", lang), fontWeight = FontWeight.Medium) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Filled.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOptionsMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    }
                }
            }

            // 1. COMPACT EXAM ŞAFAK TICKER (UNDER GREETING)
            item(key = "safak_ticker") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("top_safak_exam_banner"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
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
                                selectedExamIndex = if (currentExamIndex > 0) currentExamIndex - 1 else examList.lastIndex
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .testTag("safak_arrow_prev")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = TranslationHelper.getTranslation("prev_exam", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Whatshot,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = ExamWidgetUpdater.formatCountdownText(
                                        examTitleOrKey = currentExam.title,
                                        daysRemaining = currentExam.daysRemaining,
                                        isTr = lang == "TR"
                                    ),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 22.sp,
                                        letterSpacing = 0.3.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = currentExam.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "• ${currentExamIndex + 1}/${examList.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                selectedExamIndex = if (currentExamIndex < examList.lastIndex) currentExamIndex + 1 else 0
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .testTag("safak_arrow_next")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = TranslationHelper.getTranslation("next_exam", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // 2. MOTIVATIONAL QUOTE CARD
            item(key = "quote_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${TranslationHelper.getTranslation("quote_title_prefix", lang)} (${TranslationHelper.getTranslation("day_word", lang)} $usageDaysCount)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "\"${todayQuote.text}\"",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    lineHeight = 20.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "— ${todayQuote.author}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // 3. COMPACT AJANDAM CARD (ICON + TITLE + ARROW ONLY)
            item(key = "ajandam_menu_card", contentType = "agenda_card") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onNavigateToAgenda() }
                        .testTag("dashboard_ajandam_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = TranslationHelper.getTranslation("agenda_menu_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = TranslationHelper.getTranslation("agenda_open_btn", lang),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 4. HOME STUDY STATISTICS CARD (CURRENT WEEK ONLY + ARROW TO ALL-TIME DETAILS)
            item(key = "home_stats_card", contentType = "stats_card") {
                HomeStudyStatsCard(
                    stats = weeklyDashboardStats,
                    lang = lang,
                    onOpenDetails = onNavigateToStats
                )
            }
        }
    }

    if (showCalculatorDialog) {
        CalculatorDialog(lang = lang, onDismiss = { showCalculatorDialog = false })
    }
}

@Composable
fun AddCustomTimerDialog(
    lang: String,
    onDismiss: () -> Unit,
    onAddTimer: (String, Long) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }
    var selectedHour by remember { mutableIntStateOf(12) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    var dateText by remember { mutableStateOf(TranslationHelper.getTranslation("pick_date", lang)) }
    var timeText by remember { mutableStateOf(TranslationHelper.getTranslation("pick_time", lang)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(TranslationHelper.getTranslation("add_timer_dialog_title", lang)) },
        text = {
            Column {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text(TranslationHelper.getTranslation("add_timer_name_hint", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    selectedYear = year
                                    selectedMonth = month
                                    selectedDay = dayOfMonth
                                    dateText = "$dayOfMonth/${month + 1}/$year"
                                },
                                selectedYear, selectedMonth, selectedDay
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(dateText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    selectedHour = hourOfDay
                                    selectedMinute = minute
                                    timeText = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                },
                                selectedHour, selectedMinute, true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(timeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (titleInput.isBlank()) {
                        Toast.makeText(
                            context,
                            TranslationHelper.getTranslation("enter_timer_name_warn", lang),
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        val cal = Calendar.getInstance().apply {
                            set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute, 0)
                        }
                        onAddTimer(titleInput.trim(), cal.timeInMillis)
                    }
                }
            ) {
                Text(TranslationHelper.getTranslation("add", lang))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(TranslationHelper.getTranslation("cancel", lang))
            }
        }
    )
}

@Composable
fun CalculatorDialog(lang: String, onDismiss: () -> Unit) {
    var display by remember { mutableStateOf("0") }
    var operand1 by remember { mutableStateOf<Double?>(null) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var shouldClearOnNextDigit by remember { mutableStateOf(false) }
    val errorStr = remember(lang) { TranslationHelper.getTranslation("calc_error", lang) }

    fun formatNumber(value: Double): String {
        return if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.4f", value).trimEnd('0').trimEnd('.')
        }
    }

    fun onDigit(digit: String) {
        if (shouldClearOnNextDigit || display == "0") {
            display = digit
            shouldClearOnNextDigit = false
        } else {
            if (display.length < 12) {
                display += digit
            }
        }
    }

    fun onDecimal() {
        if (shouldClearOnNextDigit) {
            display = "0."
            shouldClearOnNextDigit = false
        } else if (!display.contains(".")) {
            display += "."
        }
    }

    fun onOperator(op: String) {
        val currentVal = display.toDoubleOrNull() ?: 0.0
        if (operand1 == null) {
            operand1 = currentVal
        } else if (pendingOp != null && !shouldClearOnNextDigit) {
            val result = when (pendingOp) {
                "+" -> operand1!! + currentVal
                "-" -> operand1!! - currentVal
                "×" -> operand1!! * currentVal
                "÷" -> if (currentVal != 0.0) operand1!! / currentVal else Double.NaN
                else -> currentVal
            }
            operand1 = result
            display = if (result.isNaN()) errorStr else formatNumber(result)
        }
        pendingOp = op
        shouldClearOnNextDigit = true
    }

    fun onEqual() {
        if (operand1 != null && pendingOp != null) {
            val currentVal = display.toDoubleOrNull() ?: 0.0
            val result = when (pendingOp) {
                "+" -> operand1!! + currentVal
                "-" -> operand1!! - currentVal
                "×" -> operand1!! * currentVal
                "÷" -> if (currentVal != 0.0) operand1!! / currentVal else Double.NaN
                else -> currentVal
            }
            display = if (result.isNaN()) errorStr else formatNumber(result)
            operand1 = null
            pendingOp = null
            shouldClearOnNextDigit = true
        }
    }

    fun onClear() {
        display = "0"
        operand1 = null
        pendingOp = null
        shouldClearOnNextDigit = false
    }

    fun onBackspace() {
        if (!shouldClearOnNextDigit && display.length > 1) {
            display = display.dropLast(1)
        } else {
            display = "0"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Calculate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = TranslationHelper.getTranslation("calculator", lang),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = display,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val buttons = remember {
                    listOf(
                        listOf("C", "⌫", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("0", "00", ".", "=")
                    )
                }

                buttons.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        row.forEach { label ->
                            val isOp = label == "÷" || label == "×" || label == "-" || label == "+" || label == "="
                            val isAction = label == "C" || label == "⌫" || label == "%"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        when {
                                            label == "=" -> MaterialTheme.colorScheme.primary
                                            isOp -> MaterialTheme.colorScheme.primaryContainer
                                            isAction -> MaterialTheme.colorScheme.surfaceVariant
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .clickable {
                                        when (label) {
                                            "C" -> onClear()
                                            "⌫" -> onBackspace()
                                            "%" -> {
                                                val v = display.toDoubleOrNull() ?: 0.0
                                                display = formatNumber(v / 100.0)
                                            }
                                            "=" -> onEqual()
                                            "+", "-", "×", "÷" -> onOperator(label)
                                            "." -> onDecimal()
                                            "00" -> {
                                                onDigit("0")
                                                onDigit("0")
                                            }
                                            else -> onDigit(label)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = when {
                                        label == "=" -> MaterialTheme.colorScheme.onPrimary
                                        isOp -> MaterialTheme.colorScheme.onPrimaryContainer
                                        isAction -> MaterialTheme.colorScheme.error
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(TranslationHelper.getTranslation("close", lang))
            }
        }
    )
}

@Composable
fun HomeStudyStatsCard(
    stats: DashboardStats,
    lang: String,
    onOpenDetails: () -> Unit = {}
) {
    val hrS = TranslationHelper.getTranslation("hr_short", lang)
    val minS = TranslationHelper.getTranslation("min_short", lang)

    fun formatMins(totalMinutes: Int): String {
        if (totalMinutes <= 0) return "0 $minS"
        val h = totalMinutes / 60
        val m = totalMinutes % 60
        return when {
            h > 0 && m > 0 -> "$h $hrS $m $minS"
            h > 0 -> "$h $hrS"
            else -> "$m $minS"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable { onOpenDetails() }
            .testTag("dashboard_stats_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("stats_card_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = TranslationHelper.getTranslation("stats_weekly_badge", lang),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Text(
                                text = "${stats.activeDaysCount} ${TranslationHelper.getTranslation("stats_active_days", lang)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        val hasAnyActivity = stats.activeDaysCount > 0 ||
                            stats.totalAllTimeMinutes > 0 ||
                            stats.totalTestsSolved > 0 ||
                            stats.totalTopicsReviewed > 0 ||
                            stats.totalDenemesSolved > 0
                        if (!hasAnyActivity) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = TranslationHelper.getTranslation("stats_card_subtitle", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onOpenDetails,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("dashboard_stats_detail_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = TranslationHelper.getTranslation("stats_view_details", lang),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Core KPI Boxes: Daily Average, Today, Total Tests
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatKpiBox(
                    label = TranslationHelper.getTranslation("stats_avg_daily", lang),
                    value = formatMins(stats.averageDailyMinutes),
                    modifier = Modifier.weight(1f),
                    highlight = true
                )
                StatKpiBox(
                    label = TranslationHelper.getTranslation("stats_today_total", lang),
                    value = formatMins(stats.todayTotalMinutes),
                    modifier = Modifier.weight(1f),
                    highlight = false
                )
                StatKpiBox(
                    label = TranslationHelper.getTranslation("stats_total_tests", lang),
                    value = "${stats.totalTestsSolved}",
                    modifier = Modifier.weight(1f),
                    highlight = false
                )
            }

            if (stats.totalAllTimeMinutes > 0 || stats.totalDenemesSolved > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                if (stats.totalAllTimeMinutes > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("stats_total_time", lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatMins(stats.totalAllTimeMinutes),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (stats.totalDenemesSolved > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("stats_total_denemes", lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${stats.totalDenemesSolved}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subject-by-subject Question Practice breakdown
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Quiz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = TranslationHelper.getTranslation("stats_practice_by_subject", lang),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (stats.practiceBySubject.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        .padding(12.dp)
                ) {
                    Text(
                        text = TranslationHelper.getTranslation("stats_no_practice_yet", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val maxTests = stats.practiceBySubject.maxOf { it.totalTests }.coerceAtLeast(1)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stats.practiceBySubject.forEach { subjStat ->
                        val ratio = (subjStat.totalTests.toFloat() / maxTests.toFloat()).coerceIn(0.05f, 1f)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = TranslationHelper.localizeSubject(subjStat.subject, lang),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        TranslationHelper.getTranslation("stats_tests_fmt", lang),
                                        subjStat.totalTests
                                    ),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            LinearProgressIndicator(
                                progress = { ratio },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatKpiBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (highlight) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = if (highlight) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

data class ExamTickerItem(
    val title: String,
    val daysRemaining: Long,
    val category: String
)
