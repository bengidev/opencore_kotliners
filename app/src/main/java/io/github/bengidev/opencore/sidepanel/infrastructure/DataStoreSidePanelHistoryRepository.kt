package io.github.bengidev.opencore.sidepanel.infrastructure

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring
import io.github.bengidev.opencore.sidepanel.domain.SidePanelConversation
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessageKind
import io.github.bengidev.opencore.sidepanel.domain.dedupeByMessageId
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.UUID

private data class HistorySnapshot(
    val conversations: LinkedHashMap<UUID, SidePanelConversation>,
    val messages: MutableMap<UUID, MutableList<SidePanelMessage>>
)

private val Context.sidePanelHistoryDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "sidepanel_history"
)

internal class DataStoreSidePanelHistoryRepository(
    private val context: Context
) : PersistenceConversationHistoryStoring {

    private val mutex = Mutex()
    private var conversations = linkedMapOf<UUID, SidePanelConversation>()
    private var messages = mutableMapOf<UUID, MutableList<SidePanelMessage>>()
    private var loaded = false

    override suspend fun listConversations(): List<SidePanelConversation> {
        ensureLoaded()
        return conversations.values
            .sortedWith(
                compareByDescending<SidePanelConversation> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
            .distinctBy { it.id }
    }

    override suspend fun loadMessages(conversationId: UUID): List<SidePanelMessage> {
        ensureLoaded()
        return messages[conversationId].orEmpty()
    }

    override suspend fun saveConversation(conversation: SidePanelConversation) {
        mutate {
            conversations[conversation.id] = conversation
        }
    }

    override suspend fun appendMessage(conversationId: UUID, message: SidePanelMessage) {
        mutate {
            val bucket = messages.getOrPut(conversationId) { mutableListOf() }
            val index = bucket.indexOfFirst { it.id == message.id }
            if (index >= 0) {
                bucket[index] = message
            } else {
                bucket += message
            }
            conversations[conversationId]?.let { existing ->
                conversations[conversationId] = existing.copy(updatedAt = message.createdAt)
            }
        }
    }

    override suspend fun deleteConversation(conversationId: UUID) {
        mutate {
            conversations.remove(conversationId)
            messages.remove(conversationId)
        }
    }

    override suspend fun setPinned(conversationId: UUID, isPinned: Boolean) {
        mutate {
            conversations[conversationId]?.let { existing ->
                conversations[conversationId] = existing.copy(isPinned = isPinned)
            }
        }
    }

    override suspend fun renameConversation(conversationId: UUID, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        mutate {
            conversations[conversationId]?.let { existing ->
                conversations[conversationId] = existing.copy(
                    title = trimmed,
                    updatedAt = Instant.now()
                )
            }
        }
    }

    override suspend fun setGroup(conversationId: UUID, groupName: String?) {
        val normalized = groupName?.trim()?.takeIf { it.isNotEmpty() }
        mutate {
            conversations[conversationId]?.let { existing ->
                conversations[conversationId] = existing.copy(groupName = normalized)
            }
        }
    }

    override suspend fun listGroups(): List<String> {
        ensureLoaded()
        return conversations.values.mapNotNull { it.groupName }.distinct().sorted()
    }

    private suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            val snapshot = readSnapshot()
            conversations = snapshot.conversations
            messages = snapshot.messages
            loaded = true
        }
    }

    private suspend fun mutate(block: () -> Unit) {
        mutex.withLock {
            if (!loaded) {
                val snapshot = readSnapshot()
                conversations = snapshot.conversations
                messages = snapshot.messages
                loaded = true
            }
            block()
            writeSnapshot(conversations, messages)
        }
    }

    private suspend fun readSnapshot(): HistorySnapshot {
        val json = context.sidePanelHistoryDataStore.data.map { preferences ->
            preferences[KEY_HISTORY]
        }.first()
        val decoded = decodeHistory(json)
        return HistorySnapshot(
            conversations = LinkedHashMap(decoded.conversations),
            messages = decoded.messages.mapValues { (_, bucket) -> bucket.toMutableList() }.toMutableMap()
        )
    }

    private suspend fun writeSnapshot(
        conversations: Map<UUID, SidePanelConversation>,
        messages: Map<UUID, List<SidePanelMessage>>
    ) {
        context.sidePanelHistoryDataStore.edit { preferences ->
            preferences[KEY_HISTORY] = encodeHistory(conversations, messages)
        }
    }

    companion object {
        private val KEY_HISTORY = stringPreferencesKey("sidepanel_history_v1")

        internal fun encodeHistory(
            conversations: Map<UUID, SidePanelConversation>,
            messages: Map<UUID, List<SidePanelMessage>>
        ): String {
            val root = JSONObject()
            val conversationArray = JSONArray()
            conversations.values.forEach { conversation ->
                conversationArray.put(
                    JSONObject()
                        .put("id", conversation.id.toString())
                        .put("title", conversation.title)
                        .put("createdAt", conversation.createdAt.toString())
                        .put("updatedAt", conversation.updatedAt.toString())
                        .put("isPinned", conversation.isPinned)
                        .put("groupName", conversation.groupName)
                )
            }
            root.put("conversations", conversationArray)

            val messagesObject = JSONObject()
            messages.forEach { (conversationId, bucket) ->
                val messageArray = JSONArray()
                bucket.forEach { message ->
                    messageArray.put(
                        JSONObject()
                            .put("id", message.id.toString())
                            .put("role", message.role)
                            .put("content", message.content)
                            .put("createdAt", message.createdAt.toString())
                            .put("kind", message.kind.wireValue)
                            .put("isComplete", message.isComplete)
                            .apply {
                                message.detailJson?.let { put("detailJson", it) }
                            }
                    )
                }
                messagesObject.put(conversationId.toString(), messageArray)
            }
            root.put("messages", messagesObject)
            return root.toString()
        }

        internal fun decodeHistory(json: String?): DecodedHistory {
            if (json.isNullOrBlank()) {
                return DecodedHistory(emptyMap(), emptyMap())
            }
            val root = JSONObject(json)
            val conversations = linkedMapOf<UUID, SidePanelConversation>()
            root.optJSONArray("conversations")?.let { array ->
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val conversation = SidePanelConversation(
                        id = UUID.fromString(item.getString("id")),
                        title = item.getString("title"),
                        createdAt = Instant.parse(item.getString("createdAt")),
                        updatedAt = Instant.parse(item.getString("updatedAt")),
                        isPinned = item.optBoolean("isPinned", false),
                        groupName = item.optString("groupName").takeIf { it.isNotEmpty() }
                    )
                    conversations[conversation.id] = conversation
                }
            }

            val messages = mutableMapOf<UUID, MutableList<SidePanelMessage>>()
            root.optJSONObject("messages")?.let { messagesObject ->
                messagesObject.keys().forEach { conversationId ->
                    val bucket = mutableListOf<SidePanelMessage>()
                    val messageArray = messagesObject.getJSONArray(conversationId)
                    for (index in 0 until messageArray.length()) {
                        val item = messageArray.getJSONObject(index)
                        bucket += SidePanelMessage(
                            id = UUID.fromString(item.getString("id")),
                            role = item.getString("role"),
                            content = item.getString("content"),
                            createdAt = Instant.parse(item.getString("createdAt")),
                            kind = SidePanelMessageKind.fromWire(item.optString("kind")),
                            isComplete = item.optBoolean("isComplete", true),
                            detailJson = item.optString("detailJson").takeIf { it.isNotEmpty() },
                        )
                    }
                    messages[UUID.fromString(conversationId)] = bucket.dedupeByMessageId().toMutableList()
                }
            }
            return DecodedHistory(conversations, messages)
        }
    }
}

internal data class DecodedHistory(
    val conversations: Map<UUID, SidePanelConversation>,
    val messages: Map<UUID, List<SidePanelMessage>>
)
