package com.mytask.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.mytask.data.local.entity.SyncTombstoneEntity

@Dao
interface SyncTombstoneDao {
    @Query("SELECT * FROM sync_tombstones")
    suspend fun getAll(): List<SyncTombstoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(tombstone: SyncTombstoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(tombstones: List<SyncTombstoneEntity>)

    @Query("DELETE FROM sync_tombstones")
    suspend fun deleteAll()

    @Query("DELETE FROM sync_tombstones WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun delete(entityType: String, entityId: Long)
}
