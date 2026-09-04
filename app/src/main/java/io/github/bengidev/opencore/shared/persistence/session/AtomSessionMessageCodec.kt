package io.github.bengidev.opencore.shared.persistence.session

import io.github.bengidev.opencore.chat.domain.ChatMessageRole
import io.github.bengidev.opencore.chat.domain.ChatOutputStreamStatus
import io.github.bengidev.opencore.chat.infrastructure.ChatOutputStreamDetailCodec
import io.github.bengidev.opencore.chat.infrastructure.ChatTextMessageDetailCodec
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

/** Encodes and decodes [SidePanelMessage] values for Room session entry payloads. */
internal object AtomSessionMessageCodec {
    private data class StoredMessage(
        val kindRaw: String,
        val role: String,
        val content: String,
        val isComplete: Boolean,
        val timestampEpochMillis: Long,
        val detailJson: String?,
    )

    fun encode(message: SidePanelMessage): ByteArray {
        val stored = storedMessage(message)
        return JSONObject()
            .put("kindRaw", stored.kindRaw)
            .put("role", stored.role)
            .put("content", stored.content)
            .put("isComplete", stored.isComplete)
            .put("timestampEpochMillis", stored.timestampEpochMillis)
            .put("detailJson", stored.detailJson)
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decode(data: ByteArray, messageId: UUID): SidePanelMessage {
        val json = JSONObject(String(data, Charsets.UTF_8))
        val kind = SidePanelMessageKind.fromWire(json.optString("kindRaw"))
        val role = json.optString("role", ChatMessageRole.ASSISTANT)
        val content = json.optString("content")
        val isComplete = json.optBoolean("isComplete", true)
        val timestamp = Instant.ofEpochMilli(json.optLong("timestampEpochMillis"))
        val detailJson = json.optString("detailJson").takeIf { it.isNotBlank() }

        return SidePanelMessage(
            id = messageId,
            role = role,
            content = content,
            createdAt = timestamp,
            kind = kind,
            isComplete = isComplete,
            detailJson = detailJson,
        )
    }

    fun encodeCompaction(checkpoint: AtomCompactionCheckpoint): ByteArray {
        return JSONObject()
            .put("summary", checkpoint.summary)
            .put("firstKeptEntryId", checkpoint.firstKeptEntryId.toString())
            .put("tokensBefore", checkpoint.tokensBefore)
            .put("readFiles", checkpoint.readFiles)
            .put("modifiedFiles", checkpoint.modifiedFiles)
            .toString()
            .toByteArray(Charsets.UTF_8)
    }

    fun decodeCompaction(data: ByteArray): AtomCompactionCheckpoint {
        val json = JSONObject(String(data, Charsets.UTF_8))
        return AtomCompactionCheckpoint(
            summary = json.getString("summary"),
            firstKeptEntryId = UUID.fromString(json.getString("firstKeptEntryId")),
            tokensBefore = json.getInt("tokensBefore"),
            readFiles = json.optJSONArray("readFiles")?.let { array ->
                List(array.length()) { index -> array.getString(index) }
            } ?: emptyList(),
            modifiedFiles = json.optJSONArray("modifiedFiles")?.let { array ->
                List(array.length()) { index -> array.getString(index) }
            } ?: emptyList(),
        )
    }

    private fun storedMessage(message: SidePanelMessage): StoredMessage =
        StoredMessage(
            kindRaw = message.kind.wireValue,
            role = message.role,
            content = message.content,
            isComplete = message.isComplete,
            timestampEpochMillis = message.createdAt.toEpochMilli(),
            detailJson = message.detailJson,
        )
}
