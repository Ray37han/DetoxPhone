package dev.minimalist.launcher.detox.ui.friction

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.detox.domain.model.BOREDOM_INTENT
import dev.minimalist.launcher.detox.domain.model.INTENT_REASONS
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.domain.model.TIMEBOX_OPTIONS_MIN
import dev.minimalist.launcher.ui.theme.LauncherTheme
import kotlinx.coroutines.delay

private val ColorBlack = Color(0xFF000000)
private val ColorSurface = Color(0xFF121212)
private val ColorSurfaceElevated = Color(0xFF1E1E1E)
private val ColorWhite = Color(0xFFFFFFFF)
private val ColorWhiteSubtle = Color(0x99FFFFFF)
private val ColorWhiteMuted = Color(0x66FFFFFF)
private val ColorWarningRed = Color(0xFFFF3B30)
private val ColorHoldingYellow = Color(0xFFFFD600)

@AndroidEntryPoint
class FrictionGateActivity : ComponentActivity() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_REASON = "extra_reason"
        const val EXTRA_COOLDOWN_REMAINING_MS = "extra_cooldown_remaining_ms"
        const val EXTRA_DAILY_BUDGET_MINUTES = "extra_daily_budget_minutes"
        const val EXTRA_COOLDOWN_MINUTES = "extra_cooldown_minutes"
        const val EXTRA_PROTECTION_LEVEL = "extra_protection_level"

        const val REASON_FRICTION = "FRICTION"
        const val REASON_COOLDOWN = "COOLDOWN"
        const val REASON_TIMEBOX_EXPIRED = "TIMEBOX_EXPIRED"
        const val REASON_FULL_BLOCK = "FULL_BLOCK"
        const val REASON_REELS_BLOCKED = "REELS_BLOCKED"
        const val REASON_BUDGET_EXHAUSTED = "BUDGET_EXHAUSTED"
    }

    private val viewModel: FrictionGateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: run { finish(); return }
        val reason = intent.getStringExtra(EXTRA_REASON) ?: REASON_FRICTION
        val cooldownRemainingMs = intent.getLongExtra(EXTRA_COOLDOWN_REMAINING_MS, 0L)
        val dailyBudgetMinutes = intent.getIntExtra(EXTRA_DAILY_BUDGET_MINUTES, 30)
        val cooldownMinutes = intent.getIntExtra(EXTRA_COOLDOWN_MINUTES, 10)
        val protectionLevel = ProtectionLevel.fromValue(
            intent.getIntExtra(EXTRA_PROTECTION_LEVEL, ProtectionLevel.FRICTION_TIMEBOX.value)
        )

        viewModel.init(
            packageName = packageName,
            reason = reason,
            cooldownRemainingMs = cooldownRemainingMs,
            dailyBudgetMinutes = dailyBudgetMinutes,
            cooldownMinutes = cooldownMinutes,
            protectionLevel = protectionLevel
        )

        setContent {
            LauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ColorBlack
                ) {
                    val uiState by viewModel.uiState.collectAsState()
                    FrictionGateScreen(
                        uiState = uiState,
                        reason = reason,
                        onProceed = { intentReason, boxMinutes ->
                            val validMinutes = if (boxMinutes > 0) boxMinutes else 5
                            val durationMs = if (uiState.protectionLevel == ProtectionLevel.FRICTION_ONLY) {
                                24 * 3600_000L
                            } else {
                                validMinutes * 60_000L
                            }
                            viewModel.onProceed(intentReason, durationMs) {
                                val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                                }
                                if (launchIntent != null) {
                                    startActivity(launchIntent)
                                }
                                finish()
                            }
                        },
                        onDismiss = {
                            viewModel.onDismiss()
                            goHome()
                        },
                        onSubstitute = { substPkg ->
                            viewModel.onDismiss()
                            launchSubstitute(substPkg)
                        }
                    )
                }
            }
        }
    }

    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
        finish()
    }

    private fun launchSubstitute(packageName: String) {
        packageManager.getLaunchIntentForPackage(packageName)?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(it)
        }
        finish()
    }
}

@Composable
fun FrictionGateScreen(
    uiState: FrictionGateUiState,
    reason: String,
    onProceed: (intentReason: String, timeboxMinutes: Int) -> Unit,
    onDismiss: () -> Unit,
    onSubstitute: (packageName: String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBlack),
        contentAlignment = Alignment.Center
    ) {
        when (reason) {
            FrictionGateActivity.REASON_FRICTION -> FrictionFlowScreen(
                appName = uiState.appDisplayName,
                protectionLevel = uiState.protectionLevel,
                dailyBudgetMinutes = uiState.dailyBudgetMinutes,
                cooldownMinutes = uiState.cooldownMinutes,
                todayOpens = uiState.todayOpens,
                substitutePackage = uiState.substitutePackage,
                onProceed = onProceed,
                onDismiss = onDismiss,
                onSubstitute = onSubstitute
            )
            FrictionGateActivity.REASON_COOLDOWN,
            FrictionGateActivity.REASON_TIMEBOX_EXPIRED -> CooldownScreen(
                appName = uiState.appDisplayName,
                cooldownRemainingMs = uiState.cooldownRemainingMs,
                expired = reason == FrictionGateActivity.REASON_TIMEBOX_EXPIRED,
                onDismiss = onDismiss
            )
            FrictionGateActivity.REASON_FULL_BLOCK -> FullBlockScreen(
                appName = uiState.appDisplayName,
                onDismiss = onDismiss
            )
            FrictionGateActivity.REASON_REELS_BLOCKED -> ReelsBlockedScreen(onDismiss = onDismiss)
            FrictionGateActivity.REASON_BUDGET_EXHAUSTED -> BudgetExhaustedScreen(
                appName = uiState.appDisplayName,
                budgetMinutes = uiState.dailyBudgetMinutes,
                onDismiss = onDismiss
            )
        }
    }
}

private enum class FrictionStep { THUMB_HOLD, MATH_CHALLENGE, INTENTION_CHOICE }

@Composable
private fun FrictionFlowScreen(
    appName: String,
    protectionLevel: ProtectionLevel,
    dailyBudgetMinutes: Int,
    cooldownMinutes: Int,
    todayOpens: Int,
    substitutePackage: String?,
    onProceed: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onSubstitute: (String) -> Unit
) {
    val requiredHoldSec = minOf(20, 4 + 2 * (todayOpens / 2))
    val requiresMathChallenge = todayOpens >= 6

    var step by remember { mutableStateOf(FrictionStep.THUMB_HOLD) }

    AnimatedContent(targetState = step, label = "friction_step") { currentStep ->
        when (currentStep) {
            FrictionStep.THUMB_HOLD -> ThumbHoldScreen(
                appName = appName,
                requiredHoldSec = requiredHoldSec,
                todayOpens = todayOpens,
                substitutePackage = substitutePackage,
                onHoldComplete = {
                    step = if (requiresMathChallenge) FrictionStep.MATH_CHALLENGE
                    else FrictionStep.INTENTION_CHOICE
                },
                onDismiss = onDismiss,
                onSubstitute = onSubstitute
            )
            FrictionStep.MATH_CHALLENGE -> MathChallengeScreen(
                onPassed = { step = FrictionStep.INTENTION_CHOICE },
                onDismiss = onDismiss
            )
            FrictionStep.INTENTION_CHOICE -> {
                if (protectionLevel == ProtectionLevel.FRICTION_ONLY) {
                    IntentionOnlyScreen(
                        appName = appName,
                        onProceed = { reason -> onProceed(reason, 0) },
                        onDismiss = onDismiss
                    )
                } else {
                    IntentionTimeboxScreen(
                        appName = appName,
                        dailyBudgetMinutes = dailyBudgetMinutes,
                        cooldownMinutes = cooldownMinutes,
                        boredomIntent = BOREDOM_INTENT,
                        onProceed = onProceed,
                        onDismiss = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun ThumbHoldScreen(
    appName: String,
    requiredHoldSec: Int,
    todayOpens: Int,
    substitutePackage: String?,
    onHoldComplete: () -> Unit,
    onDismiss: () -> Unit,
    onSubstitute: (String) -> Unit
) {
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe_scale"
    )

    LaunchedEffect(isHolding) {
        if (isHolding) {
            val requiredMs = requiredHoldSec * 1000f
            while (isHolding && elapsedMs < requiredMs) {
                delay(16)
                elapsedMs += 16f
                holdProgress = (elapsedMs / requiredMs).coerceIn(0f, 1f)
            }
            if (elapsedMs >= requiredMs) {
                onHoldComplete()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = appName,
            color = ColorWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        if (todayOpens > 0) {
            Text(
                text = "Open #${todayOpens + 1} today",
                color = ColorWhiteMuted,
                fontSize = 13.sp
            )
        }

        Spacer(Modifier.height(8.dp))

        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { holdProgress },
                modifier = Modifier.size(180.dp),
                color = ColorHoldingYellow,
                trackColor = ColorWhite.copy(alpha = 0.1f),
                strokeWidth = 6.dp
            )
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .scale(if (isHolding) breathScale else 1f)
                    .clip(CircleShape)
                    .background(ColorSurface)
                    .border(
                        2.dp,
                        if (isHolding) ColorHoldingYellow else ColorWhite.copy(alpha = 0.25f),
                        CircleShape
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isHolding = true
                                elapsedMs = 0f
                                holdProgress = 0f
                                try {
                                    awaitRelease()
                                } finally {
                                    if (holdProgress < 1f) {
                                        isHolding = false
                                        elapsedMs = 0f
                                        holdProgress = 0f
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
                        text = if (isHolding) "Breathe" else "Hold",
                        color = ColorWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isHolding) {
                        Text(
                            text = "${requiredHoldSec}s",
                            color = ColorWhiteMuted,
                            fontSize = 13.sp
                        )
                    } else {
                        val remaining = ((requiredHoldSec - elapsedMs / 1000f)).toInt().coerceAtLeast(0)
                        Text(
                            text = "${remaining}s",
                            color = ColorHoldingYellow,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Text(
            text = "Press and hold to continue\nReleasing resets the timer",
            color = ColorWhiteMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (substitutePackage != null) {
            Button(
                onClick = { onSubstitute(substitutePackage) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorSurfaceElevated,
                    contentColor = ColorWhite
                ),
                border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Open Healthy Substitute Instead", fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, contentDescription = null, tint = ColorWhiteMuted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "Close & Return Home",
                color = ColorWhiteMuted,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun MathChallengeScreen(
    onPassed: () -> Unit,
    onDismiss: () -> Unit
) {
    val a = remember { (20..79).random() }
    val b = remember { (10..79).random() }
    val correctAnswer = a + b

    var userInput by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = ColorWhite, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Focus Check",
                color = ColorWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Solve this problem to continue:",
            color = ColorWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = ColorSurface),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "$a + $b = ?",
                color = ColorWhite,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp)
            )
        }

        OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it.filter { c -> c.isDigit() } },
            label = { Text("Answer", color = ColorWhiteMuted) },
            singleLine = true,
            isError = showError,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = ColorWhite,
                unfocusedTextColor = ColorWhite,
                focusedBorderColor = ColorWhite,
                unfocusedBorderColor = ColorWhite.copy(alpha = 0.3f),
                errorBorderColor = ColorWarningRed
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (showError) {
            Text(
                "Incorrect — try again",
                color = ColorWarningRed,
                fontSize = 13.sp
            )
        }

        Button(
            onClick = {
                if (userInput.toIntOrNull() == correctAnswer) {
                    onPassed()
                } else {
                    showError = true
                    userInput = ""
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorWhite,
                contentColor = ColorBlack
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Submit Answer", fontWeight = FontWeight.SemiBold)
        }

        TextButton(onClick = onDismiss) {
            Text(
                "Close & Return Home",
                color = ColorWhiteMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun IntentionOnlyScreen(
    appName: String,
    onProceed: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf(INTENT_REASONS.first()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "Opening $appName",
            color = ColorWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "What is your intention?",
            color = ColorWhiteSubtle,
            fontSize = 14.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            INTENT_REASONS.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ColorSurface,
                                labelColor = ColorWhiteSubtle,
                                selectedContainerColor = ColorWhite,
                                selectedLabelColor = ColorBlack
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedReason == reason,
                                borderColor = ColorWhite.copy(alpha = 0.2f),
                                selectedBorderColor = ColorWhite
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { onProceed(selectedReason) },
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorWhite,
                contentColor = ColorBlack
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Open App", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onDismiss) {
            Text(
                "Close & Return Home",
                color = ColorWhiteMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun IntentionTimeboxScreen(
    appName: String,
    dailyBudgetMinutes: Int,
    cooldownMinutes: Int,
    boredomIntent: String,
    onProceed: (String, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf(INTENT_REASONS.first()) }
    var selectedMinutes by remember { mutableIntStateOf(5) }

    val isBoredom = selectedReason == boredomIntent
    val effectiveTimeboxOptions = if (isBoredom) {
        TIMEBOX_OPTIONS_MIN.filter { it <= minOf(2, dailyBudgetMinutes) }.ifEmpty { listOf(1) }
    } else {
        TIMEBOX_OPTIONS_MIN.filter { it <= dailyBudgetMinutes }.ifEmpty { listOf(1, 5, 10, 15) }
    }

    LaunchedEffect(isBoredom, dailyBudgetMinutes) {
        if (!effectiveTimeboxOptions.contains(selectedMinutes)) {
            selectedMinutes = effectiveTimeboxOptions.firstOrNull() ?: 5
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Opening $appName",
            color = ColorWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "What is your intention?",
            color = ColorWhiteSubtle,
            fontSize = 14.sp
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            INTENT_REASONS.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    row.forEach { reason ->
                        FilterChip(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            label = { Text(reason, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = ColorSurface,
                                labelColor = ColorWhiteSubtle,
                                selectedContainerColor = ColorWhite,
                                selectedLabelColor = ColorBlack
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedReason == reason,
                                borderColor = ColorWhite.copy(alpha = 0.2f),
                                selectedBorderColor = ColorWhite
                            )
                        )
                    }
                }
            }
        }

        if (isBoredom) {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorSurfaceElevated),
                border = BorderStroke(1.dp, ColorWarningRed),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "Habit sessions capped at ${minOf(2, dailyBudgetMinutes)} minute${if (minOf(2, dailyBudgetMinutes) > 1) "s" else ""}",
                    color = ColorWarningRed,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }

        Text(
            text = "How long do you need?",
            color = ColorWhiteSubtle,
            fontSize = 14.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
        ) {
            effectiveTimeboxOptions.forEach { min ->
                FilterChip(
                    selected = selectedMinutes == min,
                    onClick = { selectedMinutes = min },
                    label = { Text("${min}m", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = ColorSurface,
                        labelColor = ColorWhiteSubtle,
                        selectedContainerColor = ColorWhite,
                        selectedLabelColor = ColorBlack
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedMinutes == min,
                        borderColor = ColorWhite.copy(alpha = 0.2f),
                        selectedBorderColor = ColorWhite
                    )
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = ColorSurface),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = "Budget: ${dailyBudgetMinutes}m/day · Cooldown: ${cooldownMinutes}m after session",
                color = ColorWhiteMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        }

        Button(
            onClick = { onProceed(selectedReason, selectedMinutes) },
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorWhite,
                contentColor = ColorBlack
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Start ${selectedMinutes}m Session", fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onDismiss) {
            Text(
                "Close & Return Home",
                color = ColorWhiteMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun CooldownScreen(
    appName: String,
    cooldownRemainingMs: Long,
    expired: Boolean,
    onDismiss: () -> Unit
) {
    var remainingMs by remember { mutableStateOf(cooldownRemainingMs) }

    LaunchedEffect(Unit) {
        while (remainingMs > 0) {
            delay(1000)
            remainingMs = (remainingMs - 1000).coerceAtLeast(0)
        }
    }

    val minutes = (remainingMs / 60000).toInt()
    val seconds = ((remainingMs % 60000) / 1000).toInt()

    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = { 1f - (remainingMs.toFloat() / cooldownRemainingMs.coerceAtLeast(1)) },
                modifier = Modifier.size(140.dp),
                color = ColorWarningRed,
                trackColor = ColorWhite.copy(alpha = 0.1f),
                strokeWidth = 6.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Pause,
                    contentDescription = null,
                    tint = ColorWarningRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "%02d:%02d".format(minutes, seconds),
                    color = ColorWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Text(
            text = if (expired) "Session Complete" else "Cooldown Active",
            color = ColorWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (expired)
                "$appName session ended.\nTake a ${minutes + 1} minute break."
            else
                "$appName is on cooldown.\nRest your eyes for a moment.",
            color = ColorWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorSurfaceElevated,
                contentColor = ColorWhite
            ),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Return to Detox Phone Home", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FullBlockScreen(appName: String, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.Block,
            contentDescription = null,
            tint = ColorWarningRed,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = appName,
            color = ColorWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "This app is fully blocked.\nProtect your focus.",
            color = ColorWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorSurfaceElevated,
                contentColor = ColorWhite
            ),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Return to Detox Phone Home", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ReelsBlockedScreen(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.VideocamOff,
            contentDescription = null,
            tint = ColorWarningRed,
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = "Short-Video Blocked",
            color = ColorWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Short-form video feeds are restricted to protect your focus.",
            color = ColorWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorSurfaceElevated,
                contentColor = ColorWhite
            ),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Go Back", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BudgetExhaustedScreen(
    appName: String,
    budgetMinutes: Int,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Default.HourglassEmpty,
            contentDescription = null,
            tint = ColorWarningRed,
            modifier = Modifier.size(52.dp)
        )
        Text(
            text = "Daily Budget Exhausted",
            color = ColorWhite,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Text(
            text = "You have used your ${budgetMinutes}m daily budget for $appName.\nCome back tomorrow.",
            color = ColorWhiteMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorSurfaceElevated,
                contentColor = ColorWhite
            ),
            border = BorderStroke(1.dp, ColorWhite.copy(alpha = 0.25f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Return to Detox Phone Home", fontWeight = FontWeight.SemiBold)
        }
    }
}
