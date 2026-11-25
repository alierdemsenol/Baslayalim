package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,                  // e.g. "Matematik / Problem / Geometri", "Fizik"
    val durationSeconds: Long,            // total seconds worked
    val sessionType: String,              // "POMODORO" or "KRONOMETRE"
    val blocksCompleted: Int = 1,         // Pomodoro blocks completed
    val emergencyCount: Int = 0,          // Total emergency pause count
    val timestamp: Long = System.currentTimeMillis(),
    val dateFormatted: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(timestamp)),
    val examStage: String = ""            // "", "TYT", "AYT", "YDT"
)
