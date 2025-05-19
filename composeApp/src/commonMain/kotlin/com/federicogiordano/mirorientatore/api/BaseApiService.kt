package com.federicogiordano.mirorientatore.api

import com.federicogiordano.mirage.api.ApiClient
import io.ktor.client.*

interface BaseApiService {
    val client: HttpClient
        get() = ApiClient.httpClient
}