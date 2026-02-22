package com.maxvonamos.medtracker.app.ui.screens.addmedication

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maxvonamos.medtracker.app.data.dao.ReminderDao
import com.maxvonamos.medtracker.app.data.entity.Medication
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder
import com.maxvonamos.medtracker.app.data.repository.MedicationRepository
import com.maxvonamos.medtracker.app.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val error: String? = null,
    val reminders: List<MedicationReminder> = emptyList()
)

@HiltViewModel
class AddEditMedicationViewModel @Inject constructor(
    private val repository: MedicationRepository,
    private val reminderDao: ReminderDao,
    @ApplicationContext private val context: Context
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
        // Collect reminders in a separate coroutine
        viewModelScope.launch {
            reminderDao.getRemindersForMedication(id).collect { reminders ->
                _state.update { it.copy(reminders = reminders) }
            }
        }
    }

    fun updateName(name: String) = _state.update { it.copy(name = name, error = null) }
    fun updateDosage(dosage: String) = _state.update { it.copy(dosage = dosage) }
    fun updateNotes(notes: String) = _state.update { it.copy(notes = notes) }

    fun addReminder(result: ReminderDialogResult) {
        val medId = editingMedId ?: return
        viewModelScope.launch {
            val reminder = MedicationReminder(
                medicationId = medId,
                hour = result.hour,
                minute = result.minute,
                intervalType = result.intervalType,
                daysOfWeek = result.daysOfWeek
            )
            val id = reminderDao.insertReminder(reminder)
            val saved = reminderDao.getReminderById(id) ?: return@launch
            ReminderScheduler.scheduleAlarm(context, saved)
        }
    }

    fun deleteReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            ReminderScheduler.cancelAlarm(context, reminder.id)
            reminderDao.deleteReminder(reminder)
        }
    }

    fun toggleReminder(reminder: MedicationReminder) {
        viewModelScope.launch {
            val updated = reminder.copy(isEnabled = !reminder.isEnabled)
            reminderDao.updateReminder(updated)
            if (updated.isEnabled) {
                ReminderScheduler.scheduleAlarm(context, updated)
            } else {
                ReminderScheduler.cancelAlarm(context, reminder.id)
            }
        }
    }

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
            // Cancel all alarms for this medication's reminders
            val reminders = _state.value.reminders
            for (r in reminders) {
                ReminderScheduler.cancelAlarm(context, r.id)
            }
            val med = repository.getMedicationById(medId) ?: return@launch
            repository.deleteMedication(med)
            _state.update { it.copy(isLoading = false, isDeleted = true) }
        }
    }
}
