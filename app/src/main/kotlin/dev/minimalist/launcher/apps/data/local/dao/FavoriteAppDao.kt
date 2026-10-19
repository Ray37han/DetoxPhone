package dev.minimalist.launcher.apps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.apps.data.local.entity.FavoriteAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteAppDao {

    @Query("SELECT * FROM favorite_apps ORDER BY sort_position ASC")
    fun getAll(): Flow<List<FavoriteAppEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_apps WHERE package_name = :packageName)")
    suspend fun isFavorite(packageName: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavoriteAppEntity)

    @Query("DELETE FROM favorite_apps WHERE package_name = :packageName")
    suspend fun delete(packageName: String)

    @Query("SELECT COALESCE(MAX(sort_position), 0) FROM favorite_apps")
    suspend fun getMaxPosition(): Int
}
