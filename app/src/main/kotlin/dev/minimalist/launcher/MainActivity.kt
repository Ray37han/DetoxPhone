package dev.minimalist.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.navigation.LauncherNavGraph
import dev.minimalist.launcher.ui.theme.LauncherTheme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import dev.minimalist.launcher.navigation.NavDestination
import dev.minimalist.launcher.settings.domain.repository.SettingsRepository
import javax.inject.Inject

/**
 * Single-Activity host for the Compose NavHost.
 * Declares HOME intent filter (see AndroidManifest.xml).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.getSettings().collectAsState(initial = null)
            val currentSettings = settings ?: return@setContent
            val navController = rememberNavController()

            LauncherTheme(
                themeMode = currentSettings.themeMode,
                animationsEnabled = currentSettings.animationsEnabled
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val start = if (currentSettings.onboardingComplete) {
                        NavDestination.Home
                    } else {
                        NavDestination.Onboarding
                    }
                    LauncherNavGraph(
                        navController = navController,
                        startDestination = start
                    )
                }
            }
        }
    }

}
