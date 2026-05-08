package com.example.homebudget.viewmodel.billsplanner

import java.time.LocalDate

/**
 * AddBillUiState
 *
 * Stan UI formularza dodawania / edycji rachunku.
 * Przechowuje:
 * - dane formularza
 * - błędy walidacji
 * - informacje o trybie edycji
 * - flagi sterujące UI (loading, saved)
 */
data class AddBillUiState(
    val isEditMode: Boolean = false,
    val isLoading: Boolean = true,
    val isInitialized: Boolean = false,
    val markPaid: Boolean = false,
    val loadedExpenseId: Int? = null,

    val description: String = "",
    val amount: String = "",
    val note: String = "",

    val descriptionError: String? = null,
    val amountError: String? = null,
    val dateError: String? = null,
    val isValid: Boolean = false,

    val date: LocalDate = LocalDate.now(),
    val repeatIntervalIndex: Int = 0,

    val intervals: List<String> =
        listOf("1 miesiąc", "2 miesiące", "3 miesiące", "6 miesięcy", "12 miesięcy"),

    val error: String? = null,
    val saved: Boolean = false
)
