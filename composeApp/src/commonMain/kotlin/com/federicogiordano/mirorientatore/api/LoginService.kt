package com.federicogiordano.mirorientatore.api

import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.kotlincrypto.hash.sha2.SHA256
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LoginService : BaseApiService() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    @OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
    suspend fun login(username: String, password: String): Boolean {
        if (username.isEmpty() || password.isEmpty()) {
            return false
        }
        try {
            val digest = SHA256()
            digest.update(password.toByteArray())
            val hashedPassword = digest.digest().toHexString()
            val token = Base64.encode("$username:$hashedPassword".toByteArray())


            val response = client.post(ApiClient.getEndpoint("users/auth")) {
                header(HttpHeaders.Authorization, "Basic $token")
            }

            val success = response.status == HttpStatusCode.Accepted

            if (success) {
                _isLoggedIn.value = true
                ApiClient.setAuthHeader("Basic $token")
            }

            return success
        } catch (e: Exception) {
            return false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        ApiClient.clearAuthHeader()
    }
}