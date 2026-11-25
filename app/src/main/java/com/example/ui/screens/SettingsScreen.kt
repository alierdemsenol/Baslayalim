package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.preferences.UserPreferences
import com.example.ui.viewmodel.StudyViewModel
import com.example.util.LocalSyncHelper
import com.example.util.TranslationHelper
import com.example.widget.ExamWidgetUpdater
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
private fun SettingsWifiDiscoveryEffect(viewModel: StudyViewModel) {
    DisposableEffect(Unit) {
        viewModel.clearLanTransferStatus()
        viewModel.startLanDiscovery(mode = "TRANSFER")
        onDispose {
            viewModel.stopLanDiscovery()
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: StudyViewModel,
    onBack: (() -> Unit)? = null
) {
    val lang by viewModel.userLanguage.collectAsState()
    val currentName by viewModel.userName.collectAsState()
    val currentTrack by viewModel.userTrack.collectAsState()
    val currentExamTarget by viewModel.examTarget.collectAsState()
    val currentYdtLang by viewModel.ydtLanguage.collectAsState()
    val currentPitchBlackDark by viewModel.isPitchBlackDarkEnabled.collectAsState()
    val currentThemeMode by viewModel.themeMode.collectAsState()
    val currentAccentColorHex by viewModel.accentColorHex.collectAsState()
    val currentSyncKey by viewModel.syncKey.collectAsState()
    val customTimers by viewModel.customTimers.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val deviceUniqueCode by viewModel.deviceUniqueCode.collectAsState()
    val isParentPairingEnabled by viewModel.isParentPairingEnabled.collectAsState()
    var versionTapCount by remember { mutableIntStateOf(0) }
    var devOptionsUnlocked by remember { mutableStateOf(false) }
    val savedTransferDevices by viewModel.savedTransferDevices.collectAsState()
    val discoveredPeers by viewModel.discoveredPeers.collectAsState()
    val lanTransferStatus by viewModel.lanTransferStatus.collectAsState()
    val toastEvent by viewModel.toastEvent.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Genel, 1 = Yedekleme

    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    var selectedTarget by remember(currentExamTarget) { mutableStateOf(currentExamTarget) }
    var selectedTrack by remember(currentTrack) { mutableStateOf(currentTrack) }
    var selectedYdtLang by remember(currentYdtLang) { mutableStateOf(currentYdtLang) }

    var showAddCustomTimerDialog by remember { mutableStateOf(false) }
    var showEditExamDialog by remember { mutableStateOf<String?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonString by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }
    var showBugReportDialog by remember { mutableStateOf(false) }
    var bugReportText by remember { mutableStateOf("") }
    var bugReportFile by remember { mutableStateOf<File?>(null) }
    var showLegacyWifiMenu by remember { mutableStateOf(false) }
    var showLegacyWifiModal by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val myHardware = remember { LocalSyncHelper.getDeviceBrandAndModel() }
    val isParent = userRole == UserPreferences.ROLE_PARENT

    LaunchedEffect(toastEvent) {
        toastEvent?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastEvent()
        }
    }

    if (selectedTab == 1) {
        SettingsWifiDiscoveryEffect(viewModel = viewModel)
    }

    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
        onResult = { uri ->
            uri?.let { targetUri ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(exportedJsonString.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(
                        context,
                        TranslationHelper.getTranslation("backup_saved_toast", lang),
                        Toast.LENGTH_SHORT
                    ).show()
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

    val saveBugReportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain"),
        onResult = { uri ->
            uri?.let { targetUri ->
                try {
                    context.contentResolver.openOutputStream(targetUri)?.use { out ->
                        out.write(bugReportText.toByteArray(Charsets.UTF_8))
                    }
                    Toast.makeText(
                        context,
                        TranslationHelper.getTranslation("report_saved_toast", lang),
                        Toast.LENGTH_SHORT
                    ).show()
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

    val tracks = remember { listOf("Sayısal", "Eşit Ağırlık", "Sözel", "Dil") }

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
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // HEADER
            item(key = "settings_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (onBack != null) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = TranslationHelper.getTranslation("back", lang),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }

                    Text(
                        text = TranslationHelper.getTranslation("settings_header", lang),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }
            }

            // FAST 2-TAB SEGMENTED SWITCHER (Genel | Yedek & Wi-Fi)
            item(key = "settings_tabs") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        0 to TranslationHelper.getTranslation("settings_tab_general", lang),
                        1 to TranslationHelper.getTranslation("settings_tab_sync_wifi", lang)
                    ).forEach { (tabIdx, tabLabel) ->
                        val isSel = selectedTab == tabIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSel) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = tabIdx }
                                .testTag("settings_tab_$tabIdx"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                // TAB 0: GENEL (Profile, Theme, Dark Focus Mode, Custom Timers, Exam Dates, About)
                item(key = "settings_profile") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = TranslationHelper.getTranslation("profile_info", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text(TranslationHelper.getTranslation("your_name", lang)) },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (!isParent) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = TranslationHelper.getTranslation("target_exam", lang),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                val examTargetsList = remember(lang) {
                                    listOf(
                                        Triple("YKS", "YKS", TranslationHelper.getTranslation("exam_yks_sub", lang)),
                                        Triple("LGS", "LGS", TranslationHelper.getTranslation("exam_lgs_sub", lang)),
                                        Triple("KPSS", "KPSS", TranslationHelper.getTranslation("exam_kpss_sub", lang))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    examTargetsList.forEach { (target, code, sub) ->
                                        val isSelected = selectedTarget == target
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(58.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                                .clickable { selectedTarget = target }
                                                .padding(horizontal = 4.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = code,
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = sub,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                if (selectedTarget == "YKS") {
                                    Spacer(modifier = Modifier.height(14.dp))

                                    Text(
                                        text = TranslationHelper.getTranslation("your_track_label", lang),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        tracks.forEach { rawTrack ->
                                            val isSelected = selectedTrack == rawTrack
                                            val locTrack = TranslationHelper.localizeTrack(rawTrack, lang)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(52.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { selectedTrack = rawTrack }
                                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = locTrack,
                                                    style = MaterialTheme.typography.labelMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = if (locTrack.length > 8) 10.sp else 12.sp,
                                                        lineHeight = 13.sp
                                                    ),
                                                    textAlign = TextAlign.Center,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    if (selectedTrack == "Dil") {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = TranslationHelper.getTranslation("ydt_language_label", lang),
                                            style = MaterialTheme.typography.labelMedium,
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
                                                        .background(if (isLangSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
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
                                } else if (selectedTarget == "KPSS") {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            text = TranslationHelper.getTranslation("kpss_info_box", lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
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
                                    Toast.makeText(
                                        context,
                                        TranslationHelper.getTranslation("profile_updated", lang),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("save_profile_btn")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(TranslationHelper.getTranslation("save_changes", lang), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (!isParent) {
                    // STUDENT: PARENT PAIRING MENU SWITCH (directly below Profile)
                    item(key = "settings_parent_pairing_switch") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.SupervisorAccount,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.tertiary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = TranslationHelper.getTranslation("parent_pairing_switch_title", lang),
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("parent_pairing_switch_desc", lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = isParentPairingEnabled,
                                    onCheckedChange = { viewModel.setParentPairingEnabled(it) },
                                    modifier = Modifier.testTag("settings_parent_pairing_switch")
                                )
                            }
                        }
                    }
                }

                // ROLE SWITCHER CARD (Directly under Profile / Parent Pairing)
                item(key = "settings_role_switch") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.SupervisorAccount,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("switch_role_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentRoleName = if (isParent) {
                                TranslationHelper.getTranslation("role_parent", lang)
                            } else {
                                TranslationHelper.getTranslation("role_student", lang)
                            }
                            Text(
                                text = currentRoleName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = {
                                    val nextRole = if (isParent) UserPreferences.ROLE_STUDENT else UserPreferences.ROLE_PARENT
                                    viewModel.setUserRole(nextRole)
                                    onBack?.invoke()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("settings_switch_role_btn")
                            ) {
                                Icon(Icons.Filled.SupervisorAccount, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isParent) {
                                        TranslationHelper.getTranslation("switch_to_student", lang)
                                    } else {
                                        TranslationHelper.getTranslation("switch_to_parent", lang)
                                    },
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // APP STYLE (UYGULAMA BİÇEMİ) & PURE BLACK DARK SWITCH & ACCENT COLOR CARD
                item(key = "settings_theme") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Palette,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("theme_section_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TranslationHelper.getTranslation("theme_section_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = TranslationHelper.getTranslation("appearance_mode", lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val modeItems = listOf(
                                    "SYSTEM" to TranslationHelper.getTranslation("theme_system", lang),
                                    "LIGHT" to TranslationHelper.getTranslation("theme_light", lang),
                                    "DARK" to TranslationHelper.getTranslation("theme_dark", lang)
                                )
                                modeItems.forEach { (mode, label) ->
                                    val isSelected = currentThemeMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { viewModel.updateThemeMode(mode) }
                                            .testTag("settings_theme_mode_$mode"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Pitch Black Dark Theme Switch right under Sistem / Açık / Koyu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                    Text(
                                        text = TranslationHelper.getTranslation("pitch_black_dark_title", lang),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = TranslationHelper.getTranslation("pitch_black_dark_desc", lang),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = currentPitchBlackDark,
                                    onCheckedChange = { viewModel.setPitchBlackDarkEnabled(it) },
                                    modifier = Modifier.testTag("settings_pitch_black_switch")
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = TranslationHelper.getTranslation("accent_palette", lang),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            val isTr = lang == "TR"
                            val accentColors = remember(isTr) {
                                if (isTr) {
                                    listOf(
                                        "#FF2D75" to "Pembe",
                                        "#7C3AED" to "Mor",
                                        "#0088FF" to "Mavi",
                                        "#00D2D3" to "Turkuaz",
                                        "#10B981" to "Yeşil",
                                        "#FF6B00" to "Turuncu",
                                        "#FF0033" to "Kırmızı"
                                    )
                                } else {
                                    listOf(
                                        "#FF2D75" to "Pink",
                                        "#7C3AED" to "Purple",
                                        "#0088FF" to "Blue",
                                        "#00D2D3" to "Teal",
                                        "#10B981" to "Green",
                                        "#FF6B00" to "Orange",
                                        "#FF0033" to "Red"
                                    )
                                }
                            }

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(accentColors, key = { it.first }) { (hex, name) ->
                                    val colorVal = remember(hex) {
                                        try {
                                            Color(android.graphics.Color.parseColor(hex))
                                        } catch (_: Exception) {
                                            Color(0xFFFF2D75)
                                        }
                                    }
                                    val isSelected = currentAccentColorHex.equals(hex, ignoreCase = true)

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { viewModel.updateAccentColorHex(hex) }
                                            .padding(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(19.dp))
                                                .background(colorVal)
                                                .border(
                                                    width = if (isSelected) 3.dp else 0.dp,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                                    shape = RoundedCornerShape(19.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = name,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 11.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // EXAM DATES CARD (ABOVE Custom Timers — Both Student and Parent)
                item(key = "settings_exam_dates") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_exam_dates_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("edit_exam_dates_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TranslationHelper.getTranslation("edit_exam_dates_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val isTr = lang == "TR"
                            val examsToDisplay = remember(isTr) {
                                if (isTr) {
                                    listOf(
                                        Triple("MSÜ", "MSÜ Askeri Sınav", "20 Mart 2027"),
                                        Triple("TYT", "TYT (YKS 1. Oturum)", "19 Haziran 2027"),
                                        Triple("AYT", "AYT (YKS 2. Oturum)", "20 Haziran 2027"),
                                        Triple("LGS", "LGS Sınavı", "6 Haziran 2027"),
                                        Triple("KPSS", "KPSS Lisans", "18 Temmuz 2027")
                                    )
                                } else {
                                    listOf(
                                        Triple("MSÜ", "MSÜ Military Exam", "March 20, 2027"),
                                        Triple("TYT", "TYT (YKS Session 1)", "June 19, 2027"),
                                        Triple("AYT", "AYT (YKS Session 2)", "June 20, 2027"),
                                        Triple("LGS", "LGS High School Exam", "June 6, 2027"),
                                        Triple("KPSS", "KPSS Civil Service", "July 18, 2027")
                                    )
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                examsToDisplay.forEach { (code, title, defaultDate) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = title,
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "${TranslationHelper.getTranslation("default_prefix", lang)} $defaultDate",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { showEditExamDialog = code },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                            modifier = Modifier.height(36.dp)
                                        ) {
                                            Text(TranslationHelper.getTranslation("edit", lang), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (!isParent) {
                    // CUSTOM TIMERS CARD (BELOW Exam Dates — Student only)
                    item(key = "settings_custom_timers") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_custom_timers_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Timer,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("custom_timers_title", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = TranslationHelper.getTranslation("custom_timers_desc", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                if (customTimers.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                            .padding(14.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = TranslationHelper.getTranslation("no_custom_timers", lang),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    val now = remember(customTimers) { System.currentTimeMillis() }
                                    val sdf = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.US) }
                                    val isTr = lang == "TR"
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        customTimers.forEach { timer ->
                                            val diff = (timer.targetTimestamp - now).coerceAtLeast(0)
                                            val daysRemaining = diff / (24L * 3600L * 1000L)
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
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clip(RoundedCornerShape(10.dp))
                                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Timer,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Column {
                                                        Text(
                                                            text = timer.title,
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = sdf.format(Date(timer.targetTimestamp)),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = ExamWidgetUpdater.formatCountdownText(timer.title, daysRemaining, isTr),
                                                            style = MaterialTheme.typography.labelSmall.copy(
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    IconButton(
                                                        onClick = {
                                                            viewModel.deleteCustomTimer(timer)
                                                            Toast.makeText(
                                                                context,
                                                                "\"${timer.title}\" ${TranslationHelper.getTranslation("timer_deleted", lang)}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        },
                                                        modifier = Modifier.size(36.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Delete,
                                                            contentDescription = TranslationHelper.getTranslation("delete", lang),
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(20.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { showAddCustomTimerDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("add_custom_timer_btn")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(TranslationHelper.getTranslation("add_custom_timer", lang), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // ABOUT CARD (Version 0.0.5 -> Bug Report -> GitHub Releases -> Developer & More Apps)
                item(key = "settings_about") {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_about_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = TranslationHelper.getTranslation("about_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.align(Alignment.Start)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Timer,
                                    contentDescription = "Başlayalım Logo",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = TranslationHelper.getTranslation("app_name", lang),
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = TranslationHelper.getTranslation("version_label", lang),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!devOptionsUnlocked) {
                                            versionTapCount += 1
                                            if (versionTapCount >= 7) {
                                                devOptionsUnlocked = true
                                                Toast.makeText(
                                                    context,
                                                    TranslationHelper.getTranslation("dev_options_unlocked", lang),
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("about_version_text")
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = "Ali Erdem ŞENOL",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (devOptionsUnlocked) {
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            val (text, file) = viewModel.generatePrivacySafeBugReport()
                                            bugReportText = text
                                            bugReportFile = file
                                            showBugReportDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("bug_report_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.BugReport, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(TranslationHelper.getTranslation("bug_report_btn", lang), fontWeight = FontWeight.SemiBold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alierdemsenol/Baslayalim/releases"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("github_releases_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Code, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(TranslationHelper.getTranslation("github_releases", lang))
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            FilledTonalButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/alierdemsenol"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("developer_apps_btn"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(TranslationHelper.getTranslation("developer_apps", lang))
                            }
                        }
                    }
                }
            } else {
                // TAB 1: YEDEKLEME (Auto LAN Discovery, Saved Devices, 3-Dot Legacy Sync, JSON Backup)
                item(key = "settings_wifi_sync") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = TranslationHelper.getTranslation("sync_title", lang),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box {
                                    IconButton(
                                        onClick = { showLegacyWifiMenu = true },
                                        modifier = Modifier.testTag("settings_wifi_legacy_menu_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.MoreVert,
                                            contentDescription = TranslationHelper.getTranslation("more_options", lang)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showLegacyWifiMenu,
                                        onDismissRequest = { showLegacyWifiMenu = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(TranslationHelper.getTranslation("legacy_sync_menu_item", lang)) },
                                            leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null) },
                                            onClick = {
                                                showLegacyWifiMenu = false
                                                showLegacyWifiModal = true
                                            },
                                            modifier = Modifier.testTag("settings_legacy_sync_item")
                                        )
                                    }
                                }
                            }

                            // Compact Device Identity (Brand Model • #12345)
                            val ownRoleLabel = if (userRole == UserPreferences.ROLE_PARENT) {
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
                                        text = ownRoleLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (lanTransferStatus.isNotBlank()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = lanTransferStatus,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            // Saved Transfer Devices
                            Text(
                                text = TranslationHelper.getTranslation("saved_devices_title", lang),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (savedTransferDevices.isEmpty()) {
                                Text(
                                    text = TranslationHelper.getTranslation("no_saved_devices", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                savedTransferDevices.forEachIndexed { idx, device ->
                                    val roleBadge = if (device.role == UserPreferences.ROLE_PARENT) {
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
                                                    text = device.compactSummary(),
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.syncWithPeerAuto(device.ip, device.port) { status ->
                                                        Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                                    .testTag("sync_saved_device_$idx")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Refresh,
                                                    contentDescription = TranslationHelper.getTranslation("sync_now_btn", lang),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeTransferDevice(device.id) },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = TranslationHelper.getTranslation("delete", lang),
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Nearby Discovered Devices on Same Wi-Fi
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
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = TranslationHelper.getTranslation("nearby_devices_empty", lang),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (discoveredPeers.isNotEmpty()) {
                    itemsIndexed(discoveredPeers, key = { idx, p -> "swd_${idx}_${p.stableId}" }) { idx, peer ->
                        val roleBadge = if (peer.role == UserPreferences.ROLE_PARENT) {
                            TranslationHelper.getTranslation("role_badge_parent", lang)
                        } else {
                            TranslationHelper.getTranslation("role_badge_student", lang)
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Wifi,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.saveTransferDevice(peer) },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("save_transfer_peer_$idx")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.BookmarkAdd,
                                            contentDescription = TranslationHelper.getTranslation("save_device", lang),
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.syncWithPeerAuto(peer.ip, peer.port) { status ->
                                                Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.testTag("sync_discovered_peer_$idx")
                                    ) {
                                        Text(TranslationHelper.getTranslation("sync_now_btn", lang))
                                    }
                                }
                            }
                        }
                    }
                }

                // BACKUP & RESTORE CARD
                item(key = "settings_backup") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = TranslationHelper.getTranslation("backup_title", lang),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = TranslationHelper.getTranslation("backup_desc", lang),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            exportedJsonString = viewModel.exportBackupJson()
                                            showExportDialog = true
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("export_backup_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(TranslationHelper.getTranslation("export_backup", lang))
                                }

                                OutlinedButton(
                                    onClick = { showImportDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("import_backup_btn"),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(TranslationHelper.getTranslation("import_backup", lang))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLegacyWifiModal) {
        SettingsLegacyWifiModal(
            lang = lang,
            currentSyncKey = currentSyncKey,
            onRefreshKey = {
                val newKey = LocalSyncHelper.generate4DigitKey()
                viewModel.updateSyncKey(newKey)
                Toast.makeText(
                    context,
                    "${TranslationHelper.getTranslation("new_key_generated", lang)}: $newKey",
                    Toast.LENGTH_SHORT
                ).show()
            },
            onDismiss = { showLegacyWifiModal = false },
            onPairSync = { ip, pin, onDone ->
                scope.launch {
                    LocalSyncHelper.connectAndSyncClient(context, ip, pin) { status ->
                        viewModel.refreshPreferencesState()
                        onDone(status)
                    }
                }
            }
        )
    }

    // Edit Exam Date Dialog
    showEditExamDialog?.let { targetExam ->
        var dInput by remember(targetExam) {
            mutableStateOf(
                when (targetExam) {
                    "MSÜ" -> "20"
                    "TYT" -> "19"
                    "AYT" -> "20"
                    "LGS" -> "6"
                    "KPSS" -> "18"
                    else -> "20"
                }
            )
        }
        var mInput by remember(targetExam) {
            mutableStateOf(
                when (targetExam) {
                    "MSÜ" -> "3"
                    "KPSS" -> "7"
                    else -> "6"
                }
            )
        }
        var yInput by remember(targetExam) { mutableStateOf("2027") }

        AlertDialog(
            onDismissRequest = { showEditExamDialog = null },
            title = { Text("$targetExam ${TranslationHelper.getTranslation("edit_exam_dialog_suffix", lang)}") },
            text = {
                Column {
                    Text(TranslationHelper.getTranslation("enter_dmy", lang))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = dInput,
                            onValueChange = { dInput = it.filter { c -> c.isDigit() } },
                            label = { Text(TranslationHelper.getTranslation("day", lang)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = mInput,
                            onValueChange = { mInput = it.filter { c -> c.isDigit() } },
                            label = { Text(TranslationHelper.getTranslation("month", lang)) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = yInput,
                            onValueChange = { yInput = it.filter { c -> c.isDigit() } },
                            label = { Text(TranslationHelper.getTranslation("year", lang)) },
                            singleLine = true,
                            modifier = Modifier.weight(1.2f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val day = dInput.toIntOrNull() ?: 20
                        val month = (mInput.toIntOrNull() ?: 6) - 1
                        val year = yInput.toIntOrNull() ?: 2027
                        val cal = Calendar.getInstance().apply {
                            set(year, month, day, 10, 15, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        viewModel.updateExamDate(targetExam, cal.timeInMillis)
                        showEditExamDialog = null
                    }
                ) {
                    Text(TranslationHelper.getTranslation("save", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditExamDialog = null }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text(TranslationHelper.getTranslation("export_dialog_title", lang)) },
            text = {
                Column {
                    Text(
                        text = TranslationHelper.getTranslation("export_dialog_desc", lang),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJsonString,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
                            saveBackupFileLauncher.launch("baslayalim_backup_$stamp.json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("save_file_json", lang))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Baslayalim Backup", exportedJsonString)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(
                            context,
                            TranslationHelper.getTranslation("backup_copied", lang),
                            Toast.LENGTH_SHORT
                        ).show()
                        showExportDialog = false
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(TranslationHelper.getTranslation("copy_clipboard", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text(TranslationHelper.getTranslation("close", lang))
                }
            }
        )
    }

    if (showBugReportDialog) {
        AlertDialog(
            onDismissRequest = { showBugReportDialog = false },
            title = { Text(TranslationHelper.getTranslation("bug_report_dialog_title", lang)) },
            text = {
                Column {
                    Text(
                        text = TranslationHelper.getTranslation("bug_report_privacy_note", lang),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    bugReportFile?.let { f ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${TranslationHelper.getTranslation("bug_report_file_created", lang)} ${f.name}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = bugReportText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            val defaultName = bugReportFile?.name ?: "baslayalim_bug_report.txt"
                            saveBugReportFileLauncher.launch(defaultName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(TranslationHelper.getTranslation("save_file_txt", lang))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetFile = bugReportFile
                        if (targetFile != null && targetFile.exists()) {
                            try {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    targetFile
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Başlayalım Hata Raporu (${targetFile.name})")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, TranslationHelper.getTranslation("share_report", lang)))
                            } catch (e: Exception) {
                                Toast.makeText(context, "${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(TranslationHelper.getTranslation("share_report", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBugReportDialog = false }) {
                    Text(TranslationHelper.getTranslation("close", lang))
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(TranslationHelper.getTranslation("import_dialog_title", lang)) },
            text = {
                Column {
                    Button(
                        onClick = {
                            showImportDialog = false
                            filePickerLauncher.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(TranslationHelper.getTranslation("choose_file_json", lang))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(TranslationHelper.getTranslation("paste_json_hint", lang), style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("{ ... }") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonInput.isNotBlank()) {
                            scope.launch {
                                val success = viewModel.importBackupJson(importJsonInput)
                                val msg = if (success) TranslationHelper.getTranslation("backup_loaded", lang)
                                else TranslationHelper.getTranslation("backup_invalid", lang)
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                if (success) showImportDialog = false
                            }
                        }
                    }
                ) {
                    Text(TranslationHelper.getTranslation("restore_confirm", lang))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(TranslationHelper.getTranslation("cancel", lang))
                }
            }
        )
    }

    if (showAddCustomTimerDialog) {
        AddCustomTimerDialog(
            lang = lang,
            onDismiss = { showAddCustomTimerDialog = false },
            onAddTimer = { title, timestamp ->
                viewModel.addCustomTimer(title, timestamp)
                showAddCustomTimerDialog = false
                Toast.makeText(
                    context,
                    "\"$title\" ${TranslationHelper.getTranslation("timer_added", lang)}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }
}

@Composable
private fun SettingsLegacyWifiModal(
    lang: String,
    currentSyncKey: String,
    onRefreshKey: () -> Unit,
    onDismiss: () -> Unit,
    onPairSync: (String, String, (String) -> Unit) -> Unit
) {
    val localIp = remember { LocalSyncHelper.getLocalIpAddress() }
    var targetIpInput by remember { mutableStateOf(localIp) }
    var targetKeyInput by remember { mutableStateOf("") }
    var isConnecting by remember { mutableStateOf(false) }
    var connectStatus by remember { mutableStateOf("") }
    val context = LocalContext.current

    BackHandler(enabled = !isConnecting) { onDismiss() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.52f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { if (!isConnecting) onDismiss() }
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
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = TranslationHelper.getTranslation("connect_dialog_desc", lang),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
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
                    OutlinedButton(onClick = onRefreshKey) {
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
                        .testTag("settings_legacy_ip_input")
                )
                OutlinedTextField(
                    value = targetKeyInput,
                    onValueChange = { targetKeyInput = it.filter { c -> c.isDigit() }.take(4) },
                    label = { Text(TranslationHelper.getTranslation("key_hint", lang)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_legacy_pin_input")
                )

                if (connectStatus.isNotBlank()) {
                    Text(
                        text = connectStatus,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(enabled = !isConnecting, onClick = onDismiss) {
                        Text(TranslationHelper.getTranslation("close", lang))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        enabled = !isConnecting && targetIpInput.isNotBlank() && targetKeyInput.length == 4,
                        onClick = {
                            isConnecting = true
                            connectStatus = TranslationHelper.getTranslation("connecting_msg", lang)
                            onPairSync(targetIpInput.trim(), targetKeyInput.trim()) { status ->
                                connectStatus = status
                                isConnecting = false
                                if (status.contains("tamamlandı") || status.contains("başarıyla") ||
                                    status.contains("complete", ignoreCase = true) || status.contains("synced", ignoreCase = true)
                                ) {
                                    Toast.makeText(context, status, Toast.LENGTH_SHORT).show()
                                    onDismiss()
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
