package io.github.bengidev.opencore.shared.providers

internal data class ProviderChatContentPart(
    val type: String,
    val text: String? = null,
    val imageUrl: String? = null,
    val videoUrl: String? = null,
) {
    companion object {
        fun text(value: String) = ProviderChatContentPart(type = "text", text = value)

        fun imageUrl(dataUrl: String) = ProviderChatContentPart(type = "image_url", imageUrl = dataUrl)

        fun videoUrl(dataUrl: String) = ProviderChatContentPart(type = "video_url", videoUrl = dataUrl)
    }
}

internal sealed class ProviderChatMessageContent {
    data class Text(val value: String) : ProviderChatMessageContent()
    data class Parts(val parts: List<ProviderChatContentPart>) : ProviderChatMessageContent()
}
