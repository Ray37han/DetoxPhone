package dev.minimalist.launcher.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.minimalist.launcher.apps.data.local.dao.AppFolderDao
import dev.minimalist.launcher.apps.data.local.dao.FavoriteAppDao
import dev.minimalist.launcher.apps.data.local.dao.HiddenAppDao
import dev.minimalist.launcher.apps.data.local.dao.RenamedAppDao
import dev.minimalist.launcher.apps.data.local.entity.AppFolderEntity
import dev.minimalist.launcher.apps.data.local.entity.AppFolderMemberEntity
import dev.minimalist.launcher.apps.data.local.entity.FavoriteAppEntity
import dev.minimalist.launcher.apps.data.local.entity.HiddenAppEntity
import dev.minimalist.launcher.apps.data.local.entity.RenamedAppEntity
import dev.minimalist.launcher.detox.data.local.dao.ActiveSessionDao
import dev.minimalist.launcher.detox.data.local.dao.DailyTelemetryDao
import dev.minimalist.launcher.detox.data.local.dao.MonitoredAppDao
import dev.minimalist.launcher.detox.data.local.dao.NotificationVaultDao
import dev.minimalist.launcher.detox.data.local.dao.StrictModeConfigDao
import dev.minimalist.launcher.detox.data.local.entity.ActiveSessionEntity
import dev.minimalist.launcher.detox.data.local.entity.DailyTelemetryEntity
import dev.minimalist.launcher.detox.data.local.entity.MonitoredAppEntity
import dev.minimalist.launcher.detox.data.local.entity.NotificationVaultEntity
import dev.minimalist.launcher.detox.data.local.entity.StrictModeConfigEntity
import dev.minimalist.launcher.wellbeing.data.local.dao.AppTimerDao
import dev.minimalist.launcher.wellbeing.data.local.dao.BlockScheduleDao
import dev.minimalist.launcher.wellbeing.data.local.dao.NotificationFilterDao
import dev.minimalist.launcher.wellbeing.data.local.entity.AppTimerEntity
import dev.minimalist.launcher.wellbeing.data.local.entity.BlockScheduleEntity
import dev.minimalist.launcher.wellbeing.data.local.entity.NotificationFilterEntity

/**
 * Central Room database for the launcher.
 *
 * Version history:
 *  1 → Initial schema (app metadata + basic wellbeing)
 *  2 → Added Scientific Detox Engine tables:
 *       monitored_apps, active_sessions, daily_telemetry,
 *       notification_vault, strict_mode_config
 *  3 → Added batch_notifications (monitored_apps), scroll_decay_alpha (active_sessions)
 *  4 → Added deadline_elapsed_ms, pause_elapsed_ms (active_sessions) for monotonic timing
 */
@Database(
    entities = [
        // App metadata
        RenamedAppEntity::class,
        HiddenAppEntity::class,
        FavoriteAppEntity::class,
        AppFolderEntity::class,
        AppFolderMemberEntity::class,
        // Legacy wellbeing
        AppTimerEntity::class,
        BlockScheduleEntity::class,
        NotificationFilterEntity::class,
        // Scientific Detox Engine (v2/v3/v4)
        MonitoredAppEntity::class,
        ActiveSessionEntity::class,
        DailyTelemetryEntity::class,
        NotificationVaultEntity::class,
        StrictModeConfigEntity::class,
    ],
    version = 4,
    exportSchema = true
)
abstract class LauncherDatabase : RoomDatabase() {

    // ── App metadata DAOs ──
    abstract fun renamedAppDao(): RenamedAppDao
    abstract fun hiddenAppDao(): HiddenAppDao
    abstract fun favoriteAppDao(): FavoriteAppDao
    abstract fun appFolderDao(): AppFolderDao

    // ── Legacy wellbeing DAOs ──
    abstract fun appTimerDao(): AppTimerDao
    abstract fun blockScheduleDao(): BlockScheduleDao
    abstract fun notificationFilterDao(): NotificationFilterDao

    // ── Scientific Detox Engine DAOs ──
    abstract fun monitoredAppDao(): MonitoredAppDao
    abstract fun activeSessionDao(): ActiveSessionDao
    abstract fun dailyTelemetryDao(): DailyTelemetryDao
    abstract fun notificationVaultDao(): NotificationVaultDao
    abstract fun strictModeConfigDao(): StrictModeConfigDao

    companion object {
        /**
         * Migration 1→2: Add Scientific Detox Engine tables.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS monitored_apps (
                        package_name TEXT PRIMARY KEY NOT NULL,
                        custom_alias TEXT,
                        protection_level INTEGER NOT NULL DEFAULT 0,
                        block_short_videos INTEGER NOT NULL DEFAULT 0,
                        enable_scroll_decay INTEGER NOT NULL DEFAULT 0,
                        force_grayscale INTEGER NOT NULL DEFAULT 0,
                        daily_budget_minutes INTEGER NOT NULL DEFAULT 30,
                        cooldown_minutes INTEGER NOT NULL DEFAULT 10,
                        substitute_package TEXT
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS active_sessions (
                        package_name TEXT PRIMARY KEY NOT NULL,
                        session_start_epoch_ms INTEGER NOT NULL DEFAULT 0,
                        allocated_duration_ms INTEGER NOT NULL DEFAULT 300000,
                        cooldown_until_epoch_ms INTEGER NOT NULL DEFAULT 0,
                        intent_reason TEXT NOT NULL DEFAULT 'Boredom',
                        scroll_events_count INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS daily_telemetry (
                        date_ymd TEXT NOT NULL,
                        package_name TEXT NOT NULL,
                        completed_opens INTEGER NOT NULL DEFAULT 0,
                        aborted_opens INTEGER NOT NULL DEFAULT 0,
                        reels_blocked_count INTEGER NOT NULL DEFAULT 0,
                        total_foreground_ms INTEGER NOT NULL DEFAULT 0,
                        estimated_scroll_meters REAL NOT NULL DEFAULT 0.0,
                        PRIMARY KEY (date_ymd, package_name)
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS notification_vault (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        package_name TEXT NOT NULL,
                        sender_title TEXT NOT NULL,
                        message_body TEXT NOT NULL,
                        posted_epoch_ms INTEGER NOT NULL DEFAULT 0,
                        batch_window_id TEXT NOT NULL DEFAULT '',
                        is_read INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS strict_mode_config (
                        id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                        is_active INTEGER NOT NULL DEFAULT 0,
                        unlock_method TEXT NOT NULL DEFAULT 'TIMER_60S',
                        qr_hash_secret TEXT,
                        active_until_epoch_ms INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Insert default strict mode config row
                db.execSQL("""
                    INSERT OR IGNORE INTO strict_mode_config (id, is_active, unlock_method, active_until_epoch_ms)
                    VALUES (1, 0, 'TIMER_60S', 0)
                """.trimIndent())
            }
        }
        /**
         * Migration 2→3: Add batch_notifications to monitored_apps,
         * scroll_decay_alpha to active_sessions.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE monitored_apps ADD COLUMN batch_notifications INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE active_sessions ADD COLUMN scroll_decay_alpha REAL NOT NULL DEFAULT 0.0"
                )
            }
        }

        /**
         * Migration 3→4: Add monotonic timing columns to active_sessions (Section 6.1).
         *   deadline_elapsed_ms — SystemClock.elapsedRealtime() deadline; 0 = legacy row.
         *   pause_elapsed_ms    — elapsedRealtime() when paused; 0 = not paused.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE active_sessions ADD COLUMN deadline_elapsed_ms INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE active_sessions ADD COLUMN pause_elapsed_ms INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
