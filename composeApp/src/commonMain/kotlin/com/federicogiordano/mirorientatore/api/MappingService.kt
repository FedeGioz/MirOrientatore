package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirorientatore.data.Quiz
import com.federicogiordano.mirorientatore.data.RobotMap
import com.federicogiordano.mirorientatore.data.RobotMapDetail
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

class MappingService : BaseApiService() {
    suspend fun getMaps(): List<RobotMap> {
        return client.get(ApiClient.getEndpoint("maps")).body()
    }

    suspend fun updateMap(mapId: String): Boolean {
        return try {
            val response = client.put(ApiClient.getEndpoint("status")) {
                contentType(ContentType.Application.Json)
                setBody(mapOf("map_id" to mapId))
            }
            val mapImage = getMapImage(mapId)
            val message = WebSocketMessage(
                type = "MAP_IMAGE",
                content = mapImage,
                sender = "professor"
            )
            WebSocketServer().broadcastMessage(message)
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getMapImage(mapId: String): String{
        val response = client.get(ApiClient.getEndpoint("maps") + "/${mapId}").bodyAsText()
        val json = Json { ignoreUnknownKeys = true }
        val mapObject = json.decodeFromString<RobotMapDetail>(response)
        println("STRINGA MAPPA: ${mapObject.map}")
        return mapObject.map
    }
}

data class Map(
    val id: String,
    val name: String,
    val createdAt: String
)