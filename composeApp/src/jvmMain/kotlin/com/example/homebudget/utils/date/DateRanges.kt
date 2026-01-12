package com.example.homebudget.utils.date

import java.time.LocalDate
import java.time.ZoneId

/**
 * DateRanges
 *
 * Zakresy czasu używane w aplikacji:
 * - początek / koniec miesiąca
 * - zakres miesiąca w millis
 */
object DateRanges {

    fun startOfMonth(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long =
        DateConverters.localDateToStartOfDayMillis(
            LocalDate.of(year, month, 1),
            zone
        )

    fun endOfMonth(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long =
        DateConverters.localDateToEndOfDayMillis(
            LocalDate.of(year, month, 1).withDayOfMonth(
                LocalDate.of(year, month, 1).lengthOfMonth()
            ),
            zone
        )

    fun monthRange(
        year: Int,
        month: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Pair<Long, Long> =
        startOfMonth(year, month, zone) to endOfMonth(year, month, zone)
}