package com.maxvonamos.medtracker.app.ui.screens.addmedication

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.maxvonamos.medtracker.app.data.entity.MedicationReminder
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectable

data class ReminderDialogResult(
    val hour: Int,
    val minute: Int,
    val intervalType: String,
    val daysOfWeek: Int
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReminderDialog(
    onDismiss: () -> Unit,
    onConfirm: (ReminderDialogResult) -> Unit,
    initialHour: Int = 8,
    initialMinute: Int = 0,
    initialIntervalType: String = MedicationReminder.DAILY,
    initialDaysOfWeek: Int = 0
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute
    )
    var intervalType by remember { mutableStateOf(initialIntervalType) }
    var daysOfWeek by remember { mutableIntStateOf(initialDaysOfWeek) }

    val intervalOptions = listOf(
        MedicationReminder.DAILY to "Daily",
        MedicationReminder.EVERY_OTHER_DAY to "Every other day",
        MedicationReminder.SPECIFIC_DAYS to "Specific days"
    )

    val dayLabels = listOf(
        MedicationReminder.SUN to "Sun",
        MedicationReminder.MON to "Mon",
        MedicationReminder.TUE to "Tue",
        MedicationReminder.WED to "Wed",
        MedicationReminder.THU to "Thu",
        MedicationReminder.FRI to "Fri",
        MedicationReminder.SAT to "Sat"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Time picker
                TimePicker(state = timePickerState)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Frequency",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Frequency radio buttons
                intervalOptions.forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = intervalType == type,
                                onClick = { intervalType = type }
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = intervalType == type,
                            onClick = { intervalType = type }
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Day-of-week chips (only when Specific Days is selected)
                if (intervalType == MedicationReminder.SPECIFIC_DAYS) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        dayLabels.forEach { (bit, label) ->
                            val selected = daysOfWeek and bit != 0
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    daysOfWeek = if (selected) {
                                        daysOfWeek and bit.inv()
                                    } else {
                                        daysOfWeek or bit
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(
                        ReminderDialogResult(
                            hour = timePickerState.hour,
                            minute = timePickerState.minute,
                            intervalType = intervalType,
                            daysOfWeek = if (intervalType == MedicationReminder.SPECIFIC_DAYS) daysOfWeek else 0
                        )
                    )
                },
                enabled = intervalType != MedicationReminder.SPECIFIC_DAYS || daysOfWeek != 0
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
