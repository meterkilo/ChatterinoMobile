package com.example.chatterinomobile.data.remote.irc

object IrcParser {

    fun parse(line: String): IrcMessage? {
        if (line.isBlank()) return null

        var cursor = 0
        val length = line.length

        val tags: Map<String, String> = if (cursor < length && line[cursor] == '@') {
            cursor++
            val spaceIdx = line.indexOf(' ', cursor)
            if (spaceIdx < 0) return null
            val raw = line.substring(cursor, spaceIdx)
            cursor = spaceIdx + 1
            parseTags(raw)
        } else {
            emptyMap()
        }

        val prefix: String? = if (cursor < length && line[cursor] == ':') {
            cursor++
            val spaceIdx = line.indexOf(' ', cursor)
            if (spaceIdx < 0) return null
            val p = line.substring(cursor, spaceIdx)
            cursor = spaceIdx + 1
            p
        } else {
            null
        }

        val commandEnd = line.indexOf(' ', cursor).let { if (it < 0) length else it }
        val command = line.substring(cursor, commandEnd).uppercase()
        cursor = if (commandEnd < length) commandEnd + 1 else length
        if (command.isEmpty()) return null

        val params = mutableListOf<String>()
        while (cursor < length) {
            if (line[cursor] == ':') {

                params.add(line.substring(cursor + 1))
                break
            }
            val nextSpace = line.indexOf(' ', cursor)
            if (nextSpace < 0) {
                params.add(line.substring(cursor))
                break
            }
            params.add(line.substring(cursor, nextSpace))
            cursor = nextSpace + 1
        }

        return IrcMessage(tags, prefix, command, params)
    }

    private fun parseTags(raw: String): Map<String, String> {
        if (raw.isEmpty()) return emptyMap()
        val out = HashMap<String, String>()
        for (pair in raw.split(';')) {
            if (pair.isEmpty()) continue
            val eq = pair.indexOf('=')
            if (eq < 0) {
                out[pair] = ""
            } else {
                val key = pair.substring(0, eq)
                val value = unescapeTagValue(pair.substring(eq + 1))
                out[key] = value
            }
        }
        return out
    }

    private fun unescapeTagValue(value: String): String {
        if (value.indexOf('\\') < 0) return value
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    ':' -> sb.append(';')
                    's' -> sb.append(' ')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    '\\' -> sb.append('\\')
                    else -> sb.append(value[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
