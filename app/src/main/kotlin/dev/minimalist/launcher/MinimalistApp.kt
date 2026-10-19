package dev.minimalist.launcher

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import dev.minimalist.launcher.detox.service.DigestWorker
import javax.inject.Inject

/**
 * Application class — entry point for Hilt dependency injection.
 *
 * Implements [Configuration.Provider] so WorkManager uses [HiltWorkerFactory]
 * for injecting [DigestWorker] with Hilt dependencies.
 *
 * DigestWorker is scheduled here so the 13:00 and 20:00 digest alarms are
 * registered on every app launch (idempotent via KEEP policy).
 */
@HiltAndroidApp
class MinimalistApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Schedule idempotent digest workers for 13:00 and 20:00
        DigestWorker.schedule(this)
    }
}
