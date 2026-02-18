package com.maxvonamos.medtracker.app.widget

import android.appwidget.AppWidgetHost
import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    /**
     * Re-render all MedTracker widgets. Each widget's provideGlance will
     * call refreshWidgetData, which re-reads from the database —
     * so this handles renames, dosage changes, and any other data updates.
     */
    suspend fun refreshAllWidgets(context: Context) {
        withContext(Dispatchers.Main) {
            MedTrackerWidget().updateAll(context)
        }
    }

    /**
     * Directly push the latest taken timestamp into all widget instances
     * that are tracking the given medication. This avoids the separate
     * database connection issue where the widget can't see the app's
     * uncommitted WAL data.
     */
    suspend fun pushLastTaken(context: Context, medicationId: Long, takenAt: Long, amount: String) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MedTrackerWidget::class.java)

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                val widgetMedId = prefs[WidgetKeys.MEDICATION_ID] ?: return@updateAppWidgetState
                if (widgetMedId == medicationId) {
                    prefs[WidgetKeys.LAST_TAKEN_AT] = takenAt
                    prefs[WidgetKeys.LAST_AMOUNT] = amount
                }
            }
        }

        withContext(Dispatchers.Main) {
            MedTrackerWidget().updateAll(context)
        }
    }

    /**
     * Directly push a name/dosage change into all widget instances
     * tracking the given medication, then re-render.
     */
    suspend fun pushMedicationUpdate(context: Context, medicationId: Long, name: String, dosage: String) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MedTrackerWidget::class.java)

        for (glanceId in glanceIds) {
            updateAppWidgetState(context, glanceId) { prefs ->
                val widgetMedId = prefs[WidgetKeys.MEDICATION_ID] ?: return@updateAppWidgetState
                if (widgetMedId == medicationId) {
                    prefs[WidgetKeys.MEDICATION_NAME] = name
                    prefs[WidgetKeys.MEDICATION_DOSAGE] = dosage
                }
            }
        }

        withContext(Dispatchers.Main) {
            MedTrackerWidget().updateAll(context)
        }
    }

    /**
     * Remove all home-screen widgets that are tracking a deleted medication.
     * Clears widget state and deletes the widget from the launcher.
     */
    suspend fun removeWidgetsForMedication(context: Context, medicationId: Long) {
        val manager = GlanceAppWidgetManager(context)
        val glanceIds = manager.getGlanceIds(MedTrackerWidget::class.java)

        // Track which widgets matched so we can delete them after clearing state
        val matchedIds = mutableListOf<GlanceId>()

        for (glanceId in glanceIds) {
            // Read + clear in one pass via updateAppWidgetState
            updateAppWidgetState(context, glanceId) { prefs ->
                val widgetMedId = prefs[WidgetKeys.MEDICATION_ID] ?: return@updateAppWidgetState
                if (widgetMedId == medicationId) {
                    matchedIds.add(glanceId)
                    prefs.remove(WidgetKeys.MEDICATION_ID)
                    prefs.remove(WidgetKeys.MEDICATION_NAME)
                    prefs.remove(WidgetKeys.MEDICATION_DOSAGE)
                    prefs.remove(WidgetKeys.LAST_TAKEN_AT)
                    prefs.remove(WidgetKeys.LAST_AMOUNT)
                }
            }
        }

        // Remove matched widgets from the home screen
        for (glanceId in matchedIds) {
            try {
                val appWidgetId = manager.getAppWidgetId(glanceId)
                val host = AppWidgetHost(context, 0)
                host.deleteAppWidgetId(appWidgetId)
            } catch (_: Exception) {
                // If we can't delete it, the cleared state will show "Setup"
            }
        }

        withContext(Dispatchers.Main) {
            MedTrackerWidget().updateAll(context)
        }
    }
}
