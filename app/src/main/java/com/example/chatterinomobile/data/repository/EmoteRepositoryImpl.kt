package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Emote
import com.example.chatterinomobile.data.remote.api.BttvApi
import com.example.chatterinomobile.data.remote.api.FfzApi
import com.example.chatterinomobile.data.remote.api.SevenTvApi
import com.example.chatterinomobile.data.remote.mapper.toDomain
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Loads emotes from 7TV, BTTV, and FFZ and keeps them in an in-memory lookup
 * table keyed by emote name. A single [Mutex] guards writes so concurrent
 * channel switches don't race.
 *
 * The current APIs only expose global emote sets, so [loadEmotesForChannel]
 * ignores [channelId] for now. Channel-scoped endpoints can be layered on
 * later without changing the interface.
 */
class EmoteRepositoryImpl(
    private val sevenTvApi: SevenTvApi,
    private val bttvApi: BttvApi,
    private val ffzApi: FfzApi
) : EmoteRepository {

    private val emotesByName = HashMap<String, Emote>()
    private val mutex = Mutex()

    override suspend fun loadEmotesForChannel(channelId: String?) {
        val loaded: List<Emote> = withContext(Dispatchers.IO) {
            coroutineScope {
                val sevenTv: Deferred<List<Emote>> = async {
                    runCatching { sevenTvApi.getGlobalEmoteSet().emotes.map { it.toDomain() } }
                        .getOrElse { emptyList() }
                }
                val bttv: Deferred<List<Emote>> = async {
                    runCatching { bttvApi.getGlobalEmotes().map { it.toDomain() } }
                        .getOrElse { emptyList() }
                }
                val ffz: Deferred<List<Emote>> = async {
                    runCatching { ffzApi.getGlobalEmotes().map { it.toDomain() } }
                        .getOrElse { emptyList() }
                }
                sevenTv.await() + bttv.await() + ffz.await()
            }
        }

        mutex.withLock {
            emotesByName.clear()
            // Later providers (BTTV, FFZ) override earlier ones (7TV) on name
            // collision, matching Chatterino desktop's precedence.
            for (emote in loaded) {
                emotesByName[emote.name] = emote
            }
        }
    }

    override fun findEmote(name: String): Emote? = emotesByName[name]
}
