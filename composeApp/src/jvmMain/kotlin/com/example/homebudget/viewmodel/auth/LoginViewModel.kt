package com.example.homebudget.viewmodel.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.utils.settings.Prefs
import kotlinx.coroutines.launch

/**
 * LoginViewModel
 *
 * Odpowiada za logikę logowania użytkownika:
 * - walidację danych
 * - sprawdzenie użytkownika w bazie
 * - obsługę zapamiętania sesji
 * - komunikaty błędów
 */
class LoginViewModel : ViewModel() {

    // Aktualne dane logowania
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var errorMessage by mutableStateOf<String?>(null)
    var loginSuccess by mutableStateOf(false)
    var showPassword by mutableStateOf(false)
    var rememberMe by mutableStateOf(false)
    private val userDao = AppDatabase.getDatabase().userDao()

    /**
     * Próba logowania użytkownika.
     * Sprawdza dane, zapisuje sesję i ustawia loginSuccess.
     */
    fun login() {
        errorMessage = null

        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Nieprawidłowy email lub hasło."
            return
        }

        viewModelScope.launch {
            val user = userDao.getUserByUsername(email)

            if (user == null || user.password != password) {
                errorMessage = "Nieprawidłowy email lub hasło."
                return@launch
            }

            // Zapamiętaj login (jeśli user zaznaczył checkbox)
            if (rememberMe) {
                Prefs.setRememberedUser(user.id)
            } else {
                Prefs.setRememberedUser(null)
            }
            // Zapis ostatniego logowania
            Prefs.setLastLoggedUser(user.id)

            loginSuccess = true
        }
    }
}
