package com.example.homebudget.viewmodel.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.utils.date.DateRanges
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.time.LocalDate

/**
 * StatisticsViewModel
 *
 * ViewModel odpowiedzialny za:
 * - pobieranie danych statystycznych z bazy
 * - liczenie sum wydatków i budżetów
 * - obsługę zmiany roku
 * - obsługę wyboru miesiąca
 * - przygotowanie danych do wykresów
 *
 * Cała logika biznesowa statystyk znajduje się tutaj.
 */
@Suppress("DuplicatedCode")
class StatisticsViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val expenseDao = db.expenseDao()
    private val budgetDao = db.monthlyBudgetDao()
    private val settingsDao = db.settingsDao()

    private val _uiState =
        MutableStateFlow(
            StatisticsUiState(
                year = LocalDate.now().year
            )
        )
    val uiState = _uiState
    // Ładuje dane dla całego roku
    fun loadYear(year: Int = _uiState.value.year) {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch

            var yearSpent = 0.0
            var yearBudget = 0.0

            val monthly = mutableListOf<MonthlyStat>()

            for (month in 1..12) {
                val (start, end) = DateRanges.monthRange(year, month)

                val expenses =
                    expenseDao.getExpensesForUser(uid)
                        .filter { it.date in start..end }

                val spent = expenses.sumOf { it.amount }
                val budget =
                    budgetDao.getBudgetForMonth(uid, year, month)?.budget ?: 0.0

                yearSpent += spent
                yearBudget += budget

                monthly.add(
                    MonthlyStat(
                        monthIndex = month - 1,
                        spent = spent,
                        budget = budget
                    )
                )
            }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                year = year,
                yearSpent = yearSpent,
                yearBudget = yearBudget,
                monthlyStats = monthly,
                selectedMonthIndex = null,
                selectedMonthDetails = null,
                categoryStats = emptyList(),
                personStats = emptyList()
            )
        }
    }
    // Obsługa kliknięcia w miesiąc oraz pokazanie danych dla danego miesiąca
    fun selectMonth(monthIndex: Int) {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val year = _uiState.value.year
            val month = monthIndex + 1

            val (start, end) = DateRanges.monthRange(year, month)

            val expenses =
                expenseDao.getExpensesForUser(uid)
                    .filter { it.date in start..end }

            val spent = expenses.sumOf { it.amount }
            val budget =
                budgetDao.getBudgetForMonth(uid, year, month)?.budget ?: 0.0

            val details = MonthDetails(
                spent = spent,
                budget = budget,
                count = expenses.size,
                min = expenses.minOfOrNull { it.amount } ?: 0.0,
                max = expenses.maxOfOrNull { it.amount } ?: 0.0
            )

            _uiState.value = _uiState.value.copy(
                selectedMonthIndex = monthIndex,
                selectedMonthDetails = details,
                showMonthCharts = false,
                categoryStats = emptyList(),
                personStats = emptyList()
            )
        }
    }
    // Ładowanie danych do wykresów kategorii i osób
    fun loadMonthCharts() {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val monthIndex = _uiState.value.selectedMonthIndex ?: return@launch
            val year = _uiState.value.year
            val month = monthIndex + 1

            val (start, end) = DateRanges.monthRange(year, month)

            val categories = expenseDao.getSumByCategoryForPeriod(uid, start, end)

            val people = expenseDao.getSumByPersonForPeriod(uid, start, end)

            val settings = settingsDao.getSettingsForUser(uid)
            val colorJson = try {
                JSONObject(settings?.categoryColors ?: "{}")
            } catch (_: Exception) {
                JSONObject()
            }

            val categoryStats =
                categories.map { item ->
                    val categoryName = item.category?.trim().takeIf { !it.isNullOrBlank() } ?: "Inne"
                    val colorHex = colorJson.optString(categoryName)
                        .takeIf { it.isNotBlank() } ?: "#999999"
                    CategoryStat(
                        name = categoryName,
                        total = item.total ?: 0.0,
                        colorHex = colorHex
                    )
                }

            val personStats =
                people.map {
                    PersonStat(
                        name = it.person ?: "Nieznane",
                        total = it.total
                    )
                }

            _uiState.value = _uiState.value.copy(
                categoryStats = categoryStats,
                personStats = personStats,
                showMonthCharts = true
            )
        }
    }

    fun prevYear() {
        loadYear(_uiState.value.year - 1)
    }

    fun nextYear() {
        val current = LocalDate.now().year
        if (_uiState.value.year < current) {
            loadYear(_uiState.value.year + 1)
        }
    }
}