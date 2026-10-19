package dev.minimalist.launcher.testing

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Custom [AndroidJUnitRunner] that replaces the [Application] with
 * [HiltTestApplication] so Hilt can inject dependencies in instrumented tests.
 *
 * Configured in app/build.gradle.kts:
 *   testInstrumentationRunner = "dev.minimalist.launcher.testing.HiltTestRunner"
 */
class HiltTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
