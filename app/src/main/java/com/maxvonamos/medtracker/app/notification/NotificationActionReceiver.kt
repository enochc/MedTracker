package com.maxvonamos.medtracker.app.notification

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.maxvonamos.medtracker.app.data.database.MedTrackerDatabase
import com.maxvonamos.medtracker.app.data.entity.MedicationLog
import com.maxvonamos.medtracker.app.widget.WidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles "Taken" and "Skip" notification actions.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.maxvonamos.medtracker.app.ACTION_TAKEN"
        const val ACTION_SKIP = "com.maxvonamos.medtracker.app.ACTION_SKIP"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        val medicationId = intent.getLongExtra("medication_id", -1L)
        val notificationId = intent.getIntExtra("notification_id", -1)

        if (reminderId == -1L || medicationId == -1L) return

        // Dismiss the notification
        if (notificationId != -1) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.cancel(notificationId)
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_TAKEN -> handleTaken(context, medicationId)
                    ACTION_SKIP -> { /* Just dismiss — nothing to log */ }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleTaken(context: Context, medicationId: Long) {
        val db = Room.databaseBuilder(
            context.applicationContext,
            MedTrackerDatabase::class.java,
            "medtracker.db"
        ).build()

        try {
            val medication = db.medicationDao().getMedicationById(medicationId)
            val now = System.currentTimeMillis()
            val amount = medication?.dosage ?: ""

            val log = MedicationLog(
                medicationId = medicationId,
                amount = amount,
                takenAt = now
            )
            db.medicationDao().insertLog(log)
            db.medicationDao().pruneOldLogs(medicationId)

            // Update widgets
            WidgetUpdater.pushLastTaken(context, medicationId, now, amount)
        } finally {
            db.close()
        }
    }
}
