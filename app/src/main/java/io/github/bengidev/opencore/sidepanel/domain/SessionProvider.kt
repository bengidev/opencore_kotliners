package io.github.bengidev.opencore.sidepanel.domain

internal enum class SessionProvider(val displayName: String) {
    OpenRouter(displayName = "OpenRouter"),
    Anthropic(displayName = "Anthropic"),
    OpenAI(displayName = "OpenAI")
}
