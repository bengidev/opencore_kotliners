package io.github.bengidev.opencore.sidepanel.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SidePanelProviderApiTest {

    @Test
    fun all_includesOllamaCloud() {
        val ids = SidePanelProviderApi.all.map { it.id }
        assertTrue(ids.contains("ollama"))
        assertEquals(4, ids.size)
    }

    @Test
    fun openRouter_exposesCredentialHints() {
        val provider = SidePanelProviderApi.openRouter
        assertEquals("sk-or-v1-...", provider.credentialPlaceholder)
        assertEquals("OPENROUTER_API_KEY", provider.credentialLabel)
        assertTrue(provider.credentialPrompt.contains("openrouter.ai/keys"))
    }

    @Test
    fun ollamaCloud_usesCloudBaseUrlAndCredentialHints() {
        val provider = SidePanelProviderApi.ollamaCloud
        assertEquals("ollama", provider.id)
        assertEquals("Ollama Cloud", provider.displayName)
        assertEquals("https://ollama.com/v1", provider.baseUrl)
        assertEquals("ollama-...", provider.credentialPlaceholder)
        assertEquals("OLLAMA_API_KEY", provider.credentialLabel)
        assertTrue(provider.credentialPrompt.contains("ollama.com"))
    }
}
