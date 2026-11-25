package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.AgendaSubjectBullet
import com.example.data.model.CustomTimer
import com.example.data.model.DailyNote
import com.example.data.model.DiscoveredPeer
import com.example.data.model.ParentStudentSnapshot
import com.example.data.model.SavedBookEntry
import com.example.data.model.SavedBookHelper
import com.example.data.model.SavedPeerDevice
import com.example.data.model.StudySession
import com.example.data.model.StudyStatsCalculator
import com.example.data.preferences.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.util.ActiveTimerManager
import com.example.util.ActiveTimerSnapshot
import com.example.util.AppActionLogger
import com.example.util.BackupHelper
import com.example.util.LocalSyncHelper
import com.example.util.MotivationalQuotes
import com.example.util.NotificationHelper
import com.example.util.Quote
import com.example.util.TranslationHelper
import com.example.widget.ExamWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import java.util.Locale

enum class PomodoroState {
    IDLE,
    WORKING,
    WORK_PAUSED,
    BLOCK_FINISHED_PROMPT,
    LONG_BREAK_PROMPT,
    BREAKING,
    BREAK_PAUSED,
    SESSION_FINISHED_DAILY_STATS
}

enum class FocusModeTarget {
    TIMER,
    POMODORO,
    STOPWATCH
}

data class YKSExamCountdown(
    val title: String,
    val targetDate: Date,
    val daysRemaining: Long,
    val hoursRemaining: Long,
    val minutesRemaining: Long,
    val secondsRemaining: Long
)

data class StopwatchLap(
    val lapNumber: Int,
    val lapTimeMillis: Long,
    val totalTimeMillis: Long
)

typealias SubjectPracticeStat = com.example.data.model.SubjectPracticeStat
typealias DashboardStats = com.example.data.model.DashboardStats

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StudyRepository
    private val prefs = UserPreferences(application)

    init {
        AppActionLogger.installCrashHandler(application)
        val db = AppDatabase.getDatabase(application)
        repository = StudyRepository(db.studySessionDao(), db.customTimerDao(), db.dailyNoteDao())
        ActiveTimerManager.ensureInitialized(application)
    }

    private fun t(key: String): String = TranslationHelper.getTranslation(key, _userLanguage.value)

    // ==========================================
    // 0. USER PROFILE & PREFERENCES
    // ==========================================
    private val _userName = MutableStateFlow(prefs.userName)
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userTrack = MutableStateFlow(prefs.userTrack)
    val userTrack: StateFlow<String> = _userTrack.asStateFlow()

    private val _examTarget = MutableStateFlow(prefs.examTarget) // "YKS", "LGS", "KPSS"
    val examTarget: StateFlow<String> = _examTarget.asStateFlow()

    private val _ydtLanguage = MutableStateFlow(prefs.ydtLanguage)
    val ydtLanguage: StateFlow<String> = _ydtLanguage.asStateFlow()

    // Strictly derived from device system locale ("TR" if Turkish, "EN" for all other devices)
    private val _userLanguage = MutableStateFlow(UserPreferences.resolveSystemLanguage())
    val userLanguage: StateFlow<String> = _userLanguage.asStateFlow()

    private val _isAmoledModeEnabled = MutableStateFlow(prefs.isAmoledModeEnabled)
    val isAmoledModeEnabled: StateFlow<Boolean> = _isAmoledModeEnabled.asStateFlow()

    private val _isPitchBlackDarkEnabled = MutableStateFlow(prefs.isPitchBlackDarkEnabled)
    val isPitchBlackDarkEnabled: StateFlow<Boolean> = _isPitchBlackDarkEnabled.asStateFlow()

    private val _isOnboarded = MutableStateFlow(prefs.isOnboarded)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.appThemeMode)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _accentColorHex = MutableStateFlow(prefs.appAccentColorHex)
    val accentColorHex: StateFlow<String> = _accentColorHex.asStateFlow()

    private val _syncKey = MutableStateFlow(prefs.syncKey)
    val syncKey: StateFlow<String> = _syncKey.asStateFlow()

    private val _deviceUniqueCode = MutableStateFlow(prefs.deviceUniqueCode)
    val deviceUniqueCode: StateFlow<String> = _deviceUniqueCode.asStateFlow()

    private val _usageDaysCount = MutableStateFlow(prefs.usageDaysCount)
    val usageDaysCount: StateFlow<Int> = _usageDaysCount.asStateFlow()

    private val _userRole = MutableStateFlow(prefs.userRole)
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    private val _isParentPairingEnabled = MutableStateFlow(prefs.isParentPairingEnabled)
    val isParentPairingEnabled: StateFlow<Boolean> = _isParentPairingEnabled.asStateFlow()

    private val _savedTransferDevices = MutableStateFlow(SavedPeerDevice.decodeList(prefs.savedTransferDevicesJson))
    val savedTransferDevices: StateFlow<List<SavedPeerDevice>> = _savedTransferDevices.asStateFlow()

    private val _savedParentDevices = MutableStateFlow(SavedPeerDevice.decodeList(prefs.savedParentDevicesJson))
    val savedParentDevices: StateFlow<List<SavedPeerDevice>> = _savedParentDevices.asStateFlow()

    private val _discoveredPeers = MutableStateFlow<List<DiscoveredPeer>>(emptyList())
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = _discoveredPeers.asStateFlow()

    private val _isLanDiscoveryActive = MutableStateFlow(false)
    val isLanDiscoveryActive: StateFlow<Boolean> = _isLanDiscoveryActive.asStateFlow()

    private val _lanTransferStatus = MutableStateFlow("")
    val lanTransferStatus: StateFlow<String> = _lanTransferStatus.asStateFlow()

    private val _isSyncListenerRunning = MutableStateFlow(false)
    val isSyncListenerRunning: StateFlow<Boolean> = _isSyncListenerRunning.asStateFlow()

    private val _savedBooks = MutableStateFlow(SavedBookHelper.decodeList(prefs.savedBooksJson))
    val savedBooks: StateFlow<List<SavedBookEntry>> = _savedBooks.asStateFlow()

    private val _selectedDashboardExamIndex = MutableStateFlow(prefs.selectedDashboardExamIndex)
    val selectedDashboardExamIndex: StateFlow<Int> = _selectedDashboardExamIndex.asStateFlow()

    private val _selectedDashboardExamTitle = MutableStateFlow(prefs.selectedDashboardExamTitle)
    val selectedDashboardExamTitle: StateFlow<String> = _selectedDashboardExamTitle.asStateFlow()

    private val _selectedTimerHubTab = MutableStateFlow(prefs.selectedTimerHubTab)
    val selectedTimerHubTab: StateFlow<Int> = _selectedTimerHubTab.asStateFlow()

    private val _selectedMainTab = MutableStateFlow(prefs.selectedMainTab)
    val selectedMainTab: StateFlow<Int> = _selectedMainTab.asStateFlow()

    private val _focusLockedTarget = MutableStateFlow<FocusModeTarget?>(null)
    val focusLockedTarget: StateFlow<FocusModeTarget?> = _focusLockedTarget.asStateFlow()

    private val _openStopwatchSaveDialogTrigger = MutableStateFlow(0)
    val openStopwatchSaveDialogTrigger: StateFlow<Int> = _openStopwatchSaveDialogTrigger.asStateFlow()

    private val _parentSnapshot = MutableStateFlow<ParentStudentSnapshot?>(
        prefs.parentSyncedPayloadJson.takeIf { it.isNotBlank() }?.let { BackupHelper.parseParentStudentSnapshot(it) }
    )
    val parentSnapshot: StateFlow<ParentStudentSnapshot?> = _parentSnapshot.asStateFlow()

    private val _parentStats = MutableStateFlow(
        _parentSnapshot.value?.let {
            StudyStatsCalculator.compute(
                sessions = it.sessions,
                notes = it.dailyNotes,
                firstUsageTimestampMillis = it.firstUsageTimestamp.takeIf { ts -> ts > 0L }
            )
        } ?: DashboardStats()
    )
    val parentStats: StateFlow<DashboardStats> = _parentStats.asStateFlow()

    private val _parentWeeklyStats = MutableStateFlow(
        _parentSnapshot.value?.let {
            StudyStatsCalculator.computeWeekly(
                sessions = it.sessions,
                notes = it.dailyNotes,
                firstUsageTimestampMillis = it.firstUsageTimestamp.takeIf { ts -> ts > 0L }
            )
        } ?: DashboardStats()
    )
    val parentWeeklyStats: StateFlow<DashboardStats> = _parentWeeklyStats.asStateFlow()

    private var lanDiscoveryJob: Job? = null
    private var lanSyncServerJob: Job? = null

    private val _todayQuote = MutableStateFlow(
        MotivationalQuotes.getQuoteForUsageDay(prefs.usageDaysCount, _userLanguage.value)
    )
    val todayQuote: StateFlow<Quote> = _todayQuote.asStateFlow()

    fun syncSystemLanguage() {
        val resolved = UserPreferences.resolveSystemLanguage()
        if (_userLanguage.value != resolved) {
            _userLanguage.value = resolved
            _todayQuote.value = MotivationalQuotes.getQuoteForUsageDay(_usageDaysCount.value, resolved)
            refreshExamCountdowns()
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            val snap = ActiveTimerManager.ensureInitialized(getApplication())
            ActiveTimerManager.syncServiceAndNotification(getApplication(), snap)
        }
    }

    fun updateThemeMode(mode: String) {
        prefs.appThemeMode = mode
        _themeMode.value = mode
        AppActionLogger.logAction(getApplication(), "SETTINGS", "theme_mode=$mode")
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun updateAccentColorHex(hex: String) {
        prefs.appAccentColorHex = hex
        _accentColorHex.value = hex
        AppActionLogger.logAction(getApplication(), "SETTINGS", "accent_changed")
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun updateSyncKey(key: String) {
        prefs.syncKey = key
        _syncKey.value = key
        AppActionLogger.logAction(getApplication(), "SYNC", "key_regenerated")
    }

    fun refreshPreferencesState() {
        val lang = UserPreferences.resolveSystemLanguage()
        _themeMode.value = prefs.appThemeMode
        _accentColorHex.value = prefs.appAccentColorHex
        _syncKey.value = prefs.syncKey
        _deviceUniqueCode.value = prefs.deviceUniqueCode
        _usageDaysCount.value = prefs.usageDaysCount
        _userName.value = prefs.userName
        _userTrack.value = prefs.userTrack
        _examTarget.value = prefs.examTarget
        _ydtLanguage.value = prefs.ydtLanguage
        _userRole.value = prefs.userRole
        _isParentPairingEnabled.value = prefs.isParentPairingEnabled
        _savedTransferDevices.value = SavedPeerDevice.decodeList(prefs.savedTransferDevicesJson)
        _savedParentDevices.value = SavedPeerDevice.decodeList(prefs.savedParentDevicesJson)
        _savedBooks.value = SavedBookHelper.decodeList(prefs.savedBooksJson)
        _isOnboarded.value = prefs.isOnboarded
        _userLanguage.value = lang
        _isAmoledModeEnabled.value = prefs.isAmoledModeEnabled
        _isPitchBlackDarkEnabled.value = prefs.isPitchBlackDarkEnabled
        _selectedDashboardExamIndex.value = prefs.selectedDashboardExamIndex
        _selectedDashboardExamTitle.value = prefs.selectedDashboardExamTitle
        _selectedTimerHubTab.value = prefs.selectedTimerHubTab
        _selectedMainTab.value = prefs.selectedMainTab
        _todayQuote.value = MotivationalQuotes.getQuoteForUsageDay(prefs.usageDaysCount, lang)
        refreshParentSnapshotFromPrefs()
        normalizeStagesAndSubjectsForProfile(prefs.userTrack, prefs.examTarget, prefs.ydtLanguage)
        refreshExamCountdowns()
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun refreshParentSnapshotFromPrefs() {
        val raw = prefs.parentSyncedPayloadJson
        val parsed = if (raw.isNotBlank()) BackupHelper.parseParentStudentSnapshot(raw) else null
        _parentSnapshot.value = parsed
        _parentStats.value = parsed?.let {
            StudyStatsCalculator.compute(
                sessions = it.sessions,
                notes = it.dailyNotes,
                firstUsageTimestampMillis = it.firstUsageTimestamp.takeIf { ts -> ts > 0L }
            )
        } ?: DashboardStats()
        _parentWeeklyStats.value = parsed?.let {
            StudyStatsCalculator.computeWeekly(
                sessions = it.sessions,
                notes = it.dailyNotes,
                firstUsageTimestampMillis = it.firstUsageTimestamp.takeIf { ts -> ts > 0L }
            )
        } ?: DashboardStats()
    }

    fun setUserRole(role: String) {
        val clean = if (role == UserPreferences.ROLE_PARENT) UserPreferences.ROLE_PARENT else UserPreferences.ROLE_STUDENT
        prefs.userRole = clean
        _userRole.value = clean
        AppActionLogger.logAction(getApplication(), "PROFILE", "role=$clean")
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun setParentPairingEnabled(enabled: Boolean) {
        prefs.isParentPairingEnabled = enabled
        _isParentPairingEnabled.value = enabled
        AppActionLogger.logAction(getApplication(), "SETTINGS", "parent_pairing=$enabled")
    }

    fun updateYdtLanguage(language: String) {
        val clean = language.trim().ifBlank { "İngilizce" }
        prefs.ydtLanguage = clean
        _ydtLanguage.value = clean
        normalizeStagesAndSubjectsForProfile(_userTrack.value, _examTarget.value, clean)
        AppActionLogger.logAction(getApplication(), "PROFILE", "ydt_lang_updated")
    }

    fun saveUserProfile(
        name: String,
        track: String,
        target: String = _examTarget.value,
        ydtLanguage: String = _ydtLanguage.value,
        role: String = _userRole.value
    ) {
        val cleanRole = if (role == UserPreferences.ROLE_PARENT) UserPreferences.ROLE_PARENT else UserPreferences.ROLE_STUDENT
        val cleanYdt = ydtLanguage.trim().ifBlank { "İngilizce" }
        prefs.userName = name
        prefs.userTrack = track
        prefs.examTarget = target
        prefs.ydtLanguage = cleanYdt
        prefs.userRole = cleanRole
        prefs.isOnboarded = true

        _userName.value = name
        _userTrack.value = track
        _examTarget.value = target
        _ydtLanguage.value = cleanYdt
        _userRole.value = cleanRole
        _userLanguage.value = UserPreferences.resolveSystemLanguage()
        _isOnboarded.value = true

        normalizeStagesAndSubjectsForProfile(track, target, cleanYdt, forceDefaultSubject = true)
        AppActionLogger.logAction(getApplication(), "PROFILE", "save role=$cleanRole target=$target track=$track")
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun resetProfileAndOnboarding() {
        stopLanDiscovery()
        prefs.clear()
        _userName.value = ""
        _userTrack.value = "Sayısal"
        _examTarget.value = "YKS"
        _ydtLanguage.value = "İngilizce"
        _userRole.value = UserPreferences.ROLE_STUDENT
        _isParentPairingEnabled.value = false
        _savedTransferDevices.value = emptyList()
        _savedParentDevices.value = emptyList()
        _savedBooks.value = emptyList()
        _parentSnapshot.value = null
        _parentStats.value = DashboardStats()
        _parentWeeklyStats.value = DashboardStats()
        _deviceUniqueCode.value = prefs.deviceUniqueCode
        _isOnboarded.value = false
        _isAmoledModeEnabled.value = false
        _isPitchBlackDarkEnabled.value = false
        _selectedDashboardExamIndex.value = 0
        _selectedDashboardExamTitle.value = ""
        _selectedTimerHubTab.value = 0
        _selectedMainTab.value = 0
        _focusLockedTarget.value = null
        _userLanguage.value = UserPreferences.resolveSystemLanguage()
        normalizeStagesAndSubjectsForProfile("Sayısal", "YKS", "İngilizce", forceDefaultSubject = true)
        refreshExamCountdowns()
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    fun setAmoledModeEnabled(enabled: Boolean) {
        prefs.isAmoledModeEnabled = enabled
        _isAmoledModeEnabled.value = enabled
    }

    fun toggleFocusLock(target: FocusModeTarget) {
        _focusLockedTarget.value = if (_focusLockedTarget.value == target) null else target
    }

    fun clearFocusLock() {
        _focusLockedTarget.value = null
    }

    fun selectDashboardExam(index: Int, title: String) {
        val safeIdx = index.coerceAtLeast(0)
        prefs.selectedDashboardExamIndex = safeIdx
        prefs.selectedDashboardExamTitle = title
        _selectedDashboardExamIndex.value = safeIdx
        _selectedDashboardExamTitle.value = title
    }

    fun selectTimerHubTab(tab: Int) {
        val clamped = tab.coerceIn(0, 1)
        prefs.selectedTimerHubTab = clamped
        _selectedTimerHubTab.value = clamped
    }

    fun selectMainTab(tab: Int) {
        val clamped = tab.coerceIn(0, 2)
        prefs.selectedMainTab = clamped
        _selectedMainTab.value = clamped
    }

    fun setPitchBlackDarkEnabled(enabled: Boolean) {
        prefs.isPitchBlackDarkEnabled = enabled
        _isPitchBlackDarkEnabled.value = enabled
        AppActionLogger.logAction(getApplication(), "SETTINGS", "pitch_black_dark=$enabled")
        ExamWidgetUpdater.updateAllWidgets(getApplication())
    }

    // ==========================================
    // 0.5 SMART BOOK MEMORY & LAN PEER SYNC
    // ==========================================
    fun rememberSourceBook(bookName: String, examStage: String, subject: String) {
        val cleanBook = bookName.trim()
        val cleanSubj = subject.trim()
        if (cleanBook.isEmpty() || cleanSubj.isEmpty()) return
        val stage = if (_examTarget.value == "YKS") examStage else ""
        val updated = SavedBookHelper.upsertBook(_savedBooks.value, cleanBook, stage, cleanSubj)
        _savedBooks.value = updated
        prefs.savedBooksJson = SavedBookHelper.encodeList(updated)
    }

    fun getSuggestedBooksFor(examStage: String, subject: String, query: String = ""): List<String> {
        val stage = if (_examTarget.value == "YKS") examStage else ""
        return SavedBookHelper.filterSuggestions(_savedBooks.value, stage, subject, query)
    }

    private fun backfillSavedBooksFromDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = AppDatabase.getDatabase(getApplication())
                val notes = db.dailyNoteDao().getAllNotesOnce()
                val current = SavedBookHelper.decodeList(prefs.savedBooksJson)
                val merged = SavedBookHelper.backfillFromNotes(current, notes, prefs.examTarget)
                if (merged.size != current.size) {
                    prefs.savedBooksJson = SavedBookHelper.encodeList(merged)
                    _savedBooks.value = merged
                }
            } catch (_: Exception) {
            }
        }
    }

    fun startLanDiscovery(mode: String = "TRANSFER") {
        lanDiscoveryJob?.cancel()
        lanSyncServerJob?.cancel()
        _discoveredPeers.value = emptyList()
        _isLanDiscoveryActive.value = true
        val brand = LocalSyncHelper.getDeviceBrandOnly()
        val model = LocalSyncHelper.getDeviceModelOnly()
        val deviceCode = prefs.deviceUniqueCode
        _deviceUniqueCode.value = deviceCode
        val role = _userRole.value

        lanDiscoveryJob = viewModelScope.launch {
            try {
                LocalSyncHelper.runLanDiscoverySession(
                    brand = brand,
                    username = "",
                    role = role,
                    mode = mode,
                    model = model,
                    deviceCode = deviceCode
                ) { peers ->
                    _discoveredPeers.value = peers
                }
            } finally {
                _isLanDiscoveryActive.value = false
            }
        }

        lanSyncServerJob = viewModelScope.launch {
            _isSyncListenerRunning.value = true
            try {
                LocalSyncHelper.runSyncServer(
                    context = getApplication(),
                    expectedPin = _syncKey.value,
                    loopContinuously = true,
                    onParentSnapshotSaved = {
                        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                            refreshParentSnapshotFromPrefs()
                        }
                    },
                    onStatus = { msg ->
                        _lanTransferStatus.value = msg
                        if (msg.contains("tamamlandı", ignoreCase = true) ||
                            msg.contains("Sync complete", ignoreCase = true)
                        ) {
                            viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                refreshPreferencesState()
                            }
                        }
                    }
                )
            } finally {
                _isSyncListenerRunning.value = false
            }
        }
    }

    fun stopLanDiscovery() {
        lanDiscoveryJob?.cancel()
        lanSyncServerJob?.cancel()
        lanDiscoveryJob = null
        lanSyncServerJob = null
        _isLanDiscoveryActive.value = false
        _isSyncListenerRunning.value = false
    }

    fun clearLanTransferStatus() {
        _lanTransferStatus.value = ""
    }

    fun saveTransferDevice(peer: DiscoveredPeer) {
        val current = _savedTransferDevices.value.filterNot { it.id == peer.stableId }.toMutableList()
        current.add(
            0,
            SavedPeerDevice(
                id = peer.stableId,
                brand = peer.brand,
                model = peer.model,
                deviceCode = peer.deviceCode,
                username = peer.username,
                ip = peer.ip,
                port = peer.port,
                role = peer.role,
                lastSeenAt = System.currentTimeMillis()
            )
        )
        val trimmed = current.take(20)
        _savedTransferDevices.value = trimmed
        prefs.savedTransferDevicesJson = SavedPeerDevice.encodeList(trimmed)
        _toastEvent.value = t("device_saved_toast")
    }

    fun removeTransferDevice(id: String) {
        val updated = _savedTransferDevices.value.filterNot { it.id == id }
        _savedTransferDevices.value = updated
        prefs.savedTransferDevicesJson = SavedPeerDevice.encodeList(updated)
        _toastEvent.value = t("device_removed_toast")
    }

    fun saveParentDevice(peer: DiscoveredPeer) {
        val current = _savedParentDevices.value.filterNot { it.id == peer.stableId }.toMutableList()
        current.add(
            0,
            SavedPeerDevice(
                id = peer.stableId,
                brand = peer.brand,
                model = peer.model,
                deviceCode = peer.deviceCode,
                username = peer.username,
                ip = peer.ip,
                port = peer.port,
                role = peer.role,
                lastSeenAt = System.currentTimeMillis()
            )
        )
        val trimmed = current.take(20)
        _savedParentDevices.value = trimmed
        prefs.savedParentDevicesJson = SavedPeerDevice.encodeList(trimmed)
        _toastEvent.value = t("device_saved_toast")
    }

    fun removeParentDevice(id: String) {
        val updated = _savedParentDevices.value.filterNot { it.id == id }
        _savedParentDevices.value = updated
        prefs.savedParentDevicesJson = SavedPeerDevice.encodeList(updated)
        _toastEvent.value = t("device_removed_toast")
    }

    fun syncWithPeerAuto(ip: String, port: Int = LocalSyncHelper.DEFAULT_PORT, onDone: (String) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            LocalSyncHelper.connectAndSyncAutoPeer(
                context = getApplication(),
                hostIp = ip,
                port = port
            ) { status ->
                _lanTransferStatus.value = status
                onDone(status)
            }
            refreshPreferencesState()
        }
    }

    fun pushToParentPeer(
        ip: String,
        port: Int = LocalSyncHelper.DEFAULT_PORT,
        pinOrNull: String? = null,
        onDone: (String) -> Unit = {}
    ) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            LocalSyncHelper.pushStudentSnapshotToParent(
                context = getApplication(),
                hostIp = ip,
                port = port,
                pinOrNull = pinOrNull
            ) { status ->
                _lanTransferStatus.value = status
                onDone(status)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopLanDiscovery()
    }

    fun normalizeStageForTrack(
        stage: String,
        track: String = _userTrack.value,
        target: String = _examTarget.value,
        allowMixed: Boolean = false
    ): String {
        if (target != "YKS") return ""
        if (allowMixed && stage.equals("KARISIK", ignoreCase = true)) return "KARISIK"
        val isDil = track.trim().equals("Dil", ignoreCase = true)
        return when {
            stage.equals("TYT", ignoreCase = true) -> "TYT"
            stage.equals("YDT", ignoreCase = true) -> if (isDil) "YDT" else "AYT"
            stage.equals("AYT", ignoreCase = true) -> if (isDil) "YDT" else "AYT"
            else -> "TYT"
        }
    }

    private fun normalizeStagesAndSubjectsForProfile(
        track: String,
        target: String,
        ydtLang: String,
        forceDefaultSubject: Boolean = false
    ) {
        val pomoStage = normalizeStageForTrack(_pomoExamStage.value, track, target, allowMixed = true)
        val timerStage = normalizeStageForTrack(_timerExamStage.value, track, target, allowMixed = false)
        val swStage = normalizeStageForTrack(_stopwatchExamStage.value, track, target, allowMixed = true)

        _pomoExamStage.value = pomoStage
        _timerExamStage.value = timerStage
        _stopwatchExamStage.value = swStage

        val pomoValid = getPomodoroSubjects(track, target, pomoStage, ydtLang)
        if (pomoStage == "KARISIK") {
            _selectedSubject.value = "Karışık"
        } else if (forceDefaultSubject || _selectedSubject.value !in pomoValid) {
            _selectedSubject.value = pomoValid.firstOrNull() ?: "TYT Türkçe"
        }

        val timerValid = getSubjectsForTrackAndStage(track, target, timerStage, ydtLang)
        if (forceDefaultSubject || _branchMockSubject.value !in timerValid) {
            _branchMockSubject.value = timerValid.firstOrNull() ?: "TYT Türkçe"
        }
        if (forceDefaultSubject) {
            _isDenemeMode.value = false
            _timerSubject.value = timerValid.firstOrNull() ?: "TYT Türkçe"
        } else if (_timerSubject.value != "Branş Denemesi" && _timerSubject.value !in timerValid) {
            _timerSubject.value = timerValid.firstOrNull() ?: "TYT Türkçe"
        }

        val swValid = getSubjectsForTrackAndStage(track, target, swStage, ydtLang)
        if (swStage == "KARISIK") {
            _stopwatchSubject.value = "Karışık"
        } else if (forceDefaultSubject || _stopwatchSubject.value !in swValid) {
            _stopwatchSubject.value = swValid.firstOrNull() ?: "TYT Türkçe"
        }
    }

    fun getSubjectsForTrackAndStage(
        track: String = _userTrack.value,
        target: String = _examTarget.value,
        stage: String = "TYT",
        ydtLang: String = _ydtLanguage.value
    ): List<String> {
        if (target == "LGS") {
            return listOf(
                "LGS Türkçe",
                "LGS Paragraf",
                "LGS Matematik",
                "LGS Problem",
                "LGS Geometri",
                "LGS Fen Bilimleri",
                "LGS T.C. İnkılap Tarihi",
                "LGS Din Kültürü",
                "LGS İngilizce"
            )
        }
        if (target == "KPSS") {
            return listOf(
                "KPSS Türkçe",
                "KPSS Paragraf",
                "KPSS Matematik",
                "KPSS Problem",
                "KPSS Geometri",
                "KPSS Tarih",
                "KPSS Coğrafya",
                "KPSS Vatandaşlık & Güncel Bilgiler",
                "KPSS Eğitim Bilimleri / Alan"
            )
        }
        val effectiveStage = normalizeStageForTrack(stage, track, target, allowMixed = true)
        if (effectiveStage == "KARISIK") {
            val secondStage = if (track.trim().equals("Dil", ignoreCase = true)) "YDT" else "AYT"
            return (getSubjectsForTrackAndStage(track, target, "TYT", ydtLang) +
                getSubjectsForTrackAndStage(track, target, secondStage, ydtLang)).distinct()
        }
        if (effectiveStage == "TYT") {
            // TYT (Common for all YKS tracks): Türkçe, Paragraf, Matematik, Problem, Geometri, Sosyal Bilimler, Fen Bilimleri
            return listOf(
                "TYT Türkçe",
                "Paragraf",
                "Matematik",
                "Problem",
                "Geometri",
                "Tarih",
                "Coğrafya",
                "Felsefe",
                "Din Kültürü",
                "Fizik",
                "Kimya",
                "Biyoloji"
            )
        }
        // AYT / YDT stage strictly filtered by student's track (Geometri only for Sayısal in AYT; Paragraf/Problem never in AYT)
        return when (track) {
            "Sayısal" -> listOf(
                "Matematik",
                "Geometri",
                "Fizik",
                "Kimya",
                "Biyoloji"
            )
            "Eşit Ağırlık" -> listOf(
                "Matematik",
                "Türk Dili ve Edebiyatı",
                "Tarih",
                "Coğrafya"
            )
            "Sözel" -> listOf(
                "Türk Dili ve Edebiyatı",
                "Tarih",
                "Coğrafya",
                "Felsefe Grubu",
                "Psikoloji",
                "Sosyoloji",
                "Mantık",
                "Din Kültürü"
            )
            "Dil" -> {
                val langName = ydtLang.trim().ifBlank { "İngilizce" }
                listOf(
                    "YDT $langName",
                    "Kelime",
                    "Okuma & Çeviri",
                    "Dil Bilgisi"
                )
            }
            else -> listOf(
                "Matematik",
                "Geometri",
                "Fizik",
                "Kimya",
                "Biyoloji"
            )
        }
    }

    fun getSubjectsForTrack(
        track: String = _userTrack.value,
        target: String = _examTarget.value,
        ydtLang: String = _ydtLanguage.value
    ): List<String> {
        if (target != "YKS") {
            return getSubjectsForTrackAndStage(track, target, "", ydtLang)
        }
        val secondStage = if (track.trim().equals("Dil", ignoreCase = true)) "YDT" else "AYT"
        return (getSubjectsForTrackAndStage(track, target, "TYT", ydtLang) +
            getSubjectsForTrackAndStage(track, target, secondStage, ydtLang)).distinct()
    }

    fun getPomodoroSubjects(
        track: String = _userTrack.value,
        target: String = _examTarget.value,
        stage: String = _pomoExamStage.value,
        ydtLang: String = _ydtLanguage.value
    ): List<String> {
        return getSubjectsForTrackAndStage(track, target, stage, ydtLang)
            .filter { !it.contains("Deneme", ignoreCase = true) }
    }

    fun getTimerSubjects(
        track: String = _userTrack.value,
        target: String = _examTarget.value,
        stage: String = _timerExamStage.value,
        ydtLang: String = _ydtLanguage.value
    ): List<String> {
        val baseList = getSubjectsForTrackAndStage(track, target, stage, ydtLang).toMutableList()
        baseList.add(0, "Branş Denemesi")
        return baseList
    }

    // Toast Notification Event
    private val _toastEvent = MutableStateFlow<String?>(null)
    val toastEvent: StateFlow<String?> = _toastEvent.asStateFlow()

    fun clearToastEvent() {
        _toastEvent.value = null
    }

    // Room Flows
    val allSessions: StateFlow<List<StudySession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todaySessions: StateFlow<List<StudySession>> = repository.getTodaySessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotalSeconds: StateFlow<Long?> = repository.getTodayTotalSeconds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val todayTotalPomodoros: StateFlow<Int?> = repository.getTodayTotalPomodoros()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val customTimers: StateFlow<List<CustomTimer>> = repository.allCustomTimers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 1. COUNTDOWNS & CUSTOM TIMERS (Zero per-second churn on MainDashboard)
    // ==========================================
    private val _countdownsExpanded = MutableStateFlow(false)
    val countdownsExpanded: StateFlow<Boolean> = _countdownsExpanded.asStateFlow()

    fun toggleCountdownsExpanded() {
        _countdownsExpanded.value = !_countdownsExpanded.value
    }

    private val _tytDaysRemaining = MutableStateFlow(computeDaysLeft(prefs.customTytExamDate))
    val tytDaysRemaining: StateFlow<Long> = _tytDaysRemaining.asStateFlow()

    private val _aytDaysRemaining = MutableStateFlow(computeDaysLeft(prefs.customAytExamDate))
    val aytDaysRemaining: StateFlow<Long> = _aytDaysRemaining.asStateFlow()

    private val _lgsDaysRemaining = MutableStateFlow(computeDaysLeft(prefs.customLgsExamDate))
    val lgsDaysRemaining: StateFlow<Long> = _lgsDaysRemaining.asStateFlow()

    private val _kpssDaysRemaining = MutableStateFlow(computeDaysLeft(prefs.customKpssExamDate))
    val kpssDaysRemaining: StateFlow<Long> = _kpssDaysRemaining.asStateFlow()

    private val _msuDaysRemaining = MutableStateFlow(computeDaysLeft(prefs.customMsuExamDate))
    val msuDaysRemaining: StateFlow<Long> = _msuDaysRemaining.asStateFlow()

    private val _tytCountdown = MutableStateFlow<YKSExamCountdown?>(null)
    val tytCountdown: StateFlow<YKSExamCountdown?> = _tytCountdown.asStateFlow()

    private val _aytCountdown = MutableStateFlow<YKSExamCountdown?>(null)
    val aytCountdown: StateFlow<YKSExamCountdown?> = _aytCountdown.asStateFlow()

    private val _lgsCountdown = MutableStateFlow<YKSExamCountdown?>(null)
    val lgsCountdown: StateFlow<YKSExamCountdown?> = _lgsCountdown.asStateFlow()

    private val _kpssCountdown = MutableStateFlow<YKSExamCountdown?>(null)
    val kpssCountdown: StateFlow<YKSExamCountdown?> = _kpssCountdown.asStateFlow()

    private val _msuCountdown = MutableStateFlow<YKSExamCountdown?>(null)
    val msuCountdown: StateFlow<YKSExamCountdown?> = _msuCountdown.asStateFlow()

    private fun observePersistentTimers() {
        viewModelScope.launch {
            ActiveTimerManager.snapshot.collect { snap ->
                syncUiWithSnapshot(snap)
            }
        }
    }

    private fun syncUiWithSnapshot(snap: ActiveTimerSnapshot) {
        val now = System.currentTimeMillis()

        // Stopwatch sync
        val sw = snap.stopwatch
        val swNormStage = normalizeStageForTrack(sw.examStage, allowMixed = true)
        _stopwatchSubject.value = if (swNormStage == "KARISIK") "Karışık" else sw.subject
        _stopwatchExamStage.value = swNormStage
        _stopwatchIsRunning.value = sw.isRunning
        _stopwatchElapsedMillis.value = if (!sw.isRunning && !sw.active) 0L else sw.currentElapsedMillis(now)
        _stopwatchLaps.value = sw.laps.map {
            StopwatchLap(it.lapNumber, it.lapTimeMillis, it.totalTimeMillis)
        }
        if (sw.isRunning) {
            ensureStopwatchUiLoop()
        } else {
            stopwatchJob?.cancel()
            stopwatchJob = null
        }
        if (!sw.isRunning && !sw.active && _focusLockedTarget.value == FocusModeTarget.STOPWATCH) {
            _focusLockedTarget.value = null
        }

        // Custom Timer sync
        val tm = snap.customTimer
        val tmNormStage = normalizeStageForTrack(tm.examStage)
        _timerSubject.value = if (tm.subject == "Deneme Sınavı") "Branş Denemesi" else tm.subject
        _timerExamStage.value = tmNormStage
        _isDenemeMode.value = tm.isDenemeMode
        _denemeType.value = tm.denemeType
        _timerDurationMinutes.value = tm.durationMinutes
        _timerTotalSeconds.value = tm.totalSec
        _timerRemainingSeconds.value = tm.currentRemainingSeconds(now)
        _timerIsRunning.value = tm.isRunning
        _timerIsPaused.value = tm.isPaused
        if (tm.isRunning) {
            ensureCustomTimerUiLoop()
        } else {
            customCountdownJob?.cancel()
            customCountdownJob = null
        }
        if (!tm.isRunning && !tm.isPaused && _focusLockedTarget.value == FocusModeTarget.TIMER) {
            _focusLockedTarget.value = null
        }

        // Pomodoro sync
        val pm = snap.pomodoro
        val pState = try {
            PomodoroState.valueOf(pm.stateName)
        } catch (_: Exception) {
            PomodoroState.IDLE
        }
        val pmNormStage = normalizeStageForTrack(pm.examStage, allowMixed = true)
        _selectedSubject.value = if (pmNormStage == "KARISIK") "Karışık" else pm.subject
        _pomoExamStage.value = pmNormStage
        _workDurationMinutes.value = pm.workMinutes
        _breakDurationMinutes.value = pm.breakMinutes
        _longBreakMinutes.value = pm.longBreakMinutes
        _pomodoroState.value = pState
        _pomoTotalSeconds.value = pm.totalSec
        _pomoRemainingSeconds.value = pm.currentRemainingSeconds(now)
        _currentSessionCompletedBlocks.value = pm.completedBlocks
        _currentSessionTotalWorkSeconds.value = pm.currentTotalWorkSeconds(now)
        if (pState == PomodoroState.WORKING || pState == PomodoroState.BREAKING) {
            ensurePomodoroUiLoop()
        } else {
            pomodoroJob?.cancel()
            pomodoroJob = null
        }
        if (pState != PomodoroState.WORKING &&
            pState != PomodoroState.WORK_PAUSED &&
            pState != PomodoroState.BREAKING &&
            pState != PomodoroState.BREAK_PAUSED &&
            _focusLockedTarget.value == FocusModeTarget.POMODORO
        ) {
            _focusLockedTarget.value = null
        }
    }

    fun refreshExamCountdowns() {
        val now = System.currentTimeMillis()
        val tytDate = Date(prefs.customTytExamDate)
        val aytDate = Date(prefs.customAytExamDate)
        val lgsDate = Date(prefs.customLgsExamDate)
        val kpssDate = Date(prefs.customKpssExamDate)
        val msuDate = Date(prefs.customMsuExamDate)

        _tytDaysRemaining.value = computeDaysLeft(tytDate.time, now)
        _aytDaysRemaining.value = computeDaysLeft(aytDate.time, now)
        _lgsDaysRemaining.value = computeDaysLeft(lgsDate.time, now)
        _kpssDaysRemaining.value = computeDaysLeft(kpssDate.time, now)
        _msuDaysRemaining.value = computeDaysLeft(msuDate.time, now)

        val isTr = _userLanguage.value.equals("TR", ignoreCase = true)
        _tytCountdown.value = calculateCountdown("TYT 2027", tytDate, now)
        _aytCountdown.value = calculateCountdown("AYT 2027", aytDate, now)
        _lgsCountdown.value = calculateCountdown(if (isTr) "LGS Sınavı 2027" else "LGS Exam 2027", lgsDate, now)
        _kpssCountdown.value = calculateCountdown(if (isTr) "KPSS Lisans 2027" else "KPSS Exam 2027", kpssDate, now)
        _msuCountdown.value = calculateCountdown(if (isTr) "MSÜ Sınavı 2027" else "MSÜ Exam 2027", msuDate, now)
    }

    private fun computeDaysLeft(targetMillis: Long, now: Long = System.currentTimeMillis()): Long {
        val diff = (targetMillis - now).coerceAtLeast(0L)
        return diff / (24L * 3600L * 1000L)
    }

    fun updateExamDate(target: String, timestamp: Long) {
        when (target) {
            "TYT" -> prefs.customTytExamDate = timestamp
            "AYT" -> prefs.customAytExamDate = timestamp
            "LGS" -> prefs.customLgsExamDate = timestamp
            "KPSS" -> prefs.customKpssExamDate = timestamp
            "MSÜ" -> prefs.customMsuExamDate = timestamp
        }
        val rawParentPayload = prefs.parentSyncedPayloadJson
        if (prefs.userRole == UserPreferences.ROLE_PARENT && rawParentPayload.isNotBlank()) {
            try {
                val obj = org.json.JSONObject(rawParentPayload)
                when (target) {
                    "TYT" -> obj.put("customTytExamDate", timestamp)
                    "AYT" -> obj.put("customAytExamDate", timestamp)
                    "LGS" -> obj.put("customLgsExamDate", timestamp)
                    "KPSS" -> obj.put("customKpssExamDate", timestamp)
                    "MSÜ" -> obj.put("customMsuExamDate", timestamp)
                }
                prefs.parentSyncedPayloadJson = obj.toString()
                refreshParentSnapshotFromPrefs()
            } catch (_: Exception) {
            }
        }
        refreshExamCountdowns()
        ExamWidgetUpdater.updateAllWidgets(getApplication())
        AppActionLogger.logAction(getApplication(), "SETTINGS", "update_exam_date target=$target")
        _toastEvent.value = "$target ${t("toast_exam_date_updated")}"
    }

    private fun calculateCountdown(title: String, targetDate: Date, now: Long): YKSExamCountdown {
        val diffMillis = (targetDate.time - now).coerceAtLeast(0)
        val seconds = diffMillis / 1000
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60

        return YKSExamCountdown(
            title = title,
            targetDate = targetDate,
            daysRemaining = days,
            hoursRemaining = hours,
            minutesRemaining = minutes,
            secondsRemaining = secs
        )
    }

    fun addCustomTimer(title: String, targetTimestamp: Long) {
        viewModelScope.launch {
            repository.insertCustomTimer(CustomTimer(title = title, targetTimestamp = targetTimestamp))
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "CUSTOM_TIMER", "add")
        }
    }

    fun deleteCustomTimer(timer: CustomTimer) {
        viewModelScope.launch {
            repository.deleteCustomTimer(timer)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "CUSTOM_TIMER", "delete")
        }
    }

    // ==========================================
    // 3. POMODORO TIMER LOGIC
    // ==========================================
    private val _selectedSubject = MutableStateFlow(getSubjectsForTrack(prefs.userTrack, prefs.examTarget).firstOrNull() ?: "TYT Türkçe")
    val selectedSubject: StateFlow<String> = _selectedSubject.asStateFlow()

    private val _pomoExamStage = MutableStateFlow(if (prefs.examTarget == "YKS") "TYT" else "")
    val pomoExamStage: StateFlow<String> = _pomoExamStage.asStateFlow()

    private val _workDurationMinutes = MutableStateFlow(25)
    val workDurationMinutes: StateFlow<Int> = _workDurationMinutes.asStateFlow()

    private val _breakDurationMinutes = MutableStateFlow(5)
    val breakDurationMinutes: StateFlow<Int> = _breakDurationMinutes.asStateFlow()

    private val _pomodoroState = MutableStateFlow(PomodoroState.IDLE)
    val pomodoroState: StateFlow<PomodoroState> = _pomodoroState.asStateFlow()

    private val _pomoRemainingSeconds = MutableStateFlow(25 * 60)
    val pomoRemainingSeconds: StateFlow<Int> = _pomoRemainingSeconds.asStateFlow()

    private val _pomoTotalSeconds = MutableStateFlow(25 * 60)
    val pomoTotalSeconds: StateFlow<Int> = _pomoTotalSeconds.asStateFlow()

    private val _currentSessionCompletedBlocks = MutableStateFlow(0)
    val currentSessionCompletedBlocks: StateFlow<Int> = _currentSessionCompletedBlocks.asStateFlow()

    private val _currentSessionTotalWorkSeconds = MutableStateFlow(0L)
    val currentSessionTotalWorkSeconds: StateFlow<Long> = _currentSessionTotalWorkSeconds.asStateFlow()

    private var pomodoroJob: Job? = null

    fun selectSubject(subject: String) {
        _selectedSubject.value = subject
        ActiveTimerManager.updatePomodoroConfig(getApplication(), subject = subject)
    }

    fun selectPomoExamStage(stage: String) {
        val norm = normalizeStageForTrack(stage, allowMixed = true)
        _pomoExamStage.value = norm
        val nextSubject = if (norm == "KARISIK") {
            "Karışık"
        } else {
            val valid = getPomodoroSubjects(_userTrack.value, _examTarget.value, norm)
            if (_selectedSubject.value in valid) _selectedSubject.value else (valid.firstOrNull() ?: _selectedSubject.value)
        }
        _selectedSubject.value = nextSubject
        ActiveTimerManager.updatePomodoroConfig(getApplication(), subject = nextSubject, examStage = norm)
    }

    private val _longBreakMinutes = MutableStateFlow(45)
    val longBreakMinutes: StateFlow<Int> = _longBreakMinutes.asStateFlow()

    fun setLongBreakMinutes(mins: Int) {
        val clamped = mins.coerceIn(15, 60)
        _longBreakMinutes.value = clamped
        ActiveTimerManager.updatePomodoroConfig(getApplication(), longBreakMinutes = clamped)
    }

    fun setWorkAndBreakFromSlider(workMinutes: Int) {
        val clampedWork = workMinutes.coerceIn(20, 60)
        _workDurationMinutes.value = clampedWork
        val autoBreak = when {
            clampedWork <= 20 -> 3
            clampedWork <= 25 -> 5
            clampedWork <= 30 -> 6
            clampedWork <= 35 -> 7
            clampedWork <= 40 -> 8
            clampedWork <= 50 -> 10
            else -> 15
        }
        _breakDurationMinutes.value = autoBreak
        ActiveTimerManager.updatePomodoroConfig(
            getApplication(),
            workMinutes = clampedWork,
            breakMinutes = autoBreak
        )

        if (_pomodoroState.value == PomodoroState.IDLE) {
            _pomoRemainingSeconds.value = clampedWork * 60
            _pomoTotalSeconds.value = clampedWork * 60
        }
    }

    fun setWorkDuration(minutes: Int) {
        setWorkAndBreakFromSlider(minutes)
    }

    fun setBreakDuration(minutes: Int) {
        val clamped = minutes.coerceIn(3, 30)
        if (_pomodoroState.value == PomodoroState.IDLE) {
            _breakDurationMinutes.value = clamped
            ActiveTimerManager.updatePomodoroConfig(getApplication(), breakMinutes = clamped)
        }
    }

    fun startPomodoroWork() {
        ActiveTimerManager.startPomodoroWork(
            context = getApplication(),
            subject = _selectedSubject.value,
            workMinutes = _workDurationMinutes.value,
            breakMinutes = _breakDurationMinutes.value,
            examStage = _pomoExamStage.value
        )
    }

    fun pausePomodoroWork() {
        ActiveTimerManager.pausePomodoro(getApplication(), incrementEmergency = false)
    }

    fun pausePomodoroBreak() {
        ActiveTimerManager.pausePomodoro(getApplication(), incrementEmergency = false)
    }

    fun resumePomodoroWork() {
        ActiveTimerManager.resumePomodoroWork(getApplication())
    }

    private fun ensurePomodoroUiLoop() {
        if (pomodoroJob?.isActive == true) return
        pomodoroJob = viewModelScope.launch {
            while (true) {
                val snap = ActiveTimerManager.checkAndHandleExpirations(getApplication())
                val pm = snap.pomodoro
                if (pm.stateName != "WORKING" && pm.stateName != "BREAKING") {
                    break
                }
                val now = System.currentTimeMillis()
                _pomoRemainingSeconds.value = pm.currentRemainingSeconds(now)
                _currentSessionTotalWorkSeconds.value = pm.currentTotalWorkSeconds(now)
                delay(500L)
            }
        }
    }

    fun startLongBreakPhase() {
        ActiveTimerManager.startPomodoroBreak(
            context = getApplication(),
            isLongBreak = true,
            durationMinutes = _longBreakMinutes.value
        )
    }

    fun startBreakPhase() {
        ActiveTimerManager.startPomodoroBreak(
            context = getApplication(),
            isLongBreak = false,
            durationMinutes = _breakDurationMinutes.value
        )
    }

    fun resumeBreakTimer() {
        ActiveTimerManager.resumePomodoroBreak(getApplication())
    }

    fun finishPomodoroEarly() {
        val snap = ActiveTimerManager.ensureInitialized(getApplication())
        val totalWorkSec = snap.pomodoro.currentTotalWorkSeconds()
        val blocksDone = snap.pomodoro.completedBlocks
        val subj = snap.pomodoro.subject
        val stage = snap.pomodoro.examStage

        viewModelScope.launch {
            if (totalWorkSec > 0) {
                val session = StudySession(
                    subject = subj,
                    durationSeconds = totalWorkSec,
                    sessionType = "POMODORO",
                    blocksCompleted = blocksDone,
                    emergencyCount = 0,
                    examStage = stage
                )
                repository.insertSession(session)
                ExamWidgetUpdater.updateAllWidgets(getApplication())
            }
            AppActionLogger.logAction(getApplication(), "POMODORO", "finish_early dur=${totalWorkSec}s")
            ActiveTimerManager.finishPomodoroToDailyStats(getApplication())
        }
    }

    fun dismissDailyStatsAndResetPomodoro() {
        ActiveTimerManager.resetPomodoro(getApplication())
    }

    // ==========================================
    // 3.5 SAYAÇ (ONE-TIME TIMER & BRANŞ DENEMESİ) LOGIC
    // ==========================================
    private val _timerSubject = MutableStateFlow(getSubjectsForTrackAndStage(prefs.userTrack, prefs.examTarget, if (prefs.examTarget == "YKS") "TYT" else "", prefs.ydtLanguage).firstOrNull() ?: "TYT Türkçe")
    val timerSubject: StateFlow<String> = _timerSubject.asStateFlow()

    private val _timerExamStage = MutableStateFlow(if (prefs.examTarget == "YKS") "TYT" else "")
    val timerExamStage: StateFlow<String> = _timerExamStage.asStateFlow()

    private val _branchMockSubject = MutableStateFlow(getSubjectsForTrackAndStage(prefs.userTrack, prefs.examTarget, if (prefs.examTarget == "YKS") "TYT" else "", prefs.ydtLanguage).firstOrNull() ?: "TYT Türkçe")
    val branchMockSubject: StateFlow<String> = _branchMockSubject.asStateFlow()

    private val _isDenemeMode = MutableStateFlow(false)
    val isDenemeMode: StateFlow<Boolean> = _isDenemeMode.asStateFlow()

    private val _denemeType = MutableStateFlow("TYT Branş Denemesi - TYT Türkçe")
    val denemeType: StateFlow<String> = _denemeType.asStateFlow()

    private val _timerDurationMinutes = MutableStateFlow(45)
    val timerDurationMinutes: StateFlow<Int> = _timerDurationMinutes.asStateFlow()

    private val _timerRemainingSeconds = MutableStateFlow(45 * 60)
    val timerRemainingSeconds: StateFlow<Int> = _timerRemainingSeconds.asStateFlow()

    private val _timerTotalSeconds = MutableStateFlow(45 * 60)
    val timerTotalSeconds: StateFlow<Int> = _timerTotalSeconds.asStateFlow()

    private val _timerIsRunning = MutableStateFlow(false)
    val timerIsRunning: StateFlow<Boolean> = _timerIsRunning.asStateFlow()

    private val _timerIsPaused = MutableStateFlow(false)
    val timerIsPaused: StateFlow<Boolean> = _timerIsPaused.asStateFlow()

    private var customCountdownJob: Job? = null

    private fun buildBranchDenemeLabel(stage: String, branchSubj: String): String {
        return if (_examTarget.value == "YKS" && stage.isNotBlank()) {
            "$stage Branş Denemesi - $branchSubj"
        } else {
            "Branş Denemesi - $branchSubj"
        }
    }

    fun selectTimerSubject(subject: String) {
        _timerSubject.value = subject
        if (subject == "Branş Denemesi" || subject == "Deneme Sınavı") {
            _timerSubject.value = "Branş Denemesi"
            _isDenemeMode.value = true
            val stage = normalizeStageForTrack(_timerExamStage.value)
            _timerExamStage.value = stage
            val validBranchSubjects = getSubjectsForTrackAndStage(_userTrack.value, _examTarget.value, stage)
            val chosenBranch = if (_branchMockSubject.value in validBranchSubjects) {
                _branchMockSubject.value
            } else {
                validBranchSubjects.firstOrNull() ?: "TYT Türkçe"
            }
            _branchMockSubject.value = chosenBranch
            val clampedMinutes = _timerDurationMinutes.value.coerceIn(5, 90)
            if (_timerDurationMinutes.value != clampedMinutes) {
                _timerDurationMinutes.value = clampedMinutes
                if (!_timerIsRunning.value) {
                    _timerTotalSeconds.value = clampedMinutes * 60
                    _timerRemainingSeconds.value = clampedMinutes * 60
                }
            }
            val label = buildBranchDenemeLabel(stage, chosenBranch)
            _denemeType.value = label
            ActiveTimerManager.updateCustomTimerConfig(
                context = getApplication(),
                subject = "Branş Denemesi",
                isDenemeMode = true,
                denemeType = label,
                durationMinutes = clampedMinutes,
                examStage = stage
            )
        } else {
            _isDenemeMode.value = false
            setCustomTimerDuration(_timerDurationMinutes.value)
        }
    }

    fun selectBranchMockSubject(branchSubject: String) {
        _branchMockSubject.value = branchSubject
        val stage = normalizeStageForTrack(_timerExamStage.value)
        _timerExamStage.value = stage
        val clampedMinutes = _timerDurationMinutes.value.coerceIn(5, 90)
        val label = buildBranchDenemeLabel(stage, branchSubject)
        _denemeType.value = label
        ActiveTimerManager.updateCustomTimerConfig(
            context = getApplication(),
            subject = "Branş Denemesi",
            isDenemeMode = true,
            denemeType = label,
            durationMinutes = clampedMinutes,
            examStage = stage
        )
    }

    fun selectTimerExamStage(stage: String) {
        val norm = normalizeStageForTrack(stage)
        _timerExamStage.value = norm
        val valid = getSubjectsForTrackAndStage(_userTrack.value, _examTarget.value, norm)
        val nextBranch = if (_branchMockSubject.value in valid) _branchMockSubject.value else (valid.firstOrNull() ?: _branchMockSubject.value)
        _branchMockSubject.value = nextBranch
        if (_isDenemeMode.value || _timerSubject.value == "Branş Denemesi") {
            val clampedMinutes = _timerDurationMinutes.value.coerceIn(5, 90)
            val label = buildBranchDenemeLabel(norm, nextBranch)
            _denemeType.value = label
            ActiveTimerManager.updateCustomTimerConfig(
                context = getApplication(),
                subject = "Branş Denemesi",
                isDenemeMode = true,
                denemeType = label,
                durationMinutes = clampedMinutes,
                examStage = norm
            )
        } else {
            val nextSubj = if (_timerSubject.value in valid) _timerSubject.value else (valid.firstOrNull() ?: _timerSubject.value)
            _timerSubject.value = nextSubj
            ActiveTimerManager.updateCustomTimerConfig(
                context = getApplication(),
                subject = nextSubj,
                isDenemeMode = false,
                denemeType = _denemeType.value,
                durationMinutes = _timerDurationMinutes.value,
                examStage = norm
            )
        }
    }

    fun selectDenemeType(typeStr: String) {
        _denemeType.value = typeStr
        val clampedMinutes = _timerDurationMinutes.value.coerceIn(5, 90)
        if (_timerDurationMinutes.value != clampedMinutes) {
            _timerDurationMinutes.value = clampedMinutes
            if (!_timerIsRunning.value) {
                _timerTotalSeconds.value = clampedMinutes * 60
                _timerRemainingSeconds.value = clampedMinutes * 60
            }
        }
        ActiveTimerManager.updateCustomTimerConfig(
            context = getApplication(),
            subject = _timerSubject.value,
            isDenemeMode = true,
            denemeType = typeStr,
            durationMinutes = clampedMinutes,
            examStage = _timerExamStage.value
        )
    }

    fun setCustomTimerDuration(minutes: Int) {
        if (!_timerIsRunning.value) {
            val maxMin = if (_isDenemeMode.value) 90 else 240
            val clamped = minutes.coerceIn(5, maxMin)
            _timerDurationMinutes.value = clamped
            val totalSec = clamped * 60
            _timerTotalSeconds.value = totalSec
            _timerRemainingSeconds.value = totalSec
            ActiveTimerManager.updateCustomTimerConfig(
                context = getApplication(),
                subject = _timerSubject.value,
                isDenemeMode = _isDenemeMode.value,
                denemeType = _denemeType.value,
                durationMinutes = clamped,
                examStage = _timerExamStage.value
            )
        }
    }

    fun startCustomTimer() {
        ActiveTimerManager.startOrResumeCustomTimer(getApplication())
    }

    private fun ensureCustomTimerUiLoop() {
        if (customCountdownJob?.isActive == true) return
        customCountdownJob = viewModelScope.launch {
            while (true) {
                val snap = ActiveTimerManager.checkAndHandleExpirations(getApplication())
                val tm = snap.customTimer
                if (!tm.isRunning) break
                _timerRemainingSeconds.value = tm.currentRemainingSeconds()
                delay(500L)
            }
        }
    }

    fun pauseCustomTimer() {
        ActiveTimerManager.pauseCustomTimer(getApplication())
    }

    fun finishCustomTimerEarly() {
        val snap = ActiveTimerManager.ensureInitialized(getApplication())
        val tm = snap.customTimer
        val elapsedSec = (tm.totalSec - tm.currentRemainingSeconds()).coerceAtLeast(0)
        if (elapsedSec > 0) {
            viewModelScope.launch {
                val subjName = if (tm.isDenemeMode) tm.denemeType else tm.subject
                val sessType = if (tm.isDenemeMode) "DENEME" else "SAYAÇ"
                repository.insertSession(
                    StudySession(
                        subject = subjName,
                        durationSeconds = elapsedSec.toLong(),
                        sessionType = sessType,
                        blocksCompleted = 1,
                        emergencyCount = 0,
                        examStage = tm.examStage
                    )
                )
                ExamWidgetUpdater.updateAllWidgets(getApplication())
            }
        }
        AppActionLogger.logAction(getApplication(), "TIMER", "finish_early elapsed=${elapsedSec}s")
        ActiveTimerManager.resetCustomTimer(getApplication())
    }

    fun resetCustomTimer() {
        ActiveTimerManager.resetCustomTimer(getApplication())
    }

    // ==========================================
    // 4. KRONOMETRE (STOPWATCH) LOGIC
    // ==========================================
    private val _stopwatchSubject = MutableStateFlow(getSubjectsForTrack(prefs.userTrack, prefs.examTarget).firstOrNull() ?: "TYT Türkçe")
    val stopwatchSubject: StateFlow<String> = _stopwatchSubject.asStateFlow()

    private val _stopwatchExamStage = MutableStateFlow(if (prefs.examTarget == "YKS") "TYT" else "")
    val stopwatchExamStage: StateFlow<String> = _stopwatchExamStage.asStateFlow()

    private val _stopwatchIsRunning = MutableStateFlow(false)
    val stopwatchIsRunning: StateFlow<Boolean> = _stopwatchIsRunning.asStateFlow()

    private val _stopwatchElapsedMillis = MutableStateFlow(0L)
    val stopwatchElapsedMillis: StateFlow<Long> = _stopwatchElapsedMillis.asStateFlow()

    private val _stopwatchLaps = MutableStateFlow<List<StopwatchLap>>(emptyList())
    val stopwatchLaps: StateFlow<List<StopwatchLap>> = _stopwatchLaps.asStateFlow()

    private var stopwatchJob: Job? = null

    fun setStopwatchSubject(subject: String) {
        _stopwatchSubject.value = subject
        ActiveTimerManager.setStopwatchSubject(getApplication(), subject)
    }

    fun setStopwatchExamStage(stage: String) {
        val norm = normalizeStageForTrack(stage, allowMixed = true)
        _stopwatchExamStage.value = norm
        val nextSubject = if (norm == "KARISIK") {
            "Karışık"
        } else {
            val valid = getSubjectsForTrackAndStage(_userTrack.value, _examTarget.value, norm)
            if (_stopwatchSubject.value in valid) _stopwatchSubject.value else (valid.firstOrNull() ?: _stopwatchSubject.value)
        }
        _stopwatchSubject.value = nextSubject
        ActiveTimerManager.setStopwatchSubject(getApplication(), nextSubject)
        ActiveTimerManager.setStopwatchExamStage(getApplication(), norm)
    }

    fun startStopwatch() {
        ActiveTimerManager.startStopwatch(getApplication(), _stopwatchSubject.value)
    }

    private fun ensureStopwatchUiLoop() {
        if (stopwatchJob?.isActive == true) return
        stopwatchJob = viewModelScope.launch {
            while (true) {
                val snap = ActiveTimerManager.ensureInitialized(getApplication())
                if (!snap.stopwatch.isRunning) break
                _stopwatchElapsedMillis.value = snap.stopwatch.currentElapsedMillis()
                delay(80L)
            }
        }
    }

    fun pauseStopwatch() {
        ActiveTimerManager.pauseStopwatch(getApplication())
    }

    fun addStopwatchLap() {
        ActiveTimerManager.addStopwatchLap(getApplication())
    }

    fun requestOpenStopwatchSaveDialog() {
        val snap = ActiveTimerManager.ensureInitialized(getApplication())
        if (snap.stopwatch.isRunning) {
            ActiveTimerManager.pauseStopwatch(getApplication())
        }
        _openStopwatchSaveDialogTrigger.value += 1
    }

    fun consumeStopwatchSaveDialogRequest() {
        _openStopwatchSaveDialogTrigger.value = 0
    }

    fun stopAndSaveStopwatch(
        examStageOverride: String? = null,
        subjectOverride: String? = null
    ) {
        val snap = ActiveTimerManager.ensureInitialized(getApplication())
        val durationSec = snap.stopwatch.currentElapsedMillis() / 1000L
        val subj = subjectOverride?.takeIf { it.isNotBlank() } ?: snap.stopwatch.subject
        val stage = examStageOverride ?: snap.stopwatch.examStage
        stopwatchJob?.cancel()
        stopwatchJob = null
        _stopwatchIsRunning.value = false
        _stopwatchElapsedMillis.value = 0L
        _stopwatchLaps.value = emptyList()
        if (_focusLockedTarget.value == FocusModeTarget.STOPWATCH) {
            _focusLockedTarget.value = null
        }
        ActiveTimerManager.resetStopwatch(getApplication())

        if (durationSec > 0) {
            viewModelScope.launch {
                val session = StudySession(
                    subject = subj,
                    durationSeconds = durationSec,
                    sessionType = "KRONOMETRE",
                    blocksCompleted = 1,
                    emergencyCount = 0,
                    examStage = stage
                )
                repository.insertSession(session)
                ExamWidgetUpdater.updateAllWidgets(getApplication())
                AppActionLogger.logAction(getApplication(), "STOPWATCH", "saved dur=${durationSec}s")
                _toastEvent.value = t("toast_stopwatch_saved")
            }
        }
    }

    fun stopAndSaveStopwatchSegments(
        segments: List<Pair<String, Long>>,
        examStageOverride: String
    ) {
        val validSegments = segments.filter { it.second > 0L }
        stopwatchJob?.cancel()
        stopwatchJob = null
        _stopwatchIsRunning.value = false
        _stopwatchElapsedMillis.value = 0L
        _stopwatchLaps.value = emptyList()
        if (_focusLockedTarget.value == FocusModeTarget.STOPWATCH) {
            _focusLockedTarget.value = null
        }
        ActiveTimerManager.resetStopwatch(getApplication())

        if (validSegments.isNotEmpty()) {
            viewModelScope.launch {
                val baseTs = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    validSegments.forEachIndexed { idx, (subj, durSec) ->
                        val session = StudySession(
                            subject = subj.ifBlank { "Karışık" },
                            durationSeconds = durSec,
                            sessionType = "KRONOMETRE",
                            blocksCompleted = 1,
                            emergencyCount = 0,
                            timestamp = baseTs + idx.toLong(),
                            examStage = examStageOverride
                        )
                        repository.insertSession(session)
                    }
                }
                ExamWidgetUpdater.updateAllWidgets(getApplication())
                val totalDur = validSegments.sumOf { it.second }
                AppActionLogger.logAction(getApplication(), "STOPWATCH", "saved_segments count=${validSegments.size} dur=${totalDur}s")
                _toastEvent.value = t("toast_stopwatch_saved")
            }
        }
    }

    fun resetStopwatch() {
        stopwatchJob?.cancel()
        stopwatchJob = null
        _stopwatchIsRunning.value = false
        _stopwatchElapsedMillis.value = 0L
        _stopwatchLaps.value = emptyList()
        if (_focusLockedTarget.value == FocusModeTarget.STOPWATCH) {
            _focusLockedTarget.value = null
        }
        ActiveTimerManager.resetStopwatch(getApplication())
    }

    // ==========================================
    // 5. AJANDAM (AGENDA & DAILY NOTES)
    // ==========================================
    val allNotes: StateFlow<List<DailyNote>> = repository.allNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> = combine(allSessions, allNotes) { sessions, notes ->
        StudyStatsCalculator.compute(
            sessions = sessions,
            notes = notes,
            firstUsageTimestampMillis = prefs.firstUsageTimestamp
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    val weeklyDashboardStats: StateFlow<DashboardStats> = combine(allSessions, allNotes) { sessions, notes ->
        StudyStatsCalculator.computeWeekly(
            sessions = sessions,
            notes = notes,
            firstUsageTimestampMillis = prefs.firstUsageTimestamp
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    init {
        refreshExamCountdowns()
        observePersistentTimers()
        backfillSavedBooksFromDatabase()
    }

    fun getNoteForDate(dateStr: String): Flow<DailyNote?> = repository.getNoteForDate(dateStr)

    fun saveNoteForDate(dateStr: String, text: String, mood: String = "") {
        viewModelScope.launch {
            repository.saveNote(
                dateStr = dateStr,
                text = text,
                mood = mood,
                studyDurationMinutes = null,
                subjectBullets = null
            )
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            _toastEvent.value = t("toast_note_saved")
        }
    }

    fun saveAgendaEntry(
        dateStr: String,
        studyDurationMinutes: Int,
        subjectBullets: List<AgendaSubjectBullet>,
        noteText: String,
        mood: String = "",
        showToast: Boolean = true,
        isBulletAutoSave: Boolean = false
    ) {
        viewModelScope.launch {
            var updatedBooks = _savedBooks.value
            var booksChanged = false
            subjectBullets.forEach { b ->
                if (b.sourceBook.isNotBlank() && b.subject.isNotBlank()) {
                    val stage = if (_examTarget.value == "YKS") b.examStage else ""
                    updatedBooks = SavedBookHelper.upsertBook(updatedBooks, b.sourceBook, stage, b.subject)
                    booksChanged = true
                }
            }
            if (booksChanged) {
                _savedBooks.value = updatedBooks
                prefs.savedBooksJson = SavedBookHelper.encodeList(updatedBooks)
            }

            repository.saveAgendaEntry(
                dateStr = dateStr,
                studyDurationMinutes = studyDurationMinutes,
                subjectBullets = subjectBullets,
                noteText = noteText,
                mood = mood
            )
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "AGENDA", "save")
            if (showToast) {
                _toastEvent.value = if (isBulletAutoSave) t("agenda_auto_saved_toast") else t("agenda_saved_toast")
            }
        }
    }

    fun deleteNote(note: DailyNote) {
        viewModelScope.launch {
            repository.deleteNote(note)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "AGENDA", "delete")
        }
    }

    fun updateOrMoveAgendaBullet(
        sourceDateStr: String,
        bulletIndex: Int,
        updatedBullet: AgendaSubjectBullet,
        targetDateStr: String = sourceDateStr
    ) {
        viewModelScope.launch {
            if (updatedBullet.sourceBook.isNotBlank() && updatedBullet.subject.isNotBlank()) {
                rememberSourceBook(updatedBullet.sourceBook, updatedBullet.examStage, updatedBullet.subject)
            }
            repository.updateOrMoveBullet(
                sourceDateStr = sourceDateStr,
                bulletIndex = bulletIndex,
                updatedBullet = updatedBullet,
                targetDateStr = targetDateStr
            )
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(
                getApplication(),
                "AGENDA",
                if (targetDateStr != sourceDateStr) "move_bullet" else "edit_bullet"
            )
            _toastEvent.value = if (targetDateStr != sourceDateStr) {
                t("agenda_move_success_toast")
            } else {
                t("agenda_record_updated_toast")
            }
        }
    }

    fun moveEntireAgendaDay(sourceDateStr: String, targetDateStr: String) {
        if (sourceDateStr == targetDateStr) return
        viewModelScope.launch {
            repository.moveEntireDayToDate(sourceDateStr, targetDateStr)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "AGENDA", "move_day")
            _toastEvent.value = t("agenda_move_success_toast")
        }
    }

    fun updateDayMetadata(
        dateStr: String,
        studyDurationMinutes: Int,
        noteText: String,
        mood: String
    ) {
        viewModelScope.launch {
            repository.updateDayNoteAndDuration(dateStr, studyDurationMinutes, noteText, mood)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "AGENDA", "edit_day")
            _toastEvent.value = t("agenda_record_updated_toast")
        }
    }

    fun insertPastSession(
        subject: String,
        durationMinutes: Long,
        dateStr: String,
        sessionType: String = "KRONOMETRE"
    ) {
        viewModelScope.launch {
            val session = StudySession(
                subject = subject,
                durationSeconds = durationMinutes * 60,
                sessionType = sessionType,
                blocksCompleted = 1,
                dateFormatted = dateStr,
                timestamp = System.currentTimeMillis()
            )
            repository.insertSession(session)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            _toastEvent.value = t("toast_past_session_added")
        }
    }

    // ==========================================
    // 6. BACKUP & RESTORE / BUG REPORT / DATA MANAGEMENT
    // ==========================================
    suspend fun exportBackupJson(): String {
        val db = AppDatabase.getDatabase(getApplication())
        val freshSessions = db.studySessionDao().getAllSessionsOnce()
        val freshTimers = db.customTimerDao().getAllTimersOnce()
        val freshNotes = db.dailyNoteDao().getAllNotesOnce()
        return BackupHelper.createBackupJson(
            getApplication(),
            freshSessions,
            freshTimers,
            freshNotes
        )
    }

    suspend fun importBackupJson(jsonString: String): Boolean {
        val success = BackupHelper.restoreFromBackupJson(getApplication(), jsonString)
        if (success) {
            refreshPreferencesState()
            _isOnboarded.value = prefs.isOnboarded
        }
        return success
    }

    suspend fun generatePrivacySafeBugReport(): Pair<String, java.io.File?> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val ctx = getApplication<Application>()
        AppActionLogger.logAction(ctx, "REPORT", "generated")
        val text = AppActionLogger.buildPrivacySafeReport(ctx)
        val file = AppActionLogger.createReportFile(ctx, text)
        text to file
    }

    fun deleteSession(session: StudySession) {
        viewModelScope.launch {
            repository.deleteSession(session)
            ExamWidgetUpdater.updateAllWidgets(getApplication())
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            ActiveTimerManager.resetStopwatch(getApplication())
            ActiveTimerManager.resetCustomTimer(getApplication())
            ActiveTimerManager.resetPomodoro(getApplication())
            repository.clearAll()
            prefs.clear()
            _userName.value = ""
            _userTrack.value = "Sayısal"
            _examTarget.value = "YKS"
            _isOnboarded.value = false
            _selectedDashboardExamIndex.value = 0
            _selectedDashboardExamTitle.value = ""
            _selectedTimerHubTab.value = 0
            _selectedMainTab.value = 0
            _focusLockedTarget.value = null
            refreshExamCountdowns()
            ExamWidgetUpdater.updateAllWidgets(getApplication())
            AppActionLogger.logAction(getApplication(), "DATA", "clear_all")
        }
    }

    private fun formatTime(seconds: Int): String {
        val safeSec = seconds.coerceAtLeast(0)
        val m = safeSec / 60
        val s = safeSec % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }
}
