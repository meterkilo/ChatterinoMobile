package com.example.chatterinomobile.data.remote.irc

data class IrcMessage(
    val tags: Map<String, String>,
    val prefix: String?,
    val command: String,
    val params: List<String>
) {

    val nick: String?
        get() {
            val p = prefix ?: return null
            val bang = p.indexOf('!')
            return if (bang > 0) p.substring(0, bang) else null
        }

    val channel: String?
        get() = params.firstOrNull()?.takeIf { it.startsWith("#") }?.removePrefix("#")

    val trailing: String?
        get() = params.lastOrNull()
}
