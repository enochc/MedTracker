package com.medtracker.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.unit.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.room.Room
import com.medtracker.app.data.database.MedTrackerDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Preference keys for widget state
object WidgetKeys {
    val MEDICATION_ID = longPreferencesKey("medication_id")
    val MEDICATION_NAME = stringPreferencesKey("medication_name")
    val MEDICATION_DOSAGE = stringPreferencesKey("medication_dosage")
    val LAST_TAKEN_AT = longPreferencesKey("last_taken_at")
    val LAST_AMOUNT = stringPreferencesKey("last_amount")
    val TOTAL_DOSES = longPreferencesKey("total_doses")
}

class MedTrackerWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Refresh data from database before rendering
        refreshWidgetData(context, id)

        provideContent {
            GlanceTheme {
                MedWidgetContent()
            }
        }
    }

    private suspend fun refreshWidgetData(context: Context, glanceId: GlanceId) {
        val db = Room.databaseBuilder(
            context, MedTrackerDatabase::class.java, "medtracker.db"
        ).build()

        try {
            // Force a checkpoint to ensure we see latest WAL data
            db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")?.close()

            updateAppWidgetState(context, glanceId) { prefs ->
                val medId = prefs[WidgetKeys.MEDICATION_ID] ?: return@updateAppWidgetState
                val dao = db.medicationDao()
                val med = dao.getMedicationById(medId)

                if (med == null) {
                    // Medication was deleted — clear the widget so it shows "Setup"
                    prefs.remove(WidgetKeys.MEDICATION_ID)
                    prefs.remove(WidgetKeys.MEDICATION_NAME)
                    prefs.remove(WidgetKeys.MEDICATION_DOSAGE)
                    prefs.remove(WidgetKeys.LAST_TAKEN_AT)
                    prefs.remove(WidgetKeys.LAST_AMOUNT)
                    return@updateAppWidgetState
                }

                val lastLog = dao.getLastLogForMedication(medId)

                // Always update all fields (handles renames automatically)
                prefs[WidgetKeys.MEDICATION_NAME] = med.name
                prefs[WidgetKeys.MEDICATION_DOSAGE] = med.dosage

                // Clear or set the last taken time
                if (lastLog != null) {
                    prefs[WidgetKeys.LAST_TAKEN_AT] = lastLog.takenAt
                    prefs[WidgetKeys.LAST_AMOUNT] = lastLog.amount
                } else {
                    prefs.remove(WidgetKeys.LAST_TAKEN_AT)
                    prefs.remove(WidgetKeys.LAST_AMOUNT)
                }
            }
        } finally {
            db.close()
        }
    }

    @Composable
    private fun MedWidgetContent() {
        WidgetContentInner()
    }

    @Composable
    private fun WidgetContentInner() {
        val state = currentState<androidx.datastore.preferences.core.Preferences>()
        val medId = state[WidgetKeys.MEDICATION_ID]
        val medName = state[WidgetKeys.MEDICATION_NAME] ?: "No medication"
        val lastTakenAt = state[WidgetKeys.LAST_TAKEN_AT]

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(2.dp)
                .clickable(
                    if (medId != null) {
                        actionRunCallback<TakeMedicationAction>(
                            actionParametersOf(
                                ActionParameters.Key<Long>("med_id") to medId
                            )
                        )
                    } else {
                        // No medication bound — open config to let user pick one
                        actionRunCallback<OpenConfigAction>()
                    }
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pill icon
            Text(
                text = "\uD83D\uDC8A",
                style = TextStyle(
                    fontSize = 24.sp
                )
            )

            // Medication name
            if (medId != null) {
                Text(
                    text = medName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFFEEEEEE))
                    ),
                    maxLines = 1
                )

                // Last taken time
                Text(
                    text = if (lastTakenAt != null) {
                        val diff = System.currentTimeMillis() - lastTakenAt
                        val dayMs = 24 * 60 * 60 * 1000L
                        when {
                            diff < dayMs -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastTakenAt))
                            diff < 2 * dayMs -> "Yesterday"
                            diff < 6 * dayMs -> SimpleDateFormat("EEE h:mm a", Locale.getDefault()).format(Date(lastTakenAt))
                            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastTakenAt))
                        }
                    } else "Never",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color(0xFFCCCCCC))
                    ),
                    maxLines = 1
                )
            } else {
                Text(
                    text = "Setup",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = ColorProvider(Color(0xFFEEEEEE))
                    ),
                    maxLines = 1
                )
                Text(
                    text = "Tap here",
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color(0xFFCCCCCC))
                    ),
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Tapping a configured widget opens the Take Medication screen.
 */
class TakeMedicationAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val medId = parameters[ActionParameters.Key<Long>("med_id")] ?: return

        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("medtracker://take/$medId")
        ).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

/**
 * Tapping an unconfigured widget opens WidgetConfigActivity with the
 * correct appWidgetId so the user can pick a medication to bind.
 */
class OpenConfigAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val manager = GlanceAppWidgetManager(context)
        val appWidgetId = manager.getAppWidgetId(glanceId)

        val intent = Intent(context, WidgetConfigActivity::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        context.startActivity(intent)
    }
}

class MedTrackerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MedTrackerWidget()
}
