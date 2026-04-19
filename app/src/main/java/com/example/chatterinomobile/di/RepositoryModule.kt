package com.example.chatterinomobile.di

import com.example.chatterinomobile.data.repository.EmoteRepository
import com.example.chatterinomobile.data.repository.EmoteRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

val repositoryModule = module {
    single { EmoteRepositoryImpl(get(), get(), get()) } bind EmoteRepository::class
}