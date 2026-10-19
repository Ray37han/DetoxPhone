package dev.minimalist.launcher.detox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.detox.data.local.entity.NotificationVaultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationVaultDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: NotificationVaultEntity): Long

    @Query("SELECT * FROM notification_vault WHERE is_read = 0 ORDER BY posted_epoch_ms DESC")
    fun getUnreadFlow(): Flow<List<NotificationVaultEntity>>

    @Query("SELECT * FROM notification_vault ORDER BY posted_epoch_ms DESC LIMIT 200")
    fun getAllFlow(): Flow<List<NotificationVaultEntity>>

    @Query("SELECT COUNT(*) FROM notification_vault WHERE is_read = 0")
    fun getUnreadCountFlow(): Flow<Int>

    @Query("UPDATE notification_vault SET is_read = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE notification_vault SET is_read = 1")
    suspend fun markAllRead()

    @Query("DELETE FROM notification_vault WHERE posted_epoch_ms < :cutoffEpochMs")
    suspend fun deleteOlderThan(cutoffEpochMs: Long)

    @Query("SELECT * FROM notification_vault WHERE package_name = :packageName ORDER BY posted_epoch_ms DESC")
    fun getByPackageFlow(packageName: String): Flow<List<NotificationVaultEntity>>
}
