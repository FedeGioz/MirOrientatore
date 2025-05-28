package com.federicogiordano.mirorientatore.api

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object ApiClient {
    private const val API_URL = "http://192.168.12.20/api/v2.0.0"
    private var authHeader: String? = null

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }

        engine {
            requestTimeout = 60_000
            endpoint {
                connectTimeout = 60_000
                socketTimeout = 60_000
            }
        }

        defaultRequest {
            authHeader?.let {
                header(HttpHeaders.Authorization, it)
            }
            contentType(ContentType.Application.Json)
        }
    }

    fun setAuthHeader(header: String) {
        authHeader = header
    }

    fun clearAuthHeader() {
        authHeader = null
    }

    fun getEndpoint(path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return "$API_URL/$cleanPath"
    }
}