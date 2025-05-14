package com.federicogiordano.mirorientatore.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.viewmodels.StatusViewModel
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun AppScaffold(
    navController: NavHostController,
    currentScreen: Screens,
    onLogout: () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val statusViewModel = viewModel<StatusViewModel>()

    Scaffold(
        topBar = {
            StatusAppBar(statusViewModel, onLogout)
        },
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(Screens.Home.title) },
                    selected = currentRoute == Screens.Home.name,
                    onClick = {
                        if (currentRoute != Screens.Home.name) {
                            navController.navigate(Screens.Home.name) {
                                popUpTo(Screens.Home.name) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
//                NavigationBarItem(
//                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Functions") },
//                    label = { Text(Screens.Functions.title) },
//                    selected = currentRoute == Screens.Functions.name,
//                    onClick = {
//                        if (currentRoute != Screens.Functions.name) {
//                            navController.navigate(Screens.Functions.name) {
//                                popUpTo(Screens.Home.name)
//                                launchSingleTop = true
//                            }
//                        }
//                    }
//                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}