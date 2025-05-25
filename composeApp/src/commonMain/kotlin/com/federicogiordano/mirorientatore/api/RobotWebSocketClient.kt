package com.federicogiordano.mirorientatore.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object RobotWebSocketManager {
    private val client = RobotWebSocketClient().connect()

    fun getClient(): RobotWebSocketClient = client
}

class RobotWebSocketClient(
    private val serverUrl: String = "ws://192.168.1.63:9090/",
    private val onConnectionStatus: (Boolean, String?) -> Unit = { _, _ -> }
) {

    private var session: DefaultWebSocketSession? = null
    private var job: Job? = null

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingIntervalMillis = 20_000
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var joystickToken: String? = null
    private var messageCount = 0
    private var isConnected = false
    private var connectionJob: Job? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5

    @Serializable
    data class RobotStateRequest(
        val op: String = "call_service",
        val id: String = "call_service:/mirsupervisor/setRobotState:7",
        val service: String = "/mirsupervisor/setRobotState",
        val args: RobotStateArgs
    )

    @Serializable
    data class RobotStateArgs(
        val robotState: Int = 11,
        val web_session_id: String = "4nn6sbubuetsmujl3ae96f9140"
    )

    @Serializable
    data class VelocityCommand(
        @SerialName("op") val op: String,
        @SerialName("id") val id: String,
        @SerialName("topic") val topic: String,
        @SerialName("msg") val msg: VelocityMessage,
        @SerialName("latch") val latch: Boolean
    )

    @Serializable
    data class VelocityMessage(
        val joystick_token: String,
        val speed_command: SpeedCommand
    )

    @Serializable
    data class SpeedCommand(
        val linear: Vector3,
        val angular: Vector3
    )

    @Serializable
    data class Vector3(
        val x: Float = 0f,
        val y: Float = 0f,
        val z: Float = 0f
    )
    // {"op":"call_service","id":"call_service:/mir_sound:6","service":"/mir_sound","args":{"action":0,"priority":2,"sound_guid":"089e7c22-04d1-11f0-b4a3-00012978ebab","length":2000,"volume":100}}
    @Serializable
    data class PlaySoundRequest(
        @SerialName("op") val op: String,
        @SerialName("id") val id: String,
        @SerialName("service") val service: String,
        @SerialName("args") val args: PlaySoundArgs
    )

    @Serializable
    data class PlaySoundArgs(
        val action: Int,
        val priority: Int,
        val sound_guid: String,
        val length: Int,
        val volume: Int
    )

    fun connect(): RobotWebSocketClient {
        if (connectionJob?.isActive == true) return this

        connectionJob = scope.launch {
            try {
                reconnectAttempts = 0
                connectWithRetry()
            } catch (e: Exception) {
                onConnectionStatus(false, "Connessione fallita: ${e.message}")
                println("Connessione WebSocket fallita: ${e.message}")
            }
        }

        return this
    }

    private suspend fun connectWithRetry() {
        while (reconnectAttempts < maxReconnectAttempts && !isConnected) {
            try {
                onConnectionStatus(false, "Connessione al robot in corso... (tentativo ${reconnectAttempts + 1})")
                println("Tentativo di connessione a WebSocket (${reconnectAttempts + 1}/$maxReconnectAttempts)")

                client.webSocket(
                    urlString = serverUrl,
                    request = {
                    }
                ) {
                    session = this
                    isConnected = true
                    reconnectAttempts = 0
                    println("WebSocket connesso con successo")
                    onConnectionStatus(true, null)

                    try {
                        for (frame in incoming) {
                            when (frame) {
                                is Frame.Text -> {
                                    val text = frame.readText()
                                    println("Messaggio ricevuto: $text")
                                    processMessage(text)
                                }
                                is Frame.Close -> {
                                    println("Connessione chiusa: ${frame.readReason()}")
                                    isConnected = false
                                    break
                                }
                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        println("Errore nell'elaborazione dei frame in entrata: ${e.message}")
                        isConnected = false
                    }
                }
            } catch (e: Exception) {
                println("Tentativo di connessione WebSocket fallito: ${e.message}")
                reconnectAttempts++

                if (reconnectAttempts < maxReconnectAttempts) {
                    val delayTime = (reconnectAttempts * 1000).toLong()
                    println("Riprovo tra ${delayTime/1000} secondi...")
                    delay(delayTime)
                }
            }
        }

        if (!isConnected) {
            onConnectionStatus(false, "Connessione fallita dopo $maxReconnectAttempts tentativi")
        }
    }

    private suspend fun sendMessage(message: String) {
        try {
            session?.send(Frame.Text(message))
        } catch (e: Exception) {
            println("Errore durante l'invio del messaggio: ${e.message}")
            isConnected = false
            connect()
        }
    }

    fun sendSoundPlayRequest(soundGuid: String){
        scope.launch{
            if (!isConnected || session == null) {
                println("Impossibile inviare il suono: WebSocket non connesso o sessione è null")
                connect()
                return@launch
            }

            val soundRequest = PlaySoundRequest(
                op = "call_service",
                id = "call_service:/mir_sound:6",
                service = "/mir_sound",
                args = PlaySoundArgs(
                    action = 0,
                    priority = 2,
                    sound_guid = soundGuid,
                    length = 100000,
                    volume = 100
                )
            )

            val jsonString = Json.encodeToString(soundRequest)
            println("INVIO SUONO: $jsonString")

            sendMessage(jsonString)
        }
    }

    private fun processMessage(message: String) {
        try {
            val jsonElement = Json.parseToJsonElement(message)
            val jsonObject = jsonElement.jsonObject

            if (jsonObject.containsKey("values") &&
                jsonObject["values"]?.jsonObject?.containsKey("joystick_token") == true) {
                joystickToken = jsonObject["values"]?.jsonObject?.get("joystick_token")?.jsonPrimitive?.content
                println("Token joystick ricevuto: $joystickToken")
            }
        } catch (e: Exception) {
            println("Errore nell'elaborazione del messaggio: ${e.message}")
        }
    }

    fun sendVelocity(linear: Float, angular: Float) {
        scope.launch {
            if (joystickToken == null) {
                println("Impossibile inviare la velocità: nessun token joystick disponibile")
                sendMessage("""{"op":"call_service","service":"/mir/get_joystick_token","id":"get_token"}""")
                return@launch
            }

            if (!isConnected || session == null) {
                println("Impossibile inviare la velocità: WebSocket non connesso o sessione è null")
                connect()
                return@launch
            }

            messageCount++
            val velocityCommand = VelocityCommand(
                op = "publish",
                id = "publish:/joystick_vel:$messageCount",
                topic = "/joystick_vel",
                msg = VelocityMessage(
                    joystick_token = joystickToken!!,
                    speed_command = SpeedCommand(
                        linear = Vector3(x = linear, y = 0f, z = 0f),
                        angular = Vector3(x = 0f, y = 0f, z = angular)
                    )
                ),
                latch = false
            )

            val jsonString = Json.encodeToString(velocityCommand)
            println("INVIO VELOCITÀ: $jsonString")
            sendMessage(jsonString)
        }
    }

    fun requestManualControl() {
        scope.launch {
            val json = Json {
                encodeDefaults = true
                isLenient = true
            }

            val request = RobotStateRequest(
                args = RobotStateArgs(
                    robotState = 11,
                    web_session_id = "4nn6sbubuetsmujl3ae96f9140"
                )
            )

            val jsonString = json.encodeToString(request)
            println("RICHIEDO CONTROLLO MANUALE: $jsonString")
            sendMessage(jsonString)
        }
    }

    fun startMissionQueue() {
        scope.launch {
            val json = Json {
                encodeDefaults = true
                isLenient = true
            }

            val request = RobotStateRequest(
                args = RobotStateArgs(
                    robotState = 3,
                    web_session_id = "4nn6sbubuetsmujl3ae96f9140"
                )
            )

            val jsonString = json.encodeToString(request)
            sendMessage(jsonString)
        }
    }

    fun stopMissionQueue() {
        scope.launch {
            val json = Json {
                encodeDefaults = true
                isLenient = true
            }

            val request = RobotStateRequest(
                args = RobotStateArgs(
                    robotState = 4,
                    web_session_id = "4nn6sbubuetsmujl3ae96f9140"
                )
            )

            val jsonString = json.encodeToString(request)
            sendMessage(jsonString)
        }
    }

    fun disconnect() {
        job?.cancel()
        scope.launch {
            try {
                session?.close()
                session = null
                isConnected = false
                onConnectionStatus(false, "Disconnesso")
            } catch (e: Exception) {
                println("Errore durante la disconnessione: ${e.message}")
            }
        }
    }
}