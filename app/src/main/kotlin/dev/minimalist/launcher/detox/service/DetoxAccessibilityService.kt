package dev.minimalist.launcher.detox.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel
import dev.minimalist.launcher.detox.domain.repository.DetoxRepository
import dev.minimalist.launcher.detox.ui.friction.FrictionGateActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * DetoxAccessibilityService — the 6-engine system-wide enforcement spine.
 *
 * Intercepts every foreground app switch (launcher tap, notification tap, Recents, deep link).
 *
 * Engine 1 — Pre-Launch Friction Gate (Phase 2)
 * Engine 2 — Micro-Timebox + 1s Ticker + 30s Wind-Down + Hard Ejection (Phase 3)
 * Engine 3 — Kinetic Scroll Decay / Distance Governor (Phase 4)
 * Engine 4 — Surgical Short-Video Blocker via view-ID (Phase 4)
 * Engine 5 — Hardware Grayscale / Fallback Overlay (Phase 6)
 *
 * All enforcement runs inside TYPE_ACCESSIBILITY_OVERLAY — no SYSTEM_ALERT_WINDOW needed.
 */
@AndroidEntryPoint
class DetoxAccessibilityService : AccessibilityService() {

    @Inject lateinit var detoxRepository: DetoxRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var overlayController: OverlayController? = null
    private var lastForegroundPackage: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // ── Phase 3: session countdown ticker ──────────────────────────────────
    private var sessionTickerJob: Job? = null

    /** Epoch ms when user last switched away from a monitored app. */
    private var lastBackgroundedMs: Long = 0L

    // ── Phase 4: short-video back-action debounce ──────────────────────────
    private var lastBackActionMs: Long = 0L
    private var lastShortVideoInspectionMs: Long = 0L

    // ── Phase 4: scroll debounce ───────────────────────────────────────────
    private var lastScrollTimestampMs: Long = 0L

    @Volatile
    private var currentAppMonitoredForShortVideos: Boolean = false

    companion object {
        var activeInstance: DetoxAccessibilityService? = null
            private set

        fun isConnected(): Boolean = activeInstance != null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        activeInstance = this
        overlayController = OverlayController(this)
        serviceInfo = serviceInfo.apply {
            eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            notificationTimeout = 150
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkg = ev.packageName?.toString() ?: return

        // Skip own package, system UI, IMEs
        if (pkg == packageName) return
        if (pkg == "com.android.systemui") return

        // Synchronous pre-filter to prevent coroutine flooding and CPU heating
        if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!currentAppMonitoredForShortVideos) return
            if (!ShortVideoSignatures.VIEW_ID_MAP.containsKey(pkg)) return
            val now = System.currentTimeMillis()
            if (now - lastShortVideoInspectionMs < ShortVideoSignatures.INSPECTION_DEBOUNCE_MS) return
        }

        serviceScope.launch {
            try {
                when (ev.eventType) {
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                        handleWindowStateChanged(pkg)
                    }
                    AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                        handleScrollEvent(pkg)
                    }
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                        checkForShortVideoContent(pkg)
                    }
                }
            } catch (_: Exception) { /* graceful degradation */ }
        }
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        if (activeInstance == this) activeInstance = null
        stopSessionTicker()
        overlayController?.removeAll()
        overlayController = null
        serviceScope.cancel()
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Engine 1 + 2: Friction Gate + Timebox State Machine
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun handleWindowStateChanged(packageName: String) {
        val prevPkg = lastForegroundPackage

        // ── Restore color and clean HUD when leaving an app ─────────────────
        if (prevPkg != null && prevPkg != packageName) {
            val prevApp = detoxRepository.getMonitoredApp(prevPkg)
            if (prevApp?.forceGrayscale == true) {
                GrayscaleController.setHardwareGrayscale(applicationContext, false)
            }
            overlayController?.removePassThroughHud()
            overlayController?.removeDecayOverlay()
        }

        lastForegroundPackage = packageName

        val monitoredApp = detoxRepository.getMonitoredApp(packageName) ?: run {
            currentAppMonitoredForShortVideos = false
            // Switched to an unmonitored app — pause session ticker for grace period tracking
            if (prevPkg != null && detoxRepository.getMonitoredApp(prevPkg) != null) {
                lastBackgroundedMs = System.currentTimeMillis()
            }
            stopSessionTicker()
            return
        }

        currentAppMonitoredForShortVideos = monitoredApp.blockShortVideos

        if (monitoredApp.protectionLevel == ProtectionLevel.NONE) return

        // ── 1. Full Block (Hard block) ──────────────────────────────────────
        if (monitoredApp.protectionLevel == ProtectionLevel.FULL_BLOCK) {
            stopSessionTicker()
            showFrictionGate(
                packageName = packageName,
                reason = FrictionGateActivity.REASON_FULL_BLOCK,
                protectionLevel = ProtectionLevel.FULL_BLOCK.value
            )
            return
        }

        // ── 2. Strict Mode lock (Commitment lock) ───────────────────────────
        val strictMode = detoxRepository.getStrictModeConfig().first()
        if (strictMode?.isActive == true && !strictMode.isExpired) {
            stopSessionTicker()
            showFrictionGate(
                packageName = packageName,
                reason = FrictionGateActivity.REASON_FULL_BLOCK,
                protectionLevel = ProtectionLevel.FULL_BLOCK.value
            )
            return
        }

        val session = detoxRepository.getSession(packageName)
        val today = dateFormat.format(Date())

        // ── 3. Active Cooldown ─────────────────────────────────────────────
        if (session != null && session.cooldownUntilEpochMs > 0L) {
            if (session.isInCooldown) {
                stopSessionTicker()
                showFrictionGate(
                    packageName = packageName,
                    reason = FrictionGateActivity.REASON_COOLDOWN,
                    cooldownRemainingMs = session.cooldownRemainingMs,
                    protectionLevel = monitoredApp.protectionLevel.value
                )
                return
            } else {
                // Cooldown elapsed — clear session so app is unlocked
                detoxRepository.endSession(packageName, 0)
            }
        }

        // ── 4. Friction Only (No timebox, no cooldown) ─────────────────────
        if (monitoredApp.protectionLevel == ProtectionLevel.FRICTION_ONLY) {
            stopSessionTicker()
            if (session != null && session.cooldownUntilEpochMs == 0L) {
                val awayMs = if (lastBackgroundedMs > 0) System.currentTimeMillis() - lastBackgroundedMs else 0L
                if (lastBackgroundedMs > 0 && awayMs > 60_000L) {
                    detoxRepository.endSession(packageName, 0)
                    showFrictionGate(
                        packageName = packageName,
                        reason = FrictionGateActivity.REASON_FRICTION,
                        protectionLevel = ProtectionLevel.FRICTION_ONLY.value
                    )
                } else {
                    lastBackgroundedMs = 0L
                }
            } else {
                showFrictionGate(
                    packageName = packageName,
                    reason = FrictionGateActivity.REASON_FRICTION,
                    protectionLevel = ProtectionLevel.FRICTION_ONLY.value
                )
            }
            if (monitoredApp.forceGrayscale) {
                GrayscaleController.setHardwareGrayscale(applicationContext, true)
            }
            return
        }

        // ── 5. Friction + Timebox ──────────────────────────────────────────
        when {
            // Session expired → apply cooldown
            session != null && session.isExpired -> {
                stopSessionTicker()
                detoxRepository.endSession(packageName, monitoredApp.cooldownMinutes)
                detoxRepository.ensureTelemetryRow(today, packageName)
                showFrictionGate(
                    packageName = packageName,
                    reason = FrictionGateActivity.REASON_TIMEBOX_EXPIRED,
                    cooldownRemainingMs = monitoredApp.cooldownMinutes * 60_000L,
                    protectionLevel = ProtectionLevel.FRICTION_TIMEBOX.value
                )
            }

            // Active valid session
            session != null && !session.isExpired && !session.isInCooldown -> {
                val awayMs = if (lastBackgroundedMs > session.sessionStartEpochMs) {
                    System.currentTimeMillis() - lastBackgroundedMs
                } else 0L
                if (lastBackgroundedMs > session.sessionStartEpochMs && awayMs > 60_000L) {
                    stopSessionTicker()
                    detoxRepository.endSession(packageName, 0)
                    showFrictionGate(
                        packageName = packageName,
                        reason = FrictionGateActivity.REASON_FRICTION,
                        dailyBudgetMinutes = monitoredApp.dailyBudgetMinutes,
                        cooldownMinutes = monitoredApp.cooldownMinutes,
                        protectionLevel = ProtectionLevel.FRICTION_TIMEBOX.value
                    )
                } else {
                    lastBackgroundedMs = 0L
                    startSessionTicker(packageName)
                }
            }

            // No active session
            else -> {
                stopSessionTicker()
                val telemetry = detoxRepository.getTodayTelemetryForApp(today, packageName)
                val usedMinutes = (telemetry?.totalForegroundMs ?: 0L) / 60_000L
                val budgetRemaining = monitoredApp.dailyBudgetMinutes - usedMinutes
                if (budgetRemaining <= 0) {
                    showFrictionGate(
                        packageName = packageName,
                        reason = FrictionGateActivity.REASON_BUDGET_EXHAUSTED,
                        dailyBudgetMinutes = monitoredApp.dailyBudgetMinutes,
                        protectionLevel = ProtectionLevel.FRICTION_TIMEBOX.value
                    )
                } else {
                    showFrictionGate(
                        packageName = packageName,
                        reason = FrictionGateActivity.REASON_FRICTION,
                        dailyBudgetMinutes = budgetRemaining.toInt(),
                        cooldownMinutes = monitoredApp.cooldownMinutes,
                        protectionLevel = ProtectionLevel.FRICTION_TIMEBOX.value
                    )
                }
            }
        }

        // ── Engine 5: Grayscale ────────────────────────────────────────────
        if (monitoredApp.forceGrayscale) {
            GrayscaleController.setHardwareGrayscale(applicationContext, true)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Engine 2 Phase 3: 1-Second Session Countdown Ticker & Floating HUD
    // ─────────────────────────────────────────────────────────────────────────

    private fun startSessionTicker(packageName: String) {
        sessionTickerJob?.cancel()
        sessionTickerJob = serviceScope.launch(Dispatchers.Main) {
            // Show initial HUD immediately from the monotonic deadline.
            val initialSession = detoxRepository.getSession(packageName) ?: return@launch
            if (initialSession.isInCooldown) return@launch

            val initSec = (initialSession.remainingMs / 1000).coerceAtLeast(0)
            overlayController?.showHudPill("%02d:%02d".format(initSec / 60, initSec % 60))

            while (true) {
                val session = detoxRepository.getSession(packageName) ?: break
                if (session.isInCooldown) break

                val remainingMs = session.remainingMs
                if (remainingMs <= 0L && session.isExpired) {
                    // ── Hard Ejection (state transition happens HERE, not in animation) ──
                    val monitoredApp = detoxRepository.getMonitoredApp(packageName)
                    detoxRepository.endSession(packageName, monitoredApp?.cooldownMinutes ?: 10)
                    overlayController?.removeAll()
                    showFrictionGate(
                        packageName = packageName,
                        reason = FrictionGateActivity.REASON_TIMEBOX_EXPIRED,
                        cooldownRemainingMs = (monitoredApp?.cooldownMinutes ?: 10) * 60_000L,
                        protectionLevel = monitoredApp?.protectionLevel?.value ?: ProtectionLevel.FRICTION_TIMEBOX.value
                    )
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    break
                }

                // 1-second cadence ticker
                delay(1_000L)

                if (lastForegroundPackage != packageName) break

                val refreshed = detoxRepository.getSession(packageName) ?: break
                if (refreshed.isInCooldown) break

                val freshRemainingMs = refreshed.remainingMs
                val remSec = (freshRemainingMs / 1000).coerceAtLeast(0)

                when {
                    freshRemainingMs <= 0L && refreshed.isExpired -> {
                        // ── Hard Ejection ──
                        val monitoredApp = detoxRepository.getMonitoredApp(packageName)
                        detoxRepository.endSession(packageName, monitoredApp?.cooldownMinutes ?: 10)
                        overlayController?.removeAll()
                        showFrictionGate(
                            packageName = packageName,
                            reason = FrictionGateActivity.REASON_TIMEBOX_EXPIRED,
                            cooldownRemainingMs = (monitoredApp?.cooldownMinutes ?: 10) * 60_000L,
                            protectionLevel = monitoredApp?.protectionLevel?.value ?: ProtectionLevel.FRICTION_TIMEBOX.value
                        )
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        break
                    }
                    freshRemainingMs <= 30_000L -> {
                        // ── 30s Wind-Down Haptic & Banner ──
                        overlayController?.updateHudText("${remSec}s • Wrap up")
                        if (freshRemainingMs in 29_000L..30_000L) {
                            triggerWindDownHaptic()
                        }
                    }
                    else -> {
                        val S = refreshed.scrollEventsCount
                        val hudText = if (S >= 40) {
                            val meters = "%.1f".format(S * 0.18f)
                            "Est. ${meters}m • %02d:%02d".format(remSec / 60, remSec % 60)
                        } else {
                            "%02d:%02d".format(remSec / 60, remSec % 60)
                        }
                        overlayController?.updateHudText(hudText)
                    }
                }
            }
            sessionTickerJob = null
        }
    }

    private fun stopSessionTicker() {
        sessionTickerJob?.cancel()
        sessionTickerJob = null
        overlayController?.removePassThroughHud()
        overlayController?.removeDecayOverlay()
    }

    private fun triggerWindDownHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(VIBRATOR_SERVICE) as? Vibrator
            } ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(80)
            }
        } catch (_: Exception) {}
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Engine 3: Kinetic Scroll Decay Governor
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun handleScrollEvent(packageName: String) {
        val monitoredApp = detoxRepository.getMonitoredApp(packageName) ?: return
        if (!monitoredApp.enableScrollDecay) return

        val session = detoxRepository.getSession(packageName) ?: return
        if (session.isExpired || session.isInCooldown) return

        // Debounce: max 1 event per 150ms (prevents single fling counting as many events)
        val now = System.currentTimeMillis()
        if (now - lastScrollTimestampMs < 150L) return
        lastScrollTimestampMs = now

        detoxRepository.incrementScrollCount(packageName)
        val today = dateFormat.format(Date())
        detoxRepository.addScrollMeters(today, packageName, 0.18)

        // Compute decay overlay alpha per plan formula 5.3
        val updatedSession = detoxRepository.getSession(packageName) ?: return
        val S = updatedSession.scrollEventsCount.toFloat()
        val alpha = if (S > 60f) {
            ((S - 60f) / 50f * 0.78f).coerceIn(0f, 0.78f)
        } else 0f

        detoxRepository.updateScrollDecayAlpha(packageName, alpha)
        overlayController?.setDecayOverlayAlpha(alpha)

        if (S >= 40f) {
            val meters = "%.1f".format(S * 0.18f)
            val remSec = (updatedSession.remainingMs / 1000).coerceAtLeast(0)
            overlayController?.updateHudText("${meters}m scrolled • %02d:%02d".format(remSec / 60, remSec % 60))
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Engine 4: Surgical Short-Video Blocker (view-ID based)
    // ─────────────────────────────────────────────────────────────────────────

    private suspend fun checkForShortVideoContent(packageName: String) {
        val targetViewIds = ShortVideoSignatures.VIEW_ID_MAP[packageName] ?: return
        if (!currentAppMonitoredForShortVideos) return

        // Debounce inspection to every 250ms
        val now = System.currentTimeMillis()
        if (now - lastShortVideoInspectionMs < ShortVideoSignatures.INSPECTION_DEBOUNCE_MS) return
        lastShortVideoInspectionMs = now

        // Debounce back action to every 800ms
        if (now - lastBackActionMs < ShortVideoSignatures.BACK_ACTION_DEBOUNCE_MS) return

        val root = rootInActiveWindow ?: return
        try {
            var detected = false
            for (viewId in targetViewIds) {
                val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
                if (nodes.isNotEmpty()) {
                    val hasVisible = nodes.any { it.isVisibleToUser }
                    nodes.forEach { try { it.recycle() } catch (_: Exception) {} }
                    if (hasVisible) {
                        detected = true
                        break
                    }
                }
            }
            if (detected) {
                lastBackActionMs = now
                val today = dateFormat.format(Date())
                detoxRepository.recordReelsBlocked(today, packageName)
                overlayController?.updateHudText("Short-video blocked")
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        } catch (_: Exception) {
        } finally {
            try { root.recycle() } catch (_: Exception) {}
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun showFrictionGate(
        packageName: String,
        reason: String,
        cooldownRemainingMs: Long = 0L,
        dailyBudgetMinutes: Int = 30,
        cooldownMinutes: Int = 10,
        protectionLevel: Int = ProtectionLevel.FRICTION_TIMEBOX.value
    ) {
        val intent = Intent(this, FrictionGateActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(FrictionGateActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(FrictionGateActivity.EXTRA_REASON, reason)
            putExtra(FrictionGateActivity.EXTRA_COOLDOWN_REMAINING_MS, cooldownRemainingMs)
            putExtra(FrictionGateActivity.EXTRA_DAILY_BUDGET_MINUTES, dailyBudgetMinutes)
            putExtra(FrictionGateActivity.EXTRA_COOLDOWN_MINUTES, cooldownMinutes)
            putExtra(FrictionGateActivity.EXTRA_PROTECTION_LEVEL, protectionLevel)
        }
        startActivity(intent)
    }
}
