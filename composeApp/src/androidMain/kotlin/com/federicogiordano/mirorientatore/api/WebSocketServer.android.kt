package com.federicogiordano.mirorientatore.api

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.federicogiordano.mirorientatore.data.QuizAnswer

@Serializable
private data class ClientAnswerPayload(
    val quizId: String,
    val questionId: String,
    val selectedOption: String,
    val id: String? = null,
    val studentId: String? = null,
    val studentName: String? = null,
    val isCorrect: Boolean? = null
)

actual class WebSocketServer {
    private val _connectedStudents = MutableStateFlow<List<StudentConnection>>(emptyList())
    actual val connectedStudents: StateFlow<List<StudentConnection>> = _connectedStudents.asStateFlow()

    private val studentSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val studentMap = ConcurrentHashMap<String, StudentConnection>()
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pingJob: Job? = null

    private val jsonParser = Json { ignoreUnknownKeys = true }

    actual fun start(port: Int) {
        val serverInstance = embeddedServer(Netty, port = port) {
            install(WebSockets) {
                pingPeriod = Duration.parse("5s")
                timeout = Duration.parse("30s")
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            routing {
                webSocket("/connect") {
                    var studentId = ""
                    try {
                        val firstMessage = incoming.receive() as? Frame.Text ?: return@webSocket
                        val connectionInfo = jsonParser.decodeFromString<StudentConnection>(firstMessage.readText())

                        studentId = connectionInfo.id

                        studentSessions[studentId] = this
                        studentMap[studentId] = connectionInfo

                        _connectedStudents.value = studentMap.values.toList()

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val message = jsonParser.decodeFromString<WebSocketMessage>(frame.readText())
                                scope.launch {
                                    processMessage(message, studentId)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Error during WebSocket connection or message handling for $studentId: ${e.message}\n${e.stackTraceToString()}")
                        handleDisconnection(studentId)
                    }
                }
            }
        }

        serverInstance.start(wait = false)
        server = serverInstance.engine

        startPeriodicPing()
    }

    private fun startPeriodicPing() {
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive) {
                delay(10000)
                val disconnectedIds = mutableListOf<String>()
                studentSessions.forEach { (id, session) ->
                    try {
                        session.send(Frame.Ping(ByteArray(0)))
                    } catch (e: Exception) {
                        disconnectedIds.add(id)
                    }
                }
                disconnectedIds.forEach { handleDisconnection(it) }
            }
        }
    }

    private fun handleDisconnection(studentId: String) {
        if (studentId.isNotEmpty()) {
            studentSessions.remove(studentId)
            studentMap.remove(studentId)
            _connectedStudents.value = studentMap.values.toList()
            println("Studente disconnesso: $studentId")
        }
    }

    actual fun stop() {
        pingJob?.cancel()
        server?.stop(1000, 2000)
        server = null
        studentSessions.clear()
        studentMap.clear()
        _connectedStudents.value = emptyList()
    }

    private suspend fun processMessage(message: WebSocketMessage, senderId: String) {
        when (message.type) {
            "ANSWER", "QUIZ_ANSWER" -> {
                try {
                    val studentName = studentMap[senderId]?.name ?: "Studente Sconosciuto"

                    val answerPayload = jsonParser.decodeFromString<ClientAnswerPayload>(message.content)

                    val quizAnswer = QuizAnswer(
                        studentId = senderId,
                        studentName = studentName,
                        quizId = answerPayload.quizId,
                        questionId = answerPayload.questionId,
                        question = "",
                        answer = answerPayload.selectedOption
                    )

                    QuizService.getInstance().addAnswer(quizAnswer)

                    val confirmationMessage = WebSocketMessage(
                        type = "ANSWER_RECEIVED",
                        content = "La tua risposta è stata ricevuta",
                        sender = "professor"
                    )
                    sendToStudent(senderId, confirmationMessage)

                    println("Ricevuta risposta al quiz da $studentName (ID: $senderId): ${answerPayload.selectedOption}")
                } catch (e: Exception) {
                    println("Errore nell'elaborazione della risposta al quiz da $senderId: ${e.message}\n${e.stackTraceToString()}")
                }
            }
            else -> {
                println("Received unhandled WebSocket message from $senderId: type='${message.type}', content='${message.content}'")
            }
        }
    }

    actual suspend fun sendToStudent(studentId: String, message: WebSocketMessage) {
        studentSessions[studentId]?.let { session ->
            try {
                val messageJson = jsonParser.encodeToString(message)
                println("Sending message to student $studentId: $messageJson")
                session.send(Frame.Text(messageJson))
            } catch (e: Exception) {
                println("Failed to send message to student $studentId: ${e.message}")
                handleDisconnection(studentId)
            }
        }
    }

    actual suspend fun broadcastMessage(message: WebSocketMessage) {
        val messageJson = jsonParser.encodeToString(message)
        val disconnectedIds = mutableListOf<String>()

        println("Broadcasting message. Current student sessions: ${studentSessions.keys.joinToString(", ")}")

        studentSessions.forEach { (id, session) ->
            try {
                println("Attempting to send message to student $id")
                session.send(Frame.Text(messageJson))
                println("Successfully sent message to student $id")
            } catch (e: Exception) {
                println("Failed to send message to student $id: ${e.message}")
                disconnectedIds.add(id)
            }
        }

        if (disconnectedIds.isNotEmpty()) {
            println("Students to disconnect after broadcast: ${disconnectedIds.joinToString(", ")}")
        }
        disconnectedIds.forEach { handleDisconnection(it) }
    }
}