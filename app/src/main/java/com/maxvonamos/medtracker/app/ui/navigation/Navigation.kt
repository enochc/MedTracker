package com.maxvonamos.medtracker.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument
import com.maxvonamos.medtracker.app.ui.screens.addmedication.AddEditMedicationScreen
import com.maxvonamos.medtracker.app.ui.screens.history.HistoryScreen
import com.maxvonamos.medtracker.app.ui.screens.home.HomeScreen
import com.maxvonamos.medtracker.app.ui.screens.takemed.TakeMedicationScreen

object Routes {
    const val HOME = "home"
    const val ADD_MEDICATION = "add_medication"
    const val EDIT_MEDICATION = "edit_medication/{medId}"
    const val HISTORY = "history/{medId}"
    const val TAKE_MEDICATION = "take_medication/{medId}"

    fun editMedication(medId: Long) = "edit_medication/$medId"
    fun history(medId: Long) = "history/$medId"
    fun takeMedication(medId: Long) = "take_medication/$medId"
}

@Composable
fun MedTrackerNavHost(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddMedication = { navController.navigate(Routes.ADD_MEDICATION) },
                onEditMedication = { navController.navigate(Routes.editMedication(it)) },
                onViewHistory = { navController.navigate(Routes.history(it)) },
                onTakeMedication = { navController.navigate(Routes.takeMedication(it)) }
            )
        }

        composable(Routes.ADD_MEDICATION) {
            AddEditMedicationScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_MEDICATION,
            arguments = listOf(navArgument("medId") { type = NavType.LongType })
        ) {
            AddEditMedicationScreen(
                medicationId = it.arguments?.getLong("medId"),
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.HISTORY,
            arguments = listOf(navArgument("medId") { type = NavType.LongType })
        ) {
            HistoryScreen(
                medicationId = it.arguments?.getLong("medId") ?: 0L,
                onBack = { navController.popBackStack() }
            )
        }

        dialog(
            route = Routes.TAKE_MEDICATION,
            arguments = listOf(navArgument("medId") { type = NavType.LongType })
        ) {
            TakeMedicationScreen(
                medicationId = it.arguments?.getLong("medId") ?: 0L,
                onDone = {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                },
                onViewHistory = { medId ->
                    navController.navigate(Routes.history(medId))
                }
            )
        }
    }
}
