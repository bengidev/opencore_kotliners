package io.github.bengidev.opencore.home.infrastructure

import io.github.bengidev.opencore.chat.infrastructure.ChatCompletionsCodec
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModelCatalog
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal class HomeModelCatalogClient(
    private val httpGet: suspend (String, Map<String, String>) -> HttpGetResult = ::defaultHttpGet
) {
    data class CatalogResult(
        val models: List<SidePanelModel>,
        val errorHint: String? = null
    )

    suspend fun listModels(
        provider: SidePanelProviderApi,
        secret: String?
    ): CatalogResult = withContext(Dispatchers.IO) {
        if (secret.isNullOrBlank()) {
            return@withContext CatalogResult(SidePanelModelCatalog.modelsFor(provider))
        }

        val headers = buildMap {
            put("Authorization", "Bearer $secret")
            put("Accept", "application/json")
            putAll(provider.defaultHeaders)
        }

        try {
            val response = httpGet(provider.modelsUrl, headers)
            if (response.statusCode !in 200..299) {
                val hint = if (response.statusCode == 403) {
                    ChatCompletionsCodec.parseErrorMessage(response.body)
                        ?: "Your plan doesn't include API access. Upgrade to use these endpoints."
                } else {
                    null
                }
                return@withContext CatalogResult(
                    models = SidePanelModelCatalog.modelsFor(provider),
                    errorHint = hint
                )
            }
            val models = ProviderModelsResponseParser.parse(response.body)
            if (models.isEmpty()) {
                CatalogResult(SidePanelModelCatalog.modelsFor(provider))
            } else {
                CatalogResult(models)
            }
        } catch (_: Exception) {
            CatalogResult(SidePanelModelCatalog.modelsFor(provider))
        }
    }

    data class HttpGetResult(
        val statusCode: Int,
        val body: String
    )

    companion object {
        private suspend fun defaultHttpGet(
            url: String,
            headers: Map<String, String>
        ): HttpGetResult {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 15_000
                headers.forEach { (field, value) -> setRequestProperty(field, value) }
            }
            return try {
                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                HttpGetResult(statusCode = statusCode, body = body)
            } finally {
                connection.disconnect()
            }
        }
    }
}
