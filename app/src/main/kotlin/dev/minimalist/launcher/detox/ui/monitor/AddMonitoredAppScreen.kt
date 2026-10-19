package dev.minimalist.launcher.detox.ui.monitor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.minimalist.launcher.detox.domain.model.ProtectionLevel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMonitoredAppScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddMonitoredAppViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var expandedPackage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Protect an App", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            TextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.onSearch(it) },
                placeholder = { Text("Search apps...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(uiState.filteredApps, key = { it.packageName }) { app ->
                        AppSelectionRow(
                            app = app,
                            isExpanded = expandedPackage == app.packageName,
                            onTap = {
                                expandedPackage = if (expandedPackage == app.packageName) null
                                else app.packageName
                            },
                            onSelectLevel = { level ->
                                viewModel.addApp(app.packageName, level)
                                expandedPackage = null
                            }
                        )
                    }
                    item { Spacer(Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSelectionRow(
    app: AppItem,
    isExpanded: Boolean,
    onTap: () -> Unit,
    onSelectLevel: (ProtectionLevel) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onTap,
        colors = CardDefaults.cardColors(
            containerColor = if (app.isMonitored)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Text(
                        text = app.packageName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (app.isMonitored) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Monitored",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Protection level picker (expanded)
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select protection level:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    val levels = listOf(
                        ProtectionLevel.FRICTION_ONLY to "Friction",
                        ProtectionLevel.FRICTION_TIMEBOX to "Timebox",
                        ProtectionLevel.FULL_BLOCK to "Block"
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        levels.forEachIndexed { index, (level, label) ->
                            SegmentedButton(
                                selected = false,
                                onClick = { onSelectLevel(level) },
                                shape = SegmentedButtonDefaults.itemShape(index, levels.size)
                            ) {
                                Text(label, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
