package com.example.homebudget.utils.settings

import com.example.homebudget.data.dao.SettingsDao
import com.example.homebudget.data.entity.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * SettingsHelper
 *
 * Zestaw funkcji pomocniczych do pracy z danymi ustawień.
 *
 * Odpowiada za:
 * - parsowanie JSON
 * - normalizację danych
 * - uzupełnianie brakujących wartości
 *
 * Nie posiada stanu.
 */
object SettingsHelper {
    fun getCategories(settings: Settings): List<String> {
        return try {
            when (val element = Json.parseToJsonElement(settings.categories)) {
                is JsonArray -> {
                    // Format: ["Jedzenie","Transport"]
                    element.mapNotNull { it.jsonPrimitive.contentOrNull }
                        .filter { it.isNotBlank() }
                }
                is JsonObject -> {
                    // Format: {"Jedzenie":"#112233","Transport":"#445566"}
                    element.keys.toList()
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun normalizeCategoryName(input: String): String {
        val cleaned = input.trim().lowercase()
        if (cleaned.isBlank()) return ""
        return cleaned.replaceFirstChar { it.uppercase() }
    }

    fun getPeople(settings: Settings): List<String> {
        if (settings.peopleList.isBlank()) return emptyList()
        return try {
            when (val element = Json.parseToJsonElement(settings.peopleList)) {
                is JsonArray -> element.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull }
                is JsonObject -> {
                    // na wszelki wypadek obsługa formatu { "people": ["Ala","Jan"] }
                    val arr = element["people"] as? JsonArray ?: return emptyList()
                    arr.mapNotNull { it.jsonPrimitive.contentOrNull }
                }
                else -> emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getCategoryColors(settings: Settings): Map<String, String> {
        return try {
            val obj = Json.parseToJsonElement(settings.categoryColors).jsonObject
            obj.mapValues { it.value.jsonPrimitive.content }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    fun ensureCategoryColors(
        categories: List<String>,
        existing: Map<String, String>
        ): Map<String, String> {
        val result = existing.toMutableMap()
        categories.forEach { category ->
            if (!result.containsKey(category)) {
                result[category] = getRandomColorHex()
            }
        }
        return result
    }
    suspend fun ensureAndPersistCategoryColors(
        settings: Settings,
        settingsDao: SettingsDao
    ): Map<String, String> {
        val categories = getCategories(settings)
        val existing = getCategoryColors(settings)
        val ensured = ensureCategoryColors(categories, existing)
        if (ensured != existing) {
            settingsDao.update(
                settings.copy(categoryColors = Json.encodeToString(ensured))
            )
        }
        return ensured
    }

    private val palette = listOf(
        "#1B5E20","#2E7D32","#388E3C","#43A047","#4CAF50","#66BB6A","#81C784","#A5D6A7",
        "#0D47A1","#1565C0","#1976D2","#1E88E5","#2196F3","#42A5F5","#64B5F6","#90CAF9",
        "#E65100","#EF6C00","#F57C00","#FB8C00","#FF9800","#FFA726","#FFB74D","#FFE0B2",
        "#4A148C","#6A1B9A","#7B1FA2","#8E24AA","#9C27B0","#AB47BC","#BA68C8","#CE93D8",
        "#B71C1C","#C62828","#D32F2F","#E53935","#F44336","#EF5350","#E57373","#FFCDD2",
        "#263238","#37474F","#455A64","#546E7A","#607D8B","#78909C","#90A4AE","#CFD8DC"
    )

    private fun getRandomColorHex(): String = palette.random()
}