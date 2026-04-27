package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.local.DiskCacheRoot
import com.example.chatterinomobile.data.local.EmoteDimensionStore
import com.example.chatterinomobile.data.local.MessageHistoryStore

class CacheAdmin(
    private val emoteRepository: EmoteRepository,
    private val badgeRepository: BadgeRepository,
    private val dimensionStore: EmoteDimensionStore,
    private val diskRoot: DiskCacheRoot,
    private val historyStore: MessageHistoryStore
) {

    suspend fun clearAllCaches() {

        emoteRepository.clearCache(null)
        badgeRepository.clearCache(null)

        dimensionStore.clear()

        historyStore.clearAll()

        diskRoot.wipe()
    }
}
