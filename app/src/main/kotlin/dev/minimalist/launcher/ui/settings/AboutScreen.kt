package dev.minimalist.launcher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "About",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Light
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Detox Phone",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        var tapCount by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
        var showSnowfall by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

        if (showSnowfall) {
            dev.minimalist.launcher.ui.components.SnowfallEffect()
        }

        Text(
            text = if (showSnowfall) "Version 0.1.0 ❄" else "Version 0.1.0",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.clickable {
                tapCount++
                if (tapCount >= 5) {
                    showSnowfall = !showSnowfall
                    tapCount = 0
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "A distraction-free launcher designed to help you use your phone with intention. " +
                    "Text-only interface, app timers, notification filtering, and monochrome mode — " +
                    "all built to reduce screen time and reclaim your attention.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Features",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        val features = listOf(
            "• Text-only app list — no icons, no distractions",
            "• App timers with cooldowns and blocking",
            "• Schedule-based app blocking",
            "• Notification filtering and batching",
            "• Monochrome / grayscale mode",
            "• Short video / Reels blocker",
            "• Double-tap screen lock",
            "• Privacy-first — all data stays on device"
        )

        features.forEach { feature ->
            Text(
                text = feature,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = "← Back",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { onNavigateBack() }
                .padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}
