package io.github.bengidev.opencore.sidepanel

import androidx.compose.runtime.Composable
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.presenter.SidePanelView

@Composable
internal fun SidePanelScreen(
    component: SidePanelComponent
) {
    SidePanelView(component = component)
}
