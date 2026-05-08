package com.example.homebudget.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.User
import com.example.homebudget.data.entity.MonthlyBudget
import com.example.homebudget.data.entity.Settings
import com.example.homebudget.data.remote.AuthRepository
import com.example.homebudget.data.remote.repository.MonthlyBudgetRemoteRepository
import com.example.homebudget.data.remote.repository.SettingsRemoteRepository
import com.example.homebudget.data.sync.DashboardSyncManager
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
        _uiState.value = _uiState.value.copy(username = value, usernameError = null, errorMessage = null)
    }
    // Aktuazlizacja hasła
    fun onPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(password = value, passwordError = null, errorMessage = null)
    }
    // Aktualizacja potwierdzenia hasła
    fun onConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, confirmPasswordError = null, errorMessage = null)
    }
    // Aktualizacja nazwy użytkownika
    fun onNameChanged(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errorMessage = null)
    }
    // Aktualizacja akceptacji regulaminu
    fun onTermsAccepted(accepted: Boolean) {
        _uiState.value = _uiState.value.copy(
            acceptedTerms = accepted,
            termsError = null,
            errorMessage = null
        )
    }
    //Rejestruje nowego użytkownika po poprawnej walidacji formularza.
    fun registerUser(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.isLoading) return

        val email = state.username.trim().lowercase()
        val termsError = if (!state.acceptedTerms) "Musisz zaakceptować regulamin" else null
        var hasError = false
        val usernameError =
            if (email.isBlank()) "Email jest wymagany"
            else if (!EmailValidator.isValid(email)) "Niepoprawny adres email"
            else null
        val passwordError = PasswordValidator.validate(state.password)
        val confirmPasswordError =
            if (state.password != state.confirmPassword) "Hasła się nie zgadzają"
            else null
        if (usernameError != null || passwordError != null || confirmPasswordError != null || termsError != null) {
            hasError = true
        }
        _uiState.value = state.copy(
            username = email,
            usernameError = usernameError,
            passwordError = passwordError,
            confirmPasswordError = confirmPasswordError,
            termsError = termsError,
            isLoading = false,
            errorMessage = if (hasError) "Popraw błędy w formularzu." else null
        )
        if (hasError) return

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            try {
                val existingUser = userDao.getUserByUsername(email)
                if (existingUser != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        usernameError = "Email już istnieje",
                        errorMessage = null
                    )
                    return@launch
                }
                val supabaseResult = AuthRepository.signUp(email, state.password)
                if (supabaseResult.isFailure) {
                    val message = supabaseResult.exceptionOrNull()?.message.orEmpty()
                    val isEmailTaken = message.isEmailAlreadyRegisteredMessage()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        usernameError = if (isEmailTaken) "Email już istnieje" else null,
                        errorMessage = if (isEmailTaken) null else "Supabase: ${message.ifBlank { "Nie udało się utworzyć konta." }}"
                    )
                    return@launch
                }
                val supabaseUid = supabaseResult.getOrThrow().id
                Prefs.setSupabaseUid(supabaseUid)

                val trimmedName = state.name.trim().take(20)
                // Tworzenie użytkownika
                val newUser = User(
                    username = email,
                    password = state.password,
                    name = trimmedName,
                    createdAt = System.currentTimeMillis(),
                    lastLogin = System.currentTimeMillis()
                )
                val newUserId = userDao.insertUser(newUser).toInt()
                // Tworzenie domyślnych ustawień
                val defaultSettings = Settings(
                    userId = newUserId,
                    categories = "[\"Jedzenie\",\"Transport\",\"Rachunki\",\"Rozrywka\",\"Inne\"]",
                    currency = "PLN",
                    period = "Miesięczny",
                    savingsGoal = 0.0
                )
                settingsDao.insertSettings(defaultSettings)
                val now = Calendar.getInstance()
                val initialBudget = MonthlyBudget(
                    userId = newUserId,
                    year = now.get(Calendar.YEAR),
                    month = now.get(Calendar.MONTH) + 1,
                    budget = 0.0
                )
                monthlyBudgetDao.insertBudget(initialBudget)
                // ustaw aktualną sesję
                Prefs.setLastLoggedUser(newUserId)
                // auto-zapamiętanie po rejestracji
                Prefs.setRememberedUser(newUserId)
                // Domyślny motyw jasny
                Prefs.setThemeMode("LIGHT")
                try {
                    SettingsRemoteRepository.upsertSettings(supabaseUid, defaultSettings)
                    MonthlyBudgetRemoteRepository.upsertBudget(supabaseUid, initialBudget)
                    DashboardSyncManager.sync()
                } catch (_: Exception) {
                    // Dane lokalne zostają; kolejka zacznie działać po pierwszej zmianie online/offline.
                }
                _uiState.value = _uiState.value.copy(isLoading = false)
                onSuccess()
            } catch (e: Exception) {
                val message = e.message.orEmpty()
                val isEmailTaken = message.isEmailAlreadyRegisteredMessage()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    usernameError = if (isEmailTaken) "Email już istnieje" else _uiState.value.usernameError,
                    errorMessage = if (isEmailTaken) null else e.message ?: "Nie udało się utworzyć konta."
                )
            }
        }
    }

    private fun String.isEmailAlreadyRegisteredMessage(): Boolean {
        val normalized = lowercase()
        return normalized.contains("already registered") ||
                normalized.contains("already exists") ||
                normalized.contains("email exists") ||
                normalized.contains("email_exists") ||
                normalized.contains("duplicate") ||
                normalized.contains("istnieje")
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

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
