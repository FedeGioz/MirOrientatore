package com.federicogiordano.mirorientatore.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.federicogiordano.mirorientatore.viewmodels.StatusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusAppBar(
    statusViewModel: StatusViewModel,
    onLogout: () -> Unit = {}
) {
    val status by statusViewModel.status.collectAsState(null)

    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("MirOrientatore")
                StatusIndicator(status)
            }
        },
        actions = {
            IconButton(onClick = onLogout) {
                Icon(Icons.Default.Settings, contentDescription = "Logout")
            }
        }
    )
}

@Composable
fun StatusIndicator(status: Any?) {
    Box(
        modifier = Modifier.size(12.dp),
        contentAlignment = Alignment.Center
    ) {
        val color = if (status != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        Box(modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape))
    }
}