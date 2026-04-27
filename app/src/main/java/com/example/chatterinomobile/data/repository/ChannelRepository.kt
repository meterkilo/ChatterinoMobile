package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Channel

interface ChannelRepository {

    suspend fun getChannelByLogin(login: String): Channel?

    suspend fun getChannelsByLogins(logins: List<String>): List<Channel>
}
