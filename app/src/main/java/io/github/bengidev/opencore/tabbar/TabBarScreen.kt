package io.github.bengidev.opencore.tabbar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import io.github.bengidev.opencore.tabbar.application.TabBarComponent
import io.github.bengidev.opencore.tabbar.presenter.TabBarShell

@Composable
internal fun TabBarScreen(
    component: TabBarComponent,
    homeContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    aboutContent: @Composable () -> Unit,
) {
    val state by component.state.subscribeAsState()

    TabBarShell(
        state = state,
        onTabSelected = component::selectTab,
        homeContent = homeContent,
        settingsContent = settingsContent,
        aboutContent = aboutContent,
    )
}
