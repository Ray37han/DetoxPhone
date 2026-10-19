package dev.minimalist.launcher.detox.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import dev.minimalist.launcher.detox.data.local.entity.DailyTelemetryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyTelemetryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DailyTelemetryEntity)

    @Query("SELECT * FROM daily_telemetry WHERE date_ymd = :dateYmd ORDER BY total_foreground_ms DESC")
    fun getDayFlow(dateYmd: String): Flow<List<DailyTelemetryEntity>>

    @Query("SELECT * FROM daily_telemetry WHERE date_ymd = :dateYmd AND package_name = :packageName LIMIT 1")
    suspend fun getEntry(dateYmd: String, packageName: String): DailyTelemetryEntity?

    @Query("SELECT * FROM daily_telemetry ORDER BY date_ymd DESC, total_foreground_ms DESC LIMIT 100")
    fun getRecentFlow(): Flow<List<DailyTelemetryEntity>>

    @Query("""
        SELECT SUM(total_foreground_ms) FROM daily_telemetry 
        WHERE date_ymd = :dateYmd AND package_name != '__GLOBAL__'
    """)
    suspend fun getTotalScreenTimeMs(dateYmd: String): Long?

    @Query("""
        SELECT SUM(aborted_opens) FROM daily_telemetry 
        WHERE date_ymd = :dateYmd
    """)
    suspend fun getTotalImpulsesDeflected(dateYmd: String): Int?

    @Query("""
        UPDATE daily_telemetry
        SET completed_opens = completed_opens + 1
        WHERE date_ymd = :dateYmd AND package_name = :packageName
    """)
    suspend fun incrementCompletedOpens(dateYmd: String, packageName: String)

    @Query("""
        UPDATE daily_telemetry
        SET aborted_opens = aborted_opens + 1
        WHERE date_ymd = :dateYmd AND package_name = :packageName
    """)
    suspend fun incrementAbortedOpens(dateYmd: String, packageName: String)

    @Query("""
        UPDATE daily_telemetry
        SET reels_blocked_count = reels_blocked_count + 1
        WHERE date_ymd = :dateYmd AND package_name = :packageName
    """)
    suspend fun incrementReelsBlocked(dateYmd: String, packageName: String)

    @Query("""
        UPDATE daily_telemetry
        SET estimated_scroll_meters = estimated_scroll_meters + :meters
        WHERE date_ymd = :dateYmd AND package_name = :packageName
    """)
    suspend fun addScrollMeters(dateYmd: String, packageName: String, meters: Double)

    @Query("""
        UPDATE daily_telemetry
        SET total_foreground_ms = :foregroundMs
        WHERE date_ymd = :dateYmd AND package_name = :packageName
    """)
    suspend fun updateTotalForegroundMs(dateYmd: String, packageName: String, foregroundMs: Long)

    /** Delete records older than N days to keep DB lean. */
    @Query("DELETE FROM daily_telemetry WHERE date_ymd < :cutoffDate")
    suspend fun deleteOlderThan(cutoffDate: String)
}
