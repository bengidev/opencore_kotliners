package io.github.bengidev.opencore.shared.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDescriptorTest {

    @Test
    fun allDescriptors_includesOllamaCloud() {
        val ids = ProviderRegistry.allDescriptors.map { it.id }
        assertTrue(ids.contains("ollama"))
        assertEquals(4, ids.size)
    }

    @Test
    fun openRouter_exposesCredentialHints() {
        val provider = ProviderDescriptor.openRouter
        assertEquals("sk-or-v1-...", provider.credentialPlaceholder)
        assertEquals("OPENROUTER_API_KEY", provider.credentialLabel)
        assertTrue(provider.credentialPrompt.contains("openrouter.ai/keys"))
    }

    @Test
    fun ollamaCloud_usesCloudBaseUrlAndCredentialHints() {
        val provider = ProviderDescriptor.ollamaCloud
        assertEquals("ollama", provider.id)
        assertEquals("Ollama Cloud", provider.displayName)
        assertEquals("https://ollama.com/v1", provider.baseUrl)
        assertEquals("ollama-...", provider.credentialPlaceholder)
        assertEquals("OLLAMA_API_KEY", provider.credentialLabel)
        assertTrue(provider.credentialPrompt.contains("ollama.com"))
    }
}
