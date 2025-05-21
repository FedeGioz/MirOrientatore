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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.federicogiordano.mirorientatore.data.QuizAnswer

actual class WebSocketServer {
    private val _connectedStudents = MutableStateFlow<List<StudentConnection>>(emptyList())
    actual val connectedStudents: StateFlow<List<StudentConnection>> = _connectedStudents.asStateFlow()

    private val studentSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val studentMap = ConcurrentHashMap<String, StudentConnection>()
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var pingJob: Job? = null

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
                        val connectionInfo = Json.decodeFromString<StudentConnection>(firstMessage.readText())

                        studentId = connectionInfo.id

                        studentSessions[studentId] = this
                        studentMap[studentId] = connectionInfo

                        _connectedStudents.value = studentMap.values.toList()

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val message = Json.decodeFromString<WebSocketMessage>(frame.readText())
                                scope.launch {
                                    processMessage(message, studentId)
                                }
                            }
                        }
                    } catch (e: Exception) {
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
            "QUIZ_ANSWER" -> {
                try {
                    val studentName = studentMap[senderId]?.name ?: "Studente Sconosciuto"

                    val answerContent = Json.decodeFromString<HashMap<String, String>>(message.content.toString())

                    val quizAnswer = QuizAnswer(
                        studentId = senderId,
                        studentName = studentName,
                        quizId = answerContent["quizId"] ?: "",
                        questionId = answerContent["questionId"] ?: "",
                        question = answerContent["question"] ?: "",
                        answer = answerContent["answer"] ?: ""
                    )

                    QuizService.getInstance().addAnswer(quizAnswer)

                    sendToStudent(senderId, WebSocketMessage(
                        type = "ANSWER_RECEIVED",
                        content = "La tua risposta è stata ricevuta",
                        sender = "professor"
                    ))

                    println("Ricevuta risposta al quiz da $studentName: ${answerContent["answer"]}")
                } catch (e: Exception) {
                    println("Errore nell'elaborazione della risposta al quiz: ${e.message}")
                }
            }
//            "HELP_REQUEST" -> {
//                // TODO
//            }
            else -> {
                broadcastMessage(message)
                println("MESSAGGIO RICEVUTO: ${message.type}")
            }
        }
    }

    actual suspend fun sendToStudent(studentId: String, message: WebSocketMessage) {
        studentSessions[studentId]?.let { session ->
            try {
                session.send(Frame.Text(Json.encodeToString(message)))
            } catch (e: Exception) {
                handleDisconnection(studentId)
            }
        }
    }

    actual suspend fun broadcastMessage(message: WebSocketMessage) {
        val messageJson = Json.encodeToString(message)
        val disconnectedIds = mutableListOf<String>()

        studentSessions.forEach { (id, session) ->
            try {
                session.send(Frame.Text(messageJson))
            } catch (e: Exception) {
                disconnectedIds.add(id)
            }
        }

        disconnectedIds.forEach { handleDisconnection(it) }
    }
}