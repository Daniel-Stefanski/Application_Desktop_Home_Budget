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
import java.time.ZoneId
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
        if (isPaidStatus(expense.status)) return
        viewModelScope.launch {
            expenseDao.updateStatus(expense.id, "opłacony")
            expenseDao.updateLastReset(expense.id, System.currentTimeMillis())
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
            SortOption.DATE_ASC -> list.sortedWith(
                compareBy<Expense> { isPaidStatus(it.status) }
                    .thenBy { it.date }
            )
            SortOption.DATE_DESC -> list.sortedWith(
                compareBy<Expense> { isPaidStatus(it.status) }
                    .thenByDescending { it.date }
            )
            SortOption.AMOUNT_ASC -> list.sortedWith(
                compareBy<Expense> { isPaidStatus(it.status) }
                    .thenBy { it.amount }
            )
            SortOption.AMOUNT_DESC -> list.sortedWith(
                compareBy<Expense> { isPaidStatus(it.status) }
                    .thenByDescending { it.amount }
            )
        }

    private fun checkBillNotifications(bills: List<Expense>) {
        val today = LocalDate.now()
        val thresholds = setOf(7L, 2L, 1L, 0L, -1L)
        for (bill in bills) {
            // Przypominamy tylko o nieopłaconych
            if (isPaidStatus(bill.status)) continue

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
    // Przesuwa rachunki cykliczne na kolejny termin po rozpoczęciu nowego cyklu
    fun resetPaidBillsIfNeeded() {
        val today = LocalDate.now()
        val twentyHours = 20 * 60 * 60 * 1000L

        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val bills = expenseDao.getRecurringExpenses(uid)
            bills
                .forEach { bill ->
                    val timeSinceLastReset = System.currentTimeMillis() - bill.lastReset
                    if (timeSinceLastReset < twentyHours) {
                        return@forEach
                    }

                    val billDate = DateConverters.millisToLocalDate(bill.date)
                    val monthsPassed = (today.year - billDate.year) * 12 +
                        (today.monthValue - billDate.monthValue)

                    if (monthsPassed >= bill.repeatInterval) {
                        val nextCycleDate = billDate.plusMonths(bill.repeatInterval.toLong())
                        expenseDao.updateExpenseDateAndStatus(
                            expenseId = bill.id,
                            newDate = DateConverters.localDateToStartOfDayMillis(
                                nextCycleDate,
                                ZoneId.systemDefault()
                            ),
                            newStatus = "nieopłacony"
                        )
                        expenseDao.updateLastReset(bill.id, System.currentTimeMillis())
                    }
                }
            loadBills()
        }
    }
    private fun isPaidStatus(status: String): Boolean =
        status.trim().lowercase().startsWith("op")
}
