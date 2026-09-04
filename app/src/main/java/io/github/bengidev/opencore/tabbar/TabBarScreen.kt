package io.github.bengidev.opencore.tabbar

import androidx.compose.runtime.Composable
import io.github.bengidev.opencore.tabbar.domain.HomeTab
import io.github.bengidev.opencore.tabbar.presenter.TabBarShell

@Composable
internal fun TabBarScreen(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    homeContent: @Composable () -> Unit,
    atomsContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    aboutContent: @Composable () -> Unit,
) {
    TabBarShell(
        selectedTab = selectedTab,
        onTabSelected = onTabSelected,
        homeContent = homeContent,
        atomsContent = atomsContent,
        settingsContent = settingsContent,
        aboutContent = aboutContent,
    )
}
