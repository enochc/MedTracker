package com.medtracker.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medications")
data class Medication(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val dosage: String = "",       // e.g. "500mg", "2 tablets"
    val notes: String = "",
    val trackAmount: Boolean = false, // whether to prompt for amount when logging
    val defaultAmount: String = "",   // default amount to pre-fill
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
