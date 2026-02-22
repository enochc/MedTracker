package com.maxvonamos.medtracker.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.rememberNavController
import com.maxvonamos.medtracker.app.ui.navigation.MedTrackerNavHost
import com.maxvonamos.medtracker.app.ui.navigation.Routes
import com.maxvonamos.medtracker.app.ui.theme.MedTrackerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MedTrackerTheme {
                val navController = rememberNavController()
                
                // Track if we have already reached the history screen
                var historyVisited by remember { mutableStateOf(false) }
                
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    val route = destination.route
                    
                    // Check if we are currently on the history screen
                    if (route?.startsWith("history/") == true) {
                        historyVisited = true
                    }
                    
                    // If we are back at Home AND we were launched with a history deep link 
                    // AND we have already shown history, then finish the activity.
                    val isAtHome = route == Routes.HOME
                    val hasHistoryDeepLink = intent?.data?.toString()?.startsWith("medtracker://history/") == true
                    
                    if (isAtHome && hasHistoryDeepLink && historyVisited) {
                        finish()
                    }
                }

                MedTrackerNavHost(navController = navController)
            }
        }
    }
}
