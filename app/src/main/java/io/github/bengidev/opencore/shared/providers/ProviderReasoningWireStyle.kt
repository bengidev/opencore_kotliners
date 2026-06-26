package io.github.bengidev.opencore.shared.providers

/** How a provider encodes reasoning effort on chat completion requests. */
internal enum class ProviderReasoningWireStyle {
    /** `{ "reasoning": { "effort": "high" } }` (OpenRouter). */
    REASONING_OBJECT,
    /** Top-level `reasoning_effort` (OpenAI-compatible default). */
    TOP_LEVEL_EFFORT
}
