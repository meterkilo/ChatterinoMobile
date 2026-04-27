package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Paint

interface PaintRepository {

    suspend fun loadPaints()

    fun findPaintForUser(twitchUserId: String): Paint?
}
