package com.example.homebudget.viewmodel.billsplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.utils.date.DateConverters
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.collections.sumOf

/**
 * BillsPlannerViewModel
 *
 * ViewModel planera rachunków.
 * Odpowiada za:
 * - pobieranie rachunków cyklicznych
 * - sortowanie i podsumowania
 * - oznaczanie rachunków jako opłacone
 * - reset statusu po kolejnym cyklu
 * - generowanie powiadomień o terminach
 */
class BillsPlannerViewModel : ViewModel() {
    private val db = AppDatabase.getDatabase()
    private val expenseDao = db.expenseDao()
    private val _uiState = MutableStateFlow(BillsPlannerUiState())
    val uiState = _uiState

    fun loadBills() {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch

            val bills = expenseDao.getRecurringExpenses(uid)

            val sorted = sortBills(bills, _uiState.value.sortOption)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                bills = sorted,
                totalCount = sorted.size,
                totalAmount = sorted.sumOf { it.amount }
            )

            checkBillNotifications(sorted)
        }
    }

    fun toggleStatus(expense: Expense) {
        // Jeśli już opłacony -> ignorujemy klik
        if (expense.status == "opłacony") return
        viewModelScope.launch {
            expenseDao.updateStatus(expense.id, "opłacony")
            loadBills()
        }
    }

    fun requestDelete(expense: Expense) {
        _uiState.value = _uiState.value.copy(billToDelete = expense)
    }

    fun confirmDelete() {
        viewModelScope.launch {
            _uiState.value.billToDelete?.let {
                expenseDao.unsetRecurring(it.id)
            }
            _uiState.value = _uiState.value.copy(billToDelete = null)
            loadBills()
        }
    }

    fun cancelDelete() {
        _uiState.value = _uiState.value.copy(billToDelete = null)
    }

    private fun sortBills(
        list: List<Expense>,
        option: SortOption
    ): List<Expense> =
        when (option) {
            SortOption.DATE_ASC -> list.sortedBy { it.date }
            SortOption.DATE_DESC -> list.sortedByDescending { it.date }
            SortOption.AMOUNT_ASC -> list.sortedBy { it.amount }
            SortOption.AMOUNT_DESC -> list.sortedByDescending { it.amount }
        }

    private fun checkBillNotifications(bills: List<Expense>) {
        val today = LocalDate.now()
        val thresholds = setOf(7L, 2L, 1L, 0L, -1L)
        for (bill in bills) {
            // Przypominamy tylko o nieopłaconych
            if (bill.status == "opłacony") continue

            // (opcjonalnie) jeśli chcesz tylko stricte rachunki:
            // if (bill.category != "Rachunki") continue

            val billDate = DateConverters.millisToLocalDate(bill.date)

            val daysLeft = ChronoUnit.DAYS.between(today, billDate)
            if (daysLeft in thresholds) {
                // Klucz zawiera tez bill.date, więc przy kolejnym cyklu (gdy data się zmieni)
                // Ostrzeżenia znów będą mogły się pojawić

                val key = "${bill.id}_${bill.date}_$daysLeft"
                if (!Prefs.wasBillDeadlineWarningShown(key)) {
                    Prefs.markBillDeadlineWarningShown(key)
                    _uiState.value = _uiState.value.copy(
                        notification = BillsNotification.DeadlineSoon(
                            title = bill.description ?: "Rachunki",
                            daysLeft = daysLeft
                        )
                    )
                    return
                }
            }
        }
    }
    fun clearNotifications() {
        _uiState.value = _uiState.value.copy(notification = null)
    }
    // Resetuje status opłaconych rachunków po rozpoczęciu nowego cyklu
    fun resetPaidBillsIfNeeded() {
        val today = LocalDate.now()

        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val bills = expenseDao.getRecurringExpenses(uid)
            bills
                .filter { it.status == "opłacony" }
                .forEach { bill ->
                    val billDate = DateConverters.millisToLocalDate(bill.date)
                    val nextCycleDate = billDate.plusMonths(bill.repeatInterval.toLong())
                    if (!nextCycleDate.isAfter(today)) {
                        expenseDao.updateExpenseDateAndStatus(
                            expenseId = bill.id,
                            newDate = DateConverters.localDateToStartOfDayMillis(nextCycleDate),
                            newStatus = "nieopłacony"
                        )
                    }
                }
            loadBills()
        }
    }
}