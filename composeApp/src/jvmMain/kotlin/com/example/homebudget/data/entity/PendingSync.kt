package com.example.homebudget.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSync(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val entityType: String,
    val operation: String,
    val localId: Int?,
    val remoteId: Long?,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis()
)
