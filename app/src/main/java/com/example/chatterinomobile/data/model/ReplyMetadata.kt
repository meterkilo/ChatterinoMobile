package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ReplyMetadata(
    val parentMessageId: String,
    val parentUserId: String?,
    val parentUserLogin: String?,
    val parentDisplayName: String?,
    val parentBody: String?
)
