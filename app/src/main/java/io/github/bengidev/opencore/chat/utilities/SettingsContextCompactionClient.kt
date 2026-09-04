package io.github.bengidev.opencore.chat.utilities

import io.github.bengidev.opencore.shared.persistence.session.AtomSessionEntry
import io.github.bengidev.opencore.sidepanel.domain.SettingsContextCompactionOutcome
import io.github.bengidev.opencore.sidepanel.domain.SidePanelMessage
import io.github.bengidev.opencore.sidepanel.infrastructure.SettingsContextCompactionEngine
import io.github.bengidev.opencore.sidepanel.infrastructure.SettingsContextCompactionPreferenceStore
import java.util.UUID

internal class SettingsContextCompactionClient(
    private val compactIfNeededFn: suspend (
        List<SidePanelMessage>,
        List<AtomSessionEntry>,
        UUID?,
        Int,
    ) -> SettingsContextCompactionOutcome,
    private val compactManuallyFn: suspend (
        List<SidePanelMessage>,
        List<AtomSessionEntry>,
        UUID?,
        Int,
    ) -> SettingsContextCompactionOutcome,
    private val compactForOverflowFn: suspend (
        List<SidePanelMessage>,
        List<AtomSessionEntry>,
        UUID?,
        Int,
    ) -> SettingsContextCompactionOutcome,
) {
    suspend fun compactIfNeeded(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
    ): SettingsContextCompactionOutcome = compactIfNeededFn(messages, sessionEntries, leafEntryId, contextLength)

    suspend fun compactManually(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
    ): SettingsContextCompactionOutcome = compactManuallyFn(messages, sessionEntries, leafEntryId, contextLength)

    suspend fun compactForOverflow(
        messages: List<SidePanelMessage>,
        sessionEntries: List<AtomSessionEntry>,
        leafEntryId: UUID?,
        contextLength: Int,
    ): SettingsContextCompactionOutcome = compactForOverflowFn(messages, sessionEntries, leafEntryId, contextLength)

    companion object {
        val disabled = SettingsContextCompactionClient(
            compactIfNeededFn = { messages, _, _, _ -> SettingsContextCompactionOutcome.unchanged(messages) },
            compactManuallyFn = { messages, _, _, _ -> SettingsContextCompactionOutcome.unchanged(messages) },
            compactForOverflowFn = { messages, _, _, _ -> SettingsContextCompactionOutcome.unchanged(messages) },
        )

        fun live(
            engine: SettingsContextCompactionEngine,
            preferenceStore: SettingsContextCompactionPreferenceStore,
        ): SettingsContextCompactionClient {
            return SettingsContextCompactionClient(
                compactIfNeededFn = { messages, sessionEntries, leafEntryId, contextLength ->
                    val preference = preferenceStore.preference()
                    engine.compactIfNeeded(messages, sessionEntries, leafEntryId, contextLength, preference)
                },
                compactManuallyFn = { messages, sessionEntries, leafEntryId, contextLength ->
                    val preference = preferenceStore.preference()
                    engine.compactManually(messages, sessionEntries, leafEntryId, contextLength, preference)
                },
                compactForOverflowFn = { messages, sessionEntries, leafEntryId, contextLength ->
                    val preference = preferenceStore.preference()
                    engine.compactForOverflow(messages, sessionEntries, leafEntryId, contextLength, preference)
                },
            )
        }
    }
}
