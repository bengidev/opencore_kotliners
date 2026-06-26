package io.github.bengidev.opencore.shared.providers

/** Registry (factory) for registered AI provider adapters. */
internal object ProviderRegistry {
    private val openRouter = ProviderOpenRouterAdapter()
    private val openCode = ProviderOpenAiCompatibleAdapter(descriptor = ProviderDescriptor.openCode)
    private val commandCode = ProviderOpenAiCompatibleAdapter(descriptor = ProviderDescriptor.commandCode)
    private val ollamaCloud = ProviderOpenAiCompatibleAdapter(descriptor = ProviderDescriptor.ollamaCloud)

    val defaultAdapter: ProviderAdapting = openRouter

    val all: List<ProviderAdapting> = listOf(openRouter, openCode, commandCode, ollamaCloud)

    fun resolve(id: String?): ProviderAdapting {
        if (id == null) return defaultAdapter
        return all.firstOrNull { it.descriptor.id == id } ?: defaultAdapter
    }

    val allDescriptors: List<ProviderDescriptor>
        get() = all.map { it.descriptor }
}
