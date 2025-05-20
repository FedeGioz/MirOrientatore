package com.federicogiordano.mirorientatore.data

import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class QuizAnswer(
    val id: String = generateRandomId(),
    val studentId: String,
    val studentName: String,
    val quizId: String,
    val questionId: String,
    val question: String,
    val answer: String,
    val isCorrect: Boolean = false,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

@Serializable
data class QuizQuestion(
    val id: String = generateRandomId(),
    val text: String,
    val options: List<String>,
    val correctOptionIndex: Int
)

@Serializable
data class Quiz(
    val id: String = generateRandomId(),
    val title: String,
    val questions: List<QuizQuestion>
)

@Serializable
data class StudentQuizResult(
    val studentId: String,
    val studentName: String,
    val quizId: String,
    val score: Int,
    val totalPoints: Int,
    val answers: List<QuizAnswer>
)

@Serializable
data class QuizSummary(
    val quiz: Quiz,
    val studentResults: List<StudentQuizResult>,
    val averageScore: Float
)

// X ovviare alla multipiattaforma (non compatibile con librerie Java)
private fun generateRandomId(): String {
    return List(16) { Random.nextInt(0, 16).toString(16) }.joinToString("")
}