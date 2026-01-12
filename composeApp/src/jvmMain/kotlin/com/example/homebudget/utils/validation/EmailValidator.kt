package com.example.homebudget.utils.validation

/**
 * EmailValidator
 *
 * Centralna walidacja adresów email.
 */
object EmailValidator {

    private val basicRegex =
        Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+(\\.[A-Za-z0-9-]+)+$")

    fun isValid(email: String): Boolean {
        if (!basicRegex.matches(email)) return false
        if (email.contains("..")) return false
        if (email.endsWith(".")) return false
        if (email.contains("@.")) return false
        return true
    }
}