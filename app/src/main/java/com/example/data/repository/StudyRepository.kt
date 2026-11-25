package com.example.data.repository

import com.example.data.dao.CustomTimerDao
import com.example.data.dao.DailyNoteDao
import com.example.data.dao.StudySessionDao
import com.example.data.model.AgendaSubjectBullet
import com.example.data.model.CustomTimer
import com.example.data.model.DailyNote
import com.example.data.model.StudySession
import com.example.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyRepository(
    private val sessionDao: StudySessionDao,
    private val timerDao: CustomTimerDao,
    private val noteDao: DailyNoteDao
) {

    val allSessions: Flow<List<StudySession>> = sessionDao.getAllSessions()
    val allCustomTimers: Flow<List<CustomTimer>> = timerDao.getAllCustomTimers()
    val allNotes: Flow<List<DailyNote>> = noteDao.getAllNotes()

    fun getTodaySessions(): Flow<List<StudySession>> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return sessionDao.getSessionsForDate(todayStr)
    }

    fun getTodayTotalSeconds(): Flow<Long?> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return sessionDao.getTotalStudySecondsForDate(todayStr)
    }

    fun getTodayTotalPomodoros(): Flow<Int?> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return sessionDao.getTotalPomodoroBlocksForDate(todayStr)
    }

    fun getNoteForDate(dateStr: String): Flow<DailyNote?> {
        return noteDao.getNoteForDate(dateStr)
    }

    suspend fun saveNote(
        dateStr: String,
        text: String,
        mood: String = "",
        studyDurationMinutes: Int? = null,
        subjectBullets: List<AgendaSubjectBullet>? = null
    ) {
        val existing = noteDao.getNoteForDateOnce(dateStr)
        val mergedText = when {
            text.isBlank() -> existing?.noteText ?: ""
            existing == null || existing.noteText.isBlank() -> text.trim()
            existing.noteText == text.trim() -> existing.noteText
            else -> "${existing.noteText}\n${text.trim()}"
        }
        noteDao.insertOrUpdateNote(
            DailyNote(
                dateStr = dateStr,
                noteText = mergedText,
                mood = mood.trim().ifBlank { existing?.mood ?: "" },
                studyDurationMinutes = (studyDurationMinutes ?: existing?.studyDurationMinutes ?: 0).coerceAtLeast(0),
                subjectBulletsJson = DailyNote.encodeBullets(
                    subjectBullets ?: existing?.getSubjectBullets() ?: emptyList()
                ),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun saveAgendaEntry(
        dateStr: String,
        studyDurationMinutes: Int,
        subjectBullets: List<AgendaSubjectBullet>,
        noteText: String,
        mood: String
    ) {
        noteDao.insertOrUpdateNote(
            DailyNote(
                dateStr = dateStr,
                noteText = noteText.trim(),
                mood = mood.trim(),
                studyDurationMinutes = studyDurationMinutes.coerceAtLeast(0),
                subjectBulletsJson = DailyNote.encodeBullets(subjectBullets),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteNote(note: DailyNote) {
        noteDao.deleteNote(note)
    }

    suspend fun updateDayNoteAndDuration(
        dateStr: String,
        studyDurationMinutes: Int,
        noteText: String,
        mood: String
    ) {
        val existing = noteDao.getNoteForDateOnce(dateStr) ?: return
        noteDao.insertOrUpdateNote(
            existing.copy(
                studyDurationMinutes = studyDurationMinutes.coerceAtLeast(0),
                noteText = noteText.trim(),
                mood = mood.trim(),
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateOrMoveBullet(
        sourceDateStr: String,
        bulletIndex: Int,
        updatedBullet: AgendaSubjectBullet,
        targetDateStr: String = sourceDateStr
    ) {
        val sourceNote = noteDao.getNoteForDateOnce(sourceDateStr) ?: return
        val sourceBullets = sourceNote.getSubjectBullets().toMutableList()
        if (bulletIndex !in sourceBullets.indices) return

        val oldBullet = sourceBullets[bulletIndex]
        if (targetDateStr == sourceDateStr) {
            sourceBullets[bulletIndex] = updatedBullet
            if (updatedBullet.sessionTimestamp > 0L) {
                sessionDao.updateSessionMeta(
                    ts = updatedBullet.sessionTimestamp,
                    subject = updatedBullet.subject,
                    examStage = updatedBullet.examStage
                )
            }
            noteDao.insertOrUpdateNote(
                sourceNote.copy(
                    subjectBulletsJson = DailyNote.encodeBullets(sourceBullets),
                    timestamp = System.currentTimeMillis()
                )
            )
            return
        }

        // Moving to a different date safely without overwriting target records
        sourceBullets.removeAt(bulletIndex)

        var movedSessionMinutes = 0
        if (oldBullet.sessionTimestamp > 0L) {
            val sourceBeforeSec = sessionDao.getTotalStudySecondsForDateOnce(sourceDateStr) ?: 0L
            sessionDao.moveSessionDate(oldBullet.sessionTimestamp, sourceDateStr, targetDateStr)
            sessionDao.updateSessionMeta(
                ts = updatedBullet.sessionTimestamp,
                subject = updatedBullet.subject,
                examStage = updatedBullet.examStage
            )
            val sourceAfterSec = sessionDao.getTotalStudySecondsForDateOnce(sourceDateStr) ?: 0L
            val diffSec = (sourceBeforeSec - sourceAfterSec).coerceAtLeast(0L)
            if (diffSec > 0L) {
                movedSessionMinutes = ((diffSec + 30L) / 60L).toInt().coerceAtLeast(1)
            }
        }

        val sourceRemainingTrackedSec = sessionDao.getTotalStudySecondsForDateOnce(sourceDateStr) ?: 0L
        val sourceRemainingTrackedMin = if (sourceRemainingTrackedSec > 0L) {
            ((sourceRemainingTrackedSec + 30L) / 60L).toInt().coerceAtLeast(1)
        } else 0
        val newSourceMins = maxOf(
            sourceRemainingTrackedMin,
            (sourceNote.studyDurationMinutes - movedSessionMinutes).coerceAtLeast(0)
        )

        if (sourceBullets.isEmpty() && sourceNote.noteText.isBlank() && newSourceMins <= 0) {
            noteDao.deleteNote(sourceNote)
        } else {
            noteDao.insertOrUpdateNote(
                sourceNote.copy(
                    studyDurationMinutes = newSourceMins,
                    subjectBulletsJson = DailyNote.encodeBullets(sourceBullets),
                    timestamp = System.currentTimeMillis()
                )
            )
        }

        // Merge into target date cleanly without colliding
        val targetNote = noteDao.getNoteForDateOnce(targetDateStr)
        val mergedTargetBullets = linkedMapOf<String, AgendaSubjectBullet>()
        targetNote?.getSubjectBullets()?.forEach { b ->
            mergedTargetBullets[b.uniqueKey()] = b
        }
        mergedTargetBullets[updatedBullet.uniqueKey()] = updatedBullet

        val targetTrackedSec = sessionDao.getTotalStudySecondsForDateOnce(targetDateStr) ?: 0L
        val targetTrackedMin = if (targetTrackedSec > 0L) {
            ((targetTrackedSec + 30L) / 60L).toInt().coerceAtLeast(1)
        } else 0
        val newTargetMins = maxOf(
            targetTrackedMin,
            (targetNote?.studyDurationMinutes ?: 0) + movedSessionMinutes
        )

        noteDao.insertOrUpdateNote(
            DailyNote(
                dateStr = targetDateStr,
                noteText = targetNote?.noteText ?: "",
                mood = targetNote?.mood ?: "",
                timestamp = System.currentTimeMillis(),
                studyDurationMinutes = newTargetMins,
                subjectBulletsJson = DailyNote.encodeBullets(mergedTargetBullets.values.toList())
            )
        )
    }

    suspend fun moveEntireDayToDate(
        sourceDateStr: String,
        targetDateStr: String
    ) {
        if (sourceDateStr == targetDateStr) return
        val sourceNote = noteDao.getNoteForDateOnce(sourceDateStr)
        val targetNote = noteDao.getNoteForDateOnce(targetDateStr)

        sessionDao.moveAllSessionsToDate(sourceDateStr, targetDateStr)

        val combined = (targetNote?.getSubjectBullets() ?: emptyList()) +
            (sourceNote?.getSubjectBullets() ?: emptyList())
        val deduped = linkedMapOf<String, AgendaSubjectBullet>()
        combined.forEach { b ->
            val k = b.uniqueKey()
            if (!deduped.containsKey(k)) {
                deduped[k] = b
            }
        }

        val mergedText = when {
            targetNote == null || targetNote.noteText.isBlank() -> sourceNote?.noteText ?: ""
            sourceNote == null || sourceNote.noteText.isBlank() -> targetNote.noteText
            targetNote.noteText == sourceNote.noteText -> targetNote.noteText
            targetNote.noteText.contains(sourceNote.noteText) -> targetNote.noteText
            else -> "${targetNote.noteText}\n${sourceNote.noteText}"
        }

        val mergedMood = when {
            !targetNote?.mood.isNullOrBlank() -> targetNote!!.mood
            !sourceNote?.mood.isNullOrBlank() -> sourceNote!!.mood
            else -> ""
        }

        val targetTrackedSec = sessionDao.getTotalStudySecondsForDateOnce(targetDateStr) ?: 0L
        val targetTrackedMin = if (targetTrackedSec > 0L) {
            ((targetTrackedSec + 30L) / 60L).toInt().coerceAtLeast(1)
        } else 0
        val combinedNoteMins = (targetNote?.studyDurationMinutes ?: 0) + (sourceNote?.studyDurationMinutes ?: 0)

        noteDao.insertOrUpdateNote(
            DailyNote(
                dateStr = targetDateStr,
                noteText = mergedText.trim(),
                mood = mergedMood,
                timestamp = System.currentTimeMillis(),
                studyDurationMinutes = maxOf(targetTrackedMin, combinedNoteMins),
                subjectBulletsJson = DailyNote.encodeBullets(deduped.values.toList())
            )
        )

        if (sourceNote != null) {
            noteDao.deleteNote(sourceNote)
        }
    }

    suspend fun insertSession(session: StudySession): Long {
        val id = sessionDao.insertSession(session)
        appendSessionToAgenda(sessionDao, noteDao, session)
        return id
    }

    suspend fun deleteSession(session: StudySession) {
        sessionDao.deleteSession(session)
    }

    suspend fun insertCustomTimer(timer: CustomTimer): Long {
        return timerDao.insertTimer(timer)
    }

    suspend fun deleteCustomTimer(timer: CustomTimer) {
        timerDao.deleteTimer(timer)
    }

    suspend fun clearAll() {
        sessionDao.clearAllSessions()
        timerDao.clearAllTimers()
        noteDao.clearAllNotes()
    }

    companion object {
        suspend fun appendSessionToAgenda(
            sessionDao: StudySessionDao,
            noteDao: DailyNoteDao,
            session: StudySession
        ) {
            if (session.durationSeconds <= 0L) return
            val dateStr = session.dateFormatted.ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(session.timestamp))
            }
            val existing = noteDao.getNoteForDateOnce(dateStr)
            val existingBullets = existing?.getSubjectBullets()?.toMutableList() ?: mutableListOf()

            val bullet = buildTimerBulletForSession(session)
            val alreadyRecorded = existingBullets.any { b ->
                b.studyType == AgendaSubjectBullet.STUDY_TYPE_TIMER &&
                    b.sessionTimestamp > 0L &&
                    b.sessionTimestamp == session.timestamp &&
                    b.subject == bullet.subject
            }
            if (!alreadyRecorded) {
                existingBullets.add(bullet)
            }

            val totalTrackedSec = sessionDao.getTotalStudySecondsForDateOnce(dateStr) ?: session.durationSeconds
            val totalTrackedMin = ((totalTrackedSec + 30L) / 60L).toInt().coerceAtLeast(
                if (totalTrackedSec > 0L) 1 else 0
            )
            val mergedDuration = maxOf(existing?.studyDurationMinutes ?: 0, totalTrackedMin)

            noteDao.insertOrUpdateNote(
                DailyNote(
                    dateStr = dateStr,
                    noteText = existing?.noteText ?: "",
                    mood = existing?.mood ?: "",
                    timestamp = maxOf(existing?.timestamp ?: 0L, session.timestamp),
                    studyDurationMinutes = mergedDuration,
                    subjectBulletsJson = DailyNote.encodeBullets(existingBullets)
                )
            )
        }

        fun buildTimerBulletForSession(session: StudySession): AgendaSubjectBullet {
            val isTr = UserPreferences.resolveSystemLanguage() == "TR"
            val mins = ((session.durationSeconds + 30L) / 60L).toInt()
            val secs = (session.durationSeconds % 60L).toInt()
            val durStr = when {
                mins >= 60 -> {
                    val h = mins / 60
                    val m = mins % 60
                    if (isTr) "${h} sa ${m} dk" else "${h}h ${m}m"
                }
                mins > 0 -> if (isTr) "$mins dk" else "${mins}m"
                else -> if (isTr) "$secs sn" else "${secs}s"
            }

            val typeLabel = when (session.sessionType.uppercase(Locale.US)) {
                "POMODORO" -> "Pomodoro"
                "DENEME" -> if (isTr) "Deneme Sınavı" else "Mock Exam"
                "SAYAÇ" -> if (isTr) "Sayaç" else "Timer"
                "KRONOMETRE" -> if (isTr) "Kronometre" else "Stopwatch"
                "FİRAR" -> if (isTr) "Yarım Kalan (Firar)" else "Abandoned"
                else -> session.sessionType
            }

            val blockSuffix = if (session.sessionType.equals("POMODORO", ignoreCase = true) && session.blocksCompleted > 0) {
                if (isTr) " • ${session.blocksCompleted} blok" else " • ${session.blocksCompleted} blk"
            } else ""

            return AgendaSubjectBullet(
                subject = session.subject,
                topic = "$typeLabel • $durStr$blockSuffix",
                studyType = AgendaSubjectBullet.STUDY_TYPE_TIMER,
                testCount = 0,
                sessionTimestamp = session.timestamp,
                examStage = session.examStage
            )
        }
    }
}
