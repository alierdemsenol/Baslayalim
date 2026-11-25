package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<StudySession>>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsOnce(): List<StudySession>

    @Query("SELECT * FROM study_sessions WHERE dateFormatted = :dateStr ORDER BY timestamp DESC")
    fun getSessionsForDate(dateStr: String): Flow<List<StudySession>>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE dateFormatted = :dateStr")
    fun getTotalStudySecondsForDate(dateStr: String): Flow<Long?>

    @Query("SELECT SUM(durationSeconds) FROM study_sessions WHERE dateFormatted = :dateStr")
    suspend fun getTotalStudySecondsForDateOnce(dateStr: String): Long?

    @Query("SELECT SUM(blocksCompleted) FROM study_sessions WHERE dateFormatted = :dateStr AND sessionType = 'POMODORO'")
    fun getTotalPomodoroBlocksForDate(dateStr: String): Flow<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySession): Long

    @Query("UPDATE study_sessions SET dateFormatted = :newDateStr WHERE timestamp = :ts AND dateFormatted = :oldDateStr")
    suspend fun moveSessionDate(ts: Long, oldDateStr: String, newDateStr: String)

    @Query("UPDATE study_sessions SET dateFormatted = :newDateStr WHERE dateFormatted = :oldDateStr")
    suspend fun moveAllSessionsToDate(oldDateStr: String, newDateStr: String)

    @Query("UPDATE study_sessions SET subject = :subject, examStage = :examStage WHERE timestamp = :ts")
    suspend fun updateSessionMeta(ts: Long, subject: String, examStage: String)

    @Delete
    suspend fun deleteSession(session: StudySession)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAllSessions()
}
