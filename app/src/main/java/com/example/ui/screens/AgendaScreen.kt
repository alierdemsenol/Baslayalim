package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AgendaSubjectBullet
import com.example.data.model.DailyNote
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.TranslationHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class EditingBulletContext(
    val sourceDateStr: String,
    val bulletIndex: Int,
    val bullet: AgendaSubjectBullet
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AgendaScreen(
    viewModel: StudyViewModel,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val lang by viewModel.userLanguage.collectAsState()
    val userTrack by viewModel.userTrack.collectAsState()
    val examTarget by viewModel.examTarget.collectAsState()
    val ydtLanguage by viewModel.ydtLanguage.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val savedBooks by viewModel.savedBooks.collectAsState()
    val toastEvent by viewModel.toastEvent.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(toastEvent) {
        toastEvent?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastEvent()
        }
    }

    val dateKeyFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val displayDateFormat = remember(lang) {
        val loc = if (lang == "TR") Locale("tr", "TR") else Locale.ENGLISH
        SimpleDateFormat("dd MMM yyyy, EEEE", loc)
    }

    var selectedDateStr by remember { mutableStateOf(dateKeyFormat.format(Date())) }
    val currentNoteForDate = remember(allNotes, selectedDateStr) {
        allNotes.firstOrNull { it.dateStr == selectedDateStr }
    }

    val trackedMinutesForDate = remember(allSessions, selectedDateStr) {
        val secs = allSessions.filter { it.dateFormatted == selectedDateStr }.sumOf { it.durationSeconds }
        if (secs > 0L) ((secs + 30L) / 60L).toInt().coerceAtLeast(1) else 0
    }

    var hoursInput by remember { mutableStateOf("") }
    var minutesInput by remember { mutableStateOf("") }
    var dayNoteInput by remember { mutableStateOf("") }

    var useCustomSubject by remember { mutableStateOf(false) }
    var bulletSubjectInput by remember { mutableStateOf("") }
    var selectedExamStage by remember(examTarget, userTrack) { mutableStateOf(if (examTarget == "YKS") "TYT" else "") }
    val trackSubjects = remember(userTrack, examTarget, selectedExamStage, ydtLanguage) {
        viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, selectedExamStage, ydtLanguage)
    }
    var bulletTopicInput by remember { mutableStateOf("") }
    var selectedStudyType by remember { mutableStateOf(AgendaSubjectBullet.STUDY_TYPE_REVIEW) }
    var testCountInput by remember { mutableStateOf("1") }
    var isBookSectionExpanded by remember { mutableStateOf(false) }
    var sourceBookInput by remember { mutableStateOf("") }

    // Deneme inputs
    val denemeOptions = remember(examTarget, userTrack, lang) {
        buildDenemeTrialOptions(examTarget, userTrack, lang)
    }
    var selectedTrialType by remember(denemeOptions) {
        mutableStateOf(denemeOptions.firstOrNull()?.first ?: "BRANCH")
    }
    var denemeQuestionsInput by remember { mutableStateOf("40") }
    var denemeCorrectInput by remember { mutableStateOf("") }
    var denemeWrongInput by remember { mutableStateOf("") }

    val bulletList = remember { mutableStateListOf<AgendaSubjectBullet>() }
    var syncedDateStr by remember { mutableStateOf("") }
    var isRecentDaysExpanded by remember { mutableStateOf(false) }

    var editingBulletCtx by remember { mutableStateOf<EditingBulletContext?>(null) }
    var editingDayNote by remember { mutableStateOf<DailyNote?>(null) }

    LaunchedEffect(selectedDateStr, currentNoteForDate, trackedMinutesForDate) {
        val dateChanged = syncedDateStr != selectedDateStr
        val note = currentNoteForDate
        if (dateChanged) {
            syncedDateStr = selectedDateStr
            if (note != null) {
                val effectiveMinutes = maxOf(note.studyDurationMinutes, trackedMinutesForDate)
                val h = effectiveMinutes / 60
                val m = effectiveMinutes % 60
                hoursInput = if (h > 0) h.toString() else ""
                minutesInput = if (m > 0 || (h == 0 && effectiveMinutes > 0)) m.toString() else ""
                dayNoteInput = note.noteText
                bulletList.clear()
                bulletList.addAll(note.getSubjectBullets())
            } else {
                if (trackedMinutesForDate > 0) {
                    val h = trackedMinutesForDate / 60
                    val m = trackedMinutesForDate % 60
                    hoursInput = if (h > 0) h.toString() else ""
                    minutesInput = if (m > 0 || (h == 0 && trackedMinutesForDate > 0)) m.toString() else ""
                } else {
                    hoursInput = ""
                    minutesInput = ""
                }
                dayNoteInput = ""
                bulletList.clear()
            }
        } else {
            val dbBullets = note?.getSubjectBullets() ?: emptyList()
            if (dbBullets != bulletList.toList()) {
                bulletList.clear()
                bulletList.addAll(dbBullets)
            }
            val dbEffective = maxOf(note?.studyDurationMinutes ?: 0, trackedMinutesForDate)
            val typedTotal = (hoursInput.toIntOrNull() ?: 0) * 60 + (minutesInput.toIntOrNull() ?: 0)
            if (dbEffective > typedTotal) {
                val h = dbEffective / 60
                val m = dbEffective % 60
                hoursInput = if (h > 0) h.toString() else ""
                minutesInput = if (m > 0 || (h == 0 && dbEffective > 0)) m.toString() else ""
            }
            if (dayNoteInput.isBlank() && !note?.noteText.isNullOrBlank()) {
                dayNoteInput = note?.noteText ?: ""
            }
        }
        val validLocalized = trackSubjects.map { TranslationHelper.localizeSubject(it, lang) }
        if (!useCustomSubject && (bulletSubjectInput.isBlank() || bulletSubjectInput !in validLocalized)) {
            bulletSubjectInput = validLocalized.firstOrNull() ?: ""
        }
    }

    LaunchedEffect(trackSubjects, useCustomSubject, lang) {
        if (!useCustomSubject) {
            val validLocalized = trackSubjects.map { TranslationHelper.localizeSubject(it, lang) }
            if (bulletSubjectInput !in validLocalized) {
                bulletSubjectInput = validLocalized.firstOrNull() ?: ""
            }
        }
    }

    fun inferStageFromSubject(rawSubject: String): String {
        if (examTarget != "YKS") return ""
        return viewModel.normalizeStageForTrack(selectedExamStage, userTrack, examTarget)
    }

    fun currentTotalInputMinutes(): Int {
        val h = hoursInput.toIntOrNull() ?: 0
        val m = minutesInput.toIntOrNull() ?: 0
        return (h * 60 + m).coerceAtLeast(0)
    }

    fun addMinutesPreset(deltaMinutes: Int) {
        val total = (currentTotalInputMinutes() + deltaMinutes).coerceAtLeast(0)
        val nh = total / 60
        val nm = total % 60
        hoursInput = if (nh > 0) nh.toString() else ""
        minutesInput = if (nm > 0) nm.toString() else ""
    }

    fun persistBulletsNow(isBulletAdd: Boolean) {
        val totalMins = maxOf(currentTotalInputMinutes(), trackedMinutesForDate)
        viewModel.saveAgendaEntry(
            dateStr = selectedDateStr,
            studyDurationMinutes = totalMins,
            subjectBullets = bulletList.toList(),
            noteText = dayNoteInput.trim(),
            mood = currentNoteForDate?.mood ?: "",
            showToast = isBulletAdd,
            isBulletAutoSave = isBulletAdd
        )
    }

    fun shiftDate(deltaDays: Int) {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateKeyFormat.parse(selectedDateStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) { }
        cal.add(Calendar.DAY_OF_YEAR, deltaDays)
        selectedDateStr = dateKeyFormat.format(cal.time)
    }

    fun pickDate(initialDateStr: String, onPicked: (String) -> Unit) {
        val cal = Calendar.getInstance()
        try {
            val parsed = dateKeyFormat.parse(initialDateStr)
            if (parsed != null) cal.time = parsed
        } catch (_: Exception) { }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 12, 0, 0)
                }
                onPicked(dateKeyFormat.format(picked.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val formattedSelectedDate = remember(selectedDateStr, displayDateFormat) {
        try {
            val parsed = dateKeyFormat.parse(selectedDateStr)
            if (parsed != null) displayDateFormat.format(parsed) else selectedDateStr
        } catch (_: Exception) {
            selectedDateStr
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
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // HEADER
            item(key = "agenda_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("agenda_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = TranslationHelper.getTranslation("back", lang),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    Text(
                        text = TranslationHelper.getTranslation("agenda_menu_title", lang),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    OutlinedButton(
                        onClick = { selectedDateStr = dateKeyFormat.format(Date()) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("agenda_today_btn", lang),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // DATE SELECTOR BAR
            item(key = "agenda_date_picker") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { shiftDate(-1) },
                            modifier = Modifier.testTag("agenda_prev_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = TranslationHelper.getTranslation("agenda_prev_day", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { pickDate(selectedDateStr) { selectedDateStr = it } }
                                .padding(horizontal = 8.dp, vertical = 8.dp)
                                .testTag("agenda_pick_date_btn"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = TranslationHelper.getTranslation("agenda_select_date", lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = formattedSelectedDate,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = { shiftDate(1) },
                            modifier = Modifier.testTag("agenda_next_day_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = TranslationHelper.getTranslation("agenda_next_day", lang),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // SUBJECT + EXAM STAGE + TOPIC + STUDY TYPE (TEKRAR / SORU ÇÖZÜMÜ / DENEME) + BOOK + BULLET LIST
            item(key = "agenda_bullets_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = TranslationHelper.getTranslation("agenda_bullets_title", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Subject Quick Chips + "Diğer Ders"
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(trackSubjects, key = { it }) { rawSubject ->
                                val locName = TranslationHelper.localizeSubject(rawSubject, lang)
                                val isSel = !useCustomSubject && bulletSubjectInput == locName
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (isSel) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            useCustomSubject = false
                                            bulletSubjectInput = locName
                                            if (selectedStudyType != AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                                                selectedExamStage = inferStageFromSubject(rawSubject)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = locName,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            item(key = "other_subject_chip") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            if (useCustomSubject) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            useCustomSubject = true
                                            if (trackSubjects.any {
                                                    TranslationHelper.localizeSubject(it, lang) == bulletSubjectInput
                                                }
                                            ) {
                                                bulletSubjectInput = ""
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = TranslationHelper.getTranslation("agenda_other_subject", lang),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (useCustomSubject) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = useCustomSubject) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = bulletSubjectInput,
                                    onValueChange = { bulletSubjectInput = it },
                                    label = { Text(TranslationHelper.getTranslation("agenda_subject_label", lang)) },
                                    placeholder = { Text(TranslationHelper.getTranslation("agenda_custom_subject_hint", lang)) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("agenda_custom_subject_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }

                        // TYT / AYT STAGE PILLS (YKS)
                        if (examTarget == "YKS") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = TranslationHelper.getTranslation("agenda_exam_stage_label", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ExamStageSelectorRow(
                                selectedStage = selectedExamStage,
                                userTrack = userTrack,
                                lang = lang,
                                tagPrefix = "agenda_stage",
                                onSelectStage = { newStage ->
                                    selectedExamStage = viewModel.normalizeStageForTrack(newStage, userTrack, examTarget)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // TOPIC INPUT ("Çalıştığın Konu")
                        OutlinedTextField(
                            value = bulletTopicInput,
                            onValueChange = { bulletTopicInput = it },
                            label = { Text(TranslationHelper.getTranslation("agenda_topic_label", lang)) },
                            placeholder = { Text(TranslationHelper.getTranslation("agenda_topic_hint", lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("agenda_bullet_topic_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3-WAY STUDY TYPE TOGGLE: TEKRAR / SORU ÇÖZÜMÜ / DENEME
                        Text(
                            text = TranslationHelper.getTranslation("agenda_study_type_label", lang),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            StudyTypeChipButton(
                                selected = selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_REVIEW,
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                label = TranslationHelper.getTranslation("agenda_type_review", lang),
                                tag = "agenda_type_review_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStudyType = AgendaSubjectBullet.STUDY_TYPE_REVIEW }
                            )
                            StudyTypeChipButton(
                                selected = selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE,
                                icon = Icons.Filled.Quiz,
                                label = TranslationHelper.getTranslation("agenda_type_practice", lang),
                                tag = "agenda_type_practice_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStudyType = AgendaSubjectBullet.STUDY_TYPE_PRACTICE }
                            )
                            StudyTypeChipButton(
                                selected = selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_DENEME,
                                icon = Icons.Filled.Assignment,
                                label = TranslationHelper.getTranslation("agenda_type_deneme", lang),
                                tag = "agenda_type_deneme_btn",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedStudyType = AgendaSubjectBullet.STUDY_TYPE_DENEME }
                            )
                        }

                        // SORU ÇÖZÜMÜ: TEST COUNT + AUTO-REVIEW NOTE + COLLAPSIBLE SOURCE BOOK
                        AnimatedVisibility(visible = selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = TranslationHelper.getTranslation("agenda_test_count_label", lang),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                val cur = (testCountInput.toIntOrNull() ?: 1).coerceAtLeast(1)
                                                if (cur > 1) testCountInput = (cur - 1).toString()
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .testTag("agenda_test_minus_btn")
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "-", tint = MaterialTheme.colorScheme.primary)
                                        }

                                        OutlinedTextField(
                                            value = testCountInput,
                                            onValueChange = { raw ->
                                                testCountInput = raw.filter { it.isDigit() }.take(3)
                                            },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            ),
                                            modifier = Modifier
                                                .width(68.dp)
                                                .testTag("agenda_test_count_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        IconButton(
                                            onClick = {
                                                val cur = (testCountInput.toIntOrNull() ?: 0).coerceAtLeast(0)
                                                testCountInput = (cur + 1).coerceAtMost(999).toString()
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .testTag("agenda_test_plus_btn")
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "+", tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("agenda_practice_auto_review_hint", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Hidden-arrow collapsible Source Book toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .clickable { isBookSectionExpanded = !isBookSectionExpanded }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                        .testTag("agenda_book_toggle"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = TranslationHelper.getTranslation("agenda_book_toggle", lang),
                                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isBookSectionExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                        contentDescription = TranslationHelper.getTranslation("agenda_book_toggle", lang),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                AnimatedVisibility(visible = isBookSectionExpanded) {
                                    val bookSuggestions = remember(
                                        savedBooks,
                                        selectedExamStage,
                                        bulletSubjectInput,
                                        sourceBookInput,
                                        examTarget
                                    ) {
                                        val effStage = if (examTarget == "YKS") selectedExamStage else ""
                                        com.example.data.model.SavedBookHelper.filterSuggestions(
                                            savedBooks = savedBooks,
                                            examStage = effStage,
                                            subject = bulletSubjectInput,
                                            query = sourceBookInput
                                        )
                                    }
                                    Column {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = sourceBookInput,
                                            onValueChange = { sourceBookInput = it },
                                            label = { Text(TranslationHelper.getTranslation("agenda_book_label", lang)) },
                                            placeholder = { Text(TranslationHelper.getTranslation("agenda_book_hint", lang)) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("agenda_book_input"),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        if (bookSuggestions.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            val stagePrefix = if (examTarget == "YKS" && selectedExamStage.isNotBlank()) "$selectedExamStage • " else ""
                                            Text(
                                                text = String.format(
                                                    Locale.US,
                                                    TranslationHelper.getTranslation("agenda_book_suggestions_fmt", lang),
                                                    "$stagePrefix$bulletSubjectInput"
                                                ),
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                itemsIndexed(bookSuggestions, key = { idx, s -> "bk_${idx}_$s" }) { idx, suggested ->
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                                            .clickable { sourceBookInput = suggested }
                                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                                            .testTag("agenda_book_suggestion_$idx")
                                                    ) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(
                                                                imageVector = Icons.Filled.MenuBook,
                                                                contentDescription = null,
                                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(4.dp))
                                                            Text(
                                                                text = suggested,
                                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // DENEME SECTION (TYT / AYT(TRACK) / BRANŞ + TOTAL/CORRECT/WRONG + LIVE NET)
                        AnimatedVisibility(visible = selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                            Column {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("agenda_deneme_type_label", lang),
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    denemeOptions.forEach { (code, label) ->
                                        val isSel = selectedTrialType == code
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.tertiaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) MaterialTheme.colorScheme.tertiary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    selectedTrialType = code
                                                    if (code in listOf("TYT", "AYT", "YDT")) {
                                                        selectedExamStage = code
                                                    }
                                                    denemeQuestionsInput = defaultQuestionCountForTrial(code, examTarget, userTrack).toString()
                                                }
                                                .padding(horizontal = 4.dp)
                                                .testTag("agenda_deneme_type_$code"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = if (isSel) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = denemeQuestionsInput,
                                        onValueChange = { v -> denemeQuestionsInput = v.filter { it.isDigit() }.take(3) },
                                        label = { Text(TranslationHelper.getTranslation("agenda_deneme_questions", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("agenda_deneme_questions_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = denemeCorrectInput,
                                        onValueChange = { v -> denemeCorrectInput = v.filter { it.isDigit() }.take(3) },
                                        label = { Text(TranslationHelper.getTranslation("agenda_deneme_correct", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("agenda_deneme_correct_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = denemeWrongInput,
                                        onValueChange = { v -> denemeWrongInput = v.filter { it.isDigit() }.take(3) },
                                        label = { Text(TranslationHelper.getTranslation("agenda_deneme_wrong", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("agenda_deneme_wrong_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                val qVal = denemeQuestionsInput.toIntOrNull() ?: 0
                                val cVal = denemeCorrectInput.toIntOrNull() ?: 0
                                val wVal = denemeWrongInput.toIntOrNull() ?: 0
                                val blankVal = (qVal - cVal - wVal).coerceAtLeast(0)
                                val netVal = cVal - (wVal / 4.0)

                                Spacer(modifier = Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = String.format(
                                            Locale.US,
                                            TranslationHelper.getTranslation("agenda_deneme_net_fmt", lang),
                                            blankVal,
                                            netVal
                                        ),
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = {
                                val subj = bulletSubjectInput.trim()
                                val topicRaw = bulletTopicInput.trim()
                                val trialLabel = denemeOptions.firstOrNull { it.first == selectedTrialType }?.second ?: selectedTrialType

                                if (selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                                    val totalQ = denemeQuestionsInput.toIntOrNull() ?: 0
                                    val correctQ = denemeCorrectInput.toIntOrNull() ?: 0
                                    val wrongQ = denemeWrongInput.toIntOrNull() ?: 0
                                    if (subj.isEmpty()) {
                                        Toast.makeText(context, TranslationHelper.getTranslation("agenda_fill_bullet_warning", lang), Toast.LENGTH_SHORT).show()
                                    } else if (totalQ <= 0 || correctQ < 0 || wrongQ < 0 || (correctQ + wrongQ) > totalQ) {
                                        Toast.makeText(context, TranslationHelper.getTranslation("agenda_deneme_invalid_warn", lang), Toast.LENGTH_SHORT).show()
                                    } else {
                                        val effectiveTopic = if (topicRaw.isNotEmpty()) topicRaw else "$trialLabel ${TranslationHelper.getTranslation("agenda_type_deneme", lang)}"
                                        bulletList.add(
                                            AgendaSubjectBullet(
                                                subject = subj,
                                                topic = effectiveTopic,
                                                studyType = AgendaSubjectBullet.STUDY_TYPE_DENEME,
                                                examStage = if (examTarget == "YKS") selectedExamStage else "",
                                                trialType = selectedTrialType,
                                                totalQuestions = totalQ,
                                                correctCount = correctQ,
                                                wrongCount = wrongQ
                                            )
                                        )
                                        bulletTopicInput = ""
                                        denemeCorrectInput = ""
                                        denemeWrongInput = ""
                                        persistBulletsNow(isBulletAdd = true)
                                    }
                                } else {
                                    if (subj.isEmpty() || topicRaw.isEmpty()) {
                                        Toast.makeText(
                                            context,
                                            TranslationHelper.getTranslation("agenda_fill_bullet_warning", lang),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        val tests = if (selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) {
                                            testCountInput.toIntOrNull() ?: 0
                                        } else {
                                            0
                                        }
                                        if (selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE && tests <= 0) {
                                            Toast.makeText(
                                                context,
                                                TranslationHelper.getTranslation("agenda_enter_test_warning", lang),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            bulletList.add(
                                                AgendaSubjectBullet(
                                                    subject = subj,
                                                    topic = topicRaw,
                                                    studyType = selectedStudyType,
                                                    testCount = tests,
                                                    examStage = if (examTarget == "YKS") selectedExamStage else "",
                                                    sourceBook = if (selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) sourceBookInput.trim() else ""
                                                )
                                            )
                                            bulletTopicInput = ""
                                            if (selectedStudyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) {
                                                testCountInput = "1"
                                                sourceBookInput = ""
                                                isBookSectionExpanded = false
                                            }
                                            persistBulletsNow(isBulletAdd = true)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("agenda_add_bullet_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TranslationHelper.getTranslation("agenda_add_bullet", lang),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // SELECTED DAY ENTRIES
                        if (bulletList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = TranslationHelper.getTranslation("agenda_no_bullets", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                bulletList.forEachIndexed { index, item ->
                                    AgendaBulletRow(
                                        item = item,
                                        lang = lang,
                                        onEdit = {
                                            editingBulletCtx = EditingBulletContext(selectedDateStr, index, item)
                                        },
                                        onDelete = {
                                            bulletList.removeAt(index)
                                            persistBulletsNow(isBulletAdd = false)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // DAILY STUDY DURATION & END-OF-DAY NOTE CARD
            item(key = "agenda_duration_note_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = TranslationHelper.getTranslation("agenda_duration_label", lang),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (trackedMinutesForDate > 0) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val th = trackedMinutesForDate / 60
                            val tm = trackedMinutesForDate % 60
                            val hrS = TranslationHelper.getTranslation("hr_short", lang)
                            val minS = TranslationHelper.getTranslation("min_short", lang)
                            val trackedLabel = if (th > 0) "$th $hrS $tm $minS" else "$tm $minS"
                            Text(
                                text = "${TranslationHelper.getTranslation("agenda_auto_tracked", lang)}: $trackedLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = hoursInput,
                                onValueChange = { hoursInput = it.filter { c -> c.isDigit() }.take(2) },
                                label = { Text(TranslationHelper.getTranslation("agenda_hours_label", lang)) },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("agenda_hours_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = minutesInput,
                                onValueChange = { minutesInput = it.filter { c -> c.isDigit() }.take(3) },
                                label = { Text(TranslationHelper.getTranslation("agenda_minutes_label", lang)) },
                                placeholder = { Text("0") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("agenda_minutes_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val minS = TranslationHelper.getTranslation("min_short", lang)
                            val hrS = TranslationHelper.getTranslation("hr_short", lang)
                            listOf(
                                15 to "+15 $minS",
                                30 to "+30 $minS",
                                60 to "+1 $hrS"
                            ).forEach { (mins, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { addMinutesPreset(mins) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = TranslationHelper.getTranslation("agenda_day_note_label", lang),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = dayNoteInput,
                            onValueChange = { dayNoteInput = it },
                            placeholder = { Text(TranslationHelper.getTranslation("agenda_day_note_hint", lang)) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(88.dp)
                                .testTag("agenda_day_note_input"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                viewModel.saveAgendaEntry(
                                    dateStr = selectedDateStr,
                                    studyDurationMinutes = currentTotalInputMinutes(),
                                    subjectBullets = bulletList.toList(),
                                    noteText = dayNoteInput.trim(),
                                    mood = currentNoteForDate?.mood ?: "",
                                    showToast = true,
                                    isBulletAutoSave = false
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("agenda_save_btn"),
                            shape = RoundedCornerShape(50)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = TranslationHelper.getTranslation("agenda_save_btn", lang),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // SAVED DAYS HISTORY HEADER (COLLAPSED BY DEFAULT BEHIND ARROW)
            item(key = "agenda_recent_header") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { isRecentDaysExpanded = !isRecentDaysExpanded }
                        .testTag("agenda_recent_toggle"),
                    shape = RoundedCornerShape(16.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("agenda_recent_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${allNotes.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isRecentDaysExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                            contentDescription = TranslationHelper.getTranslation("agenda_recent_title", lang),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (isRecentDaysExpanded) {
                if (allNotes.isEmpty()) {
                    item(key = "agenda_recent_empty") {
                        Text(
                            text = TranslationHelper.getTranslation("agenda_recent_empty", lang),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                } else {
                    itemsIndexed(allNotes, key = { _, n -> n.dateStr }) { _, entry ->
                        val bullets = remember(entry.subjectBulletsJson) { entry.getSubjectBullets() }
                        val isSel = entry.dateStr == selectedDateStr
                        val hrS = TranslationHelper.getTranslation("hr_short", lang)
                        val minS = TranslationHelper.getTranslation("min_short", lang)
                        val eh = entry.studyDurationMinutes / 60
                        val em = entry.studyDurationMinutes % 60
                        val durText = when {
                            entry.studyDurationMinutes <= 0 -> null
                            eh > 0 -> "$eh $hrS $em $minS"
                            else -> "$em $minS"
                        }
                        val totalDayTests = bullets
                            .filter { it.studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE }
                            .sumOf { it.testCount.coerceAtLeast(1) }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSel) 2.dp else 0.dp,
                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedDateStr = entry.dateStr },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    FlowRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = entry.dateStr,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (durText != null) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = durText,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        if (totalDayTests > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$totalDayTests ${TranslationHelper.getTranslation("agenda_test_unit", lang)}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { editingDayNote = entry },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("agenda_edit_day_btn_${entry.dateStr}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = TranslationHelper.getTranslation("edit", lang),
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = {
                                                pickDate(entry.dateStr) { targetDate ->
                                                    if (targetDate != entry.dateStr) {
                                                        viewModel.moveEntireAgendaDay(entry.dateStr, targetDate)
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("agenda_move_day_btn_${entry.dateStr}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SwapHoriz,
                                                contentDescription = TranslationHelper.getTranslation("agenda_move_day_btn", lang),
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteNote(entry) },
                                            modifier = Modifier
                                                .size(48.dp)
                                                .testTag("agenda_delete_day_btn_${entry.dateStr}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = TranslationHelper.getTranslation("delete", lang),
                                                tint = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (bullets.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        bullets.forEachIndexed { bIdx, b ->
                                            val stagePrefix = if (b.examStage.isNotBlank()) "[${b.examStage}] " else ""
                                            val bookSuffix = if (b.sourceBook.isNotBlank()) " • ${b.sourceBook}" else ""
                                            val suffix = when (b.studyType) {
                                                AgendaSubjectBullet.STUDY_TYPE_PRACTICE ->
                                                    " (${b.testCount.coerceAtLeast(1)} ${TranslationHelper.getTranslation("agenda_test_unit", lang)} • ${TranslationHelper.getTranslation("agenda_practice_auto_review", lang)})"
                                                AgendaSubjectBullet.STUDY_TYPE_DENEME -> {
                                                    val q = b.totalQuestions
                                                    val c = b.correctCount
                                                    val w = b.wrongCount
                                                    val net = String.format(Locale.US, "%.2f", b.computeNet())
                                                    " (${b.trialType}: ${q}S / ${c}D ${w}Y • $net Net)"
                                                }
                                                AgendaSubjectBullet.STUDY_TYPE_REVIEW ->
                                                    " (${TranslationHelper.getTranslation("agenda_type_review", lang)})"
                                                else -> ""
                                            }
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                                    .testTag("agenda_past_bullet_${entry.dateStr}_$bIdx"),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "• $stagePrefix${TranslationHelper.localizeSubject(b.subject, lang)}: ${b.topic}$suffix$bookSuffix",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (entry.noteText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = entry.noteText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingBulletCtx?.let { ctx ->
        EditAgendaBulletDialog(
            sourceDateStr = ctx.sourceDateStr,
            initialBullet = ctx.bullet,
            userTrack = userTrack,
            examTarget = examTarget,
            ydtLanguage = ydtLanguage,
            viewModel = viewModel,
            lang = lang,
            onPickDate = { cur, onRes -> pickDate(cur, onRes) },
            onDismiss = { editingBulletCtx = null },
            onSaveOrMove = { updatedBullet, targetDateStr ->
                editingBulletCtx = null
                viewModel.updateOrMoveAgendaBullet(
                    sourceDateStr = ctx.sourceDateStr,
                    bulletIndex = ctx.bulletIndex,
                    updatedBullet = updatedBullet,
                    targetDateStr = targetDateStr
                )
            }
        )
    }

    editingDayNote?.let { note ->
        EditDayMetadataDialog(
            note = note,
            lang = lang,
            onDismiss = { editingDayNote = null },
            onEditBullet = { idx, bullet ->
                editingDayNote = null
                editingBulletCtx = EditingBulletContext(note.dateStr, idx, bullet)
            },
            onSave = { durMins, updatedText ->
                editingDayNote = null
                viewModel.updateDayMetadata(
                    dateStr = note.dateStr,
                    studyDurationMinutes = durMins,
                    noteText = updatedText,
                    mood = note.mood
                )
            }
        )
    }
}

private fun buildDenemeTrialOptions(
    examTarget: String,
    userTrack: String,
    lang: String
): List<Pair<String, String>> {
    val branchLabel = TranslationHelper.getTranslation("agenda_deneme_branch", lang)
    return when (examTarget) {
        "YKS" -> {
            val secondOption = if (userTrack.equals("Dil", ignoreCase = true)) {
                "YDT" to TranslationHelper.getTranslation("agenda_deneme_ydt", lang)
            } else {
                val trackLoc = TranslationHelper.localizeTrack(userTrack, lang)
                "AYT" to String.format(
                    Locale.US,
                    TranslationHelper.getTranslation("agenda_deneme_ayt_fmt", lang),
                    trackLoc
                )
            }
            listOf(
                "TYT" to TranslationHelper.getTranslation("agenda_deneme_tyt", lang),
                secondOption,
                "BRANCH" to branchLabel
            )
        }
        else -> listOf(
            "GENEL" to TranslationHelper.getTranslation("agenda_deneme_genel", lang),
            "BRANCH" to branchLabel
        )
    }
}

private fun defaultQuestionCountForTrial(code: String, examTarget: String, userTrack: String): Int {
    return when {
        code == "TYT" -> 120
        code == "AYT" || code == "YDT" -> 80
        code == "GENEL" && examTarget == "LGS" -> 90
        code == "GENEL" && examTarget == "KPSS" -> 120
        else -> 40
    }
}

@Composable
private fun StudyTypeChipButton(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AgendaBulletRow(
    item: AgendaSubjectBullet,
    lang: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isPractice = item.studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE
    val isDeneme = item.studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME
    val isTimer = item.studyType == AgendaSubjectBullet.STUDY_TYPE_TIMER
    val isReview = item.studyType == AgendaSubjectBullet.STUDY_TYPE_REVIEW

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isPractice || isDeneme -> MaterialTheme.colorScheme.tertiaryContainer
                            isTimer -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isPractice -> Icons.Filled.Quiz
                        isDeneme -> Icons.Filled.Assignment
                        isTimer -> Icons.Filled.Timer
                        isReview -> Icons.AutoMirrored.Filled.MenuBook
                        else -> Icons.Filled.EditNote
                    },
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = when {
                        isPractice || isDeneme -> MaterialTheme.colorScheme.onTertiaryContainer
                        isTimer -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (item.examStage.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = item.examStage,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Text(
                        text = TranslationHelper.localizeSubject(item.subject, lang),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (isPractice || isReview || isTimer || isDeneme) {
                        val badgeLabel = when {
                            isPractice -> "${item.testCount.coerceAtLeast(1)} ${TranslationHelper.getTranslation("agenda_test_unit", lang)}"
                            isDeneme -> TranslationHelper.getTranslation("agenda_type_deneme", lang)
                            isTimer -> TranslationHelper.getTranslation("agenda_type_timer", lang)
                            else -> TranslationHelper.getTranslation("agenda_type_review", lang)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isPractice || isDeneme) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.surface
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = if (isPractice || isDeneme) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPractice) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("agenda_practice_auto_review", lang),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.topic,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isDeneme && item.totalQuestions > 0) {
                    val netStr = String.format(Locale.US, "%.2f", item.computeNet())
                    Text(
                        text = "${item.trialType} • ${item.totalQuestions} Soru • ${item.correctCount}D / ${item.wrongCount}Y / ${item.blankCount}B • $netStr Net",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                if (item.sourceBook.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.sourceBook,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onEdit,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("agenda_edit_bullet_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = TranslationHelper.getTranslation("agenda_edit_record", lang),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = TranslationHelper.getTranslation("delete", lang),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun EditAgendaBulletDialog(
    sourceDateStr: String,
    initialBullet: AgendaSubjectBullet,
    userTrack: String,
    examTarget: String,
    ydtLanguage: String,
    viewModel: StudyViewModel,
    lang: String,
    onPickDate: (String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSaveOrMove: (AgendaSubjectBullet, String) -> Unit
) {
    val context = LocalContext.current
    val savedBooks by viewModel.savedBooks.collectAsState()
    var subjectText by remember { mutableStateOf(TranslationHelper.localizeSubject(initialBullet.subject, lang)) }
    var stage by remember {
        mutableStateOf(
            if (examTarget == "YKS") viewModel.normalizeStageForTrack(initialBullet.examStage, userTrack, examTarget)
            else initialBullet.examStage
        )
    }
    val stageSubjects = remember(userTrack, examTarget, stage, ydtLanguage) {
        viewModel.getSubjectsForTrackAndStage(userTrack, examTarget, stage, ydtLanguage)
    }
    var studyType by remember {
        mutableStateOf(initialBullet.studyType)
    }
    var topicText by remember { mutableStateOf(initialBullet.topic) }
    var testCountText by remember { mutableStateOf(initialBullet.testCount.coerceAtLeast(1).toString()) }
    var sourceBookText by remember { mutableStateOf(initialBullet.sourceBook) }
    var trialType by remember { mutableStateOf(initialBullet.trialType.ifBlank { "TYT" }) }
    var questionsText by remember { mutableStateOf(if (initialBullet.totalQuestions > 0) initialBullet.totalQuestions.toString() else "40") }
    var correctText by remember { mutableStateOf(initialBullet.correctCount.toString()) }
    var wrongText by remember { mutableStateOf(initialBullet.wrongCount.toString()) }
    var targetDateStr by remember { mutableStateOf(sourceDateStr) }

    val denemeOptions = remember(examTarget, userTrack, lang) {
        buildDenemeTrialOptions(examTarget, userTrack, lang)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = TranslationHelper.getTranslation("agenda_edit_record", lang),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Move to another date button
                OutlinedButton(
                    onClick = {
                        onPickDate(targetDateStr) { picked ->
                            targetDateStr = picked
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("edit_bullet_date_btn")
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${TranslationHelper.getTranslation("agenda_target_date", lang)}: $targetDateStr")
                }

                // Quick subject chips + editable subject input
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(stageSubjects, key = { it }) { raw ->
                        val loc = TranslationHelper.localizeSubject(raw, lang)
                        val isSel = subjectText == loc
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { subjectText = loc }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = loc,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = subjectText,
                    onValueChange = { subjectText = it },
                    label = { Text(TranslationHelper.getTranslation("agenda_subject_label", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (examTarget == "YKS") {
                    ExamStageSelectorRow(
                        selectedStage = stage,
                        userTrack = userTrack,
                        lang = lang,
                        tagPrefix = "edit_bullet_stage",
                        onSelectStage = { newStage ->
                            stage = viewModel.normalizeStageForTrack(newStage, userTrack, examTarget)
                        }
                    )
                }

                OutlinedTextField(
                    value = topicText,
                    onValueChange = { topicText = it },
                    label = { Text(TranslationHelper.getTranslation("agenda_topic_label", lang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_bullet_topic_input")
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StudyTypeChipButton(
                        selected = studyType == AgendaSubjectBullet.STUDY_TYPE_REVIEW,
                        icon = Icons.AutoMirrored.Filled.MenuBook,
                        label = TranslationHelper.getTranslation("agenda_type_review", lang),
                        tag = "edit_bullet_type_review",
                        modifier = Modifier.weight(1f),
                        onClick = { studyType = AgendaSubjectBullet.STUDY_TYPE_REVIEW }
                    )
                    StudyTypeChipButton(
                        selected = studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE,
                        icon = Icons.Filled.Quiz,
                        label = TranslationHelper.getTranslation("agenda_type_practice", lang),
                        tag = "edit_bullet_type_practice",
                        modifier = Modifier.weight(1f),
                        onClick = { studyType = AgendaSubjectBullet.STUDY_TYPE_PRACTICE }
                    )
                    StudyTypeChipButton(
                        selected = studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME,
                        icon = Icons.Filled.Assignment,
                        label = TranslationHelper.getTranslation("agenda_type_deneme", lang),
                        tag = "edit_bullet_type_deneme",
                        modifier = Modifier.weight(1f),
                        onClick = { studyType = AgendaSubjectBullet.STUDY_TYPE_DENEME }
                    )
                }

                if (studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) {
                    val editBookSuggestions = remember(savedBooks, stage, subjectText, sourceBookText, examTarget) {
                        val effStage = if (examTarget == "YKS") stage else ""
                        com.example.data.model.SavedBookHelper.filterSuggestions(
                            savedBooks = savedBooks,
                            examStage = effStage,
                            subject = subjectText,
                            query = sourceBookText
                        )
                    }
                    OutlinedTextField(
                        value = testCountText,
                        onValueChange = { v -> testCountText = v.filter { it.isDigit() }.take(3) },
                        label = { Text(TranslationHelper.getTranslation("agenda_test_count_label", lang)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = sourceBookText,
                        onValueChange = { sourceBookText = it },
                        label = { Text(TranslationHelper.getTranslation("agenda_book_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (editBookSuggestions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            itemsIndexed(editBookSuggestions, key = { idx, s -> "ebk_${idx}_$s" }) { _, suggested ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                        .clickable { sourceBookText = suggested }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = suggested,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        denemeOptions.forEach { (code, label) ->
                            val isSel = trialType == code
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable {
                                        trialType = code
                                        if (code in listOf("TYT", "AYT", "YDT")) stage = code
                                    }
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSel) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = questionsText,
                            onValueChange = { v -> questionsText = v.filter { it.isDigit() }.take(3) },
                            label = { Text(TranslationHelper.getTranslation("agenda_deneme_questions", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = correctText,
                            onValueChange = { v -> correctText = v.filter { it.isDigit() }.take(3) },
                            label = { Text(TranslationHelper.getTranslation("agenda_deneme_correct", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = wrongText,
                            onValueChange = { v -> wrongText = v.filter { it.isDigit() }.take(3) },
                            label = { Text(TranslationHelper.getTranslation("agenda_deneme_wrong", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = sourceBookText,
                        onValueChange = { sourceBookText = it },
                        label = { Text(TranslationHelper.getTranslation("agenda_book_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val s = subjectText.trim()
                    val t = topicText.trim()
                    if (s.isEmpty() || t.isEmpty()) {
                        Toast.makeText(context, TranslationHelper.getTranslation("agenda_fill_bullet_warning", lang), Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                        val q = questionsText.toIntOrNull() ?: 0
                        val c = correctText.toIntOrNull() ?: 0
                        val w = wrongText.toIntOrNull() ?: 0
                        if (q <= 0 || c + w > q) {
                            Toast.makeText(context, TranslationHelper.getTranslation("agenda_deneme_invalid_warn", lang), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSaveOrMove(
                            initialBullet.copy(
                                subject = s,
                                topic = t,
                                studyType = AgendaSubjectBullet.STUDY_TYPE_DENEME,
                                testCount = 0,
                                examStage = stage,
                                sourceBook = "",
                                trialType = trialType,
                                totalQuestions = q,
                                correctCount = c,
                                wrongCount = w
                            ),
                            targetDateStr
                        )
                    } else {
                        val tests = if (studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) {
                            (testCountText.toIntOrNull() ?: 1).coerceAtLeast(1)
                        } else 0
                        onSaveOrMove(
                            initialBullet.copy(
                                subject = s,
                                topic = t,
                                studyType = studyType,
                                testCount = tests,
                                examStage = stage,
                                sourceBook = sourceBookText.trim(),
                                trialType = "",
                                totalQuestions = 0,
                                correctCount = 0,
                                wrongCount = 0
                            ),
                            targetDateStr
                        )
                    }
                },
                modifier = Modifier.testTag("edit_bullet_save_btn")
            ) {
                Text(TranslationHelper.getTranslation("save", lang))
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
private fun EditDayMetadataDialog(
    note: DailyNote,
    lang: String,
    onDismiss: () -> Unit,
    onEditBullet: (Int, AgendaSubjectBullet) -> Unit,
    onSave: (Int, String) -> Unit
) {
    var hoursText by remember {
        mutableStateOf((note.studyDurationMinutes / 60).let { if (it > 0) it.toString() else "" })
    }
    var minsText by remember {
        mutableStateOf((note.studyDurationMinutes % 60).let { if (it > 0) it.toString() else "" })
    }
    var noteText by remember { mutableStateOf(note.noteText) }
    val dayBullets = remember(note.subjectBulletsJson) { note.getSubjectBullets() }

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
            .padding(20.dp)
            .testTag("edit_day_metadata_dialog"),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${TranslationHelper.getTranslation("agenda_edit_day_title", lang)} (${note.dateStr})",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hoursText,
                            onValueChange = { v -> hoursText = v.filter { it.isDigit() }.take(2) },
                            label = { Text(TranslationHelper.getTranslation("agenda_hours_label", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = minsText,
                            onValueChange = { v -> minsText = v.filter { it.isDigit() }.take(3) },
                            label = { Text(TranslationHelper.getTranslation("agenda_minutes_label", lang)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text(TranslationHelper.getTranslation("agenda_day_note_label", lang)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    if (dayBullets.isNotEmpty()) {
                        Text(
                            text = TranslationHelper.getTranslation("agenda_edit_record", lang),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            dayBullets.forEachIndexed { idx, bullet ->
                                val stagePrefix = if (bullet.examStage.isNotBlank()) "[${bullet.examStage}] " else ""
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .clickable { onEditBullet(idx, bullet) }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                        .testTag("edit_day_dialog_bullet_$idx"),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$stagePrefix${TranslationHelper.localizeSubject(bullet.subject, lang)}: ${bullet.topic}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = TranslationHelper.getTranslation("agenda_edit_record", lang),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(TranslationHelper.getTranslation("cancel", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val total = (hoursText.toIntOrNull() ?: 0) * 60 + (minsText.toIntOrNull() ?: 0)
                            onSave(total.coerceAtLeast(0), noteText.trim())
                        }
                    ) {
                        Text(TranslationHelper.getTranslation("save", lang))
                    }
                }
            }
        }
    }
}
