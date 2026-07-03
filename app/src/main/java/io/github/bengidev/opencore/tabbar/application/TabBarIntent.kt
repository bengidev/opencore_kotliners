package io.github.bengidev.opencore.tabbar.application

import io.github.bengidev.opencore.tabbar.domain.HomeTab

internal sealed interface TabBarIntent {
    data class TabSelected(val tab: HomeTab) : TabBarIntent
}
