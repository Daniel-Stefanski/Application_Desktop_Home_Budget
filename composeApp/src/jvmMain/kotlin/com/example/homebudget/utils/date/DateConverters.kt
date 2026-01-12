package com.example.homebudget.utils.date

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * DateConverters
 *
 * Jedno miejsce do konwersji dat:
 * - LocalDate ↔ millis
 * - początek / koniec dnia
 *
 * NIE zawiera logiki biznesowej.
 */
object DateConverters {

    fun localDateToStartOfDayMillis(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long =
        date
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

    fun localDateToEndOfDayMillis(
        date: LocalDate,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long =
        date
            .atTime(23, 59, 59)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    fun millisToLocalDate(
        millis: Long,
        zone: ZoneId = ZoneId.systemDefault()
    ): LocalDate =
        Instant
            .ofEpochMilli(millis)
            .atZone(zone)
            .toLocalDate()
}