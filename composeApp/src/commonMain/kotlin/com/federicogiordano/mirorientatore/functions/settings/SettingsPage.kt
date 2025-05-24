package com.federicogiordano.mirorientatore.functions.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.federicogiordano.mirorientatore.Screens


@Composable
fun FunctionItemCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title)
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    onNavigateTo: (route: String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(Screens.Settings.title) }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(settingsFunctionsList) { settingsFunction ->
                FunctionItemCard(
                    title = settingsFunction.title,
                    icon = settingsFunction.icon,
                    onClick = {
                        when (settingsFunction) {
                            is SettingsFunction.QuizLibrary -> {
                                onNavigateTo(Screens.QuizLibrary.name)
                            }
                        }
                    }
                )
            }
        }
    }
}