package dev.minimalist.launcher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.minimalist.launcher.apps.domain.model.LauncherApp

/**
 * Bottom sheet for long-press actions on an app item.
 * Supports rename, toggle favorite, toggle hidden.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionSheet(
    app: LauncherApp,
    onDismiss: () -> Unit,
    onRename: (String?) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleHidden: () -> Unit,
    onDetoxProtection: (() -> Unit)? = null
) {
    val sheetState = rememberModalBottomSheetState()
    var isRenaming by remember { mutableStateOf(false) }
    var renameText by remember { mutableStateOf(app.customName ?: app.originalName) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = app.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isRenaming) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("App name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val name = renameText.trim()
                            onRename(name.ifBlank { null })
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { isRenaming = false }) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = {
                        val name = renameText.trim()
                        onRename(name.ifBlank { null })
                    }) {
                        Text("Save")
                    }
                    if (app.customName != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { onRename(null) }) {
                            Text("Reset")
                        }
                    }
                }
            } else {
                TextButton(
                    onClick = { isRenaming = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Rename", modifier = Modifier.fillMaxWidth())
                }

                TextButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (app.isFavorite) "Remove from favorites" else "Add to favorites",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                TextButton(
                    onClick = onToggleHidden,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (app.isHidden) "Unhide" else "Hide",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (onDetoxProtection != null) {
                    TextButton(
                        onClick = onDetoxProtection,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "🛡️ Detox Protection",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
