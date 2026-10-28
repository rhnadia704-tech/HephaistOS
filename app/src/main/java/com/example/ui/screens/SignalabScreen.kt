package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkSignatureCluster
import com.example.model.SignalabModule
import com.example.model.SignalabSignMode
import com.example.model.SignalabSignOptions
import com.example.model.SignalabUiState
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalabScreen(
    uiState: SignalabUiState,
    availableFiles: List<File>,
    onSelectFilePath: (String) -> Unit,
    onUriSelected: (Uri) -> Unit,
    onLaunchScan: (String) -> Unit,
    onOpenSignDialog: () -> Unit,
    onDismissSignDialog: () -> Unit,
    onConfirmSignAndRepack: (SignalabSignOptions) -> Unit,
    onRefreshFiles: () -> Unit,
    onOpenTerminal: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBackToHome() }

    val clipboardManager = LocalClipboardManager.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onUriSelected(uri)
        }
    }

    ScaffoldWithHeader(
        uiState = uiState,
        onBackToHome = onBackToHome,
        onRefreshFiles = onRefreshFiles
    ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Module Banner
            item {
                SignalabModuleHeader(activeModule = uiState.activeModule)
            }

            // Target File Selection Card
            item {
                SignalabTargetSelectionCard(
                    selectedPath = uiState.selectedFilePath,
                    isScanning = uiState.isScanning,
                    availableFiles = availableFiles,
                    onPathChanged = onSelectFilePath,
                    onOpenFilePicker = {
                        filePickerLauncher.launch(
                            arrayOf(
                                "application/zip",
                                "application/octet-stream",
                                "*/*"
                            )
                        )
                    },
                    onLaunchScan = { onLaunchScan(uiState.selectedFilePath) }
                )
            }

            // Live Scan / Repack Progress
            if (uiState.isScanning || uiState.isResigningAndRepacking) {
                item {
                    SignalabProgressCard(
                        title = if (uiState.isScanning) "Scan & Unpack Haute Précision" else "Resignature & Recompilation LABOUT",
                        stepText = if (uiState.isScanning) uiState.scanStepText else uiState.resignStepText,
                        progress = if (uiState.isScanning) uiState.scanProgress else uiState.resignProgress
                    )
                }
            }

            // Status or Error Messages
            if (uiState.errorMessage != null) {
                item {
                    SignalabErrorCard(message = uiState.errorMessage)
                }
            }

            if (uiState.statusMessage != null && !uiState.isScanning && !uiState.isResigningAndRepacking) {
                item {
                    SignalabStatusCard(message = uiState.statusMessage)
                }
            }

            // Scan Results Card
            if (uiState.scanResult != null) {
                item {
                    SignalabScanResultCard(
                        result = uiState.scanResult,
                        onOpenSignDialog = onOpenSignDialog,
                        onCopy = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                        }
                    )
                }
            }

            // Final Result Card (LABOUT)
            if (uiState.finalRepackedPath != null) {
                item {
                    SignalabFinalResultCard(
                        outputPath = uiState.finalRepackedPath,
                        onCopyPath = {
                            clipboardManager.setText(AnnotatedString(uiState.finalRepackedPath))
                        },
                        onOpenTerminal = onOpenTerminal
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // SIGN YOUR ROM POPUP Dialog
    if (uiState.showSignDialog && uiState.scanResult != null) {
        SignalabSignDialog(
            scanResult = uiState.scanResult,
            initialOptions = uiState.signOptions,
            onDismiss = onDismissSignDialog,
            onConfirm = { options ->
                onConfirmSignAndRepack(options)
            }
        )
    }
}

@Composable
private fun ScaffoldWithHeader(
    uiState: SignalabUiState,
    onBackToHome: () -> Unit,
    onRefreshFiles: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackToHome,
                modifier = Modifier.testTag("signalab_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour à l'accueil"
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SIGNALAB",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "IMG Signer • Audit Verisin • Resignature & Repack",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = onRefreshFiles,
                modifier = Modifier.testTag("signalab_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualiser les fichiers"
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
private fun SignalabModuleHeader(activeModule: SignalabModule) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoFixHigh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column {
                Text(
                    text = activeModule.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = activeModule.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SignalabTargetSelectionCard(
    selectedPath: String,
    isScanning: Boolean,
    availableFiles: List<File>,
    onPathChanged: (String) -> Unit,
    onOpenFilePicker: () -> Unit,
    onLaunchScan: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Fichier Cible (IMG, EROFS, EXT4, ZIP, Payload)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedTextField(
                value = selectedPath,
                onValueChange = onPathChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signalab_target_input"),
                placeholder = { Text("Ex: /sdcard/HEPHAISTOS/system.img") },
                singleLine = true,
                trailingIcon = {
                    IconButton(
                        onClick = onOpenFilePicker,
                        modifier = Modifier.testTag("signalab_file_picker_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "Ouvrir le gestionnaire"
                        )
                    }
                }
            )

            // Quick chips
            if (availableFiles.isNotEmpty()) {
                Text(
                    text = "Fichiers détectés dans l'espace HEPHAISTOS :",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableFiles.take(8)) { file ->
                        AssistChip(
                            onClick = { onPathChanged(file.absolutePath) },
                            label = {
                                Text(
                                    text = file.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            Button(
                onClick = onLaunchScan,
                enabled = selectedPath.isNotBlank() && !isScanning,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signalab_launch_scan_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isScanning) "Déballage & Scan en cours..." else "LANCER LE SCAN",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun SignalabProgressCard(
    title: String,
    stepText: String,
    progress: Float
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Text(
                text = stepText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun SignalabErrorCard(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SignalabStatusCard(message: String) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SignalabScanResultCard(
    result: com.example.model.SignalabScanResult,
    onOpenSignDialog: () -> Unit,
    onCopy: (String) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Résultats du Scan Cryptographique",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                AssistChip(
                    onClick = {},
                    label = { Text(result.blueprint.containerFormat.uppercase()) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }

            // Summary grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ScanStatBox(label = "Fichiers extraits", value = "${result.blueprint.totalExtractedFiles}")
                ScanStatBox(label = "APKs audités", value = "${result.blueprint.totalApksFound}")
                ScanStatBox(label = "Clusters clés", value = "${result.clusters.size}")
                ScanStatBox(label = "Permissions XML", value = "${result.permissionFilesCount}")
            }

            HorizontalDivider()

            // Clusters preview
            Text(
                text = "Regroupement des Clés & Signatures Détectées :",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                result.clusters.forEach { cluster ->
                    ClusterRowItem(cluster = cluster)
                }
            }

            // Database location
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Base de données enregistrée dans :",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = result.blueprint.scanDataDirPath,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Button(
                onClick = onOpenSignDialog,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("signalab_open_sign_dialog_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SIGN YOUR ROM",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ScanStatBox(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ClusterRowItem(cluster: ApkSignatureCluster) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cluster.clusterId,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    if (cluster.isGoogleServices) {
                        Text(
                            text = "[GApps/Google]",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = "${cluster.associatedApks.size} applications • ${cluster.keyType} ${cluster.keySizeBits}b • ${cluster.schemesDisplay}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SignalabFinalResultCard(
    outputPath: String,
    onCopyPath: () -> Unit,
    onOpenTerminal: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ROM Recompilée & Resignée dans LABOUT !",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = outputPath,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopyPath,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copier")
                }

                Button(
                    onClick = onOpenTerminal,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Terminal")
                }
            }
        }
    }
}

@Composable
private fun SignalabSignDialog(
    scanResult: com.example.model.SignalabScanResult,
    initialOptions: SignalabSignOptions,
    onDismiss: () -> Unit,
    onConfirm: (SignalabSignOptions) -> Unit
) {
    var selectedMode by remember { mutableStateOf(initialOptions.mode) }
    var cn by remember { mutableStateOf(initialOptions.customCommonName) }
    var org by remember { mutableStateOf(initialOptions.customOrganization) }
    var country by remember { mutableStateOf(initialOptions.customCountry) }
    var preserveGapps by remember { mutableStateOf(initialOptions.preserveGoogleApps) }
    var cleanArt by remember { mutableStateOf(initialOptions.cleanArtArtifacts) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "SIGN YOUR ROM — Choix de Resignature",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Sélectionnez le mode de resignature pour les ${scanResult.clusters.size} clusters détectés :",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Radio Mode CUSTOM SIGN
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = SignalabSignMode.CUSTOM_SIGN }
                ) {
                    RadioButton(
                        selected = selectedMode == SignalabSignMode.CUSTOM_SIGN,
                        onClick = { selectedMode = SignalabSignMode.CUSTOM_SIGN }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "CUSTOM SIGN", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Clé personnalisée respectant le mapping et la structure cible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Radio Mode CLONE SIGN
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedMode = SignalabSignMode.CLONE_SIGN }
                ) {
                    RadioButton(
                        selected = selectedMode == SignalabSignMode.CLONE_SIGN,
                        onClick = { selectedMode = SignalabSignMode.CLONE_SIGN }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = "CLONE SIGN", fontWeight = FontWeight.Bold)
                        Text(
                            text = "Reproduction miroir des Subject DNs et de la chaîne de confiance",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Custom Identity Form if CUSTOM SIGN
                AnimatedVisibility(visible = selectedMode == SignalabSignMode.CUSTOM_SIGN) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = cn,
                            onValueChange = { cn = it },
                            label = { Text("Nom Commun (CN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = org,
                            onValueChange = { org = it },
                            label = { Text("Organisation (O)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it },
                            label = { Text("Code Pays (C)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                HorizontalDivider()

                // Preserve GApps
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Préserver les GApps", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Conserve les services Google intacts (SafetyNet)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = preserveGapps, onCheckedChange = { preserveGapps = it })
                }

                // Clean ART Artifacts
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Purger odex/vdex/oat/fsvmeta", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Dexopt propre au 1er boot (évite bootloop)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = cleanArt, onCheckedChange = { cleanArt = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(
                        SignalabSignOptions(
                            mode = selectedMode,
                            customCommonName = cn,
                            customOrganization = org,
                            customCountry = country,
                            preserveGoogleApps = preserveGapps,
                            cleanArtArtifacts = cleanArt
                        )
                    )
                },
                modifier = Modifier.testTag("signalab_confirm_sign_button")
            ) {
                Text("LANCER LA RESIGNATURE")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
