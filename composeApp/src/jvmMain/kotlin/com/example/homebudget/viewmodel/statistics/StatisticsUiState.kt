package com.example.homebudget.viewmodel.statistics

/**
 * StatisticsUiState
 *
 * Stan UI dla ekranu statystyk.
 * Przechowuje:
 * - dane roczne
 * - dane miesięczne
 * - szczegóły wybranego miesiąca
 * - dane do wykresów
 * - flagi sterujące UI
 */
data class StatisticsUiState(
    val isLoading: Boolean = true,

    val year: Int,
    val yearSpent: Double = 0.0,
    val yearBudget: Double = 0.0,

    val monthlyStats: List<MonthlyStat> = emptyList(),

    val selectedMonthIndex: Int? = null,
    val selectedMonthDetails: MonthDetails? = null,

    val categoryStats: List<CategoryStat> = emptyList(),
    val personStats: List<PersonStat> = emptyList(),

    val showMonthCharts: Boolean = false,

    val error: String? = null
)
// Podsumowanie jednego miesiąca (do wykresu rocznego)
data class MonthlyStat(
    val monthIndex: Int, // 0–11
    val spent: Double,
    val budget: Double
)
// Szczegóły wybranego miesiąca (do sekcji "Szczegóły miesiąca")
data class MonthDetails(
    val spent: Double,
    val budget: Double,
    val count: Int,
    val min: Double,
    val max: Double
)
// Podsumowanie miesiąca wykresem kategorii
data class CategoryStat(
    val name: String,
    val total: Double,
    val colorHex: String
)
// Podsumowanie miesiąca wykresem osób
data class PersonStat(
    val name: String,
    val total: Double
)