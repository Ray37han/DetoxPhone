package dev.minimalist.launcher.wellbeing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for per-app usage timers and friction rules.
 *
 * Each row configures how the launcher intervenes when the user
 * spends time in a specific app.
 */
@Entity(tableName = "app_timers")
data class AppTimerEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** Daily allowed usage in minutes. 0 = unlimited. */
    @ColumnInfo(name = "daily_limit_minutes")
    val dailyLimitMinutes: Int = 0,

    /** "ONLY_REMIND" | "COOLDOWN_PERIOD" | "EXIT_APP" */
    @ColumnInfo(name = "timer_behaviour")
    val timerBehaviour: String = "ONLY_REMIND",

    /** Cooldown duration in seconds before app can be reopened. */
    @ColumnInfo(name = "cooldown_seconds")
    val cooldownSeconds: Int = 30,

    /** Whether to show a launch delay countdown before opening. */
    @ColumnInfo(name = "launch_delay_enabled")
    val launchDelayEnabled: Boolean = false,

    /** Launch delay duration in seconds. */
    @ColumnInfo(name = "launch_delay_seconds")
    val launchDelaySeconds: Int = 5,

    /** Whether this timer rule is currently active. */
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true
)
