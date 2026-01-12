package com.example.homebudget.viewmodel.billsplanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.utils.date.DateConverters
import com.example.homebudget.utils.money.AmountParser
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * AddBillViewModel
 *
 * ViewModel odpowiedzialny za:
 * - wczytywanie danych rachunku do edycji
 * - walidację formularza
 * - zapis nowego rachunku lub aktualizację istniejącego
 * - obsługę cykliczności rachunków
 *
 * Nie zawiera kodu UI – tylko logikę biznesową.
 */
class AddBillViewModel(
    private val expenseId: Int? = null
) : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val expenseDao = db.expenseDao()

    private val _uiState =
        MutableStateFlow(AddBillUiState())
    val uiState = _uiState
    // Wczytuje dane rachunku (jeśli edycja) lub inicjalizuje pusty formularz
    fun load() {
        val currentId = expenseId
        if (_uiState.value.isInitialized && _uiState.value.loadedExpenseId == currentId) return

        viewModelScope.launch {
            Prefs.getLastLoggedUser() ?: return@launch

            if (currentId != null) {
                val expense = expenseDao.getExpenseById(currentId)
                if (expense != null) {
                    _uiState.value = AddBillUiState(
                        isEditMode = true,
                        isLoading = false,
                        isInitialized = true,
                        loadedExpenseId = currentId,
                        description = expense.description ?: "",
                        amount = expense.amount.toString(),
                        note = expense.note ?: "",
                        markPaid = expense.status == "opłacony",
                        date = DateConverters.millisToLocalDate(expense.date),
                        repeatIntervalIndex = listOf(1, 2, 3, 6, 12).indexOf(expense.repeatInterval)
                            .coerceAtLeast(0)
                    )
                    validate()
                    return@launch
                }
            }

            _uiState.value = AddBillUiState(
                isLoading = false,
                isInitialized = true,
                loadedExpenseId = currentId,
            )
            validate()
        }
    }
    // Zapisuje nowe rachunek lub aktualizuje istniejący
    fun save() {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val state = _uiState.value

            val dateMillis = DateConverters.localDateToStartOfDayMillis(state.date)
            if (expenseId != null) {
                // Edycja rachunku
                expenseDao.updateExpenseFull(
                    expenseId = expenseId,
                    description = state.description,
                    amount = AmountParser.parse(state.amount) ?: return@launch,
                    note = state.note.ifBlank { null },
                    date = dateMillis,
                    interval = listOf(1, 2, 3, 6 ,12)[state.repeatIntervalIndex]
                )
                expenseDao.updateStatus(
                    expenseId,
                    if (state.markPaid) "opłacony" else "nieopłacony"
                )
            } else {
                // Dodaj nowy rachunek
                val expense = Expense(
                    userId = uid,
                    category = "Rachunki",
                    paymentMethod = "Przelew",
                    amount = AmountParser.parse(state.amount) ?: return@launch,
                    description = state.description,
                    note = state.note.ifBlank { null },
                    date = dateMillis,
                    timestamp = System.currentTimeMillis(),
                    isRecurring = true,
                    repeatInterval = listOf(1, 2, 3, 6, 12)[state.repeatIntervalIndex],
                    status = if (state.markPaid) "opłacony" else "nieopłacony"
                )
                expenseDao.insertExpense(expense)
            }

            _uiState.value = state.copy(saved = true)
        }
    }

    fun updateDescription(value: String) {
        _uiState.value = _uiState.value.copy(description = value)
        validate()
    }

    fun updateAmount(value: String) {
        _uiState.value = _uiState.value.copy(amount = value)
        validate()
    }

    fun updateNote(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun updateInterval(index: Int) {
        _uiState.value = _uiState.value.copy(repeatIntervalIndex = index)
    }

    fun updateDate(date: LocalDate) {
        _uiState.value = _uiState.value.copy(date = date)
    }

    fun updateMarkPaid(value: Boolean) {
        _uiState.value = _uiState.value.copy(markPaid = value)
    }

    fun resetForm() {
        _uiState.value = AddBillUiState()
    }
    // Sprawdza poprawność danych formularza
    private fun validate() {
        val descValid = _uiState.value.description.isNotBlank()
        val amountValid = AmountParser.isValid(_uiState.value.amount)
        _uiState.value = _uiState.value.copy(
            descriptionError = if (!descValid) "Wpisz opis" else null,
            amountError = if (!amountValid) "Podaj prawidłową kwotę" else null,
            isValid = descValid && amountValid
        )
    }
}