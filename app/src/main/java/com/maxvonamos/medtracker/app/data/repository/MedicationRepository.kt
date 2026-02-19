package com.maxvonamos.medtracker.app.data.repository

import android.content.Context
import com.maxvonamos.medtracker.app.data.dao.MedicationDao
import com.maxvonamos.medtracker.app.data.entity.Medication
import com.maxvonamos.medtracker.app.data.entity.MedicationLog
import com.maxvonamos.medtracker.app.data.entity.MedicationWithLastLog
import com.maxvonamos.medtracker.app.widget.WidgetUpdater
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

    suspend fun updateMedication(medication: Medication) {
        dao.updateMedication(medication)
        // Push name/dosage changes to any widgets tracking this medication
        WidgetUpdater.pushMedicationUpdate(
            context, medication.id, medication.name, medication.dosage
        )
    }

    suspend fun deleteMedication(medication: Medication) {
        // Remove associated widgets from the home screen before deleting
        WidgetUpdater.removeWidgetsForMedication(context, medication.id)
        dao.deleteMedication(medication)
    }

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
        // Prune logs beyond the 50-entry limit for this medication
        dao.pruneOldLogs(medicationId)
        // Directly push the timestamp to widget state (bypasses database re-query)
        WidgetUpdater.pushLastTaken(context, medicationId, takenAt, amount)
        return id
    }

    fun getLogsForMedication(medicationId: Long): Flow<List<MedicationLog>> =
        dao.getLogsForMedication(medicationId)

    suspend fun getLastLogForMedication(medicationId: Long): MedicationLog? =
        dao.getLastLogForMedication(medicationId)

    suspend fun deleteLog(log: MedicationLog) {
        dao.deleteLog(log)
        // Refresh widgets so the "last taken" label updates
        WidgetUpdater.refreshAllWidgets(context)
    }
}
