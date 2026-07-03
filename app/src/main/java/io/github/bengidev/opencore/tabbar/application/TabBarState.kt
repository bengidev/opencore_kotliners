package io.github.bengidev.opencore.tabbar.application

import io.github.bengidev.opencore.tabbar.domain.HomeTab

internal data class TabBarState(
    val selectedTab: HomeTab = HomeTab.HOME,
)
