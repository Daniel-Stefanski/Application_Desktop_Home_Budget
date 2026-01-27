package com.example.homebudget.viewmodel.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.dto.CategorySum
import com.example.homebudget.data.entity.MonthlyBudget
import com.example.homebudget.utils.date.DateRanges
import com.example.homebudget.utils.formatting.DateFormatter
import com.example.homebudget.utils.money.AmountParser
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.settings.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * DashboardViewModel
 *
 * Odpowiada za:
 * - pobieranie danych użytkownika
 * - obliczanie wydatków miesięcznych
 * - obsługę budżetu
 * - logikę ostrzeżeń o przekroczeniu budżetu
 */
class DashboardViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val userDao = db.userDao()
    private val expenseDao = db.expenseDao()
    private val monthlyBudgetDao = db.monthlyBudgetDao()
    private val settingsDao = db.settingsDao()

    // Jedno źródło prawdy dla dashboardu
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState

    init {
        val today = LocalDate.now()
        _uiState.value = _uiState.value.copy(
            selectedYear = today.year,
            selectedMonth = today.monthValue
        )
        loadData()
    }
    // Ręczne odświeżenie danych dashboardu
    fun reload() {
        loadData()
    }
    // Aktualizacja wartości budżetu wpisywanej przez użytkownika
    fun onBudgetChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            budgetInput = value,
            usePreviousBudget = false // ręczna zmiana wyłącza checkbox
        )
    }
    // Zapis nowego budżetu miesięcznego do bazy danych
    fun saveBudget() {
        viewModelScope.launch {
            try {
                val uid = Prefs.getLastLoggedUser() ?: return@launch
                val current = _uiState.value
                val amount = AmountParser.parse(current.budgetInput) ?: return@launch
                val newBudget = MonthlyBudget(
                    userId = uid,
                    year = current.selectedYear,
                    month = current.selectedMonth,
                    budget = amount,
                    isDefault = current.usePreviousBudget
                )
                monthlyBudgetDao.insertBudget(newBudget)
                // Odśwież dane
                loadData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    // Przejście do poprzedniego miesiąca
    fun prevMonth() {
        val current = _uiState.value
        var year = current.selectedYear
        var month = current.selectedMonth - 1
        if (month < 1) {
            month = 12
            year -= 1
        }
        _uiState.value = current.copy(selectedYear = year, selectedMonth = month)
        loadData()
    }
    // Przejście do następnego miesiaca (z blokadą przyszłych miesięcy)
    fun nextMonth() {
        val current = _uiState.value
        val today = LocalDate.now()

        // Nie pozwalamy wyjść w przyszłość
        val candidateYear: Int
        val candidateMonth: Int

        if (current.selectedYear == today.year && current.selectedMonth == today.monthValue) {
            return
        } else {
            var month = current.selectedMonth + 1
            var year = current.selectedYear
            if (month > 12) {
                month = 1
                year += 1
            }
            // zabezpieczenie przed „przeskoczeniem” za dzisiaj
            if (year > today.year || (year == today.year && month > today.monthValue)) {
                return
            }
            candidateYear = year
            candidateMonth = month
        }

        _uiState.value = current.copy(
            selectedYear = candidateYear,
            selectedMonth = candidateMonth
        )
        loadData()
    }
    /**
     * Ustawia stan błędu informujący o braku zalogowanego użytkownika
     * i kończy dalsze przetwarzanie.
     */
    private fun setNoUserError() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = "Brak zalogowanego użytkownika."
        )
    }
    /**
     * Główna metoda pobierająca dane dashboardu:
     * 1. Pobiera użytkownika
     * 2. Wyznacza zakres miesiąca
     * 3. Sumuje wydatki
     * 4. Pobiera budżet
     * 5. Sprawdza przekroczenie budżetu
     */
    private fun loadData() {
        viewModelScope.launch {
            try {
                // 1) Użytkownik – bierzemy ostatnio zalogowanego
                val userId = Prefs.getLastLoggedUser()
                if (userId == null) {
                    setNoUserError()
                    return@launch
                }

                val user = userDao.getUserById(userId)
                if (user == null) {
                    setNoUserError()
                    return@launch
                }
                val settings = settingsDao.getSettingsForUser(user.id)
                val categoryColors = if (settings != null) {
                    SettingsHelper.ensureAndPersistCategoryColors(settings, settingsDao)
                } else emptyMap()
                val year = _uiState.value.selectedYear
                val month = _uiState.value.selectedMonth

                val monthLabel = DateFormatter.formatMonth(year, month)

                val now = LocalDate.now()
                val isCurrentMonth = year == now.year && month == now.monthValue

                // 2) Zakres dat (początek i koniec miesiąca)
                val (startMillis, endMillis) = DateRanges.monthRange(year, month)

                // 3) Suma po kategoriach
                val sums: List<CategorySum> =
                    expenseDao.getSumByCategoryForPeriod(user.id, startMillis, endMillis)

                val categories = sums.map { sum ->
                    val name = sum.category?.takeIf { it.isNotBlank() } ?: "Inne"
                    val value = sum.total ?: 0.0
                    CategoryUi(name = name, amount = value)
                }.filter { it.amount > 0.0 }

                val totalSpent = categories.sumOf { it.amount }

                // 4) Budżet
                val budgetEntity = monthlyBudgetDao.getBudgetForMonth(user.id, year, month)
                val defaultBudget = getDefaultBudget(user.id, year, month)
                val budget = when {
                    budgetEntity != null -> budgetEntity.budget
                    isCurrentMonth && defaultBudget != null -> defaultBudget.budget
                    else -> 0.0
                }
                val useDefault = when {
                    budgetEntity != null -> budgetEntity.isDefault
                    defaultBudget != null -> true
                    else -> false
                }

                val percentUsed =
                    if (budget > 0.0) (totalSpent / budget * 100.0)
                    else 0.0
                val todayDate = LocalDate.now().toString()
                val lastWarningDate = Prefs.getLastBudgetWarningDate()
                val shouldShowWarning = isCurrentMonth && budget > 0.0 && totalSpent > budget && lastWarningDate != todayDate

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    userName = user.name,
                    monthLabel = monthLabel,
                    totalSpent = totalSpent,
                    budget = budget,
                    percentUsed = percentUsed,
                    categories = categories,
                    categoryColors = categoryColors,
                    isCurrentMonth = isCurrentMonth,
                    budgetInput = if (budget > 0) budget.toString() else "",
                    showBudgetExceededDialog = shouldShowWarning,
                    error = null,
                    canEditBudget = isCurrentMonth,
                    usePreviousBudget = useDefault,
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Nie udało się załadować danych dashboardu."
                )
            }
        }
    }
    private suspend fun getDefaultBudget(
        userId: Int,
        year: Int,
        month: Int
    ): MonthlyBudget? {
        var y = year
        var m = month - 1
        while (true) {
            if (m < 1) {
                m = 12
                y -= 1
            }
            val budget = monthlyBudgetDao.getBudgetForMonth(userId, y, m)
                ?: return null
            if (budget.isDefault) return budget
            m -= 1
        }
    }
    /**
     * Ukrywa dialog ostrzegawczy
     * i zapisuje datę w Prefs (1x dziennie)
     */
    fun dismissBudgetWarning() {
        Prefs.setLastBudgetWarningDate(LocalDate.now().toString())
        _uiState.value = _uiState.value.copy(showBudgetExceededDialog = false)
    }

    fun toggleUsePreviousBudget(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(usePreviousBudget = enabled)
    }
}

data class CategoryUi(
    val name: String,
    val amount: Double
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val monthLabel: String = "",
    val selectedYear: Int = LocalDate.now().year,
    val selectedMonth: Int = LocalDate.now().monthValue,
    val totalSpent: Double = 0.0,
    val budget: Double = 0.0,
    val percentUsed: Double = 0.0,
    val categories: List<CategoryUi> = emptyList(),
    val categoryColors: Map<String, String> = emptyMap(),
    val isCurrentMonth: Boolean = true,
    val error: String? = null,
    val budgetInput: String = "",
    val showBudgetExceededDialog: Boolean = false,
    val canEditBudget: Boolean = true, // blokada dla starych miesięcy
    val usePreviousBudget: Boolean = false
)