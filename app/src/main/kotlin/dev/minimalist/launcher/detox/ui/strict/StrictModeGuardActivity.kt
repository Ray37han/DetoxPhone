package dev.minimalist.launcher.detox.ui.strict

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.detox.domain.model.StrictModeConfig
import dev.minimalist.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay

/**
 * StrictModeGuard — Commitment Lock Unlock Ceremony (Plan Phase 6, Pillar 6).
 *
 * Displayed when user attempts to disable Strict Mode.
 * Unlock methods:
 *  - TIMER_60S: 60-second continuous thumb-hold (releasing resets)
 *  - QR_CODE:   User must scan a QR with their partner's device which contains
 *               a hash secret, and paste the decoded string (Plan Section 6.6.4)
 */
@AndroidEntryPoint
class StrictModeGuardActivity : ComponentActivity() {

    companion object {
        const val EXTRA_REASON = "extra_reason"
        const val REASON_DISABLE = "DISABLE"
    }

    private val viewModel: StrictModeGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherTheme {
                Surface(Modifier.fillMaxSize(), color = Color.Black) {
                    val config by viewModel.strictModeConfig.collectAsState()
                    StrictModeGuardScreen(
                        config = config,
                        onUnlocked = {
                            viewModel.deactivateStrictMode()
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
private fun StrictModeGuardScreen(
    config: StrictModeConfig?,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        when (config?.unlockMethod) {
            dev.minimalist.launcher.detox.domain.model.UnlockMethod.QR_CODE -> QrUnlockScreen(
                expectedSecret = config.qrHashSecret ?: "",
                onUnlocked = onUnlocked,
                onCancel = onCancel
            )
            else -> Timer60sUnlockScreen(onUnlocked = onUnlocked, onCancel = onCancel)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 60-Second Thumb Hold Unlock (plan primary method)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun Timer60sUnlockScreen(onUnlocked: () -> Unit, onCancel: () -> Unit) {
    val requiredMs = 60_000f
    var isHolding by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            while (isHolding && elapsedMs < requiredMs) {
                delay(16)
                elapsedMs += 16f
            }
            if (elapsedMs >= requiredMs) onUnlocked()
        }
    }

    val progress = (elapsedMs / requiredMs).coerceIn(0f, 1f)
    val progressAnim by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(16, easing = LinearEasing),
        label = "strict_progress"
    )
    val remainingSec = ((requiredMs - elapsedMs) / 1000f).toInt().coerceAtLeast(0)

    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("Strict Mode", color = Color(0xFFFF3B30), fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Text(
            text = "Hold for 60 continuous seconds to disable commitment lock.\nReleasing resets the timer.",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progressAnim },
                modifier = Modifier.size(180.dp),
                color = if (isHolding) Color(0xFFFFD600) else Color(0xFFFF3B30),
                trackColor = Color.White.copy(alpha = 0.1f),
                strokeWidth = 6.dp
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF121212))
                    .border(
                        2.dp,
                        if (isHolding) Color(0xFFFFD600) else Color.White.copy(alpha = 0.25f),
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                elapsedMs = 0f
                                try {
                                    awaitRelease()
                                } finally {
                                    if (progress < 1f) {
                                        isHolding = false
                                        elapsedMs = 0f
                                    }
                                    isHolding = false
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (isHolding) "Holding..." else "Hold",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (isHolding) {
                        Text("${remainingSec}s", color = Color(0xFFEF9A9A), fontSize = 13.sp)
                    }
                }
            }
        }

        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color.White.copy(alpha = 0.4f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QR Code Partner Unlock (plan Section 6.6.4)
// Hash: SHA-256 of (secret + today's date) — user must paste decoded token
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QrUnlockScreen(
    expectedSecret: String,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit
) {
    var inputToken by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("🔑 Partner Unlock", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Text(
            text = "Ask your partner to scan the QR code in\nSettings → Strict Mode → Show QR.\nThen enter the token they read to you.",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Your commitment lock requires a trusted person to unlock it.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier.padding(14.dp),
                textAlign = TextAlign.Center
            )
        }

        OutlinedTextField(
            value = inputToken,
            onValueChange = {
                inputToken = it
                showError = false
            },
            label = { Text("Partner Token", color = Color.White.copy(alpha = 0.5f)) },
            isError = showError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            Text("Invalid token. Ask your partner again.", color = Color(0xFFEF9A9A), fontSize = 12.sp)
        }

        Button(
            onClick = {
                if (verifyQrToken(inputToken.trim(), expectedSecret)) {
                    onUnlocked()
                } else {
                    showError = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7B1FA2)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Unlock with Token", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = onCancel) {
            Text("Cancel", color = Color.White.copy(alpha = 0.4f))
        }
    }
}

/**
 * Verifies the token provided by the partner device.
 * Token = SHA-256(secret + "yyyy-MM-dd") truncated to 6 hex chars.
 */
private fun verifyQrToken(token: String, secret: String): Boolean {
    return try {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
        val input = "$secret$today"
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(input.toByteArray())
        val expected = hash.joinToString("") { "%02x".format(it) }.take(6)
        token.equals(expected, ignoreCase = true)
    } catch (_: Exception) {
        false
    }
}
