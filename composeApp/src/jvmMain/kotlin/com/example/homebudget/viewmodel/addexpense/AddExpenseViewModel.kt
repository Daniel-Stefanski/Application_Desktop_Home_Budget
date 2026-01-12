package com.example.homebudget.viewmodel.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.utils.date.DateConverters
import com.example.homebudget.utils.money.AmountParser
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.settings.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * AddExpenseUiState
 *
 * Reprezentuje pełny stan UI formularza dodawania wydatku.
 *
 * Zawiera:
 * - wartości pól formularza
 * - listy danych (kategorie, płatności, osoby)
 * - informacje o błędach walidacji
 * - flagi stanu (loading, valid, dialog zapisany)
 *
 * Każda zmiana stanu powoduje rekombinację UI.
 */

data class AddExpenseUiState(
    val isLoading: Boolean = true,

    val amount: String = "",
    val description: String = "",
    val note: String = "",

    val categories: List<String> = emptyList(),
    val paymentMethods: List<String> = listOf("Brak", "Gotówka", "Karta", "Blik", "Przelew"),
    val people: List<String> = listOf("Ja"),

    val selectedCategoryIndex: Int = 0,
    val selectedPaymentIndex: Int = 0,
    val selectedPersonIndex: Int = 0,

    val date: LocalDate = LocalDate.now(),
    val isRecurring: Boolean = false,
    val cycles: List<String> = listOf("1 miesiąc", "2 miesiące", "3 miesiące", "6 miesięcy", "12 miesięcy"),
    val selectedCycleIndex: Int = 0,

    val amountError: String? = null,
    val descriptionError: String? = null,
    val categoryError: String? = null,
    val paymentError: String? = null,
    val globalError: String? = null,

    val isValid: Boolean = false,
    val showSavedDialog: Boolean = false
)
/**
 * AddExpenseViewModel
 *
 * Warstwa logiki biznesowej dla ekranu dodawania wydatków.
 *
 * Odpowiada za:
 * - wczytanie danych użytkownika i ustawień
 * - zarządzanie stanem formularza
 * - walidację pól
 * - zapis wydatku do bazy danych
 * - obsługę wydatków cyklicznych
 *
 * ViewModel NIE zawiera żadnej logiki UI.
 * Stan jest eksponowany przez StateFlow (single source of truth).
 */
class AddExpenseViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val expenseDao = db.expenseDao()
    private val settingsDao = db.settingsDao()

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState

    private var userId: Int? = null
    /**
     * loadInitialData
     *
     * Wczytuje dane początkowe wymagane do działania formularza:
     * - aktualnie zalogowanego użytkownika
     * - ustawienia użytkownika (kategorie, osoby, domyślne wartości)
     *
     * Wywoływana:
     * - przy inicjalizacji ViewModelu
     * - po powrocie do formularza (reloadDefaultsFromSettings)
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val id = Prefs.getLastLoggedUser()
                if (id == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        globalError = "Brak zalogowanego użytkownika."
                    )
                    return@launch
                }
                userId = id

                val settings = settingsDao.getSettingsForUser(id)
                val categoriesFromSettings =
                    settings?.let { SettingsHelper.getCategories(it) } ?: emptyList()
                val peopleFromSettings =
                    settings?.let { SettingsHelper.getPeople(it) } ?: emptyList()

                val categories = listOf("Brak") + categoriesFromSettings
                val people = listOf("Ja") + peopleFromSettings

                val defaultCategory = settings?.defaultCategory
                val defaultPayment = settings?.defaultPaymentMethod
                val selectedCategoryIndex = categories.indexOf(defaultCategory).takeIf { it >= 0 } ?: 0
                val selectedPaymentIndex = _uiState.value.paymentMethods.indexOf(defaultPayment).takeIf { it >= 0 } ?: 0

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    categories = categories,
                    people = people,

                    selectedCategoryIndex = selectedCategoryIndex,
                    selectedPaymentIndex = selectedPaymentIndex
                )

                validate()
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    globalError = "Nie udało się załadować danych."
                )
            }
        }
    }

    // --- Handlery zmian pól ---
    fun onAmountChange(value: String) {
        _uiState.value = _uiState.value.copy(amount = value, amountError = null)
        validate()
    }
    fun onDescriptionChange(value: String) {
        _uiState.value = _uiState.value.copy(description = value, descriptionError = null)
        validate()
    }
    fun onNoteChange(value: String) {
        _uiState.value = _uiState.value.copy(note = value)
    }

    fun onCategorySelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCategoryIndex = index, categoryError = null)
        validate()
    }

    fun onPaymentSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedPaymentIndex = index, paymentError = null)
        validate()
    }

    fun onPersonSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedPersonIndex = index)
    }

    fun onDateChange(newDate: LocalDate) {
        _uiState.value = _uiState.value.copy(date = newDate)
    }

    fun onRecurringChange(checked: Boolean) {
        _uiState.value = _uiState.value.copy(isRecurring = checked)
    }

    fun onCycleSelected(index: Int) {
        _uiState.value = _uiState.value.copy(selectedCycleIndex = index)
    }
    fun resetForm() {
        _uiState.value = AddExpenseUiState(isLoading = true)
        loadInitialData()
    }
    /**
     * validate
     *
     * Sprawdza poprawność danych formularza.
     *
     * Waliduje:
     * - kwotę
     * - opis
     * - kategorię
     * - metodę płatności
     *
     * Ustawia:
     * - komunikaty błędów
     * - flagę isValid sterującą przyciskiem "Zapisz"
     */
    private fun validate() {
        val s = _uiState.value

        val amountValid = AmountParser.isValid(s.amount)
        val descValid = s.description.isNotBlank()

        val categoryValid = s.categories.isNotEmpty() &&
                s.selectedCategoryIndex in s.categories.indices &&
                s.categories[s.selectedCategoryIndex] != "Brak"

        val paymentValid = s.paymentMethods.isNotEmpty() &&
                s.selectedPaymentIndex in s.paymentMethods.indices &&
                s.paymentMethods[s.selectedPaymentIndex] != "Brak"

        _uiState.value = _uiState.value.copy(
            amountError = if (!amountValid) "Podaj prawidłową kwotę" else null,
            descriptionError = if (!descValid) "Wpisz opis" else null,
            categoryError = if (!categoryValid) "Wybierz kategorię" else null,
            paymentError = if (!paymentValid) "Wybierz metodę płatności" else null,
            isValid = amountValid && descValid && categoryValid && paymentValid
        )
    }
    /**
     * saveExpense
     *
     * Zapisuje wydatek do bazy danych.
     *
     * Kroki:
     * 1. Sprawdza poprawność formularza
     * 2. Mapuje dane UI na encję Expense
     * 3. Zapisuje dane w bazie
     * 4. Wyświetla dialog sukcesu
     */
    fun saveExpense() {
        val current = _uiState.value
        val uid = userId
        if (uid == null) {
            _uiState.value = current.copy(globalError = "Brak zalogowanego użytkownika.")
            return
        }
        if (!current.isValid) {
            validate()
            return
        }

        val amount = AmountParser.parse(current.amount) ?: return

        val category = current.categories.getOrNull(current.selectedCategoryIndex) ?: "Brak"
        val payment = current.paymentMethods.getOrNull(current.selectedPaymentIndex) ?: "Brak"
        val person = current.people.getOrNull(current.selectedPersonIndex) ?: "Ja"

        val dateMillis = DateConverters.localDateToStartOfDayMillis(current.date)

        val isRecurring = current.isRecurring
        val repeatInterval =
            if (isRecurring && current.selectedCycleIndex in current.cycles.indices) {
                current.cycles[current.selectedCycleIndex]
                    .split(" ")[0]
                    .toIntOrNull() ?: 1
            } else 1

        viewModelScope.launch {
            try {
                val expense = Expense(
                    userId = uid,
                    category = category,
                    amount = amount,
                    description = current.description,
                    note = current.note.ifBlank { null },
                    paymentMethod = payment,
                    date = dateMillis,
                    timestamp = System.currentTimeMillis(),
                    isRecurring = isRecurring,
                    repeatInterval = repeatInterval,
                    person = person,
                    // reszta pól ma wartości domyślne w data class
                )

                expenseDao.insertExpense(expense)

                _uiState.value = current.copy(
                    showSavedDialog = true,
                    globalError = null
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.value = current.copy(
                    globalError = "Nie udało się zapisać wydatku."
                )
            }
        }
    }

    fun onSavedDialogResult(addAnother: Boolean) {
        if (addAnother) {
            // wyczyść formularz
            resetForm()
        } else {
            // tylko schowaj dialog; nawigacją zajmie się ekran
            _uiState.value = _uiState.value.copy(showSavedDialog = false)
        }
    }
}