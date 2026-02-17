package com.medtracker.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.entity.MedicationLog
import com.medtracker.app.data.entity.MedicationWithLastLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    // -- Medications --

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY name ASC")
    fun getActiveMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    suspend fun getMedicationById(id: Long): Medication?

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationByIdFlow(id: Long): Flow<Medication?>

    @Query("""
        SELECT m.*,
               (SELECT takenAt FROM medication_logs WHERE medicationId = m.id ORDER BY takenAt DESC LIMIT 1) as lastTakenAt,
               (SELECT amount FROM medication_logs WHERE medicationId = m.id ORDER BY takenAt DESC LIMIT 1) as lastAmount,
               (SELECT COUNT(*) FROM medication_logs WHERE medicationId = m.id) as totalDoses
        FROM medications m
        WHERE m.isActive = 1
        ORDER BY m.name ASC
    """)
    fun getMedicationsWithLastLog(): Flow<List<MedicationWithLastLog>>

    // -- Logs --

    @Insert
    suspend fun insertLog(log: MedicationLog): Long

    @Delete
    suspend fun deleteLog(log: MedicationLog)

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY takenAt DESC")
    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLog>>

    @Query("SELECT * FROM medication_logs WHERE medicationId = :medicationId ORDER BY takenAt DESC LIMIT 1")
    suspend fun getLastLogForMedication(medicationId: Long): MedicationLog?

    @Query("SELECT * FROM medication_logs ORDER BY takenAt DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<MedicationLog>>

    @Query("""
        SELECT * FROM medication_logs
        WHERE medicationId = :medicationId AND takenAt BETWEEN :startTime AND :endTime
        ORDER BY takenAt DESC
    """)
    fun getLogsForMedicationInRange(medicationId: Long, startTime: Long, endTime: Long): Flow<List<MedicationLog>>
}
