package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AheAction
import com.example.model.AheExtension
import com.example.model.AheField
import com.example.model.AheSection
import com.example.model.TerminalEntry

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AheDynamicScreen(
    extension: AheExtension,
    availableProjects: List<String>,
    availableKeysets: List<String>,
    isExecuting: Boolean,
    currentStepText: String,
    progress: Float,
    logs: List<TerminalEntry>,
    onExecuteAction: (AheAction, Map<String, String>) -> Unit,
    onClearLogs: () -> Unit,
    onBack: () -> Unit,
    onUninstall: () -> Unit,
    onOpenGlobalTerminal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val tabs = extension.manifest.ui.tabs
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val formValues = remember {
        mutableStateMapOf<String, String>().apply {
            // Seed defaults
            tabs.forEach { t ->
                t.sections.forEach { s ->
                    s.fields.forEach { f ->
                        put(f.id, f.default)
                    }
                }
            }
        }
    }

    val terminalListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            terminalListState.animateScrollToItem(logs.size - 1)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), repeatMode = RepeatMode.Reverse),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("ahe_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = extension.manifest.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "v${extension.manifest.version} • Par ${extension.manifest.author}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = onOpenGlobalTerminal,
                modifier = Modifier.testTag("ahe_open_global_terminal_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Ouvrir le Terminal Global",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            IconButton(
                onClick = onUninstall,
                modifier = Modifier.testTag("ahe_uninstall_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Désinstaller",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        // Header Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isExecuting) MaterialTheme.colorScheme.primary else Color(0xFF2ECC71))
                                .alpha(if (isExecuting) pulseAlpha else 1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isExecuting) "Exécution en cours..." else "Module .ahe Connecté",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "ID: ${extension.manifest.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (extension.manifest.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = extension.manifest.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isExecuting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currentStepText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Tabs if multiple
        if (tabs.size > 1) {
            SecondaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(tab.title) },
                        icon = { Icon(getIconForName(tab.icon), contentDescription = null, modifier = Modifier.size(18.dp)) }
                    )
                }
            }
        }

        // Active Tab Content
        val currentTab = tabs.getOrNull(selectedTabIndex) ?: tabs.firstOrNull()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentTab != null) {
                items(currentTab.sections) { section ->
                    AheSectionCard(
                        section = section,
                        formValues = formValues,
                        availableProjects = availableProjects,
                        availableKeysets = availableKeysets,
                        isExecuting = isExecuting,
                        onExecuteAction = { action -> onExecuteAction(action, formValues.toMap()) }
                    )
                }
            }

            // Real-Time Terminal Output Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF14171A)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Terminal,
                                    contentDescription = null,
                                    tint = Color(0xFF00E676),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Terminal d'Exécution .ahe",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = onClearLogs,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Effacer",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFF2A2E33))

                        LazyColumn(
                            state = terminalListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (logs.isEmpty()) {
                                item {
                                    Text(
                                        text = "$ Terminal en attente d'action...",
                                        color = Color.Gray,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            } else {
                                items(logs) { entry ->
                                    val textColor = when {
                                        entry.isError -> Color(0xFFFF5252)
                                        entry.isSuccess -> Color(0xFF69F0AE)
                                        entry.isCommand -> Color(0xFF40C4FF)
                                        else -> Color(0xFFE0E0E0)
                                    }
                                    Text(
                                        text = entry.text,
                                        color = textColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        modifier = Modifier.padding(vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AheSectionCard(
    section: AheSection,
    formValues: MutableMap<String, String>,
    availableProjects: List<String>,
    availableKeysets: List<String>,
    isExecuting: Boolean,
    onExecuteAction: (AheAction) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (section.title.isNotBlank()) {
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            if (section.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = section.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (section.title.isNotBlank() || section.subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fields
            section.fields.forEach { field ->
                AheFieldInput(
                    field = field,
                    currentValue = formValues[field.id] ?: field.default,
                    availableProjects = availableProjects,
                    availableKeysets = availableKeysets,
                    onValueChange = { formValues[field.id] = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Single action
            section.action?.let { action ->
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { onExecuteAction(action) },
                    enabled = !isExecuting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ahe_action_${action.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = getIconForName(action.icon),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(action.label)
                }
            }

            // Multiple actions
            if (section.actions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    section.actions.forEach { action ->
                        FilledTonalButton(
                            onClick = { onExecuteAction(action) },
                            enabled = !isExecuting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("ahe_action_${action.id}"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = getIconForName(action.icon),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(action.label, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AheFieldInput(
    field: AheField,
    currentValue: String,
    availableProjects: List<String>,
    availableKeysets: List<String>,
    onValueChange: (String) -> Unit
) {
    when (field.type) {
        "dropdown" -> {
            val options = when (field.source) {
                "unpacked_projects" -> availableProjects.ifEmpty { listOf("Aucun projet déballé") }
                "keysets" -> availableKeysets.ifEmpty { listOf("Aucun Keyset") }
                else -> listOf("Option 1", "Option 2")
            }
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentValue.ifBlank { options.firstOrNull() ?: "" },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(field.label) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("ahe_field_${field.id}")
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    options.forEach { opt ->
                        DropdownMenuItem(
                            text = { Text(opt) },
                            onClick = {
                                onValueChange(opt)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
        "number" -> {
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ahe_field_${field.id}")
            )
        }
        else -> {
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChange,
                label = { Text(field.label) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ahe_field_${field.id}")
            )
        }
    }
}

fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "bolt", "flash" -> Icons.Default.Bolt
        "build", "tools" -> Icons.Default.Build
        "play" -> Icons.Default.PlayArrow
        "folder" -> Icons.Default.Folder
        "refresh" -> Icons.Default.Refresh
        "check" -> Icons.Default.CheckCircle
        "extension" -> Icons.Default.Extension
        else -> Icons.Default.Dashboard
    }
}
