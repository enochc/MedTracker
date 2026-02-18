package com.maxvonamos.medtracker.app.data.entity

import androidx.room.Embedded

data class MedicationWithLastLog(
    @Embedded val medication: Medication,
    val lastTakenAt: Long?,
    val lastAmount: String?,
    val totalDoses: Int
)
