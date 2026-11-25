package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "custom_timers")
data class CustomTimer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val targetTimestamp: Long
)
