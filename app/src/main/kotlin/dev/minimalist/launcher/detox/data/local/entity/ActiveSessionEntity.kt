package dev.minimalist.launcher.detox.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Active (or cooldown) session for a monitored app.
 * Stored so sessions survive process death.
 *
 * Timing model (Section 6.1):
 *   deadline_elapsed_ms  = SystemClock.elapsedRealtime() value at which the session expires.
 *   pause_elapsed_ms     = SystemClock.elapsedRealtime() value at which session was paused
 *                          (0 = not paused).
 *
 * Wall-clock fields (sessionStartEpochMs) are kept only for display/telemetry.
 */
@Entity(tableName = "active_sessions")
data class ActiveSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "session_start_epoch_ms")
    val sessionStartEpochMs: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "allocated_duration_ms")
    val allocatedDurationMs: Long = 5 * 60_000L, // default 5 min

    @ColumnInfo(name = "cooldown_until_epoch_ms")
    val cooldownUntilEpochMs: Long = 0L,

    @ColumnInfo(name = "intent_reason")
    val intentReason: String = "Browsing out of habit",

    @ColumnInfo(name = "scroll_events_count")
    val scrollEventsCount: Int = 0,

    /** Current scroll decay overlay alpha [0.0, 0.78] — written by service, read by HUD. */
    @ColumnInfo(name = "scroll_decay_alpha")
    val scrollDecayAlpha: Float = 0f,

    /**
     * Monotonic deadline: SystemClock.elapsedRealtime() value at which the session expires.
     * 0 means not set (legacy row — compute from sessionStartEpochMs for backwards compat).
     */
    @ColumnInfo(name = "deadline_elapsed_ms")
    val deadlineElapsedMs: Long = 0L,

    /**
     * Monotonic pause timestamp: SystemClock.elapsedRealtime() when the session was paused.
     * 0 means the session is not paused.
     */
    @ColumnInfo(name = "pause_elapsed_ms")
    val pauseElapsedMs: Long = 0L,
)
