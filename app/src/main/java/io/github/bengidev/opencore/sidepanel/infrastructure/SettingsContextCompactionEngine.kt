package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.home.utilities.ContextWindowEstimator
import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionCompactionPlanner
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionContextBuilder
import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntry
import io.github.bengidev.opencore.shared.tokenization.ContextTokenCounter
import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionOutcome
import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionPreference
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import java.util.UUID

internal interface SettingsContextCompactionStrategizing {
    suspend fun compact(
        messages: List<SidePanelMessage>,
        contextLength: Int,
        minRecentMessages: Int,
    ): List<SidePanelMessage>
}

internal interface SettingsContextCompactionSummarizing {
    suspend fun summarize(messages: List<SidePanelMessage>): String
    suspend fun summarize(prompt: String): String
}

internal class SettingsContextCompactionTrimStrategy : SettingsContextCompactionStrategizing {
    override suspend fun compact(
        messages: List<SidePanelMessage>,
        contextLength: Int,
        minRecentMessages: Int,
    ): List<SidePanelMessage> {
        if (contextLength <= 0 || messages.size <= minRecentMessages) return messages

        val working = messages.toMutableList()
        val reserveTokens = SettingsContextCompactionPreference().reserveTokens
        val targetTokens = maxOf(0, contextLength - reserveTokens)

        while (working.size > minRecentMessages + 1 &&
            ContextTokenCounter.countTokens(working) > targetTokens
        ) {
            val dropIndex = indexOfOldestDroppableMessage(working, minRecentMessages) ?: break
            working.removeAt(dropIndex)
        }
        return working
    }

    private fun indexOfOldestDroppableMessage(
        messages: List<SidePanelMessage>,
        minRecentMessages: Int,
    ): Int? {
        val protectedTailStart = maxOf(0, messages.size - minRecentMessages)
        for (index in 0 until protectedTailStart) {
            if (!isLeadingSystemMessage(messages[index], index)) {
                return index
            }
        }
        return null
    }

    private fun isLeadingSystemMessage(message: SidePanelMessage, index: Int): Boolean =
        index == 0 && message.role == ChatMessageRole.SYSTEM
}

internal class SettingsContextCompactionEngine(
    private val trimStrategy: SettingsContextCompactionStrategizing = SettingsContextCompactionTrimStrategy(),
    private val summarizer: SettingsContextCompactionSummarizing,
) {
    fun shouldCompact(
        messages: List<SidePanelMessage>,
        contextLength: Int,
        preference: SettingsContextCompactionPreference,
    ): Boolean {
        if (!preference.isEnabled || contextLength <= 0 || messages.isEmpty()) return false
        return ContextWindowEstimator.shouldCompact(
            messages = messages,
            draft = null,
            contextLength = contextLength,
            reserveTokens = preference.reserveTokens,
        )
    }

    suspend fun compactIfNeeded(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
        preference: SettingsContextCompactionPreference,
    ): SettingsContextCompactionOutcome {
        if (!shouldCompact(messages, contextLength, preference)) {
            return SettingsContextCompactionOutcome.unchanged(messages)
        }
        return performCompaction(
            messages = messages,
            sessionEntries = sessionEntries,
            leafEntryId = leafEntryId,
            contextLength = contextLength,
            preference = preference,
            keepRecentTokens = preference.keepRecentTokens,
        )
    }

    suspend fun compactManually(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
        preference: SettingsContextCompactionPreference,
    ): SettingsContextCompactionOutcome =
        performCompaction(
            messages = messages,
            sessionEntries = sessionEntries,
            leafEntryId = leafEntryId,
            contextLength = contextLength,
            preference = preference,
            keepRecentTokens = 0,
        )

    suspend fun compactForOverflow(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
        preference: SettingsContextCompactionPreference,
    ): SettingsContextCompactionOutcome =
        performCompaction(
            messages = messages,
            sessionEntries = sessionEntries,
            leafEntryId = leafEntryId,
            contextLength = contextLength,
            preference = preference,
            keepRecentTokens = preference.keepRecentTokens,
        )

    private suspend fun performCompaction(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
        preference: SettingsContextCompactionPreference,
        keepRecentTokens: Int,
    ): SettingsContextCompactionOutcome {
        val tokensBefore = ContextTokenCounter.countTokens(messages)
        val plannerSettings = AtomSessionCompactionPlanner.Settings(keepRecentTokens = keepRecentTokens)
        val preparation = AtomSessionCompactionPlanner.prepareCompaction(
            entries = sessionEntries,
            leafId = leafEntryId,
            settings = plannerSettings,
            tokensBefore = tokensBefore,
        )
        if (preparation == null) {
            if (contextLength <= 0) return SettingsContextCompactionOutcome.unchanged(messages)
            val trimmed = trimStrategy.compact(messages, contextLength, preference.minRecentMessages)
            if (trimmed == messages) return SettingsContextCompactionOutcome.unchanged(messages)
            return SettingsContextCompactionOutcome(projectedMessages = trimmed, checkpoint = null)
        }

        val summaryBody = summarizePreparedSegment(preparation)
        val checkpoint = AtomCompactionCheckpoint(
            summary = summaryBody,
            firstKeptEntryId = preparation.firstKeptEntryId,
            tokensBefore = tokensBefore,
            readFiles = preparation.readFiles,
            modifiedFiles = preparation.modifiedFiles,
        )

        val syntheticEntries = sessionEntries.toMutableList()
        val compactionEntry = AtomSessionEntry.compactionEntry(
            id = UUID.randomUUID(),
            atomId = sessionEntries.firstOrNull()?.atomId ?: UUID.randomUUID(),
            parentId = leafEntryId,
            checkpoint = checkpoint,
            timestamp = java.time.Instant.now(),
        )
        syntheticEntries.add(compactionEntry)

        val projected = AtomSessionContextBuilder.buildModelMessages(
            entries = syntheticEntries,
            leafId = compactionEntry.id,
        )
        return SettingsContextCompactionOutcome(projectedMessages = projected, checkpoint = checkpoint)
    }

    private suspend fun summarizePreparedSegment(
        preparation: AtomSessionCompactionPlanner.Preparation,
    ): String {
        if (preparation.isSplitTurn) {
            val historyPrompt = AtomSessionCompactionPlanner.structuredSummarizationPrompt(
                messagesToSummarize = preparation.messagesToSummarize,
                previousSummary = preparation.previousSummary,
            )
            val historySummary = summarizer.summarize(historyPrompt)
            val turnPrefixPrompt = AtomSessionCompactionPlanner.splitTurnPrefixPrompt(
                preparation.turnPrefixMessages,
            )
            val turnPrefixSummary = summarizer.summarize(turnPrefixPrompt)
            val merged = AtomSessionCompactionPlanner.mergeSplitSummaries(historySummary, turnPrefixSummary)
            return appendFileTags(merged, preparation.readFiles, preparation.modifiedFiles)
        }

        val prompt = AtomSessionCompactionPlanner.structuredSummarizationPrompt(
            messagesToSummarize = preparation.messagesToSummarize,
            previousSummary = preparation.previousSummary,
        )
        val summaryText = summarizer.summarize(prompt)
        return appendFileTags(summaryText, preparation.readFiles, preparation.modifiedFiles)
    }

    private fun appendFileTags(
        summary: String,
        readFiles: List<String>,
        modifiedFiles: List<String>,
    ): String {
        var body = summary
        if (readFiles.isNotEmpty()) {
            body += "\n\n<read-files>\n${readFiles.joinToString("\n")}\n</read-files>"
        }
        if (modifiedFiles.isNotEmpty()) {
            body += "\n\n<modified-files>\n${modifiedFiles.joinToString("\n")}\n</modified-files>"
        }
        return body
    }
}
