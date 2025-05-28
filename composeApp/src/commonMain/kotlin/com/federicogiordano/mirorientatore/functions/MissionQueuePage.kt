package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.federicogiordano.mirorientatore.api.MissionService
import com.federicogiordano.mirorientatore.data.RobotMission
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.api.RobotWebSocketManager
import kotlinx.coroutines.launch

// Define what makes a mission valid for display.
// This is an example; adjust the condition based on your RobotMission structure
// and what constitutes an "empty" or "invalid" mission.
// e.g., fun RobotMission.isValidForDisplay(): Boolean = this.name.isNotBlank() && this.guid.isNotBlank()
fun RobotMission.isValidForDisplay(): Boolean = this.name.isNotBlank()


@Composable
fun MissionQueue(navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    var missionQueue by remember { mutableStateOf<List<RobotMission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isLoading = true // Set loading state at the beginning
        try {
            val fetchedMissions = MissionService().getMissionQueue()
            // Filter missions to only include those valid for display
            missionQueue = fetchedMissions.filter { it.isValidForDisplay() }
        } catch (e: Exception) {
            // Log error e (e.g., using Log.e("MissionQueue", "Error fetching queue", e))
            missionQueue = emptyList() // Ensure queue is empty on error
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Coda Missioni",
                style = MaterialTheme.typography.headlineMedium
            )

            Button(
                onClick = {
                    coroutineScope.launch {
                        RobotWebSocketManager.getClient().startMissionQueue()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Avvia")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Avvia")
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (missionQueue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nessuna missione in coda", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                itemsIndexed(
                    items = missionQueue,
                    // Provide a stable key for each item for better performance
                    key = { _, item -> item.guid }
                ) { index, mission ->
                    MissionQueueItem(
                        mission = mission,
                        canMoveUp = index > 0,
                        canMoveDown = index < missionQueue.size - 1,
                        onRemove = {
                            coroutineScope.launch {
                                MissionService().removeMissionFromQueue(mission)
                                // Update local state; no need to re-filter if missionQueue already contains only valid items
                                missionQueue = missionQueue.filter { it.guid != mission.guid }
                            }
                        },
                        onMoveUp = {
                            if (index > 0) {
                                val newList = missionQueue.toMutableList()
                                val movedItem = newList.removeAt(index) // The item that is being moved
                                newList.add(index - 1, movedItem)
                                coroutineScope.launch {
                                    // Pass the actual mission that was moved to the service
                                    MissionService().reorderMissionQueue(movedItem, true)
                                    missionQueue = newList
                                }
                            }
                        },
                        onMoveDown = {
                            if (index < missionQueue.size - 1) {
                                val newList = missionQueue.toMutableList()
                                val movedItem = newList.removeAt(index) // The item that is being moved
                                newList.add(index + 1, movedItem)
                                coroutineScope.launch {
                                    // Pass the actual mission that was moved to the service
                                    MissionService().reorderMissionQueue(movedItem, false)
                                    missionQueue = newList
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MissionQueueItem(
    mission: RobotMission,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mission.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ID: ${mission.guid.take(8)}...",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = canMoveUp
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Sposta su",
                        tint = if (canMoveUp) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = onMoveDown,
                    enabled = canMoveDown
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Sposta giù",
                        tint = if (canMoveDown) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(onClick = onRemove) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Rimuovi",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}