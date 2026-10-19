package dev.minimalist.launcher.wellbeing.domain.model

import dev.minimalist.launcher.settings.domain.model.TimerBehaviour

/**
 * Domain model for a per-app usage timer configuration.
 */
data class AppTimer(
    val packageName: String,
    val dailyLimitMinutes: Int = 0,
    val timerBehaviour: TimerBehaviour = TimerBehaviour.ONLY_REMIND,
    val cooldownSeconds: Int = 30,
    val launchDelayEnabled: Boolean = false,
    val launchDelaySeconds: Int = 5,
    val isEnabled: Boolean = true
)

/**
 * Domain model for a time-based blocking schedule.
 */
data class BlockSchedule(
    val id: Long = 0,
    val packageNames: List<String>,
    val scheduleName: String,
    val startMinutes: Int,
    val endMinutes: Int,
    val activeDays: Set<DayOfWeek> = DayOfWeek.ALL,
    val isEnabled: Boolean = true,
    val lastModifiedMs: Long = System.currentTimeMillis()
) {
    /** Check if this schedule is active right now. */
    fun isActiveNow(currentMinuteOfDay: Int, currentDay: DayOfWeek): Boolean {
        if (!isEnabled) return false
        if (currentDay !in activeDays) return false
        return if (startMinutes <= endMinutes) {
            currentMinuteOfDay in startMinutes..endMinutes
        } else {
            // Wraps midnight (e.g. 22:00 → 06:00)
            currentMinuteOfDay >= startMinutes || currentMinuteOfDay <= endMinutes
        }
    }
}

/** Days of the week with bitmask support. */
enum class DayOfWeek(val bit: Int) {
    SUNDAY(0x01),
    MONDAY(0x02),
    TUESDAY(0x04),
    WEDNESDAY(0x08),
    THURSDAY(0x10),
    FRIDAY(0x20),
    SATURDAY(0x40);

    companion object {
        val ALL: Set<DayOfWeek> = entries.toSet()

        fun fromBitmask(mask: Int): Set<DayOfWeek> =
            entries.filter { mask and it.bit != 0 }.toSet()

        fun toBitmask(days: Set<DayOfWeek>): Int =
            days.fold(0) { acc, day -> acc or day.bit }
    }
}

/**
 * Domain model for per-app notification filter settings.
 */
data class NotificationFilter(
    val packageName: String,
    val filterMode: NotificationFilterMode = NotificationFilterMode.ALLOW,
    val batchIntervalMinutes: Int = 60,
    val hideBadge: Boolean = false
)

enum class NotificationFilterMode {
    ALLOW, BATCH, BLOCK;

    companion object {
        fun fromString(value: String?): NotificationFilterMode =
            entries.firstOrNull { it.name == value } ?: ALLOW
    }
}

/**
 * Reels/Shorts blocking status for a specific platform.
 */
enum class ReelsBlockingStatus {
    ENABLED, DISABLED, UNKNOWN
}

/**
 * Configuration for short video blocking on a specific platform.
 */
data class ShortVideoBlockingSetting(
    val platformPackage: String,
    val platformName: String,
    val status: ReelsBlockingStatus = ReelsBlockingStatus.DISABLED
)
