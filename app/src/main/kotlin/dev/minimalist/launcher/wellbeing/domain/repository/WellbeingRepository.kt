package dev.minimalist.launcher.wellbeing.domain.repository

import dev.minimalist.launcher.core.common.Result
import dev.minimalist.launcher.wellbeing.domain.model.AppTimer
import dev.minimalist.launcher.wellbeing.domain.model.BlockSchedule
import dev.minimalist.launcher.wellbeing.domain.model.NotificationFilter
import kotlinx.coroutines.flow.Flow

/**
 * Contract for wellbeing-related data: usage statistics, app limits,
 * blocking rules, notification filters, and schedule management.
 */
interface WellbeingRepository {

    // ── Usage Stats ──

    /** Whether PACKAGE_USAGE_STATS permission is granted. */
    fun isUsageAccessGranted(): Flow<Boolean>

    /** Total foreground usage in ms for [packageName] today. */
    suspend fun getDailyUsageMs(packageName: String): Result<Long>

    /** Number of launches for [packageName] today. */
    suspend fun getDailyLaunchCount(packageName: String): Result<Int>

    /** Daily usage for all apps, keyed by package name. */
    suspend fun getAllDailyUsageMs(): Result<Map<String, Long>>

    // ── App Timers ──

    fun getAllTimers(): Flow<List<AppTimer>>
    fun getEnabledTimers(): Flow<List<AppTimer>>
    fun observeTimerForApp(packageName: String): Flow<AppTimer?>
    suspend fun getTimerForApp(packageName: String): AppTimer?
    suspend fun upsertTimer(timer: AppTimer): Result<Unit>
    suspend fun deleteTimer(packageName: String): Result<Unit>

    // ── Block Schedules ──

    fun getAllSchedules(): Flow<List<BlockSchedule>>
    fun getEnabledSchedules(): Flow<List<BlockSchedule>>
    suspend fun getScheduleById(id: Long): BlockSchedule?
    suspend fun upsertSchedule(schedule: BlockSchedule): Result<Long>
    suspend fun deleteSchedule(id: Long): Result<Unit>
    suspend fun setScheduleEnabled(id: Long, enabled: Boolean): Result<Unit>

    // ── Notification Filters ──

    fun getAllNotificationFilters(): Flow<List<NotificationFilter>>
    suspend fun getNotificationFilter(packageName: String): NotificationFilter?
    suspend fun upsertNotificationFilter(filter: NotificationFilter): Result<Unit>
    suspend fun deleteNotificationFilter(packageName: String): Result<Unit>
}
