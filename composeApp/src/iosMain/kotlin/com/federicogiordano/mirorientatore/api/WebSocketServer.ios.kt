package com.federicogiordano.mirorientatore.api

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class WebSocketServer {
    private val _connectedStudents = MutableStateFlow<List<StudentConnection>>(emptyList())
    actual val connectedStudents: StateFlow<List<StudentConnection>> = _connectedStudents.asStateFlow()

    actual fun start(port: Int) {
        println("WebSocket server not supported on this platform")
    }

    actual fun stop() {
    }

    actual suspend fun sendToStudent(studentId: String, message: WebSocketMessage) {
    }

    actual suspend fun broadcastMessage(message: WebSocketMessage) {
    }
}