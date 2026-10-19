package dev.minimalist.launcher.detox.domain.model

/** Protection level for a monitored app. */
enum class ProtectionLevel(val value: Int) {
    NONE(0),
    FRICTION_ONLY(1),        // Friction gate (delay + intent prompt)
    FRICTION_TIMEBOX(2),     // Friction + daily budget
    FULL_BLOCK(3);           // Hard blocked

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: NONE
    }
}

/** Strict-mode unlock method. */
enum class UnlockMethod(val key: String) {
    TIMER_60S("TIMER_60S"),
    QR_CODE("QR_CODE"),
    PARTNER_PIN("PARTNER_PIN");

    companion object {
        fun fromKey(k: String) = entries.firstOrNull { it.key == k } ?: TIMER_60S
    }
}

/** Domain model for a monitored/protected app. */
data class MonitoredApp(
    val packageName: String,
    val customAlias: String? = null,
    val protectionLevel: ProtectionLevel = ProtectionLevel.NONE,
    val blockShortVideos: Boolean = false,
    val enableScrollDecay: Boolean = false,
    val forceGrayscale: Boolean = false,
    val dailyBudgetMinutes: Int = 30,
    val cooldownMinutes: Int = 10,
    val substitutePackage: String? = null,
    val batchNotifications: Boolean = false
)

/** Domain model for an active or cooldown session. */
data class ActiveSession(
    val packageName: String,
    val sessionStartEpochMs: Long = System.currentTimeMillis(),
    val allocatedDurationMs: Long = 5 * 60_000L,
    val cooldownUntilEpochMs: Long = 0L,
    val intentReason: String = "Browsing out of habit",
    val scrollEventsCount: Int = 0,
    /**
     * Monotonic deadline from SystemClock.elapsedRealtime().
     * 0 = legacy row; fall back to wall-clock calculation.
     */
    val deadlineElapsedMs: Long = 0L,
    /** Non-zero when session is paused. */
    val pauseElapsedMs: Long = 0L
) {
    val isInCooldown: Boolean get() = cooldownUntilEpochMs > System.currentTimeMillis()

    /**
     * Session is expired when monotonic deadline has passed (Section 6.1).
     * Falls back to wall-clock for legacy rows where deadlineElapsedMs == 0.
     */
    val isExpired: Boolean
        get() {
            if (cooldownUntilEpochMs > 0L) return false
            return if (deadlineElapsedMs > 0L) {
                android.os.SystemClock.elapsedRealtime() >= deadlineElapsedMs
            } else {
                System.currentTimeMillis() - sessionStartEpochMs > allocatedDurationMs
            }
        }

    /**
     * Remaining milliseconds derived from monotonic deadline.
     * Falls back to wall-clock for legacy rows.
     */
    val remainingMs: Long
        get() = if (deadlineElapsedMs > 0L) {
            maxOf(0L, deadlineElapsedMs - android.os.SystemClock.elapsedRealtime())
        } else {
            maxOf(0L, (sessionStartEpochMs + allocatedDurationMs) - System.currentTimeMillis())
        }

    val cooldownRemainingMs: Long get() = cooldownUntilEpochMs - System.currentTimeMillis()
}

/** Daily stats snapshot for one app on one day. */
data class DailyTelemetry(
    val dateYmd: String,
    val packageName: String,
    val completedOpens: Int = 0,
    val abortedOpens: Int = 0,
    val reelsBlockedCount: Int = 0,
    val totalForegroundMs: Long = 0L,
    val estimatedScrollMeters: Double = 0.0
)

/** An intercepted notification stored in the local vault. */
data class VaultedNotification(
    val id: Long = 0,
    val packageName: String,
    val senderTitle: String,
    val messageBody: String,
    val postedEpochMs: Long = System.currentTimeMillis(),
    val batchWindowId: String = "",
    val isRead: Boolean = false
)

/** Strict Mode configuration (commitment lock). */
data class StrictModeConfig(
    val isActive: Boolean = false,
    val unlockMethod: UnlockMethod = UnlockMethod.TIMER_60S,
    val qrHashSecret: String? = null,
    val activeUntilEpochMs: Long = 0L
) {
    val isExpired: Boolean get() = activeUntilEpochMs > 0L && System.currentTimeMillis() > activeUntilEpochMs
}

/** Intent reasons shown in friction gate (plan Pillar B, Step C). Neutral, nonjudgmental copy. */
val INTENT_REASONS = listOf(
    "Reply to a message",
    "Work or task",
    "Quick news check",
    "Short break",
    "Browsing out of habit",
    "Entertainment"
)

/** The habitual-browsing reason key — caps timebox to min(2 min, budgetRemaining). */
const val BOREDOM_INTENT = "Browsing out of habit"

/** Micro-timebox options in minutes → ms. */
val TIMEBOX_OPTIONS_MIN = listOf(1, 5, 10, 15)
