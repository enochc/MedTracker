package com.medtracker.app.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {
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

        // Trigger re-render
        withContext(Dispatchers.Main) {
            MedTrackerWidget().updateAll(context)
        }
    }
}
