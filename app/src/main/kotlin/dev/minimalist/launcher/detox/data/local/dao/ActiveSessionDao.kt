package dev.minimalist.launcher.detox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.detox.data.local.entity.ActiveSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActiveSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ActiveSessionEntity)

    @Query("SELECT * FROM active_sessions WHERE package_name = :packageName LIMIT 1")
    suspend fun getSession(packageName: String): ActiveSessionEntity?

    @Query("SELECT * FROM active_sessions WHERE package_name = :packageName LIMIT 1")
    fun getSessionFlow(packageName: String): Flow<ActiveSessionEntity?>

    @Query("SELECT * FROM active_sessions")
    fun getAllFlow(): Flow<List<ActiveSessionEntity>>

    @Query("DELETE FROM active_sessions WHERE package_name = :packageName")
    suspend fun clearSession(packageName: String)

    @Query("DELETE FROM active_sessions")
    suspend fun clearAll()

    /** Increment scroll count for active session. */
    @Query("""
        UPDATE active_sessions 
        SET scroll_events_count = scroll_events_count + 1 
        WHERE package_name = :packageName
    """)
    suspend fun incrementScrollCount(packageName: String)

    /** Set cooldown until epoch for an app after session expires. */
    @Query("""
        UPDATE active_sessions 
        SET cooldown_until_epoch_ms = :cooldownUntilEpochMs 
        WHERE package_name = :packageName
    """)
    suspend fun setCooldown(packageName: String, cooldownUntilEpochMs: Long)

    /** Update scroll decay overlay alpha for the HUD. */
    @Query("""
        UPDATE active_sessions
        SET scroll_decay_alpha = :alpha
        WHERE package_name = :packageName
    """)
    suspend fun updateScrollDecayAlpha(packageName: String, alpha: Float)
}
