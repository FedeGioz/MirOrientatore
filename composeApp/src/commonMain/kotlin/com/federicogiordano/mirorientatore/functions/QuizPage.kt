package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
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
    val availableQuizzes by quizService.availableQuizzes.collectAsState()
    var showResults by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    AppScaffold(
        navController = navController,
        currentScreen = Screens.QuizLauncher
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
                                Spacer(Modifier.width(8.dp))
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
                    availableQuizzes = availableQuizzes,
                    onSendQuiz = { quiz ->
                        coroutineScope.launch {
                            quizService.sendQuizToStudents(quiz)
                        }
                    },
                    onEvaluateAnswer = { answerId, isCorrect ->
                        quizService.evaluateAnswer(answerId, isCorrect)
                    },
                    onStopQuiz = {
                        quizService.stopActiveQuiz()
                    }
                )
            }
        }
    }
}

@Composable
fun QuizManagementScreen(
    activeQuiz: Quiz?,
    pendingAnswers: List<QuizAnswer>,
    availableQuizzes: List<Quiz>,
    onSendQuiz: (Quiz) -> Unit,
    onEvaluateAnswer: (String, Boolean) -> Unit,
    onStopQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        if (activeQuiz != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Quiz Attivo: ${activeQuiz.title}",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        "Domande: ${activeQuiz.questions.size}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onStopQuiz,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = "Termina Quiz")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Termina Quiz Attivo")
                    }
                }
            }
        } else {
            Text(
                "Avvia Quiz dalla Libreria",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            if (availableQuizzes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nessun quiz salvato. Vai alla Libreria Quiz per crearne uno.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableQuizzes, key = { it.id }) { quiz ->
                        AvailableQuizCard(
                            quiz = quiz,
                            onSendQuiz = { onSendQuiz(quiz) }
                        )
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
                    .weight(1f)
                    .heightIn(min = 100.dp),
                contentAlignment = Alignment.Center
            ) {
                if (activeQuiz != null) {
                    Text("Nessuna risposta in sospeso per il quiz attivo.")
                } else if (availableQuizzes.isNotEmpty()){
                    Text("Seleziona un quiz dalla libreria per iniziare.")
                } else {
                    Text("Vai alla Libreria Quiz per creare un quiz e avviarlo da lì.")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(pendingAnswers, key = { it.id }) { answer ->
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
fun AvailableQuizCard(
    quiz: Quiz,
    onSendQuiz: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSendQuiz),
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
            IconButton(
                onClick = onSendQuiz,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Avvia Quiz",
                    tint = MaterialTheme.colorScheme.primary
                )
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

        if (results.isEmpty()){
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center){
                Text("Nessun risultato disponibile.")
            }
        } else {
            LazyColumn {
                items(results, key = { it.studentId + it.quizId }) { result ->
                    StudentResultCard(result)
                }
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
                    result.score.toFloat() / result.totalPoints.toFloat()
                else 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            val percentage = if (result.totalPoints > 0) (result.score.toFloat() / result.totalPoints.toFloat()) * 100 else 0f
            Text(
                text = "Punteggio: ${percentage.roundToInt()}%",
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
                val currentError = questionTextErrors[index]
                val newError = "Deve avere almeno 2 opzioni."
                questionTextErrors[index] = if (currentError != null) "$currentError $newError" else newError
                isValid = false
            }

            val currentOptionErrors = optionTextErrors[index].toMutableList()
            while (currentOptionErrors.size < question.options.size) currentOptionErrors.add(null)
            while (currentOptionErrors.size > question.options.size) currentOptionErrors.removeLast()
            optionTextErrors[index] = currentOptionErrors

            question.options.forEachIndexed { optIndex, option ->
                if (option.isBlank()) {
                    val mutableCurrentOptionErrors = optionTextErrors[index].toMutableList()
                    mutableCurrentOptionErrors[optIndex] = "L'opzione non può essere vuota"
                    optionTextErrors[index] = mutableCurrentOptionErrors
                    isValid = false
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
                    isError = titleError != null,
                    singleLine = true
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
                                                optionTextErrors[index][optionIndex] != null,
                                        singleLine = true
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

                                            if (currentOptions.isNotEmpty()) {
                                                newCorrectIndex = newCorrectIndex.coerceIn(0, currentOptions.size - 1)
                                            } else {
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

                            val validCorrectIndex = if (q.options.isEmpty()) -1
                            else q.correctOptionIndex.coerceIn(0, q.options.size - 1)
                            q.copy(correctOptionIndex = validCorrectIndex)
                        }
                        val quiz = Quiz(
                            title = title.trim(),
                            questions = finalQuestions.filter { it.text.isNotBlank() && it.options.size >= 2 && it.options.all { opt -> opt.isNotBlank() } }
                        )
                        if (quiz.questions.isNotEmpty()){
                            onCreateQuiz(quiz)
                        }
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
    val diffMillis = currentTime - timestamp
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 60 -> "Pochi secondi fa"
        minutes < 60 -> "${minutes}m fa"
        hours < 24 -> "${hours}h fa"
        else -> "${hours / 24}g fa"
    }
}