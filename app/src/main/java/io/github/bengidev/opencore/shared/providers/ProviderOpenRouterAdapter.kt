package io.github.bengidev.opencore.shared.providers

/** OpenRouter-specific adapter: nested reasoning object and provider routing. */
internal class ProviderOpenRouterAdapter : ProviderAdapting {
    private val base = ProviderOpenAiCompatibleAdapter(
        descriptor = ProviderDescriptor.openRouter,
        reasoningWireStyle = ProviderReasoningWireStyle.REASONING_OBJECT,
        supportsProviderRouting = true
    )

    override val descriptor: ProviderDescriptor get() = base.descriptor
    override val supportsProviderRouting: Boolean get() = base.supportsProviderRouting

    override fun encodeChatCompletionRequest(
        secret: String,
        request: ProviderChatRequest
    ): ProviderHttpRequest = base.encodeChatCompletionRequest(secret, request)

    override fun encodeModelsListRequest(secret: String): ProviderHttpRequest =
        base.encodeModelsListRequest(secret)
}
