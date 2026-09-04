package io.github.bengidev.opencore.shared.persistence.session

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.shared.tokenization.ContextTokenCounter
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import java.util.UUID

/** Pi-style compaction preparation: token-budget cut points and summarization spans. */
internal object AtomSessionCompactionPlanner {
    data class Preparation(
        val firstKeptEntryId: UUID,
        val messagesToSummarize: List<SidePanelMessage>,
        val turnPrefixMessages: List<SidePanelMessage>,
        val isSplitTurn: Boolean,
        val tokensBefore: Int,
        val previousSummary: String?,
        val readFiles: List<String>,
        val modifiedFiles: List<String>,
    )

    data class Settings(
        val keepRecentTokens: Int = 20_000,
    )

    private data class CutPoint(
        val cutIndex: Int,
        val isSplitTurn: Boolean,
        val turnPrefixMessages: List<SidePanelMessage>,
    )

    fun prepareCompaction(
        entries: List<AtomSessionEntry>,
        leafId: UUID?,
        settings: Settings,
        tokensBefore: Int,
    ): Preparation? {
        val path = AtomSessionContextBuilder.buildPath(entries, leafId)
        if (path.isEmpty() || path.last().kind == AtomSessionEntryKind.COMPACTION) return null

        val previousCompactionIndex = path.indexOfLast { it.kind == AtomSessionEntryKind.COMPACTION }
        var boundaryStart = 0
        var previousSummary: String? = null
        if (previousCompactionIndex >= 0) {
            val previousCompaction = path[previousCompactionIndex]
            previousSummary = previousCompaction.compaction?.summary
            val firstKept = previousCompaction.compaction?.firstKeptEntryId
            if (firstKept != null) {
                val keptIndex = path.indexOfFirst { it.id == firstKept }
                boundaryStart = if (keptIndex >= 0) keptIndex else previousCompactionIndex + 1
            } else {
                boundaryStart = previousCompactionIndex + 1
            }
        }

        val messageEntries = path.mapIndexedNotNull { index, entry ->
            if (entry.kind == AtomSessionEntryKind.MESSAGE && entry.message != null) {
                index to entry.message
            } else {
                null
            }
        }
        if (messageEntries.size <= 1) return null

        val cut = findCutPoint(
            path = path,
            messageEntries = messageEntries,
            boundaryStartIndex = boundaryStart,
            keepRecentTokens = settings.keepRecentTokens,
        ) ?: return null

        val firstKeptEntryId = path[messageEntries[cut.cutIndex].first].id
        val summarizeEndPathIndex = messageEntries[cut.cutIndex].first
        val messagesToSummarize = messageEntries
            .filter { (pathIndex, _) -> pathIndex >= boundaryStart && pathIndex < summarizeEndPathIndex }
            .map { it.second }

        val turnPrefixMessages = cut.turnPrefixMessages
        if (messagesToSummarize.isEmpty() && turnPrefixMessages.isEmpty()) return null

        val fileOps = extractFileOperations(messagesToSummarize + turnPrefixMessages)

        return Preparation(
            firstKeptEntryId = firstKeptEntryId,
            messagesToSummarize = messagesToSummarize,
            turnPrefixMessages = turnPrefixMessages,
            isSplitTurn = cut.isSplitTurn,
            tokensBefore = tokensBefore,
            previousSummary = previousSummary,
            readFiles = fileOps.first,
            modifiedFiles = fileOps.second,
        )
    }

    fun serializeForSummarization(messages: List<SidePanelMessage>): String =
        messages.joinToString("\n") { message ->
            when (message.kind) {
                SidePanelMessageKind.TEXT -> {
                    val roleLabel = if (message.role == ChatMessageRole.ASSISTANT) "Assistant" else "User"
                    "[$roleLabel]: ${message.content}"
                }
                SidePanelMessageKind.THINKING -> "[Assistant thinking]: ${message.content}"
                SidePanelMessageKind.SYSTEM -> "[System]: ${message.content}"
                SidePanelMessageKind.OUTPUT_STREAM -> {
                    val detail = message.detailJson?.take(2_000) ?: ""
                    "[Command]: ${message.content}\n[Output]: $detail"
                }
            }
        }

    fun structuredSummarizationPrompt(
        messagesToSummarize: List<SidePanelMessage>,
        previousSummary: String?,
        customInstructions: String? = null,
    ): String {
        val conversation = serializeForSummarization(messagesToSummarize)
        val prompt = buildString {
            appendLine(
                """
                Summarize the following conversation segment using this structure:

                ## Goal
                ## Constraints & Preferences
                ## Progress
                ### Done
                ### In Progress
                ### Blocked
                ## Key Decisions
                ## Next Steps
                ## Critical Context

                <read-files>
                </read-files>

                <modified-files>
                </modified-files>

                Conversation:
                $conversation
                """.trimIndent(),
            )
            if (!previousSummary.isNullOrBlank()) {
                appendLine()
                appendLine("Previous compaction summary to merge and update:")
                appendLine(previousSummary)
            }
            if (!customInstructions.isNullOrBlank()) {
                appendLine()
                appendLine("Additional focus:")
                appendLine(customInstructions)
            }
        }
        return prompt
    }

    fun splitTurnPrefixPrompt(messages: List<SidePanelMessage>): String {
        val conversation = serializeForSummarization(messages)
        return """
            Summarize the early portion of the current assistant turn. Preserve tool output, decisions, and facts needed to continue the turn.

            Turn prefix:
            $conversation
            """.trimIndent()
    }

    fun mergeSplitSummaries(historySummary: String, turnPrefixSummary: String): String =
        """
        ${historySummary.trim()}

        ## Current Turn (partial)
        ${turnPrefixSummary.trim()}
        """.trimIndent()

    private fun findCutPoint(
        path: List<AtomSessionEntry>,
        messageEntries: List<Pair<Int, SidePanelMessage>>,
        boundaryStartIndex: Int,
        keepRecentTokens: Int,
    ): CutPoint? {
        val lastIndex = messageEntries.indices.lastOrNull() ?: return null

        val turnStartIndex = messageEntries.indexOfLast { (pathIndex, message) ->
            pathIndex >= boundaryStartIndex && isUserTurnStart(message)
        }.let { if (it < 0) boundaryStartIndex else it }

        val turnTokenCount = messageEntries
            .drop(turnStartIndex)
            .sumOf { (_, message) -> ContextTokenCounter.countTokens(message) }

        if (turnTokenCount > keepRecentTokens) {
            return splitTurnCutPoint(messageEntries, turnStartIndex, keepRecentTokens)
        }

        var accumulated = 0
        for (index in lastIndex downTo 0) {
            val (pathIndex, message) = messageEntries[index]
            if (pathIndex < boundaryStartIndex) break
            accumulated += ContextTokenCounter.countTokens(message)
            if (accumulated < keepRecentTokens) continue

            val snapped = snapToUserBoundary(
                messageEntries = messageEntries,
                candidateIndex = index,
                boundaryStartIndex = boundaryStartIndex,
                keepRecentTokens = keepRecentTokens,
            )
            return CutPoint(
                cutIndex = snapped ?: index,
                isSplitTurn = false,
                turnPrefixMessages = emptyList(),
            )
        }

        return CutPoint(
            cutIndex = messageEntries.indices.first,
            isSplitTurn = false,
            turnPrefixMessages = emptyList(),
        )
    }

    private fun splitTurnCutPoint(
        messageEntries: List<Pair<Int, SidePanelMessage>>,
        turnStartIndex: Int,
        keepRecentTokens: Int,
    ): CutPoint? {
        var accumulated = 0
        for (index in messageEntries.indices.last downTo turnStartIndex) {
            val message = messageEntries[index].second
            accumulated += ContextTokenCounter.countTokens(message)
            if (accumulated < keepRecentTokens && index != turnStartIndex) continue

            val turnPrefix = if (index > turnStartIndex) {
                messageEntries.subList(turnStartIndex, index).map { it.second }
            } else {
                emptyList()
            }
            return CutPoint(
                cutIndex = index,
                isSplitTurn = turnPrefix.isNotEmpty(),
                turnPrefixMessages = turnPrefix,
            )
        }
        return null
    }

    private fun snapToUserBoundary(
        messageEntries: List<Pair<Int, SidePanelMessage>>,
        candidateIndex: Int,
        boundaryStartIndex: Int,
        keepRecentTokens: Int,
    ): Int? {
        for (index in candidateIndex downTo 0) {
            val (pathIndex, message) = messageEntries[index]
            if (pathIndex < boundaryStartIndex) break
            if (!isUserTurnStart(message)) continue
            val tailTokens = messageEntries
                .drop(index)
                .sumOf { (_, msg) -> ContextTokenCounter.countTokens(msg) }
            if (tailTokens >= keepRecentTokens) {
                return index
            }
        }
        return null
    }

    private fun isUserTurnStart(message: SidePanelMessage): Boolean =
        message.kind == SidePanelMessageKind.TEXT && message.role == ChatMessageRole.USER

    private fun extractFileOperations(messages: List<SidePanelMessage>): Pair<List<String>, List<String>> {
        val readFiles = mutableSetOf<String>()
        val modifiedFiles = mutableSetOf<String>()
        for (message in messages) {
            if (message.kind != SidePanelMessageKind.OUTPUT_STREAM) continue
            val command = message.content.lowercase()
            if (command.contains("read") || command.contains("cat ")) {
                readFiles.add(message.content)
            }
            if (command.contains("write") || command.contains("edit") || command.contains("patch")) {
                modifiedFiles.add(message.content)
            }
        }
        return readFiles.sorted() to modifiedFiles.sorted()
    }
}
