package dev.minimalist.launcher.wellbeing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.wellbeing.data.local.entity.NotificationFilterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationFilterDao {

    @Query("SELECT * FROM notification_filters")
    fun getAll(): Flow<List<NotificationFilterEntity>>

    @Query("SELECT * FROM notification_filters WHERE package_name = :packageName")
    suspend fun getForApp(packageName: String): NotificationFilterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: NotificationFilterEntity)

    @Query("DELETE FROM notification_filters WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM notification_filters WHERE filter_mode = 'BLOCK'")
    fun getBlocked(): Flow<List<NotificationFilterEntity>>
}
