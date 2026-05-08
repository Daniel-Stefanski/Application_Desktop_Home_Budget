package com.example.homebudget.viewmodel.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Contribution
import com.example.homebudget.data.entity.SavingsGoal
import com.example.homebudget.data.sync.RemoteSync
import com.example.homebudget.utils.date.DateConverters
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.utils.settings.SettingsHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * ViewModel obsługujący logikę celów oszczędnościowych.
 * Odpowiada za CRUD celów, wpłaty, wypłaty i powiadomienia.
 */
class SavingsViewModel : ViewModel() {

    private val db = AppDatabase.getDatabase()
    private val goalDao = db.savingsGoalDao()
    private val contributionDao = db.contributionDao()

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState
    // Pobiera cele użytkownika i sortuje je:
    // 1. Niezakończone z terminem
    // 2. Niezakończone bez terminu
    // 3. Zakończone
    fun loadGoals() {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val goals = goalDao.getGoalsForUser(uid)
                .sortedWith(
                    compareBy<SavingsGoal> {
                        // Zakonczone na dół
                        it.savedAmount >= it.targetAmount
                    }.thenBy {
                        // Cele bez terminu na dół
                        it.endDate ?: Long.MAX_VALUE
                    }
                )
            _uiState.value = _uiState.value.copy(isLoading = false, goals = goals)
            checkNotifications(goals)

        }
        loadPeople()
    }

    fun showAddGoalDialog() {
        _uiState.value = _uiState.value.copy(showAddGoalDialog = true)
    }

    fun showEditGoalDialog(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(showEditGoal = goal)
    }

    fun showWithdrawDialog(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(showWithdrawFor = goal)
    }

    fun showAddContributionDialog(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(showAddContributionFor = goal)
    }

    fun showHistoryDialog(goal: SavingsGoal) {
        viewModelScope.launch {
            val list = contributionDao.getContributionsForGoal(goal.id)
            _uiState.value = _uiState.value.copy(
                showHistoryFor = goal,
                contributions = list
            )
        }
    }

    fun showDeleteGoalDialog(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(showDeleteGoal = goal)
    }

    fun dismissDialogs() {
        _uiState.value = _uiState.value.copy(
            showAddGoalDialog = false,
            showWithdrawFor = null,
            showEditGoal = null,
            showAddContributionFor = null,
            showHistoryFor = null,
            showDeleteGoal = null,
            contributions = emptyList()
        )
    }

    private fun loadPeople() {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val settings = db.settingsDao().getSettingsForUser(uid)

            val people = listOf("Tylko ja") +
                    (settings?.let { SettingsHelper.getPeople(it) } ?: emptyList())
            _uiState.value = _uiState.value.copy(availablePeople = people.distinct())
        }
    }

    fun addContribution(goal: SavingsGoal, amount: Double, person: String) {
        viewModelScope.launch {
            val contribution = Contribution(
                userId = goal.userId,
                goalId = goal.id,
                personName = person,
                amount = amount
            )
            val localContributionId = contributionDao.insert(contribution).toInt()
            val updatedGoal = goal.copy(savedAmount = goal.savedAmount + amount)
            goalDao.update(updatedGoal)
            RemoteSync.syncContributionInsert(localContributionId, contribution, goal)
            RemoteSync.syncGoalUpdate(updatedGoal)
            dismissDialogs()
            loadGoals()
        }
    }

    fun addGoal(title: String, targetAmount: Double, sharedWith: List<String>, endDate: Long?) {
        viewModelScope.launch {
            val uid = Prefs.getLastLoggedUser() ?: return@launch
            val goal = SavingsGoal(
                userId = uid,
                title = title,
                targetAmount = targetAmount,
                endDate = endDate,
                sharedWith = if (sharedWith.isEmpty()) null else sharedWith.joinToString(", ")
            )
            val localGoalId = goalDao.insert(goal).toInt()
            RemoteSync.syncGoalInsert(localGoalId, goal)
            dismissDialogs()
            loadGoals()
        }
    }

    fun withdraw(goal: SavingsGoal, amount: Double, person: String) {
        viewModelScope.launch {
            val contribution = Contribution(
                userId = goal.userId,
                goalId = goal.id,
                personName = person,
                amount = -amount
            )
            val localContributionId = contributionDao.insert(contribution).toInt()
            val updatedGoal = goal.copy(savedAmount = goal.savedAmount - amount)
            goalDao.update(updatedGoal)
            RemoteSync.syncContributionInsert(localContributionId, contribution, goal)
            RemoteSync.syncGoalUpdate(updatedGoal)
            dismissDialogs()
            loadGoals()
        }
    }

    fun updateGoal(goal: SavingsGoal, title: String, targetAmount: Double, sharedWith: List<String>, endDate: Long?) {
        viewModelScope.launch {
            val updatedGoal = goal.copy(
                title = title,
                targetAmount = targetAmount,
                endDate = endDate,
                sharedWith = if (sharedWith.isEmpty()) null else sharedWith.joinToString(", ")
            )
            goalDao.update(updatedGoal)
            RemoteSync.syncGoalUpdate(updatedGoal)
            dismissDialogs()
            loadGoals()
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            contributionDao.deleteByGoal(goal.id)
            goalDao.delete(goal)
            RemoteSync.syncGoalDelete(goal)
            loadGoals()
        }
    }
    // Sprawdza czy należy wyświetlić powiadomienia
    // (ukończenie celu lub zbliżający się termin)
    private fun checkNotifications(goals: List<SavingsGoal>) {
        goals.forEach { goal ->
            // 100%
            val progress =
                if (goal.targetAmount > 0)
                    goal.savedAmount / goal.targetAmount
                else 0.0

            if (progress >= 1.0 && !Prefs.wasGoalCompleted(goal.id)) {
                Prefs.markGoalCompleted(goal.id)
                _uiState.value = _uiState.value.copy(
                    notification = SavingsNotification.GoalCompleted(goal.title)
                )
                return
            }

            // Termin oszczędności
            goal.endDate?.let {end ->
                val endDate = DateConverters.millisToLocalDate(end)
                val daysLeft = ChronoUnit.DAYS.between(
                    LocalDate.now(),
                    endDate
                )
                val thresholds = listOf(30L, 14L, 7L, 2L, 1L)
                if (daysLeft in thresholds) {
                    val key = "${goal.id}_${goal.endDate}_${daysLeft}_${LocalDate.now()}"
                    if (!Prefs.wasDeadlineWarningShown(key)) {
                        Prefs.markDeadlineWarningShown(key)
                        _uiState.value = _uiState.value.copy(
                            notification = SavingsNotification.DeadlineSoon(
                                goal.title,
                                daysLeft
                            )
                        )
                        return
                    }
                }
            }
        }
    }
    fun clearNotifications() {
        _uiState.value = _uiState.value.copy(notification = null)
    }
}
