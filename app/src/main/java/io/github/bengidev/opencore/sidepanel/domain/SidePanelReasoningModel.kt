package io.github.bengidev.opencore.sidepanel.domain

internal enum class SidePanelReasoningModel(val title: String) {
    Off("Off"),
    Low("Low"),
    Medium("Medium"),
    High("High");

    /** Provider `reasoning.effort` value, or null when reasoning is off. */
    val effort: String?
        get() = when (this) {
            Off -> null
            Low -> "low"
            Medium -> "medium"
            High -> "high"
        }

    companion object {
        fun fromWire(value: String?): SidePanelReasoningModel =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: High
    }
}
