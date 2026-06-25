package io.github.bengidev.opencore.home.application

import io.github.bengidev.opencore.home.infrastructure.HomeModelCatalogClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelCredentialStore

internal object HomeTestCatalog {
    val sampleModels: List<SidePanelModel> = listOf(
        SidePanelModel(
            id = "meta-llama/llama-3.3-70b-instruct:free",
            displayTitle = "Llama 3.3 70B",
            isFree = true,
            contextLength = 131_072
        ),
        SidePanelModel(
            id = "deepseek/deepseek-r1:free",
            displayTitle = "DeepSeek R1",
            isFree = true,
            contextLength = 163_840,
            supportedReasoningEfforts = listOf("high", "medium", "low")
        )
    )

    fun credentialStoreWithKey(
        providerId: String = SidePanelProviderApi.default.id
    ): InMemorySidePanelCredentialStore = InMemorySidePanelCredentialStore().apply {
        save("test-key", providerId)
    }

    fun catalogClient(
        ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
        httpGet: suspend (String, Map<String, String>) -> HomeModelCatalogClient.HttpGetResult =
            { _, _ -> HomeModelCatalogClient.HttpGetResult(statusCode = 200, body = sampleModelsBody) }
    ): HomeModelCatalogClient = HomeModelCatalogClient(httpGet = httpGet, ioDispatcher = ioDispatcher)

    val sampleModelsBody: String = """
        {
          "data": [
            {
              "id": "meta-llama/llama-3.3-70b-instruct:free",
              "name": "Llama 3.3 70B",
              "context_length": 131072,
              "architecture": { "modality": "text" },
              "pricing": { "prompt": "0", "completion": "0" }
            },
            {
              "id": "deepseek/deepseek-r1:free",
              "name": "DeepSeek R1",
              "context_length": 163840,
              "architecture": { "modality": "text+reasoning" },
              "pricing": { "prompt": "0", "completion": "0" }
            }
          ]
        }
    """.trimIndent()
}
