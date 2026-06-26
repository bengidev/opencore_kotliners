package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ProviderRegistryTest {

    private fun sampleChatRequest(
        modelId: String = "glm-5",
        reasoningEffort: String? = "high",
        providerSortBy: String? = null
    ) = ProviderChatRequest(
        providerId = ProviderDescriptor.openRouter.id,
        modelId = modelId,
        messages = listOf(
            SidePanelMessage(
                id = UUID.randomUUID(),
                role = "user",
                content = "Hi",
                createdAt = Instant.now()
            )
        ),
        reasoningEffort = reasoningEffort,
        providerSortBy = providerSortBy
    )

    @Test
    fun resolve_returnsOpenRouterAdapterForOpenRouterId() {
        val adapter = ProviderRegistry.resolve("openrouter")

        assertTrue(adapter is ProviderOpenRouterAdapter)
        assertEquals("openrouter", adapter.descriptor.id)
    }

    @Test
    fun resolve_fallsBackToDefaultForUnknownId() {
        assertEquals(
            ProviderRegistry.defaultAdapter.descriptor.id,
            ProviderRegistry.resolve("unknown").descriptor.id
        )
        assertEquals("openrouter", ProviderRegistry.resolve(null).descriptor.id)
    }

    @Test
    fun all_containsFourProviders() {
        assertEquals(4, ProviderRegistry.all.size)
        assertEquals(
            setOf("openrouter", "opencode", "commandcode", "ollama"),
            ProviderRegistry.all.map { it.descriptor.id }.toSet()
        )
    }

    @Test
    fun openRouter_supportsProviderRouting() {
        assertTrue(ProviderRegistry.resolve("openrouter").supportsProviderRouting)
    }

    @Test
    fun openCode_usesTopLevelReasoningEffort() {
        val request = ProviderRegistry.resolve("opencode")
            .encodeChatCompletionRequest("test-key", sampleChatRequest())

        val body = request.body.orEmpty()
        assertTrue(body.contains("\"reasoning_effort\":\"high\""))
        assertTrue(!body.contains("\"reasoning\":{\"effort\""))
    }

    @Test
    fun openRouter_usesNestedReasoningObject() {
        val request = ProviderRegistry.resolve("openrouter")
            .encodeChatCompletionRequest(
                "test-key",
                sampleChatRequest(modelId = "openrouter/free")
            )

        val body = request.body.orEmpty()
        assertTrue(body.contains("\"reasoning\":{\"effort\":\"high\"}"))
        assertTrue(!body.contains("reasoning_effort"))
    }

    @Test
    fun descriptorIdsMatchExpectedValues() {
        val expected = mapOf(
            "openrouter" to "OpenRouter",
            "opencode" to "OpenCode",
            "commandcode" to "Command Code",
            "ollama" to "Ollama Cloud"
        )

        assertEquals(expected.keys, ProviderRegistry.all.map { it.descriptor.id }.toSet())
        ProviderRegistry.all.forEach { adapter ->
            assertEquals(expected[adapter.descriptor.id], adapter.descriptor.displayName)
        }
    }
}
