package com.example.homebudget.viewmodel.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Settings
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.settings.SettingsHelper
import com.example.homebudget.utils.validation.EmailValidator
import com.example.homebudget.utils.validation.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

data class SettingsUiState(
    val isLoading: Boolean = true,
    // Settings
    val categories: List<String> = emptyList(),
    val categoryColors: Map<String, String> = emptyMap(),
    val people: List<String> = emptyList(),
    // User
    val userName: String = "",
    val userEmail: String = "",
    val createdAt: Long? = null,
    val lastLogin: Long? = null,
    // domyślne
    val defaultCategory: String? = null,
    val defaultPaymentMethod: String? = null,

    val error: String? = null
)
/**
 * SettingsViewModel
 *
 * Warstwa logiki biznesowej dla ekranu ustawień.
 *
 * Odpowiada za:
 * - pobieranie i zapisywanie ustawień użytkownika
 * - walidację danych wejściowych
 * - komunikację z bazą danych
 * - przygotowanie stanu UI (SettingsUiState)
 *
 * ViewModel nie zna Compose UI.
 */
class SettingsViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val userDao = db.userDao()
    private val settingsDao = db.settingsDao()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    private var currentSettings: Settings? = null
    /**
     * Wczytuje komplet ustawień aktualnie zalogowanego użytkownika.
     *
     * - pobiera użytkownika z Prefs
     * - pobiera ustawienia z bazy danych
     * - normalizuje kategorie i kolory
     * - uzupełnia brakujące dane
     * - publikuje stan do UI
     */
    fun loadSettings() {
        viewModelScope.launch {
            val userId = Prefs.getLastLoggedUser()
            if (userId == null) {
                _uiState.value = SettingsUiState(error = "Brak użytkownika")
                return@launch
            }

            val settings = settingsDao.getSettingsForUser(userId)
            if (settings == null) {
                _uiState.value = SettingsUiState(error = "Brak ustawień")
                return@launch
            }
            val user = userDao.getUserById(userId)

            currentSettings = settings

            val categories = SettingsHelper.getCategories(settings)
            val colors = SettingsHelper.ensureCategoryColors(
                categories,
                SettingsHelper.getCategoryColors(settings)
            )
            // Zapis do DB jeśli brakowało kolorów
            if (colors != SettingsHelper.getCategoryColors(settings)) {
                settingsDao.update(
                    settings.copy(
                        categoryColors = Json.encodeToString(colors)
                    )
                )
            }
            _uiState.value = SettingsUiState(
                isLoading = false,
                categories = categories,
                categoryColors = colors,
                people = SettingsHelper.getPeople(settings),

                userName = user?.name ?: "",
                userEmail = user?.username ?: "",
                createdAt = user?.createdAt,
                lastLogin = user?.lastLogin,

                defaultCategory = settings.defaultCategory,
                defaultPaymentMethod = settings.defaultPaymentMethod
            )
        }
    }
    /**
     * Zapisuje zmiany danych konta użytkownika.
     *
     * Obsługuje:
     * - zmianę nazwy użytkownika
     * - zmianę adresu email (z walidacją)
     * - zmianę hasła (z walidacją)
     *
     * Funkcja wywołuje callbacki:
     * - onError – w przypadku błędu
     * - onSuccess – po poprawnym zapisie
     */
    fun saveAccountChanges(
        newName: String,
        newEmail: String,
        oldPassword: String,
        newPassword: String,
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val userId = Prefs.getLastLoggedUser() ?: return@launch
            val user = userDao.getUserById(userId) ?: return@launch
            // Imię
            if (newName.isNotBlank() && newName != user.name) {
                userDao.updateUserName(userId, newName)
            }
            // Email (sprawdzanie unikalności)
            if (newEmail.isNotBlank() && newEmail != user.username) {
                if (!EmailValidator.isValid(newEmail)) {
                    onError("Niepoprawny adres email.")
                    return@launch
                }
                val exists = userDao.getUserByUsername(newEmail)
                if (exists != null) {
                    onError("Ten email jest już zajęty")
                    return@launch
                }
                userDao.updateUserEmail(userId, newEmail)
            }
            // HASŁO (tylko jeśli użytkownik chce zmienić)
            if (oldPassword.isNotBlank() || newPassword.isNotBlank()) {
                if (oldPassword != user.password) {
                    onError("Stare hasło jest niepoprawne")
                    return@launch
                }
                val passwordError = PasswordValidator.validate(newPassword)
                if (passwordError != null) {
                    onError(passwordError)
                    return@launch
                }
                userDao.updateUserPassword(userId, newPassword)
            }
            onSuccess()
            loadSettings()
        }
    }

    fun deleteAccount(onDone: () -> Unit) {
        viewModelScope.launch {
            val userId = Prefs.getLastLoggedUser() ?: return@launch
            userDao.deleteUser(userId)
            Prefs.resetAll()
            onDone()
        }
    }
    /**
     * Resetuje dane użytkownika bez usuwania konta.
     *
     * Usuwa:
     * - wydatki
     * - budżety miesięczne
     *
     * Przywraca:
     * - domyślne kategorie
     * - domyślne kolory
     * - pustą listę osób
     */
    fun resetUserData(onDone: () -> Unit) {
        viewModelScope.launch {
            val userId = Prefs.getLastLoggedUser() ?: return@launch

            // usuń dane użytkownika
            db.expenseDao().deleteAll(userId)
            db.monthlyBudgetDao().deleteAllForUser(userId)

            // usuń dane użytkownika
            val settings = settingsDao.getSettingsForUser(userId) ?: return@launch

            val defaultCategories = listOf("Jedzenie","Transport","Rachunki","Rozrywka","Inne")
            val defaultColors = mapOf(
                "Jedzenie" to "#4CAF50",
                "Transport" to "#2196F3",
                "Rachunki" to "#FF9800",
                "Rozrywka" to "#9C27B0",
                "Inne" to "#9E9E9E"
            )

            settingsDao.update(
                settings.copy(
                    categories = Json.encodeToString(defaultCategories),
                    categoryColors = Json.encodeToString(defaultColors),
                    peopleList = "[]"
                )
            )

            onDone()
            loadSettings()
        }
    }

    fun updateDefaultCategory(category: String?) {
        val s = currentSettings ?: return
        saveSettings(s.copy(defaultCategory = category))
    }

    fun updateDefaultPaymentMethod(paymentMethod: String?) {
        val s = currentSettings ?: return
        saveSettings(s.copy(defaultPaymentMethod = paymentMethod))
    }

    fun addCategory(name: String) {
        val s = currentSettings ?: return
        val normalized = SettingsHelper.normalizeCategoryName(name)
        if (normalized.isBlank()) return
        val exists = _uiState.value.categories.any { it.equals(normalized, ignoreCase = true) }
        if (exists) return
        val updated = _uiState.value.categories + normalized
        saveSettings(s.copy(categories = Json.encodeToString(updated)))
    }

    fun removeCategory(name: String) {
        val s = currentSettings ?: return
        val updatedCategories = _uiState.value.categories.filterNot { it == name }
        val updatedColors = _uiState.value.categoryColors.toMutableMap()
        updatedColors.remove(name)
        val newDefault = if (_uiState.value.defaultCategory == name) null else _uiState.value.defaultCategory
        saveSettings(
            s.copy(
                categories = Json.encodeToString(updatedCategories),
                categoryColors = Json.encodeToString(updatedColors),
                defaultCategory = newDefault
            )
        )
    }

    fun addPerson(name: String) {
        val s = currentSettings ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (trimmed.length > 20) return
        if (_uiState.value.people.any { it.equals(trimmed, ignoreCase = true)}) return

        val updated = _uiState.value.people + trimmed
        saveSettings(
            s.copy(peopleList = Json.encodeToString(updated))
        )
    }

    fun removePerson(name: String) {
        val s = currentSettings ?: return
        val updated = _uiState.value.people.filterNot { it == name }
        saveSettings(
            s.copy(peopleList = Json.encodeToString(updated))
        )
    }

    private fun saveSettings(updated: Settings) {
        viewModelScope.launch {
            settingsDao.update(updated)
            currentSettings = updated
            loadSettings()
        }
    }

    fun updateCategoryColor(category: String, colorHex: String) {
        val s = currentSettings ?: return
        val updatedColors = _uiState.value.categoryColors.toMutableMap()
        updatedColors[category] = colorHex
        saveSettings(
            s.copy(categoryColors = Json.encodeToString(updatedColors))
        )
    }
}