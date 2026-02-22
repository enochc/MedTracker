package com.maxvonamos.medtracker.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.maxvonamos.medtracker.app.data.database.MedTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives alarms scheduled by ReminderScheduler.
 * Queries the database for the reminder + medication, shows a notification,
 * and schedules the next alarm.
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getLongExtra("reminder_id", -1L)
        val medicationId = intent.getLongExtra("medication_id", -1L)
        if (reminderId == -1L || medicationId == -1L) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackerDatabase::class.java,
                    "medtracker.db"
                ).build()

                val reminder = db.reminderDao().getReminderById(reminderId)
                val medication = db.medicationDao().getMedicationById(medicationId)

                if (reminder != null && medication != null && reminder.isEnabled) {
                    NotificationHelper.showReminderNotification(context, medication, reminder)

                    // Schedule the next alarm
                    ReminderScheduler.scheduleAlarm(context, reminder)
                }

                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
