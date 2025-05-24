package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.data.Quiz

@Composable
fun QuizLibraryPage(
    navController: NavHostController,
    availableQuizzes: List<Quiz>,
    quizService: QuizService,
    onImportQuizzes: () -> Unit,
    onExportQuizzes: () -> Unit
) {
    var showCreateQuizDialog by remember { mutableStateOf(false) }
    var quizToEdit by remember { mutableStateOf<Quiz?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onImportQuizzes) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Importa Quiz")
                }
                IconButton(onClick = onExportQuizzes) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Esporta Quiz")
                }
            }

            if (availableQuizzes.isEmpty()) {
                Text(
                    text = "Nessun quiz disponibile. Clicca '+' per crearne uno nuovo, oppure importa quiz esistenti usando le icone in alto.",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(availableQuizzes, key = { it.id }) { quiz ->
                        QuizLibraryItemCard(
                            quiz = quiz,
                            onDeleteQuiz = { quizService.deleteQuiz(quiz.id) },
                            onEditQuiz = {
                                quizToEdit = quiz
                                showCreateQuizDialog = true
                            }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                quizToEdit = null
                showCreateQuizDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Crea Quiz")
        }
    }

    if (showCreateQuizDialog) {
        CreateQuizDialog(
            existingQuiz = quizToEdit,
            onDismiss = {
                showCreateQuizDialog = false
                quizToEdit = null
            },
            onCreateQuiz = { quiz ->
                quizService.saveQuiz(quiz)
                showCreateQuizDialog = false
                quizToEdit = null
            }
        )
    }
}

@Composable
fun QuizLibraryItemCard(
    quiz: Quiz,
    onDeleteQuiz: () -> Unit,
    onEditQuiz: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
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
            Row {
                IconButton(onClick = onEditQuiz) {
                    Icon(Icons.Filled.Edit, contentDescription = "Modifica Quiz")
                }
                IconButton(onClick = onDeleteQuiz) {
                    Icon(Icons.Filled.Delete, contentDescription = "Elimina Quiz")
                }
            }
        }
    }
}