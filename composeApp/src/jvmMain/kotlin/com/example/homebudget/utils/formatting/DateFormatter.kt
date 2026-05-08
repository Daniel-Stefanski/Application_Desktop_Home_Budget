package com.example.homebudget.utils.formatting

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
/**
 * DateFormatter
 *
 * Jedno źródło prawdy do formatowania dat dla UI.
 *
 * UI-only helper:
 * - dd.MM.yyyy
 * - polskie nazwy miesięcy
 */
object DateFormatter {

    private val polishLocale = Locale.forLanguageTag("pl")

    private val dayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd.MM.yyyy", polishLocale)

    /**
     * Formatuje LocalDate do postaci dd.MM.yyyy
     */
    fun formatDate(date: LocalDate): String =
        dayFormatter.format(date)

    /**
     * Parsuje tekst dd.MM.yyyy do LocalDate
     *
     * @return LocalDate lub null jeśli niepoprawne
     */
    fun parseDate(text: String): LocalDate? =
        try {
            LocalDate.parse(text, dayFormatter)
        } catch (_: Exception) {
            null
        }

    /**
     * Zwraca nazwę miesiąca po polsku + rok
     * np. "Styczeń 2026"
     */
    fun formatMonth(year: Int, month: Int): String {
        val monthName = Month.of(month)
            .getDisplayName(TextStyle.FULL, polishLocale)
            .replaceFirstChar { it.uppercase() }

        return "$monthName $year"
    }
}

