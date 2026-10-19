package dev.minimalist.launcher.detox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Persisted config for every app enrolled in the Scientific Detox Engine.
 *
 * protection_level:
 *   0 = None
 *   1 = Friction Only (delay + intent prompt)
 *   2 = Friction + Timebox (daily budget)
 *   3 = Full Block
 */
@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "custom_alias")
    val customAlias: String? = null,

    @ColumnInfo(name = "protection_level")
    val protectionLevel: Int = 0,

    @ColumnInfo(name = "block_short_videos")
    val blockShortVideos: Boolean = false,

    @ColumnInfo(name = "enable_scroll_decay")
    val enableScrollDecay: Boolean = false,

    @ColumnInfo(name = "force_grayscale")
    val forceGrayscale: Boolean = false,

    @ColumnInfo(name = "daily_budget_minutes")
    val dailyBudgetMinutes: Int = 30,

    @ColumnInfo(name = "cooldown_minutes")
    val cooldownMinutes: Int = 10,

    @ColumnInfo(name = "substitute_package")
    val substitutePackage: String? = null,

    /** Whether notifications from this app should be silently batched into the vault. */
    @ColumnInfo(name = "batch_notifications")
    val batchNotifications: Boolean = false
)
