package com.medtracker.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.entity.MedicationWithLastLog
import com.medtracker.app.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    val medications: StateFlow<List<MedicationWithLastLog>> =
        repository.getMedicationsWithLastLog()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            repository.deleteMedication(medication)
        }
    }

    fun quickLog(medicationId: Long) {
        viewModelScope.launch {
            repository.logMedication(medicationId)
        }
    }
}
