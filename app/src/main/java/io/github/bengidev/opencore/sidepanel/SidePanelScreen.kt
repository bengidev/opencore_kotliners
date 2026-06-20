package io.github.bengidev.opencore.sidepanel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.presenter.SidePanelDrawerContent
import io.github.bengidev.opencore.sidepanel.presenter.SidePanelSettingsSheet
import io.github.bengidev.opencore.sidepanel.theme.OpenCoreSidePanelTheme

@Composable
internal fun SidePanelDrawer(
    component: SidePanelComponent,
    darkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val state by component.state.subscribeAsState()
    OpenCoreSidePanelTheme(darkTheme = darkTheme) {
        SidePanelDrawerContent(
            state = state,
            onSessionTapped = component::onSessionTapped,
            onSettingsTapped = component::onSettingsTapped,
            modifier = modifier
        )
    }
}

@Composable
internal fun SidePanelSettingsSheetRoute(
    component: SidePanelComponent,
    darkTheme: Boolean
) {
    val state by component.state.subscribeAsState()
    if (state.isSettingsVisible) {
        OpenCoreSidePanelTheme(darkTheme = darkTheme) {
            SidePanelSettingsSheet(
                state = state,
                onProviderSelected = component::onProviderSelected,
                onApiKeyChanged = component::onApiKeyChanged,
                onSaveTapped = component::onSaveApiKeyTapped,
                onRemoveTapped = component::onRemoveApiKeyTapped,
                onDismiss = component::onSettingsDismissed
            )
        }
    }
}
