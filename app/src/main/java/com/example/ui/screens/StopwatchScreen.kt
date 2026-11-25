package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StopwatchLap
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@Composable
fun StopwatchScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val stopwatchSubject by viewModel.stopwatchSubject.collectAsState()
    val stopwatchExamStage by viewModel.stopwatchExamStage.collectAsState()
    val isRunning by viewModel.stopwatchIsRunning.collectAsState()
    val laps by viewModel.stopwatchLaps.collectAsState()

    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val ydtLanguage by viewModel.ydtLanguage.collectAsState()
    val availableSubjects = remember(userTrack, examTarget, stopwatchExamStage, ydtLanguage) {
        viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, stopwatchExamStage, ydtLanguage)
    }

    var showSaveStageDialog by remember { mutableStateOf(false) }

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
            Text(
                text = TranslationHelper.getTranslation("stopwatch_title", lang),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )

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

            Text(
                text = TranslationHelper.getTranslation("stopwatch_subject_label", lang),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
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

            Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 14.dp))

            // Isolated clock display so high-frequency ticks never recompose subject chips, buttons, or lap lists
            StopwatchClockCard(
                elapsedMillisFlow = viewModel.stopwatchElapsedMillis,
                isCompactHeight = isCompactHeight
            )

            Spacer(modifier = Modifier.height(if (isCompactHeight) 10.dp else 14.dp))

            StopwatchActionControls(
                isRunning = isRunning,
                elapsedMillisFlow = viewModel.stopwatchElapsedMillis,
                lang = lang,
                onStart = { viewModel.startStopwatch() },
                onPause = { viewModel.pauseStopwatch() },
                onLap = { viewModel.addStopwatchLap() },
                onSave = {
                    if (examTarget == "YKS") {
                        showSaveStageDialog = true
                    } else {
                        viewModel.stopAndSaveStopwatch()
                    }
                },
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
            mutableStateOf(viewModel.normalizeStageForTrack(stopwatchExamStage))
        }
        AlertDialog(
            onDismissRequest = { showSaveStageDialog = false },
            title = {
                Text(
                    text = TranslationHelper.getTranslation("save_session_dialog_title", lang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.testTag("stopwatch_save_stage_dialog")) {
                    Text(
                        text = TranslationHelper.getTranslation("save_session_stage_prompt", lang),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ExamStageSelectorRow(
                        selectedStage = dialogStage,
                        userTrack = userTrack,
                        lang = lang,
                        tagPrefix = "stopwatch_dialog_stage",
                        onSelectStage = { dialogStage = it }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveStageDialog = false
                        viewModel.setStopwatchExamStage(dialogStage)
                        viewModel.stopAndSaveStopwatch(examStageOverride = dialogStage)
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
