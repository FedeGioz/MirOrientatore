package com.federicogiordano.mirorientatore

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.federicogiordano.mirorientatore.api.WebSocketServer
import com.federicogiordano.mirorientatore.api.WebSocketServerManager
import com.federicogiordano.mirorientatore.data.QuizAnswer
import com.federicogiordano.mirorientatore.functions.ConnectedStudentsPage
import com.federicogiordano.mirorientatore.functions.DiagnosticsScreen
import com.federicogiordano.mirorientatore.functions.DistanceChart
import com.federicogiordano.mirorientatore.functions.MapsList
import com.federicogiordano.mirorientatore.functions.MissionQueue
import com.federicogiordano.mirorientatore.functions.MissionsList
import com.federicogiordano.mirorientatore.functions.QuizPage
import com.federicogiordano.mirorientatore.functions.SoundsList

@Composable
fun App(){
    val navController = rememberNavController()

    WebSocketServerManager.getInstance().start()

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

        composable(Screens.QuizAnswers.name) {
            QuizPage(navController)
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
//                else -> FunctionSubScreen("Unknown Function", navController)
            }
        }
    }
}