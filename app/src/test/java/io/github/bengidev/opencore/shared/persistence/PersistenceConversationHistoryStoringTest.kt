package io.github.bengidev.opencore.shared.persistence

import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistenceConversationHistoryStoringTest {

    @Test
    fun previewStore_listConversationsIsEmpty() = runTest {
        val store = PersistenceConversationHistoryStore.preview

        assertTrue(store.listConversations().isEmpty())
    }

    @Test
    fun dataStoreAdapter_delegatesToRepository() = runTest {
        val delegate = InMemorySidePanelHistoryRepository(seed = emptyList())
        val store = PersistenceConversationHistoryStore(delegate)

        assertTrue(store.listConversations().isEmpty())
    }
}
