package dev.minimalist.launcher.apps.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [LauncherApp] domain model.
 */
class LauncherAppTest {

    @Test
    fun `displayName returns customName when set`() {
        val app = LauncherApp(
            packageName  = "com.example.app",
            activityName = "com.example.app.MainActivity",
            originalName = "Example App",
            customName   = "My App"
        )
        assertThat(app.displayName).isEqualTo("My App")
    }

    @Test
    fun `displayName returns originalName when customName is null`() {
        val app = LauncherApp(
            packageName  = "com.example.app",
            activityName = "com.example.app.MainActivity",
            originalName = "Example App",
            customName   = null
        )
        assertThat(app.displayName).isEqualTo("Example App")
    }

    @Test
    fun `displayName returns originalName when customName is blank`() {
        val app = LauncherApp(
            packageName  = "com.example.app",
            activityName = "com.example.app.MainActivity",
            originalName = "Example App",
            customName   = "   "
        )
        assertThat(app.displayName).isEqualTo("Example App")
    }

    @Test
    fun `default values are correct`() {
        val app = LauncherApp(
            packageName  = "com.example",
            activityName = "com.example.Main",
            originalName = "App"
        )
        assertThat(app.isHidden).isFalse()
        assertThat(app.isFavorite).isFalse()
        assertThat(app.folderId).isNull()
        assertThat(app.sortPosition).isEqualTo(0)
        assertThat(app.customName).isNull()
    }
}
