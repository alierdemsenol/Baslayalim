package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.preferences.UserPreferences
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.LocalSyncHelper
import com.example.util.TranslationHelper
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(viewModel: StudyViewModel) {
    val lang by viewModel.userLanguage.collectAsState()
    val currentSyncKey by viewModel.syncKey.collectAsState()
    val currentYdtLang by viewModel.ydtLanguage.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val deviceUniqueCode by viewModel.deviceUniqueCode.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val lanTransferStatus by viewModel.lanTransferStatus.collectAsState()
    var onboardingStep by remember { mutableIntStateOf(0) } // 0 = Welcome & Direct Tools, 1 = Quick Setup

    var nameInput by remember { mutableStateOf("") }
    var selectedTarget by remember { mutableStateOf("YKS") } // "YKS", "LGS", "KPSS"
    var selectedTrack by remember { mutableStateOf("Sayısal") }
    var selectedYdtLang by remember(currentYdtLang) { mutableStateOf(currentYdtLang) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showWifiSyncDialog by remember { mutableStateOf(false) }
    var jsonPasteInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myHardware = remember { LocalSyncHelper.getDeviceBrandAndModel() }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { selectedUri ->
                try {
                    val inputStream = context.contentResolver.openInputStream(selectedUri)
                    val jsonString = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                    if (jsonString.isNotBlank()) {
                        scope.launch {
                            val success = viewModel.importBackupJson(jsonString)
                            val msg = if (success) TranslationHelper.getTranslation("backup_loaded", lang)
                            else TranslationHelper.getTranslation("backup_invalid", lang)
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        context,
                        "${TranslationHelper.getTranslation("file_read_error", lang)}: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    )

    val tracks = remember(lang) {
        listOf(
            Triple("Sayısal", TranslationHelper.getTranslation("track_sayisal", lang), TranslationHelper.getTranslation("track_sayisal_desc", lang)),
            Triple("Eşit Ağırlık", TranslationHelper.getTranslation("track_ea", lang), TranslationHelper.getTranslation("track_ea_desc", lang)),
            Triple("Sözel", TranslationHelper.getTranslation("track_sozel", lang), TranslationHelper.getTranslation("track_sozel_desc", lang)),
            Triple("Dil", TranslationHelper.getTranslation("track_dil", lang), TranslationHelper.getTranslation("track_dil_desc", lang))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 600.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Two concise step dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(0, 1).forEach { stepIndex ->
                    val isActive = onboardingStep == stepIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isActive) 28.dp else 10.dp, 10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isActive) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (onboardingStep == 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = TranslationHelper.getTranslation("welcome_title", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = TranslationHelper.getTranslation("welcome_subtitle", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("intro_tools_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FeatureBadge(
                                    icon = Icons.Filled.AvTimer,
                                    label = TranslationHelper.getTranslation("badge_stopwatch", lang)
                                )
                                FeatureBadge(
                                    icon = Icons.Filled.HourglassTop,
                                    label = TranslationHelper.getTranslation("badge_timer", lang)
                                )
                                FeatureBadge(
                                    icon = Icons.Filled.Timer,
                                    label = TranslationHelper.getTranslation("badge_pomodoro", lang)
                                )
                                FeatureBadge(
                                    icon = Icons.Filled.CalendarMonth,
                                    label = TranslationHelper.getTranslation("badge_agenda", lang)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onboardingStep = 1 },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_next_step_1"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("next_button", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_restore_btn_step0"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("restore_button", lang))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showWifiSyncDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_sync_btn"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("onboarding_wifi_sync_btn", lang), maxLines = 1)
                    }
                }
            } else {
                // STEP 1: QUICK PROFILE SETUP (STUDENT vs PARENT + PROFILE USERNAME)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = TranslationHelper.getTranslation("setup_title", lang),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = TranslationHelper.getTranslation("setup_desc", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // NAME INPUT
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = TranslationHelper.getTranslation("name_prompt", lang),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                placeholder = { Text(TranslationHelper.getTranslation("name_placeholder", lang)) },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_name_input"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // ROLE SELECTION (ÖĞRENCİ vs VELİ)
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = TranslationHelper.getTranslation("role_label", lang),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val roles = listOf(
                                    Triple(
                                        UserPreferences.ROLE_STUDENT,
                                        TranslationHelper.getTranslation("role_student", lang),
                                        TranslationHelper.getTranslation("role_student_desc", lang)
                                    ),
                                    Triple(
                                        UserPreferences.ROLE_PARENT,
                                        TranslationHelper.getTranslation("role_parent", lang),
                                        TranslationHelper.getTranslation("role_parent_desc", lang)
                                    )
                                )
                                roles.forEach { (roleCode, roleTitle, roleDesc) ->
                                    val isSel = userRole == roleCode
                                    val tag = if (roleCode == UserPreferences.ROLE_STUDENT) {
                                        "onboarding_role_student"
                                    } else {
                                        "onboarding_role_parent"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(
                                                if (isSel) MaterialTheme.colorScheme.primaryContainer
                                                else MaterialTheme.colorScheme.surfaceVariant
                                            )
                                            .border(
                                                width = if (isSel) 2.dp else 1.dp,
                                                color = if (isSel) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { viewModel.setUserRole(roleCode) }
                                            .padding(horizontal = 10.dp, vertical = 10.dp)
                                            .testTag(tag)
                                    ) {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (roleCode == UserPreferences.ROLE_PARENT) {
                                                        Icons.Filled.SupervisorAccount
                                                    } else {
                                                        Icons.Filled.School
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = roleTitle,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = roleDesc,
                                                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = TranslationHelper.getTranslation("network_username_label", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "$myHardware • #$deviceUniqueCode",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("onboarding_network_identity")
                                    )
                                }
                            }
                        }
                    }

                    if (userRole == UserPreferences.ROLE_PARENT) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_onboarding_info_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SupervisorAccount,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("parent_onboarding_info", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(14.dp))

                        // EXAM TARGET SELECTION (YKS, LGS, KPSS)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = TranslationHelper.getTranslation("exam_target_label", lang),
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val targetItems = listOf(
                                    Triple("YKS", "YKS", TranslationHelper.getTranslation("exam_yks_sub", lang)),
                                    Triple("LGS", "LGS", TranslationHelper.getTranslation("exam_lgs_sub", lang)),
                                    Triple("KPSS", "KPSS", TranslationHelper.getTranslation("exam_kpss_sub", lang))
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    targetItems.forEach { (code, title, sub) ->
                                        val isSel = selectedTarget == code
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(60.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(
                                                    if (isSel) MaterialTheme.colorScheme.primaryContainer
                                                    else MaterialTheme.colorScheme.surfaceVariant
                                                )
                                                .border(
                                                    width = if (isSel) 2.dp else 1.dp,
                                                    color = if (isSel) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { selectedTarget = code }
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                    text = title,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (selectedTarget == "YKS") {
                            Spacer(modifier = Modifier.height(14.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = TranslationHelper.getTranslation("track_label", lang),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    tracks.forEach { (rawKey, displayTitle, trackDesc) ->
                                        val isSel = selectedTrack == rawKey
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                                .clickable { selectedTrack = rawKey }
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = displayTitle,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSel) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = trackDesc,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (isSel) {
                                                Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    }

                                    if (selectedTrack == "Dil") {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = TranslationHelper.getTranslation("ydt_language_label", lang),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        val ydtLanguages = remember { listOf("İngilizce", "Almanca", "Fransızca", "Arapça", "Rusça") }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            ydtLanguages.forEach { yLang ->
                                                val isLangSel = selectedYdtLang == yLang
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (isLangSel) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.surfaceVariant
                                                        )
                                                        .clickable { selectedYdtLang = yLang }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = yLang,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isLangSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
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

                Spacer(modifier = Modifier.height(20.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            if (nameInput.isBlank()) {
                                Toast.makeText(
                                    context,
                                    TranslationHelper.getTranslation("enter_name_warning", lang),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                val trackToSave = when (selectedTarget) {
                                    "LGS" -> "LGS Genel"
                                    "KPSS" -> "KPSS Genel"
                                    else -> selectedTrack
                                }
                                viewModel.saveUserProfile(
                                    name = nameInput.trim(),
                                    track = trackToSave,
                                    target = selectedTarget,
                                    ydtLanguage = selectedYdtLang,
                                    role = userRole
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("onboarding_start_btn"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("start_button", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showRestoreDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_restore_btn"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("restore_button", lang))
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showWifiSyncDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("onboarding_sync_btn_step1"),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("onboarding_wifi_sync_btn", lang), maxLines = 1)
                    }
                }
            }
        }
    }

    if (showWifiSyncDialog) {
        val localIp = remember { LocalSyncHelper.getLocalIpAddress() }
        var targetIpInput by remember { mutableStateOf(localIp) }
        var targetKeyInput by remember { mutableStateOf("") }
        var syncStatus by remember { mutableStateOf("") }
        var isConnecting by remember { mutableStateOf(false) }
        var showMoreMenu by remember { mutableStateOf(false) }
        var showLegacyInputs by remember { mutableStateOf(false) }

        DisposableEffect(Unit) {
            viewModel.clearLanTransferStatus()
            viewModel.startLanDiscovery(mode = "TRANSFER")
            onDispose {
                viewModel.stopLanDiscovery()
            }
        }

        BackHandler(enabled = !isConnecting) {
            showWifiSyncDialog = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.52f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { if (!isConnecting) showWifiSyncDialog = false }
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .padding(20.dp)
                .testTag("onboarding_wifi_sync_dialog"),
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
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = TranslationHelper.getTranslation("sync_title", lang),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(
                                onClick = { showMoreMenu = true },
                                modifier = Modifier.testTag("onboarding_wifi_legacy_menu_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.MoreVert,
                                    contentDescription = TranslationHelper.getTranslation("more_options", lang)
                                )
                            }
                            DropdownMenu(
                                expanded = showMoreMenu,
                                onDismissRequest = { showMoreMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(TranslationHelper.getTranslation("legacy_sync_menu_item", lang)) },
                                    leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                                    onClick = {
                                        showMoreMenu = false
                                        showLegacyInputs = !showLegacyInputs
                                    },
                                    modifier = Modifier.testTag("onboarding_legacy_menu_item")
                                )
                            }
                        }
                    }

                    // Own Brand + Device Code identity pill
                    val ownRoleBadge = if (userRole == UserPreferences.ROLE_PARENT) {
                        TranslationHelper.getTranslation("role_badge_parent", lang)
                    } else {
                        TranslationHelper.getTranslation("role_badge_student", lang)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "$myHardware • #$deviceUniqueCode",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = ownRoleBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = TranslationHelper.getTranslation("nearby_devices_title", lang),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = TranslationHelper.getTranslation("nearby_devices_scanning", lang),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (discoveredPeers.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("nearby_devices_empty", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        discoveredPeers.forEach { peer ->
                            val roleBadge = if (peer.role == UserPreferences.ROLE_PARENT) {
                                TranslationHelper.getTranslation("role_badge_parent", lang)
                            } else {
                                TranslationHelper.getTranslation("role_badge_student", lang)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Devices,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = peer.compactSummary(),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = roleBadge,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Button(
                                    enabled = !isConnecting,
                                    onClick = {
                                        isConnecting = true
                                        viewModel.syncWithPeerAuto(peer.ip, peer.port) { status ->
                                            syncStatus = status
                                            isConnecting = false
                                            if (status.contains("tamamlandı") || status.contains("başarıyla") ||
                                                status.contains("complete", ignoreCase = true) || status.contains("synced", ignoreCase = true)
                                            ) {
                                                showWifiSyncDialog = false
                                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text(TranslationHelper.getTranslation("sync_now_btn", lang))
                                }
                            }
                        }
                    }

                    val activeStatus = syncStatus.ifBlank { lanTransferStatus }
                    if (activeStatus.isNotBlank()) {
                        Text(
                            text = activeStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (showLegacyInputs) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = TranslationHelper.getTranslation("legacy_sync_menu_item", lang),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                Text(TranslationHelper.getTranslation("sync_key_label", lang), style = MaterialTheme.typography.labelSmall)
                                Text(
                                    text = currentSyncKey,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${TranslationHelper.getTranslation("ip_label", lang)} $localIp",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            OutlinedButton(
                                onClick = {
                                    val newKey = LocalSyncHelper.generate4DigitKey()
                                    viewModel.updateSyncKey(newKey)
                                }
                            ) {
                                Text(TranslationHelper.getTranslation("refresh_key", lang))
                            }
                        }

                        OutlinedTextField(
                            value = targetIpInput,
                            onValueChange = { targetIpInput = it },
                            label = { Text(TranslationHelper.getTranslation("ip_hint", lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_sync_ip_input")
                        )
                        OutlinedTextField(
                            value = targetKeyInput,
                            onValueChange = { targetKeyInput = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text(TranslationHelper.getTranslation("key_hint", lang)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("onboarding_sync_key_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = !isConnecting,
                            onClick = { showWifiSyncDialog = false }
                        ) {
                            Text(TranslationHelper.getTranslation("close", lang))
                        }
                        if (showLegacyInputs) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                enabled = !isConnecting && targetKeyInput.length == 4,
                                onClick = {
                                    isConnecting = true
                                    syncStatus = TranslationHelper.getTranslation("connecting_msg", lang)
                                    scope.launch {
                                        LocalSyncHelper.connectAndSyncClient(
                                            context,
                                            targetIpInput.trim(),
                                            targetKeyInput.trim()
                                        ) { status ->
                                            syncStatus = status
                                            isConnecting = false
                                            viewModel.refreshPreferencesState()
                                            if (status.contains("tamamlandı") || status.contains("başarıyla") ||
                                                status.contains("complete", ignoreCase = true) || status.contains("synced", ignoreCase = true)
                                            ) {
                                                showWifiSyncDialog = false
                                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                            }
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
    }

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text(TranslationHelper.getTranslation("restore_button", lang)) },
            text = {
                Column {
                    Text(TranslationHelper.getTranslation("restore_prompt", lang))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showRestoreDialog = false
                            filePickerLauncher.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(TranslationHelper.getTranslation("choose_file_json", lang))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = jsonPasteInput,
                        onValueChange = { jsonPasteInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("{ ... }") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (jsonPasteInput.isNotBlank()) {
                            scope.launch {
                                val success = viewModel.importBackupJson(jsonPasteInput)
                                val msg = if (success) TranslationHelper.getTranslation("backup_loaded", lang)
                                else TranslationHelper.getTranslation("backup_invalid", lang)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) showRestoreDialog = false
                            }
                        }
                    }
                ) { Text(TranslationHelper.getTranslation("restore_confirm", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }
}

@Composable
fun FeatureBadge(icon: ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}
