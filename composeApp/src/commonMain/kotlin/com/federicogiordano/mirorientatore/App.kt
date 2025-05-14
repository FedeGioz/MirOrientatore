package com.federicogiordano.mirorientatore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

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

//            when (functionId) {
//                "mapping" -> MapsList(navController)
//                "missions" -> MissionsList(navController)
//                "sounds" -> SoundsList(navController)
//                "diagnostics" ->
//                    FunctionSubScreen(functionId.replace("_", " ").capitalize(), navController)
//                else -> FunctionSubScreen("Unknown Function", navController)
//            }
        }
    }
}