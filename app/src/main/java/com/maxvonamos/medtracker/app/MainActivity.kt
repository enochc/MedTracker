package com.maxvonamos.medtracker.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
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
                
                // Track if we were launched via a deep link to history
                // If so, we want to finish the activity when backing out of history
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    val isAtHome = destination.route == Routes.HOME
                    val hasHistoryDeepLink = intent?.data?.toString()?.startsWith("medtracker://history/") == true
                    
                    if (isAtHome && hasHistoryDeepLink) {
                        finish()
                    }
                }

                MedTrackerNavHost(navController = navController)
            }
        }
    }
}
