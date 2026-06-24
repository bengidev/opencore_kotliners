package io.github.bengidev.opencore.home.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderModelsResponseParserTest {

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

        val models = ProviderModelsResponseParser.parse(body)

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

        val models = ProviderModelsResponseParser.parse(body)

        assertTrue(models.single().isFree)
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

        val models = ProviderModelsResponseParser.parse(body)

        assertTrue(models.first().isFree)
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

        val models = ProviderModelsResponseParser.parse(body)

        assertEquals(1, models.size)
        assertEquals("meta-llama/llama-3.3-70b-instruct:free", models.first().id)
    }
}

class HomeModelCatalogClientTest {

    @Test
    fun listModels_withoutSecret_returnsFallbackCatalog() = runTest {
        val client = HomeModelCatalogClient()
        val result = client.listModels(SidePanelProviderApi.openRouter, secret = null)

        assertTrue(result.models.any { it.displayTitle == "Free Models Router" })
        assertTrue(result.models.all { it.isFree })
    }

    @Test
    fun listModels_withSuccessfulResponse_returnsParsedModels() = runTest {
        val body = """
            {
              "data": [
                {
                  "id": "deepseek/deepseek-r1:free",
                  "name": "DeepSeek: R1 (free)",
                  "context_length": 163840,
                  "architecture": { "modality": "text" },
                  "pricing": { "prompt": "0", "completion": "0" }
                }
              ]
            }
        """.trimIndent()
        val client = HomeModelCatalogClient { _, _ ->
            HomeModelCatalogClient.HttpGetResult(statusCode = 200, body = body)
        }

        val result = client.listModels(SidePanelProviderApi.openRouter, secret = "sk-test")

        assertEquals(1, result.models.size)
        assertTrue(result.models.first().isFree)
        assertTrue(result.models.first().supportsReasoning)
        assertEquals(163_840, result.models.first().contextLength)
    }

    @Test
    fun listModels_withForbiddenResponse_returnsFallbackAndHint() = runTest {
        val client = HomeModelCatalogClient { _, _ ->
            HomeModelCatalogClient.HttpGetResult(
                statusCode = 403,
                body = """{"error":{"message":"Plan upgrade required"}}"""
            )
        }

        val result = client.listModels(SidePanelProviderApi.openRouter, secret = "sk-test")

        assertFalse(result.isLive)
        assertEquals("Plan upgrade required", result.errorHint)
        assertTrue(result.models.any { it.displayTitle == "Free Models Router" })
    }
}
