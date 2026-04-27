package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Paint
import com.example.chatterinomobile.data.remote.api.SevenTvCosmeticsApi
import com.example.chatterinomobile.data.remote.mapper.toDomain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class PaintRepositoryImpl(
    private val sevenTvCosmeticsApi: SevenTvCosmeticsApi
) : PaintRepository {

    private val paintByUserId = HashMap<String, Paint>()
    private val writeMutex = Mutex()

    override suspend fun loadPaints() {
        val cosmetics = withContext(Dispatchers.IO) {
            runCatching { sevenTvCosmeticsApi.getCosmetics() }.getOrNull()
        } ?: return

        writeMutex.withLock {
            paintByUserId.clear()
            for (paintDto in cosmetics.paints) {
                val paint = paintDto.toDomain()
                for (userId in paintDto.users) {
                    paintByUserId[userId] = paint
                }
            }
        }
    }

    override fun findPaintForUser(twitchUserId: String): Paint? =
        paintByUserId[twitchUserId]
}
