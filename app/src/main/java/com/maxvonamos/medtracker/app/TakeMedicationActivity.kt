package com.maxvonamos.medtracker.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.maxvonamos.medtracker.app.ui.screens.takemed.TakeMedicationScreen
import com.maxvonamos.medtracker.app.ui.theme.MedTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * A transparent activity that shows only the Take Medication dialog.
 * Used when the widget launches the "take" action so the main app
 * doesn't appear behind the dialog.
 */
@AndroidEntryPoint
class TakeMedicationActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ensure the activity takes up the full screen (transparently) 
        // to avoid status bar glitches when tapping.
        enableEdgeToEdge()

        // Support both explicit intent (from widget) and deep link URI (medtracker://take/{id})
        val medId = intent?.getLongExtra("med_id", 0L).let { if (it == 0L) null else it }
            ?: intent?.data?.lastPathSegment?.toLongOrNull()
        
        if (medId == null) {
            finish()
            return
        }

        setContent {
            MedTrackerTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            // Tapping outside the card dismisses
                            finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    TakeMedicationScreen(
                        medicationId = medId,
                        onDone = { finish() },
                        onViewHistory = { id ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("medtracker://history/$id"))
                            intent.`package` = packageName
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            startActivity(intent)
                            finish()
                        }
                    )
                }
            }
        }
    }
}
