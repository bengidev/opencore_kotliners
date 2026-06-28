package io.github.bengidev.opencore.chat.presenter

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ChatThreadLayoutPolicyTest {

    @Test
    fun tailScrollIndex_targetsLastDisplayMessage() {
        val messages = listOf("oldest", "middle", "newest")

        assertEquals(-1, ChatThreadLayoutPolicy.tailScrollIndex(emptyList<String>()))
        assertEquals(0, ChatThreadLayoutPolicy.tailScrollIndex(listOf("only")))
        assertEquals(2, ChatThreadLayoutPolicy.tailScrollIndex(messages))
    }

    @Test
    fun tailScrollIndex_usesDisplayOrderLength() {
        val messages = listOf(1, 2, 3, 4, 5)

        val index = ChatThreadLayoutPolicy.tailScrollIndex(ChatThreadLayoutPolicy.displayOrder(messages))

        assertEquals(messages.lastIndex, index)
    }

    @Test
    fun usesBottomBoxAnchor_notReverseLayout() {
        assertFalse(ChatThreadLayoutPolicy.useReverseLayout())
        assertEquals(Alignment.BottomStart, ChatThreadLayoutPolicy.listAlignment)
    }

    @Test
    fun tailScrollOffset_pinsLastRowToViewportBottom() {
        assertEquals(Int.MAX_VALUE, ChatThreadLayoutPolicy.tailScrollOffset())
    }
}
