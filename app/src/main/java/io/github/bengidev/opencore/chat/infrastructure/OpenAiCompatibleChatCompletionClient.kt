package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

internal class OpenAiCompatibleChatCompletionClient(
    private val httpPost: suspend (String, Map<String, String>, String) -> String = ::defaultHttpPost
) {
    suspend fun complete(
        provider: SidePanelProviderApi,
        modelId: String,
        apiKey: String,
        messages: List<SidePanelMessage>,
        reasoning: SidePanelReasoningModel
    ): SidePanelMessage {
        return try {
            val body = ChatCompletionsCodec.encodeRequest(modelId, messages, reasoning)
            val headers = buildMap {
                put("Authorization", "Bearer $apiKey")
                putAll(provider.defaultHeaders)
            }
            val response = httpPost(provider.chatCompletionsUrl, headers, body)
            assistantMessage(ChatCompletionsCodec.decodeAssistantContent(response))
        } catch (error: ChatCompletionException) {
            assistantMessage(error.message.orEmpty())
        } catch (error: Exception) {
            assistantMessage(formatChatRequestError(error))
        }
    }

    private fun assistantMessage(content: String): SidePanelMessage =
        SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.ASSISTANT,
            content = content,
            createdAt = Instant.now()
        )
}

private suspend fun defaultHttpPost(
    url: String,
    headers: Map<String, String>,
    body: String
): String = withContext(Dispatchers.IO) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 30_000
        readTimeout = 60_000
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
    }
    try {
        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        }
        val responseCode = connection.responseCode
        val responseBody = (if (responseCode in 200..299) connection.inputStream else connection.errorStream)
            ?.bufferedReader()
            ?.use { it.readText() }
            .orEmpty()
        if (responseCode !in 200..299) {
            throw ChatCompletionException(
                ChatCompletionsCodec.parseErrorMessage(responseBody) ?: "HTTP $responseCode"
            )
        }
        responseBody
    } finally {
        connection.disconnect()
    }
}
