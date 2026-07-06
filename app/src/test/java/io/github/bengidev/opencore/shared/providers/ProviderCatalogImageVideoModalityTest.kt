package io.github.bengidev.opencore.shared.providers

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderCatalogImageVideoModalityTest {
    @Test
    fun catalogImageModality_enablesImageInputSupport() {
        val json = """
            {"id":"openai/gpt-4o","name":"GPT-4o","architecture":{"modality":"text+image"}}
        """.trimIndent()

        val model = ProviderCatalogParser.parseEntry(json)

        assertTrue(model.supportsImageInput)
        assertFalse(model.supportsVideoInput)
    }

    @Test
    fun catalogTextOnlyModality_disablesVisualInputSupport() {
        val json = """
            {"id":"meta-llama/llama-3.3-70b-instruct:free","name":"Llama 3.3 70B","architecture":{"modality":"text"}}
        """.trimIndent()

        val model = ProviderCatalogParser.parseEntry(json)

        assertFalse(model.supportsImageInput)
        assertFalse(model.supportsVideoInput)
    }

    @Test
    fun catalogInputModalities_enablesCapabilitiesWithoutLegacyModalityString() {
        val json = """
            {
              "id":"openai/gpt-4o",
              "name":"GPT-4o",
              "architecture": {
                "input_modalities": ["text", "image"]
              }
            }
        """.trimIndent()

        val model = ProviderCatalogParser.parseEntry(json)

        assertTrue(model.supportsImageInput)
        assertFalse(model.supportsVideoInput)
        assertFalse(model.supportsFileInput)
    }

    @Test
    fun catalogFileModality_enablesFileInputSupport() {
        val json = """
            {"id":"google/gemini-2.0-flash","name":"Gemini 2.0 Flash","architecture":{"modality":"text+image+file"}}
        """.trimIndent()

        val model = ProviderCatalogParser.parseEntry(json)

        assertTrue(model.supportsImageInput)
        assertTrue(model.supportsFileInput)
        assertFalse(model.supportsVideoInput)
    }
}
