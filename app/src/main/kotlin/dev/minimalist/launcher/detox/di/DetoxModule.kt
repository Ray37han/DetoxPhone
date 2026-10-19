package dev.minimalist.launcher.detox.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.minimalist.launcher.detox.data.DetoxRepositoryImpl
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DetoxModule {

    @Binds
    @Singleton
    abstract fun bindDetoxRepository(impl: DetoxRepositoryImpl): DetoxRepository
}
