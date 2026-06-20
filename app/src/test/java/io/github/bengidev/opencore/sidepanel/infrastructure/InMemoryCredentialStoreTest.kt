package io.github.bengidev.opencore.sidepanel.infrastructure

import io.github.bengidev.opencore.sidepanel.domain.SessionProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryCredentialStoreTest {
    @Test
    fun saveAndLoadApiKey_roundTrips() = runTest {
        val store = InMemoryCredentialStore()
        store.saveApiKey(SessionProvider.OpenRouter, "sk-test")
        assertEquals("sk-test", store.loadApiKey(SessionProvider.OpenRouter))
    }

    @Test
    fun loadApiKey_returnsNullWhenAbsent() = runTest {
        val store = InMemoryCredentialStore()
        assertNull(store.loadApiKey(SessionProvider.OpenRouter))
    }

    @Test
    fun removeApiKey_clearsKey() = runTest {
        val store = InMemoryCredentialStore()
        store.saveApiKey(SessionProvider.OpenRouter, "sk-test")
        store.removeApiKey(SessionProvider.OpenRouter)
        assertNull(store.loadApiKey(SessionProvider.OpenRouter))
    }

    @Test
    fun keysArePerProvider() = runTest {
        val store = InMemoryCredentialStore()
        store.saveApiKey(SessionProvider.OpenRouter, "sk-or")
        store.saveApiKey(SessionProvider.Anthropic, "sk-an")
        assertEquals("sk-or", store.loadApiKey(SessionProvider.OpenRouter))
        assertEquals("sk-an", store.loadApiKey(SessionProvider.Anthropic))
        assertNull(store.loadApiKey(SessionProvider.OpenAI))
    }
}
