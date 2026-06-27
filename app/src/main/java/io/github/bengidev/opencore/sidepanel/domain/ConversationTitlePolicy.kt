package io.github.bengidev.opencore.sidepanel.domain

/** Derives the display title for a conversation from the latest user message. */
internal object ConversationTitlePolicy {
    const val MAX_LENGTH = 80

    fun fromUserMessage(text: String): String = text.trim().take(MAX_LENGTH)
}
