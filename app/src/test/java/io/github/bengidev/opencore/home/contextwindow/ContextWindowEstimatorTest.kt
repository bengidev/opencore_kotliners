package io.github.bengidev.opencore.home.contextwindow

import io.github.bengidev.opencore.home.contextwindow.utilities.ContextWindowEstimator
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.util.UUID

class ContextWindowEstimatorTest {

    private fun estimatedTokens(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return (trimmed.length + 3) / 4
    }

    private fun message(
        content: String,
        kind: SidePanelMessageKind = SidePanelMessageKind.TEXT,
        role: String = "user",
    ) = SidePanelMessage(
        id = UUID.randomUUID(),
        role = role,
        content = content,
        createdAt = Instant.parse("2024-01-01T00:00:00Z"),
        kind = kind,
    )

    @Test
    fun emptyConversationWithKnownLimit() {
        val usage = ContextWindowEstimator.estimate(
            messages = emptyList(),
            draft = null,
            contextLength = 258_000,
        )

        assertEquals(0, usage.tokensUsed)
        assertEquals(258_000, usage.tokenLimit)
        assertEquals(0.0, usage.fractionUsed, 0.0)
    }

    @Test
    fun whitespaceDraftIgnored() {
        val usage = ContextWindowEstimator.estimate(
            messages = emptyList(),
            draft = "   \n\t  ",
            contextLength = 258_000,
        )

        assertEquals(0, usage.tokensUsed)
    }

    @Test
    fun messagesContributeToUsedTokens() {
        val userText = "Hello from the user"
        val assistantText = "Hi"
        val usage = ContextWindowEstimator.estimate(
            messages = listOf(
                message(content = userText, role = "user"),
                message(content = assistantText, role = "assistant"),
            ),
            draft = null,
            contextLength = 131_072,
        )

        val expectedUsed = estimatedTokens(userText) + estimatedTokens(assistantText)
        assertEquals(expectedUsed, usage.tokensUsed)
        assertEquals(131_072, usage.tokenLimit)
        assertEquals(expectedUsed / 131_072.0, usage.fractionUsed, 0.0)
    }

    @Test
    fun draftIncludedInEstimate() {
        val messageText = "Hi"
        val draftText = "Hello"
        val usage = ContextWindowEstimator.estimate(
            messages = listOf(message(content = messageText)),
            draft = draftText,
            contextLength = 100_000,
        )

        val expectedUsed = estimatedTokens(messageText) + estimatedTokens(draftText)
        assertEquals(expectedUsed, usage.tokensUsed)
    }

    @Test
    fun thinkingMessagesCounted() {
        val thinkingText = "Reasoning here"
        val answerText = "Answer"
        val usage = ContextWindowEstimator.estimate(
            messages = listOf(
                message(content = thinkingText, kind = SidePanelMessageKind.THINKING, role = "assistant"),
                message(content = answerText, role = "assistant"),
            ),
            draft = null,
            contextLength = 163_840,
        )

        val expectedUsed = estimatedTokens(thinkingText) + estimatedTokens(answerText)
        assertEquals(expectedUsed, usage.tokensUsed)
    }

    @Test
    fun modelContextLengthSetsTokenLimit() {
        val usage = ContextWindowEstimator.estimate(
            messages = listOf(message(content = "Ping")),
            draft = null,
            contextLength = 200_000,
        )

        assertEquals(200_000, usage.tokenLimit)
    }

    @Test
    fun nilContextLengthYieldsUnknownLimit() {
        val usage = ContextWindowEstimator.estimate(
            messages = listOf(message(content = "Ping")),
            draft = null,
            contextLength = null,
        )

        assertEquals(0, usage.tokenLimit)
        assertEquals(0.0, usage.fractionUsed, 0.0)
    }
}
