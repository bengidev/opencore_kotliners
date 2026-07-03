package io.github.bengidev.opencore.sidepanel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.sidepanel.application.SidePanelComponent
import io.github.bengidev.opencore.sidepanel.presenter.SidePanelSettingContent

@Composable
internal fun SidePanelSettingsScreen(
    component: SidePanelComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.settingState.subscribeAsState()

    LaunchedEffect(Unit) { component.setting.onAppear() }

    SidePanelSettingContent(
        state = state,
        onDismiss = null,
        onProviderSelected = component.setting::selectProvider,
        onDraftChanged = component.setting::onDraftChanged,
        onSave = component.setting::save,
        onClear = component.setting::clear,
        modifier = modifier
            .fillMaxSize()
            .background(HomeTheme.palette.surfaceBase)
            .testTag("settings-view"),
    )
}
