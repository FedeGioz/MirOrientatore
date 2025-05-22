package com.federicogiordano.mirorientatore.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
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
                                popUpTo(Screens.Home.name) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.People, contentDescription = "Studenti") },
                    label = { Text(Screens.ConnectedStudents.title) },
                    selected = currentRoute == Screens.ConnectedStudents.name,
                    onClick = {
                        if (currentRoute != Screens.ConnectedStudents.name) {
                            navController.navigate(Screens.ConnectedStudents.name) {
                                popUpTo(Screens.Home.name)
                                launchSingleTop = true
                            }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Quiz") },
                    label = { Text(Screens.QuizLauncher.title) },
                    selected = currentRoute == Screens.QuizLauncher.name,
                    onClick = {
                        if (currentRoute != Screens.QuizLauncher.name) {
                            navController.navigate(Screens.QuizLauncher.name) {
                                popUpTo(Screens.Home.name)
                                launchSingleTop = true
                            }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.LibraryBooks, contentDescription = "Libreria Quiz") },
                    label = { Text(Screens.QuizLibrary.title) },
                    selected = currentRoute == Screens.QuizLibrary.name,
                    onClick = {
                        if (currentRoute != Screens.QuizLibrary.name) {
                            navController.navigate(Screens.QuizLibrary.name) {
                                popUpTo(Screens.Home.name)
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        content(innerPadding)
    }
}