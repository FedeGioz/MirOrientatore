package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirorientatore.data.RobotStatus
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

class StatusService : BaseApiService() {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStatus(): RobotStatus {
        return try {
            val httpResponse: HttpResponse = client.get(ApiClient.getEndpoint("status"))
            val responseBodyText: String = httpResponse.bodyAsText()
            println("StatusService: Corpo della risposta API grezza: $responseBodyText")

            val parsedStatus = json.decodeFromString<RobotStatus>(responseBodyText)
            println("StatusService: Oggetto RobotStatus analizzato PRIMA DEL RITORNO - Batteria: ${parsedStatus.battery_percentage}, StateText: '${parsedStatus.stateText}', MapID: '${parsedStatus.mapId}'")
            println("STATO ANALIZZATO: $parsedStatus")
            parsedStatus
        } catch (e: Exception) {
            println("StatusService: Errore nella chiamata API o nell'analisi di getStatus(): ${e.message}")
            e.printStackTrace()
            val errorStatus = RobotStatus(stateText = "Errore nel recupero/analisi dello stato: ${e.message}")
            println("StatusService: Restituzione oggetto stato di errore - StateText: '${errorStatus.stateText}'")
            errorStatus
        }
    }

    fun statusFlow(intervalSeconds: Long = 10): Flow<RobotStatus> = flow {
        while (true) {
            try {

                emit(getStatus())
            } catch (e: Exception) {
                println("StatusService: Errore nel ciclo statusFlow: ${e.message}")
                val flowErrorStatus = RobotStatus(battery_percentage = 50f, stateText = "Errore di Connessione nel Flow")
                println("StatusService: Emissione oggetto stato di errore del flow - StateText: '${flowErrorStatus.stateText}'")
                emit(flowErrorStatus)
            }
            delay(intervalSeconds * 1000)
        }
    }
}