package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.RobotWebSocketClient
import com.federicogiordano.mirorientatore.api.RobotWebSocketManager
import com.federicogiordano.mirorientatore.api.StudentConnection
import com.federicogiordano.mirorientatore.api.WebSocketMessage
import com.federicogiordano.mirorientatore.api.WebSocketServerManager
import com.federicogiordano.mirorientatore.ui.AppScaffold
import kotlinx.coroutines.launch

@Composable
fun ConnectedStudentsPage(navController: NavHostController) {
    val webSocketServer = remember { WebSocketServerManager.getInstance() }
    val connectedStudents by webSocketServer.connectedStudents.collectAsState()
    val scope = rememberCoroutineScope()

    val anyStudentHasJoystick = connectedStudents.any { it.hasJoystickAccess }

    AppScaffold(
        navController = navController,
        currentScreen = Screens.ConnectedStudents
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Studenti Connessi",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    "Totale: ${connectedStudents.size}",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            if (connectedStudents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessuno studente connesso", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (anyStudentHasJoystick) 80.dp else 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(connectedStudents, key = { it.id }) { student ->
                        StudentCard(
                            student = student,
                            onDisconnect = {
                                scope.launch {
                                    webSocketServer.sendToStudent(
                                        student.id,
                                        WebSocketMessage("DISCONNECT", "Disconnesso dal professore", "professor")
                                    )
                                }
                            },
                            onAllowJoystick = {
                                scope.launch {
                                    RobotWebSocketManager.getClient().requestManualControl()
                                    webSocketServer.allowJoystickForStudent(student.id)
                                }
                            },
                            onRevokeJoystick = {
                                scope.launch {
                                    webSocketServer.revokeJoystickForStudent(student.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(
    student: StudentConnection,
    onDisconnect: () -> Unit,
    onAllowJoystick: () -> Unit,
    onRevokeJoystick: () -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (student.hasJoystickAccess) "Revoca Controllo Joystick" else "Abilita Controllo Joystick") },
            text = { Text(if (student.hasJoystickAccess) "Sei sicuro di voler revocare il controllo joystick per ${student.name}?" else "Sei sicuro di voler abilitare il controllo joystick per ${student.name}?") },
            confirmButton = {
                Button(
                    onClick = {
                        if (student.hasJoystickAccess) {
                            onRevokeJoystick()
                        } else {
                            onAllowJoystick()
                        }
                        showDialog = false
                    }
                ) {
                    Text(if (student.hasJoystickAccess) "Revoca" else "Abilita")
                }
            },
            dismissButton = {
                Button(onClick = { showDialog = false }) {
                    Text("Annulla")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = student.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${student.id}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (student.hasJoystickAccess) {
                    Text(
                        text = "Controllo Joystick: Abilitato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        text = "Controllo Joystick: Disabilitato",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnetti ${student.name}",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}