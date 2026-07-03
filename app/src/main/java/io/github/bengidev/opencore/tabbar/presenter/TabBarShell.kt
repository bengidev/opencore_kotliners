package io.github.bengidev.opencore.tabbar.presenter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import io.github.bengidev.opencore.home.theme.HomeTheme
import io.github.bengidev.opencore.tabbar.domain.HomeTab

@Composable
internal fun TabBarShell(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
    homeContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    aboutContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HomeTheme.palette.surfaceBase,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (!isImeVisible) {
                TabNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selectedTab) {
                HomeTab.HOME -> homeContent()
                HomeTab.SETTINGS -> settingsContent()
                HomeTab.ABOUT -> aboutContent()
            }
        }
    }
}

@Composable
private fun TabNavigationBar(
    selectedTab: HomeTab,
    onTabSelected: (HomeTab) -> Unit,
) {
    val palette = HomeTheme.palette

    NavigationBar(
        containerColor = palette.surfaceRaised,
        contentColor = palette.textPrimary,
    ) {
        HomeTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = null,
                    )
                },
                label = { Text(tab.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = palette.controlStrongText,
                    selectedTextColor = palette.textPrimary,
                    indicatorColor = palette.controlStrong,
                    unselectedIconColor = palette.textTertiary,
                    unselectedTextColor = palette.textSecondary,
                ),
            )
        }
    }
}

private val HomeTab.icon: ImageVector
    get() = when (this) {
        HomeTab.HOME -> Icons.Default.Home
        HomeTab.SETTINGS -> Icons.Default.Settings
        HomeTab.ABOUT -> Icons.Default.Info
    }
