package io.github.bengidev.opencore.sidepanel

import com.arkivanov.decompose.ComponentContext
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.infrastructure.InMemorySidePanelHistoryRepository
import io.github.bengidev.opencore.shared.persistence.PersistenceConversationHistoryStoring

internal class SidePanelFacade {
    fun createComponent(
        componentContext: ComponentContext,
        history: PersistenceConversationHistoryStoring = InMemorySidePanelHistoryRepository(),
        setting: SidePanelSettingComponent,
    ): SidePanelComponent = SidePanelComponent(
        componentContext = componentContext,
        history = history,
        setting = setting,
    )
}
