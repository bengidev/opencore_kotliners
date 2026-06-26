package io.github.bengidev.opencore.shared.providers

/** A reasoning-effort option for a model, sourced from the provider catalog. */
internal data class ModelReasoningEffort(
    val wireValue: String?
) {
    val id: String get() = wireValue ?: "off"

    val title: String
        get() = wireValue?.let(::displayTitle) ?: "Off"

    val requestEffort: String? get() = wireValue

    companion object {
        val off = ModelReasoningEffort(wireValue = null)

        fun displayTitle(wireValue: String): String = when (wireValue) {
            "none" -> "None"
            "minimal" -> "Minimal"
            "xhigh" -> "Extra High"
            "max" -> "Max"
            else -> wireValue.replaceFirstChar { it.uppercase() }
        }

        fun catalogOptions(
            wireEfforts: List<String>,
            reasoningMandatory: Boolean
        ): List<ModelReasoningEffort> {
            if (wireEfforts.isEmpty()) return emptyList()
            val options = wireEfforts.map { ModelReasoningEffort(it) }.toMutableList()
            if (!reasoningMandatory && "none" !in wireEfforts) {
                options += off
            }
            return options
        }

        fun resolvedSelection(
            storedWireValue: String?,
            available: List<ModelReasoningEffort>
        ): ModelReasoningEffort {
            if (available.isEmpty()) return off
            storedWireValue?.let { stored ->
                available.firstOrNull { it.wireValue == stored }?.let { return it }
            }
            if (storedWireValue == null && available.any { it.wireValue == null }) {
                return off
            }
            available.firstOrNull { it.wireValue == "high" }?.let { return it }
            return available.firstOrNull { it.wireValue != null } ?: available.first()
        }
    }
}
