package dev.minimalist.launcher.wellbeing.ui.blocking

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.ui.theme.LauncherTheme

/**
 * Fullscreen blocking overlay shown when an app exceeds its timer or
 * is blocked by a schedule. Launches as singleInstance to overlay
 * the blocked app.
 */
@AndroidEntryPoint
class AppBlockedActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_BLOCK_REASON = "extra_block_reason"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
        val reason = intent.getStringExtra(EXTRA_BLOCK_REASON) ?: "App blocked"

        setContent {
            LauncherTheme {
                BlockedScreen(
                    appName = packageName.substringAfterLast('.'),
                    reason = reason,
                    onGoHome = {
                        finishAndRemoveTask()
                    }
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Go home instead of back to the blocked app
        finishAndRemoveTask()
    }
}

@Composable
private fun BlockedScreen(
    appName: String,
    reason: String,
    onGoHome: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏸",
                fontSize = 48.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Take a break",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Light
                ),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            TextButton(onClick = onGoHome) {
                Text(
                    text = "Go home",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
