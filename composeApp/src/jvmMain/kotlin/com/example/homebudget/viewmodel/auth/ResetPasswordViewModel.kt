package com.example.homebudget.viewmodel.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.utils.validation.EmailValidator
import com.example.homebudget.utils.validation.PasswordValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel : ViewModel() {

    private val userDao = AppDatabase.getDatabase().userDao()

    private val _uiState = MutableStateFlow(ResetPasswordUiState())
    val uiState: StateFlow<ResetPasswordUiState> = _uiState

    fun onEmailChanged(value: String) {
        _uiState.value = _uiState.value.copy(email = value)
    }

    fun onNewPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(newPassword = value)
    }

    fun onConfirmPasswordChanged(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value)
    }

    fun resetPassword(onSuccess: () -> Unit) {
        val state = _uiState.value

        if (state.email.isBlank() || state.newPassword.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Wszystkie pola muszą być wypełnione.")
            return
        }

        if (!EmailValidator.isValid(state.email)) {
            _uiState.value = state.copy(errorMessage = "Adres email jest niepoprawny.")
            return
        }

        val passwordError = PasswordValidator.validate(state.newPassword)
        if (passwordError != null) {
            _uiState.value = state.copy(errorMessage = passwordError)
            return
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Hasła nie są takie same.")
            return
        }

        _uiState.value = state.copy(errorMessage = null)

        viewModelScope.launch {
            val user = userDao.getUserByUsername(state.email)
            if (user == null) {
                _uiState.value = state.copy(errorMessage = "Użytkownik o takim emailu nie istnieje.")
                return@launch
            }

            userDao.updatePassword(state.email, state.newPassword)

            // UX-czyścimy formularz
            _uiState.value = ResetPasswordUiState()
            onSuccess()
        }
    }
}

data class ResetPasswordUiState(
    val email: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val errorMessage: String? = null
)