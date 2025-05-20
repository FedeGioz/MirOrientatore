package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import com.federicogiordano.mirorientatore.data.RobotMission
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

class MissionService : BaseApiService() {
    suspend fun getMissions(): List<RobotMission> {
        return client.get(ApiClient.getEndpoint("missions")).body()
    }

//    suspend fun createMission(mission: RobotMission) {
//        client.post(ApiClient.getEndpoint("missions")) {
//            contentType(ContentType.Application.Json)
//            setBody(mission)
//        }
//    }

    suspend fun addMissionToQueue(mission: RobotMission) {
        client.post(ApiClient.getEndpoint("mission_queue")) {
            contentType(ContentType.Application.Json)
            setBody(mapOf("mission_id" to mission.guid))
        }
    }

    suspend fun getMissionQueue() : List<RobotMission>{
        return client.get(ApiClient.getEndpoint("mission_queue")).body()
    }

    suspend fun removeMissionFromQueue(mission: RobotMission){
        client.delete(ApiClient.getEndpoint("mission_queue") + "/${mission.guid}")
    }

    suspend fun reorderMissionQueue(mission: RobotMission, direction: Boolean) {
        val response = client.get(ApiClient.getEndpoint("mission_queue") + "/${mission.guid}")

        val missionData = response.body<JsonObject>()

        var currentPriority = missionData["priority"]?.jsonPrimitive?.int ?: 10

        client.put(ApiClient.getEndpoint("mission_queue") + "/${mission.guid}") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "mission_id" to mission.guid,
                "priority" to if (direction) ++currentPriority else --currentPriority
            ))
        }
    }
}