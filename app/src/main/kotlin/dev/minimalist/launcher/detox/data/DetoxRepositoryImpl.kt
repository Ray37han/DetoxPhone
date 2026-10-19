package dev.minimalist.launcher.detox.data

import android.os.SystemClock
import dev.minimalist.launcher.detox.data.local.dao.ActiveSessionDao
import dev.minimalist.launcher.detox.data.local.dao.DailyTelemetryDao
import dev.minimalist.launcher.detox.data.local.dao.MonitoredAppDao
import dev.minimalist.launcher.detox.data.local.dao.NotificationVaultDao
import dev.minimalist.launcher.detox.data.local.dao.StrictModeConfigDao
import dev.minimalist.launcher.detox.data.local.entity.ActiveSessionEntity
import dev.minimalist.launcher.detox.data.local.entity.DailyTelemetryEntity
import dev.minimalist.launcher.detox.data.local.entity.MonitoredAppEntity
import dev.minimalist.launcher.detox.data.local.entity.NotificationVaultEntity
import dev.minimalist.launcher.detox.data.local.entity.StrictModeConfigEntity
import dev.minimalist.launcher.detox.domain.model.ActiveSession
import dev.minimalist.launcher.detox.domain.model.DailyTelemetry
import dev.minimalist.launcher.detox.domain.model.MonitoredApp
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.domain.model.StrictModeConfig
import dev.minimalist.launcher.detox.domain.model.UnlockMethod
import dev.minimalist.launcher.detox.domain.model.VaultedNotification
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DetoxRepositoryImpl @Inject constructor(
    private val monitoredAppDao: MonitoredAppDao,
    private val activeSessionDao: ActiveSessionDao,
    private val dailyTelemetryDao: DailyTelemetryDao,
    private val notificationVaultDao: NotificationVaultDao,
    private val strictModeConfigDao: StrictModeConfigDao
) : DetoxRepository {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private fun today(): String = dateFormat.format(Date())

    // ── Monitored Apps ────────────────────────────────────────────────

    override fun getAllMonitoredApps(): Flow<List<MonitoredApp>> =
        monitoredAppDao.getAllFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun getMonitoredApp(packageName: String): MonitoredApp? =
        monitoredAppDao.getByPackage(packageName)?.toDomain()

    override suspend fun upsertMonitoredApp(app: MonitoredApp) =
        monitoredAppDao.upsert(app.toEntity())

    override suspend fun removeMonitoredApp(packageName: String) =
        monitoredAppDao.deleteByPackage(packageName)

    // ── Sessions ──────────────────────────────────────────────────────

    override suspend fun getSession(packageName: String): ActiveSession? {
        val entity = activeSessionDao.getSession(packageName) ?: return null
        if (entity.cooldownUntilEpochMs > 0 && System.currentTimeMillis() >= entity.cooldownUntilEpochMs) {
            activeSessionDao.clearSession(packageName)
            return null
        }
        return entity.toDomain()
    }

    override fun getSessionFlow(packageName: String): Flow<ActiveSession?> =
        activeSessionDao.getSessionFlow(packageName).map { entity ->
            if (entity != null && entity.cooldownUntilEpochMs > 0 && System.currentTimeMillis() >= entity.cooldownUntilEpochMs) {
                activeSessionDao.clearSession(packageName)
                null
            } else {
                entity?.toDomain()
            }
        }

    override suspend fun startSession(
        packageName: String,
        allocatedDurationMs: Long,
        intentReason: String
    ) {
        // Use monotonic clock for the deadline so time doesn't drift with wall-clock skew.
        val deadline = SystemClock.elapsedRealtime() + allocatedDurationMs
        activeSessionDao.upsert(
            ActiveSessionEntity(
                packageName = packageName,
                sessionStartEpochMs = System.currentTimeMillis(),
                allocatedDurationMs = allocatedDurationMs,
                cooldownUntilEpochMs = 0L,
                intentReason = intentReason,
                scrollEventsCount = 0,
                deadlineElapsedMs = deadline,
                pauseElapsedMs = 0L
            )
        )
    }

    override suspend fun endSession(packageName: String, applyCooldownMinutes: Int) {
        if (applyCooldownMinutes > 0) {
            val cooldownUntil = System.currentTimeMillis() + applyCooldownMinutes * 60_000L
            activeSessionDao.setCooldown(packageName, cooldownUntil)
        } else {
            activeSessionDao.clearSession(packageName)
        }
    }

    override suspend fun incrementScrollCount(packageName: String) =
        activeSessionDao.incrementScrollCount(packageName)

    override suspend fun updateScrollDecayAlpha(packageName: String, alpha: Float) =
        activeSessionDao.updateScrollDecayAlpha(packageName, alpha)

    override suspend fun clearAllSessions() = activeSessionDao.clearAll()

    // ── Daily Telemetry ───────────────────────────────────────────────

    override fun getTodayTelemetry(): Flow<List<DailyTelemetry>> =
        dailyTelemetryDao.getDayFlow(today()).map { list -> list.map { it.toDomain() } }

    override fun getRecentTelemetry(): Flow<List<DailyTelemetry>> =
        dailyTelemetryDao.getRecentFlow().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureTelemetryRow(dateYmd: String, packageName: String) {
        val existing = dailyTelemetryDao.getEntry(dateYmd, packageName)
        if (existing == null) {
            dailyTelemetryDao.upsert(
                DailyTelemetryEntity(dateYmd = dateYmd, packageName = packageName)
            )
        }
    }

    override suspend fun getTodayTelemetryForApp(dateYmd: String, packageName: String): DailyTelemetry? =
        dailyTelemetryDao.getEntry(dateYmd, packageName)?.toDomain()

    override suspend fun recordCompletedOpen(dateYmd: String, packageName: String) {
        ensureTelemetryRow(dateYmd, packageName)
        dailyTelemetryDao.incrementCompletedOpens(dateYmd, packageName)
    }

    override suspend fun recordAbortedOpen(dateYmd: String, packageName: String) {
        ensureTelemetryRow(dateYmd, packageName)
        dailyTelemetryDao.incrementAbortedOpens(dateYmd, packageName)
    }

    override suspend fun recordReelsBlocked(dateYmd: String, packageName: String) {
        ensureTelemetryRow(dateYmd, packageName)
        dailyTelemetryDao.incrementReelsBlocked(dateYmd, packageName)
    }

    override suspend fun addScrollMeters(dateYmd: String, packageName: String, meters: Double) {
        ensureTelemetryRow(dateYmd, packageName)
        dailyTelemetryDao.addScrollMeters(dateYmd, packageName, meters)
    }

    override suspend fun updateForegroundTime(dateYmd: String, packageName: String, ms: Long) {
        ensureTelemetryRow(dateYmd, packageName)
        dailyTelemetryDao.updateTotalForegroundMs(dateYmd, packageName, ms)
    }

    override suspend fun pruneOldTelemetry(keepDays: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -keepDays)
        val cutoff = dateFormat.format(cal.time)
        dailyTelemetryDao.deleteOlderThan(cutoff)
    }

    // ── Notification Vault ─────────────────────────────────────────────

    override fun getUnreadNotifications(): Flow<List<VaultedNotification>> =
        notificationVaultDao.getUnreadFlow().map { list -> list.map { it.toDomain() } }

    override fun getUnreadCount(): Flow<Int> = notificationVaultDao.getUnreadCountFlow()

    override suspend fun vaultNotification(notification: VaultedNotification) {
        notificationVaultDao.insert(notification.toEntity())
    }

    override suspend fun markNotificationRead(id: Long) = notificationVaultDao.markRead(id)

    override suspend fun markAllNotificationsRead() = notificationVaultDao.markAllRead()

    override suspend fun pruneOldNotifications(keepDays: Int) {
        val cutoff = System.currentTimeMillis() - keepDays * 24 * 3600_000L
        notificationVaultDao.deleteOlderThan(cutoff)
    }

    // ── Strict Mode ────────────────────────────────────────────────────

    override fun getStrictModeConfig(): Flow<StrictModeConfig?> =
        strictModeConfigDao.getConfigFlow().map { it?.toDomain() }

    override suspend fun updateStrictModeConfig(config: StrictModeConfig) {
        strictModeConfigDao.upsert(config.toEntity())
    }

    override suspend fun activateStrictMode(durationMs: Long, method: String) {
        val until = if (durationMs > 0) System.currentTimeMillis() + durationMs else 0L
        val current = strictModeConfigDao.getConfig() ?: StrictModeConfigEntity()
        strictModeConfigDao.upsert(
            current.copy(
                isActive = true,
                unlockMethod = method,
                activeUntilEpochMs = until
            )
        )
    }

    override suspend fun deactivateStrictMode() {
        val current = strictModeConfigDao.getConfig() ?: StrictModeConfigEntity()
        strictModeConfigDao.upsert(current.copy(isActive = false))
    }

    // ── Mappers ─────────────────────────────────────────────────────────

    private fun MonitoredAppEntity.toDomain() = MonitoredApp(
        packageName = packageName,
        customAlias = customAlias,
        protectionLevel = ProtectionLevel.fromValue(protectionLevel),
        blockShortVideos = blockShortVideos,
        enableScrollDecay = enableScrollDecay,
        forceGrayscale = forceGrayscale,
        dailyBudgetMinutes = dailyBudgetMinutes,
        cooldownMinutes = cooldownMinutes,
        substitutePackage = substitutePackage,
        batchNotifications = batchNotifications
)

    private fun MonitoredApp.toEntity() = MonitoredAppEntity(
        packageName = packageName,
        customAlias = customAlias,
        protectionLevel = protectionLevel.value,
        blockShortVideos = blockShortVideos,
        enableScrollDecay = enableScrollDecay,
        forceGrayscale = forceGrayscale,
        dailyBudgetMinutes = dailyBudgetMinutes,
        cooldownMinutes = cooldownMinutes,
        substitutePackage = substitutePackage,
        batchNotifications = batchNotifications
)

    private fun ActiveSessionEntity.toDomain() = ActiveSession(
        packageName = packageName,
        sessionStartEpochMs = sessionStartEpochMs,
        allocatedDurationMs = allocatedDurationMs,
        cooldownUntilEpochMs = cooldownUntilEpochMs,
        intentReason = intentReason,
        scrollEventsCount = scrollEventsCount,
        deadlineElapsedMs = deadlineElapsedMs,
        pauseElapsedMs = pauseElapsedMs
    )

    private fun DailyTelemetryEntity.toDomain() = DailyTelemetry(
        dateYmd = dateYmd,
        packageName = packageName,
        completedOpens = completedOpens,
        abortedOpens = abortedOpens,
        reelsBlockedCount = reelsBlockedCount,
        totalForegroundMs = totalForegroundMs,
        estimatedScrollMeters = estimatedScrollMeters
    )

    private fun NotificationVaultEntity.toDomain() = VaultedNotification(
        id = id,
        packageName = packageName,
        senderTitle = senderTitle,
        messageBody = messageBody,
        postedEpochMs = postedEpochMs,
        batchWindowId = batchWindowId,
        isRead = isRead
    )

    private fun VaultedNotification.toEntity() = NotificationVaultEntity(
        id = id,
        packageName = packageName,
        senderTitle = senderTitle,
        messageBody = messageBody,
        postedEpochMs = postedEpochMs,
        batchWindowId = batchWindowId,
        isRead = isRead
    )

    private fun StrictModeConfigEntity.toDomain() = StrictModeConfig(
        isActive = isActive,
        unlockMethod = UnlockMethod.fromKey(unlockMethod),
        qrHashSecret = qrHashSecret,
        activeUntilEpochMs = activeUntilEpochMs
    )

    private fun StrictModeConfig.toEntity() = StrictModeConfigEntity(
        id = 1,
        isActive = isActive,
        unlockMethod = unlockMethod.key,
        qrHashSecret = qrHashSecret,
        activeUntilEpochMs = activeUntilEpochMs
    )
}
