package com.example.homebudget.viewmodel.savings

import com.example.homebudget.data.entity.Contribution
import com.example.homebudget.data.entity.SavingsGoal

data class SavingsUiState(
    val isLoading: Boolean = true,
    val goals: List<SavingsGoal> = emptyList(),

    val showAddGoalDialog: Boolean = false,
    val showEditGoal: SavingsGoal? = null,
    val showWithdrawFor: SavingsGoal? = null,
    val showDeleteGoal: SavingsGoal? = null,

    val showAddContributionFor: SavingsGoal? = null,
    val showHistoryFor: SavingsGoal? = null,
    val contributions: List<Contribution> = emptyList(),

    val availablePeople: List<String> = emptyList(),
    val error: String? = null,

    val notification: SavingsNotification? = null
)

sealed class SavingsNotification {
    data class GoalCompleted(val title: String) : SavingsNotification()
    data class DeadlineSoon(val title: String, val daysLeft: Long) : SavingsNotification()
}
