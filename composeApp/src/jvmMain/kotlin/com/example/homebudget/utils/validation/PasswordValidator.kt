package com.example.homebudget.utils.validation

/**
 * PasswordValidator
 *
 * Centralna walidacja haseł w aplikacji.
 *
 * Zwraca:
 * - null → hasło poprawne
 * - String → komunikat błędu
 */
object PasswordValidator {

    fun validate(password: String): String? {
        if (password.length < 8) {
            return "Hasło musi mieć min. 8 znaków."
        }
        if (!password.any { it.isLowerCase() }) {
            return "Hasło musi zawierać małą literę."
        }
        if (!password.any { it.isUpperCase() }) {
            return "Hasło musi zawierać dużą literę."
        }
        if (!password.any { it.isDigit() }) {
            return "Hasło musi zawierać cyfrę."
        }
        if (!password.any { !it.isLetterOrDigit() }) {
            return "Hasło musi zawierać znak specjalny."
        }
        return null
    }
}