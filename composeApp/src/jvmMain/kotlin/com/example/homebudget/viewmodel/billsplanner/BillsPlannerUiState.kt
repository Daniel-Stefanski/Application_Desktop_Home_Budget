package com.example.homebudget.viewmodel.billsplanner

import com.example.homebudget.data.entity.Expense

/**
 * BillsPlannerUiState
 *
 * Stan UI planera rachunków.
 * Zawiera:
 * - listę rachunków
 * - podsumowania
 * - aktualne dialogi
 * - powiadomienia o terminach
 */
data class BillsPlannerUiState(
    val isLoading: Boolean = true,
    val bills: List<Expense> = emptyList(),
    val sortOption: SortOption = SortOption.DATE_ASC,
    val billToDelete: Expense? = null,
    val totalCount: Int = 0,
    val totalAmount: Double = 0.0,
    val error: String? = null,
    val notification: BillsNotification? = null
)

enum class SortOption {
    DATE_ASC,
    DATE_DESC,
    AMOUNT_ASC,
    AMOUNT_DESC
}

sealed class BillsNotification {
    data class DeadlineSoon(val title: String, val daysLeft: Long) : BillsNotification()
}