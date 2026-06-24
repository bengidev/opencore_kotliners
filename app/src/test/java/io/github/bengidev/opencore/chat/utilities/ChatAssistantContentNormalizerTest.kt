package io.github.bengidev.opencore.chat.utilities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAssistantContentNormalizerTest {

    @Test
    fun displayText_extractsJsonContentBlocks() {
        val raw = """[{"type":"text","text":"GeForce is NVIDIA GPUs."}]"""
        assertEquals(
            "GeForce is NVIDIA GPUs.",
            ChatAssistantContentNormalizer.displayText(raw)
        )
    }

    @Test
    fun displayText_extractsPythonishContentBlocks() {
        val raw = "[{'type': 'text', 'text': \"GeForce is NVIDIA's brand for consumer GPUs.\"}]"
        assertEquals(
            "GeForce is NVIDIA's brand for consumer GPUs.",
            ChatAssistantContentNormalizer.displayText(raw)
        )
    }

    @Test
    fun displayText_passesThroughNormalProse() {
        val raw = "GeForce is NVIDIA's brand for consumer GPUs."
        assertEquals(raw, ChatAssistantContentNormalizer.displayText(raw))
    }

    @Test
    fun isSafetyOnlyOutput_detectsClassifierLabels() {
        val raw = "User Safety: safe\nResponse Safety: safe"
        assertTrue(ChatAssistantContentNormalizer.isSafetyOnlyOutput(raw))
    }

    @Test
    fun displayText_replacesSafetyOnlyOutputWithFallback() {
        val raw = "User Safety: safe\nResponse Safety: safe"
        assertEquals(
            ChatAssistantContentNormalizer.SAFETY_ONLY_FALLBACK,
            ChatAssistantContentNormalizer.displayText(raw)
        )
    }

    @Test
    fun mixedSafetyAndAnswer_isNotSafetyOnly() {
        val raw = "User Safety: safe\nA computer is a programmable machine."
        assertFalse(ChatAssistantContentNormalizer.isSafetyOnlyOutput(raw))
        assertEquals(raw, ChatAssistantContentNormalizer.displayText(raw))
    }
}
