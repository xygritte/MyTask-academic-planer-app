package com.mytask.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sync_tombstones",
    primaryKeys = ["entityType", "entityId"],
    indices = [Index(value = ["deletedAt"])]
)
data class SyncTombstoneEntity(
    val entityType: String,
    val entityId: Long,
    val deletedAt: Long = System.currentTimeMillis()
)
