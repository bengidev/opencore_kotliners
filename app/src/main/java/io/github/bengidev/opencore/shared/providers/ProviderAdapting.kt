package io.github.bengidev.opencore.shared.providers

/** Adapter contract for an external AI provider. */
internal interface ProviderAdapting {
    val descriptor: ProviderDescriptor
    val supportsProviderRouting: Boolean

    fun encodeChatCompletionRequest(secret: String, request: ProviderChatRequest): ProviderHttpRequest

    fun encodeModelsListRequest(secret: String): ProviderHttpRequest
}
