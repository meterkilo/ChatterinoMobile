package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Paint
import kotlinx.coroutines.flow.SharedFlow

interface PaintRepository {

    val paintAssignments: SharedFlow<PaintAssignment>

    fun requestPaintForUser(twitchUserId: String)

    fun findPaintForUser(twitchUserId: String): Paint?

    fun snapshot(): Map<String, Paint>
}

data class PaintAssignment(
    val twitchUserId: String,
    val paint: Paint
)
