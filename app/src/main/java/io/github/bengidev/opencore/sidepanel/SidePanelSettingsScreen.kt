package io.github.bengidev.opencore.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.setting.SidePanelSettingComponent
import io.github.bengidev.opencore.sidepanel.presenter.SidePanelSettingContent

@Composable
internal fun SidePanelSettingsScreen(
    component: SidePanelSettingComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.subscribeAsState()

    LaunchedEffect(Unit) { component.onAppear() }

    SidePanelSettingContent(
        state = state,
        onDismiss = null,
        onProviderSelected = component::selectProvider,
        onDraftChanged = component::onDraftChanged,
        onSave = component::save,
        onClear = component::clear,
        modifier = modifier
            .fillMaxSize()
            .background(HomeTheme.palette.surfaceBase)
            .statusBarsPadding()
            .testTag("settings-view"),
    )
}
