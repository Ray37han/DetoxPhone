package dev.minimalist.launcher.ui.apps

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.minimalist.launcher.apps.domain.model.LauncherApp
import dev.minimalist.launcher.ui.home.AppActionSheet
import kotlinx.coroutines.launch

/**
 * Full app list screen — text-only alphabetical listing with inline search
 * and alphabet fast-scrubber.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AllAppsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetox: () -> Unit = {},
    viewModel: AllAppsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showAppActions by remember { mutableStateOf<LauncherApp?>(null) }

    // Dismiss soft keyboard when user scrolls the app list
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            keyboardController?.hide()
        }
    }

    BackHandler {
        if (state.searchQuery.isNotEmpty()) {
            viewModel.clearSearch()
        } else {
            onNavigateBack()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 8.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Search field ──
            TextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                leadingIcon = {
                    Text(
                        text = "←",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f),
                        modifier = Modifier
                            .clickable {
                                keyboardController?.hide()
                                onNavigateBack()
                            }
                            .padding(8.dp)
                    )
                },
                placeholder = {
                    Text(
                        "Search apps…",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (state.filteredApps.size == 1) {
                            val targetApp = state.filteredApps.first()
                            if (targetApp.packageName == context.packageName) {
                                onNavigateToDetox()
                            } else {
                                launchApp(context, targetApp)
                            }
                            viewModel.clearSearch()
                            keyboardController?.hide()
                        }
                    }
                ),
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        Text(
                            text = "✕",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                            modifier = Modifier
                                .clickable { viewModel.clearSearch() }
                                .padding(8.dp)
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground,
                    unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                    focusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                ),
                textStyle = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ── App list + Alphabet Indexer ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Apps LazyColumn
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(state.settings.listItemSpacing.dp)
                ) {
                    items(
                        items = state.filteredApps,
                        key = { it.packageName }
                    ) { app ->
                        Text(
                            text = app.displayName,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    onClick = {
                                        if (app.packageName == context.packageName) {
                                            onNavigateToDetox()
                                        } else {
                                            launchApp(context, app)
                                        }
                                        viewModel.clearSearch()
                                    },
                                    onLongClick = { showAppActions = app }
                                )
                                .padding(vertical = 6.dp)
                        )
                    }
                }

                // Alphabet quick scrubber on right edge (when not searching)
                if (state.searchQuery.isEmpty() && state.filteredApps.size > 8) {
                    val alphabet = remember { ('A'..'Z').toList() }
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        alphabet.forEach { letter ->
                            Text(
                                text = letter.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .clickable {
                                        val targetIndex = state.filteredApps.indexOfFirst {
                                            it.displayName.startsWith(letter, ignoreCase = true)
                                        }
                                        if (targetIndex >= 0) {
                                            coroutineScope.launch {
                                                listState.scrollToItem(targetIndex)
                                            }
                                        }
                                    }
                                    .padding(vertical = 1.dp, horizontal = 2.dp)
                            )
                        }
                    }
                }
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
    }
}

private fun launchApp(context: Context, app: LauncherApp) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(it)
        }
    } catch (_: Exception) { /* App may have been uninstalled */ }
}
