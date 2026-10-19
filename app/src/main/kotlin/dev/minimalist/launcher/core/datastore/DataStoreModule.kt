package dev.minimalist.launcher.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Extension property that creates a single DataStore<Preferences> instance
 * scoped to the application context.
 *
 * The DataStore file is named "launcher_preferences".
 * Do not create multiple instances of DataStore for the same file —
 * the [preferencesDataStore] delegate enforces this at the application level.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "launcher_preferences"
)

/**
 * Hilt module providing [DataStore<Preferences>] as a singleton.
 *
 * Why not [dagger.hilt.android.qualifiers.ApplicationContext] directly on DataStore?
 * — The [preferencesDataStore] delegate must be created exactly once per process;
 *   routing it through Hilt ensures that.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
}
