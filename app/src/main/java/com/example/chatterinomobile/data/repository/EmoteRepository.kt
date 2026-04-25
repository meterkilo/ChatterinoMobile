package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Emote

interface EmoteRepository {
    /**
     * Populate the cache for this scope. `null` = global. Implementation is
     * stale-while-revalidate: disk is served immediately, network refresh is
     * kicked off in the background when the saved snapshot is past TTL.
     */
    suspend fun loadEmotesForChannel(channelId: String?)

    fun findEmote(name: String, channelId: String? = null): Emote?

    /**
     * Called by the image-decode path the first time a BTTV/FFZ/Twitch emote
     * is drawn on this device. Upgrades the cached [Emote.aspectRatio] so the
     * layout can reserve exact space for every subsequent render — including
     * cold starts after the dimension store is loaded.
     *
     * No-op when dimensions match what's already cached, so the render path
     * can call this unconditionally on every decode without burning IO.
     */
    fun recordDimensions(emoteId: String, channelId: String?, width: Int, height: Int)

    /**
     * Clears in-memory only. `channelId=null` nukes everything; otherwise
     * drops that channel's set. Disk is left alone — use
     * [com.example.chatterinomobile.data.repository.CacheAdmin] for the
     * user-facing wipe that also clears disk.
     */
    fun clearCache(channelId: String? = null)
}
