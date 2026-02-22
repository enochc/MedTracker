package com.maxvonamos.medtracker.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_reminders",
    foreignKeys = [
        ForeignKey(
            entity = Medication::class,
            parentColumns = ["id"],
            childColumns = ["medicationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("medicationId")]
)
data class MedicationReminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val medicationId: Long,
    val hour: Int,          // 0-23
    val minute: Int,        // 0-59
    val intervalType: String,  // "DAILY", "EVERY_OTHER_DAY", "SPECIFIC_DAYS"
    val daysOfWeek: Int = 0,   // Bitmask: Sun=1, Mon=2, Tue=4, Wed=8, Thu=16, Fri=32, Sat=64
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DAILY = "DAILY"
        const val EVERY_OTHER_DAY = "EVERY_OTHER_DAY"
        const val SPECIFIC_DAYS = "SPECIFIC_DAYS"

        // Day bitmask constants (Calendar.SUNDAY=1 .. Calendar.SATURDAY=7)
        const val SUN = 1
        const val MON = 2
        const val TUE = 4
        const val WED = 8
        const val THU = 16
        const val FRI = 32
        const val SAT = 64
    }

    fun isDayEnabled(dayBit: Int): Boolean = daysOfWeek and dayBit != 0
}
