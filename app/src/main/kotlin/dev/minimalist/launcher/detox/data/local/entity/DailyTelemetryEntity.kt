package dev.minimalist.launcher.detox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

/**
 * Daily telemetry per app (or "__GLOBAL__" for phone unlocks).
 * Composite primary key: (date_ymd, package_name)
 */
@Entity(
    tableName = "daily_telemetry",
    primaryKeys = ["date_ymd", "package_name"]
)
data class DailyTelemetryEntity(
    @ColumnInfo(name = "date_ymd")
    val dateYmd: String, // "YYYY-MM-DD"

    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** Times user waited through friction and entered the app. */
    @ColumnInfo(name = "completed_opens")
    val completedOpens: Int = 0,

    /** Impulses Deflected: times user closed the friction screen. */
    @ColumnInfo(name = "aborted_opens")
    val abortedOpens: Int = 0,

    /** Times surgical short-video blocker triggered. */
    @ColumnInfo(name = "reels_blocked_count")
    val reelsBlockedCount: Int = 0,

    /** Exact foreground time in milliseconds from UsageStatsManager. */
    @ColumnInfo(name = "total_foreground_ms")
    val totalForegroundMs: Long = 0L,

    /** Estimated scroll distance in meters (scrollEventsCount * 0.18). */
    @ColumnInfo(name = "estimated_scroll_meters")
    val estimatedScrollMeters: Double = 0.0
)
