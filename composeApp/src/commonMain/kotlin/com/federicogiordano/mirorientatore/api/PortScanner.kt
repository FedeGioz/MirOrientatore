package com.federicogiordano.mirorientatore.api

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.pow

class PortScanner {
    private val _scanStatus = MutableStateFlow<ScanStatus>(ScanStatus.NotStarted)
    val scanStatus: StateFlow<ScanStatus> = _scanStatus

    suspend fun findProfessorDevice(): String? {
        _scanStatus.value = ScanStatus.Scanning

        return try {
            val localIpRange = getLocalIpRange()
            _scanStatus.value = ScanStatus.Scanning

            for (ip in localIpRange) {
                if (isPortOpen(ip, 8080)) {
                    _scanStatus.value = ScanStatus.Found(ip)
                    return ip
                }
            }

            _scanStatus.value = ScanStatus.NotFound
            null
        } catch (e: Exception) {
            _scanStatus.value = ScanStatus.Error(e.message ?: "Unknown error")
            null
        }
    }

    private suspend fun isPortOpen(ip: String, port: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = aSocket(SelectorManager(Dispatchers.IO))
                .tcp()
                .connect(InetSocketAddress(ip, port)) {
                    socketTimeout = 1000
                }
            socket.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun getLocalIpRange(): List<String> {
        return listOf(
            "192.168.0", "192.168.1", "192.168.2",
            "192.168.43",
            "10.0.0", "10.0.2"
        ).flatMap { prefix ->
            (1..254).map { "$prefix.$it" }
        }
    }

    sealed class ScanStatus {
        object NotStarted : ScanStatus()
        object Scanning : ScanStatus()
        object NotFound : ScanStatus()
        data class Found(val ipAddress: String) : ScanStatus()
        data class Error(val message: String) : ScanStatus()
    }
}