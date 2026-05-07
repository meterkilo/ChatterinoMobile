package com.example.chatterinomobile.data.remote.mapper

import com.example.chatterinomobile.data.model.GradientFunction
import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.remote.dto.SevenTvActivePaintDto
import com.example.chatterinomobile.data.remote.dto.SevenTvColorDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintDataDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintGradientStopDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintImageDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintLayerDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintLayerTypeDto
import com.example.chatterinomobile.data.remote.dto.SevenTvPaintShadowDto
import com.example.chatterinomobile.data.remote.dto.SevenTvUserCosmeticsResponseDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

private val TestJson = Json { ignoreUnknownKeys = true }

class SevenTvCosmeticsMapperTest {

    @Test
    fun mapsLinearGradientLayer() {
        val paint = paintWith(
            layer = SevenTvPaintLayerTypeDto(
                typename = "PaintLayerTypeLinearGradient",
                angle = 45,
                repeating = true,
                stops = listOf(
                    SevenTvPaintGradientStopDto(at = 0f, color = SevenTvColorDto(r = 0xAA, g = 0xBB, b = 0xCC, a = 0xDD)),
                    SevenTvPaintGradientStopDto(at = 1f, color = SevenTvColorDto(r = 0x11, g = 0x22, b = 0x33, a = 0x44))
                )
            )
        ).toDomain()

        val gradient = paint as Paint.Gradient
        assertEquals(GradientFunction.LINEAR, gradient.function)
        assertEquals(45, gradient.angle)
        assertEquals(true, gradient.repeating)
        assertEquals(0xDDAABBCC, gradient.stops.first().color)
    }

    @Test
    fun mapsSingleColorLayer() {
        val paint = paintWith(
            layer = SevenTvPaintLayerTypeDto(
                typename = "PaintLayerTypeSingleColor",
                color = SevenTvColorDto(r = 0xFF, g = 0x00, b = 0x80, a = 0xFF)
            )
        ).toDomain()

        val solid = paint as Paint.Solid
        assertEquals(0xFFFF0080, solid.color)
    }

    @Test
    fun mapsImageLayerPicksAnimatedHighScaleWebp() {
        val staticAvif1x = SevenTvPaintImageDto(
            url = "static-1x.avif", mime = "image/avif", scale = 1, frameCount = 1, width = 130, height = 32
        )
        val staticWebp4x = SevenTvPaintImageDto(
            url = "static-4x.webp", mime = "image/webp", scale = 4, frameCount = 1, width = 520, height = 128
        )
        val animatedWebp1x = SevenTvPaintImageDto(
            url = "anim-1x.webp", mime = "image/webp", scale = 1, frameCount = 125, width = 96, height = 32
        )
        val animatedWebp4x = SevenTvPaintImageDto(
            url = "anim-4x.webp", mime = "image/webp", scale = 4, frameCount = 125, width = 384, height = 128
        )
        val animatedAvif4x = SevenTvPaintImageDto(
            url = "anim-4x.avif", mime = "image/avif", scale = 4, frameCount = 125, width = 384, height = 128
        )
        val animatedGif4x = SevenTvPaintImageDto(
            url = "anim-4x.gif", mime = "image/gif", scale = 4, frameCount = 125, width = 384, height = 128
        )

        val paint = paintWith(
            layer = SevenTvPaintLayerTypeDto(
                typename = "PaintLayerTypeImage",
                images = listOf(staticAvif1x, staticWebp4x, animatedWebp1x, animatedAvif4x, animatedGif4x, animatedWebp4x)
            )
        ).toDomain() as Paint.Image

        assertEquals("anim-4x.webp", paint.url)
        assertTrue(paint.animated)
        assertEquals(384f / 128f, paint.aspectRatio, 0.001f)
    }

    @Test
    fun mapsImageLayerFallsBackToStaticWhenNoAnimated() {
        val staticWebp4x = SevenTvPaintImageDto(
            url = "static-4x.webp", mime = "image/webp", scale = 4, frameCount = 1, width = 520, height = 128
        )
        val paint = paintWith(
            layer = SevenTvPaintLayerTypeDto(
                typename = "PaintLayerTypeImage",
                images = listOf(staticWebp4x)
            )
        ).toDomain() as Paint.Image

        assertEquals("static-4x.webp", paint.url)
        assertEquals(false, paint.animated)
    }

    @Test
    fun mapsShadowsViaTopLevelData() {
        val paint = SevenTvActivePaintDto(
            id = "p",
            name = "P",
            data = SevenTvPaintDataDto(
                layers = listOf(
                    SevenTvPaintLayerDto(
                        id = "l",
                        ty = SevenTvPaintLayerTypeDto(typename = "PaintLayerTypeSingleColor", color = SevenTvColorDto(a = 0xFF))
                    )
                ),
                shadows = listOf(
                    SevenTvPaintShadowDto(
                        color = SevenTvColorDto(r = 0x11, g = 0x22, b = 0x33, a = 0xFF),
                        offsetX = 1f, offsetY = 2f, blur = 3f
                    )
                )
            )
        ).toDomain() as Paint

        val shadow = paint.shadows.single()
        assertEquals(1f, shadow.xOffset, 0f)
        assertEquals(2f, shadow.yOffset, 0f)
        assertEquals(3f, shadow.radius, 0f)
        assertEquals(0xFF112233, shadow.color)
    }

    @Test
    fun decodesLiveV4UserCosmeticsResponse() {
        val response = TestJson.decodeFromString<SevenTvUserCosmeticsResponseDto>(
            """
            {
              "data": {
                "users": {
                  "userByConnection": {
                    "id": "01FFRYC79R0007P57XYW0BJDJQ",
                    "style": {
                      "activePaintId": "01H4KSAWP80000TDEXCZCHXRPB",
                      "activePaint": {
                        "id": "01H4KSAWP80000TDEXCZCHXRPB",
                        "name": "TwitchCon 2023 Paris",
                        "data": {
                          "layers": [
                            {
                              "id": "01JAMR28DRMJ1C6EHWTDH8T5X3",
                              "opacity": 1.0,
                              "ty": {
                                "__typename": "PaintLayerTypeImage",
                                "images": [
                                  {"url": "https://cdn.7tv.app/x/4x.webp", "mime": "image/webp", "scale": 4, "width": 520, "height": 128, "frameCount": 1}
                                ]
                              }
                            }
                          ],
                          "shadows": []
                        }
                      },
                      "activeBadgeId": null,
                      "activeBadge": null
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val paintDto = response.data?.users?.userByConnection?.style?.activePaint
        assertNotNull(paintDto)
        val paint = paintDto!!.toDomain() as Paint.Image
        assertEquals("https://cdn.7tv.app/x/4x.webp", paint.url)
    }

    private fun paintWith(layer: SevenTvPaintLayerTypeDto): SevenTvActivePaintDto = SevenTvActivePaintDto(
        id = "paint-id",
        name = "Paint",
        data = SevenTvPaintDataDto(
            layers = listOf(SevenTvPaintLayerDto(id = "layer-id", ty = layer))
        )
    )
}
