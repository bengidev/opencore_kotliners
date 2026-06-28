package io.github.bengidev.opencore.chat.presenter

import androidx.compose.ui.Alignment

/**
 * Bottom-anchored chat thread layout, mirroring iOS `defaultScrollAnchor(.bottom)`.
 *
 * The message list keeps chronological order and is aligned to the bottom of the
 * thread viewport so short conversations sit just above the composer.
 */
internal object ChatThreadLayoutPolicy {
    fun <T> displayOrder(messages: List<T>): List<T> = messages

    fun tailScrollIndex(messageCount: Int): Int {
        if (messageCount <= 0) return -1
        return messageCount - 1
    }

    fun useReverseLayout(): Boolean = false

    fun tailScrollOffset(): Int = Int.MAX_VALUE

    val listAlignment: Alignment = Alignment.BottomStart
}
