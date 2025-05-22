package com.federicogiordano.mirorientatore.api

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

    private val _studentResults = MutableStateFlow<kotlin.collections.Map<String, StudentQuizResult>>(kotlin.collections.emptyMap<String, StudentQuizResult>())
    val studentResults: StateFlow<kotlin.collections.Map<String, StudentQuizResult>> = _studentResults.asStateFlow()

    private val _pendingAnswers = MutableStateFlow<kotlin.collections.List<QuizAnswer>>(kotlin.collections.emptyList())
    val pendingAnswers: StateFlow<kotlin.collections.List<QuizAnswer>> = _pendingAnswers.asStateFlow()

    private val _availableQuizzes = MutableStateFlow<kotlin.collections.List<Quiz>>(kotlin.collections.emptyList())
    val availableQuizzes: StateFlow<kotlin.collections.List<Quiz>> = _availableQuizzes.asStateFlow()

    init {
        serviceScope.launch {
            loadPersistedQuizzes()
        }
    }

    private suspend fun loadPersistedQuizzes() {
        withContext(Dispatchers.Default) {
            try {
                val fileContent = FileSystem.readTextFromFile(quizzesFileName)
                if (fileContent != null) {
                    val quizzes = json.decodeFromString(ListSerializer(Quiz.serializer()), fileContent)
                    _availableQuizzes.value = quizzes.sortedBy { it.title }
                    println("Successfully loaded ${_availableQuizzes.value.size} quizzes from $quizzesFileName")
                } else {
                    _availableQuizzes.value = kotlin.collections.emptyList()
                    println("No persisted quizzes file found or content was null: $quizzesFileName. Initializing with empty list.")
                }
            } catch (e: Exception) {
                println("Error loading persisted quizzes: ${e.message}")
                e.printStackTrace()
                _availableQuizzes.value = kotlin.collections.emptyList()
            }
        }
    }

    private suspend fun persistQuizzes() {
        withContext(Dispatchers.Default) {
            try {
                val quizzesJson = json.encodeToString(ListSerializer(Quiz.serializer()), _availableQuizzes.value)
                val success = FileSystem.writeTextToFile(quizzesFileName, quizzesJson)
                if (success) {
                    println("Successfully persisted ${_availableQuizzes.value.size} quizzes to $quizzesFileName")
                } else {
                    println("Failed to persist quizzes to $quizzesFileName")
                }
            } catch (e: Exception) {
                println("Error persisting quizzes: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun sendQuizToStudents(quiz: Quiz) {
        resetActiveSessionData()
        _activeQuiz.value = quiz
        val webSocketServer = WebSocketServerManager.getInstance()
        val message = WebSocketMessage(
            type = "QUIZ",
            content = Json.encodeToString(quiz),
            sender = "professor"
        )
        println("Sending WebSocket message: $message")
        webSocketServer.broadcastMessage(message)
    }

    fun addAnswer(answer: QuizAnswer) {
        if (_activeQuiz.value != null && _activeQuiz.value?.id == answer.quizId) {
            _pendingAnswers.value = _pendingAnswers.value + answer
        }
    }

    fun evaluateAnswer(answerId: String, isCorrect: Boolean) {
        val answer = _pendingAnswers.value.find { it.id == answerId } ?: return
        val currentActiveQuiz = _activeQuiz.value
        if (currentActiveQuiz == null || answer.quizId != currentActiveQuiz.id) return

        val evaluatedAnswer = answer.copy(isCorrect = isCorrect)
        updateStudentResult(evaluatedAnswer)
        _pendingAnswers.value = _pendingAnswers.value.filterNot { it.id == answerId }

        serviceScope.launch {
            val webSocketServer = WebSocketServerManager.getInstance()
            val message = WebSocketMessage(
                type = "ANSWER_EVALUATED",
                content = Json.encodeToString(evaluatedAnswer),
                sender = "professor"
            )
            println("Sending WebSocket message to student ${evaluatedAnswer.studentId}: $message") // Added log
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
            answers = kotlin.collections.emptyList()
        )

        val updatedResult = studentResult.copy(
            score = studentResult.score + if (answer.isCorrect) 1 else 0,
            totalPoints = studentResult.totalPoints + 1,
            answers = studentResult.answers + answer
        )

        currentResults[answer.studentId] = updatedResult
        _studentResults.value = currentResults
    }

    fun getAverageScore(): Float {
        val resultsList = _studentResults.value.values.toList()
        if (resultsList.isEmpty()) return 0f

        val currentActiveQuiz = _activeQuiz.value
        val totalPossibleScore = currentActiveQuiz?.questions?.size ?: 0
        if (totalPossibleScore == 0) return 0f

        var totalPercentageSum = 0f
        var participatingStudentsCount = 0

        resultsList.forEach { studentResult ->
            if (studentResult.quizId == currentActiveQuiz?.id && studentResult.totalPoints > 0) {
                totalPercentageSum += (studentResult.score.toFloat() / totalPossibleScore.toFloat()) * 100
                participatingStudentsCount++
            }
        }
        return if (participatingStudentsCount > 0) totalPercentageSum / participatingStudentsCount.toFloat() else 0f
    }


    private fun resetActiveSessionData() {
        _activeQuiz.value = null
        _studentResults.value = kotlin.collections.emptyMap<String, StudentQuizResult>()
        _pendingAnswers.value = kotlin.collections.emptyList()
    }

    fun stopActiveQuiz() {
        resetActiveSessionData()
    }


    fun saveQuiz(quiz: Quiz) {
        serviceScope.launch {
            val currentQuizzes = _availableQuizzes.value.toMutableList()
            val existingIndex = currentQuizzes.indexOfFirst { it.id == quiz.id }
            if (existingIndex != -1) {
                currentQuizzes[existingIndex] = quiz
            } else {
                currentQuizzes.add(quiz)
            }
            _availableQuizzes.value = currentQuizzes.sortedBy { it.title }
            persistQuizzes()
        }
    }

    fun deleteQuiz(quizId: String) {
        serviceScope.launch {
            _availableQuizzes.value = _availableQuizzes.value.filterNot { it.id == quizId }
            persistQuizzes()
        }
    }

    fun getQuiz(quizId: String): Quiz? {
        return _availableQuizzes.value.find { it.id == quizId }
    }

    companion object {
        @Volatile
        private var INSTANCE: QuizService? = null
        private val lock = Any()

        fun getInstance(context: Any? = null): QuizService =
            INSTANCE ?: synchronizedBlock(lock) {
                INSTANCE ?: QuizService().also {
                    FileSystem.initialize(context)
                    INSTANCE = it
                }
            }
    }
}