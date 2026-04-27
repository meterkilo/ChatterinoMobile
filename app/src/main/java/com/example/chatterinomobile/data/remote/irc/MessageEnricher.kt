package com.example.chatterinomobile.data.remote.irc

import com.example.chatterinomobile.data.model.ChatMessage
import com.example.chatterinomobile.data.model.MessageFragment
import com.example.chatterinomobile.data.repository.EmoteRepository
import com.example.chatterinomobile.data.repository.PaintRepository

class MessageEnricher(
    private val emoteRepository: EmoteRepository,
    private val paintRepository: PaintRepository
) {

    fun enrich(message: ChatMessage): ChatMessage {
        val paint = paintRepository.findPaintForUser(message.author.id)
        val swapped = swapThirdPartyEmotes(
            fragments = message.fragment,
            channelId = message.channelId
        )

        if (paint == null && swapped === message.fragment) return message

        val author = if (paint != null) message.author.copy(paint = paint) else message.author
        return message.copy(
            author = author,
            fragment = swapped
        )
    }

    private fun swapThirdPartyEmotes(
        fragments: List<MessageFragment>,
        channelId: String
    ): List<MessageFragment> {

        if (fragments.none { it is MessageFragment.Text && it.content.containsEmoteCandidate() }) {
            return fragments
        }

        val out = ArrayList<MessageFragment>(fragments.size)
        for (fragment in fragments) {
            if (fragment !is MessageFragment.Text) {
                out.add(fragment)
                continue
            }
            expandTextFragment(fragment.content, channelId, out)
        }
        return out
    }

    private fun expandTextFragment(
        text: String,
        channelId: String,
        out: MutableList<MessageFragment>
    ) {
        if (text.isEmpty()) return
        val buffer = StringBuilder()
        var i = 0
        val n = text.length
        while (i < n) {

            val wsStart = i
            while (i < n && text[i].isWhitespace()) i++
            if (i > wsStart) buffer.append(text, wsStart, i)
            if (i >= n) break

            val wordStart = i
            while (i < n && !text[i].isWhitespace()) i++
            val word = text.substring(wordStart, i)

            val emote = emoteRepository.findEmote(word, channelId)
            if (emote != null) {
                if (buffer.isNotEmpty()) {
                    out.add(MessageFragment.Text(buffer.toString()))
                    buffer.clear()
                }
                out.add(
                    MessageFragment.Emote(
                        id = emote.id,
                        name = emote.name,
                        url = emote.urls.x3
                    )
                )
            } else {
                buffer.append(word)
            }
        }
        if (buffer.isNotEmpty()) {
            out.add(MessageFragment.Text(buffer.toString()))
        }
    }

    private fun String.containsEmoteCandidate(): Boolean {
        for (c in this) if (!c.isWhitespace()) return true
        return false
    }
}
