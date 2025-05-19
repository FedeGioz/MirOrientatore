package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController    
import com.federicogiordano.mirorientatore.data.RobotMap
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.federicogiordano.mirorientatore.api.MappingService
import com.federicogiordano.mirorientatore.api.StatusService

@Composable
fun MapsList(navController: NavHostController) {
    val statusService = remember { StatusService() }
    var maps by remember { mutableStateOf<List<RobotMap>>(emptyList()) }
    var activeMapId by remember { mutableStateOf("") }
    var selectedMap by remember { mutableStateOf<RobotMap?>(null) }
    var showConfirmation by remember { mutableStateOf(false) }
    var isUpdating by remember { mutableStateOf(false) }
    var updateResult by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        maps = MappingService().getMaps()
    }

    LaunchedEffect(Unit) {
        val status = statusService.getStatus()
        activeMapId = status.mapId
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            "Available Maps",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (maps.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else {
            LazyColumn {
                items(maps) { map ->
                    val isActive = map.guid == activeMapId

                    MapItem(
                        map = map,
                        isActive = isActive,
                        onClick = {
                            selectedMap = map
                            showConfirmation = true
                        }
                    )
                }
            }
        }

        updateResult?.let {
            Text(
                text = if (it) "Map updated successfully!" else "Failed to update map",
                color = if (it) Color.Green else Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }

    if (showConfirmation && selectedMap != null) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Confirm Map Selection") },
            text = { Text("Set ${selectedMap?.name ?: ""} as the active map?") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmation = false
                        selectedMap?.guid?.let { mapId ->
                            isUpdating = true

                            MainScope().launch {
                                val success = MappingService().updateMap(mapId)

                                if (success) {
                                    delay(1000)
                                    val newStatus = statusService.getStatus()
                                    updateResult = newStatus.mapId == mapId
                                    activeMapId = newStatus.mapId
                                } else {
                                    updateResult = false
                                }

                                selectedMap = null
                                isUpdating = false

                                updateResult = null
                            }
                        }
                    },
                    enabled = !isUpdating
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (isUpdating) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun MapItem(map: RobotMap, isActive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = map.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Guid: ${map.guid}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active Map",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}