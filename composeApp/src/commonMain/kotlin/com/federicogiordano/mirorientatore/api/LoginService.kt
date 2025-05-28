package com.federicogiordano.mirorientatore.api

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.digest.SHA256
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.network.tls.extensions.HashAlgorithm
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class LoginService : BaseApiService() {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun login(username: String, password: String): Boolean {
        if (username.isEmpty() || password.isEmpty()) {
            return false
        }

        try {
            val sha256 = CryptographyProvider.Default.get(HashAlgorithm.SHA256)
            val hashedPasswordBytes = sha256.hasher().hash(password.toByteArray())
            val hashedPassword = hashedPasswordBytes.joinToString("") { "%02x".format(it) }
            val token = Base64.encode("$username:$hashedPassword".toByteArray())

            println("TOKEN CREATO: $token")


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
            println("Login error: ${e.message}")
            return false
        }
    }

    fun logout() {
        _isLoggedIn.value = false
        ApiClient.clearAuthHeader()
    }
}