package com.maxvonamos.medtracker.app.ui.screens.takemed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxvonamos.medtracker.app.data.entity.Medication
import com.maxvonamos.medtracker.app.data.entity.MedicationLog
import com.maxvonamos.medtracker.app.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TakeMedState(
    val medication: Medication? = null,
    val lastLog: MedicationLog? = null,
    val amount: String = "",
    val note: String = "",
    val selectedTime: Long = System.currentTimeMillis(),
    val isLogging: Boolean = false,
    val isLogged: Boolean = false
)

@HiltViewModel
class TakeMedicationViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TakeMedState())
    val state = _state.asStateFlow()

    fun load(medicationId: Long) {
        viewModelScope.launch {
            val med = repository.getMedicationById(medicationId) ?: return@launch
            val lastLog = repository.getLastLogForMedication(medicationId)
            _state.update {
                it.copy(
                    medication = med,
                    lastLog = lastLog,
                    amount = med.dosage,
                    selectedTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun updateAmount(amount: String) = _state.update { it.copy(amount = amount) }
    fun updateNote(note: String) = _state.update { it.copy(note = note) }
    fun updateSelectedTime(timeMillis: Long) = _state.update { it.copy(selectedTime = timeMillis) }

    fun confirm() {
        val med = _state.value.medication ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLogging = true) }
            repository.logMedication(
                medicationId = med.id,
                takenAt = _state.value.selectedTime,
                amount = _state.value.amount.trim(),
                note = _state.value.note.trim()
            )
            _state.update { it.copy(isLogging = false, isLogged = true) }
        }
    }
}
