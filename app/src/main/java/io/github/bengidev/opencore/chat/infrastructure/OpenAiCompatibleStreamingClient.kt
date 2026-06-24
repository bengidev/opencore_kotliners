package io.github.bengidev.opencore.chat.infrastructure

import io.github.bengidev.opencore.chat.domain.ChatStreamError
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelProviderApi
import io.github.bengidev.opencore.sidepanel.domain.SidePanelReasoningModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import java.net.HttpURLConnection
import java.net.URL

internal class OpenAiCompatibleStreamingClient(
    private val httpStream: suspend (
        String,
        Map<String, String>,
        String,
        suspend (ByteArray) -> Unit
    ) -> HttpStreamResult = ::defaultHttpStream
) {
    fun stream(
        provider: SidePanelProviderApi,
        modelId: String,
        apiKey: String,
        messages: List<SidePanelMessage>,
        reasoning: SidePanelReasoningModel
    ): Flow<ChatStreamingEvent> = channelFlow {
        try {
            val body = ChatCompletionsCodec.encodeRequest(
                modelId = modelId,
                messages = messages,
                reasoning = reasoning,
                stream = true
            )
            val headers = buildMap {
                put("Authorization", "Bearer $apiKey")
                put("Accept", "text/event-stream")
                putAll(provider.defaultHeaders)
            }
            var decoder = ChatSSEDecoder()
            var didEmitDone = false

            suspend fun dispatchSseEvent(event: ChatSSEDecoder.SseEvent) {
                when (event) {
                    is ChatSSEDecoder.SseEvent.Done -> {
                        send(ChatStreamingEvent.Done)
                        didEmitDone = true
                    }
                    is ChatSSEDecoder.SseEvent.Data -> {
                        ChatStreamingCodec.mapDataPayload(event.payload)?.forEach { chatEvent ->
                            send(chatEvent)
                            if (chatEvent is ChatStreamingEvent.Error) didEmitDone = true
                        }
                    }
                }
            }

            when (
                val result = httpStream(provider.chatCompletionsUrl, headers, body) { chunk ->
                    for (event in decoder.append(chunk)) {
                        dispatchSseEvent(event)
                    }
                }
            ) {
                is HttpStreamResult.Failure -> {
                    send(ChatStreamingEvent.Error(ChatStreamError(formatHttpError(result))))
                    didEmitDone = true
                }
                is HttpStreamResult.Success -> Unit
            }

            for (event in decoder.flush()) {
                dispatchSseEvent(event)
            }

            if (!didEmitDone) {
                send(ChatStreamingEvent.Done)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (error: Exception) {
            send(ChatStreamingEvent.Error(ChatStreamError(formatChatRequestError(error))))
        }
    }.flowOn(Dispatchers.IO)

    private fun formatHttpError(result: HttpStreamResult.Failure): String {
        val status = result.statusCode
        val body = result.errorBody
        if (status == 401) {
            return "Unauthorized (401). Check that your API key is valid."
        }
        if (status == 403) {
            val providerMessage = ChatCompletionsCodec.parseErrorMessage(body)
            return providerMessage?.let { "Forbidden (403): $it" }
                ?: "Forbidden (403). Your plan may not include API access. Upgrade your provider plan to use these endpoints."
        }
        val providerMessage = ChatCompletionsCodec.parseErrorMessage(body)
        return providerMessage?.let { "Request failed ($status): $it" }
            ?: "Request failed with status $status."
    }

}

internal sealed class HttpStreamResult {
    data object Success : HttpStreamResult()
    data class Failure(val statusCode: Int, val errorBody: String) : HttpStreamResult()
}

private suspend fun defaultHttpStream(
    url: String,
    headers: Map<String, String>,
    body: String,
    onChunk: suspend (ByteArray) -> Unit
): HttpStreamResult {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        doOutput = true
        connectTimeout = 30_000
        readTimeout = 0
        setRequestProperty("Content-Type", "application/json; charset=utf-8")
        headers.forEach { (name, value) -> setRequestProperty(name, value) }
    }
    val cancelHandler = currentCoroutineContext()[Job]?.invokeOnCompletion { _ ->
        runCatching { connection.disconnect() }
    }
    try {
        connection.outputStream.use { stream ->
            stream.write(body.toByteArray(Charsets.UTF_8))
        }
        val responseCode = connection.responseCode
        if (responseCode !in 200..299) {
            val errorBody = connection.errorStream
                ?.bufferedReader()
                ?.use { it.readText(limit = 64 * 1024) }
                .orEmpty()
            return HttpStreamResult.Failure(responseCode, errorBody)
        }
        connection.inputStream.use { stream ->
            val buffer = ByteArray(8_192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val read = stream.read(buffer)
                if (read <= 0) break
                onChunk(if (read == buffer.size) buffer else buffer.copyOf(read))
            }
        }
        return HttpStreamResult.Success
    } finally {
        cancelHandler?.dispose()
        connection.disconnect()
    }
}

private fun java.io.Reader.readText(limit: Int): String {
    val output = StringBuilder()
    val buffer = CharArray(1_024)
    while (output.length < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - output.length))
        if (read <= 0) break
        output.append(buffer, 0, read)
    }
    return output.toString()
}
