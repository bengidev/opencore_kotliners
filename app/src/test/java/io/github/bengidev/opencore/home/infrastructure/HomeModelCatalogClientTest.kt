package io.github.bengidev.opencore.home.infrastructure

import io.github.bengidev.opencore.shared.providers.ProviderDescriptor
import io.github.bengidev.opencore.shared.providers.ProviderRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelCatalogClientTest {

    private val openRouter = ProviderRegistry.resolve(ProviderDescriptor.openRouter.id)

    @Test
    fun listModels_withoutSecret_returnsEmptyCatalog() = runTest {
        val client = HomeModelCatalogClient()
        val result = client.listModels(openRouter, secret = null)

        assertTrue(result.models.isEmpty())
        assertFalse(result.isLive)
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
                  "architecture": { "modality": "text+reasoning" },
                  "pricing": { "prompt": "0", "completion": "0" }
                }
              ]
            }
        """.trimIndent()
        val client = HomeModelCatalogClient(
            httpGet = { _, _ ->
                HomeModelCatalogClient.HttpGetResult(statusCode = 200, body = body)
            }
        )

        val result = client.listModels(openRouter, secret = "sk-test")

        assertEquals(1, result.models.size)
        assertTrue(result.models.first().isFree)
        assertTrue(result.models.first().supportsReasoning)
        assertEquals(163_840, result.models.first().contextLength)
    }

    @Test
    fun listModels_withForbiddenResponse_returnsEmptyCatalogAndHint() = runTest {
        val client = HomeModelCatalogClient(
            httpGet = { _, _ ->
                HomeModelCatalogClient.HttpGetResult(
                    statusCode = 403,
                    body = """{"error":{"message":"Plan upgrade required"}}"""
                )
            }
        )

        val result = client.listModels(openRouter, secret = "sk-test")

        assertFalse(result.isLive)
        assertEquals("Plan upgrade required", result.errorHint)
        assertTrue(result.models.isEmpty())
    }

    @Test
    fun listModels_withNetworkFailure_returnsGenericHint() = runTest {
        val client = HomeModelCatalogClient(
            httpGet = { _, _ -> throw RuntimeException("offline") }
        )

        val result = client.listModels(openRouter, secret = "sk-test")

        assertFalse(result.isLive)
        assertTrue(result.models.isEmpty())
        assertEquals(
            "Couldn't load models. Check your connection and try again.",
            result.errorHint
        )
    }
}
