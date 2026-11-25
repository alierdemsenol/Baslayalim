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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val ydtLanguage by viewModel.ydtLanguage.collectAsState()

    val timerSubject by viewModel.timerSubject.collectAsState()
    val timerExamStage by viewModel.timerExamStage.collectAsState()
    val branchMockSubject by viewModel.branchMockSubject.collectAsState()
    val isDenemeMode by viewModel.isDenemeMode.collectAsState()
    val denemeType by viewModel.denemeType.collectAsState()
    val timerMinutes by viewModel.timerDurationMinutes.collectAsState()
    val totalSecs by viewModel.timerTotalSeconds.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val isPaused by viewModel.timerIsPaused.collectAsState()

    val availableSubjects = remember(userTrack, examTarget, timerExamStage, ydtLanguage) {
        viewModel.getTimerSubjects(userTrack, examTarget, timerExamStage, ydtLanguage)
    }
    val branchSubjects = remember(userTrack, examTarget, timerExamStage, ydtLanguage) {
        viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, timerExamStage, ydtLanguage)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactHeight = maxHeight < 440.dp
        val ringSize = if (isCompactHeight) 160.dp else 220.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .padding(horizontal = 20.dp, vertical = if (isCompactHeight) 8.dp else 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!isRunning && !isPaused) {
                    if (examTarget == "YKS") {
                        ExamStageSelectorRow(
                            selectedStage = timerExamStage,
                            userTrack = userTrack,
                            lang = lang,
                            tagPrefix = "timer_stage",
                            onSelectStage = { viewModel.selectTimerExamStage(it) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text(
                        text = TranslationHelper.getTranslation("timer_subject_label", lang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("timer_subject_list")
                    ) {
                        items(availableSubjects, key = { it }) { subject ->
                            val isSelected = timerSubject == subject || (isDenemeMode && subject == "Branş Denemesi")
                            val isDenemeChip = subject == "Branş Denemesi" || subject == "Deneme Sınavı"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) {
                                            if (isDenemeChip) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                                        } else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { viewModel.selectTimerSubject(subject) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .then(if (isDenemeChip) Modifier.testTag("timer_brans_chip") else Modifier)
                            ) {
                                Text(
                                    text = TranslationHelper.localizeSubject(subject, lang),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) {
                                        if (isDenemeChip) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onPrimary
                                    } else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isDenemeMode) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("timer_brans_deneme_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = TranslationHelper.getTranslation("brans_denemesi_title", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("brans_denemesi_desc", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    branchSubjects.forEach { bSubj ->
                                        val isSel = branchMockSubject == bSubj
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface)
                                                .clickable { viewModel.selectBranchMockSubject(bSubj) }
                                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                                .testTag("branch_subject_$bSubj")
                                        ) {
                                            Text(
                                                text = TranslationHelper.localizeSubject(bSubj, lang),
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSel) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = TranslationHelper.getTranslation("timer_duration_label", lang),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "$timerMinutes ${TranslationHelper.getTranslation("min_short", lang)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                }
                                Slider(
                                    value = timerMinutes.coerceIn(5, 90).toFloat(),
                                    onValueChange = { viewModel.setCustomTimerDuration(it.toInt()) },
                                    valueRange = 5f..90f,
                                    steps = 16,
                                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.tertiary),
                                    modifier = Modifier.testTag("timer_brans_slider")
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val minLabel = TranslationHelper.getTranslation("min_short", lang)
                                    listOf(20, 40, 60, 90).forEach { preset ->
                                        val isPreset = timerMinutes == preset
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isPreset) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surface)
                                                .clickable { viewModel.setCustomTimerDuration(preset) }
                                                .padding(vertical = 6.dp)
                                                .testTag("timer_brans_preset_$preset"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$preset $minLabel",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = if (isPreset) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = TranslationHelper.getTranslation("timer_duration_label", lang),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "$timerMinutes ${TranslationHelper.getTranslation("min_short", lang)}",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Slider(
                                    value = timerMinutes.toFloat(),
                                    onValueChange = { viewModel.setCustomTimerDuration(it.toInt()) },
                                    valueRange = 10f..180f,
                                    steps = 33,
                                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
                    }
                }
            }

            if (isRunning || isPaused) {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
                val baseLabel = if (isDenemeMode) TranslationHelper.localizeDenemeOption(denemeType, lang)
                else TranslationHelper.localizeSubject(timerSubject, lang)
                val fullLabel = if (timerExamStage.isNotBlank() && !isDenemeMode) "$baseLabel • $timerExamStage" else baseLabel
                TimerCountdownRing(
                    remainingSecondsFlow = viewModel.timerRemainingSeconds,
                    totalSecs = totalSecs,
                    isDenemeMode = isDenemeMode,
                    ringSize = ringSize,
                    labelText = fullLabel
                )
                Spacer(modifier = Modifier.height(if (isCompactHeight) 8.dp else 16.dp))
            } else {
                Spacer(modifier = Modifier.height(if (isCompactHeight) 14.dp else 24.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isRunning && !isPaused) {
                    Button(
                        onClick = { viewModel.startCustomTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("timer_start_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDenemeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (isDenemeMode) TranslationHelper.getTranslation("btn_start_deneme", lang)
                            else TranslationHelper.getTranslation("btn_start_timer", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else if (isRunning) {
                    Button(
                        onClick = { viewModel.pauseCustomTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("timer_pause_btn"),
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
                } else if (isPaused) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.startCustomTimer() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("timer_resume_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                TranslationHelper.getTranslation("btn_continue", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = { viewModel.abandonCustomTimerFirar() },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("timer_firar_btn"),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                TranslationHelper.getTranslation("btn_end_firar", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetCustomTimer() },
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("timer_delete_btn"),
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

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = TranslationHelper.getTranslation("timer_footer_note", lang),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
private fun TimerCountdownRing(
    remainingSecondsFlow: StateFlow<Int>,
    totalSecs: Int,
    isDenemeMode: Boolean,
    ringSize: Dp,
    labelText: String
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
            strokeWidth = 12.dp
        )

        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = if (isDenemeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            strokeWidth = 12.dp
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
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = labelText,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isDenemeMode) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
            )
        }
    }
}
