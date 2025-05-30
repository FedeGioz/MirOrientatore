package com.federicogiordano.mirorientatore.functions.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.ui.graphics.vector.ImageVector

sealed class SettingsFunction(val route: String, val title: String, val icon: ImageVector) {
    data object QuizLibrary : SettingsFunction(
        route = "settings_quiz_library",
        title = "Libreria Quiz",
        icon = Icons.Filled.LibraryBooks
    ) }

val settingsFunctionsList = listOf(
    SettingsFunction.QuizLibrary
)