package com.example.chatterinomobile

import android.app.Application
import com.example.chatterinomobile.di.networkModule
import com.example.chatterinomobile.di.repositoryModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class ChatterinoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(Level.INFO)
            androidContext(this@ChatterinoApp)
            modules(
                networkModule,
                repositoryModule
            )
        }
    }
}