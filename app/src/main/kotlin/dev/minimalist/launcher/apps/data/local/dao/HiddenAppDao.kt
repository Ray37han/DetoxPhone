package dev.minimalist.launcher.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.apps.data.local.entity.HiddenAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HiddenAppDao {

    @Query("SELECT * FROM hidden_apps")
    fun getAll(): Flow<List<HiddenAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM hidden_apps WHERE package_name = :packageName)")
    suspend fun isHidden(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HiddenAppEntity)

    @Query("DELETE FROM hidden_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)
}
