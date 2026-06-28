package io.github.bengidev.opencore.chat.presenter

import androidx.compose.ui.Alignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatThreadLayoutPolicyTest {

    @Test
    fun displayOrder_preservesChronologicalOrder() {
        val messages = listOf("oldest", "middle", "newest")

        val display = ChatThreadLayoutPolicy.displayOrder(messages)

        assertEquals(messages, display)
    }

    @Test
    fun tailScrollIndex_targetsLastMessage() {
        assertEquals(-1, ChatThreadLayoutPolicy.tailScrollIndex(messageCount = 0))
        assertEquals(0, ChatThreadLayoutPolicy.tailScrollIndex(messageCount = 1))
        assertEquals(11, ChatThreadLayoutPolicy.tailScrollIndex(messageCount = 12))
    }

    @Test
    fun usesBottomBoxAnchor_notReverseLayout() {
        assertFalse(ChatThreadLayoutPolicy.useReverseLayout())
        assertEquals(Alignment.BottomStart, ChatThreadLayoutPolicy.listAlignment)
    }

    @Test
    fun tailScrollOffset_pinsLastRowToViewportBottom() {
        assertTrue(ChatThreadLayoutPolicy.tailScrollOffset() > 0)
    }
}
