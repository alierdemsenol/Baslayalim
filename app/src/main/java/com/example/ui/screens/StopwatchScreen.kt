package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.FocusModeTarget
import com.example.ui.viewmodel.StopwatchLap
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StopwatchScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val stopwatchSubject by viewModel.stopwatchSubject.collectAsState()
    val stopwatchExamStage by viewModel.stopwatchExamStage.collectAsState()
    val isRunning by viewModel.stopwatchIsRunning.collectAsState()
    val elapsedMillis by viewModel.stopwatchElapsedMillis.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()
    val focusLockedTarget by viewModel.focusLockedTarget.collectAsState()
    val saveDialogTrigger by viewModel.openStopwatchSaveDialogTrigger.collectAsState()

    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val ydtLanguage by viewModel.ydtLanguage.collectAsState()
    val availableSubjects = remember(userTrack, examTarget, stopwatchExamStage, ydtLanguage) {
        viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, stopwatchExamStage, ydtLanguage)
    }

    var showSaveStageDialog by remember { mutableStateOf(false) }

    LaunchedEffect(saveDialogTrigger) {
        if (saveDialogTrigger > 0) {
            if (viewModel.stopwatchElapsedMillis.value > 0L) {
                showSaveStageDialog = true
            }
            viewModel.consumeStopwatchSaveDialogRequest()
        }
    }

    val isKarisik = examTarget == "YKS" && stopwatchExamStage == "KARISIK"
    val isFocusLocked = focusLockedTarget == FocusModeTarget.STOPWATCH
    val canFocusLock = isRunning || elapsedMillis > 0L

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactHeight = maxHeight < 460.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 16.dp, vertical = if (isCompactHeight) 8.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.size(48.dp))
                Text(
                    text = TranslationHelper.getTranslation("stopwatch_title", lang),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (canFocusLock) {
                        IconButton(
                            onClick = { viewModel.toggleFocusLock(FocusModeTarget.STOPWATCH) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFocusLocked) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                                )
                                .testTag("stopwatch_focus_lock_btn")
                        ) {
                            Icon(
                                imageVector = if (isFocusLocked) Icons.Filled.Lock else Icons.Outlined.LockOpen,
                                contentDescription = TranslationHelper.getTranslation("focus_lock_enable", lang),
                                tint = if (isFocusLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (examTarget == "YKS") {
                ExamStageSelectorRow(
                    selectedStage = stopwatchExamStage,
                    userTrack = userTrack,
                    lang = lang,
                    tagPrefix = "stopwatch_stage",
                    enabled = !isRunning,
                    includeMixed = true,
                    onSelectStage = { viewModel.setStopwatchExamStage(it) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isKarisik) {
                Text(
                    text = TranslationHelper.getTranslation("focus_karisik_mode_info", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("stopwatch_karisik_info")
                )
            } else {
                Text(
                    text = TranslationHelper.getTranslation("stopwatch_subject_label", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("stopwatch_subject_label")
                )
                Spacer(modifier = Modifier.height(4.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stopwatch_subject_list")
                ) {
                    items(availableSubjects, key = { it }) { subject ->
                        val isSelected = stopwatchSubject == subject
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable {
                                    if (!isRunning) {
                                        viewModel.setStopwatchSubject(subject)
                                    }
                                }
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
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

            // Isolated clock display so high-frequency ticks never recompose subject chips, buttons, or lap lists
            StopwatchClockCard(
                elapsedMillisFlow = viewModel.stopwatchElapsedMillis,
                isCompactHeight = isCompactHeight
            )

            if (isFocusLocked && canFocusLock) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = TranslationHelper.getTranslation("focus_lock_armed", lang),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag("stopwatch_focus_lock_armed")
                )
            }

            Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

            StopwatchActionControls(
                isRunning = isRunning,
                elapsedMillisFlow = viewModel.stopwatchElapsedMillis,
                lang = lang,
                onStart = { viewModel.startStopwatch() },
                onPause = { viewModel.pauseStopwatch() },
                onLap = { viewModel.addStopwatchLap() },
                onSave = { showSaveStageDialog = true },
                onDelete = { viewModel.resetStopwatch() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            if (laps.isNotEmpty()) {
                val lapPrefix = TranslationHelper.getTranslation("stopwatch_lap_prefix", lang)
                val totalPrefix = TranslationHelper.getTranslation("stopwatch_total_prefix", lang)
                val reversedLaps = remember(laps) { laps.reversed() }

                Text(
                    text = TranslationHelper.getTranslation("stopwatch_laps_title", lang),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(reversedLaps, key = { it.lapNumber }) { lap ->
                        StopwatchLapRow(lap = lap, lapPrefix = lapPrefix, totalPrefix = totalPrefix)
                    }
                }
            }
        }
    }

    if (showSaveStageDialog) {
        var dialogStage by remember(stopwatchExamStage) {
            mutableStateOf(viewModel.normalizeStageForTrack(stopwatchExamStage, allowMixed = true))
        }
        val dialogSubjects = remember(userTrack, examTarget, dialogStage, ydtLanguage) {
            viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, dialogStage, ydtLanguage)
        }
        var dialogSubject by remember(stopwatchSubject, dialogStage) {
            mutableStateOf(
                if (dialogStage == "KARISIK") {
                    "Karışık"
                } else if (stopwatchSubject in dialogSubjects) {
                    stopwatchSubject
                } else {
                    dialogSubjects.firstOrNull() ?: stopwatchSubject
                }
            )
        }
        var beforeLastLapSubject by remember(showSaveStageDialog) { mutableStateOf<String?>(null) }
        var afterLastLapSubject by remember(showSaveStageDialog) { mutableStateOf<String?>(null) }

        val lastLapMs = laps.lastOrNull()?.totalTimeMillis?.coerceAtMost(elapsedMillis) ?: 0L
        val beforeLastLapSec = (lastLapMs / 1000L).coerceAtLeast(0L)
        val afterLastLapSec = ((elapsedMillis - lastLapMs).coerceAtLeast(0L) / 1000L)

        AlertDialog(
            onDismissRequest = { showSaveStageDialog = false },
            title = {
                Text(
                    text = TranslationHelper.getTranslation("save_session_dialog_title", lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .testTag("stopwatch_save_stage_dialog")
                ) {
                    if (examTarget == "YKS") {
                        Text(
                            text = TranslationHelper.getTranslation("save_session_stage_prompt", lang),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        ExamStageSelectorRow(
                            selectedStage = dialogStage,
                            userTrack = userTrack,
                            lang = lang,
                            tagPrefix = "stopwatch_dialog_stage",
                            includeMixed = true,
                            onSelectStage = { nextStage ->
                                dialogStage = nextStage
                                dialogSubject = if (nextStage == "KARISIK") {
                                    "Karışık"
                                } else {
                                    val valid = viewModel.getSubjectsForTrackAndStage(
                                        userTrack,
                                        examTarget,
                                        nextStage,
                                        ydtLanguage
                                    )
                                    if (dialogSubject in valid) dialogSubject
                                    else (valid.firstOrNull() ?: dialogSubject)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (dialogStage == "KARISIK") {
                        Text(
                            text = TranslationHelper.getTranslation("focus_karisik_mode_info", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (laps.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = TranslationHelper.getTranslation("sw_lap_segments_optional_hint", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StopwatchOptionalLapSubjectSelector(
                                title = "${TranslationHelper.getTranslation("sw_before_last_lap", lang)} (${formatSegmentDuration(beforeLastLapSec)})",
                                selectedSubject = beforeLastLapSubject,
                                subjects = dialogSubjects,
                                lang = lang,
                                sectionTag = "stopwatch_karisik_before_lap_section",
                                tagPrefix = "sw_before_lap",
                                onSelect = { beforeLastLapSubject = it }
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            StopwatchOptionalLapSubjectSelector(
                                title = "${TranslationHelper.getTranslation("sw_after_last_lap", lang)} (${formatSegmentDuration(afterLastLapSec)})",
                                selectedSubject = afterLastLapSubject,
                                subjects = dialogSubjects,
                                lang = lang,
                                sectionTag = "stopwatch_karisik_after_lap_section",
                                tagPrefix = "sw_after_lap",
                                onSelect = { afterLastLapSubject = it }
                            )
                        }
                    } else {
                        Text(
                            text = TranslationHelper.getTranslation("save_session_subject_prompt", lang),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            dialogSubjects.forEach { subj ->
                                val isSel = dialogSubject == subj
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { dialogSubject = subj }
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                        .testTag("stopwatch_dialog_subject_$subj")
                                ) {
                                    Text(
                                        text = TranslationHelper.localizeSubject(subj, lang),
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalStage = if (examTarget == "YKS") dialogStage else ""
                        val finalSubject = if (finalStage == "KARISIK") "Karışık" else dialogSubject
                        val hasCustomLapSelection = finalStage == "KARISIK" &&
                            laps.isNotEmpty() &&
                            (beforeLastLapSubject != null || afterLastLapSubject != null)

                        showSaveStageDialog = false
                        viewModel.setStopwatchExamStage(finalStage)
                        if (finalStage != "KARISIK") {
                            viewModel.setStopwatchSubject(finalSubject)
                        }

                        if (hasCustomLapSelection) {
                            val totalSec = (elapsedMillis / 1000L).coerceAtLeast(1L)
                            val effBeforeSec = if (afterLastLapSec > 0L) beforeLastLapSec.coerceAtLeast(1L) else totalSec
                            val effAfterSec = if (afterLastLapSec > 0L) (totalSec - effBeforeSec).coerceAtLeast(1L) else 0L
                            val bSubj = beforeLastLapSubject ?: "Karışık"
                            val aSubj = afterLastLapSubject ?: "Karışık"
                            val segments = when {
                                effAfterSec <= 0L -> listOf(bSubj to totalSec)
                                bSubj == aSubj -> listOf(bSubj to totalSec)
                                else -> listOf(bSubj to effBeforeSec, aSubj to effAfterSec)
                            }
                            viewModel.stopAndSaveStopwatchSegments(
                                segments = segments,
                                examStageOverride = finalStage
                            )
                        } else {
                            viewModel.stopAndSaveStopwatch(
                                examStageOverride = finalStage,
                                subjectOverride = finalSubject
                            )
                        }
                    },
                    modifier = Modifier.testTag("stopwatch_confirm_save_btn")
                ) {
                    Text(TranslationHelper.getTranslation("save_session_confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveStageDialog = false }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }
}

private fun formatSegmentDuration(totalSeconds: Long): String {
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", mins, secs)
}

@Composable
private fun StopwatchOptionalLapSubjectSelector(
    title: String,
    selectedSubject: String?,
    subjects: List<String>,
    lang: String,
    sectionTag: String,
    tagPrefix: String,
    onSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(sectionTag)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item(key = "none") {
                val isSel = selectedSubject == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSel) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(null) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("${tagPrefix}_none")
                ) {
                    Text(
                        text = TranslationHelper.getTranslation("sw_optional_none", lang),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (isSel) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            items(subjects, key = { it }) { subj ->
                val isSel = selectedSubject == subj
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onSelect(if (isSel) null else subj) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("${tagPrefix}_$subj")
                ) {
                    Text(
                        text = TranslationHelper.localizeSubject(subj, lang),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StopwatchClockCard(
    elapsedMillisFlow: StateFlow<Long>,
    isCompactHeight: Boolean
) {
    val elapsedMillis by elapsedMillisFlow.collectAsState()
    val totalSeconds = elapsedMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val millis = (elapsedMillis % 1000) / 10

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = if (isCompactHeight) 14.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hours > 0) String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                else String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, millis),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontWeight = FontWeight.Light,
                    fontSize = if (hours > 0) 36.sp else 46.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StopwatchActionControls(
    isRunning: Boolean,
    elapsedMillisFlow: StateFlow<Long>,
    lang: String,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onLap: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val elapsedMillis by elapsedMillisFlow.collectAsState()
    val hasElapsed = elapsedMillis > 0L

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!isRunning && !hasElapsed) {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("stopwatch_start_btn"),
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
        } else if (isRunning) {
            OutlinedButton(
                onClick = onPause,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stopwatch_pause_btn"),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.Pause, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(TranslationHelper.getTranslation("stopwatch_pause", lang))
            }

            Button(
                onClick = onLap,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stopwatch_lap_btn"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(TranslationHelper.getTranslation("stopwatch_lap", lang))
            }
        } else {
            Button(
                onClick = onStart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stopwatch_resume_btn"),
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(TranslationHelper.getTranslation("stopwatch_resume", lang))
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("stopwatch_save_btn"),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text(TranslationHelper.getTranslation("stopwatch_stop_save", lang))
            }

            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("stopwatch_delete_btn"),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = TranslationHelper.getTranslation("delete", lang),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun StopwatchLapRow(
    lap: StopwatchLap,
    lapPrefix: String,
    totalPrefix: String
) {
    val lapSecs = lap.lapTimeMillis / 1000
    val lapMins = lapSecs / 60
    val lapS = lapSecs % 60
    val lapMs = (lap.lapTimeMillis % 1000) / 10

    val tSecs = lap.totalTimeMillis / 1000
    val totalMins = tSecs / 60
    val totalS = tSecs % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$lapPrefix ${lap.lapNumber}",
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = String.format(Locale.US, "+%02d:%02d.%02d", lapMins, lapS, lapMs),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = String.format(Locale.US, "%s: %02d:%02d", totalPrefix, totalMins, totalS),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
