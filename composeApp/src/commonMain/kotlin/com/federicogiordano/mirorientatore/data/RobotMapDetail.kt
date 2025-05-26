package com.federicogiordano.mirorientatore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RobotMapDetail(
    @SerialName("guid") val guid: String = "",
    @SerialName("map") val map: String = ""
)