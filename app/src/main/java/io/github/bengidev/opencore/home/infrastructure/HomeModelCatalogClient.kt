package io.github.bengidev.opencore.home.infrastructure

import io.github.bengidev.opencore.shared.providers.ProviderAdapting
import io.github.bengidev.opencore.shared.providers.ProviderCatalogParser
import io.github.bengidev.opencore.shared.providers.ProviderWireTypes
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

internal class HomeModelCatalogClient(
    private val httpGet: suspend (String, Map<String, String>) -> HttpGetResult = ::defaultHttpGet,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    data class CatalogResult(
        val models: List<SidePanelModel>,
        val errorHint: String? = null,
        val isLive: Boolean = false
    )

    suspend fun listModels(
        adapter: ProviderAdapting,
        secret: String?
    ): CatalogResult = withContext(ioDispatcher) {
        if (secret.isNullOrBlank()) {
            return@withContext CatalogResult(models = emptyList(), isLive = false)
        }

        val request = adapter.encodeModelsListRequest(secret)

        try {
            val response = httpGet(request.url, request.headers)
            if (response.statusCode !in 200..299) {
                val hint = if (response.statusCode == 403) {
                    ProviderWireTypes.parseErrorMessage(response.body)
                        ?: "Your plan doesn't include API access. Upgrade to use these endpoints."
                } else {
                    GENERIC_LOAD_ERROR_HINT
                }
                return@withContext CatalogResult(
                    models = emptyList(),
                    errorHint = hint,
                    isLive = false
                )
            }
            val models = ProviderCatalogParser.parse(response.body)
            CatalogResult(models = models, isLive = models.isNotEmpty())
        } catch (_: Exception) {
            CatalogResult(
                models = emptyList(),
                isLive = false,
                errorHint = GENERIC_LOAD_ERROR_HINT
            )
        }
    }

    data class HttpGetResult(
        val statusCode: Int,
        val body: String
    )

    companion object {
        private const val GENERIC_LOAD_ERROR_HINT =
            "Couldn't load models. Check your connection and try again."

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
