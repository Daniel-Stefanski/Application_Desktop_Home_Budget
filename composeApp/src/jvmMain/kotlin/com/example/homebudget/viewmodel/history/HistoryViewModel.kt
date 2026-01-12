package com.example.homebudget.viewmodel.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.utils.date.DateConverters
import com.example.homebudget.utils.formatting.DateFormatter
import com.example.homebudget.utils.money.AmountParser
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class HistoryListItem {
    data class Header(val date: String) : HistoryListItem()
    data class ExpenseItem(val expense: Expense) : HistoryListItem()
}

data class HistoryUiState(
    val isLoading: Boolean = true,
    val error: String? = null,

    val items: List<HistoryListItem> = emptyList(),

    val sortOptions: List<String> = listOf(
        "Od najnowszych",
        "Od najstarszych",
        "Kwota rosnąco",
        "Kwota malejąco"
    ),
    val selectedSortIndex: Int = 0,

    val searchQuery: String = "",

    // Filtr zaawansowany (tekstowo, jak w dialogu)
    val startDateText: String = "",
    val endDateText: String = "",
    val minAmountText: String = "",
    val maxAmountText: String = "",
    val categories: List<String> = listOf("Wszystkie"),
    val selectedCategoryIndex: Int = 0,
    val paymentMethods: List<String> = listOf("Wszystkie", "Gotówka", "Karta", "Blik", "Przelew"),
    val selectedPaymentIndex: Int = 0,
    val onlyRecurring: Boolean = false,

    val showAdvancedDialog: Boolean = false
)

/**
 * HistoryViewModel
 *
 * Logika biznesowa ekranu historii wydatków.
 *
 * Odpowiada za:
 * - pobieranie danych z bazy
 * - filtrowanie i sortowanie wydatków
 * - grupowanie wydatków po dacie
 */

class HistoryViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val expenseDao = db.expenseDao()

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState

    private var allExpenses: List<Expense> = emptyList()

    init {
        reload()
    }
    /**
     * Pobiera wydatki zalogowanego użytkownika z bazy danych
     * i inicjalizuje listę kategorii.
     */
    fun reload() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val id = Prefs.getLastLoggedUser()
                if (id == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Brak zalogowanego użytkownika."
                    )
                    return@launch
                }

                allExpenses = expenseDao.getExpensesForUser(id)

                val categories = listOf("Wszystkie") +
                        allExpenses.map { it.category }
                            .filter { it.isNotBlank() }
                            .distinct()
                            .sorted()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = null,
                    categories = categories
                )

                applyFilters()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Nie udało się załadować historii wydatków."
                )
            }
        }
    }

    // Handlery prostych filtrów
    fun onSearchQueryChange(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
        applyFilters()
    }
    fun onSortSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedSortIndex = index)
        applyFilters()
    }
    fun setAdvancedDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showAdvancedDialog = visible)
    }
    fun applyAdvancedFilter(
        startDate: String,
        endDate: String,
        minAmount: String,
        maxAmount: String,
        categoryIndex: Int,
        paymentIndex: Int,
        onlyRecurring: Boolean
    ) {
        _uiState.value = _uiState.value.copy(
            startDateText = startDate,
            endDateText = endDate,
            minAmountText = minAmount,
            maxAmountText = maxAmount,
            selectedCategoryIndex = categoryIndex,
            selectedPaymentIndex = paymentIndex,
            onlyRecurring = onlyRecurring
        )
        applyFilters()
    }
    fun clearAdvancedFilter() {
        _uiState.value = _uiState.value.copy(
            startDateText = "",
            endDateText = "",
            minAmountText = "",
            maxAmountText = "",
            selectedCategoryIndex = 0,
            selectedPaymentIndex = 0,
            onlyRecurring = false
        )
        applyFilters()
    }
    /**
     * Główna funkcja filtrowania wydatków.
     *
     * Odpowiada za:
     * - zastosowanie wszystkich filtrów
     * - sortowanie wyników
     * - grupowanie po dacie
     */
    private fun applyFilters() {
        val state = _uiState.value

        var filtered = allExpenses

        // Kategoria
        val selectedCategory =
            state.categories.getOrNull(state.selectedCategoryIndex) ?: "Wszystkie"
        if (selectedCategory != "Wszystkie") {
            filtered = filtered.filter { it.category == selectedCategory }
        }

        // Szukanie po opisie / notatce
        if (state.searchQuery.isNotBlank()) {
            val query = state.searchQuery.trim()
            filtered = filtered.filter {
                (it.description?.contains(query, ignoreCase = true) == true) ||
                        (it.note?.contains(query, ignoreCase = true) == true)
            }
        }

        // Zakres dat
        val startMillis = parseDateToStartMillis(state.startDateText)
        val endMillis = parseDateToEndMillis(state.endDateText)

        if (startMillis != null || endMillis != null) {
            filtered = filtered.filter { exp ->
                val d = exp.date
                val afterStart = startMillis?.let { d >= it } ?: true
                val beforeEnd = endMillis?.let { d <= it } ?: true
                afterStart && beforeEnd
            }
        }

        // Zakres kwoty
        val minAmount = AmountParser.parse(state.minAmountText)
        val maxAmount = AmountParser.parse(state.maxAmountText)
        if (minAmount != null) {
            filtered = filtered.filter { it.amount >= minAmount }
        }
        if (maxAmount != null) {
            filtered = filtered.filter { it.amount <= maxAmount }
        }

        // Metoda płatności
        val selectedPayment =
            state.paymentMethods.getOrNull(state.selectedPaymentIndex) ?: "Wszystkie"
        if (selectedPayment != "Wszystkie") {
            filtered = filtered.filter { it.paymentMethod == selectedPayment }
        }

        // Tylko powtarzalne
        if (state.onlyRecurring) {
            filtered = filtered.filter { it.isRecurring }
        }

        // Sortowanie
        filtered = when (state.selectedSortIndex) {
            0 -> filtered.sortedByDescending { it.date }      // od najnowszych
            1 -> filtered.sortedBy { it.date }                // od najstarszych
            2 -> filtered.sortedBy { it.amount }              // kwota rosnąco
            3 -> filtered.sortedByDescending { it.amount }    // kwota malejąco
            else -> filtered
        }

        val groupedItems = groupByDate(filtered)

        _uiState.value = state.copy(items = groupedItems)
    }
    /**
     * Grupuje listę wydatków według daty
     * i tworzy listę nagłówków + elementów.
     */
    private fun groupByDate(
        expenses: List<Expense>
    ): List<HistoryListItem> {
        val result = mutableListOf<HistoryListItem>()
        var lastLabel: String? = null

        for (exp in expenses) {
            val date = DateConverters.millisToLocalDate(exp.date)
            val label = DateFormatter.formatDate(date)

            if (label != lastLabel) {
                result.add(HistoryListItem.Header(label))
                lastLabel = label
            }
            result.add(HistoryListItem.ExpenseItem(exp))
        }
        return result
    }
    /**
     * Parsuje tekstową datę na timestamp (początek dnia).
     */
    private fun parseDateToStartMillis(
        text: String
    ): Long? {
        if (text.isBlank()) return null
        val date = DateFormatter.parseDate(text) ?: return null
        return DateConverters.localDateToStartOfDayMillis(date)
    }
    /**
     * Parsuje tekstową datę na timestamp (koniec dnia).
     */
    private fun parseDateToEndMillis(
        text: String
    ): Long? {
        if (text.isBlank()) return null
        val date = DateFormatter.parseDate(text) ?: return null
        return DateConverters.localDateToEndOfDayMillis(date)
    }
}
