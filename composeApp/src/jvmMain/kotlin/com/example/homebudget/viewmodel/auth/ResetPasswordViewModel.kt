package com.example.homebudget.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.validation.EmailValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ResetPasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun resetPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val email = state.email.trim().lowercase()

        if (email.isBlank()) {
            _uiState.value = state.copy(
                email = email,
                errorMessage = "Email jest wymagany"
            )
            return
        }

        if (!EmailValidator.isValid(email)) {
            _uiState.value = state.copy(
                email = email,
                errorMessage = "Niepoprawny adres email"
            )
            return
        }

        _uiState.value = state.copy(email = email, isLoading = true, errorMessage = null)

        viewModelScope.launch {
            val result = sendResetRequest(email)

            if (result.isSuccess) {
                Prefs.setPendingPasswordResetEmail(email)
                _uiState.value = ResetPasswordUiState(email = email)
                onSuccess()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Nie udało się wysłać zgłoszenia resetu. Sprawdź internet i spróbuj ponownie."
                )
            }
        }
    }

    private suspend fun sendResetRequest(email: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(PASSWORD_RESET_WEBHOOK_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 15_000
                connection.readTimeout = 15_000

                val jsonBody = """{"email":"${email.escapeJson()}"}"""
                BufferedWriter(OutputStreamWriter(connection.outputStream)).use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }
                        ?: "Błąd połączenia z serwerem. Przepraszamy"
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
        const val PASSWORD_RESET_WEBHOOK_URL =
            "https://jojigot576.app.n8n.cloud/webhook/request-password-reset"
    }
}

data class ResetPasswordUiState(
    val email: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
