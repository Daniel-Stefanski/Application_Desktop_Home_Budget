package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "contributions")
data class Contribution(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val remoteId: Long? = null,
    val userId: Int,
    val goalId: Int,
    val personName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis()
)
