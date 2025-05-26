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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration
import java.util.concurrent.ConcurrentHashMap
import com.federicogiordano.mirorientatore.data.QuizAnswer
import com.federicogiordano.mirorientatore.data.RobotStatus

@Serializable
private data class ClientAnswerPayload(
    val quizId: String,
    val questionId: String,
    val answer: String,
    val id: String? = null,
    val studentId: String? = null,
    val studentName: String? = null,
    val isCorrect: Boolean? = null
)

@Serializable
private data class RobotVelocityControlPayload(
    @SerialName("msg") val msg: RobotVelocityMessage
)

@Serializable
private data class RobotVelocityMessage(
    @SerialName("speed_command") val speedCommand: RobotSpeedCommand
)

@Serializable
private data class RobotSpeedCommand(
    @SerialName("linear") val linear: RobotVector3,
    @SerialName("angular") val angular: RobotVector3
)

@Serializable
private data class RobotVector3(
    @SerialName("x") val x: Float = 0f,
    @SerialName("y") val y: Float = 0f,
    @SerialName("z") val z: Float = 0f
)


actual class WebSocketServer {
    private val _connectedStudents = MutableStateFlow<List<StudentConnection>>(emptyList())
    actual val connectedStudents: StateFlow<List<StudentConnection>> = _connectedStudents.asStateFlow()

    private val studentSessions = ConcurrentHashMap<String, DefaultWebSocketServerSession>()
    private val studentMap = ConcurrentHashMap<String, StudentConnection>()
    private var server: ApplicationEngine? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var pingJob: Job? = null
    private var statusUpdateJob: Job? = null

    private val jsonParser = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val statusService = StatusService()
    private val robotWebSocketClient = RobotWebSocketManager.getClient()


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

                        val mapImage = MappingService().getMapImage(StatusService().getStatus().mapId)
                        val message = WebSocketMessage(
                            type = "MAP_IMAGE",
                            content = mapImage,
                            sender = "professor"
                        )
                        WebSocketServer().sendToStudent(studentId, message)
                        println("Studente connesso: $studentId - ${connectionInfo.name}. Totale: ${studentMap.size}")

                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val messageText = frame.readText()
                                try {
                                    val message = jsonParser.decodeFromString<WebSocketMessage>(messageText)
                                    scope.launch {
                                        processMessage(message, studentId)
                                    }
                                } catch (e: Exception) {
                                    println("Errore deserializzando messaggio da $studentId: $messageText, Errore: ${e.message}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        println("Errore durante la connessione WebSocket o la gestione dei messaggi per $studentId: ${e.message}\n${e.stackTraceToString()}")
                    } finally {
                        handleDisconnection(studentId)
                        println("Sessione WebSocket terminata per: $studentId. Studenti rimanenti: ${studentMap.size}")
                    }
                }
            }
        }

        serverInstance.start(wait = false)
        server = serverInstance.engine

        startPeriodicPing()
        startStatusUpdates()
        println("WebSocketServer avviato sulla porta $port")
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
                        println("Ping fallito per lo studente $id, disconnessione in corso.")
                        disconnectedIds.add(id)
                    }
                }
                disconnectedIds.forEach { handleDisconnection(it) }
            }
        }
    }

    private fun startStatusUpdates() {
        statusUpdateJob?.cancel()
        statusUpdateJob = scope.launch {
            println("Avvio della raccolta degli aggiornamenti di stato del robot...")
            try {
                statusService.statusFlow(intervalSeconds = 10).collect { robotStatus ->
                    val statusJson = try {
                        jsonParser.encodeToString(robotStatus)
                    } catch (e: Exception) {
                        println("Errore durante la serializzazione di RobotStatus: ${e.message}")
                        return@collect
                    }

                    if (statusJson == "{}" || statusJson.length < 20) {
                        println("WebSocketServer: Warning - Serialized RobotStatus for broadcast is unexpectedly short or empty: '$statusJson'. Original status object: $robotStatus")
                    }

                    val statusMessage = WebSocketMessage(
                        type = "ROBOT_STATUS",
                        content = statusJson,
                        sender = "server"
                    )

                    println("Invio aggiornamento stato robot: $statusJson")
                    broadcastMessage(statusMessage)
                }
            } catch (e: Exception) {
                println("Errore nel flusso di aggiornamento dello stato del robot: ${e.message}\n${e.stackTraceToString()}")
            }
        }
    }

    private fun handleDisconnection(studentId: String) {
        if (studentId.isNotEmpty()) {
            studentSessions.remove(studentId)
            val removedStudent = studentMap.remove(studentId)
            if (removedStudent != null) {
                _connectedStudents.value = studentMap.values.toList()
                println("Studente disconnesso: $studentId - ${removedStudent.name}. Studenti rimanenti: ${studentMap.size}")
            }
        }
    }

    actual fun stop() {
        println("Arresto del WebSocketServer...")
        statusUpdateJob?.cancel()
        pingJob?.cancel()
        scope.launch {
            studentSessions.forEach { (id, session) ->
                try {
                    session.close(CloseReason(CloseReason.Codes.GOING_AWAY, "Server shutdown"))
                } catch (e: Exception) {
                    println("Errore durante la chiusura della sessione per $id: ${e.message}")
                }
            }
            studentSessions.clear()
            studentMap.clear()
            _connectedStudents.value = emptyList()
        }

        server?.stop(1000, 2000)
        server = null
        scope.coroutineContext[Job]?.cancel()
        println("WebSocketServer arrestato.")
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
                        answer = answerPayload.answer
                    )

                    QuizService.getInstance().addAnswer(quizAnswer)

                    val confirmationMessage = WebSocketMessage(
                        type = "ANSWER_RECEIVED",
                        content = "La tua risposta è stata ricevuta",
                        sender = "professor"
                    )
                    sendToStudent(senderId, confirmationMessage)
                    println("Ricevuta risposta al quiz da $studentName (ID: $senderId): ${answerPayload.answer}")

                } catch (e: Exception) {
                    println("Errore nell'elaborazione della risposta al quiz da $senderId: ${e.message}\n${e.stackTraceToString()}")
                    val errorMessage = WebSocketMessage(
                        type = "ERROR",
                        content = "Errore nell'elaborazione della tua risposta: ${e.localizedMessage}",
                        sender = "server"
                    )
                    sendToStudent(senderId, errorMessage)
                }
            }
            "ROBOT_CONTROL_VELOCITY" -> {
                try {
                    println("Ricevuto ROBOT_CONTROL_VELOCITY da $senderId: ${message.content}")
                    val velocityPayload = jsonParser.decodeFromString<RobotVelocityControlPayload>(message.content)
                    val linearX = velocityPayload.msg.speedCommand.linear.x
                    val angularZ = velocityPayload.msg.speedCommand.angular.z
                    robotWebSocketClient.sendVelocity(linearX, angularZ)
                    println("Inoltrato comando velocità al robot: linearX=$linearX, angularZ=$angularZ")
                } catch (e: Exception) {
                    println("Errore nell'elaborazione del comando ROBOT_CONTROL_VELOCITY da $senderId: ${e.message}\n${e.stackTraceToString()}")
                    val errorMessage = WebSocketMessage(
                        type = "ERROR",
                        content = "Errore nell'elaborazione del comando di velocità: ${e.localizedMessage}",
                        sender = "server"
                    )
                    sendToStudent(senderId, errorMessage)
                }
            }
            else -> {
                println("Ricevuto messaggio WebSocket non gestito da $senderId: type='${message.type}', content='${message.content}'")
            }
        }
    }

    actual suspend fun sendToStudent(studentId: String, message: WebSocketMessage) {
        studentSessions[studentId]?.let { session ->
            try {
                if (session.isActive) {
                    val messageJson = jsonParser.encodeToString(message)
                    session.send(Frame.Text(messageJson))
                } else {
                    println("Tentativo di invio su sessione non attiva per lo studente $studentId.")
                    handleDisconnection(studentId)
                }
            } catch (e: Exception) {
                println("Invio messaggio fallito allo studente $studentId: ${e.message}")
                handleDisconnection(studentId)
            }
        }
    }

    actual suspend fun broadcastMessage(message: WebSocketMessage) {
        if (studentSessions.isEmpty()) {
            return
        }
        val messageJson = jsonParser.encodeToString(message)
        val disconnectedIds = mutableListOf<String>()
        studentSessions.forEach { (id, session) ->
            try {
                if (session.isActive) {
                    session.send(Frame.Text(messageJson))
                } else {
                    println("Tentativo di broadcast su sessione non attiva per lo studente $id.")
                    disconnectedIds.add(id)
                }
            } catch (e: Exception) {
                println("Invio messaggio broadcast fallito allo studente $id: ${e.message}")
                disconnectedIds.add(id)
            }
        }
        if (disconnectedIds.isNotEmpty()) {
            println("Studenti da disconnettere dopo il broadcast: ${disconnectedIds.joinToString(", ")}")
        }
        disconnectedIds.forEach { handleDisconnection(it) }
    }


    actual suspend fun allowJoystickForStudent(studentId: String) {
        studentMap[studentId]?.let { currentStudent ->
            if (!currentStudent.hasJoystickAccess) {
                val otherStudentsWithJoystick = studentMap.values.filter { it.id != studentId && it.hasJoystickAccess }
                otherStudentsWithJoystick.forEach { otherStudent ->
                    studentMap[otherStudent.id] = otherStudent.copy(hasJoystickAccess = false)
                    sendToStudent(
                        otherStudent.id,
                        WebSocketMessage("DISABLE_JOYSTICK", "Controllo joystick assegnato ad altro studente.", "professor")
                    )
                }


                val updatedStudent = currentStudent.copy(hasJoystickAccess = true)
                studentMap[studentId] = updatedStudent
                _connectedStudents.value = studentMap.values.toList().sortedBy { it.name }
                sendToStudent(
                    studentId,
                    WebSocketMessage("ALLOW_JOYSTICK", "Controllo joystick abilitato", "professor")
                )
                println("Server: Joystick access GRANTED for student $studentId - ${updatedStudent.name}")

                if (otherStudentsWithJoystick.isNotEmpty()) {
                    _connectedStudents.value = studentMap.values.toList().sortedBy { it.name }
                }
            }
        } ?: println("Server: Attempted to allow joystick for unknown student $studentId")
    }

    actual suspend fun revokeJoystickForStudent(studentId: String) {
        studentMap[studentId]?.let { currentStudent ->
            if (currentStudent.hasJoystickAccess) {
                val updatedStudent = currentStudent.copy(hasJoystickAccess = false)
                studentMap[studentId] = updatedStudent
                _connectedStudents.value = studentMap.values.toList().sortedBy { it.name }
                sendToStudent(
                    studentId,
                    WebSocketMessage("DISABLE_JOYSTICK", "Controllo joystick revocato", "professor")
                )
                println("Server: Joystick access REVOKED for student $studentId - ${updatedStudent.name}")
            }
        } ?: println("Server: Attempted to revoke joystick for unknown student $studentId")
    }

    actual suspend fun revokeAllJoystickAccess() {
        val studentIds = studentMap.keys().toList()
        var stateChanged = false
        for (id in studentIds) {
            studentMap[id]?.let { currentStudent ->
                if (currentStudent.hasJoystickAccess) {
                    studentMap[id] = currentStudent.copy(hasJoystickAccess = false)
                    stateChanged = true
                    sendToStudent(
                        id,
                        WebSocketMessage("DISABLE_JOYSTICK", "Controllo joystick revocato da remoto.", "professor")
                    )
                }
            }
        }

        if (stateChanged) {
            _connectedStudents.value = studentMap.values.toList().sortedBy { it.name }
            println("Server: All joystick access revoked and internal state updated.")
        }
    }
}