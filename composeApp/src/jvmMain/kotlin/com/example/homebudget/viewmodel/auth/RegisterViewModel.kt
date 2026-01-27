package com.example.homebudget.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.User
import com.example.homebudget.data.entity.MonthlyBudget
import com.example.homebudget.data.entity.Settings
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.validation.EmailValidator
import com.example.homebudget.utils.validation.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * RegistrationViewModel
 *
 * Warstwa logiki biznesowej rejestracji użytkownika.
 *
 * Odpowiada za:
 * - walidację danych formularza
 * - sprawdzanie unikalności emaila
 * - tworzenie użytkownika w bazie
 * - inicjalizację ustawień i budżetu
 * - zapis informacji o zalogowanym użytkowniku
 */
class RegistrationViewModel : ViewModel() {
    // Dostęp do bazy danych aplikacji
    private val db = AppDatabase.getDatabase()
    // DAO odpowiedzialne za poszczególne tabele
    private val userDao = db.userDao()
    private val settingsDao = db.settingsDao()
    private val monthlyBudgetDao = db.monthlyBudgetDao()
    // Stan UI - jedno źródło prawdy dla ekranu rejestracji
    private val _uiState = MutableStateFlow(RegistrationUiState())
    val uiState: StateFlow<RegistrationUiState> = _uiState
    // Aktualizacja adresu email
    fun onUsernameChanged(value: String) {
        _uiState.value = _uiState.value.copy(username = value)
    }
    // Aktuazlizacja hasła
    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value)
    }
    // Aktualizacja potwierdzenia hasła
    fun onConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value)
    }
    // Aktualizacja nazwy użytkownika
    fun onNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value)
    }
    // Aktualizacja akceptacji regulaminu
    fun onTermsAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(
            acceptedTerms = accepted,
            termsError = null
        )
    }
    //Rejestruje nowego użytkownika po poprawnej walidacji formularza.
    fun registerUser(onSuccess: () -> Unit) {
        val state = _uiState.value
        val termsError = if (!state.acceptedTerms) "Musisz zaakceptować regulamin." else null
        var hasError = false
        val usernameError =
            if (state.username.isBlank()) "Email jest wymagany"
            else if (!EmailValidator.isValid(state.username)) "Nie poprawny adres email."
            else null
        val passwordError = PasswordValidator.validate(state.password)
        val confirmPasswordError =
            if (state.password != state.confirmPassword) "Hasła nie są takie same."
            else null
        if (usernameError != null || passwordError != null || confirmPasswordError != null || termsError != null) {
            hasError = true
        }
        _uiState.value = state.copy(
            usernameError = usernameError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError,
            termsError = termsError,
            errorMessage = if (hasError) "Popraw błędy w formularzu." else null
        )
        if (hasError) return
        viewModelScope.launch {
            val existingUser = userDao.getUserByUsername(state.username)
            if (existingUser != null) {
                _uiState.value = _uiState.value.copy(
                    usernameError = "Użytkownik o takim emailu już istnieje."
                )
                return@launch
            }
            val trimmedName = state.name.trim().take(20)
            // Tworzenie użytkownika
            val newUser = User(
                username = state.username,
                password = state.password,
                name = trimmedName,
                createdAt = System.currentTimeMillis(),
                lastLogin = System.currentTimeMillis()
            )
            val newUserId = userDao.insertUser(newUser).toInt()
            // Tworzenie domyślnych ustawień
            settingsDao.insertSettings(
                Settings(
                    userId = newUserId,
                    categories = "[\"Jedzenie\",\"Transport\",\"Rachunki\",\"Rozrywka\",\"Inne\"]",
                    currency = "PLN",
                    period = "Miesięczny",
                    savingsGoal = 0.0
                )
            )
            val now = Calendar.getInstance()
            monthlyBudgetDao.insertBudget(
                MonthlyBudget(
                    userId = newUserId,
                    year = now.get(Calendar.YEAR),
                    month = now.get(Calendar.MONTH) + 1,
                    budget = 0.0
                )
            )
            // 🔐 ustaw aktualną sesję
            Prefs.setLastLoggedUser(newUserId)
            // 🔐 (opcjonalnie) auto-zapamiętanie po rejestracji
            Prefs.setRememberedUser(newUserId)
            // Domyślny motyw jasny
            Prefs.setThemeMode("LIGHT")
            onSuccess()
        }
    }
}
data class RegistrationUiState(
    val username: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",

    val acceptedTerms: Boolean = false,
    val termsError: String? = null,

    val usernameError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,

    val errorMessage: String? = null
)