package com.medtracker.app.data.repository

import android.content.Context
import com.medtracker.app.data.dao.MedicationDao
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.entity.MedicationLog
import com.medtracker.app.data.entity.MedicationWithLastLog
import com.medtracker.app.widget.WidgetUpdater
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val dao: MedicationDao,
    @ApplicationContext private val context: Context
) {
    fun getMedicationsWithLastLog(): Flow<List<MedicationWithLastLog>> =
        dao.getMedicationsWithLastLog()

    fun getActiveMedications(): Flow<List<Medication>> =
        dao.getActiveMedications()

    suspend fun getMedicationById(id: Long): Medication? =
        dao.getMedicationById(id)

    fun getMedicationByIdFlow(id: Long): Flow<Medication?> =
        dao.getMedicationByIdFlow(id)

    suspend fun addMedication(medication: Medication): Long =
        dao.insertMedication(medication)

    suspend fun updateMedication(medication: Medication) =
        dao.updateMedication(medication)

    suspend fun deleteMedication(medication: Medication) =
        dao.deleteMedication(medication)

    suspend fun logMedication(medicationId: Long, amount: String = "", note: String = ""): Long {
        val takenAt = System.currentTimeMillis()
        val id = dao.insertLog(
            MedicationLog(
                medicationId = medicationId,
                amount = amount,
                note = note,
                takenAt = takenAt
            )
        )
        // Directly push the timestamp to widget state (bypasses database re-query)
        WidgetUpdater.pushLastTaken(context, medicationId, takenAt, amount)
        return id
    }

    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLog>> =
        dao.getLogsForMedication(medicationId)

    suspend fun getLastLogForMedication(medicationId: Long): MedicationLog? =
        dao.getLastLogForMedication(medicationId)

    suspend fun deleteLog(log: MedicationLog) =
        dao.deleteLog(log)
}
