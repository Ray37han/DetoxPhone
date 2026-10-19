package dev.minimalist.launcher.wellbeing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.wellbeing.data.local.entity.BlockScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockScheduleDao {

    @Query("SELECT * FROM block_schedules ORDER BY start_minutes ASC")
    fun getAll(): Flow<List<BlockScheduleEntity>>

    @Query("SELECT * FROM block_schedules WHERE id = :id")
    suspend fun getById(id: Long): BlockScheduleEntity?

    @Query("SELECT * FROM block_schedules WHERE is_enabled = 1")
    fun getEnabled(): Flow<List<BlockScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BlockScheduleEntity): Long

    @Query("DELETE FROM block_schedules WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE block_schedules SET is_enabled = :enabled, last_modified_ms = :modifiedMs WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean, modifiedMs: Long = System.currentTimeMillis())
}
