package com.federicogiordano.mirorientatore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RobotMap(
    @SerialName("name") val name: String = "",
    @SerialName("guid") val guid: String = ""
)