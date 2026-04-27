package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ChatUser(
    val id : String,
    val login: String,
    val displayName: String,
    val color: String? = null,
    val paint: Paint? = null
)
