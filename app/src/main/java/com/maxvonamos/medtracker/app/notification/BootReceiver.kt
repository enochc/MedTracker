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
 * Re-schedules all enabled medication reminders after device reboot.
 * Alarms are lost on reboot, so we must restore them.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    MedTrackerDatabase::class.java,
                    "medtracker.db"
                ).build()

                ReminderScheduler.rescheduleAllAlarms(context, db.reminderDao())
                db.close()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
