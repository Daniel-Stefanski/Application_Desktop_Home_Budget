package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "settings",
    foreignKeys = [ForeignKey(
        entity = User::class,
        parentColumns = ["id"],
        childColumns = ["userId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Settings(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val categories: String,
    val currency: String,
    val period: String,
    val savingsGoal: Double,
    val categoryColors: String = "{}",
    val peopleList: String = "[]",
    val defaultCategory: String? = null,
    val defaultPaymentMethod: String? = null
)
