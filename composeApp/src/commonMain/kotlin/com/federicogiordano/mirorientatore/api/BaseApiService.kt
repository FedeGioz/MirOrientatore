package com.federicogiordano.mirorientatore.api

import io.ktor.client.*

abstract class BaseApiService {
    companion object {
        private val sharedClient: HttpClient by lazy { ApiClient.httpClient }

        val client: HttpClient get() = sharedClient
    }
}