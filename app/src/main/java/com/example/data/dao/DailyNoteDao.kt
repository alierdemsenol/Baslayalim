package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DailyNote
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyNoteDao {

    @Query("SELECT * FROM daily_notes ORDER BY dateStr DESC")
    fun getAllNotes(): Flow<List<DailyNote>>

    @Query("SELECT * FROM daily_notes ORDER BY dateStr DESC")
    suspend fun getAllNotesOnce(): List<DailyNote>

    @Query("SELECT * FROM daily_notes WHERE dateStr = :dateStr LIMIT 1")
    fun getNoteForDate(dateStr: String): Flow<DailyNote?>

    @Query("SELECT * FROM daily_notes WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getNoteForDateOnce(dateStr: String): DailyNote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateNote(note: DailyNote)

    @Delete
    suspend fun deleteNote(note: DailyNote)

    @Query("DELETE FROM daily_notes")
    suspend fun clearAllNotes()
}
