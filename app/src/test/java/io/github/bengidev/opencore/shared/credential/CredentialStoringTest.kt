package io.github.bengidev.opencore.shared.credential

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialStoringTest {

    private val providerId = "openrouter"

    @Test
    fun saveAndSecret_roundTrip() {
        val store = CredentialInMemoryStore()

        store.save("sk-test", providerId)

        assertEquals("sk-test", store.secret(providerId))
    }

    @Test
    fun clear_removesSecret() {
        val store = CredentialInMemoryStore()
        store.save("sk-test", providerId)

        store.clear(providerId)

        assertNull(store.secret(providerId))
    }

    @Test
    fun secret_returnsNullForEmptyStoredValue() {
        val store = CredentialInMemoryStore()
        store.save("", providerId)

        assertNull(store.secret(providerId))
    }

    @Test
    fun clear_onAbsentSecret_doesNotThrow() {
        CredentialInMemoryStore().clear(providerId)
    }
}
