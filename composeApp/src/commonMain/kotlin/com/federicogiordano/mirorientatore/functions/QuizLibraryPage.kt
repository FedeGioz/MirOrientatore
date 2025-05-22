package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.data.Quiz
import com.federicogiordano.mirorientatore.ui.AppScaffold

@Composable
fun QuizLibraryPage(navController: NavHostController) {
    val quizService = remember { QuizService.getInstance() }
    val availableQuizzes by quizService.availableQuizzes.collectAsState()
    var showCreateQuizDialog by remember { mutableStateOf(false) }

    AppScaffold(
        navController = navController,
        currentScreen = Screens.QuizLibrary,
        content = { innerPadding ->
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (availableQuizzes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Nessun quiz disponibile nella libreria.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(availableQuizzes, key = { it.id }) { quiz ->
                            QuizLibraryItem(
                                quiz = quiz,
                                onDeleteQuiz = { quizService.deleteQuiz(quiz.id) }
                            )
                        }
                    }
                }

                FloatingActionButton(
                    onClick = { showCreateQuizDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Crea Nuovo Quiz")
                }
            }
        }
    )

    if (showCreateQuizDialog) {
        CreateQuizDialog(
            onDismiss = { showCreateQuizDialog = false },
            onCreateQuiz = { quiz ->
                quizService.saveQuiz(quiz)
                showCreateQuizDialog = false
            }
        )
    }
}

@Composable
fun QuizLibraryItem(
    quiz: Quiz,
    onDeleteQuiz: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = quiz.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "Domande: ${quiz.questions.size}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            IconButton(onClick = onDeleteQuiz) {
                Icon(Icons.Filled.Delete, contentDescription = "Elimina Quiz")
            }
        }
    }
}