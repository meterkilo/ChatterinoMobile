package com.example.chatterinomobile.data.model

import kotlinx.serialization.Serializable

@Serializable
sealed class MessageFragment {
    @Serializable
    data class Text(val content: String) : MessageFragment()

    @Serializable
    data class Emote(
        val id: String,
        val name: String,
        val url: String,
        val aspectRatio: Float? = null,
        val isZeroWidth: Boolean = false
    ) : MessageFragment()

    @Serializable
    data class Mention(val username: String) : MessageFragment()
}
