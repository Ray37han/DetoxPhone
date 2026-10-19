package dev.minimalist.launcher.detox.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import dev.minimalist.launcher.detox.data.local.entity.MonitoredAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MonitoredAppDao {

    @Query("SELECT * FROM monitored_apps ORDER BY package_name ASC")
    fun getAllFlow(): Flow<List<MonitoredAppEntity>>

    @Query("SELECT * FROM monitored_apps WHERE package_name = :packageName LIMIT 1")
    suspend fun getByPackage(packageName: String): MonitoredAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MonitoredAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<MonitoredAppEntity>)

    @Delete
    suspend fun delete(entity: MonitoredAppEntity)

    @Query("DELETE FROM monitored_apps WHERE package_name = :packageName")
    suspend fun deleteByPackage(packageName: String)

    @Query("SELECT COUNT(*) FROM monitored_apps WHERE protection_level > 0")
    fun getActiveMonitoredCount(): Flow<Int>

    @Update
    suspend fun update(entity: MonitoredAppEntity)
}
