package io.github.bengidev.opencore.shared.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogParserTest {

    @Test
    fun catalogEntry_parsesSupportedReasoningEfforts() {
        val json = """
            {"id":"openai/o4-mini","name":"O4 Mini","supported_parameters":["reasoning"],"reasoning":{"supported_efforts":["high","medium","low","none"],"mandatory":false}}
        """.trimIndent()

        val model = ProviderCatalogParser.parseEntry(json)

        assertEquals(listOf("high", "medium", "low", "none"), model.supportedReasoningEfforts)
        assertFalse(model.reasoningMandatory)
    }

    @Test
    fun catalogRouterEntry_enablesSpeedModes() {
        val json = """
            {"id":"openrouter/free","name":"Free Models Router","architecture":{"tokenizer":"Router"}}
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parseEntry(json).supportsSpeedModes)
    }

    @Test
    fun catalogStandardEntry_hidesSpeedModes() {
        val json = """
            {"id":"meta-llama/llama-3.3-70b-instruct:free","name":"Llama 3.3 70B","architecture":{"tokenizer":"Llama3"}}
        """.trimIndent()

        assertFalse(ProviderCatalogParser.parseEntry(json).supportsSpeedModes)
    }
}
