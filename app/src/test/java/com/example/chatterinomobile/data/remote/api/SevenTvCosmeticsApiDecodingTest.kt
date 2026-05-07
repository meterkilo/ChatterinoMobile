package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.SevenTvUserCosmeticsResponseDto
import com.example.chatterinomobile.data.remote.dto.SevenTvGraphQlRequestDto
import com.example.chatterinomobile.data.remote.dto.SevenTvUserCosmeticsVariablesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SevenTvCosmeticsApiDecodingTest {

    @Test
    fun requestBodyIncludesRequiredPlatformEnum() {
        val body = Json.encodeToString(
            SevenTvGraphQlRequestDto(
                operationName = "UserCosmetics",
                query = "query",
                variables = SevenTvUserCosmeticsVariablesDto(
                    platform = "TWITCH",
                    id = "189660963"
                )
            )
        )

        assertTrue(body.contains("\"platform\":\"TWITCH\""))
        assertTrue(body.contains("\"id\":\"189660963\""))
    }

    @Test
    fun decodesLiveIdiniV4ResponseThroughKtorPipeline() = runBlocking {
        val raw = javaClass.classLoader!!.getResource("idini_v4_cosmetics.json")!!.readText()

        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(raw),
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "application/json")
            )
        }
        val client = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }

        val response: SevenTvUserCosmeticsResponseDto = client.get("https://stub/").body()

        val style = response.data?.users?.userByConnection?.style
        assertNotNull("style should decode", style)
        assertEquals("01H4KSAWP80000TDEXCZCHXRPB", style!!.activePaintId)
        assertNotNull("activePaint should decode", style.activePaint)
        assertEquals("TwitchCon 2023 Paris", style.activePaint!!.name)

        val layer = style.activePaint!!.data.layers.firstOrNull()
        assertNotNull("layer should decode", layer)
        assertEquals("PaintLayerTypeImage", layer!!.ty.typename)
        assertTrue("images list should be non-empty", layer.ty.images.isNotEmpty())
    }
}
