package com.medtracker.app.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medtracker.app.data.dao.MedicationDao
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.entity.MedicationLog

@Database(
    entities = [Medication::class, MedicationLog::class],
    version = 1,
    exportSchema = true
)
abstract class MedTrackerDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
}
