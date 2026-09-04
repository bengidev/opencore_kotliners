package io.github.bengidev.opencore.sidepanel.domain

import io.github.bengidev.opencore.shared.persistence.session.AtomCompactionCheckpoint

internal data class SettingsContextCompactionOutcome(
    val projectedMessages: List<SidePanelMessage>,
    val checkpoint: AtomCompactionCheckpoint?,
) {
    companion object {
        fun unchanged(messages: List<SidePanelMessage>): SettingsContextCompactionOutcome =
            SettingsContextCompactionOutcome(messages, null)
    }
}
