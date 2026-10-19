package dev.minimalist.launcher.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.minimalist.launcher.apps.data.local.dao.AppFolderDao
import dev.minimalist.launcher.apps.data.local.dao.FavoriteAppDao
import dev.minimalist.launcher.apps.data.local.dao.HiddenAppDao
import dev.minimalist.launcher.apps.data.local.dao.RenamedAppDao
import dev.minimalist.launcher.detox.data.local.dao.ActiveSessionDao
import dev.minimalist.launcher.detox.data.local.dao.DailyTelemetryDao
import dev.minimalist.launcher.detox.data.local.dao.MonitoredAppDao
import dev.minimalist.launcher.detox.data.local.dao.NotificationVaultDao
import dev.minimalist.launcher.detox.data.local.dao.StrictModeConfigDao
import dev.minimalist.launcher.wellbeing.data.local.dao.AppTimerDao
import dev.minimalist.launcher.wellbeing.data.local.dao.BlockScheduleDao
import dev.minimalist.launcher.wellbeing.data.local.dao.NotificationFilterDao
import javax.inject.Singleton

/**
 * Hilt module providing the [LauncherDatabase] singleton and all DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private const val DATABASE_NAME = "minimalist_launcher.db"

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): LauncherDatabase = Room.databaseBuilder(
        context,
        LauncherDatabase::class.java,
        DATABASE_NAME
    )
        .addMigrations(
            LauncherDatabase.MIGRATION_1_2,
            LauncherDatabase.MIGRATION_2_3,
            LauncherDatabase.MIGRATION_3_4
        )
        .fallbackToDestructiveMigration()
        .build()

    // ── App metadata DAOs ──

    @Provides
    fun provideRenamedAppDao(db: LauncherDatabase): RenamedAppDao = db.renamedAppDao()

    @Provides
    fun provideHiddenAppDao(db: LauncherDatabase): HiddenAppDao = db.hiddenAppDao()

    @Provides
    fun provideFavoriteAppDao(db: LauncherDatabase): FavoriteAppDao = db.favoriteAppDao()

    @Provides
    fun provideAppFolderDao(db: LauncherDatabase): AppFolderDao = db.appFolderDao()

    // ── Legacy wellbeing DAOs ──

    @Provides
    fun provideAppTimerDao(db: LauncherDatabase): AppTimerDao = db.appTimerDao()

    @Provides
    fun provideBlockScheduleDao(db: LauncherDatabase): BlockScheduleDao = db.blockScheduleDao()

    @Provides
    fun provideNotificationFilterDao(db: LauncherDatabase): NotificationFilterDao = db.notificationFilterDao()

    // ── Scientific Detox Engine DAOs ──

    @Provides
    fun provideMonitoredAppDao(db: LauncherDatabase): MonitoredAppDao = db.monitoredAppDao()

    @Provides
    fun provideActiveSessionDao(db: LauncherDatabase): ActiveSessionDao = db.activeSessionDao()

    @Provides
    fun provideDailyTelemetryDao(db: LauncherDatabase): DailyTelemetryDao = db.dailyTelemetryDao()

    @Provides
    fun provideNotificationVaultDao(db: LauncherDatabase): NotificationVaultDao = db.notificationVaultDao()

    @Provides
    fun provideStrictModeConfigDao(db: LauncherDatabase): StrictModeConfigDao = db.strictModeConfigDao()
}
