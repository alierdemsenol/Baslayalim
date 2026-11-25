package com.example.util

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.AgendaSubjectBullet
import com.example.data.model.CustomTimer
import com.example.data.model.DailyNote
import com.example.data.model.StudySession
import com.example.data.preferences.UserPreferences
import com.example.data.repository.StudyRepository
import com.example.widget.ExamWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupHelper {

    suspend fun createBackupJson(
        context: Context,
        sessions: List<StudySession>,
        customTimers: List<CustomTimer>,
        dailyNotes: List<DailyNote> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val prefs = UserPreferences(context)
        val root = JSONObject()

        root.put("version", 8)
        root.put("userName", prefs.userName)
        root.put("userTrack", prefs.userTrack)
        root.put("examTarget", prefs.examTarget)
        root.put("isAmoledModeEnabled", prefs.isAmoledModeEnabled)
        root.put("isPitchBlackDarkEnabled", prefs.isPitchBlackDarkEnabled)
        root.put("isOnboarded", prefs.isOnboarded)
        root.put("customTytExamDate", prefs.customTytExamDate)
        root.put("customAytExamDate", prefs.customAytExamDate)
        root.put("customMsuExamDate", prefs.customMsuExamDate)
        root.put("customLgsExamDate", prefs.customLgsExamDate)
        root.put("customKpssExamDate", prefs.customKpssExamDate)
        root.put("firstUsageTime", prefs.firstUsageTimestamp)
        root.put("appThemeMode", prefs.appThemeMode)
        root.put("appAccentColorHex", prefs.appAccentColorHex)
        root.put("syncKey", prefs.syncKey)
        root.put("ydtLanguage", prefs.ydtLanguage)
        root.put("isParentPairingEnabled", prefs.isParentPairingEnabled)
        root.put("savedBooksJson", prefs.savedBooksJson)
        root.put("deviceBrand", LocalSyncHelper.getDeviceBrandOnly())
        root.put("deviceModel", LocalSyncHelper.getDeviceModelOnly())

        val sessionsArray = JSONArray()
        for (session in sessions) {
            val sessionObj = JSONObject().apply {
                put("subject", session.subject)
                put("durationSeconds", session.durationSeconds)
                put("sessionType", session.sessionType)
                put("blocksCompleted", session.blocksCompleted)
                put("emergencyCount", session.emergencyCount)
                put("timestamp", session.timestamp)
                put("dateFormatted", session.dateFormatted)
                if (session.examStage.isNotBlank()) {
                    put("examStage", session.examStage)
                }
            }
            sessionsArray.put(sessionObj)
        }
        root.put("sessions", sessionsArray)

        val timersArray = JSONArray()
        for (timer in customTimers) {
            val timerObj = JSONObject().apply {
                put("title", timer.title)
                put("targetTimestamp", timer.targetTimestamp)
            }
            timersArray.put(timerObj)
        }
        root.put("customTimers", timersArray)

        val notesArray = JSONArray()
        for (note in dailyNotes) {
            val noteObj = JSONObject().apply {
                put("dateStr", note.dateStr)
                put("noteText", note.noteText)
                put("mood", note.mood)
                put("timestamp", note.timestamp)
                put("studyDurationMinutes", note.studyDurationMinutes)
                put("subjectBulletsJson", note.subjectBulletsJson)
            }
            notesArray.put(noteObj)
        }
        root.put("dailyNotes", notesArray)

        AppActionLogger.logAction(context, "BACKUP", "export")
        root.toString(2)
    }

    suspend fun restoreFromBackupJson(
        context: Context,
        jsonString: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val trimmedJson = jsonString.trim()
            if (trimmedJson.isEmpty()) return@withContext false

            val root = JSONObject(trimmedJson)
            val prefs = UserPreferences(context)
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val db = AppDatabase.getDatabase(context)

            val existingSessions = db.studySessionDao().getAllSessionsOnce()
            val existingNotes = db.dailyNoteDao().getAllNotesOnce()

            // Calculate activity scores so higher-activity side wins scalar conflicts while blank gaps fill from both sides
            val localActivityScore: Long =
                existingSessions.size * 10L +
                    (existingSessions.sumOf { it.durationSeconds } / 60L) +
                    existingNotes.size * 5L +
                    existingNotes.sumOf { it.studyDurationMinutes.toLong() + it.getSubjectBullets().size * 3L }

            val incomingSessionsArray = root.optJSONArray("sessions")
            var incomingActivityScore = 0L
            if (incomingSessionsArray != null) {
                incomingActivityScore += incomingSessionsArray.length() * 10L
                for (i in 0 until incomingSessionsArray.length()) {
                    val sObj = incomingSessionsArray.optJSONObject(i) ?: continue
                    incomingActivityScore += (sObj.optLong("durationSeconds", 0L).coerceAtLeast(0L) / 60L)
                }
            }
            val incomingNotesArray = root.optJSONArray("dailyNotes")
            if (incomingNotesArray != null) {
                incomingActivityScore += incomingNotesArray.length() * 5L
                for (i in 0 until incomingNotesArray.length()) {
                    val nObj = incomingNotesArray.optJSONObject(i) ?: continue
                    incomingActivityScore += nObj.optInt("studyDurationMinutes", 0).coerceAtLeast(0).toLong()
                    val bulletsRaw = nObj.optString("subjectBulletsJson", "[]")
                    incomingActivityScore += try {
                        JSONArray(bulletsRaw).length() * 3L
                    } catch (_: Exception) {
                        0L
                    }
                }
            }

            val incomingWinsConflicts = (!prefs.isOnboarded) || (localActivityScore == 0L) || (incomingActivityScore >= localActivityScore)

            if (root.has("userName")) {
                val incName = root.optString("userName", "").trim()
                if (incName.isNotEmpty() && (prefs.userName.isBlank() || incomingWinsConflicts)) {
                    prefs.userName = incName
                }
            }
            if (root.has("userTrack")) {
                val trk = root.optString("userTrack", "").trim()
                if (trk.isNotEmpty() && (prefs.userTrack.isBlank() || incomingWinsConflicts)) {
                    prefs.userTrack = trk
                }
            }
            if (root.has("examTarget")) {
                val tgt = root.optString("examTarget", "").trim()
                if ((tgt == "YKS" || tgt == "LGS" || tgt == "KPSS") && (prefs.examTarget.isBlank() || incomingWinsConflicts)) {
                    prefs.examTarget = tgt
                }
            }
            if (root.has("isAmoledModeEnabled") && incomingWinsConflicts) {
                prefs.isAmoledModeEnabled = root.optBoolean("isAmoledModeEnabled", false)
            }
            if (root.has("isPitchBlackDarkEnabled") && incomingWinsConflicts) {
                prefs.isPitchBlackDarkEnabled = root.optBoolean("isPitchBlackDarkEnabled", false)
            }
            if (root.has("customTytExamDate")) {
                val tyt = root.optLong("customTytExamDate", 0L)
                if (tyt > 0L && (incomingWinsConflicts || prefs.customTytExamDate == UserPreferences.defaultTytExamMillis())) {
                    prefs.customTytExamDate = tyt
                }
            }
            if (root.has("customAytExamDate")) {
                val ayt = root.optLong("customAytExamDate", 0L)
                if (ayt > 0L && (incomingWinsConflicts || prefs.customAytExamDate == UserPreferences.defaultAytExamMillis())) {
                    prefs.customAytExamDate = ayt
                }
            }
            if (root.has("customMsuExamDate")) {
                val msu = root.optLong("customMsuExamDate", 0L)
                if (msu > 0L && (incomingWinsConflicts || prefs.customMsuExamDate == UserPreferences.defaultMsuExamMillis())) {
                    prefs.customMsuExamDate = msu
                }
            }
            if (root.has("customLgsExamDate")) {
                val lgs = root.optLong("customLgsExamDate", 0L)
                if (lgs > 0L && (incomingWinsConflicts || prefs.customLgsExamDate == UserPreferences.defaultLgsExamMillis())) {
                    prefs.customLgsExamDate = lgs
                }
            }
            if (root.has("customKpssExamDate")) {
                val kpss = root.optLong("customKpssExamDate", 0L)
                if (kpss > 0L && (incomingWinsConflicts || prefs.customKpssExamDate == UserPreferences.defaultKpssExamMillis())) {
                    prefs.customKpssExamDate = kpss
                }
            }
            if (root.has("firstUsageTime")) {
                val fut = root.optLong("firstUsageTime", 0L)
                if (fut > 0L) {
                    val cur = prefs.firstUsageTimestamp
                    prefs.firstUsageTimestamp = if (cur > 0L) minOf(cur, fut) else fut
                }
            }
            if (root.has("appThemeMode") && incomingWinsConflicts) {
                val theme = root.optString("appThemeMode", "SYSTEM")
                if (theme == "SYSTEM" || theme == "LIGHT" || theme == "DARK") {
                    prefs.appThemeMode = theme
                }
            }
            if (root.has("appAccentColorHex") && incomingWinsConflicts) {
                val hex = root.optString("appAccentColorHex", "#FF2D75")
                if (hex.startsWith("#") && (hex.length == 7 || hex.length == 9)) {
                    prefs.appAccentColorHex = hex
                }
            }
            if (root.has("syncKey") && (!prefs.isOnboarded || localActivityScore == 0L)) {
                val key = root.optString("syncKey", "")
                if (key.length == 4 && key.all { it.isDigit() }) {
                    prefs.syncKey = key
                }
            }
            if (root.has("ydtLanguage")) {
                val ydt = root.optString("ydtLanguage", "").trim()
                if (ydt.isNotEmpty() && (prefs.ydtLanguage.isBlank() || incomingWinsConflicts)) {
                    prefs.ydtLanguage = ydt
                }
            }
            if (root.has("isParentPairingEnabled") && incomingWinsConflicts) {
                prefs.isParentPairingEnabled = root.optBoolean("isParentPairingEnabled", false)
            }
            if (root.has("savedBooksJson")) {
                val incomingBooks = com.example.data.model.SavedBookHelper.decodeList(
                    root.optString("savedBooksJson", "[]")
                )
                var mergedBooks = com.example.data.model.SavedBookHelper.decodeList(prefs.savedBooksJson)
                incomingBooks.forEach { b ->
                    mergedBooks = com.example.data.model.SavedBookHelper.upsertBook(
                        mergedBooks, b.bookName, b.examStage, b.subject, b.lastUsedAt
                    )
                }
                prefs.savedBooksJson = com.example.data.model.SavedBookHelper.encodeList(mergedBooks)
            }
            prefs.isOnboarded = true

            val newlyImportedSessions = mutableListOf<StudySession>()

            val sessionsArray = incomingSessionsArray
            if (sessionsArray != null) {
                val existingSignatures = existingSessions.map {
                    "${it.timestamp}_${it.subject}_${it.durationSeconds}_${it.sessionType}"
                }.toMutableSet()

                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.optJSONObject(i) ?: continue
                    val timestamp = sObj.optLong("timestamp", System.currentTimeMillis())
                    val subject = sObj.optString("subject", "Genel").ifBlank { "Genel" }
                    val durationSeconds = sObj.optLong("durationSeconds", 0L).coerceAtLeast(0L)
                    val sessionType = sObj.optString("sessionType", "POMODORO").ifBlank { "POMODORO" }
                    val examStage = sObj.optString("examStage", "").trim()
                    val rawDate = sObj.optString("dateFormatted", "").trim()
                    val dateFormatted = if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        rawDate
                    } else {
                        dateFmt.format(Date(timestamp))
                    }
                    val sig = "${timestamp}_${subject}_${durationSeconds}_${sessionType}"

                    if (!existingSignatures.contains(sig)) {
                        existingSignatures.add(sig)
                        val session = StudySession(
                            subject = subject,
                            durationSeconds = durationSeconds,
                            sessionType = sessionType,
                            blocksCompleted = sObj.optInt("blocksCompleted", 1).coerceAtLeast(0),
                            emergencyCount = sObj.optInt("emergencyCount", 0).coerceAtLeast(0),
                            timestamp = timestamp,
                            dateFormatted = dateFormatted,
                            examStage = examStage
                        )
                        db.studySessionDao().insertSession(session)
                        newlyImportedSessions.add(session)
                    }
                }
            }

            val timersArray = root.optJSONArray("customTimers")
            if (timersArray != null) {
                val existingTimers = db.customTimerDao().getAllTimersOnce()
                val existingTimerSigs = existingTimers.map {
                    "${it.title}_${it.targetTimestamp}"
                }.toMutableSet()

                for (i in 0 until timersArray.length()) {
                    val tObj = timersArray.optJSONObject(i) ?: continue
                    val title = tObj.optString("title", "Özel Sayaç").ifBlank { "Özel Sayaç" }
                    val targetTimestamp = tObj.optLong("targetTimestamp", System.currentTimeMillis())
                    val sig = "${title}_${targetTimestamp}"

                    if (!existingTimerSigs.contains(sig)) {
                        existingTimerSigs.add(sig)
                        db.customTimerDao().insertTimer(
                            CustomTimer(
                                title = title,
                                targetTimestamp = targetTimestamp
                            )
                        )
                    }
                }
            }

            val notesArray = incomingNotesArray
            if (notesArray != null) {
                val existingByDate = existingNotes.associateBy { it.dateStr }
                for (i in 0 until notesArray.length()) {
                    val nObj = notesArray.optJSONObject(i) ?: continue
                    val dateStr = nObj.optString("dateStr", "").trim()
                    val incomingNoteText = nObj.optString("noteText", "").trim()
                    val incomingMood = nObj.optString("mood", "").trim()
                    val incomingTs = nObj.optLong("timestamp", System.currentTimeMillis())
                    val incomingMinutes = nObj.optInt("studyDurationMinutes", 0).coerceAtLeast(0)
                    val incomingBulletsJson = nObj.optString("subjectBulletsJson", "[]").ifBlank { "[]" }

                    if (dateStr.isNotBlank()) {
                        val incomingNote = DailyNote(
                            dateStr = dateStr,
                            noteText = incomingNoteText,
                            mood = incomingMood,
                            timestamp = incomingTs,
                            studyDurationMinutes = incomingMinutes,
                            subjectBulletsJson = incomingBulletsJson
                        )

                        val existing = existingByDate[dateStr]
                        val mergedNote = if (existing == null) {
                            incomingNote
                        } else {
                            // If higher-activity device wins scalar conflicts, still union all non-duplicate bullets from both sides
                            val primaryFirst = if (incomingWinsConflicts) {
                                incomingNote.getSubjectBullets() + existing.getSubjectBullets()
                            } else {
                                existing.getSubjectBullets() + incomingNote.getSubjectBullets()
                            }
                            val deduped = linkedMapOf<String, AgendaSubjectBullet>()
                            primaryFirst.forEach { b ->
                                val key = b.uniqueKey()
                                val prev = deduped[key]
                                if (prev == null) {
                                    deduped[key] = b
                                } else {
                                    // Fill empty details mutually if one side lacks sourceBook or examStage
                                    deduped[key] = prev.copy(
                                        testCount = maxOf(prev.testCount, b.testCount),
                                        sourceBook = prev.sourceBook.ifBlank { b.sourceBook },
                                        examStage = prev.examStage.ifBlank { b.examStage },
                                        trialType = prev.trialType.ifBlank { b.trialType },
                                        totalQuestions = maxOf(prev.totalQuestions, b.totalQuestions),
                                        correctCount = maxOf(prev.correctCount, b.correctCount),
                                        wrongCount = maxOf(prev.wrongCount, b.wrongCount)
                                    )
                                }
                            }

                            val mergedText = when {
                                existing.noteText.isBlank() -> incomingNote.noteText
                                incomingNote.noteText.isBlank() -> existing.noteText
                                existing.noteText == incomingNote.noteText -> existing.noteText
                                incomingNote.noteText.contains(existing.noteText) -> incomingNote.noteText
                                existing.noteText.contains(incomingNote.noteText) -> existing.noteText
                                incomingWinsConflicts -> "${incomingNote.noteText}\n${existing.noteText}"
                                else -> "${existing.noteText}\n${incomingNote.noteText}"
                            }

                            val mergedMood = when {
                                existing.mood.isBlank() -> incomingNote.mood
                                incomingNote.mood.isBlank() -> existing.mood
                                incomingWinsConflicts -> incomingNote.mood
                                else -> existing.mood
                            }

                            DailyNote(
                                dateStr = dateStr,
                                noteText = mergedText,
                                mood = mergedMood,
                                timestamp = maxOf(existing.timestamp, incomingNote.timestamp),
                                studyDurationMinutes = maxOf(existing.studyDurationMinutes, incomingNote.studyDurationMinutes),
                                subjectBulletsJson = DailyNote.encodeBullets(deduped.values.toList())
                            )
                        }
                        db.dailyNoteDao().insertOrUpdateNote(mergedNote)
                    }
                }
            }

            // Backfill legacy backups (where dailyNotes were absent or lacked session entries) into Agenda safely
            newlyImportedSessions.forEach { session ->
                StudyRepository.appendSessionToAgenda(
                    db.studySessionDao(),
                    db.dailyNoteDao(),
                    session
                )
            }

            // Backfill source books from all notes
            val allNotesNow = db.dailyNoteDao().getAllNotesOnce()
            val existingBooks = com.example.data.model.SavedBookHelper.decodeList(prefs.savedBooksJson)
            val backfilledBooks = com.example.data.model.SavedBookHelper.backfillFromNotes(
                existingBooks,
                allNotesNow,
                prefs.examTarget
            )
            prefs.savedBooksJson = com.example.data.model.SavedBookHelper.encodeList(backfilledBooks)

            ExamWidgetUpdater.updateAllWidgets(context)
            AppActionLogger.logAction(context, "BACKUP", "restore_ok")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            AppActionLogger.logError(context, "BACKUP", "restore_err:${e.javaClass.simpleName}")
            false
        }
    }

    suspend fun createStudentSnapshotJson(
        context: Context,
        senderBrand: String,
        senderUsername: String,
        senderModel: String = "",
        senderDeviceCode: String = ""
    ): String = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(context)
        val freshSessions = db.studySessionDao().getAllSessionsOnce()
        val freshTimers = db.customTimerDao().getAllTimersOnce()
        val freshNotes = db.dailyNoteDao().getAllNotesOnce()
        val raw = createBackupJson(context, freshSessions, freshTimers, freshNotes)
        val obj = JSONObject(raw)
        obj.put("payloadType", "PARENT_SNAPSHOT_V1")
        obj.put("studentBrand", senderBrand)
        obj.put("studentModel", senderModel)
        obj.put("studentDeviceCode", senderDeviceCode)
        obj.put("studentUsername", senderUsername)
        obj.put("syncedAtMillis", System.currentTimeMillis())
        obj.toString()
    }

    fun parseParentStudentSnapshot(jsonString: String): com.example.data.model.ParentStudentSnapshot? {
        if (jsonString.isBlank()) return null
        return try {
            val root = JSONObject(jsonString.trim())
            val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

            val sessions = mutableListOf<StudySession>()
            val sessionsArray = root.optJSONArray("sessions")
            if (sessionsArray != null) {
                for (i in 0 until sessionsArray.length()) {
                    val sObj = sessionsArray.optJSONObject(i) ?: continue
                    val ts = sObj.optLong("timestamp", System.currentTimeMillis())
                    val rawDate = sObj.optString("dateFormatted", "").trim()
                    val dateFormatted = if (rawDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                        rawDate
                    } else {
                        dateFmt.format(Date(ts))
                    }
                    sessions.add(
                        StudySession(
                            id = i.toLong() + 1L,
                            subject = sObj.optString("subject", "Genel").ifBlank { "Genel" },
                            durationSeconds = sObj.optLong("durationSeconds", 0L).coerceAtLeast(0L),
                            sessionType = sObj.optString("sessionType", "POMODORO").ifBlank { "POMODORO" },
                            blocksCompleted = sObj.optInt("blocksCompleted", 1).coerceAtLeast(0),
                            emergencyCount = sObj.optInt("emergencyCount", 0).coerceAtLeast(0),
                            timestamp = ts,
                            dateFormatted = dateFormatted,
                            examStage = sObj.optString("examStage", "").trim()
                        )
                    )
                }
            }

            val timers = mutableListOf<CustomTimer>()
            val timersArray = root.optJSONArray("customTimers")
            if (timersArray != null) {
                for (i in 0 until timersArray.length()) {
                    val tObj = timersArray.optJSONObject(i) ?: continue
                    timers.add(
                        CustomTimer(
                            id = i.toLong() + 1L,
                            title = tObj.optString("title", "Özel Sayaç").ifBlank { "Özel Sayaç" },
                            targetTimestamp = tObj.optLong("targetTimestamp", System.currentTimeMillis())
                        )
                    )
                }
            }

            val notes = mutableListOf<DailyNote>()
            val notesArray = root.optJSONArray("dailyNotes")
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    val nObj = notesArray.optJSONObject(i) ?: continue
                    val dateStr = nObj.optString("dateStr", "").trim()
                    if (dateStr.isNotBlank()) {
                        notes.add(
                            DailyNote(
                                dateStr = dateStr,
                                noteText = nObj.optString("noteText", "").trim(),
                                mood = nObj.optString("mood", "").trim(),
                                timestamp = nObj.optLong("timestamp", System.currentTimeMillis()),
                                studyDurationMinutes = nObj.optInt("studyDurationMinutes", 0).coerceAtLeast(0),
                                subjectBulletsJson = nObj.optString("subjectBulletsJson", "[]").ifBlank { "[]" }
                            )
                        )
                    }
                }
            }

            com.example.data.model.ParentStudentSnapshot(
                studentName = root.optString("userName", "").trim(),
                studentUsername = root.optString("studentUsername", root.optString("networkUsername", "")).trim(),
                studentBrand = root.optString("studentBrand", "").trim(),
                studentModel = root.optString("studentModel", "").trim(),
                studentDeviceCode = root.optString("studentDeviceCode", "").trim(),
                examTarget = root.optString("examTarget", "YKS").ifBlank { "YKS" },
                userTrack = root.optString("userTrack", "Sayısal").ifBlank { "Sayısal" },
                ydtLanguage = root.optString("ydtLanguage", "İngilizce").ifBlank { "İngilizce" },
                syncedAtMillis = root.optLong("syncedAtMillis", System.currentTimeMillis()),
                customTytExamDate = root.optLong("customTytExamDate", UserPreferences.defaultTytExamMillis()),
                customAytExamDate = root.optLong("customAytExamDate", UserPreferences.defaultAytExamMillis()),
                customMsuExamDate = root.optLong("customMsuExamDate", UserPreferences.defaultMsuExamMillis()),
                customLgsExamDate = root.optLong("customLgsExamDate", UserPreferences.defaultLgsExamMillis()),
                customKpssExamDate = root.optLong("customKpssExamDate", UserPreferences.defaultKpssExamMillis()),
                sessions = sessions.sortedByDescending { it.timestamp },
                customTimers = timers,
                dailyNotes = notes.sortedByDescending { it.dateStr }
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun saveParentStudentSnapshot(
        context: Context,
        jsonString: String
    ): com.example.data.model.ParentStudentSnapshot? = withContext(Dispatchers.IO) {
        val parsed = parseParentStudentSnapshot(jsonString) ?: return@withContext null
        val prefs = UserPreferences(context)
        val stampedJson = try {
            val obj = JSONObject(jsonString.trim())
            if (!obj.has("syncedAtMillis") || obj.optLong("syncedAtMillis", 0L) <= 0L) {
                obj.put("syncedAtMillis", System.currentTimeMillis())
            }
            obj.toString()
        } catch (_: Exception) {
            jsonString.trim()
        }
        prefs.parentSyncedPayloadJson = stampedJson
        prefs.parentLastSyncAt = parsed.syncedAtMillis.takeIf { it > 0L } ?: System.currentTimeMillis()
        ExamWidgetUpdater.updateAllWidgets(context)
        AppActionLogger.logAction(context, "SYNC", "parent_snapshot_saved")
        parseParentStudentSnapshot(stampedJson) ?: parsed
    }
}
