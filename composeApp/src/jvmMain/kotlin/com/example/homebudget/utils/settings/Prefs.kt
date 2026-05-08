package com.example.homebudget.utils.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Prefs
 *
 * Globalny mechanizm zapisu danych aplikacji do pliku JSON.
 * Używany do:
 * - zapamiętywania użytkownika
 * - motywu aplikacji
 * - flag powiadomień
 *
 * Dane są zapisywane lokalnie w pliku homebudget_prefs.json
 */

object Prefs {
    private val prefsFile = File(
        System.getProperty("user.home"),
        ".homebudget/homebudget_prefs.json"

    )
    @Serializable
    data class PrefsData(
        val rememberedUserId: Int? = null, // auto-login
        val lastLoggedUserId: Int? = null, // aktualna sesja
        val themeMode: String = "SYSTEM",
        val supabaseUid: String? = null,
        val pendingPasswordResetEmail: String? = null,

        // Powiadomienie budżetu
        val lastBudgetWarningDate: String? = null,

        // Powiadomienia oszczędności
        val completedSavingsGoal: Set<Int> = emptySet(),
        val savingsDeadlineWarnings: Set<String> = emptySet(),

        // Powiadomienie planowania rachunków
        val billsDeadlineWarnings: Set<String> = emptySet()
    )

    private var data = PrefsData()

    init {
        load()
    }

    fun load() {
        if (prefsFile.exists()) {
            try {
                val text = prefsFile.readText()
                data = Json.Default.decodeFromString(text)
            } catch (_: Exception) {
                data = PrefsData()
            }
        }
    }

    fun save() {
        prefsFile.writeText(Json.encodeToString(data))
    }

    fun setRememberedUser(userId: Int?) {
        data = data.copy(rememberedUserId = userId)
        save()
    }

    fun getRememberedUser(): Int? = data.rememberedUserId

    fun setLastLoggedUser(userId: Int?) {
        data = data.copy(lastLoggedUserId = userId)
        save()
    }

    fun getLastLoggedUser(): Int? = data.lastLoggedUserId
    fun resetAll() {
        data = data.copy(
            rememberedUserId = null,
            lastLoggedUserId = null,
            supabaseUid = null,
            pendingPasswordResetEmail = null
        )
        save()
    }

    fun setSupabaseUid(uid: String?) {
        data = data.copy(supabaseUid = uid)
        save()
    }

    fun getSupabaseUid(): String? = data.supabaseUid

    fun setPendingPasswordResetEmail(email: String?) {
        data = data.copy(pendingPasswordResetEmail = email)
        save()
    }

    fun getPendingPasswordResetEmail(): String? = data.pendingPasswordResetEmail

    fun clearPendingPasswordResetEmail() {
        data = data.copy(pendingPasswordResetEmail = null)
        save()
    }

    fun setThemeMode(mode: String) {
        data = data.copy(themeMode = mode)
        save()
    }

    fun getThemeMode(): String = data.themeMode

    fun getLastBudgetWarningDate(): String? = data.lastBudgetWarningDate
    fun setLastBudgetWarningDate(date: String?) {
        data = data.copy(lastBudgetWarningDate = date)
        save()
    }

    fun wasGoalCompleted(goalId: Int): Boolean = data.completedSavingsGoal.contains(goalId)
    fun markGoalCompleted(goalId: Int) {
        data = data.copy(completedSavingsGoal = data.completedSavingsGoal + goalId)
        save()
    }
    fun wasDeadlineWarningShown(key: String): Boolean = data.savingsDeadlineWarnings.contains(key)
    fun markDeadlineWarningShown(key: String) {
        data = data.copy(savingsDeadlineWarnings = data.savingsDeadlineWarnings + key)
        save()
    }

    fun wasBillDeadlineWarningShown(key: String): Boolean = data.billsDeadlineWarnings.contains(key)
    fun markBillDeadlineWarningShown(key: String) {
        data = data.copy(billsDeadlineWarnings = data.billsDeadlineWarnings + key)
        save()
    }
}
