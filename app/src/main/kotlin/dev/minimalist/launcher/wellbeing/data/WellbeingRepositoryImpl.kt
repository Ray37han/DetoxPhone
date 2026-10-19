package dev.minimalist.launcher.wellbeing.data

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.minimalist.launcher.core.common.Result
import dev.minimalist.launcher.core.common.runCatchingResult
import dev.minimalist.launcher.settings.domain.model.TimerBehaviour
import dev.minimalist.launcher.wellbeing.data.local.dao.AppTimerDao
import dev.minimalist.launcher.wellbeing.data.local.dao.BlockScheduleDao
import dev.minimalist.launcher.wellbeing.data.local.dao.NotificationFilterDao
import dev.minimalist.launcher.wellbeing.data.local.entity.AppTimerEntity
import dev.minimalist.launcher.wellbeing.data.local.entity.BlockScheduleEntity
import dev.minimalist.launcher.wellbeing.data.local.entity.NotificationFilterEntity
import dev.minimalist.launcher.wellbeing.domain.model.AppTimer
import dev.minimalist.launcher.wellbeing.domain.model.BlockSchedule
import dev.minimalist.launcher.wellbeing.domain.model.DayOfWeek
import dev.minimalist.launcher.wellbeing.domain.model.NotificationFilter
import dev.minimalist.launcher.wellbeing.domain.model.NotificationFilterMode
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellbeingRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appTimerDao: AppTimerDao,
    private val blockScheduleDao: BlockScheduleDao,
    private val notificationFilterDao: NotificationFilterDao
) : WellbeingRepository {

    private val usageStatsManager: UsageStatsManager
        get() = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    // ── Usage Stats ──────────────────────────────────────────────────────────

    override fun isUsageAccessGranted(): Flow<Boolean> = flow {
        while (true) {
            emit(checkUsageAccess())
            delay(2000) // Re-check periodically
        }
    }

    private fun checkUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun getDailyUsageMs(packageName: String): Result<Long> = runCatchingResult {
        if (!checkUsageAccess()) return@runCatchingResult 0L
        val (start, end) = todayRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, start, end
        )
        stats.firstOrNull { it.packageName == packageName }?.totalTimeInForeground ?: 0L
    }

    override suspend fun getDailyLaunchCount(packageName: String): Result<Int> = runCatchingResult {
        if (!checkUsageAccess()) return@runCatchingResult 0
        val (start, end) = todayRange()
        val events = usageStatsManager.queryEvents(start, end)
        var count = 0
        val event = android.app.usage.UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName &&
                event.eventType == android.app.usage.UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                count++
            }
        }
        count
    }

    override suspend fun getAllDailyUsageMs(): Result<Map<String, Long>> = runCatchingResult {
        if (!checkUsageAccess()) return@runCatchingResult emptyMap()
        val (start, end) = todayRange()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, start, end
        )
        stats.filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to it.totalTimeInForeground }
    }

    private fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        return start to end
    }

    // ── App Timers ───────────────────────────────────────────────────────────

    override fun getAllTimers(): Flow<List<AppTimer>> =
        appTimerDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getEnabledTimers(): Flow<List<AppTimer>> =
        appTimerDao.getEnabled().map { list -> list.map { it.toDomain() } }

    override fun observeTimerForApp(packageName: String): Flow<AppTimer?> =
        appTimerDao.observeForApp(packageName).map { it?.toDomain() }

    override suspend fun getTimerForApp(packageName: String): AppTimer? =
        appTimerDao.getForApp(packageName)?.toDomain()

    override suspend fun upsertTimer(timer: AppTimer): Result<Unit> = runCatchingResult {
        appTimerDao.upsert(timer.toEntity())
    }

    override suspend fun deleteTimer(packageName: String): Result<Unit> = runCatchingResult {
        appTimerDao.delete(packageName)
    }

    // ── Block Schedules ──────────────────────────────────────────────────────

    override fun getAllSchedules(): Flow<List<BlockSchedule>> =
        blockScheduleDao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getEnabledSchedules(): Flow<List<BlockSchedule>> =
        blockScheduleDao.getEnabled().map { list -> list.map { it.toDomain() } }

    override suspend fun getScheduleById(id: Long): BlockSchedule? =
        blockScheduleDao.getById(id)?.toDomain()

    override suspend fun upsertSchedule(schedule: BlockSchedule): Result<Long> = runCatchingResult {
        blockScheduleDao.upsert(schedule.toEntity())
    }

    override suspend fun deleteSchedule(id: Long): Result<Unit> = runCatchingResult {
        blockScheduleDao.delete(id)
    }

    override suspend fun setScheduleEnabled(id: Long, enabled: Boolean): Result<Unit> = runCatchingResult {
        blockScheduleDao.setEnabled(id, enabled)
    }

    // ── Notification Filters ─────────────────────────────────────────────────

    override fun getAllNotificationFilters(): Flow<List<NotificationFilter>> =
        notificationFilterDao.getAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getNotificationFilter(packageName: String): NotificationFilter? =
        notificationFilterDao.getForApp(packageName)?.toDomain()

    override suspend fun upsertNotificationFilter(filter: NotificationFilter): Result<Unit> = runCatchingResult {
        notificationFilterDao.upsert(filter.toEntity())
    }

    override suspend fun deleteNotificationFilter(packageName: String): Result<Unit> = runCatchingResult {
        notificationFilterDao.delete(packageName)
    }

    // ── Mappers ──────────────────────────────────────────────────────────────

    private fun AppTimerEntity.toDomain() = AppTimer(
        packageName = packageName,
        dailyLimitMinutes = dailyLimitMinutes,
        timerBehaviour = TimerBehaviour.fromString(timerBehaviour),
        cooldownSeconds = cooldownSeconds,
        launchDelayEnabled = launchDelayEnabled,
        launchDelaySeconds = launchDelaySeconds,
        isEnabled = isEnabled
    )

    private fun AppTimer.toEntity() = AppTimerEntity(
        packageName = packageName,
        dailyLimitMinutes = dailyLimitMinutes,
        timerBehaviour = timerBehaviour.name,
        cooldownSeconds = cooldownSeconds,
        launchDelayEnabled = launchDelayEnabled,
        launchDelaySeconds = launchDelaySeconds,
        isEnabled = isEnabled
    )

    private fun BlockScheduleEntity.toDomain() = BlockSchedule(
        id = id,
        packageNames = packageNames.split(",").filter { it.isNotBlank() },
        scheduleName = scheduleName,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        activeDays = DayOfWeek.fromBitmask(activeDays),
        isEnabled = isEnabled,
        lastModifiedMs = lastModifiedMs
    )

    private fun BlockSchedule.toEntity() = BlockScheduleEntity(
        id = id,
        packageNames = packageNames.joinToString(","),
        scheduleName = scheduleName,
        startMinutes = startMinutes,
        endMinutes = endMinutes,
        activeDays = DayOfWeek.toBitmask(activeDays),
        isEnabled = isEnabled,
        lastModifiedMs = lastModifiedMs
    )

    private fun NotificationFilterEntity.toDomain() = NotificationFilter(
        packageName = packageName,
        filterMode = NotificationFilterMode.fromString(filterMode),
        batchIntervalMinutes = batchIntervalMinutes,
        hideBadge = hideBadge
    )

    private fun NotificationFilter.toEntity() = NotificationFilterEntity(
        packageName = packageName,
        filterMode = filterMode.name,
        batchIntervalMinutes = batchIntervalMinutes,
        hideBadge = hideBadge
    )
}
