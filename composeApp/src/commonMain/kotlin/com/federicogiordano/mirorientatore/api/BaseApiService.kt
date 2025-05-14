package com.federicogiordano.mirorientatore.api

import io.ktor.client.*

interface BaseApiService {
    val client: HttpClient
        get() = ApiClient.httpClient
}