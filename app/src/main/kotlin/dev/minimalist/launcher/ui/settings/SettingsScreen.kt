package dev.minimalist.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimalist.launcher.settings.domain.model.ThemeMode
import dev.minimalist.launcher.ui.components.SettingsSection
import dev.minimalist.launcher.ui.components.SettingsRow
import dev.minimalist.launcher.ui.components.SettingsPickerDialog

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHiddenApps: () -> Unit,
    onNavigateToWellbeing: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = state.settings

    var showThemePicker by remember { mutableStateOf(false) }
    var showFontSizePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // ── Header ──
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        // ── Appearance ──
        SettingsSection(title = "Appearance") {
            SettingsRow(
                title = "Theme",
                subtitle = settings.themeMode.name.lowercase().replaceFirstChar { it.uppercase() },
                onClick = { showThemePicker = true }
            )

            SettingsRow(
                title = "Font size",
                subtitle = settings.fontSize.name.lowercase().replace("_", " ")
                    .replaceFirstChar { it.uppercase() },
                onClick = { showFontSizePicker = true }
            )

            SwitchRow(
                title = "Show clock",
                checked = settings.showClock,
                onCheckedChange = { viewModel.setShowClock(it) }
            )

            SwitchRow(
                title = "Show date",
                checked = settings.showDate,
                onCheckedChange = { viewModel.setShowDate(it) }
            )

            SwitchRow(
                title = "Animations",
                checked = settings.animationsEnabled,
                onCheckedChange = { viewModel.setAnimationsEnabled(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Behaviour ──
        SettingsSection(title = "Behaviour") {
            SwitchRow(
                title = "Double-tap to lock screen",
                checked = settings.screenLockDoubleTap,
                onCheckedChange = { viewModel.setScreenLockDoubleTap(it) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Navigation Items ──
        SettingsSection(title = "Manage") {
            SettingsRow(
                title = "Hidden apps",
                subtitle = "${state.hiddenApps.size} hidden",
                onClick = onNavigateToHiddenApps
            )

            SettingsRow(
                title = "Digital wellbeing",
                subtitle = "Timers, blocking, notifications",
                onClick = onNavigateToWellbeing
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        SettingsRow(
            title = "About",
            subtitle = "Detox Phone",
            onClick = onNavigateToAbout
        )

        Spacer(modifier = Modifier.height(48.dp))

        // ── Back ──
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

    // ── Theme Picker ──
    if (showThemePicker) {
        SettingsPickerDialog(
            title = "Theme",
            options = ThemeMode.entries.map {
                it.name.lowercase().replaceFirstChar { c -> c.uppercase() }
            },
            selectedIndex = ThemeMode.entries.indexOf(settings.themeMode),
            onSelect = { index ->
                viewModel.setThemeMode(ThemeMode.entries[index])
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
    }

    // ── Font Size Picker ──
    if (showFontSizePicker) {
        val fontSizes = dev.minimalist.launcher.settings.domain.model.FontSize.entries
        SettingsPickerDialog(
            title = "Font size",
            options = fontSizes.map {
                it.name.lowercase().replace("_", " ").replaceFirstChar { c -> c.uppercase() }
            },
            selectedIndex = fontSizes.indexOf(settings.fontSize),
            onSelect = { index ->
                viewModel.setFontSize(fontSizes[index])
                showFontSizePicker = false
            },
            onDismiss = { showFontSizePicker = false }
        )
    }
}

@Composable
fun SwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
