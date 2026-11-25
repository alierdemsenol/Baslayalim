package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class AgendaSubjectBullet(
    val subject: String,
    val topic: String,
    val studyType: String = STUDY_TYPE_REVIEW, // REVIEW, PRACTICE, DENEME, TIMER, LEGACY
    val testCount: Int = 0,
    val sessionTimestamp: Long = 0L,
    val examStage: String = "",                // "", "TYT", "AYT", "YDT"
    val sourceBook: String = "",               // Book/source name for PRACTICE
    val trialType: String = "",                // "TYT", "AYT", "YDT", "BRANCH" for DENEME
    val totalQuestions: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0
) {
    // Legacy alias kept for source/display compatibility
    val note: String get() = topic

    val blankCount: Int
        get() = (totalQuestions - correctCount - wrongCount).coerceAtLeast(0)

    fun computeNet(): Double = correctCount.toDouble() - (wrongCount.toDouble() / 4.0)

    fun uniqueKey(): String = if (sessionTimestamp > 0L) {
        "ts_${sessionTimestamp}_${subject}_${examStage}"
    } else {
        "manual_${examStage}_${subject.lowercase(java.util.Locale.US)}_${topic.lowercase(java.util.Locale.US)}_${studyType}_${testCount}_${sourceBook.lowercase(java.util.Locale.US)}_${trialType}_${totalQuestions}_${correctCount}_${wrongCount}"
    }

    companion object {
        const val STUDY_TYPE_REVIEW = "REVIEW"
        const val STUDY_TYPE_PRACTICE = "PRACTICE"
        const val STUDY_TYPE_DENEME = "DENEME"
        const val STUDY_TYPE_TIMER = "TIMER"
        const val STUDY_TYPE_LEGACY = "LEGACY"
    }
}

@Entity(tableName = "daily_notes")
data class DailyNote(
    @PrimaryKey
    val dateStr: String, // e.g. "2026-11-13"
    val noteText: String,
    val mood: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val studyDurationMinutes: Int = 0,
    val subjectBulletsJson: String = "[]"
) {
    fun getSubjectBullets(): List<AgendaSubjectBullet> {
        if (subjectBulletsJson.isBlank() || subjectBulletsJson == "[]") return emptyList()
        return try {
            val arr = JSONArray(subjectBulletsJson)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val subj = obj.optString("subject", "").trim()
                    val topic = obj.optString("topic", "").ifBlank { obj.optString("note", "") }.trim()
                    val rawType = obj.optString("studyType", "").trim()
                    val studyType = if (rawType.isNotEmpty()) rawType else AgendaSubjectBullet.STUDY_TYPE_LEGACY
                    val testCount = obj.optInt("testCount", 0).coerceAtLeast(0)
                    val sessionTs = obj.optLong("sessionTimestamp", 0L)
                    val examStage = obj.optString("examStage", "").trim()
                    val sourceBook = obj.optString("sourceBook", "").trim()
                    val trialType = obj.optString("trialType", "").trim()
                    val totalQ = obj.optInt("totalQuestions", 0).coerceAtLeast(0)
                    val correctC = obj.optInt("correctCount", 0).coerceAtLeast(0)
                    val wrongC = obj.optInt("wrongCount", 0).coerceAtLeast(0)
                    if (subj.isNotEmpty() || topic.isNotEmpty() || studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                        add(
                            AgendaSubjectBullet(
                                subject = subj,
                                topic = topic,
                                studyType = studyType,
                                testCount = if (studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) testCount else 0,
                                sessionTimestamp = sessionTs,
                                examStage = examStage,
                                sourceBook = sourceBook,
                                trialType = if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) trialType else "",
                                totalQuestions = if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) totalQ else 0,
                                correctCount = if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) correctC else 0,
                                wrongCount = if (studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) wrongC else 0
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        fun encodeBullets(bullets: List<AgendaSubjectBullet>): String {
            val arr = JSONArray()
            bullets.forEach { b ->
                val obj = JSONObject().apply {
                    put("subject", b.subject.trim())
                    put("topic", b.topic.trim())
                    put("note", b.topic.trim())
                    put("studyType", b.studyType.ifBlank { AgendaSubjectBullet.STUDY_TYPE_REVIEW })
                    put("testCount", if (b.studyType == AgendaSubjectBullet.STUDY_TYPE_PRACTICE) b.testCount.coerceAtLeast(0) else 0)
                    if (b.sessionTimestamp > 0L) {
                        put("sessionTimestamp", b.sessionTimestamp)
                    }
                    if (b.examStage.isNotBlank()) {
                        put("examStage", b.examStage.trim())
                    }
                    if (b.sourceBook.isNotBlank()) {
                        put("sourceBook", b.sourceBook.trim())
                    }
                    if (b.studyType == AgendaSubjectBullet.STUDY_TYPE_DENEME) {
                        put("trialType", b.trialType.trim())
                        put("totalQuestions", b.totalQuestions.coerceAtLeast(0))
                        put("correctCount", b.correctCount.coerceAtLeast(0))
                        put("wrongCount", b.wrongCount.coerceAtLeast(0))
                    }
                }
                arr.put(obj)
            }
            return arr.toString()
        }
    }
}
