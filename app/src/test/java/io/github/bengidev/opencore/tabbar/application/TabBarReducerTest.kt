package io.github.bengidev.opencore.tabbar.application

import io.github.bengidev.opencore.tabbar.domain.HomeTab
import org.junit.Assert.assertEquals
import org.junit.Test

class TabBarReducerTest {

    @Test
    fun selectingTab_updatesSelectedTab() {
        val result = TabBarReducer.reduce(
            TabBarState(selectedTab = HomeTab.HOME),
            TabBarIntent.TabSelected(HomeTab.SETTINGS),
        )

        assertEquals(HomeTab.SETTINGS, result.selectedTab)
    }

    @Test
    fun defaultTab_isHome() {
        assertEquals(HomeTab.HOME, TabBarState().selectedTab)
    }
}
