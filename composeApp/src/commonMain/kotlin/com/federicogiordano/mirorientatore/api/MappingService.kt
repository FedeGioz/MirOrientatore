package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import com.federicogiordano.mirorientatore.data.RobotMap
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

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
            response.status.isSuccess()
        } catch (e: Exception) {
            false
        }
    }
}

data class Map(
    val id: String,
    val name: String,
    val createdAt: String
)