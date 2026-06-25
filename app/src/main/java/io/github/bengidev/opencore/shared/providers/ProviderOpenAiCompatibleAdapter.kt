package io.github.bengidev.opencore.shared.providers

/** Default adapter for OpenAI-compatible chat providers. */
internal class ProviderOpenAiCompatibleAdapter(
    override val descriptor: ProviderDescriptor,
    private val reasoningWireStyle: ProviderReasoningWireStyle = ProviderReasoningWireStyle.TOP_LEVEL_EFFORT,
    override val supportsProviderRouting: Boolean = false
) : ProviderAdapting {

    override fun encodeChatCompletionRequest(
        secret: String,
        request: ProviderChatRequest
    ): ProviderHttpRequest {
        val headers = buildMap {
            put("Authorization", "Bearer $secret")
            put("Content-Type", "application/json; charset=utf-8")
            put("Accept", "text/event-stream")
            putAll(descriptor.defaultHeaders)
        }
        val body = ProviderWireTypes.encodeChatCompletionBody(
            request = request,
            reasoningWireStyle = reasoningWireStyle,
            supportsProviderRouting = supportsProviderRouting
        )
        return ProviderHttpRequest(
            url = descriptor.chatCompletionsUrl,
            headers = headers,
            body = body
        )
    }

    override fun encodeModelsListRequest(secret: String): ProviderHttpRequest {
        val headers = buildMap {
            put("Authorization", "Bearer $secret")
            put("Accept", "application/json")
            putAll(descriptor.defaultHeaders)
        }
        return ProviderHttpRequest(
            url = descriptor.modelsUrl,
            headers = headers,
            method = "GET"
        )
    }
}
