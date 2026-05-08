package com.example.homebudget.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.validation.PasswordValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class SetNewPasswordViewModel : ViewModel() {

    private val userDao = AppDatabase.getDatabase().userDao()

    private val _uiState = MutableStateFlow(SetNewPasswordUiState())
    val uiState: StateFlow<SetNewPasswordUiState> = _uiState

    fun setInitialToken(token: String?) {
        if (token.isNullOrBlank() || _uiState.value.token.isNotBlank()) return
        _uiState.value = _uiState.value.copy(token = token.trim())
    }

    fun onTokenChanged(value: String) {
        _uiState.value = _uiState.value.copy(token = value, errorMessage = null)
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value, errorMessage = null)
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun setNewPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val token = state.token.trim()

        if (token.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Brak tokena resetu hasła")
            return
        }

        if (state.newPassword.isBlank() || state.confirmPassword.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Wypełnij wszystkie pola")
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Hasła się nie zgadzają")
            return
        }

        val passwordError = PasswordValidator.validate(state.newPassword)
        if (passwordError != null) {
            _uiState.value = state.copy(errorMessage = "Hasło nie spełnia wymagań")
            return
        }

        _uiState.value = state.copy(token = token, isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = sendChangePasswordRequest(token, state.newPassword)

            if (result.isSuccess) {
                syncLocalPasswordIfPossible(state.newPassword)
                Prefs.setPendingPasswordResetEmail(null)
                Prefs.resetAll()
                _uiState.value = SetNewPasswordUiState()
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = extractServerMessage(result.exceptionOrNull()?.message)
                )
            }
        }
    }

    private suspend fun sendChangePasswordRequest(
        token: String,
        newPassword: String
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(SET_NEW_PASSWORD_WEBHOOK_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                val jsonBody =
                    """{"token":"${token.escapeJson()}","newPassword":"${newPassword.escapeJson()}"}"""
                BufferedWriter(OutputStreamWriter(connection.outputStream)).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                        ?: "Nie udało się zmienić hasła."
                }

                if (responseCode in 200..299) {
                    Result.success(responseText)
                } else {
                    Result.failure(IllegalStateException(responseText))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private suspend fun syncLocalPasswordIfPossible(newPassword: String) {
        val email = Prefs.getPendingPasswordResetEmail()?.trim()?.lowercase() ?: return
        withContext(Dispatchers.IO) {
            userDao.updatePassword(email, newPassword)
        }
    }

    private fun extractServerMessage(responseText: String?): String {
        if (responseText.isNullOrBlank()) {
            return "Błąd połączenia. Sprawdź internet i spróbuj ponownie."
        }
        val marker = """"message""""
        val markerIndex = responseText.indexOf(marker)
        if (markerIndex == -1) return "Nie udało się zmienić hasła."

        val colonIndex = responseText.indexOf(':', startIndex = markerIndex + marker.length)
        if (colonIndex == -1) return "Nie udało się zmienić hasła."

        val firstQuote = responseText.indexOf('"', startIndex = colonIndex + 1)
        if (firstQuote == -1) return "Nie udało się zmienić hasła."

        val secondQuote = responseText.indexOf('"', startIndex = firstQuote + 1)
        if (secondQuote == -1) return "Nie udało się zmienić hasła."

        return responseText.substring(firstQuote + 1, secondQuote)
            .ifBlank { "Nie udało się zmienić hasła." }
    }

    private fun String.escapeJson(): String =
        buildString {
            for (char in this@escapeJson) {
                when (char) {
                    '\\' -> append("\\\\")
                    '"' -> append("\\\"")
                    '\b' -> append("\\b")
                    '\u000C' -> append("\\f")
                    '\n' -> append("\\n")
                    '\r' -> append("\\r")
                    '\t' -> append("\\t")
                    else -> append(char)
                }
            }
        }

    private companion object {
        const val SET_NEW_PASSWORD_WEBHOOK_URL =
            "https://jojigot576.app.n8n.cloud/webhook/set-new-password"
    }
}

data class SetNewPasswordUiState(
    val token: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
