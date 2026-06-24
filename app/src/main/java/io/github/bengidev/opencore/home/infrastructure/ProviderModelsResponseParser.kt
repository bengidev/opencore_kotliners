package io.github.bengidev.opencore.home.infrastructure

import io.github.bengidev.opencore.chat.infrastructure.ChatJsonStringField
import io.github.bengidev.opencore.sidepanel.domain.SidePanelModel
import java.math.BigDecimal

internal object ProviderModelsResponseParser {
    fun parse(responseBody: String): List<SidePanelModel> {
        val dataStart = responseBody.indexOf("\"data\"")
        if (dataStart < 0) return emptyList()
        val arrayStart = responseBody.indexOf('[', dataStart)
        if (arrayStart < 0) return emptyList()

        return extractObjects(responseBody, arrayStart + 1)
            .mapNotNull(::parseEntry)
            .filter { entry ->
                val modality = entry.modality
                modality.isNullOrEmpty() || modality.contains("text")
            }
            .map { it.toSidePanelModel() }
            .sortedWith(compareByDescending<SidePanelModel> { it.isFree }.thenBy { it.displayTitle })
    }

    private data class ParsedEntry(
        val id: String,
        val name: String?,
        val contextLength: Int?,
        val modality: String?,
        val tokenizer: String?,
        val promptPrice: String?,
        val completionPrice: String?
    ) {
        val isFree: Boolean
            get() = when {
                promptPrice != null && completionPrice != null ->
                    isZeroPrice(promptPrice) && isZeroPrice(completionPrice)
                name != null -> name.contains("free", ignoreCase = true)
                else -> false
            }

        fun toSidePanelModel(): SidePanelModel = SidePanelModel(
            id = id,
            displayTitle = name ?: id,
            isFree = isFree,
            contextLength = contextLength,
            supportsReasoning = supportsReasoning(id, modality),
            supportsSpeedModes = tokenizer == "Router" || id == "openrouter/free"
        )
    }

    private fun parseEntry(objectJson: String): ParsedEntry? {
        val id = ChatJsonStringField.extract(objectJson, "id") ?: return null
        val pricingObject = extractNestedObject(objectJson, "pricing")
        val architectureObject = extractNestedObject(objectJson, "architecture")
        return ParsedEntry(
            id = id,
            name = ChatJsonStringField.extract(objectJson, "name"),
            contextLength = extractIntField(objectJson, "context_length"),
            modality = architectureObject?.let { ChatJsonStringField.extract(it, "modality") },
            tokenizer = architectureObject?.let { ChatJsonStringField.extract(it, "tokenizer") },
            promptPrice = pricingObject?.let { ChatJsonStringField.extract(it, "prompt") },
            completionPrice = pricingObject?.let { ChatJsonStringField.extract(it, "completion") }
        )
    }

    private fun supportsReasoning(id: String, modality: String?): Boolean {
        val reasoningIds = listOf(
            "deepseek-r1",
            "deepseek-r1-distill",
            "deepseek/deepseek-r1",
            "deepseek/deepseek-r1-distill",
            "openai/o1",
            "openai/o3",
            "openai/o1-mini",
            "openai/o3-mini",
            "qwen/qwq",
            "qwen/qvq",
            "deepseek-v4-pro",
            "deepseek-v4-flash",
            "kimi-k2.5",
            "kimi-k2.6"
        )
        if (reasoningIds.any { prefix -> id.startsWith(prefix) || id.contains(prefix) }) {
            return true
        }
        return modality?.contains("reasoning") == true
    }

    private fun extractObjects(json: String, startIndex: Int): List<String> {
        val objects = mutableListOf<String>()
        var index = startIndex
        while (index < json.length) {
            while (index < json.length && json[index].isWhitespace()) index++
            if (index >= json.length || json[index] == ']') break
            if (json[index] != '{') {
                index++
                continue
            }
            val end = findMatchingBrace(json, index) ?: break
            objects += json.substring(index, end + 1)
            index = end + 1
            while (index < json.length && json[index].isWhitespace()) index++
            if (index < json.length && json[index] == ',') index++
        }
        return objects
    }

    private fun extractNestedObject(json: String, key: String): String? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != '{') return null
        val end = findMatchingBrace(json, index) ?: return null
        return json.substring(index, end + 1)
    }

    private fun extractIntField(json: String, key: String): Int? {
        val keyToken = "\"$key\""
        val keyIndex = json.indexOf(keyToken)
        if (keyIndex < 0) return null
        var index = keyIndex + keyToken.length
        while (index < json.length && json[index].isWhitespace()) index++
        if (index >= json.length || json[index] != ':') return null
        index++
        while (index < json.length && json[index].isWhitespace()) index++
        val start = index
        while (index < json.length && (json[index].isDigit() || json[index] == '-')) index++
        if (start == index) return null
        return json.substring(start, index).toIntOrNull()
    }

    private fun isZeroPrice(value: String): Boolean =
        value.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) == 0

    private fun findMatchingBrace(json: String, openIndex: Int): Int? {
        var depth = 0
        var inString = false
        var escaped = false
        for (index in openIndex until json.length) {
            val char = json[index]
            if (inString) {
                if (escaped) {
                    escaped = false
                } else when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                continue
            }
            when (char) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }
}
