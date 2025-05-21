package com.federicogiordano.mirorientatore.functions

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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessuno studente connesso", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(connectedStudents) { student ->
                        StudentCard(
                            student = student,
                            onDisconnect = {
                                scope.launch {
                                    webSocketServer.sendToStudent(
                                        student.id,
                                        WebSocketMessage("DISCONNECT", "Disconnesso dal professore", "professor")
                                    )
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
fun StudentCard(student: StudentConnection, onDisconnect: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
            }

            IconButton(onClick = onDisconnect) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Disconnetti",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}