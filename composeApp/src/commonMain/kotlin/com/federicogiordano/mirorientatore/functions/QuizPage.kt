package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.data.*
import com.federicogiordano.mirorientatore.ui.AppScaffold
import kotlin.math.roundToInt
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

@Composable
fun QuizPage(navController: NavHostController) {
    val quizService = remember { QuizService.getInstance() }
    val pendingAnswers by quizService.pendingAnswers.collectAsState()
    val studentResults by quizService.studentResults.collectAsState()
    val activeQuiz by quizService.activeQuiz.collectAsState()
    var showCreateQuiz by remember { mutableStateOf(false) }
    var showResults by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AppScaffold(
        navController = navController,
        currentScreen = Screens.QuizAnswers
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = if (showResults) 1 else 0
            ) {
                Tab(
                    selected = !showResults,
                    onClick = { showResults = false },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Quiz Management")
                            if (pendingAnswers.isNotEmpty()) {
                                Badge { Text(pendingAnswers.size.toString()) }
                            }
                        }
                    }
                )
                Tab(
                    selected = showResults,
                    onClick = { showResults = true },
                    text = { Text("Results") }
                )
            }

            if (showResults) {
                QuizResultsScreen(
                    results = studentResults.values.toList(),
                    averageScore = quizService.getAverageScore()
                )
            } else {
                QuizManagementScreen(
                    activeQuiz = activeQuiz,
                    pendingAnswers = pendingAnswers,
                    onCreateQuiz = { showCreateQuiz = true },
                    onEvaluateAnswer = { answerId, isCorrect ->
                        quizService.evaluateAnswer(answerId, isCorrect)
                    }
                )
            }
        }
    }

    if (showCreateQuiz) {
        CreateQuizDialog(
            onDismiss = { showCreateQuiz = false },
            onCreateQuiz = { quiz ->
                showCreateQuiz = false
                coroutineScope.launch {
                    quizService.sendQuizToStudents(quiz)
                }
            }
        )
    }
}

@Composable
fun QuizManagementScreen(
    activeQuiz: Quiz?,
    pendingAnswers: List<QuizAnswer>,
    onCreateQuiz: () -> Unit,
    onEvaluateAnswer: (String, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                if (activeQuiz != null) {
                    Text(
                        "Active Quiz: ${activeQuiz.title}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Questions: ${activeQuiz.questions.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "No active quiz",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onCreateQuiz
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Create Quiz")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Create Quiz")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Pending Answers (${pendingAnswers.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (pendingAnswers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No pending answers")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(pendingAnswers) { answer ->
                    AnswerCard(
                        answer = answer,
                        onAccept = { onEvaluateAnswer(answer.id, true) },
                        onDecline = { onEvaluateAnswer(answer.id, false) }
                    )
                }
            }
        }
    }
}

@Composable
fun AnswerCard(
    answer: QuizAnswer,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = answer.studentName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatTimestamp(answer.timestamp),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Question: ${answer.question}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Answer: ${answer.answer}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onDecline,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Incorrect")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Incorrect")
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Correct")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Correct")
                }
            }
        }
    }
}

@Composable
fun QuizResultsScreen(
    results: List<StudentQuizResult>,
    averageScore: Float
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "Quiz Statistics",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Average Score: ${averageScore.roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Total Students: ${results.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            "Student Results",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn {
            items(results) { result ->
                StudentResultCard(result)
            }
        }
    }
}

@Composable
fun StudentResultCard(result: StudentQuizResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.studentName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "${result.score}/${result.totalPoints}",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            LinearProgressIndicator(
                progress = if (result.totalPoints > 0)
                    result.score.toFloat() / result.totalPoints
                else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Text(
                text = "Score: ${((result.score.toFloat() / result.totalPoints) * 100).roundToInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CreateQuizDialog(
    onDismiss: () -> Unit,
    onCreateQuiz: (Quiz) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var questions by remember { mutableStateOf(listOf(QuizQuestion(
        text = "",
        options = listOf("", "", "", ""),
        correctOptionIndex = 0
    ))) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Quiz") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Quiz Title") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Add questions and options here")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val quiz = Quiz(
                            title = title,
                            questions = questions
                        )
                        onCreateQuiz(quiz)
                    }
                }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val currentTime = Clock.System.now().toEpochMilliseconds()
    val seconds = (currentTime - timestamp) / 1000
    return when {
        seconds < 60 -> "Just now"
        seconds < 3600 -> "${seconds / 60}m ago"
        else -> "${seconds / 3600}h ago"
    }
}