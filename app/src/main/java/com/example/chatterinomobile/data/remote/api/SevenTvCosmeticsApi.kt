package com.example.chatterinomobile.data.remote.api

import com.example.chatterinomobile.data.remote.dto.SevenTvGraphQlRequestDto
import com.example.chatterinomobile.data.remote.dto.SevenTvUserCosmeticsResponseDto
import com.example.chatterinomobile.data.remote.dto.SevenTvUserCosmeticsVariablesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class SevenTvCosmeticsApi(private val httpClient: HttpClient) {

    suspend fun getUserCosmetics(twitchUserId: String): SevenTvUserCosmeticsResponseDto {
        return httpClient.post(GQL_URL) {
            contentType(ContentType.Application.Json)
            setBody(
                SevenTvGraphQlRequestDto(
                    operationName = "UserCosmetics",
                    query = USER_COSMETICS_QUERY,
                    variables = SevenTvUserCosmeticsVariablesDto(
                        platform = "TWITCH",
                        id = twitchUserId
                    )
                )
            )
        }.body()
    }

    companion object {
        private const val GQL_URL = "https://7tv.io/v4/gql"
        private const val USER_COSMETICS_QUERY = """
            query UserCosmetics(${'$'}platform: Platform!, ${'$'}id: String!) {
              users {
                userByConnection(platform: ${'$'}platform, platformId: ${'$'}id) {
                  id
                  style {
                    activePaintId
                    activePaint {
                      id
                      name
                      data {
                        layers {
                          id
                          opacity
                          ty {
                            __typename
                            ... on PaintLayerTypeSingleColor {
                              color { hex r g b a }
                            }
                            ... on PaintLayerTypeLinearGradient {
                              angle
                              repeating
                              stops { at color { hex r g b a } }
                            }
                            ... on PaintLayerTypeRadialGradient {
                              repeating
                              shape
                              stops { at color { hex r g b a } }
                            }
                            ... on PaintLayerTypeImage {
                              images {
                                url mime size scale width height frameCount
                              }
                            }
                          }
                        }
                        shadows {
                          color { hex r g b a }
                          offsetX
                          offsetY
                          blur
                        }
                      }
                    }
                    activeBadgeId
                    activeBadge {
                      id
                      name
                      description
                      tags
                      images {
                        url mime size scale width height frameCount
                      }
                    }
                  }
                }
              }
            }
        """
    }
}
