package io.github.bengidev.opencore.sidepanel.application

import io.github.bengidev.opencore.sidepanel.domain.SessionItem
import io.github.bengidev.opencore.sidepanel.domain.SessionProvider
import io.github.bengidev.opencore.sidepanel.domain.SessionSamples
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SidePanelReducerTest {
    @Test
    fun sessionsLoaded_updatesSessions() {
        val state = SidePanelState()
        val result = SidePanelReducer.reduce(state, SidePanelIntent.SessionsLoaded(SessionSamples.sessions))
        assertEquals(SessionSamples.sessions, result.sessions)
    }

    @Test
    fun sessionSelected_marksOnlyMatchingSessionActive() {
        val state = SidePanelState(sessions = SessionSamples.sessions)
        val result = SidePanelReducer.reduce(state, SidePanelIntent.SessionSelected("s2"))
        assertFalse(result.sessions.first { it.id == "s1" }.isActive)
        assertTrue(result.sessions.first { it.id == "s2" }.isActive)
    }

    @Test
    fun sessionRenamed_updatesTitle() {
        val state = SidePanelState(sessions = SessionSamples.sessions)
        val result = SidePanelReducer.reduce(state, SidePanelIntent.SessionRenamed("s1", "New title"))
        assertEquals("New title", result.sessions.first { it.id == "s1" }.title)
    }

    @Test
    fun sessionDeleted_removesSession() {
        val state = SidePanelState(sessions = SessionSamples.sessions)
        val result = SidePanelReducer.reduce(state, SidePanelIntent.SessionDeleted("s1"))
        assertFalse(result.sessions.any { it.id == "s1" })
    }

    @Test
    fun settingsTapped_setsVisibleTrue() {
        val result = SidePanelReducer.reduce(SidePanelState(), SidePanelIntent.SettingsTapped)
        assertTrue(result.isSettingsVisible)
    }

    @Test
    fun settingsDismissed_setsVisibleFalse() {
        val state = SidePanelState(isSettingsVisible = true)
        val result = SidePanelReducer.reduce(state, SidePanelIntent.SettingsDismissed)
        assertFalse(result.isSettingsVisible)
    }

    @Test
    fun providerSelected_updatesSelectedProvider() {
        val result = SidePanelReducer.reduce(SidePanelState(), SidePanelIntent.ProviderSelected(SessionProvider.Anthropic))
        assertEquals(SessionProvider.Anthropic, result.selectedProvider)
    }

    @Test
    fun apiKeyChanged_updatesInput() {
        val result = SidePanelReducer.reduce(SidePanelState(), SidePanelIntent.ApiKeyChanged("sk-test"))
        assertEquals("sk-test", result.apiKeyInput)
    }

    @Test
    fun apiKeyLoaded_populatesKeyAndInput() {
        val result = SidePanelReducer.reduce(SidePanelState(), SidePanelIntent.ApiKeyLoaded(SessionProvider.OpenRouter, "sk-loaded"))
        assertEquals("sk-loaded", result.storedApiKey)
        assertEquals("sk-loaded", result.apiKeyInput)
        assertTrue(result.isKeyStored)
    }

    @Test
    fun apiKeyLoaded_nullClearsKeyAndInput() {
        val state = SidePanelState(storedApiKey = "old", apiKeyInput = "old")
        val result = SidePanelReducer.reduce(state, SidePanelIntent.ApiKeyLoaded(SessionProvider.OpenRouter, null))
        assertNull(result.storedApiKey)
        assertEquals("", result.apiKeyInput)
        assertFalse(result.isKeyStored)
    }

    @Test
    fun apiKeyRemoved_clearsKeyForSelectedProvider() {
        val state = SidePanelState(selectedProvider = SessionProvider.OpenRouter, storedApiKey = "sk-old", apiKeyInput = "sk-old")
        val result = SidePanelReducer.reduce(state, SidePanelIntent.ApiKeyRemoved(SessionProvider.OpenRouter))
        assertNull(result.storedApiKey)
        assertEquals("", result.apiKeyInput)
    }
}
