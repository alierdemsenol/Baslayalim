package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmergencyPromptDialog
import com.example.ui.viewmodel.EmergencyPromptTarget
import com.example.ui.viewmodel.PomodoroState
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PomodoroScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val selectedSubject by viewModel.selectedSubject.collectAsState()
    val pomoExamStage by viewModel.pomoExamStage.collectAsState()
    val workMinutes by viewModel.workDurationMinutes.collectAsState()
    val breakMinutes by viewModel.breakDurationMinutes.collectAsState()
    val pomodoroState by viewModel.pomodoroState.collectAsState()
    val totalSecs by viewModel.pomoTotalSeconds.collectAsState()

    val emergencyPromptTarget by viewModel.emergencyPromptTarget.collectAsState()
    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val ydtLanguage by viewModel.ydtLanguage.collectAsState()
    val availableSubjects = remember(userTrack, examTarget, pomoExamStage, ydtLanguage) {
        viewModel.getPomodoroSubjects(userTrack, examTarget, pomoExamStage, ydtLanguage)
    }
    val longBreakMinutes by viewModel.longBreakMinutes.collectAsState()

    val toastEvent by viewModel.toastEvent.collectAsState()
    val context = LocalContext.current

    var showPaydosNoteDialog by remember { mutableStateOf(false) }
    var paydosNoteText by remember { mutableStateOf("") }

    LaunchedEffect(toastEvent) {
        toastEvent?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            viewModel.clearToastEvent()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactHeight = maxHeight < 440.dp
        val ringSize = if (isCompactHeight) 156.dp else 210.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (pomodoroState == PomodoroState.IDLE) {
                    if (examTarget == "YKS") {
                        ExamStageSelectorRow(
                            selectedStage = pomoExamStage,
                            userTrack = userTrack,
                            lang = lang,
                            tagPrefix = "pomo_stage",
                            includeMixed = true,
                            onSelectStage = { viewModel.selectPomoExamStage(it) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = TranslationHelper.getTranslation("pomo_subject_label", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableSubjects, key = { it }) { subject ->
                            val isSelected = selectedSubject == subject
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectSubject(subject) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = TranslationHelper.localizeSubject(subject, lang),
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = TranslationHelper.getTranslation("pomo_slider_title", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        TranslationHelper.getTranslation("pomo_work_break_fmt", lang),
                                        workMinutes,
                                        breakMinutes
                                    ),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Slider(
                                value = workMinutes.toFloat(),
                                onValueChange = { viewModel.setWorkAndBreakFromSlider(it.toInt()) },
                                valueRange = 20f..60f,
                                steps = 7,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(TranslationHelper.getTranslation("pomo_min_hint", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(TranslationHelper.getTranslation("pomo_rec_hint", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(TranslationHelper.getTranslation("pomo_max_hint", lang), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            if (pomodoroState != PomodoroState.IDLE) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))

                PomodoroCountdownRing(
                    remainingSecondsFlow = viewModel.pomoRemainingSeconds,
                    totalSecs = totalSecs,
                    pomodoroState = pomodoroState,
                    examStage = pomoExamStage,
                    ringSize = ringSize,
                    lang = lang
                )

                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
            } else {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 12.dp else 20.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (pomodoroState) {
                    PomodoroState.IDLE -> {
                        Button(
                            onClick = { viewModel.startPomodoroWork() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("pomo_start_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                TranslationHelper.getTranslation("btn_start_pomodoro", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    PomodoroState.WORKING -> {
                        Button(
                            onClick = { viewModel.requestEmergencyPrompt(EmergencyPromptTarget.POMODORO_PAUSE) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                TranslationHelper.getTranslation("btn_emergency_pause", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    PomodoroState.WORK_PAUSED -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { viewModel.resumePomodoroWork() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    TranslationHelper.getTranslation("btn_continue", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Button(
                                onClick = { viewModel.abandonPomodoroFirar() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    TranslationHelper.getTranslation("btn_end_firar", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    PomodoroState.BLOCK_FINISHED_PROMPT -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = TranslationHelper.getTranslation("pomo_block_finished_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        showPaydosNoteDialog = true
                                        viewModel.startBreakPhase()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(TranslationHelper.getTranslation("pomo_paydos_break_btn", lang), textAlign = TextAlign.Center)
                                }

                                Button(
                                    onClick = { viewModel.startBreakPhase() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(50.dp),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(TranslationHelper.getTranslation("pomo_direct_break_btn", lang), textAlign = TextAlign.Center)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.dismissDailyStatsAndResetPomodoro() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(TranslationHelper.getTranslation("pomo_finish_session", lang))
                            }
                        }
                    }

                    PomodoroState.LONG_BREAK_PROMPT -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("pomo_4_done_title", lang),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TranslationHelper.getTranslation("pomo_4_done_desc", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    TranslationHelper.getTranslation("pomo_long_break_label", lang),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    "$longBreakMinutes ${TranslationHelper.getTranslation("min_short", lang)}",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                )
                            }
                            Slider(
                                value = longBreakMinutes.toFloat(),
                                onValueChange = { viewModel.setLongBreakMinutes(it.toInt()) },
                                valueRange = 15f..60f,
                                steps = 8,
                                colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.startLongBreakPhase() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    TranslationHelper.getTranslation("pomo_start_long_break", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    PomodoroState.BREAKING -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { viewModel.requestEmergencyPrompt(EmergencyPromptTarget.POMODORO_PAUSE) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(TranslationHelper.getTranslation("pomo_pause_break", lang))
                            }

                            Button(
                                onClick = { viewModel.dismissDailyStatsAndResetPomodoro() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Filled.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(TranslationHelper.getTranslation("pomo_finish_session", lang))
                            }
                        }
                    }

                    PomodoroState.BREAK_PAUSED -> {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.resumeBreakTimer() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(50)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(TranslationHelper.getTranslation("pomo_resume_break", lang))
                            }

                            Button(
                                onClick = { viewModel.dismissDailyStatsAndResetPomodoro() },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text(TranslationHelper.getTranslation("pomo_finish_session", lang))
                            }
                        }
                    }

                    PomodoroState.SESSION_FINISHED_DAILY_STATS -> {
                        Button(
                            onClick = { viewModel.dismissDailyStatsAndResetPomodoro() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(TranslationHelper.getTranslation("ok", lang))
                        }
                    }
                }
            }
        }
    }

    if (emergencyPromptTarget != EmergencyPromptTarget.NONE) {
        EmergencyPromptDialog(
            lang = lang,
            onConfirmEmergency = { viewModel.respondEmergencyPrompt(isEmergency = true) },
            onConfirmNormal = { viewModel.respondEmergencyPrompt(isEmergency = false) },
            onAbandonFirar = { viewModel.respondEmergencyFirar() },
            onDismiss = { viewModel.respondEmergencyPrompt(isEmergency = false) }
        )
    }

    if (showPaydosNoteDialog) {
        val todayStr = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val moodKeys = remember {
            listOf(
                "Motive & Özgüvenli",
                "Sakin & Kararlı",
                "Odaklanmış & Güçlü",
                "Zihnen Yorgun ama Huzurlu",
                "Kaygılı / Stresli",
                "Bitkin & Dinlenme İhtiyacı",
                "Azimli & Vazgeçmeyen"
            )
        }
        var selectedMood by remember { mutableStateOf(moodKeys.first()) }

        AlertDialog(
            onDismissRequest = { showPaydosNoteDialog = false },
            title = { Text(TranslationHelper.getTranslation("paydos_dialog_title", lang)) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = TranslationHelper.getTranslation("paydos_dialog_question", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    moodKeys.forEach { mood ->
                        val isSel = selectedMood == mood
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSel) 2.dp else 0.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { selectedMood = mood }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = TranslationHelper.localizeMood(mood, lang),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = TranslationHelper.getTranslation("agenda_day_note_label", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = paydosNoteText,
                        onValueChange = { paydosNoteText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp),
                        placeholder = { Text(TranslationHelper.getTranslation("paydos_note_hint", lang)) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveNoteForDate(todayStr, paydosNoteText.trim(), mood = selectedMood)
                        showPaydosNoteDialog = false
                        paydosNoteText = ""
                    }
                ) {
                    Text(TranslationHelper.getTranslation("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaydosNoteDialog = false }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun ExamStageSelectorRow(
    selectedStage: String,
    userTrack: String,
    lang: String,
    tagPrefix: String,
    enabled: Boolean = true,
    includeMixed: Boolean = false,
    onSelectStage: (String) -> Unit
) {
    val options = remember(userTrack, lang, includeMixed) {
        val secondOption = if (userTrack.trim().equals("Dil", ignoreCase = true)) {
            "YDT" to TranslationHelper.getTranslation("stage_ydt", lang)
        } else {
            "AYT" to TranslationHelper.getTranslation("stage_ayt", lang)
        }
        buildList {
            add("TYT" to TranslationHelper.getTranslation("stage_tyt", lang))
            add(secondOption)
            if (includeMixed) {
                add("KARISIK" to TranslationHelper.getTranslation("stage_karisik", lang))
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { (code, label) ->
            val isSel = selectedStage.equals(code, ignoreCase = true)
            val tagCode = if (code.isBlank()) "GEN" else code
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = if (isSel) 2.dp else 1.dp,
                        color = if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = enabled) { onSelectStage(code) }
                    .testTag("${tagPrefix}_$tagCode"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PomodoroCountdownRing(
    remainingSecondsFlow: kotlinx.coroutines.flow.StateFlow<Int>,
    totalSecs: Int,
    pomodoroState: PomodoroState,
    examStage: String,
    ringSize: Dp,
    lang: String
) {
    val remainingSecs by remainingSecondsFlow.collectAsState()
    Box(
        modifier = Modifier.size(ringSize),
        contentAlignment = Alignment.Center
    ) {
        val progress = if (totalSecs > 0) remainingSecs.toFloat() / totalSecs.toFloat() else 1f

        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 10.dp
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = if (pomodoroState == PomodoroState.BREAKING || pomodoroState == PomodoroState.BREAK_PAUSED)
                MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.primary,
            strokeWidth = 10.dp
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val h = remainingSecs / 3600
            val m = (remainingSecs % 3600) / 60
            val s = remainingSecs % 60
            val timeStr = if (h > 0) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
            else String.format(Locale.US, "%02d:%02d", m, s)
            Text(
                text = timeStr,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = if (h > 0) 36.sp else 46.sp
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
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
            val stageSuffix = if (examStage.isNotBlank()) " • $examStage" else ""
            Text(
                text = TranslationHelper.getTranslation(stateKey, lang) + stageSuffix,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
