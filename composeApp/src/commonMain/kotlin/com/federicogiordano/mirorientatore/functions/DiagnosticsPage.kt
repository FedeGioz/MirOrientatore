package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.api.DistanceStatistic
import com.federicogiordano.mirorientatore.api.StatisticsService

@Composable
fun DiagnosticsScreen(navController: NavHostController) {
    val coroutineScope = rememberCoroutineScope()
    var distanceStats by remember { mutableStateOf<List<DistanceStatistic>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            distanceStats = StatisticsService().getDistanceStats()
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            errorMessage = "Impossibile caricare le statistiche: ${e.message}"
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
                "Diagnostica",
                style = MaterialTheme.typography.headlineMedium
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            DistanceChart(distanceStats)
        }
    }
}

@Composable
fun DistanceChart(data: List<DistanceStatistic>) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxWidth().height(300.dp), contentAlignment = Alignment.Center) {
            Text("Nessun dato sulla distanza disponibile")
        }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            "Statistiche GetDistance",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val distances = data.mapNotNull { it.distance }
                val maxDistance = distances.maxOrNull() ?: 1f
                val minDistance = distances.minOrNull() ?: 0f

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val chartWidth = width - 40f
                    val chartHeight = height - 40f

                    drawLine(
                        Color.Gray,
                        Offset(20f, 20f),
                        Offset(20f, height - 20f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        Color.Gray,
                        Offset(20f, height - 20f),
                        Offset(width - 20f, height - 20f),
                        strokeWidth = 2f
                    )

                    val range = maxDistance - minDistance
                    val points = data.mapIndexedNotNull { index, stat ->
                        stat.distance?.let {
                            val x = 20f + (index * chartWidth / (data.size - 1))
                            val normalizedY = if (range > 0f) (it - minDistance) / range else 0.5f
                            val y = height - 20f - (normalizedY * chartHeight)
                            Offset(x, y)
                        }
                    }

                    for (i in 0 until points.size - 1) {
                        drawLine(
                            primaryColor,
                            points[i],
                            points[i + 1],
                            strokeWidth = 3f
                        )
                    }

                    points.forEach { point ->
                        drawCircle(
                            primaryColor,
                            radius = 6f,
                            center = point
                        )
                    }
                }

                Text(
                    "Distanza (m)",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.TopStart).padding(start = 4.dp)
                )
                Text(
                    "Data",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Data", style = MaterialTheme.typography.bodyMedium)
            Text("Distanza (m)", style = MaterialTheme.typography.bodyMedium)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        data.forEach { stat ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stat.date ?: "N/D", style = MaterialTheme.typography.bodySmall)
                Text(stat.distance?.toString() ?: "N/D", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}