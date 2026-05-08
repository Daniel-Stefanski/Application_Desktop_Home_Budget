package com.example.homebudget.viewmodel.auth

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.User
import com.example.homebudget.data.remote.AuthRepository
import com.example.homebudget.data.sync.DashboardSyncManager
import com.example.homebudget.data.sync.SyncProcessor
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.work.worker.WorkSchedulerSupabase
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
            try {
                val normalizedEmail = email.trim().lowercase()
                val supabaseResult = AuthRepository.signIn(normalizedEmail, password)
                if (supabaseResult.isSuccess) {
                    Prefs.setSupabaseUid(supabaseResult.getOrThrow().id)
                }

                val localUser = userDao.getUserByUsername(normalizedEmail)

                val userId = when {
                    supabaseResult.isSuccess && localUser == null -> {
                        val now = System.currentTimeMillis()
                        userDao.insertUser(
                            User(
                                username = normalizedEmail,
                                password = password,
                                name = "",
                                createdAt = now,
                                lastLogin = now
                            )
                        ).toInt()
                    }
                    localUser != null && localUser.password == password -> {
                        userDao.updateLastLogin(localUser.id, System.currentTimeMillis())
                        localUser.id
                    }
                    else -> {
                        errorMessage = "Nieprawidłowy email lub hasło."
                        return@launch
                    }
                }

                if (userId <= 0) {
                    errorMessage = "Nieprawidłowy email lub hasło."
                    return@launch
                }

                // Zapamiętaj login (jeśli user zaznaczył checkbox)
                if (rememberMe) {
                    Prefs.setRememberedUser(userId)
                } else {
                    Prefs.setRememberedUser(null)
                }
                // Zapis ostatniego logowania
                Prefs.setLastLoggedUser(userId)

                SyncProcessor.processPendingSync()
                if (!DashboardSyncManager.sync()) {
                    WorkSchedulerSupabase.scheduleSupabaseSync()
                }

                loginSuccess = true
            } catch (e: Exception) {
                errorMessage = e.message ?: "Nie udało się zalogować."
            }
        }
    }
}
