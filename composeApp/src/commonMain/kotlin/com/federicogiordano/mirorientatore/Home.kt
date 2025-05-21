package com.federicogiordano.mirorientatore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.federicogiordano.mirorientatore.api.RobotWebSocketManager
import com.federicogiordano.mirorientatore.ui.AppScaffold
import com.federicogiordano.mirorientatore.ui.Functions
import com.federicogiordano.mirorientatore.ui.JoystickController
import com.federicogiordano.mirorientatore.viewmodels.StatusViewModel
import kotlinx.coroutines.delay

enum class Screens(val title: String) {
    Home("Home"),
    ConnectedStudents("Studenti Connessi"),
    QuizAnswers("Quiz Attivi"),
    QuizLibrary("Libreria Quiz"),
    Registration("Registrazione"),
    Waiting("Connessione in corso"),
}

@Composable
fun HomeView(
    navController: NavHostController = rememberNavController()
) {
    var linearVelocity by remember { mutableStateOf(0f) }
    var angularVelocity by remember { mutableStateOf(0f) }
    var isJoystickActive by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }
    var isJoystickExpanded by remember { mutableStateOf(false) }
    val webSocketClient = remember { RobotWebSocketManager.getClient() }
    val statusViewModel = remember { StatusViewModel() }
    val status by statusViewModel.status.collectAsState(null)
    val stateId = status?.stateId
    val isButtonEnabled = stateId != null && stateId != 11

    LaunchedEffect(isJoystickActive, linearVelocity, angularVelocity) {
        while (isJoystickActive && isConnected) {
            webSocketClient.sendVelocity(linearVelocity, angularVelocity)
            delay(100)
        }
    }

    AppScaffold(
        navController = navController,
        currentScreen = Screens.Home,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = innerPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isJoystickExpanded = !isJoystickExpanded }
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Joystick",
                            style = MaterialTheme.typography.h6
                        )
                        Icon(
                            imageVector = if (isJoystickExpanded)
                                Icons.Default.KeyboardArrowUp
                            else
                                Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isJoystickExpanded) "Comprimi" else "Espandi"
                        )
                    }

                    if (isJoystickExpanded) {
                        Button(
                            onClick = {
                                println("CLICKED MANUAL")
                                webSocketClient.requestManualControl()
                                isConnected = true
                            },
                            enabled = isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = if (isConnected)
                                    MaterialTheme.colors.primaryVariant
                                else
                                    MaterialTheme.colors.primary,
                                disabledBackgroundColor = MaterialTheme.colors.surface.copy(alpha = 0.7f)
                            ),
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text(
                                text = when {
                                    !isButtonEnabled -> "Controllo Manuale Non Disponibile"
                                    isConnected -> "Controllo Manuale Attivo"
                                    else -> "Attiva Controllo Manuale"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(5.dp))

                        JoystickController(
                            modifier = Modifier.padding(all = 32.dp).align(Alignment.CenterHorizontally),
                            onVelocityChanged = { linear, angular ->
                                linearVelocity = linear
                                angularVelocity = -angular

                                isJoystickActive = kotlin.math.abs(linear) > 0.01f ||
                                        kotlin.math.abs(angular) > 0.01f

                                if (!isJoystickActive && isConnected) {
                                    webSocketClient.sendVelocity(0f, 0f)
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Functions(navController)
        }
    }
}