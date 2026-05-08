package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val remoteId: Long? = null,
    val title: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val endDate: Long? = null,
    val sharedWith: String? = null,
    val notificationCompletedSent: Boolean = false
)
