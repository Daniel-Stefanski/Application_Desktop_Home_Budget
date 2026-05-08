package com.example.homebudget.data.sync

import com.example.homebudget.data.database.AppDatabase
import com.example.homebudget.data.entity.Contribution
import com.example.homebudget.data.entity.Expense
import com.example.homebudget.data.entity.MonthlyBudget
import com.example.homebudget.data.entity.PendingSync
import com.example.homebudget.data.entity.SavingsGoal
import com.example.homebudget.data.entity.Settings
import com.example.homebudget.data.remote.repository.ExpenseRemoteRepository
import com.example.homebudget.data.remote.repository.MonthlyBudgetRemoteRepository
import com.example.homebudget.data.remote.repository.SavingsRemoteRepository
import com.example.homebudget.data.remote.repository.SettingsRemoteRepository
import com.example.homebudget.utils.settings.Prefs
import com.example.homebudget.work.worker.WorkSchedulerSupabase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object RemoteSync {
    private val db get() = AppDatabase.getDatabase()

    suspend fun syncExpenseInsert(localId: Int, expense: Expense) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueueExpense(localId, null, expense, SyncConstants.OP_INSERT)
            notifyQueued()
            return
        }
        try {
            val remoteId = ExpenseRemoteRepository.insertExpense(supabaseUid, expense.copy(id = localId))
            db.expenseDao().updateRemoteId(localId, remoteId)
            notifySynced()
        } catch (_: Exception) {
            enqueueExpense(localId, null, expense.copy(id = localId), SyncConstants.OP_INSERT)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncExpenseUpdate(expense: Expense) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueueExpense(expense.id, expense.remoteId, expense, SyncConstants.OP_UPDATE)
            notifyQueued()
            return
        }
        try {
            val remoteId = expense.remoteId
            if (remoteId != null) {
                ExpenseRemoteRepository.updateExpense(supabaseUid, remoteId, expense)
            } else {
                val insertedId = ExpenseRemoteRepository.insertExpense(supabaseUid, expense)
                db.expenseDao().updateRemoteId(expense.id, insertedId)
            }
            notifySynced()
        } catch (_: Exception) {
            enqueueExpense(expense.id, expense.remoteId, expense, if (expense.remoteId == null) SyncConstants.OP_INSERT else SyncConstants.OP_UPDATE)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncExpenseDelete(expense: Expense) {
        try {
            expense.remoteId?.let { ExpenseRemoteRepository.deleteExpense(it) }
            notifySynced()
        } catch (_: Exception) {
            enqueueExpense(expense.id, expense.remoteId, expense, SyncConstants.OP_DELETE)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncBudget(budget: MonthlyBudget) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueue(SyncConstants.ENTITY_BUDGET, budget.id, null, SyncConstants.OP_UPDATE, Json.encodeToString(budget))
            notifyQueued()
            return
        }
        try {
            MonthlyBudgetRemoteRepository.upsertBudget(supabaseUid, budget)
            notifySynced()
        } catch (_: Exception) {
            enqueue(SyncConstants.ENTITY_BUDGET, budget.id, null, SyncConstants.OP_UPDATE, Json.encodeToString(budget))
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncGoalInsert(localId: Int, goal: SavingsGoal) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueueGoal(localId, null, goal.copy(id = localId), SyncConstants.OP_INSERT)
            notifyQueued()
            return
        }
        try {
            val remoteId = SavingsRemoteRepository.insertGoal(supabaseUid, goal.copy(id = localId))
            db.savingsGoalDao().updateRemoteId(localId, remoteId)
            notifySynced()
        } catch (_: Exception) {
            enqueueGoal(localId, null, goal.copy(id = localId), SyncConstants.OP_INSERT)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncGoalUpdate(goal: SavingsGoal) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueueGoal(goal.id, goal.remoteId, goal, SyncConstants.OP_UPDATE)
            notifyQueued()
            return
        }
        try {
            val remoteId = goal.remoteId
            if (remoteId != null) {
                SavingsRemoteRepository.updateGoal(remoteId, goal)
            } else {
                val insertedId = SavingsRemoteRepository.insertGoal(supabaseUid, goal)
                db.savingsGoalDao().updateRemoteId(goal.id, insertedId)
            }
            notifySynced()
        } catch (_: Exception) {
            enqueueGoal(goal.id, goal.remoteId, goal, if (goal.remoteId == null) SyncConstants.OP_INSERT else SyncConstants.OP_UPDATE)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncGoalDelete(goal: SavingsGoal) {
        try {
            goal.remoteId?.let { SavingsRemoteRepository.deleteGoal(it) }
            notifySynced()
        } catch (_: Exception) {
            enqueueGoal(goal.id, goal.remoteId, goal, SyncConstants.OP_DELETE)
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncContributionInsert(localId: Int, contribution: Contribution, goal: SavingsGoal) {
        val supabaseUid = Prefs.getSupabaseUid()
        val remoteGoalId = goal.remoteId
        if (supabaseUid.isNullOrBlank() || remoteGoalId == null) {
            enqueue(SyncConstants.ENTITY_CONTRIBUTION, localId, null, SyncConstants.OP_INSERT, Json.encodeToString(contribution.copy(id = localId)))
            notifyQueued()
            WorkSchedulerSupabase.scheduleSupabaseSync()
            return
        }
        try {
            val remoteId = SavingsRemoteRepository.insertContribution(supabaseUid, remoteGoalId, contribution.copy(id = localId))
            db.contributionDao().updateRemoteId(localId, remoteId)
            notifySynced()
        } catch (_: Exception) {
            enqueue(SyncConstants.ENTITY_CONTRIBUTION, localId, null, SyncConstants.OP_INSERT, Json.encodeToString(contribution.copy(id = localId)))
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    suspend fun syncSettings(settings: Settings) {
        val supabaseUid = Prefs.getSupabaseUid()
        if (supabaseUid.isNullOrBlank()) {
            enqueue(SyncConstants.ENTITY_SETTINGS, settings.userId, null, SyncConstants.OP_UPDATE, Json.encodeToString(settings))
            notifyQueued()
            return
        }
        try {
            SettingsRemoteRepository.upsertSettings(supabaseUid, settings)
            notifySynced()
        } catch (_: Exception) {
            enqueue(SyncConstants.ENTITY_SETTINGS, settings.userId, null, SyncConstants.OP_UPDATE, Json.encodeToString(settings))
            notifyQueued()
        }
        WorkSchedulerSupabase.scheduleSupabaseSync()
    }

    private suspend fun enqueueExpense(localId: Int, remoteId: Long?, expense: Expense, operation: String) =
        enqueue(SyncConstants.ENTITY_EXPENSE, localId, remoteId, operation, Json.encodeToString(expense))

    private suspend fun enqueueGoal(localId: Int, remoteId: Long?, goal: SavingsGoal, operation: String) =
        enqueue(SyncConstants.ENTITY_SAVINGS_GOAL, localId, remoteId, operation, Json.encodeToString(goal))

    private suspend fun enqueue(entityType: String, localId: Int?, remoteId: Long?, operation: String, payload: String) {
        PendingSyncHelper.enqueueOrMerge(
            db.pendingSyncDao(),
            PendingSync(entityType = entityType, operation = operation, localId = localId, remoteId = remoteId, payloadJson = payload)
        )
    }

    private fun notifySynced() {
        SupabaseToastBus.showSynced()
    }

    private fun notifyQueued() {
        SupabaseToastBus.showQueued()
    }
}
