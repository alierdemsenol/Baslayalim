package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SubjectPracticeStat(
    val subject: String,
    val totalTests: Int,
    val entryCount: Int,
    val examStage: String = ""
)

data class SubjectTimeStat(
    val subject: String,
    val examStage: String,
    val totalSeconds: Long
)

data class StageStat(
    val stage: String, // "TYT", "AYT", "YDT", "GENEL"
    val totalMinutes: Int,
    val sessionCount: Int,
    val testsSolved: Int,
    val topicsReviewed: Int,
    val denemesSolved: Int
)

data class DashboardStats(
    val averageDailyMinutes: Int = 0,
    val todayTotalMinutes: Int = 0,
    val totalAllTimeMinutes: Int = 0,
    val activeDaysCount: Int = 0,
    val totalTestsSolved: Int = 0,
    val totalTopicsReviewed: Int = 0,
    val totalDenemesSolved: Int = 0,
    val practiceBySubject: List<SubjectPracticeStat> = emptyList(),
    val practiceBySubjectAndStage: List<SubjectPracticeStat> = emptyList(),
    val timeBySubjectAndStage: List<SubjectTimeStat> = emptyList(),
    val stageStats: List<StageStat> = emptyList()
)

object StudyStatsCalculator {

    fun resolveStageLabel(rawStage: String, subject: String = ""): String {
        val s = rawStage.trim().uppercase(Locale.US)
        if (s == "TYT" || s == "AYT" || s == "YDT") return s
        val subUpper = subject.trim().uppercase(Locale.US)
        return when {
            subUpper.startsWith("TYT ") || subUpper == "PARAGRAF" || subUpper == "PROBLEM" -> "TYT"
            subUpper.startsWith("YDT ") -> "YDT"
            subUpper.startsWith("AYT ") -> "AYT"
            else -> "GENEL"
        }
    }

    fun currentWeekDateRangeKeys(nowMillis: Long = System.currentTimeMillis()): Pair<String, String> {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val diffFromMonday = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        cal.add(Calendar.DAY_OF_YEAR, -diffFromMonday)
        val mondayKey = dateFmt.format(cal.time)
        cal.add(Calendar.DAY_OF_YEAR, 6)
        val sundayKey = dateFmt.format(cal.time)
        return mondayKey to sundayKey
    }

    fun computeWeekly(
        sessions: List<StudySession>,
        notes: List<DailyNote>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardStats {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val (weekStart, weekEnd) = currentWeekDateRangeKeys(nowMillis)
        val weeklySessions = sessions.filter { s ->
            val key = s.dateFormatted.ifBlank { dateFmt.format(Date(s.timestamp)) }
            key >= weekStart && key <= weekEnd
        }
        val weeklyNotes = notes.filter { n ->
            n.dateStr >= weekStart && n.dateStr <= weekEnd
        }
        return compute(weeklySessions, weeklyNotes, nowMillis)
    }

    fun compute(
        sessions: List<StudySession>,
        notes: List<DailyNote>,
        nowMillis: Long = System.currentTimeMillis()
    ): DashboardStats {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateFmt.format(Date(nowMillis))

        val sessionSecondsByDate = mutableMapOf<String, Long>()
        val stageSessionSeconds = mutableMapOf<String, Long>()
        val stageSessionCount = mutableMapOf<String, Int>()
        val subjectStageSeconds = mutableMapOf<Pair<String, String>, Long>()
        val sessionDatesWithStage = mutableSetOf<Pair<String, String>>()

        sessions.forEach { s ->
            val d = s.dateFormatted.ifBlank { dateFmt.format(Date(s.timestamp)) }
            val stage = resolveStageLabel(s.examStage, s.subject)
            if (s.durationSeconds > 0L) {
                sessionSecondsByDate[d] = (sessionSecondsByDate[d] ?: 0L) + s.durationSeconds
                stageSessionSeconds[stage] = (stageSessionSeconds[stage] ?: 0L) + s.durationSeconds
                stageSessionCount[stage] = (stageSessionCount[stage] ?: 0) + 1
                val subKey = s.subject.trim().ifBlank { "Genel" } to stage
                subjectStageSeconds[subKey] = (subjectStageSeconds[subKey] ?: 0L) + s.durationSeconds
                sessionDatesWithStage.add(d to stage)
            }
        }

        val agendaMinutesByDate = mutableMapOf<String, Int>()
        val practiceTestsBySubject = mutableMapOf<String, Pair<Int, Int>>()
        val practiceTestsBySubjectStage = mutableMapOf<Pair<String, String>, Pair<Int, Int>>()
        val stageTests = mutableMapOf<String, Int>()
        val stageReviewed = mutableMapOf<String, Int>()
        val stageDenemes = mutableMapOf<String, Int>()
        val stageManualAgendaMins = mutableMapOf<String, Int>()

        var totalTests = 0
        var totalReviewed = 0
        var totalDenemes = 0

        notes.forEach { note ->
            val bullets = note.getSubjectBullets()
            if (note.studyDurationMinutes > 0 || bullets.isNotEmpty()) {
                agendaMinutesByDate[note.dateStr] = maxOf(
                    agendaMinutesByDate[note.dateStr] ?: 0,
                    note.studyDurationMinutes
                )
            }
            val stagesInNote = mutableSetOf<String>()
            bullets.forEach { b ->
                val stage = if (b.studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME &&
                    b.trialType.uppercase(Locale.US) in listOf("TYT", "AYT", "YDT")
                ) {
                    b.trialType.uppercase(Locale.US)
                } else {
                    resolveStageLabel(b.examStage, b.subject)
                }
                stagesInNote.add(stage)

                when (b.studyType) {
                    AgendaSubjectBullet.STUDY_TYPE_PRACTICE -> {
                        val tests = b.testCount.coerceAtLeast(1)
                        totalTests += tests
                        totalReviewed += 1
                        stageTests[stage] = (stageTests[stage] ?: 0) + tests
                        stageReviewed[stage] = (stageReviewed[stage] ?: 0) + 1

                        val subj = b.subject.trim().ifBlank { "Genel" }
                        val cur = practiceTestsBySubject[subj] ?: (0 to 0)
                        practiceTestsBySubject[subj] = (cur.first + tests) to (cur.second + 1)

                        val curSt = practiceTestsBySubjectStage[subj to stage] ?: (0 to 0)
                        practiceTestsBySubjectStage[subj to stage] = (curSt.first + tests) to (curSt.second + 1)
                    }
                    AgendaSubjectBullet.STUDY_TYPE_REVIEW -> {
                        totalReviewed += 1
                        stageReviewed[stage] = (stageReviewed[stage] ?: 0) + 1
                    }
                    AgendaSubjectBullet.STUDY_TYPE_DENEME -> {
                        totalDenemes += 1
                        stageDenemes[stage] = (stageDenemes[stage] ?: 0) + 1
                    }
                }
            }
            val noteSessionSecs = sessionSecondsByDate[note.dateStr] ?: 0L
            val noteSessionMins = ((noteSessionSecs + 30L) / 60L).toInt()
            val extraAgendaMins = (note.studyDurationMinutes - noteSessionMins).coerceAtLeast(0)
            if (extraAgendaMins > 0) {
                val targetStages = if (stagesInNote.isNotEmpty()) stagesInNote else setOf("GENEL")
                val share = extraAgendaMins / targetStages.size
                targetStages.forEach { st ->
                    stageManualAgendaMins[st] = (stageManualAgendaMins[st] ?: 0) + share
                }
            }
        }

        val allActiveDates = (sessionSecondsByDate.keys + agendaMinutesByDate.keys).toSet()
        var totalMinutes = 0
        allActiveDates.forEach { dateKey ->
            val sessionMins = (((sessionSecondsByDate[dateKey] ?: 0L) + 30L) / 60L).toInt()
            val agendaMins = agendaMinutesByDate[dateKey] ?: 0
            totalMinutes += maxOf(sessionMins, agendaMins)
        }

        val todaySessionMins = (((sessionSecondsByDate[todayStr] ?: 0L) + 30L) / 60L).toInt()
        val todayAgendaMins = agendaMinutesByDate[todayStr] ?: 0
        val todayMinutes = maxOf(todaySessionMins, todayAgendaMins)

        val activeDays = allActiveDates.count { dateKey ->
            val sm = (((sessionSecondsByDate[dateKey] ?: 0L) + 30L) / 60L).toInt()
            val am = agendaMinutesByDate[dateKey] ?: 0
            maxOf(sm, am) > 0
        }
        val avgMinutes = if (activeDays > 0) totalMinutes / activeDays else 0

        val subjectStats = practiceTestsBySubject.entries
            .map { (subj, pair) -> SubjectPracticeStat(subj, pair.first, pair.second) }
            .sortedByDescending { it.totalTests }

        val subjectStageStats = practiceTestsBySubjectStage.entries
            .map { (key, pair) ->
                SubjectPracticeStat(
                    subject = key.first,
                    totalTests = pair.first,
                    entryCount = pair.second,
                    examStage = key.second
                )
            }
            .sortedByDescending { it.totalTests }

        val timeStageStats = subjectStageSeconds.entries
            .map { (key, secs) -> SubjectTimeStat(key.first, key.second, secs) }
            .sortedByDescending { it.totalSeconds }

        val stageOrder = listOf("TYT", "AYT", "YDT", "GENEL")
        val allStageKeys = (stageSessionSeconds.keys + stageTests.keys + stageReviewed.keys + stageDenemes.keys + stageManualAgendaMins.keys)
            .sortedBy { st -> stageOrder.indexOf(st).let { if (it < 0) 99 else it } }
        val stageStats = allStageKeys.map { st ->
            val sessMins = (((stageSessionSeconds[st] ?: 0L) + 30L) / 60L).toInt()
            val manualMins = stageManualAgendaMins[st] ?: 0
            StageStat(
                stage = st,
                totalMinutes = sessMins + manualMins,
                sessionCount = stageSessionCount[st] ?: 0,
                testsSolved = stageTests[st] ?: 0,
                topicsReviewed = stageReviewed[st] ?: 0,
                denemesSolved = stageDenemes[st] ?: 0
            )
        }.filter { it.totalMinutes > 0 || it.testsSolved > 0 || it.topicsReviewed > 0 || it.denemesSolved > 0 }

        return DashboardStats(
            averageDailyMinutes = avgMinutes,
            todayTotalMinutes = todayMinutes,
            totalAllTimeMinutes = totalMinutes,
            activeDaysCount = activeDays,
            totalTestsSolved = totalTests,
            totalTopicsReviewed = totalReviewed,
            totalDenemesSolved = totalDenemes,
            practiceBySubject = subjectStats,
            practiceBySubjectAndStage = subjectStageStats,
            timeBySubjectAndStage = timeStageStats,
            stageStats = stageStats
        )
    }
}
