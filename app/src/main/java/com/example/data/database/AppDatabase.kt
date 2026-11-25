package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.CustomTimerDao
import com.example.data.dao.DailyNoteDao
import com.example.data.dao.StudySessionDao
import com.example.data.model.CustomTimer
import com.example.data.model.DailyNote
import com.example.data.model.StudySession

@Database(entities = [StudySession::class, CustomTimer::class, DailyNote::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studySessionDao(): StudySessionDao
    abstract fun customTimerDao(): CustomTimerDao
    abstract fun dailyNoteDao(): DailyNoteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE daily_notes ADD COLUMN studyDurationMinutes INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE daily_notes ADD COLUMN subjectBulletsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE study_sessions ADD COLUMN examStage TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "yks_odak_db"
                )
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
