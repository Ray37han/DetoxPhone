package dev.minimalist.launcher.detox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Intercepted notifications held in the local vault.
 * Used by the Batched Notification Digest Drawer.
 * Never transmitted over network — 100% on-device.
 */
@Entity(tableName = "notification_vault")
data class NotificationVaultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "sender_title")
    val senderTitle: String,

    @ColumnInfo(name = "message_body")
    val messageBody: String,

    @ColumnInfo(name = "posted_epoch_ms")
    val postedEpochMs: Long = System.currentTimeMillis(),

    /** Assigned digest batch window e.g. "2026-10-16_14:00" */
    @ColumnInfo(name = "batch_window_id")
    val batchWindowId: String = "",

    @ColumnInfo(name = "is_read")
    val isRead: Boolean = false
)
