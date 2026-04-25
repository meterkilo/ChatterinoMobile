package com.example.chatterinomobile.di

import com.example.chatterinomobile.BuildConfig
import com.example.chatterinomobile.data.local.BadgeDiskCache
import com.example.chatterinomobile.data.local.DiskCacheRoot
import com.example.chatterinomobile.data.local.EmoteDimensionStore
import com.example.chatterinomobile.data.local.EmoteDiskCache
import com.example.chatterinomobile.data.local.MessageHistoryStore
import com.example.chatterinomobile.data.local.TokenStore
import com.example.chatterinomobile.data.repository.AnonymousAuthRepository
import com.example.chatterinomobile.data.repository.AuthRepository
import com.example.chatterinomobile.data.repository.BadgeRepository
import com.example.chatterinomobile.data.repository.BadgeRepositoryImpl
import com.example.chatterinomobile.data.repository.CacheAdmin
import com.example.chatterinomobile.data.repository.ChannelRepository
import com.example.chatterinomobile.data.repository.ChannelRepositoryImpl
import com.example.chatterinomobile.data.repository.ChatRepository
import com.example.chatterinomobile.data.repository.ChatRepositoryImpl
import com.example.chatterinomobile.data.repository.EmoteRepository
import com.example.chatterinomobile.data.repository.EmoteRepositoryImpl
import com.example.chatterinomobile.data.repository.PaintRepository
import com.example.chatterinomobile.data.repository.PaintRepositoryImpl
import com.example.chatterinomobile.data.repository.TwitchOAuthRepository
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single { TokenStore(get()) }

    // Disk cache plumbing. Single DiskCacheRoot so the settings-level
    // "Clear cache" has one place to wipe. Everything here lives under
    // cacheDir so Android's own "Clear cache" in system settings works too.
    single { DiskCacheRoot(get()) }
    single { EmoteDiskCache(get()) }
    single { BadgeDiskCache(get()) }
    single { EmoteDimensionStore(get()) }

    // Persistent chat scrollback (SQLDelight). Singleton: the underlying
    // SqlDriver opens the DB once and all reads/writes funnel through it.
    single { MessageHistoryStore(get()) }

    // OAuth is optional for local/CI builds. When the client ID is missing we
    // intentionally keep the app in anonymous read-only mode instead of
    // crashing at startup or hardcoding someone else's client ID.
    single {
        if (BuildConfig.TWITCH_CLIENT_ID.isBlank()) {
            AnonymousAuthRepository(BuildConfig.TWITCH_CLIENT_ID)
        } else {
            TwitchOAuthRepository(get(), get())
        }
    } bind AuthRepository::class

    // Emotes / cosmetics / channels
    single {
        EmoteRepositoryImpl(
            sevenTvApi = get(),
            bttvApi = get(),
            ffzApi = get(),
            diskCache = get(),
            dimensionStore = get()
        )
    } bind EmoteRepository::class
    single {
        BadgeRepositoryImpl(
            helixApi = get(),
            sevenTvCosmeticsApi = get(),
            diskCache = get()
        )
    } bind BadgeRepository::class
    single { PaintRepositoryImpl(get()) } bind PaintRepository::class
    single { ChannelRepositoryImpl(get()) } bind ChannelRepository::class

    // CacheAdmin owns the user-facing "Clear cache" action. ViewModels bind
    // here rather than touching each repository directly so nothing can be
    // forgotten when a new cache layer is added.
    single { CacheAdmin(get(), get(), get(), get(), get()) }

    // Chat — depends on TwitchIrcClient + IrcMessageMapper + ModerationEventMapper
    // + MessageEnricher from networkModule. The enricher pulls Emote/Paint
    // repositories from this module, so load order matters only in that
    // repositoryModule must be listed alongside (Koin resolves lazily).
    single {
        ChatRepositoryImpl(
            ircClient = get(),
            mapper = get(),
            moderationMapper = get(),
            roomStateMapper = get(),
            userStateMapper = get(),
            enricher = get(),
            channelRepository = get(),
            badgeRepository = get(),
            emoteRepository = get(),
            historyStore = get()
        )
    } bind ChatRepository::class
}
