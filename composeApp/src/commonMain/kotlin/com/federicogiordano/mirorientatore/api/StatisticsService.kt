package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import io.ktor.client.call.*
import io.ktor.client.request.*

data class DistanceStatistic(
    val date: String?,
    val distance: Float?
)

class StatisticsService : BaseApiService() {
    suspend fun getDistanceStats(): List<DistanceStatistic> {
        return client.get(ApiClient.getEndpoint("distance_statistics")).body()
    }
}