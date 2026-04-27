package com.example.chatterinomobile.data.repository

import com.example.chatterinomobile.data.model.Channel
import com.example.chatterinomobile.data.remote.api.TwitchHelixApi
import com.example.chatterinomobile.data.remote.mapper.toChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val helixApi: TwitchHelixApi
) : ChannelRepository {

    override suspend fun getChannelByLogin(login: String): Channel? =
        getChannelsByLogins(listOf(login)).firstOrNull()

    override suspend fun getChannelsByLogins(logins: List<String>): List<Channel> {
        if (logins.isEmpty()) return emptyList()
        val normalized = logins.map { it.lowercase() }.distinct()

        return withContext(Dispatchers.IO) {
            coroutineScope {
                val usersDeferred = async {
                    runCatching { helixApi.getUsersByLogin(normalized) }.getOrElse { emptyList() }
                }
                val streamsDeferred = async {
                    runCatching { helixApi.getStreamsByLogin(normalized) }.getOrElse { emptyList() }
                }
                val users = usersDeferred.await()
                val streamsByUserId = streamsDeferred.await().associateBy { it.userId }

                users.map { user -> user.toChannel(streamsByUserId[user.id]) }
            }
        }
    }
}
