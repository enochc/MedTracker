package com.maxvonamos.medtracker.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.maxvonamos.medtracker.app.data.dao.ReminderDao
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder
import java.util.Calendar

object ReminderScheduler {

    /**
     * Compute the next alarm time for a reminder, starting from now.
     */
    fun computeNextAlarmTime(reminder: MedicationReminder): Long {
        val now = Calendar.getInstance()
        val alarm = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        return when (reminder.intervalType) {
            MedicationReminder.DAILY -> {
                // If today's time has passed, schedule for tomorrow
                if (alarm.before(now) || alarm == now) {
                    alarm.add(Calendar.DAY_OF_MONTH, 1)
                }
                alarm.timeInMillis
            }
            MedicationReminder.EVERY_OTHER_DAY -> {
                // If today's time has passed, start from tomorrow; then find next valid day
                if (alarm.before(now) || alarm == now) {
                    alarm.add(Calendar.DAY_OF_MONTH, 1)
                }
                // Every other day from creation date
                val creationCal = Calendar.getInstance().apply { timeInMillis = reminder.createdAt }
                val daysSinceCreation = ((alarm.timeInMillis - creationCal.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
                if (daysSinceCreation % 2 != 0) {
                    alarm.add(Calendar.DAY_OF_MONTH, 1)
                }
                alarm.timeInMillis
            }
            MedicationReminder.SPECIFIC_DAYS -> {
                if (reminder.daysOfWeek == 0) return Long.MAX_VALUE // No days selected

                // Try today first if time hasn't passed
                if (alarm.after(now) && isDaySelected(reminder, alarm.get(Calendar.DAY_OF_WEEK))) {
                    return alarm.timeInMillis
                }

                // Otherwise search the next 7 days
                alarm.add(Calendar.DAY_OF_MONTH, 1)
                alarm.set(Calendar.HOUR_OF_DAY, reminder.hour)
                alarm.set(Calendar.MINUTE, reminder.minute)
                for (i in 0 until 7) {
                    if (isDaySelected(reminder, alarm.get(Calendar.DAY_OF_WEEK))) {
                        return alarm.timeInMillis
                    }
                    alarm.add(Calendar.DAY_OF_MONTH, 1)
                }
                Long.MAX_VALUE // Should not reach here if at least one day is selected
            }
            else -> {
                // Default to daily
                if (alarm.before(now) || alarm == now) {
                    alarm.add(Calendar.DAY_OF_MONTH, 1)
                }
                alarm.timeInMillis
            }
        }
    }

    private fun isDaySelected(reminder: MedicationReminder, calendarDay: Int): Boolean {
        val bit = when (calendarDay) {
            Calendar.SUNDAY -> MedicationReminder.SUN
            Calendar.MONDAY -> MedicationReminder.MON
            Calendar.TUESDAY -> MedicationReminder.TUE
            Calendar.WEDNESDAY -> MedicationReminder.WED
            Calendar.THURSDAY -> MedicationReminder.THU
            Calendar.FRIDAY -> MedicationReminder.FRI
            Calendar.SATURDAY -> MedicationReminder.SAT
            else -> 0
        }
        return reminder.isDayEnabled(bit)
    }

    /**
     * Schedule an exact alarm for a reminder.
     */
    fun scheduleAlarm(context: Context, reminder: MedicationReminder) {
        val alarmTime = computeNextAlarmTime(reminder)
        if (alarmTime == Long.MAX_VALUE) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("reminder_id", reminder.id)
            putExtra("medication_id", reminder.medicationId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        if (canScheduleExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        } else {
            // Fallback to inexact alarm if permission is missing
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancel an alarm for a reminder.
     */
    fun cancelAlarm(context: Context, reminderId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    /**
     * Reschedule all enabled reminders (e.g. after boot).
     */
    suspend fun rescheduleAllAlarms(context: Context, reminderDao: ReminderDao) {
        val reminders = reminderDao.getEnabledReminders()
        for (reminder in reminders) {
            scheduleAlarm(context, reminder)
        }
    }
}
