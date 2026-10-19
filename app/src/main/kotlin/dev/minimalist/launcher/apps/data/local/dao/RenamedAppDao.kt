package dev.minimalist.launcher.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.apps.data.local.entity.RenamedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RenamedAppDao {

    @Query("SELECT * FROM renamed_apps")
    fun getAll(): Flow<List<RenamedAppEntity>>

    @Query("SELECT custom_name FROM renamed_apps WHERE package_name = :packageName")
    suspend fun getCustomName(packageName: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RenamedAppEntity)

    @Query("DELETE FROM renamed_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)
}
