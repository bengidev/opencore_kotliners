package io.github.bengidev.opencore.sidepanel.domain

import androidx.compose.runtime.Immutable

@Immutable
internal data class SessionItem(
    val id: String,
    val title: String,
    val preview: String,
    val isActive: Boolean = false
)
