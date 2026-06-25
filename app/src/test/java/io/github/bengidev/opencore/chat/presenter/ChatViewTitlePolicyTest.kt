package io.github.bengidev.opencore.chat.presenter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun blankTitle_noHeadingSemantics() {
        assertFalse(ChatViewTitlePolicy.requiresHeadingSemantics(null))
        assertFalse(ChatViewTitlePolicy.requiresHeadingSemantics(""))
        assertFalse(ChatViewTitlePolicy.requiresHeadingSemantics("   "))
    }

    @Test
    fun nonBlankTitle_requiresHeadingSemantics() {
        assertTrue(ChatViewTitlePolicy.requiresHeadingSemantics("Draft chat"))
    }
}
