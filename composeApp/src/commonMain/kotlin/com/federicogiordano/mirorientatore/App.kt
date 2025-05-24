package com.federicogiordano.mirorientatore

import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.api.WebSocketServerManager
import com.federicogiordano.mirorientatore.functions.*
import com.federicogiordano.mirorientatore.functions.settings.SettingsPage

@Composable
fun App(
    triggerImport: () -> Unit,
    triggerExport: () -> Unit
) {
    val navController = rememberNavController()
    val quizService = remember { QuizService.getInstance() }

    LaunchedEffect(Unit) {
        WebSocketServerManager.getInstance().start()
    }

    NavHost(
        navController = navController,
        startDestination = Screens.Home.name
    ) {
        composable(Screens.Home.name) {
            HomeView(navController)
        }

        composable(Screens.ConnectedStudents.name) {
            ConnectedStudentsPage(navController)
        }

        composable(Screens.QuizLauncher.name) {
            QuizPage(navController)
        }

        composable(Screens.QuizLibrary.name) {
            val availableQuizzes by quizService.availableQuizzes.collectAsState()
            QuizLibraryPage(
                navController = navController,
                availableQuizzes = availableQuizzes,
                quizService = quizService,
                onImportQuizzes = triggerImport,
                onExportQuizzes = triggerExport
            )
        }

        composable(Screens.Settings.name) {
            SettingsPage(
                onNavigateTo = { route ->
                    navController.navigate(route)
                }
            )
        }

        composable(
            route = "function/{functionId}",
            arguments = listOf(navArgument("functionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val functionId = backStackEntry.arguments?.getString("functionId")

            when (functionId) {
                "mapping" -> MapsList(navController)
                "missions" -> MissionsList(navController)
                "mission_queue" -> MissionQueue(navController)
                "sounds" -> SoundsList(navController)
                "diagnostics" -> DiagnosticsScreen(navController)
            }
        }
    }
}