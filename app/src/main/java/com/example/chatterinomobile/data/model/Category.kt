package com.example.chatterinomobile.data.model

data class Category(
    val id: String,
    val name: String,
    val boxArtUrl: String? = null,
    val viewerCount: Int = 0
)
