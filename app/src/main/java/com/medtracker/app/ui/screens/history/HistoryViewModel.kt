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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.min

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: MedicationRepository
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val _medicationId = MutableStateFlow(0L)
    private val _currentPage = MutableStateFlow(0)

    val medication: StateFlow<Medication?> = _medicationId
        .flatMapLatest { id ->
            if (id > 0) repository.getMedicationByIdFlow(id)
            else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // All logs (up to 50 from the DAO query)
    private val allLogs: StateFlow<List<MedicationLog>> = _medicationId
        .flatMapLatest { id ->
            if (id > 0) repository.getLogsForMedication(id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = allLogs
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalPages: StateFlow<Int> = allLogs
        .map { logs -> if (logs.isEmpty()) 1 else ceil(logs.size.toDouble() / PAGE_SIZE).toInt() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val currentPage: StateFlow<Int> = _currentPage

    // Paginated logs for the current page
    val logs: StateFlow<List<MedicationLog>> = combine(allLogs, _currentPage) { logs, page ->
        val start = page * PAGE_SIZE
        val end = min(start + PAGE_SIZE, logs.size)
        if (start < logs.size) logs.subList(start, end)
        else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun load(medicationId: Long) {
        _medicationId.value = medicationId
        _currentPage.value = 0
    }

    fun nextPage() {
        val maxPage = totalPages.value - 1
        if (_currentPage.value < maxPage) {
            _currentPage.value++
        }
    }

    fun previousPage() {
        if (_currentPage.value > 0) {
            _currentPage.value--
        }
    }

    fun deleteLog(log: MedicationLog) {
        viewModelScope.launch {
            repository.deleteLog(log)
        }
    }
}
