package dev.minimalist.launcher.ui.wellbeing

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimalist.launcher.ui.components.SettingsRow
import dev.minimalist.launcher.ui.components.SettingsSection
import dev.minimalist.launcher.ui.settings.SwitchRow

@Composable
fun WellbeingScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTimers: () -> Unit,
    onNavigateToSchedules: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToMonochrome: () -> Unit,
    onNavigateToDetox: () -> Unit = {},
    viewModel: WellbeingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Digital wellbeing",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Usage Access Warning ──
        if (!state.hasUsageAccess) {
            Text(
                text = "⚠ Usage access not granted. Tap to open settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .clickable {
                        context.startActivity(
                            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .padding(vertical = 8.dp)
            )
        }

        // ── Daily Screen Time Stats ──
        if (state.hasUsageAccess) {
            val totalMinutes = state.totalScreenTimeMs / (1000 * 60)
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            val timeText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

            SettingsSection(title = "Today's screen time") {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (state.topAppsUsage.isNotEmpty()) {
                    Text(
                        text = "Most used:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    state.topAppsUsage.forEach { app ->
                        val appMins = app.usageMs / (1000 * 60)
                        val appHours = appMins / 60
                        val appRemMins = appMins % 60
                        val appTimeStr = if (appHours > 0) "${appHours}h ${appRemMins}m" else "${appRemMins}m"
                        val simpleName = app.packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }

                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = simpleName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = appTimeStr,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── Core Toggles ──
        SettingsSection(title = "Precision Mode") {
            SwitchRow(
                title = "Precision Mode",
                checked = state.settings.precisionModeEnabled,
                onCheckedChange = { viewModel.setPrecisionMode(it) }
            )
            Text(
                text = "Tracks foreground apps to enforce timers and blocking rules. Requires Accessibility Service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── App Timers ──
        SettingsSection(title = "App Timers") {
            SettingsRow(
                title = "App timers",
                subtitle = "${state.timers.size} apps configured",
                onClick = onNavigateToTimers
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Scientific Detox Engine ──
        SettingsSection(title = "🧠 Scientific Detox Engine") {
            SettingsRow(
                title = "Detox Dashboard",
                subtitle = "Friction gates, timeboxes, stats & digest",
                onClick = onNavigateToDetox
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Block Schedules ──
        SettingsSection(title = "Blocking Schedules") {
            SettingsRow(
                title = "Block schedules",
                subtitle = "${state.schedules.size} schedules",
                onClick = onNavigateToSchedules
            )

            SwitchRow(
                title = "7-day challenge lock",
                checked = state.settings.blockingChallengeActive,
                onCheckedChange = { viewModel.setBlockingChallenge(it) }
            )
            Text(
                text = "Prevents disabling blocking rules for 7 days after activation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Content Blocking ──
        SettingsSection(title = "Content Blocking") {
            SwitchRow(
                title = "Block Reels / Shorts",
                checked = state.settings.reelsBlockingEnabled,
                onCheckedChange = { viewModel.setReelsBlocking(it) }
            )
            Text(
                text = "Blocks short video feeds in Instagram, YouTube, and TikTok. Requires Accessibility Service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Display & Notifications ──
        SettingsSection(title = "Display & Notifications") {
            SettingsRow(
                title = "Monochrome mode",
                subtitle = if (state.settings.monochromeModeActive) "Active" else "Off",
                onClick = onNavigateToMonochrome
            )

            SettingsRow(
                title = "Notification filtering",
                subtitle = "Control which apps can notify you",
                onClick = onNavigateToNotifications
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "← Back",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            modifier = Modifier
                .clickable { onNavigateBack() }
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
