package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val year: Int,
    val month: Int,
    val budget: Double,
    val isDefault: Boolean = false
)
