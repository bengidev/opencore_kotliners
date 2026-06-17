package io.github.bengidev.opencore.ui.decompose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.resume

@Composable
internal fun rememberComponentContext(): ComponentContext {
    val childLifecycle = remember { LifecycleRegistry() }
    DisposableEffect(childLifecycle) {
        childLifecycle.resume()
        onDispose { childLifecycle.destroy() }
    }

    return remember(childLifecycle) { DefaultComponentContext(lifecycle = childLifecycle) }
}
