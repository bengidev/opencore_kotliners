package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatStreamingEvent
import io.github.bengidev.opencore.chat.infrastructure.ChatStreamingClient
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.sidepanel.infrastructure.SettingsContextCompactionSummarizing
import io.github.bengidev.opencore.sidepanel.infrastructure.SidePanelPreferenceStore
import java.time.Instant
import java.util.UUID

internal class SettingsContextCompactionStreamSummarizer(
    private val streaming: ChatStreamingClient,
    private val preferenceStore: SidePanelPreferenceStore,
) : SettingsContextCompactionSummarizing {

    override suspend fun summarize(prompt: String): String {
        val preference = preferenceStore.preference()
        val modelId = preference.modelId
            ?: throw SettingsContextCompactionException.MissingModel

        val requestMessage = SidePanelMessage(
            id = UUID.randomUUID(),
            role = ChatMessageRole.USER,
            content = prompt,
            createdAt = Instant.now(),
        )

        var summary = ""
        streaming.stream(requestMessage.let { listOf(it) }, null, null).collect { event ->
            when (event) {
                is ChatStreamingEvent.TextDelta -> summary += event.text
                is ChatStreamingEvent.ThinkingDelta,
                is ChatStreamingEvent.OutputStreamBegan,
                is ChatStreamingEvent.OutputStreamDelta,
                is ChatStreamingEvent.OutputStreamEnded -> Unit
                ChatStreamingEvent.Done -> return@collect
                is ChatStreamingEvent.Error -> {
                    throw SettingsContextCompactionException.SummarizationFailed(event.error.message)
                }
            }
        }
        return summary.trim()
    }

    override suspend fun summarize(messages: List<SidePanelMessage>): String {
        val transcript = messages.joinToString("\n") { message ->
            "${message.role}: ${messageText(message)}"
        }
        val prompt = """
            Summarize the following conversation for continuation. Preserve goals, decisions, facts, and open tasks. Be concise.

            $transcript
            """.trimIndent()
        return summarize(prompt)
    }

    private fun messageText(message: SidePanelMessage): String =
        when (message.kind) {
            SidePanelMessageKind.TEXT -> message.content
            SidePanelMessageKind.THINKING -> message.content
            SidePanelMessageKind.SYSTEM -> message.content
            SidePanelMessageKind.OUTPUT_STREAM -> message.content
        }
}

internal sealed class SettingsContextCompactionException(message: String) : Exception(message) {
    data object MissingModel : SettingsContextCompactionException("No model selected for compaction summarization.")
    data class SummarizationFailed(val detail: String) :
        SettingsContextCompactionException("Compaction summarization failed: $detail")
}
