package dev.minimalist.launcher.detox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Strict Mode (Commitment Lock) config.
 * Single-row table — always upsert with id=1.
 *
 * unlock_method: "TIMER_60S" | "QR_CODE" | "PARTNER_PIN"
 */
@Entity(tableName = "strict_mode_config")
data class StrictModeConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = 1,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    @ColumnInfo(name = "unlock_method")
    val unlockMethod: String = "TIMER_60S",

    @ColumnInfo(name = "qr_hash_secret")
    val qrHashSecret: String? = null,

    @ColumnInfo(name = "active_until_epoch_ms")
    val activeUntilEpochMs: Long = 0L
)
