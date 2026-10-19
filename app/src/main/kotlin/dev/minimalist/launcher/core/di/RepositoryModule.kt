package dev.minimalist.launcher.core.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.minimalist.launcher.apps.data.AppRepositoryImpl
import dev.minimalist.launcher.apps.domain.repository.AppRepository
import dev.minimalist.launcher.settings.data.SettingsRepositoryImpl
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import dev.minimalist.launcher.wellbeing.data.WellbeingRepositoryImpl
import dev.minimalist.launcher.wellbeing.domain.repository.WellbeingRepository
import javax.inject.Singleton

/**
 * Hilt module binding repository interfaces to their implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAppRepository(impl: AppRepositoryImpl): AppRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindWellbeingRepository(impl: WellbeingRepositoryImpl): WellbeingRepository
}
