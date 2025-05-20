package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirorientatore.data.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class QuizService : BaseApiService() {
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    private val _activeQuiz = MutableStateFlow<Quiz?>(null)
    val activeQuiz = _activeQuiz.asStateFlow()

    private val _studentResults = MutableStateFlow(mapOf<String, StudentQuizResult>())
    val studentResults = _studentResults.asStateFlow()

    private val _pendingAnswers = MutableStateFlow<List<QuizAnswer>>(emptyList())
    val pendingAnswers = _pendingAnswers.asStateFlow()

    suspend fun sendQuizToStudents(quiz: Quiz) {
        _activeQuiz.value = quiz

        val webSocketServer = WebSocketServerManager.getInstance()
        webSocketServer.broadcastMessage(
            WebSocketMessage(
                type = "QUIZ",
                content = Json.encodeToString(quiz),
                sender = "professor"
            )
        )
    }

    fun addAnswer(answer: QuizAnswer) {
        val currentAnswers = _pendingAnswers.value
        _pendingAnswers.value = currentAnswers + answer
    }

    fun evaluateAnswer(answerId: String, isCorrect: Boolean) {
        val currentAnswers = _pendingAnswers.value
        val answer = currentAnswers.find { answerItem -> answerItem.id == answerId } ?: return
        val evaluatedAnswer = answer.copy(isCorrect = isCorrect)

        updateStudentResult(evaluatedAnswer)

        _pendingAnswers.value = currentAnswers.filter { answerItem -> answerItem.id != answerId }

        serviceScope.launch {
            val webSocketServer = WebSocketServerManager.getInstance()
            webSocketServer.sendToStudent(
                evaluatedAnswer.studentId,
                WebSocketMessage(
                    type = "QUIZ_RESULT",
                    content = Json.encodeToString(mapOf(
                        "answerId" to answerId,
                        "isCorrect" to isCorrect
                    )),
                    sender = "professor"
                )
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

        val updatedResult = studentResult.copy(
            score = studentResult.score + if (answer.isCorrect) 1 else 0,
            totalPoints = studentResult.totalPoints + 1,
            answers = studentResult.answers + answer
        )

        currentResults[answer.studentId] = updatedResult
        _studentResults.value = currentResults
    }

    fun getAverageScore(): Float {
        val results = _studentResults.value.values
        if (results.isEmpty()) return 0f

        val totalScore = results.sumOf { it.score }
        val totalQuestions = results.sumOf { it.totalPoints }

        return if (totalQuestions > 0) {
            (totalScore.toFloat() / totalQuestions) * 100
        } else 0f
    }

    fun reset() {
        _activeQuiz.value = null
        _studentResults.value = mapOf()
        _pendingAnswers.value = emptyList()
    }

    companion object {
        fun getInstance(): QuizService {
            return QuizService()
        }
    }
}