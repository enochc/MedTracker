package com.maxvonamos.medtracker.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.maxvonamos.medtracker.app.R
import com.maxvonamos.medtracker.app.data.entity.Medication
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder

object NotificationHelper {

    private const val CHANNEL_ID = "medication_reminders"
    private const val CHANNEL_NAME = "Medication Reminders"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders to take your medications"
            enableVibration(true)
            enableLights(true)
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun showReminderNotification(
        context: Context,
        medication: Medication,
        reminder: MedicationReminder
    ) {
        val notificationId = reminder.id.toInt()

        // "Taken" action
        val takenIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.maxvonamos.medtracker.app.ACTION_TAKEN"
            putExtra("reminder_id", reminder.id)
            putExtra("medication_id", medication.id)
            putExtra("notification_id", notificationId)
        }
        val takenPending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // "Skip" action
        val skipIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.maxvonamos.medtracker.app.ACTION_SKIP"
            putExtra("reminder_id", reminder.id)
            putExtra("medication_id", medication.id)
            putExtra("notification_id", notificationId)
        }
        val skipPending = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentText = if (medication.dosage.isNotBlank()) {
            "Time to take ${medication.dosage}"
        } else {
            "Time for your medication"
        }

        val displayName = if (medication.nickname.isNotBlank()) medication.nickname else medication.name

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayName)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Taken", takenPending)
            .addAction(0, "Skip", skipPending)
            .build()

        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(notificationId, notification)
    }
}
