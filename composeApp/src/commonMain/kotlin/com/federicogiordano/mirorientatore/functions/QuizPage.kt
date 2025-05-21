package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
                            Text("Gestione Quiz")
                            if (pendingAnswers.isNotEmpty()) {
                                Badge { Text(pendingAnswers.size.toString()) }
                            }
                        }
                    }
                )
                Tab(
                    selected = showResults,
                    onClick = { showResults = true },
                    text = { Text("Risultati") }
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
                        "Quiz Attivo: ${activeQuiz.title}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Domande: ${activeQuiz.questions.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Nessun quiz attivo",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onCreateQuiz
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Crea Quiz")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Crea Quiz")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Risposte in Sospeso (${pendingAnswers.size})",
            style = MaterialTheme.typography.titleMedium
        )

        if (pendingAnswers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nessuna risposta in sospeso")
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
                text = "Domanda: ${answer.question}",
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Risposta: ${answer.answer}",
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
                    Icon(Icons.Default.Close, contentDescription = "Non corretto")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Non corretto")
                }

                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Corretto")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Corretto")
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
                    "Statistiche Quiz",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Punteggio Medio: ${averageScore.roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    "Studenti Totali: ${results.size}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Text(
            "Risultati Studenti",
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
                text = "Punteggio: ${((result.score.toFloat() / result.totalPoints) * 100).roundToInt()}%",
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
    val questions = remember {
        mutableStateListOf(
            QuizQuestion(
                text = "",
                options = List(2) { "" },
                correctOptionIndex = 0
            )
        )
    }
    var titleError by remember { mutableStateOf<String?>(null) }
    val questionTextErrors = remember { mutableStateListOf<String?>() }
    val optionTextErrors = remember { mutableStateListOf<List<String?>>() }


    fun validateQuiz(): Boolean {
        var isValid = true
        titleError = if (title.isBlank()) "Il titolo non può essere vuoto" else null
        if (titleError != null) isValid = false

        while (questionTextErrors.size < questions.size) questionTextErrors.add(null)
        while (questionTextErrors.size > questions.size) questionTextErrors.removeLast()
        while (optionTextErrors.size < questions.size) optionTextErrors.add(emptyList())
        while (optionTextErrors.size > questions.size) optionTextErrors.removeLast()

        questions.forEachIndexed { index, question ->
            if (question.text.isBlank()) {
                questionTextErrors[index] = "La domanda non può essere vuota"
                isValid = false
            } else {
                questionTextErrors[index] = null
            }

            if (question.options.size < 2) {
                questionTextErrors[index] = (questionTextErrors[index]?.plus(" ") ?: "") + "Deve avere almeno 2 opzioni."
                isValid = false
            }

            val currentOptionErrors = optionTextErrors[index].toMutableList()
            while (currentOptionErrors.size < question.options.size) currentOptionErrors.add(null)
            while (currentOptionErrors.size > question.options.size) currentOptionErrors.removeLast()
            optionTextErrors[index] = currentOptionErrors

            var questionHasOptionError = false
            question.options.forEachIndexed { optIndex, option ->
                if (option.isBlank()) {
                    val mutableCurrentOptionErrors = optionTextErrors[index].toMutableList()
                    mutableCurrentOptionErrors[optIndex] = "L'opzione non può essere vuota"
                    optionTextErrors[index] = mutableCurrentOptionErrors
                    isValid = false
                    questionHasOptionError = true
                } else {
                    val mutableCurrentOptionErrors = optionTextErrors[index].toMutableList()
                    if (optIndex < mutableCurrentOptionErrors.size) {
                        mutableCurrentOptionErrors[optIndex] = null
                        optionTextErrors[index] = mutableCurrentOptionErrors
                    }
                }
            }

            if (question.options.isNotEmpty() && (question.correctOptionIndex < 0 || question.correctOptionIndex >= question.options.size)) {
                val currentQError = questionTextErrors[index]
                val newError = "Seleziona una risposta corretta valida."
                questionTextErrors[index] = if (currentQError != null) "$currentQError $newError" else newError
                isValid = false
            }
        }
        return isValid
    }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crea Nuovo Quiz") },
        text = {
            Column(modifier = Modifier.heightIn(max = 600.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Titolo Quiz") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = titleError != null
                )
                if (titleError != null) {
                    Text(titleError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Domande:", style = MaterialTheme.typography.titleMedium)

                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(questions, key = { _, question -> question.id }) { index, question ->
                        Column(modifier = Modifier.padding(vertical = 8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = question.text,
                                    onValueChange = { newText ->
                                        questions[index] = question.copy(text = newText)
                                        if (index < questionTextErrors.size) questionTextErrors[index] = null
                                    },
                                    label = { Text("Domanda ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    isError = index < questionTextErrors.size && questionTextErrors[index] != null
                                )
                                if (questions.size > 1) {
                                    IconButton(onClick = {
                                        questions.removeAt(index)
                                        if (index < questionTextErrors.size) questionTextErrors.removeAt(index)
                                        if (index < optionTextErrors.size) optionTextErrors.removeAt(index)
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Rimuovi domanda")
                                    }
                                }
                            }
                            if (index < questionTextErrors.size && questionTextErrors[index] != null) {
                                Text(questionTextErrors[index]!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Opzioni e Risposta Corretta:", style = MaterialTheme.typography.bodyMedium)
                            question.options.forEachIndexed { optionIndex, optionText ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    RadioButton(
                                        selected = question.correctOptionIndex == optionIndex,
                                        onClick = { questions[index] = question.copy(correctOptionIndex = optionIndex) }
                                    )
                                    OutlinedTextField(
                                        value = optionText,
                                        onValueChange = { newOptionText ->
                                            val newOptions = question.options.toMutableList()
                                            newOptions[optionIndex] = newOptionText
                                            questions[index] = question.copy(options = newOptions)
                                            if (index < optionTextErrors.size && optionIndex < optionTextErrors[index].size) {
                                                val currentOptErrs = optionTextErrors[index].toMutableList()
                                                currentOptErrs[optionIndex] = null
                                                optionTextErrors[index] = currentOptErrs
                                            }
                                        },
                                        label = { Text("Opzione ${optionIndex + 1}") },
                                        modifier = Modifier.weight(1f),
                                        isError = index < optionTextErrors.size &&
                                                optionIndex < optionTextErrors[index].size &&
                                                optionTextErrors[index][optionIndex] != null
                                    )
                                    if (question.options.size > 2) {
                                        IconButton(onClick = {
                                            val currentOptions = question.options.toMutableList()
                                            currentOptions.removeAt(optionIndex)

                                            var newCorrectIndex = question.correctOptionIndex
                                            if (newCorrectIndex == optionIndex) {
                                                newCorrectIndex = 0
                                            } else if (newCorrectIndex > optionIndex) {
                                                newCorrectIndex--
                                            }
                                            if (newCorrectIndex >= currentOptions.size && currentOptions.isNotEmpty()) {
                                                newCorrectIndex = currentOptions.size -1
                                            } else if (currentOptions.isEmpty()) {
                                                newCorrectIndex = -1
                                            }


                                            questions[index] = question.copy(options = currentOptions, correctOptionIndex = newCorrectIndex)

                                            if (index < optionTextErrors.size) {
                                                val optErrs = optionTextErrors[index].toMutableList()
                                                if (optionIndex < optErrs.size) optErrs.removeAt(optionIndex)
                                                optionTextErrors[index] = optErrs
                                            }
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Rimuovi opzione")
                                        }
                                    }
                                }
                                if (index < optionTextErrors.size &&
                                    optionIndex < optionTextErrors[index].size &&
                                    optionTextErrors[index][optionIndex] != null
                                ) {
                                    Text(
                                        optionTextErrors[index][optionIndex]!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(start = 48.dp)
                                    )
                                }
                            }
                            if (question.options.size < 4) {
                                TextButton(
                                    onClick = {
                                        val newOptions = question.options.toMutableList()
                                        newOptions.add("")
                                        questions[index] = question.copy(options = newOptions)
                                        if (index < optionTextErrors.size) {
                                            val optErrs = optionTextErrors[index].toMutableList()
                                            optErrs.add(null)
                                            optionTextErrors[index] = optErrs
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Aggiungi opzione")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Aggiungi Opzione")
                                }
                            }
                        }
                        if (index < questions.size - 1) {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        questions.add(
                            QuizQuestion(
                                text = "",
                                options = List(2) { "" },
                                correctOptionIndex = 0
                            )
                        )
                        questionTextErrors.add(null)
                        optionTextErrors.add(List(2) { null })
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Aggiungi Domanda")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aggiungi Domanda")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (validateQuiz()) {
                        val finalQuestions = questions.map { q ->
                            val validCorrectIndex = if (q.options.isEmpty()) -1 else q.correctOptionIndex.coerceIn(0, q.options.size - 1)
                            q.copy(correctOptionIndex = validCorrectIndex)
                        }
                        val quiz = Quiz(
                            title = title,
                            questions = finalQuestions.toList()
                        )
                        onCreateQuiz(quiz)
                    }
                }
            ) {
                Text("Crea")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annulla")
            }
        }
    )
}

private fun formatTimestamp(timestamp: Long): String {
    val currentTime = Clock.System.now().toEpochMilliseconds()
    val seconds = (currentTime - timestamp) / 1000
    return when {
        seconds < 60 -> "Pochi secondi fa"
        seconds < 3600 -> "${seconds / 60}m fa"
        else -> "${seconds / 3600}h fa"
    }
}