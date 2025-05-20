package com.federicogiordano.mirorientatore.api

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
data class StudentConnection(
    val id: String,
    val name: String
)

@Serializable
data class WebSocketMessage(
    val type: String,
    val content: String,
    val sender: String
)

object WebSocketServerManager {
    private var instance: WebSocketServer? = null

    fun getInstance(): WebSocketServer {
        if (instance == null) {
            instance = WebSocketServer()
        }
        return instance!!
    }
}

expect class WebSocketServer() {
    val connectedStudents: StateFlow<List<StudentConnection>>

    fun start(port: Int = 8080)
    fun stop()
    suspend fun sendToStudent(studentId: String, message: WebSocketMessage)
    suspend fun broadcastMessage(message: WebSocketMessage)
}