package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.PortScanner
import kotlinx.coroutines.delay

@Composable
fun WaitingScreen(navController: NavController) {
    var dotsCount by remember { mutableStateOf(1) }
    var isScanning by remember { mutableStateOf(true) }
    val portScanner = remember { PortScanner() }

    LaunchedEffect(key1 = true) {
        while (isScanning) {
            dotsCount = (dotsCount % 3) + 1
            delay(500)
        }
    }

    LaunchedEffect(key1 = true) {
        val professorDeviceIp = portScanner.findProfessorDevice()
        if (professorDeviceIp != null) {
            isScanning = false
            navController.navigate(Screens.Home.name) {
                popUpTo(Screens.Registration.name) { inclusive = true }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Ricerca del dispositivo del professore in corso",
            style = MaterialTheme.typography.h5
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = ".".repeat(dotsCount),
                fontSize = 48.sp,
                color = MaterialTheme.colors.primary
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Attendere mentre ti connettiamo",
            style = MaterialTheme.typography.subtitle1
        )
    }
}