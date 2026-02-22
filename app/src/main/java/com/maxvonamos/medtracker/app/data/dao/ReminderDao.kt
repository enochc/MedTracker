package com.maxvonamos.medtracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MedicationReminder): Long

    @Update
    suspend fun updateReminder(reminder: MedicationReminder)

    @Delete
    suspend fun deleteReminder(reminder: MedicationReminder)

    @Query("SELECT * FROM medication_reminders WHERE medicationId = :medicationId ORDER BY hour, minute")
    fun getRemindersForMedication(medicationId: Long): Flow<List<MedicationReminder>>

    @Query("SELECT * FROM medication_reminders WHERE id = :id")
    suspend fun getReminderById(id: Long): MedicationReminder?

    @Query("SELECT * FROM medication_reminders WHERE isEnabled = 1")
    suspend fun getEnabledReminders(): List<MedicationReminder>

    @Query("DELETE FROM medication_reminders WHERE medicationId = :medicationId")
    suspend fun deleteRemindersForMedication(medicationId: Long)
}
