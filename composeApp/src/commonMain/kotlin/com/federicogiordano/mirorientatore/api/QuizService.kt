package com.federicogiordano.mirorientatore.api

import com.benasher44.uuid.uuid4
import com.federicogiordano.mirorientatore.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

class QuizService private constructor() {
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val quizzesFileName = "available_quizzes.json"

    private val _activeQuiz = MutableStateFlow<Quiz?>(null)
    val activeQuiz: StateFlow<Quiz?> = _activeQuiz.asStateFlow()

    private val _studentResults = MutableStateFlow<kotlin.collections.Map<String, StudentQuizResult>>(emptyMap<String, StudentQuizResult>())
    val studentResults: StateFlow<kotlin.collections.Map<String, StudentQuizResult>> = _studentResults.asStateFlow()

    private val _pendingAnswers = MutableStateFlow<List<QuizAnswer>>(emptyList())
    val pendingAnswers: StateFlow<List<QuizAnswer>> = _pendingAnswers.asStateFlow()

    private val _availableQuizzes = MutableStateFlow<List<Quiz>>(emptyList())
    val availableQuizzes: StateFlow<List<Quiz>> = _availableQuizzes.asStateFlow()

    init {
        serviceScope.launch {
            loadPersistedQuizzes()
        }
    }

    private suspend fun loadPersistedQuizzes() {
        withContext(Dispatchers.Default) {
            try {
                val jsonString = FileSystem.readTextFromFile(quizzesFileName)
                if (jsonString != null) {
                    val quizzes = json.decodeFromString(ListSerializer(Quiz.serializer()), jsonString)
                    _availableQuizzes.value = quizzes.sortedBy { it.title }
                    println("Caricati ${_availableQuizzes.value.size} quiz da $quizzesFileName")
                } else {
                    _availableQuizzes.value = emptyList()
                    println("File dei quiz ($quizzesFileName) non trovato o vuoto. Inizializzazione senza quiz.")
                }
            } catch (e: Exception) {
                _availableQuizzes.value = emptyList()
                println("Errore durante il caricamento dei quiz: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private suspend fun persistQuizzes(): Boolean {
        return withContext(Dispatchers.Default) {
            try {
                val jsonString = json.encodeToString(ListSerializer(Quiz.serializer()), _availableQuizzes.value)
                val success = FileSystem.writeTextToFile(quizzesFileName, jsonString)
                if (success) {
                    println("Salvati ${_availableQuizzes.value.size} quiz in $quizzesFileName")
                } else {
                    println("Impossibile salvare i quiz in $quizzesFileName. FileSystem potrebbe non essere stato inizializzato correttamente con il contesto Android.")
                }
                success
            } catch (e: Exception) {
                println("Errore durante il salvataggio dei quiz: ${e.message}")
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun sendQuizToStudents(quiz: Quiz) {
        resetActiveSessionData()
        _activeQuiz.value = quiz
        val webSocketServer = WebSocketServerManager.getInstance()
        val message = WebSocketMessage(
            type = "QUIZ",
            content = Json.encodeToString(Quiz.serializer(), quiz),
            sender = "professor"
        )
        println("Invio messaggio WebSocket: $message")
        webSocketServer.broadcastMessage(message)
    }

    fun addAnswer(answer: QuizAnswer) {
        val currentActiveQuiz = _activeQuiz.value
        if (currentActiveQuiz != null && currentActiveQuiz.id == answer.quizId) {
            val alreadyPending = _pendingAnswers.value.any { it.id == answer.id }
            val alreadyProcessed = _studentResults.value.values.any { result ->
                result.answers.any { ans -> ans.id == answer.id }
            }

            if (!alreadyPending && !alreadyProcessed) {
                _pendingAnswers.value = _pendingAnswers.value + answer
                println("Risposta da ${answer.studentName} per la domanda ${answer.questionId} aggiunta in sospeso. Totale in sospeso: ${_pendingAnswers.value.size}")
                evaluateAnswer(answer.id)
            } else {
                println("ID risposta ${answer.id} duplicato o già elaborato ricevuto. Ignorato.")
            }
        } else {
            println("Ricevuta risposta per quiz inattivo o non corrispondente. QuizID: ${answer.quizId}, ActiveQuizID: ${currentActiveQuiz?.id}")
        }
    }

    fun evaluateAnswer(answerId: String) {
        val answer = _pendingAnswers.value.find { it.id == answerId }
        if (answer == null) {
            println("Errore: Risposta con ID $answerId non trovata nelle risposte in sospeso per la valutazione.")
            return
        }

        val currentActiveQuiz = _activeQuiz.value
        if (currentActiveQuiz == null || answer.quizId != currentActiveQuiz.id) {
            println("Errore: Corrispondenza del quiz attivo non trovata o quiz non trovato durante la valutazione per la risposta $answerId. Rimozione da quelle in sospeso.")
            _pendingAnswers.value = _pendingAnswers.value.filterNot { it.id == answerId }
            return
        }

        val questionDefinition = currentActiveQuiz.questions.find { it.id == answer.questionId }
        if (questionDefinition == null) {
            println("Errore: Domanda con ID ${answer.questionId} non trovata nel quiz attivo ${currentActiveQuiz.id} per la risposta $answerId. Rimozione da quelle in sospeso.")
            _pendingAnswers.value = _pendingAnswers.value.filterNot { it.id == answerId }
            return
        }

        val correctAnswerText = questionDefinition.options.getOrNull(questionDefinition.correctOptionIndex)
        val isActuallyCorrect = (correctAnswerText != null && answer.answer == correctAnswerText)

        val evaluatedAnswer = answer.copy(
            question = questionDefinition.text,
            isCorrect = isActuallyCorrect
        )

        updateStudentResult(evaluatedAnswer)
        _pendingAnswers.value = _pendingAnswers.value.filterNot { it.id == answerId }

        serviceScope.launch {
            val webSocketServer = WebSocketServerManager.getInstance()
            val message = WebSocketMessage(
                type = "ANSWER_EVALUATED",
                content = Json.encodeToString(QuizAnswer.serializer(), evaluatedAnswer),
                sender = "professor"
            )
            println("Invio risposta auto-valutata allo studente ${evaluatedAnswer.studentId}: $message")
            webSocketServer.sendToStudent(
                evaluatedAnswer.studentId,
                message
            )
        }
    }

    private fun updateStudentResult(answer: QuizAnswer) {
        val currentResults = _studentResults.value.toMutableMap()
        val studentResult = currentResults[answer.studentId] ?: StudentQuizResult(
            studentId = answer.studentId,
            studentName = answer.studentName,
            quizId = answer.quizId,
            score = 0,
            totalPoints = 0,
            answers = emptyList()
        )

        val scoreIncrement = if (answer.isCorrect) 1 else 0
        val existingAnswerInResult = studentResult.answers.find { it.id == answer.id }
        val updatedAnswersList: List<QuizAnswer>
        var newScore = studentResult.score
        var newTotalPoints = studentResult.totalPoints


        if (existingAnswerInResult == null) {
            updatedAnswersList = studentResult.answers + answer
            newScore += scoreIncrement
            newTotalPoints += 1
        } else {
            updatedAnswersList = studentResult.answers.map { if (it.id == answer.id) answer else it }
            val oldScoreIncrement = if (existingAnswerInResult.isCorrect) 1 else 0
            newScore = studentResult.score - oldScoreIncrement + scoreIncrement
        }


        val updatedResult = studentResult.copy(
            score = newScore,
            totalPoints = newTotalPoints,
            answers = updatedAnswersList
        )

        currentResults[answer.studentId] = updatedResult
        _studentResults.value = currentResults
    }

    fun getAverageScore(): Float {
        val resultsList: List<StudentQuizResult> = _studentResults.value.values.toList()
        if (resultsList.isEmpty()) return 0f

        val currentActiveQuiz: Quiz? = _activeQuiz.value
        val totalPossiblePointsInQuiz = currentActiveQuiz?.questions?.size ?: 0
        if (totalPossiblePointsInQuiz == 0) return 0f

        var totalPercentageSum = 0f
        var participatingStudentsCount = 0

        resultsList.forEach { studentResult: StudentQuizResult ->
            if (studentResult.quizId == currentActiveQuiz?.id) {
                val percentage = if (totalPossiblePointsInQuiz > 0) {
                    (studentResult.score.toFloat() / totalPossiblePointsInQuiz.toFloat()) * 100f
                } else {
                    0f
                }
                totalPercentageSum += percentage
                participatingStudentsCount++
            }
        }
        return if (participatingStudentsCount > 0) totalPercentageSum / participatingStudentsCount.toFloat() else 0f
    }

    private fun resetActiveSessionData() {
        _activeQuiz.value = null
        _studentResults.value = emptyMap<String, StudentQuizResult>()
        _pendingAnswers.value = emptyList()
    }

    fun stopActiveQuiz() {
        val previouslyActiveQuizId = _activeQuiz.value?.id

        resetActiveSessionData()

        previouslyActiveQuizId?.let { quizId ->
            serviceScope.launch {
                val webSocketServer = WebSocketServerManager.getInstance()
                val message = WebSocketMessage(
                    type = "QUIZ_ENDED",
                    content = quizId,
                    sender = "professor"
                )
                println("Trasmissione messaggio di fine quiz per ID quiz: $quizId")
                webSocketServer.broadcastMessage(message)
            }
        }
    }

    fun saveQuiz(quiz: Quiz) {
        serviceScope.launch {
            val currentQuizzes = _availableQuizzes.value.toMutableList()
            val quizWithId = if (quiz.id.isBlank()) quiz.copy(id = uuid4().toString()) else quiz

            val existingIndex = currentQuizzes.indexOfFirst { it.id == quizWithId.id }
            if (existingIndex != -1) {
                currentQuizzes[existingIndex] = quizWithId
            } else {
                currentQuizzes.add(quizWithId)
            }
            _availableQuizzes.value = currentQuizzes.sortedBy { it.title }
            val persistenceSuccess = persistQuizzes()
            if (!persistenceSuccess) {
                println("Impossibile salvare il quiz dopo il salvataggio: ${quiz.title}. I dati potrebbero essere solo in memoria.")
            }
        }
    }

    fun deleteQuiz(quizId: String) {
        serviceScope.launch {
            _availableQuizzes.value = _availableQuizzes.value.filterNot { it.id == quizId }
            val persistenceSuccess = persistQuizzes()
            if (!persistenceSuccess) {
                println("Impossibile salvare la libreria dei quiz dopo l'eliminazione del quizId: $quizId. I dati potrebbero essere solo in memoria.")
            }
        }
    }

    fun getQuiz(quizId: String): Quiz? {
        return _availableQuizzes.value.find { it.id == quizId }
    }

    suspend fun importQuizzesFromJson(jsonContent: String): Boolean {
        return try {
            val importedQuizzes = json.decodeFromString(ListSerializer(Quiz.serializer()), jsonContent)
            val currentQuizzes = _availableQuizzes.value.toMutableList()
            val currentQuizMap = currentQuizzes.associateBy { it.id }.toMutableMap()

            importedQuizzes.forEach { importedQuiz ->
                val quizToImport = if (importedQuiz.id.isBlank()) importedQuiz.copy(id = uuid4().toString()) else importedQuiz
                currentQuizMap[quizToImport.id] = quizToImport
            }

            _availableQuizzes.value = currentQuizMap.values.toList().sortedBy { it.title }
            val persistenceSuccess = persistQuizzes()
            if (persistenceSuccess) {
                println("Quiz importati/aggiornati con successo da JSON e salvati. Totale quiz ora: ${_availableQuizzes.value.size}")
            } else {
                println("Quiz importati/aggiornati in memoria da JSON, ma NON è stato possibile salvarli. Totale quiz in memoria: ${_availableQuizzes.value.size}")
            }
            persistenceSuccess
        } catch (e: Exception) {
            println("Errore durante l'importazione dei quiz da JSON (errore di decodifica/elaborazione): ${e.message}")
            e.printStackTrace()
            false
        }
    }

    fun exportQuizzesToJson(): String? {
        return try {
            if (_availableQuizzes.value.isEmpty()) {
                println("Nessun quiz disponibile per l'esportazione.")
                return "[]"
            }
            json.encodeToString(ListSerializer(Quiz.serializer()), _availableQuizzes.value)
        } catch (e: Exception) {
            println("Errore durante l'esportazione dei quiz in JSON: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: QuizService? = null
        private val lock = Any()

        fun getInstance(context: Any? = null): QuizService =
            INSTANCE ?: synchronizedBlock(lock) {
                INSTANCE ?: QuizService().also { service ->
                    FileSystem.initialize(context)
                    INSTANCE = service
                }
            }
    }
}