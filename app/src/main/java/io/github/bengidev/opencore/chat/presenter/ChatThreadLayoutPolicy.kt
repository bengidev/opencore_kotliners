package io.github.bengidev.opencore.chat.presenter

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

/**
 * Bottom-anchored chat thread layout, mirroring iOS `defaultScrollAnchor(.bottom)`.
 *
 * The message list keeps chronological order and is aligned to the bottom of the
 * thread viewport so short conversations sit just above the composer.
 */
internal object ChatThreadLayoutPolicy {
    fun <T> displayOrder(messages: List<T>): List<T> = messages

    fun <T> tailScrollIndex(messages: List<T>): Int {
        val ordered = displayOrder(messages)
        if (ordered.isEmpty()) return -1
        return ordered.lastIndex
    }

    fun useReverseLayout(): Boolean = false

    fun tailScrollOffset(): Int = Int.MAX_VALUE

    val listAlignment: Alignment = Alignment.BottomStart

    fun contentPadding(): PaddingValues = PaddingValues(top = 8.dp, bottom = 16.dp)
}
