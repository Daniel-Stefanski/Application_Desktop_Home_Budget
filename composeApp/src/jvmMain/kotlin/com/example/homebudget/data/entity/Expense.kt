package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "expenses",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["date"]),
        Index(value = ["isRecurring"]),
        Index(value = ["status"]),
        Index(value = ["category"]),
        Index(value = ["repeatInterval"])
    ]
)
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val remoteId: Long? = null,
    val category: String,
    val amount: Double,
    val description: String?,
    val note: String?,
    val paymentMethod: String,
    val date: Long,
    val timestamp: Long,
    val isRecurring: Boolean = false,
    val repeatInterval: Int = 1,
    val person: String? = null,
    val status: String = "nieopłacony",
    val lastReset: Long = 0
)
