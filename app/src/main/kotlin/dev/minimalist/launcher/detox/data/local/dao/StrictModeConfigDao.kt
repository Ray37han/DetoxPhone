package dev.minimalist.launcher.detox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.detox.data.local.entity.StrictModeConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrictModeConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: StrictModeConfigEntity)

    @Query("SELECT * FROM strict_mode_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<StrictModeConfigEntity?>

    @Query("SELECT * FROM strict_mode_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): StrictModeConfigEntity?

    @Query("UPDATE strict_mode_config SET is_active = :isActive WHERE id = 1")
    suspend fun setActive(isActive: Boolean)

    @Query("UPDATE strict_mode_config SET active_until_epoch_ms = :epochMs WHERE id = 1")
    suspend fun setActiveUntil(epochMs: Long)
}
