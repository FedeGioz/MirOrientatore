package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import com.federicogiordano.mirorientatore.data.RobotSound
import io.ktor.client.call.body
import io.ktor.client.request.get

class SoundsService : BaseApiService() {
    suspend fun getSounds(): List<RobotSound> {
        return client.get(ApiClient.getEndpoint("sounds")).body()
    }
}