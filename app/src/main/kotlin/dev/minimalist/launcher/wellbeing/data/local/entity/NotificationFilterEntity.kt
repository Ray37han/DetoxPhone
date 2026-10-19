package dev.minimalist.launcher.wellbeing.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for per-app notification filter settings.
 *
 * Controls how notifications from each app are handled:
 * allow, batch (show only at intervals), or block entirely.
 */
@Entity(tableName = "notification_filters")
data class NotificationFilterEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    /** "ALLOW" | "BATCH" | "BLOCK" */
    @ColumnInfo(name = "filter_mode")
    val filterMode: String = "ALLOW",

    /** Batch interval in minutes. Only used when filterMode = "BATCH". */
    @ColumnInfo(name = "batch_interval_minutes")
    val batchIntervalMinutes: Int = 60,

    /** Whether to hide badge counts for this app. */
    @ColumnInfo(name = "hide_badge")
    val hideBadge: Boolean = false
)
