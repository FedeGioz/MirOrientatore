package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import com.federicogiordano.mirorientatore.data.RobotMission
import com.federicogiordano.mirorientatore.data.RobotStatus
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

class StatusService : BaseApiService() {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStatus(): RobotStatus {
        return try {
            val httpResponse: HttpResponse = client.get(ApiClient.getEndpoint("status"))
            val responseBodyText: String = httpResponse.bodyAsText()
            println("StatusService: Raw API response body: $responseBodyText")

            val parsedStatus = json.decodeFromString<RobotStatus>(responseBodyText)
            println("StatusService: Parsed RobotStatus object BEFORE RETURN - Battery: ${parsedStatus.battery_percentage}, StateText: '${parsedStatus.stateText}', MapID: '${parsedStatus.mapId}'")
            println("PARSED STATUS: $parsedStatus")
            parsedStatus
        } catch (e: Exception) {
            println("StatusService: Error in getStatus() API call or parsing: ${e.message}")
            e.printStackTrace()
            val errorStatus = RobotStatus(stateText = "Error fetching/parsing status: ${e.message}")
            println("StatusService: Returning error status object - StateText: '${errorStatus.stateText}'")
            errorStatus
        }
    }

    suspend fun getMap(){
        return client.get(ApiClient.getEndpoint("mission_queue")).body()
    }

    fun statusFlow(intervalSeconds: Long = 10): Flow<RobotStatus> = flow {
        while (true) {
            try {

                emit(getStatus())
            } catch (e: Exception) {
                println("StatusService: Error in statusFlow loop: ${e.message}")
                val flowErrorStatus = RobotStatus(battery_percentage = 50f, stateText = "Errore di Connessione nel Flow")
                println("StatusService: Emitting flow error status object - StateText: '${flowErrorStatus.stateText}'")
                emit(flowErrorStatus)
            }
            delay(intervalSeconds * 1000)
        }
    }
}