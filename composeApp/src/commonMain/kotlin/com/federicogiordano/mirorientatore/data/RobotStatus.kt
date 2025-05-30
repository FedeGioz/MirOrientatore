package com.federicogiordano.mirorientatore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RobotStatus(
    @SerialName("mode_id") val modeId: Int = -1,
    @SerialName("mission_queue_id") val missionQueueId: Int? = null,
    @SerialName("robot_name") val robotName: String = "",
    @SerialName("uptime") val uptime: Long = -1,
    @SerialName("errors") val errors: List<String> = emptyList(),
    @SerialName("battery_percentage") val battery_percentage: Float = 0f,
    @SerialName("map_id") val mapId: String = "",
    @SerialName("mission_text") val missionText: String = "",
    @SerialName("state_id") val stateId: Int = -1,
    @SerialName("state_text") val stateText: String = "",
    @SerialName("velocity") val velocity: Velocity = Velocity(),
    @SerialName("robot_model") val robotModel: String = "",
    @SerialName("mode_text") val modeText: String = "",
    @SerialName("battery_time_remaining") val batteryTimeRemaining: Long = -1,
    @SerialName("position") val position: Position = Position()
)

@Serializable
data class Velocity(
    @SerialName("linear") val linear: Float? = null,
    @SerialName("angular") val angular: Float? = null
)

@Serializable
data class Position(
    @SerialName("x") val x: Float? = null,
    @SerialName("y") val y: Float? = null,
    @SerialName("orientation") val orientation: Float? = null
)