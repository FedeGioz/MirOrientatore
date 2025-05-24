package com.federicogiordano.mirorientatore.functions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavHostController
import com.federicogiordano.mirorientatore.Screens
import com.federicogiordano.mirorientatore.api.QuizService
import com.federicogiordano.mirorientatore.data.*
import com.federicogiordano.mirorientatore.ui.AppScaffold
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun QuizPage(navController: NavHostController) {
    val quizService = remember { QuizService.getInstance() }
    val pendingAnswers by quizService.pendingAnswers.collectAsState()
    val studentResults: Map<String, StudentQuizResult> by quizService.studentResults.collectAsState()
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
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuizManagementScreen(
                activeQuiz = activeQuiz,
                availableQuizzes = availableQuizzes,
                onSendQuiz = { quiz ->
                    coroutineScope.launch {
                        quizService.sendQuizToStudents(quiz)
                        showResults = false
                    }
                },
                onStopQuiz = {
                    coroutineScope.launch {
                        quizService.stopActiveQuiz()
                        showResults = true
                    }
                }
            )

            val currentActiveQuiz = activeQuiz
            if (currentActiveQuiz != null) {
                val processedAnswersForCurrentQuiz = studentResults.values
                    .filter { it.quizId == currentActiveQuiz.id }
                    .flatMap { it.answers }
                    .sortedByDescending { it.timestamp }

                if (pendingAnswers.isNotEmpty()) {
                    Text(
                        "Risposte in arrivo (${pendingAnswers.size})...",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(pendingAnswers, key = { "pending-${it.id}" }) { answer ->
                            AnswerCard(answer)
                        }
                    }
                } else if (processedAnswersForCurrentQuiz.isNotEmpty()) {
                    Text(
                        "Risposte ricevute (${processedAnswersForCurrentQuiz.size}):",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(processedAnswersForCurrentQuiz, key = { "processed-${it.id}" }) { answer ->
                            AnswerCard(answer)
                        }
                    }
                } else {
                    Text(
                        "Quiz attivo: ${currentActiveQuiz.title}. In attesa di risposte.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuizManagementScreen(
    activeQuiz: Quiz?,
    availableQuizzes: List<Quiz>,
    onSendQuiz: (Quiz) -> Unit,
    onStopQuiz: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (activeQuiz == null) {
            Text("Seleziona un Quiz da inviare:", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(8.dp))
            if (availableQuizzes.isEmpty()) {
                Text("Nessun quiz disponibile. Vai alla libreria per crearne uno.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    items(availableQuizzes) { quiz ->
                        AvailableQuizCard(quiz = quiz, onSendQuiz = { onSendQuiz(quiz) })
                    }
                }
            }
        } else {
            val quizService = remember { QuizService.getInstance() }

            Text("Quiz Attivo: ${activeQuiz.title}", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStopQuiz,
                enabled = activeQuiz != null,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(imageVector = Icons.Filled.Stop, contentDescription = "Ferma Quiz Attivo")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ferma Quiz")
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
            .padding(vertical = 4.dp)
            .clickable(onClick = onSendQuiz),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(quiz.title, style = MaterialTheme.typography.titleMedium)
                Text("Domande: ${quiz.questions.size}", style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.PlayArrow, contentDescription = "Invia Quiz")
        }
    }
}


@Composable
fun AnswerCard(
    answer: QuizAnswer
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (answer.isCorrect) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "${answer.studentName} (${formatTimestamp(answer.timestamp)})",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (answer.question.isNotBlank()) {
                Text("Domanda: ${answer.question}", style = MaterialTheme.typography.bodyMedium)
            }
            Text("Risposta: ${answer.answer}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Corretta: ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Icon(
                    imageVector = if (answer.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                    contentDescription = if (answer.isCorrect) "Corretta" else "Sbagliata",
                    tint = if (answer.isCorrect) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun StudentResultCard(result: StudentQuizResult) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(result.studentName, style = MaterialTheme.typography.titleMedium)
            Text(
                "Punteggio: ${result.score}/${result.totalPoints}",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun CreateQuizDialog(
    existingQuiz: Quiz? = null,
    onDismiss: () -> Unit,
    onCreateQuiz: (Quiz) -> Unit
) {
    var title by remember { mutableStateOf(existingQuiz?.title ?: "") }
    val questions = remember {
        existingQuiz?.questions?.map { it.copy(options = it.options.toMutableList()) }?.toMutableStateList()
            ?: mutableStateListOf(QuizQuestion(id = uuid4(), text = "", options = mutableStateListOf("", ""), correctOptionIndex = -1))
    }
    var titleError by remember { mutableStateOf<String?>(null) }

    val questionTextErrors = remember {
        val initialErrors: List<String?> = existingQuiz?.questions?.map { null } ?: List(questions.size) { null }
        initialErrors.toMutableStateList()
    }
    val optionTextErrors = remember {
        val initialOptionErrors: List<List<String?>> = existingQuiz?.questions?.map { question ->
            List(question.options.size) { null }
        } ?: List(questions.size) { List(questions.getOrNull(it)?.options?.size ?: 2) { null } }
        initialOptionErrors.toMutableStateList()
    }


    fun validateQuiz(): Boolean {
        var currentIsValid = true
        titleError = if (title.isBlank()) "Il titolo non può essere vuoto" else null
        if (titleError != null) currentIsValid = false

        while (questionTextErrors.size < questions.size) questionTextErrors.add(null)
        while (questionTextErrors.size > questions.size) questionTextErrors.removeLast()

        while (optionTextErrors.size < questions.size) {
            optionTextErrors.add(List(questions.getOrNull(optionTextErrors.size)?.options?.size ?: 2) { null })
        }
        while (optionTextErrors.size > questions.size) optionTextErrors.removeLast()

        questions.forEachIndexed { qIndex, question ->
            optionTextErrors[qIndex] = optionTextErrors[qIndex].toMutableList().apply {
                while (size < question.options.size) add(null)
                while (size > question.options.size) removeLast()
            }

            var questionSpecificErrorAccumulator = ""

            val qError = if (question.text.isBlank()) "La domanda non può essere vuota" else null
            questionTextErrors[qIndex] = qError
            if (qError != null) currentIsValid = false

            question.options.forEachIndexed { optIndex, option ->
                val oError = if (option.isBlank()) "L'opzione non può essere vuota" else null
                if (optIndex < optionTextErrors[qIndex].size) {
                    (optionTextErrors[qIndex] as MutableList<String?>)[optIndex] = oError
                }
                if (oError != null) currentIsValid = false
            }

            if (question.correctOptionIndex < 0 || question.correctOptionIndex >= question.options.size) {
                questionSpecificErrorAccumulator += " Devi selezionare una risposta corretta."
            } else if (question.options.getOrNull(question.correctOptionIndex)?.isBlank() == true) {
                questionSpecificErrorAccumulator += " La risposta corretta selezionata non può essere vuota."
            }

            if (questionSpecificErrorAccumulator.isNotEmpty()) {
                currentIsValid = false
                val existingError = questionTextErrors.getOrNull(qIndex)
                questionTextErrors[qIndex] = if (existingError.isNullOrBlank()) {
                    questionSpecificErrorAccumulator.trim()
                } else {
                    "$existingError $questionSpecificErrorAccumulator".trim()
                }
            }
        }
        return currentIsValid
    }

    AlertDialog(
        onDismissRequest = { /* Deliberately empty, use dismissButton */ },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.95f).fillMaxHeight(0.9f),
        title = { Text(if (existingQuiz == null) "Crea Nuovo Quiz" else "Modifica Quiz") },
        text = {
            Column(modifier = Modifier.fillMaxSize()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it; titleError = null },
                    label = { Text("Titolo del Quiz") },
                    isError = titleError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (titleError != null) {
                    Text(titleError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Domande:", style = MaterialTheme.typography.titleMedium)
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(questions, key = { _, q -> q.id }) { index, question ->
                        QuestionEditCard(
                            question = question,
                            questionIndex = index,
                            questionTextError = questionTextErrors.getOrNull(index),
                            optionTextErrors = optionTextErrors.getOrNull(index) ?: List(question.options.size) { null },
                            onQuestionChange = { updatedQuestion ->
                                questions[index] = updatedQuestion
                            },
                            onRemoveQuestion = {
                                if (questions.size > 1) questions.removeAt(index)
                            },
                            isLastQuestion = index == questions.size - 1
                        )
                    }
                }
                Button(
                    onClick = {
                        questions.add(QuizQuestion(id = uuid4(), text = "", options = mutableStateListOf("", ""), correctOptionIndex = -1))
                        questionTextErrors.add(null)
                        optionTextErrors.add(mutableStateListOf(null, null))
                    },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Text("Aggiungi Domanda")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (validateQuiz()) {
                    val newQuiz = Quiz(
                        id = existingQuiz?.id ?: uuid4(),
                        title = title,
                        questions = questions.toList().map { it.copy(options = it.options.toList()) }
                    )
                    onCreateQuiz(newQuiz)
                }
            }) { Text(if (existingQuiz == null) "Crea" else "Salva") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annulla") }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuestionEditCard(
    question: QuizQuestion,
    questionIndex: Int,
    questionTextError: String?,
    optionTextErrors: List<String?>,
    onQuestionChange: (QuizQuestion) -> Unit,
    onRemoveQuestion: () -> Unit,
    isLastQuestion: Boolean
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(
            value = question.text,
            onValueChange = { newText -> onQuestionChange(question.copy(text = newText)) },
            label = { Text("Domanda ${questionIndex + 1}") },
            isError = questionTextError != null,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (questionIndex > 0 || !isLastQuestion) {
                    IconButton(onClick = onRemoveQuestion) {
                        Icon(Icons.Filled.Delete, "Rimuovi domanda")
                    }
                }
            }
        )
        if (questionTextError != null) {
            Text(questionTextError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(8.dp))
        question.options.forEachIndexed { optIndex, option ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = option,
                    onValueChange = { newOptionText ->
                        val newOptions = question.options.toMutableList()
                        newOptions[optIndex] = newOptionText
                        onQuestionChange(question.copy(options = newOptions))
                    },
                    label = { Text("Opzione ${optIndex + 1}") },
                    isError = optionTextErrors.getOrNull(optIndex) != null,
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                )
                if (question.options.size > 2) {
                    IconButton(onClick = {
                        val newOptions = question.options.toMutableList()
                        newOptions.removeAt(optIndex)
                        val newCorrectIndex = if (question.correctOptionIndex == optIndex) -1
                        else if (question.correctOptionIndex > optIndex) question.correctOptionIndex - 1
                        else question.correctOptionIndex
                        onQuestionChange(question.copy(options = newOptions, correctOptionIndex = newCorrectIndex))
                    }) {
                        Icon(Icons.Filled.Close, "Rimuovi opzione")
                    }
                }
            }
            optionTextErrors.getOrNull(optIndex)?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
        Button(onClick = {
            val newOptions = question.options.toMutableList()
            newOptions.add("")
            onQuestionChange(question.copy(options = newOptions))
        }, modifier = Modifier.padding(top = 4.dp)) {
            Text("Aggiungi Opzione")
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Risposta Corretta:", style = MaterialTheme.typography.bodyMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            question.options.forEachIndexed { index, optionText ->
                if (optionText.isNotBlank()) {
                    FilterChip(
                        selected = index == question.correctOptionIndex,
                        onClick = { onQuestionChange(question.copy(correctOptionIndex = index)) },
                        label = { Text("Opzione ${index + 1}") }
                    )
                }
            }
        }
        if (!isLastQuestion) {
            Divider(modifier = Modifier.padding(top = 16.dp))
        }
    }
}


private fun formatTimestamp(timestamp: Long): String {
    val currentTime = Clock.System.now().toEpochMilliseconds()
    val diffMillis = currentTime - timestamp
    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        seconds < 5 -> "Ora"
        seconds < 60 -> "$seconds s fa"
        minutes < 2 -> "1 min fa"
        minutes < 60 -> "$minutes min fa"
        hours < 2 -> "1 h fa"
        hours < 24 -> "$hours h fa"
        else -> "${hours / 24} g fa"
    }
}

fun uuid4(): String = com.benasher44.uuid.uuid4().toString()