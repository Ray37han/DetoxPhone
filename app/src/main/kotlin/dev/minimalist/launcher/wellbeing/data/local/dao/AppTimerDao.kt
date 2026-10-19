package dev.minimalist.launcher.wellbeing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.wellbeing.data.local.entity.AppTimerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppTimerDao {

    @Query("SELECT * FROM app_timers")
    fun getAll(): Flow<List<AppTimerEntity>>

    @Query("SELECT * FROM app_timers WHERE package_name = :packageName")
    suspend fun getForApp(packageName: String): AppTimerEntity?

    @Query("SELECT * FROM app_timers WHERE package_name = :packageName")
    fun observeForApp(packageName: String): Flow<AppTimerEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: AppTimerEntity)

    @Query("DELETE FROM app_timers WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT * FROM app_timers WHERE is_enabled = 1")
    fun getEnabled(): Flow<List<AppTimerEntity>>
}
