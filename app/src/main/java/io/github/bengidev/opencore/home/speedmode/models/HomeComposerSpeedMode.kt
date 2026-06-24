package io.github.bengidev.opencore.home.speedmode.models

/** Response speed preset for supported models. */
internal enum class HomeComposerSpeedMode {
    STANDARD,
    FAST,
    ;

    val title: String
        get() = when (this) {
            STANDARD -> "Standard"
            FAST -> "Fast"
        }

    /** OpenRouter `provider.sort.by` value, or `null` for default routing. */
    val providerSortBy: String?
        get() = when (this) {
            STANDARD -> null
            FAST -> "throughput"
        }
}
