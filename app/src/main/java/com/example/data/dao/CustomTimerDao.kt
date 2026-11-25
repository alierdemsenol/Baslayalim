package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CustomTimer
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomTimerDao {

    @Query("SELECT * FROM custom_timers ORDER BY targetTimestamp ASC")
    fun getAllCustomTimers(): Flow<List<CustomTimer>>

    @Query("SELECT * FROM custom_timers ORDER BY targetTimestamp ASC")
    suspend fun getAllTimersOnce(): List<CustomTimer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTimer(timer: CustomTimer): Long

    @Delete
    suspend fun deleteTimer(timer: CustomTimer)

    @Query("DELETE FROM custom_timers")
    suspend fun clearAllTimers()
}
