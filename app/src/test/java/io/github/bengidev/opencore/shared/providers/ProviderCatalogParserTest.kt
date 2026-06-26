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

    @Test
    fun parse_extractsFreeFlagFromPricing() {
        val body = """
            {
              "data": [
                {
                  "id": "meta-llama/llama-3.3-70b-instruct:free",
                  "name": "Meta: Llama 3.3 70B Instruct (free)",
                  "context_length": 131072,
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0", "completion": "0" }
                },
                {
                  "id": "openai/gpt-4o",
                  "name": "OpenAI: GPT-4o",
                  "context_length": 128000,
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0.000005", "completion": "0.000015" }
                }
              ]
            }
        """.trimIndent()

        val models = ProviderCatalogParser.parse(body)

        assertEquals(2, models.size)
        assertTrue(models.first { it.id.endsWith(":free") }.isFree)
        assertFalse(models.first { it.id == "openai/gpt-4o" }.isFree)
        assertEquals(131_072, models.first { it.id.endsWith(":free") }.contextLength)
    }

    @Test
    fun parse_treatsDecimalZeroPricingAsFree() {
        val body = """
            {
              "data": [
                {
                  "id": "vendor/model:free",
                  "name": "Vendor Model",
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0.0", "completion": "0.000000" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().isFree)
    }

    @Test
    fun parse_sortsFreeModelsFirst() {
        val body = """
            {
              "data": [
                {
                  "id": "openai/gpt-4o",
                  "name": "OpenAI: GPT-4o",
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "1", "completion": "1" }
                },
                {
                  "id": "meta-llama/llama-3.3-70b-instruct:free",
                  "name": "Meta: Llama 3.3 70B Instruct (free)",
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0", "completion": "0" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).first().isFree)
    }

    @Test
    fun parse_filtersNonTextModalities() {
        val body = """
            {
              "data": [
                {
                  "id": "google/lyria-3",
                  "name": "Google: Lyria 3",
                  "architecture": { "modality": "audio" }
                },
                {
                  "id": "meta-llama/llama-3.3-70b-instruct:free",
                  "name": "Meta: Llama 3.3 70B Instruct (free)",
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0", "completion": "0" }
                }
              ]
            }
        """.trimIndent()

        val models = ProviderCatalogParser.parse(body)

        assertEquals(1, models.size)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", models.first().id)
    }

    @Test
    fun parse_routerTokenizer_enablesSpeedModes() {
        val body = """
            {
              "data": [
                {
                  "id": "openrouter/free",
                  "name": "Free Models Router",
                  "architecture": { "modality": "text", "tokenizer": "Router" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().supportsSpeedModes)
    }

    @Test
    fun parse_treatsFreeSuffixAsFreeWithoutPricing() {
        val body = """
            {
              "data": [
                {
                  "id": "meta-llama/llama-3.3-70b-instruct:free",
                  "name": "Llama 3.3 70B",
                  "architecture": { "modality": "text" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().isFree)
    }

    @Test
    fun parse_resolvesContextLengthFromAlternateKeys() {
        val body = """
            {
              "data": [
                {
                  "id": "vendor/model",
                  "name": "Vendor Model",
                  "context": 65536,
                  "architecture": { "modality": "text" }
                }
              ]
            }
        """.trimIndent()

        assertEquals(65_536, ProviderCatalogParser.parse(body).single().contextLength)
    }

    @Test
    fun parse_detectsReasoningFromSupportedParameters() {
        val body = """
            {
              "data": [
                {
                  "id": "vendor/reasoner",
                  "name": "Reasoner",
                  "architecture": { "modality": "text" },
                  "supported_parameters": ["reasoning_effort"]
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().supportsReasoning)
    }

    @Test
    fun parse_openRouterFreeId_enablesSpeedModesWithoutRouterTokenizer() {
        val body = """
            {
              "data": [
                {
                  "id": "openrouter/free",
                  "name": "Free Models Router",
                  "architecture": { "modality": "text", "tokenizer": "Other" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().supportsSpeedModes)
    }

    @Test
    fun parse_detectsReasoningFromThinkingSuffix() {
        val body = """
            {
              "data": [
                {
                  "id": "vendor/kimi-k2-thinking",
                  "name": "Kimi K2 Thinking",
                  "architecture": { "modality": "text" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().supportsReasoning)
    }

    @Test
    fun parse_treatsFreeNameAsFreeWithoutPricing() {
        val body = """
            {
              "data": [
                {
                  "id": "vendor/sample-model",
                  "name": "Sample Free Model",
                  "architecture": { "modality": "text" }
                }
              ]
            }
        """.trimIndent()

        assertTrue(ProviderCatalogParser.parse(body).single().isFree)
    }

    @Test
    fun parse_standardTokenizer_hidesSpeedModes() {
        val body = """
            {
              "data": [
                {
                  "id": "meta-llama/llama-3.3-70b-instruct:free",
                  "name": "Llama 3.3 70B",
                  "architecture": { "modality": "text", "tokenizer": "Llama3" }
                }
              ]
            }
        """.trimIndent()

        assertFalse(ProviderCatalogParser.parse(body).single().supportsSpeedModes)
    }
}
