package dev.minimalist.launcher.detox.domain.repository

import dev.minimalist.launcher.detox.domain.model.ActiveSession
import dev.minimalist.launcher.detox.domain.model.DailyTelemetry
import dev.minimalist.launcher.detox.domain.model.MonitoredApp
import dev.minimalist.launcher.detox.domain.model.StrictModeConfig
import dev.minimalist.launcher.detox.domain.model.VaultedNotification
import kotlinx.coroutines.flow.Flow

interface DetoxRepository {

    // ── Monitored Apps ────────────────────────────────────────────────

    fun getAllMonitoredApps(): Flow<List<MonitoredApp>>
    suspend fun getMonitoredApp(packageName: String): MonitoredApp?
    suspend fun upsertMonitoredApp(app: MonitoredApp)
    suspend fun removeMonitoredApp(packageName: String)

    // ── Sessions ──────────────────────────────────────────────────────

    suspend fun getSession(packageName: String): ActiveSession?
    fun getSessionFlow(packageName: String): Flow<ActiveSession?>
    suspend fun startSession(
        packageName: String,
        allocatedDurationMs: Long,
        intentReason: String
    )
    suspend fun endSession(packageName: String, applyCooldownMinutes: Int = 0)
    suspend fun incrementScrollCount(packageName: String)
    /** Store scroll decay alpha in the active session for HUD rendering. */
    suspend fun updateScrollDecayAlpha(packageName: String, alpha: Float)
    suspend fun clearAllSessions()

    // ── Daily Telemetry ───────────────────────────────────────────────

    fun getTodayTelemetry(): Flow<List<DailyTelemetry>>
    fun getRecentTelemetry(): Flow<List<DailyTelemetry>>
    suspend fun ensureTelemetryRow(dateYmd: String, packageName: String)
    suspend fun getTodayTelemetryForApp(dateYmd: String, packageName: String): DailyTelemetry?
    suspend fun recordCompletedOpen(dateYmd: String, packageName: String)
    suspend fun recordAbortedOpen(dateYmd: String, packageName: String)
    suspend fun recordReelsBlocked(dateYmd: String, packageName: String)
    suspend fun addScrollMeters(dateYmd: String, packageName: String, meters: Double)
    suspend fun updateForegroundTime(dateYmd: String, packageName: String, ms: Long)
    suspend fun pruneOldTelemetry(keepDays: Int = 30)

    // ── Notification Vault ─────────────────────────────────────────────

    fun getUnreadNotifications(): Flow<List<VaultedNotification>>
    fun getUnreadCount(): Flow<Int>
    suspend fun vaultNotification(notification: VaultedNotification)
    suspend fun markNotificationRead(id: Long)
    suspend fun markAllNotificationsRead()
    suspend fun pruneOldNotifications(keepDays: Int = 7)

    // ── Strict Mode ────────────────────────────────────────────────────

    fun getStrictModeConfig(): Flow<StrictModeConfig?>
    suspend fun updateStrictModeConfig(config: StrictModeConfig)
    suspend fun activateStrictMode(durationMs: Long, method: String)
    suspend fun deactivateStrictMode()
}
