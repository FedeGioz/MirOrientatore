package com.federicogiordano.mirorientatore.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RobotSound(
    @SerialName("name") val name: String = "",
    @SerialName("guid") val guid: String = "",
    @SerialName("lenght") val lenght: Int = 0,
    @SerialName("url") val url: String = ""
)