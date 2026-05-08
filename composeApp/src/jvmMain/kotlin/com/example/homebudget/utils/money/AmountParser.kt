package com.example.homebudget.utils.money

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * AmountParser
 *
 * Jedno źródło prawdy do parsowania kwot pieniężnych.
 *
 * Obsługuje:
 * - spacje: "1 200"
 * - NBSP
 * - przecinki: "12,50"
 *
 * Zasady:
 * - separator dziesiętny: ,
 * - brak separatorów tysięcy
 * - max 2 miejsca po przecinku
 * - brak kropki
 * NIE rzuca wyjątków.
 */
object AmountParser {
    private val symbols = DecimalFormatSymbols(Locale.forLanguageTag("pl-PL")).apply {
        groupingSeparator = ' '
        decimalSeparator = ','
    }
    private val formatter = DecimalFormat("#,##0.00", symbols).apply {
        roundingMode = RoundingMode.HALF_UP
        isParseBigDecimal = true
    }
    /**
     * Sanitizacja INPUTU (pisanie w polu)
     * - blokuje kropki
     * - zostawia cyfry, spacje i jeden przecinek
     * - max 2 cyfry po przecinku
     */
    fun sanitize(input: String): String {
        val noDots = input.replace(".", "")
        val filtered = noDots.replace(Regex("[^0-9,\\s\\u00A0]"), "")
            .replace('\u00A0', ' ')
        val parts = filtered.split(",")
        val integerPart = parts.firstOrNull()
            ?.replace(Regex("\\s+"), " ")
            ?.trimStart()
            ?: ""
        return when {
            parts.size == 1 -> integerPart
            integerPart.isBlank() -> integerPart
            parts.size >= 2 -> integerPart + "," + parts.drop(1).joinToString("").filter { it.isDigit() }.take(2)
            else -> ""
        }
    }
    /**
     * Formatowanie do postaci XX,YY
     * Używane po zapisie / przy edycji
     */
    fun format(input: String): String {
        val value = parse(input) ?: return input
        return formatter.format(value)
    }
    /**
     * Parsuje tekst na Double.
     *
     * @return Double lub null jeśli niepoprawne
     */
    fun parse(text: String): Double? {
        if (text.isBlank()) return null

        return try {
            val normalized = text
                .replace(" ", "")
                .replace("\u00A0", "")
                .replace(",", ".")
                .replace(Regex("[^0-9.]"), "")
            if (normalized.isBlank()) return null
            BigDecimal(normalized).toDouble()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sprawdza czy kwota jest poprawna (> 0).
     */
    fun isValid(text: String): Boolean {
        val value = parse(text)
        return value != null && value > 0.0
    }
}
