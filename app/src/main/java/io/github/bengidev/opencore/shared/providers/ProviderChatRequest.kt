package io.github.bengidev.opencore.shared.providers

import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage

internal data class ProviderChatRequest(
    val providerId: String,
    val modelId: String,
    val messages: List<SidePanelMessage>,
    val reasoningEffort: String? = null,
    val providerSortBy: String? = null
)

internal data class ProviderHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val body: String? = null,
    val method: String = if (body != null) "POST" else "GET"
)
