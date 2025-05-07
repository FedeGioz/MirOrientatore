package com.federicogiordano.mirorientatore

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform