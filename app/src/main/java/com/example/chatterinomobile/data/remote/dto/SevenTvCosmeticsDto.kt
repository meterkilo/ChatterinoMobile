package com.example.chatterinomobile.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SevenTvGraphQlRequestDto(
    val operationName: String,
    val query: String,
    val variables: SevenTvUserCosmeticsVariablesDto
)

@Serializable
data class SevenTvUserCosmeticsVariablesDto(
    val platform: String,
    val id: String
)

@Serializable
data class SevenTvUserCosmeticsResponseDto(
    val data: SevenTvUserCosmeticsDataDto? = null,
    val errors: List<SevenTvGraphQlErrorDto> = emptyList()
)

@Serializable
data class SevenTvGraphQlErrorDto(
    val message: String = ""
)

@Serializable
data class SevenTvUserCosmeticsDataDto(
    val users: SevenTvUsersDto? = null
)

@Serializable
data class SevenTvUsersDto(
    val userByConnection: SevenTvUserCosmeticsUserDto? = null
)

@Serializable
data class SevenTvUserCosmeticsUserDto(
    val id: String,
    val style: SevenTvUserStyleDto? = null
)

@Serializable
data class SevenTvUserStyleDto(
    val activePaintId: String? = null,
    val activePaint: SevenTvActivePaintDto? = null,
    val activeBadgeId: String? = null,
    val activeBadge: SevenTvActiveBadgeDto? = null
)

@Serializable
data class SevenTvActivePaintDto(
    val id: String,
    val name: String,
    val data: SevenTvPaintDataDto = SevenTvPaintDataDto()
)

@Serializable
data class SevenTvPaintDataDto(
    val layers: List<SevenTvPaintLayerDto> = emptyList(),
    val shadows: List<SevenTvPaintShadowDto> = emptyList()
)

@Serializable
data class SevenTvPaintLayerDto(
    val id: String,
    val opacity: Float = 1f,
    val ty: SevenTvPaintLayerTypeDto
)

@Serializable
data class SevenTvPaintLayerTypeDto(
    @SerialName("__typename") val typename: String,
    val color: SevenTvColorDto? = null,
    val angle: Int = 0,
    val repeating: Boolean = false,
    val stops: List<SevenTvPaintGradientStopDto> = emptyList(),
    val shape: String? = null,
    val images: List<SevenTvPaintImageDto> = emptyList()
)

@Serializable
data class SevenTvPaintGradientStopDto(
    val at: Float,
    val color: SevenTvColorDto
)

@Serializable
data class SevenTvPaintImageDto(
    val url: String,
    val mime: String,
    val size: Int = 0,
    val scale: Int = 1,
    val width: Int = 0,
    val height: Int = 0,
    val frameCount: Int = 1
)

@Serializable
data class SevenTvPaintShadowDto(
    val color: SevenTvColorDto,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blur: Float = 0f
)

@Serializable
data class SevenTvColorDto(
    val hex: String? = null,
    val r: Int = 0,
    val g: Int = 0,
    val b: Int = 0,
    val a: Int = 255
)

@Serializable
data class SevenTvActiveBadgeDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val images: List<SevenTvPaintImageDto> = emptyList()
)
