package com.maxvonamos.medtracker.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.maxvonamos.medtracker.app.data.dao.MedicationDao
import com.maxvonamos.medtracker.app.data.dao.ReminderDao
import com.maxvonamos.medtracker.app.data.entity.Medication
import com.maxvonamos.medtracker.app.data.entity.MedicationLog
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder

@Database(
    entities = [Medication::class, MedicationLog::class, MedicationReminder::class],
    version = 2,
    exportSchema = true
)
abstract class MedTrackerDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS medication_reminders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        medicationId INTEGER NOT NULL,
                        hour INTEGER NOT NULL,
                        minute INTEGER NOT NULL,
                        intervalType TEXT NOT NULL,
                        daysOfWeek INTEGER NOT NULL DEFAULT 0,
                        isEnabled INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (medicationId) REFERENCES medications(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_medication_reminders_medicationId ON medication_reminders(medicationId)")
            }
        }
    }
}
