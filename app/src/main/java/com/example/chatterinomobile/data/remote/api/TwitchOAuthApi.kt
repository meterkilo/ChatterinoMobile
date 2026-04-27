package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.TwitchDeviceCodeDto
import com.example.chatterinomobile.data.remote.dto.TwitchOAuthErrorDto
import com.example.chatterinomobile.data.remote.dto.TwitchTokenDto
import com.example.chatterinomobile.data.remote.dto.TwitchValidateTokenDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

class TwitchOAuthApi(
    private val httpClient: HttpClient
) {

    suspend fun startDeviceFlow(clientId: String, scopes: List<String>): TwitchDeviceCodeDto =
        httpClient.post("$BASE_URL/device") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", clientId)
                        append("scopes", scopes.joinToString(" "))
                    }
                )
            )
        }.body()

    suspend fun exchangeDeviceCode(
        clientId: String,
        scopes: List<String>,
        deviceCode: String
    ): DeviceCodeExchangeResult {
        val response = httpClient.post("$BASE_URL/token") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", clientId)
                        append("scopes", scopes.joinToString(" "))
                        append("device_code", deviceCode)
                        append("grant_type", DEVICE_CODE_GRANT_TYPE)
                    }
                )
            )
        }

        return when (response.status) {
            HttpStatusCode.OK -> DeviceCodeExchangeResult.Authorized(response.body())
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> {
                when (normalizeErrorMessage(response.body<TwitchOAuthErrorDto>())) {
                    "authorization_pending" -> DeviceCodeExchangeResult.AuthorizationPending
                    "slow_down" -> DeviceCodeExchangeResult.SlowDown
                    "expired_token" -> DeviceCodeExchangeResult.ExpiredToken
                    "access_denied" -> DeviceCodeExchangeResult.AccessDenied
                    "invalid device code" -> DeviceCodeExchangeResult.ExpiredToken
                    else -> DeviceCodeExchangeResult.Failed(
                        "Device code exchange failed (${response.status.value})"
                    )
                }
            }
            else -> DeviceCodeExchangeResult.Failed(
                "Device code exchange failed (${response.status.value})"
            )
        }
    }

    suspend fun refreshAccessToken(
        clientId: String,
        refreshToken: String
    ): RefreshTokenResult {
        val response = httpClient.post("$BASE_URL/token") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("client_id", clientId)
                        append("refresh_token", refreshToken)
                        append("grant_type", "refresh_token")
                    }
                )
            )
        }

        return when (response.status) {
            HttpStatusCode.OK -> RefreshTokenResult.Success(response.body())
            HttpStatusCode.BadRequest,
            HttpStatusCode.Unauthorized -> {
                val message = normalizeErrorMessage(response.body<TwitchOAuthErrorDto>())
                if (message == "invalid refresh token") {
                    RefreshTokenResult.InvalidRefreshToken
                } else {
                    RefreshTokenResult.Failed("Refresh failed (${response.status.value})")
                }
            }
            else -> RefreshTokenResult.Failed("Refresh failed (${response.status.value})")
        }
    }

    suspend fun validateAccessToken(accessToken: String): ValidateTokenResult {
        val response = httpClient.get("$BASE_URL/validate") {
            header(HttpHeaders.Authorization, "OAuth $accessToken")
        }

        return when (response.status) {
            HttpStatusCode.OK -> ValidateTokenResult.Valid(response.body())
            HttpStatusCode.Unauthorized -> ValidateTokenResult.Invalid
            else -> ValidateTokenResult.Failed("Validate failed (${response.status.value})")
        }
    }

    private fun normalizeErrorMessage(error: TwitchOAuthErrorDto): String =
        (error.message ?: error.error).orEmpty().trim().lowercase()

    sealed interface DeviceCodeExchangeResult {
        data class Authorized(val token: TwitchTokenDto) : DeviceCodeExchangeResult
        data object AuthorizationPending : DeviceCodeExchangeResult
        data object SlowDown : DeviceCodeExchangeResult
        data object ExpiredToken : DeviceCodeExchangeResult
        data object AccessDenied : DeviceCodeExchangeResult
        data class Failed(val message: String) : DeviceCodeExchangeResult
    }

    sealed interface RefreshTokenResult {
        data class Success(val token: TwitchTokenDto) : RefreshTokenResult
        data object InvalidRefreshToken : RefreshTokenResult
        data class Failed(val message: String) : RefreshTokenResult
    }

    sealed interface ValidateTokenResult {
        data class Valid(val token: TwitchValidateTokenDto) : ValidateTokenResult
        data object Invalid : ValidateTokenResult
        data class Failed(val message: String) : ValidateTokenResult
    }

    companion object {
        private const val BASE_URL = "https://id.twitch.tv/oauth2"
        private const val DEVICE_CODE_GRANT_TYPE =
            "urn:ietf:params:oauth:grant-type:device_code"
    }
}
