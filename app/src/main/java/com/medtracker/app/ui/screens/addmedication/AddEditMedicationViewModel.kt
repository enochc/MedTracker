package com.medtracker.app.ui.screens.addmedication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditState(
    val name: String = "",
    val dosage: String = "",
    val notes: String = "",
    val trackAmount: Boolean = false,
    val defaultAmount: String = "",
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddEditMedicationViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddEditState())
    val state = _state.asStateFlow()

    private var editingMedId: Long? = null

    fun loadMedication(id: Long) {
        viewModelScope.launch {
            val med = repository.getMedicationById(id) ?: return@launch
            editingMedId = id
            _state.update {
                it.copy(
                    name = med.name,
                    dosage = med.dosage,
                    notes = med.notes,
                    trackAmount = med.trackAmount,
                    defaultAmount = med.defaultAmount
                )
            }
        }
    }

    fun updateName(name: String) = _state.update { it.copy(name = name, error = null) }
    fun updateDosage(dosage: String) = _state.update { it.copy(dosage = dosage) }
    fun updateNotes(notes: String) = _state.update { it.copy(notes = notes) }
    fun updateTrackAmount(track: Boolean) = _state.update { it.copy(trackAmount = track) }
    fun updateDefaultAmount(amount: String) = _state.update { it.copy(defaultAmount = amount) }

    fun save() {
        val current = _state.value
        if (current.name.isBlank()) {
            _state.update { it.copy(error = "Name is required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val medication = Medication(
                id = editingMedId ?: 0,
                name = current.name.trim(),
                dosage = current.dosage.trim(),
                notes = current.notes.trim(),
                trackAmount = current.trackAmount,
                defaultAmount = current.defaultAmount.trim()
            )
            if (editingMedId != null) {
                repository.updateMedication(medication)
            } else {
                repository.addMedication(medication)
            }
            _state.update { it.copy(isLoading = false, isSaved = true) }
        }
    }

    fun deleteMedication() {
        val medId = editingMedId ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val med = repository.getMedicationById(medId) ?: return@launch
            repository.deleteMedication(med)
            _state.update { it.copy(isLoading = false, isDeleted = true) }
        }
    }
}
