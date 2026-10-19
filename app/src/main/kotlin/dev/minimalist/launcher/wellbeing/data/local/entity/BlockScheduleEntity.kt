package dev.minimalist.launcher.wellbeing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for time-based app blocking schedules.
 *
 * Each schedule defines a recurring time window during which
 * specified apps are blocked. Supports weekday selection.
 */
@Entity(tableName = "block_schedules")
data class BlockScheduleEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    /** Comma-separated package names this schedule applies to. */
    @ColumnInfo(name = "package_names")
    val packageNames: String,

    /** Display name for this schedule (e.g. "Work Hours", "Sleep Time"). */
    @ColumnInfo(name = "schedule_name")
    val scheduleName: String,

    /** Start time as minutes since midnight (0–1439). */
    @ColumnInfo(name = "start_minutes")
    val startMinutes: Int,

    /** End time as minutes since midnight (0–1439). */
    @ColumnInfo(name = "end_minutes")
    val endMinutes: Int,

    /**
     * Bitmask for active days. Bit 0 = Sunday, Bit 1 = Monday, ..., Bit 6 = Saturday.
     * 0x7F = every day.
     */
    @ColumnInfo(name = "active_days")
    val activeDays: Int = 0x7F,

    /** Whether this schedule is currently active. */
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    /**
     * Epoch ms when this schedule was last modified.
     * Used for anti-bypass delay enforcement.
     */
    @ColumnInfo(name = "last_modified_ms")
    val lastModifiedMs: Long = System.currentTimeMillis()
)
