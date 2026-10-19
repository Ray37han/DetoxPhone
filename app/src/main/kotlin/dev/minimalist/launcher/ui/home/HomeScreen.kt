package dev.minimalist.launcher.ui.home

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import kotlin.math.abs

/**
 * Home screen — primary launcher view.
 *
 * Gesture contract (Section 16.2):
 *   Swipe UP   → App Drawer
 *   Swipe LEFT → Settings
 *   Swipe RIGHT→ Google Search (with visible text fallback)
 *   Tap        → Launch app
 *   Long-press → App action sheet
 *   Back       → Absorbed (launcher root)
 *
 * Edge protection: horizontal gestures only recognised in the central
 * 64dp-inset zone to avoid clashing with Android system edge-back.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onNavigateToAllApps: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDetox: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    var showAppActions by remember { mutableStateOf<LauncherApp?>(null) }
    var showSearchFallback by remember { mutableStateOf(false) }

    // Intercept back only when sheet or dialog is open; otherwise allow system gesture
    BackHandler(enabled = showAppActions != null || showSearchFallback) {
        if (showAppActions != null) {
            showAppActions = null
        } else if (showSearchFallback) {
            showSearchFallback = false
        }
    }

    val systemGestures = WindowInsets.systemGestures
    val leftEdgePx = maxOf(systemGestures.getLeft(density, layoutDirection).toFloat(), with(density) { 48.dp.toPx() })
    val rightEdgePx = maxOf(systemGestures.getRight(density, layoutDirection).toFloat(), with(density) { 48.dp.toPx() })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            // ── 2-D gesture coordinator (replaces detectVerticalDragGestures) ──
            .pointerInput(Unit) {
                detectHomeGesture(
                    leftEdgePx = leftEdgePx,
                    rightEdgePx = rightEdgePx,
                    onSwipeUp = { onNavigateToAllApps() },
                    onSwipeLeft = { onNavigateToSettings() },
                    onSwipeRight = {
                        val launched = tryLaunchGoogleSearch(context)
                        if (!launched) showSearchFallback = true
                    }
                )
            }
            // Double-tap lock (separate recogniser, non-conflicting)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (state.settings.screenLockDoubleTap) {
                            viewModel.lockScreen()
                        }
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // ── Clock ──
            AnimatedVisibility(
                visible = state.settings.showClock,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = state.currentTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontWeight = FontWeight.Light,
                        fontSize = 64.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // ── Date ──
            AnimatedVisibility(
                visible = state.settings.showDate,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = state.currentDate,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Normal
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // ── Favorite Apps ──
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(
                    state.settings.listItemSpacing.dp
                )
            ) {
                items(
                    items = state.favoriteApps,
                    key = { it.packageName }
                ) { app ->
                    AppTextItem(
                        app = app,
                        onClick = {
                            if (app.packageName == context.packageName) {
                                onNavigateToDetox()
                            } else {
                                viewModel.launchApp(app)
                            }
                        },
                        onLongClick = { showAppActions = app }
                    )
                }

                if (state.favoriteApps.isEmpty()) {
                    item {
                        Text(
                            text = "No favorite apps selected.\nTap 'All apps' below to choose apps.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            }

            // ── Bottom nav bar (accessible controls, Section 16.8) ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "↑ All apps",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.95f),
                    modifier = Modifier
                        .clickable { onNavigateToAllApps() }
                        .semantics { contentDescription = "Open app drawer" }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                )

                // Search affordance — accessible without swipe gesture
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                    modifier = Modifier
                        .clickable {
                            val launched = tryLaunchGoogleSearch(context)
                            if (!launched) showSearchFallback = true
                        }
                        .semantics { contentDescription = "Open Google Search" }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                )

                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                    modifier = Modifier
                        .clickable { onNavigateToSettings() }
                        .semantics { contentDescription = "Open settings" }
                        .padding(vertical = 12.dp, horizontal = 8.dp)
                )
            }
        }

        // ── App action bottom sheet ──
        showAppActions?.let { app ->
            AppActionSheet(
                app = app,
                onDismiss = { showAppActions = null },
                onRename = { newName ->
                    viewModel.renameApp(app.packageName, newName)
                    showAppActions = null
                },
                onToggleFavorite = {
                    viewModel.setFavorite(app.packageName, !app.isFavorite)
                    showAppActions = null
                },
                onToggleHidden = {
                    viewModel.setHidden(app.packageName, !app.isHidden)
                    showAppActions = null
                },
                onDetoxProtection = {
                    showAppActions = null
                    onNavigateToDetox()
                }
            )
        }

        // ── Search fallback dialog (Section 16.2) ──
        if (showSearchFallback) {
            SearchFallbackDialog(
                onDismiss = { showSearchFallback = false },
                onOpenBrowser = {
                    showSearchFallback = false
                    val i = Intent(Intent.ACTION_WEB_SEARCH).apply {
                        putExtra(android.app.SearchManager.QUERY, "")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(i) }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Gesture recogniser (Section 16.2 + 16.9)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 2-D home gesture coordinator.
 *
 * Rules:
 * - Uses platform-derived touch slop, not hard-coded pixels.
 * - Classifies dominant axis only after clearly exceeding slop.
 * - Swipe UP anywhere triggers All Apps.
 * - Left/right gestures near edges (leftEdgePx/rightEdgePx) are passed through
 *   so Android system back navigation gestures are never intercepted.
 * - Multi-touch cancels the gesture.
 */
private suspend fun PointerInputScope.detectHomeGesture(
    leftEdgePx: Float,
    rightEdgePx: Float,
    onSwipeUp: () -> Unit,
    onSwipeLeft: () -> Unit,
    onSwipeRight: () -> Unit,
) {
    val slopPx = viewConfiguration.touchSlop

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        val startPos: Offset = down.position

        var cumulativeX = 0f
        var cumulativeY = 0f
        var gestureConsumed = false

        if (currentEvent.changes.size > 1) return@awaitEachGesture

        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.size > 1) return@awaitEachGesture

            val change = event.changes.firstOrNull() ?: break

            cumulativeX += change.positionChange().x
            cumulativeY += change.positionChange().y

            val absX = abs(cumulativeX)
            val absY = abs(cumulativeY)

            if (!gestureConsumed && (absX > slopPx || absY > slopPx)) {
                gestureConsumed = true
                when {
                    // Dominant: UP (swipe up for All Apps everywhere)
                    absY > absX && cumulativeY < -slopPx -> {
                        onSwipeUp()
                        change.consume()
                        return@awaitEachGesture
                    }
                    // Dominant: Horizontal
                    absX > absY -> {
                        val inEdgeZone = startPos.x < leftEdgePx ||
                                startPos.x > (size.width - rightEdgePx)
                        if (!inEdgeZone) {
                            if (cumulativeX < -slopPx) {
                                onSwipeLeft()
                                change.consume()
                                return@awaitEachGesture
                            } else if (cumulativeX > slopPx) {
                                onSwipeRight()
                                change.consume()
                                return@awaitEachGesture
                            }
                        }
                        // In edge zone -> let Android OS handle system back gesture
                        return@awaitEachGesture
                    }
                    else -> return@awaitEachGesture
                }
            }
        } while (event.changes.any { it.pressed })
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Google Search intent helper (Section 16.2)
// ─────────────────────────────────────────────────────────────────────────────

private fun tryLaunchGoogleSearch(context: android.content.Context): Boolean {
    // 1. Try Google app directly
    val googlePkg = "com.google.android.googlequicksearchbox"
    val googleIntent = context.packageManager
        .getLaunchIntentForPackage(googlePkg)
    if (googleIntent != null) {
        runCatching {
            context.startActivity(googleIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return true
        }
    }

    // 2. Fall back to ACTION_WEB_SEARCH
    val webIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (webIntent.resolveActivity(context.packageManager) != null) {
        runCatching {
            context.startActivity(webIntent)
            return true
        }
    }

    return false
}

// ─────────────────────────────────────────────────────────────────────────────
// Search fallback dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SearchFallbackDialog(
    onDismiss: () -> Unit,
    onOpenBrowser: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Google Search unavailable",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Text(
                text = "The Google app isn't installed. You can open a browser to search instead.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenBrowser) {
                Text("Open browser")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// App text item (unchanged)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * A single app displayed as text only — the core minimalist aesthetic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppTextItem(
    app: LauncherApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = app.displayName,
        style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Normal
        ),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(vertical = 6.dp)
    )
}
