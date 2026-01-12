package com.example.homebudget.data.entity

import androidx.room.Entity

//MonthlyBudget.kt – model danych miesięcznych kwot budżetu.
@Entity(tableName = "monthly_budgets",
primaryKeys = ["userId", "year", "month"])
data class MonthlyBudget(
    val userId: Int,
    val year: Int,
    val month: Int,
    val budget: Double,        // faktyczny budżet na ten miesiąc
    val isDefault: Boolean = false  // jeśli true, oznacza powtarzający się budżet
)