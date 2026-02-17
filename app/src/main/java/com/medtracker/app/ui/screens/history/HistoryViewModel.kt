package com.medtracker.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtracker.app.data.entity.Medication
import com.medtracker.app.data.entity.MedicationLog
import com.medtracker.app.data.repository.MedicationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    private val _medicationId = MutableStateFlow(0L)

    val medication: StateFlow<Medication?> = _medicationId
        .flatMapLatest { id ->
            if (id > 0) repository.getMedicationByIdFlow(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val logs: StateFlow<List<MedicationLog>> = _medicationId
        .flatMapLatest { id ->
            if (id > 0) repository.getLogsForMedication(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(medicationId: Long) {
        _medicationId.value = medicationId
    }

    fun deleteLog(log: MedicationLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}
