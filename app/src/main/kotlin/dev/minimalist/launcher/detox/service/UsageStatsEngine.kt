package dev.minimalist.launcher.detox.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * UsageStatsEngine — Millisecond-accurate screen time and unlock count.
 *
 * Uses queryEvents() instead of queryUsageStats() to avoid the midnight-bucket
 * inaccuracy bug described in plan Section 6.6.2.
 */
object UsageStatsEngine {

    data class DailyUsageSummary(
        val unlockCount: Int,
        /** packageName → total foreground milliseconds */
        val foregroundMsMap: Map<String, Long>
    )

    /**
     * Query all usage events from [startOfDayMs] to [nowMs].
     * Requires PACKAGE_USAGE_STATS permission (granted via Settings).
     */
    fun queryDailyMetrics(
        context: Context,
        startOfDayMs: Long,
        nowMs: Long
    ): DailyUsageSummary {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val events = usm.queryEvents(startOfDayMs, nowMs)
        val event = UsageEvents.Event()

        var unlockCount = 0
        val foregroundStartMap = mutableMapOf<String, Long>()
        val totalDurationMap    = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            when (event.eventType) {
                UsageEvents.Event.KEYGUARD_HIDDEN -> unlockCount++
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    foregroundStartMap[event.packageName] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = foregroundStartMap.remove(event.packageName)
                    if (start != null && event.timeStamp > start) {
                        totalDurationMap[event.packageName] =
                            (totalDurationMap[event.packageName] ?: 0L) + (event.timeStamp - start)
                    }
                }
            }
        }
        return DailyUsageSummary(unlockCount, totalDurationMap)
    }

    /**
     * Returns the start-of-today epoch in milliseconds (midnight local time).
     */
    fun startOfTodayMs(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }
}
