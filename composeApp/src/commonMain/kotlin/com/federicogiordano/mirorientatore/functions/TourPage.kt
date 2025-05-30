package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.MissionService
import com.federicogiordano.mirorientatore.api.RobotWebSocketManager
import com.federicogiordano.mirorientatore.data.RobotMission
import com.federicogiordano.mirorientatore.ui.AppScaffold
import kotlinx.coroutines.launch

@Composable
fun TourPage(navController: NavHostController) {
    val missionService = remember { MissionService() }
    var fullRobotMissions by remember { mutableStateOf(emptyList<RobotMission>()) }
    var missionDisplayNames by remember { mutableStateOf(emptyList<String>()) }
    var selectedMissionName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var tourState by remember { mutableStateOf(TourControlState.STOPPED) }
    val robotWebSocketClient = remember { RobotWebSocketManager.getClient() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val fetchedMissions = missionService.getMissions()
            fullRobotMissions = fetchedMissions
            missionDisplayNames = fetchedMissions.map { it.name }
            selectedMissionName = missionDisplayNames.firstOrNull() ?: ""
        } catch (e: Exception) {
            println("Errore durante il recupero delle missioni: ${e.message}")
            fullRobotMissions = emptyList()
            missionDisplayNames = emptyList()
            selectedMissionName = ""
        }
    }

    AppScaffold(
        navController = navController,
        currentScreen = Screens.Tour,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Text("Seleziona Percorso:", style = MaterialTheme.typography.h6)

            Box {
                OutlinedButton(onClick = { expanded = true }) {
                    Text(selectedMissionName.ifEmpty { "Nessun percorso selezionato" })
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Seleziona percorso")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    missionDisplayNames.forEach { missionName ->
                        DropdownMenuItem(onClick = {
                            selectedMissionName = missionName
                            expanded = false
                            tourState = TourControlState.STOPPED
                        }) {
                            Text(missionName)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        if (selectedMissionName.isNotEmpty()) {
                            val missionToQueue = fullRobotMissions.find { it.name == selectedMissionName }
                            if (missionToQueue != null) {
                                println("Avvio del tour: $selectedMissionName")
                                coroutineScope.launch {
                                    try {
                                        missionService.addMissionToQueue(mission = missionToQueue)
                                        robotWebSocketClient.startMissionQueue()
                                        tourState = TourControlState.STARTED
                                    } catch (e: Exception) {
                                        println("Errore durante l'aggiunta della missione alla coda o l'avvio della coda: ${e.message}")
                                    }
                                }
                            } else {
                                println("Errore: Impossibile trovare i dettagli della missione per '$selectedMissionName'.")
                            }
                        }
                    },
                    enabled = selectedMissionName.isNotEmpty() && tourState != TourControlState.STARTED
                ) {
                    Text("Start")
                }

                Button(
                    onClick = {
                        println("Metti in pausa il tour: $selectedMissionName")
                        robotWebSocketClient.stopMissionQueue()
                        tourState = TourControlState.PAUSED
                    },
                    enabled = tourState == TourControlState.STARTED
                ) {
                    Text("Pause")
                }

                Button(
                    onClick = {
                        println("Ferma il tour: $selectedMissionName")
                        coroutineScope.launch {
                            missionService.clearMissionQueue()
                        }
                        tourState = TourControlState.STOPPED
                    },
                    enabled = tourState != TourControlState.STOPPED
                ) {
                    Text("Stop")
                }
            }
        }
    }
}

enum class TourControlState {
    STARTED, PAUSED, STOPPED
}