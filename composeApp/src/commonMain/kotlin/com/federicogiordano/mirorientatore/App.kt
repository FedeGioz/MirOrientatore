package com.federicogiordano.mirorientatore

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.federicogiordano.mirorientatore.functions.DiagnosticsScreen
import com.federicogiordano.mirorientatore.functions.DistanceChart
import com.federicogiordano.mirorientatore.functions.MapsList
import com.federicogiordano.mirorientatore.functions.MissionQueue
import com.federicogiordano.mirorientatore.functions.MissionsList
import com.federicogiordano.mirorientatore.functions.SoundsList

@Composable
fun App(){
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screens.Home.name
    ) {

        composable(Screens.Home.name) {
            HomeView(navController)
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