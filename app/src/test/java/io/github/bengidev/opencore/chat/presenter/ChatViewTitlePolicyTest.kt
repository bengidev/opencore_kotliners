package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChatViewTitlePolicyTest {

    @Test
    fun blankTitle_hidden() {
        assertNull(ChatViewTitlePolicy.resolve(""))
        assertNull(ChatViewTitlePolicy.resolve("   "))
    }

    @Test
    fun nonBlankTitle_shown() {
        assertEquals("Draft chat", ChatViewTitlePolicy.resolve("Draft chat"))
    }
}
