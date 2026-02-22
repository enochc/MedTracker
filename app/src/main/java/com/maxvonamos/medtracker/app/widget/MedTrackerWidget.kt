package com.maxvonamos.medtracker.app.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.text.format.DateUtils
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
import androidx.glance.text.TextAlign
import androidx.room.Room
import com.maxvonamos.medtracker.app.data.database.MedTrackerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Preference keys for widget state
object WidgetKeys {
    val MEDICATION_ID = longPreferencesKey("medication_id")
    val MEDICATION_NAME = stringPreferencesKey("medication_name")
    val MEDICATION_NICKNAME = stringPreferencesKey("medication_nickname")
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
                    prefs.remove(WidgetKeys.MEDICATION_NICKNAME)
                    prefs.remove(WidgetKeys.MEDICATION_DOSAGE)
                    prefs.remove(WidgetKeys.LAST_TAKEN_AT)
                    prefs.remove(WidgetKeys.LAST_AMOUNT)
                    return@updateAppWidgetState
                }

                val lastLog = dao.getLastLogForMedication(medId)

                // Always update all fields (handles renames automatically)
                prefs[WidgetKeys.MEDICATION_NAME] = med.name
                prefs[WidgetKeys.MEDICATION_NICKNAME] = med.nickname
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
        val medNickname = state[WidgetKeys.MEDICATION_NICKNAME]
        val lastTakenAt = state[WidgetKeys.LAST_TAKEN_AT]

        val displayName = if (!medNickname.isNullOrBlank()) medNickname else medName

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
                    fontSize = 28.sp
                )
            )

            // Medication name (or nickname)
            if (medId != null) {
                Text(
                    text = displayName,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFFEEEEEE)),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )

                // Last taken time
                Text(
                    text = if (lastTakenAt != null) {
                        val now = System.currentTimeMillis()
                        val timeStr = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(lastTakenAt))
                        
                        when {
                            DateUtils.isToday(lastTakenAt) -> timeStr
                            isYesterday(lastTakenAt) -> "Yesterday\n$timeStr"
                            else -> {
                                val diff = now - lastTakenAt
                                val weekMs = 7 * 24 * 60 * 60 * 1000L
                                if (diff < weekMs) {
                                    SimpleDateFormat("EEE\nh:mm a", Locale.getDefault()).format(Date(lastTakenAt))
                                } else {
                                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastTakenAt))
                                }
                            }
                        }
                    } else "Never",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFFCCCCCC)),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 2
                )
            } else {
                Text(
                    text = "Setup",
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorProvider(Color(0xFFEEEEEE)),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
                Text(
                    text = "Tap here",
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = ColorProvider(Color(0xFFCCCCCC)),
                        textAlign = TextAlign.Center
                    ),
                    maxLines = 1
                )
            }
        }
    }

    private fun isYesterday(whenMs: Long): Boolean {
        return DateUtils.isToday(whenMs + DateUtils.DAY_IN_MILLIS)
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

        // Explicit intent to TakeMedicationActivity (transparent dialog-only activity)
        // so the main app doesn't launch behind it
        val intent = Intent(context, Class.forName("com.maxvonamos.medtracker.app.TakeMedicationActivity")).apply {
            putExtra("med_id", medId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
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

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == "com.maxvonamos.medtracker.app.ACTION_WIDGET_PINNED") {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val medId = intent.getLongExtra("med_id", -1L)

            if (appWidgetId != -1 && medId != -1L) {
                CoroutineScope(Dispatchers.IO).launch {
                    val db = Room.databaseBuilder(
                        context, MedTrackerDatabase::class.java, "medtracker.db"
                    ).build()

                    val med = db.medicationDao().getMedicationById(medId)
                    db.close()

                    val manager = GlanceAppWidgetManager(context)
                    val glanceId = manager.getGlanceIdBy(appWidgetId)

                    updateAppWidgetState(context, glanceId) { prefs ->
                        prefs[WidgetKeys.MEDICATION_ID] = medId
                        // Set labels immediately so the first render isn't blank/generic
                        if (med != null) {
                            prefs[WidgetKeys.MEDICATION_NAME] = med.name
                            prefs[WidgetKeys.MEDICATION_NICKNAME] = med.nickname
                            prefs[WidgetKeys.MEDICATION_DOSAGE] = med.dosage
                        }
                    }
                    glanceAppWidget.update(context, glanceId)
                }
            }
        }
    }
}
