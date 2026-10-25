package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkSignItem
import com.example.model.ApkVerificationDetail
import com.example.model.GemeauSubScreen
import com.example.model.GemeauTab
import com.example.model.GemeauUiState
import com.example.model.GemeauVerifySubTab
import com.example.model.KeysetInfo
import com.example.model.RomVerificationAudit
import com.example.model.TerminalEntry
import com.example.model.SignatureGroup
import com.example.model.SignatureGroupApkEntry
import com.example.ui.components.InternalFilePickerDialog
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Checkbox
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GemeauScreen(
    gemeauState: GemeauUiState,
    onTabSelected: (GemeauTab) -> Unit,
    onRefreshKeysets: () -> Unit,
    onSelectKeyset: (KeysetInfo) -> Unit,
    onUpdateKeymakerForm: (name: String, cn: String, org: String, country: String, alg: String, years: Int) -> Unit,
    onGenerateKeyset: () -> Unit,
    onSelectApks: (List<File>) -> Unit,
    onSetKeyRoleForApk: (String) -> Unit,
    onSignApks: (KeysetInfo, String) -> Unit,
    onSelectRomFile: (File?) -> Unit,
    onSignRom: (KeysetInfo, Boolean) -> Unit,
    onSelectVerifySubTab: (GemeauVerifySubTab) -> Unit = {},
    onSelectApksForVerification: (List<File>) -> Unit = {},
    onSelectRomForApkExtraction: (File?) -> Unit = {},
    onExtractApksFromRom: () -> Unit = {},
    onVerifyApks: () -> Unit = {},
    onSelectRomForAudit: (File?) -> Unit = {},
    onAuditRom: () -> Unit = {},
    onSelectRSignerTarget: (File?) -> Unit = {},
    onAnalyzeRSignerTarget: () -> Unit = {},
    onExecuteRSigner: (KeysetInfo, Boolean, Boolean) -> Unit = { _, _, _ -> },
    onGenerateKeysetForRSigner: (name: String, cn: String, org: String, country: String, alg: String, years: Int, onComplete: (KeysetInfo) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onNavigateSubScreen: (GemeauSubScreen) -> Unit = {},
    onNavigateBackInGemeau: () -> Unit = {},
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (gemeauState.currentSubScreen == GemeauSubScreen.HUB) {
            onBackToHome()
        } else {
            onNavigateBackInGemeau()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    var showApkFilePicker by remember { mutableStateOf(false) }
    var showRomFilePicker by remember { mutableStateOf(false) }

    var showVerifyApkFilePicker by remember { mutableStateOf(false) }
    var showVerifyRomExtractPicker by remember { mutableStateOf(false) }
    var showVerifyRomAuditPicker by remember { mutableStateOf(false) }

    var showChooseKeyDialogForApk by remember { mutableStateOf(false) }
    var showChooseKeyDialogForRom by remember { mutableStateOf(false) }
    var pendingWithVbmeta by remember { mutableStateOf(false) }

    var showRSignerTargetPicker by remember { mutableStateOf(false) }
    var showRSignerKeysetDialog by remember { mutableStateOf(false) }
    var pendingRSignerWithVbmeta by remember { mutableStateOf(false) }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportDialogContent by remember { mutableStateOf("") }
    var reportDialogTitle by remember { mutableStateOf("") }

    val topTitle = when (gemeauState.currentSubScreen) {
        GemeauSubScreen.HUB -> "GÉMEAU"
        GemeauSubScreen.KEYMAKER -> "GÉMEAU > Keymaker"
        GemeauSubScreen.SIGNAPK -> "GÉMEAU > SIGNAPK"
        GemeauSubScreen.ROMSIGN -> "GÉMEAU > ROMsign"
        GemeauSubScreen.VERIFICATION_HUB -> "GÉMEAU > VÉRIFICATION"
        GemeauSubScreen.VERIFY_APP -> "Vérification des Applications (APKs)"
        GemeauSubScreen.VERIFY_ROM -> "Audit de ROM Complète"
        GemeauSubScreen.RSIGNER -> "GÉMEAU > R-SIGNER"
    }

    val topSubtitle = when (gemeauState.currentSubScreen) {
        GemeauSubScreen.HUB -> "Atelier Cryptographique, Signatures & Audits de ROMs"
        GemeauSubScreen.KEYMAKER -> "Générateur de clés AOSP, AVB et certificats X.509"
        GemeauSubScreen.SIGNAPK -> "Signature unitaire et par lots d'APKs"
        GemeauSubScreen.ROMSIGN -> "Resignature globale de ROMs, GSIs et alignement SELinux"
        GemeauSubScreen.VERIFICATION_HUB -> "Choisissez le mode d'audit cryptographique"
        GemeauSubScreen.VERIFY_APP -> "Audit des schémas v1, v2, v3, v4 et certificats"
        GemeauSubScreen.VERIFY_ROM -> "Audit de la chaîne de confiance et de mac_permissions.xml"
        GemeauSubScreen.RSIGNER -> "Resignature Intelligente de ROMs, GSIs & Alignement 1:1"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (gemeauState.currentSubScreen == GemeauSubScreen.HUB) {
                            onBackToHome()
                        } else {
                            onNavigateBackInGemeau()
                        }
                    },
                    modifier = Modifier.testTag("gemeau_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (gemeauState.currentSubScreen) {
                            GemeauSubScreen.HUB -> Icons.Default.VpnKey
                            GemeauSubScreen.KEYMAKER -> Icons.Default.Key
                            GemeauSubScreen.SIGNAPK -> Icons.Default.Layers
                            GemeauSubScreen.ROMSIGN -> Icons.Default.Build
                            GemeauSubScreen.RSIGNER -> Icons.Default.Security
                            else -> Icons.Default.VerifiedUser
                        },
                        contentDescription = "GEMEAU",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = topTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Signer v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = topSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Main SubScreen Router
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (gemeauState.currentSubScreen) {
                GemeauSubScreen.HUB -> {
                    GemeauHubView(
                        state = gemeauState,
                        onOpenFeature = onNavigateSubScreen
                    )
                }
                GemeauSubScreen.KEYMAKER -> {
                    KeymakerPage(
                        state = gemeauState,
                        onUpdateForm = onUpdateKeymakerForm,
                        onGenerateKeyset = onGenerateKeyset,
                        onRefreshKeysets = onRefreshKeysets,
                        onSelectKeyset = onSelectKeyset
                    )
                }
                GemeauSubScreen.SIGNAPK -> {
                    SignApkPage(
                        state = gemeauState,
                        onBrowseClick = { showApkFilePicker = true },
                        onStartSignClick = {
                            if (gemeauState.availableKeysets.isEmpty()) {
                                onRefreshKeysets()
                            }
                            showChooseKeyDialogForApk = true
                        }
                    )
                }
                GemeauSubScreen.ROMSIGN -> {
                    RomSignPage(
                        state = gemeauState,
                        onBrowseRom = { showRomFilePicker = true },
                        onStartSignRom = { withVbmeta ->
                            pendingWithVbmeta = withVbmeta
                            if (gemeauState.availableKeysets.isEmpty()) {
                                onRefreshKeysets()
                            }
                            showChooseKeyDialogForRom = true
                        },
                        onViewReport = { path, title ->
                            try {
                                val f = File(path)
                                if (f.exists()) {
                                    reportDialogContent = f.readText()
                                    reportDialogTitle = title
                                    showReportDialog = true
                                }
                            } catch (e: Exception) {
                                reportDialogContent = "Erreur lecture rapport: ${e.message}"
                                reportDialogTitle = title
                                showReportDialog = true
                            }
                        }
                    )
                }
                GemeauSubScreen.VERIFICATION_HUB -> {
                    GemeauVerificationCarouselView(
                        onSelectVerifyApp = { onNavigateSubScreen(GemeauSubScreen.VERIFY_APP) },
                        onSelectVerifyRom = { onNavigateSubScreen(GemeauSubScreen.VERIFY_ROM) }
                    )
                }
                GemeauSubScreen.VERIFY_APP -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        VerifyAppSection(
                            state = gemeauState,
                            onBrowseApks = { showVerifyApkFilePicker = true },
                            onBrowseRomForApks = { showVerifyRomExtractPicker = true },
                            onExtractApksFromRom = onExtractApksFromRom,
                            onVerifyApks = onVerifyApks,
                            onRemoveApk = { file ->
                                val updated = gemeauState.verifySelectedApks.filter { it.absolutePath != file.absolutePath }
                                onSelectApksForVerification(updated)
                            }
                        )
                    }
                }
                GemeauSubScreen.VERIFY_ROM -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        VerifyRomSection(
                            state = gemeauState,
                            onBrowseRomForAudit = { showVerifyRomAuditPicker = true },
                            onAuditRom = onAuditRom,
                            onViewReport = { path, title ->
                                try {
                                    val f = File(path)
                                    if (f.exists()) {
                                        reportDialogContent = f.readText()
                                        reportDialogTitle = title
                                        showReportDialog = true
                                    }
                                } catch (e: Exception) {
                                    reportDialogContent = "Erreur lecture rapport: ${e.message}"
                                    reportDialogTitle = title
                                    showReportDialog = true
                                }
                            }
                        )
                    }
                }
                GemeauSubScreen.RSIGNER -> {
                    RSignerPage(
                        state = gemeauState,
                        onBrowseTarget = { showRSignerTargetPicker = true },
                        onAnalyzeTarget = onAnalyzeRSignerTarget,
                        onStartSign = { withVbmeta ->
                            pendingRSignerWithVbmeta = withVbmeta
                            if (gemeauState.availableKeysets.isEmpty()) {
                                onRefreshKeysets()
                            }
                            showRSignerKeysetDialog = true
                        },
                        onViewReport = { path, title ->
                            try {
                                val f = File(path)
                                if (f.exists()) {
                                    reportDialogContent = f.readText()
                                    reportDialogTitle = title
                                    showReportDialog = true
                                }
                            } catch (e: Exception) {
                                reportDialogContent = "Erreur lecture rapport: ${e.message}"
                                reportDialogTitle = title
                                showReportDialog = true
                            }
                        }
                    )
                }
            }
        }
    }

    // Modal File Picker for APKs
    if (showApkFilePicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showApkFilePicker = false },
            onFileSelected = { file ->
                showApkFilePicker = false
                val current = gemeauState.selectedApks.map { it.file }.toMutableList()
                if (!current.contains(file)) {
                    current.add(file)
                }
                onSelectApks(current)
            }
        )
    }

    // Modal File Picker for ROMs / GSIs / Imgs
    if (showRomFilePicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showRomFilePicker = false },
            onFileSelected = { file ->
                showRomFilePicker = false
                onSelectRomFile(file)
            }
        )
    }

    // Modal File Picker for Verification APKs
    if (showVerifyApkFilePicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showVerifyApkFilePicker = false },
            onFileSelected = { file ->
                showVerifyApkFilePicker = false
                val current = gemeauState.verifySelectedApks.toMutableList()
                if (!current.any { it.absolutePath == file.absolutePath }) {
                    current.add(file)
                }
                onSelectApksForVerification(current)
            }
        )
    }

    // Modal File Picker for Extracting APKs from ROM
    if (showVerifyRomExtractPicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showVerifyRomExtractPicker = false },
            onFileSelected = { file ->
                showVerifyRomExtractPicker = false
                onSelectRomForApkExtraction(file)
            }
        )
    }

    // Modal File Picker for Auditing Full ROM
    if (showVerifyRomAuditPicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showVerifyRomAuditPicker = false },
            onFileSelected = { file ->
                showVerifyRomAuditPicker = false
                onSelectRomForAudit(file)
            }
        )
    }

    // Dialog: Select Keyset for APK Signing
    if (showChooseKeyDialogForApk) {
        ChooseKeyDialog(
            keysets = gemeauState.availableKeysets,
            selectedKeyset = gemeauState.selectedKeyset,
            selectedRole = gemeauState.selectedKeyRoleForApk,
            showRoleSelector = true,
            onSelectRole = onSetKeyRoleForApk,
            onConfirm = { chosenKeyset, chosenRole ->
                showChooseKeyDialogForApk = false
                onSignApks(chosenKeyset, chosenRole)
            },
            onDismiss = { showChooseKeyDialogForApk = false }
        )
    }

    // Dialog: Select Keyset for ROM Signing
    if (showChooseKeyDialogForRom) {
        ChooseKeyDialog(
            keysets = gemeauState.availableKeysets,
            selectedKeyset = gemeauState.selectedKeyset,
            selectedRole = "platform",
            showRoleSelector = false,
            onSelectRole = {},
            onConfirm = { chosenKeyset, _ ->
                showChooseKeyDialogForRom = false
                onSignRom(chosenKeyset, pendingWithVbmeta)
            },
            onDismiss = { showChooseKeyDialogForRom = false }
        )
    }

    // Modal File Picker for R-SIGNER Target (.img, .zip, payload.bin, ROM, GSI)
    if (showRSignerTargetPicker) {
        InternalFilePickerDialog(
            initialDirectory = null,
            onDismiss = { showRSignerTargetPicker = false },
            onFileSelected = { file ->
                showRSignerTargetPicker = false
                onSelectRSignerTarget(file)
            }
        )
    }

    // Modal Dialog: Keyset Selector / 1:1 Creator for R-SIGNER
    if (showRSignerKeysetDialog) {
        RSignerKeysetDialog(
            keysets = gemeauState.availableKeysets,
            selectedKeyset = gemeauState.selectedKeyset,
            detectedGroups = gemeauState.rsignerDetectedGroups,
            withVbmeta = pendingRSignerWithVbmeta,
            onConfirmExisting = { chosenKeyset, preserveGapps ->
                showRSignerKeysetDialog = false
                onExecuteRSigner(chosenKeyset, pendingRSignerWithVbmeta, preserveGapps)
            },
            onConfirmCreateNew = { name, cn, org, country, alg, years, preserveGapps ->
                showRSignerKeysetDialog = false
                onGenerateKeysetForRSigner(name, cn, org, country, alg, years) { newKeyset ->
                    onExecuteRSigner(newKeyset, pendingRSignerWithVbmeta, preserveGapps)
                }
            },
            onDismiss = { showRSignerKeysetDialog = false }
        )
    }

    // Modal Report Viewer
    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = reportDialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { showReportDialog = false }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141414))
                        .padding(12.dp)
                ) {
                    val scrollState = rememberScrollState()
                    Text(
                        text = reportDialogContent,
                        color = Color(0xFFEEEEEE),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        modifier = Modifier.verticalScroll(scrollState)
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showReportDialog = false }) {
                    Text("Fermer")
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 1: KEYMAKER
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KeymakerPage(
    state: GemeauUiState,
    onUpdateForm: (name: String, cn: String, org: String, country: String, alg: String, years: Int) -> Unit,
    onGenerateKeyset: () -> Unit,
    onRefreshKeysets: () -> Unit,
    onSelectKeyset: (KeysetInfo) -> Unit
) {
    var name by remember(state.newKeysetName) { mutableStateOf(state.newKeysetName) }
    var cn by remember(state.commonName) { mutableStateOf(state.commonName) }
    var org by remember(state.organization) { mutableStateOf(state.organization) }
    var country by remember(state.countryCode) { mutableStateOf(state.countryCode) }
    var algorithm by remember(state.keyAlgorithm) { mutableStateOf(state.keyAlgorithm) }
    var years by remember(state.validityYears) { mutableStateOf(state.validityYears.toString()) }

    var expandedAlgDropdown by remember { mutableStateOf(false) }
    val algorithms = listOf("RSA 2048", "RSA 4096", "EC P-256")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Keyset Form Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Création de Keyset Personnalisé",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Génération d'un jeu de clés complet pour signer une ROM entière",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                        },
                        label = { Text("Nom du Keyset") },
                        modifier = Modifier.fillMaxWidth().testTag("keymaker_name_input"),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = cn,
                            onValueChange = {
                                cn = it
                                onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                            },
                            label = { Text("Common Name (CN)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = country,
                            onValueChange = {
                                country = it
                                onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                            },
                            label = { Text("Pays (ex: FR)") },
                            modifier = Modifier.width(100.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = org,
                        onValueChange = {
                            org = it
                            onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                        },
                        label = { Text("Organisation (O)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        ExposedDropdownMenuBox(
                            expanded = expandedAlgDropdown,
                            onExpandedChange = { expandedAlgDropdown = !expandedAlgDropdown },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = algorithm,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Algorithme") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAlgDropdown) },
                                modifier = Modifier.menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expandedAlgDropdown,
                                onDismissRequest = { expandedAlgDropdown = false }
                            ) {
                                algorithms.forEach { alg ->
                                    DropdownMenuItem(
                                        text = { Text(alg) },
                                        onClick = {
                                            algorithm = alg
                                            expandedAlgDropdown = false
                                            onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        OutlinedTextField(
                            value = years,
                            onValueChange = {
                                years = it
                                onUpdateForm(name, cn, org, country, algorithm, years.toIntOrNull() ?: 25)
                            },
                            label = { Text("Validité (ans)") },
                            modifier = Modifier.width(110.dp),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Jeu AOSP/AVB complet généré dans /sdcard/HEPHAISTOS/SIGNER/KEYSET :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• releasekey, platform, shared, media, networkstack\n• avb.pem & avb.pk8 (signature vbmeta.img)",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onGenerateKeyset,
                        enabled = !state.isExecuting && name.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("launch_generate_keyset_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("GÉNÉRER LE KEYSET COMPLET", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Available Keysets Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Trousseaux Détectés (${state.availableKeysets.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onRefreshKeysets) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Rafraîchir")
                        }
                    }

                    if (state.availableKeysets.isEmpty()) {
                        Text(
                            text = "Aucun keyset trouvé dans /sdcard/HEPHAISTOS/SIGNER/KEYSET/. Utilisez le formulaire ci-dessus pour en générer un.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        state.availableKeysets.forEach { k ->
                            val isSelected = state.selectedKeyset?.name == k.name
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectKeyset(k) },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(k.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            text = "${k.algorithm} • ${k.keyCount} clés • ${k.creationDate} • AVB: ${if (k.hasAvbKey) "Oui" else "Non"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Sélectionné",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Console Card
        if (state.isExecuting || state.progress > 0f || state.logs.isNotEmpty()) {
            item {
                GemeauConsoleCard(
                    isExecuting = state.isExecuting,
                    progress = state.progress,
                    currentStep = state.currentStepText,
                    logs = state.logs,
                    reportPath = state.lastGeneratedReportPath,
                    outputPath = state.lastSignedOutputPath,
                    onViewReport = {}
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 2: SIGNAPK
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun SignApkPage(
    state: GemeauUiState,
    onBrowseClick: () -> Unit,
    onStartSignClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Signature d'un ou Plusieurs APKs",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sélectionnez vos fichiers APKs. Vous choisirez ensuite la clé du Keyset souhaitée dans un popup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onBrowseClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("browse_apks_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Rechercher des APKs à signer...")
                    }

                    if (state.selectedApks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "APKs sélectionnés (${state.selectedApks.size}) :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        state.selectedApks.forEach { apk ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(apk.name, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        Text(apk.formattedSize, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = onStartSignClick,
                            enabled = !state.isExecuting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("launch_sign_apks_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SIGNER LES APKS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Live Console Card
        if (state.isExecuting || state.progress > 0f || state.logs.isNotEmpty()) {
            item {
                GemeauConsoleCard(
                    isExecuting = state.isExecuting,
                    progress = state.progress,
                    currentStep = state.currentStepText,
                    logs = state.logs,
                    reportPath = state.lastGeneratedReportPath,
                    outputPath = state.lastSignedOutputPath,
                    onViewReport = {}
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 3: ROMSIGN
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun RomSignPage(
    state: GemeauUiState,
    onBrowseRom: () -> Unit,
    onStartSignRom: (withVbmeta: Boolean) -> Unit,
    onViewReport: (path: String, title: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ROM File Selection Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resignature Intégrale de Custom ROM / GSI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sélectionnez le fichier ROM (ZIP, partition IMG, GSI ou payload).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onBrowseRom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("browse_rom_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (state.selectedRomFile == null) "Rechercher la ROM / GSI / IMG..." else "Changer de fichier ROM...")
                    }

                    if (state.selectedRomFile != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(state.selectedRomFile.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = "${"%.2f".format(state.selectedRomFile.length() / 1048576.0)} Mo • ${state.selectedRomFile.extension.uppercase()}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trust Chain & Security Architecture Info Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Intelligence de Sécurité & Préservation de Confiance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    GemeauFeatureRow(
                        title = "Préservation Intelligente des GApps",
                        desc = "Détection et exclusion automatique des paquets Google (Play Services, Play Store, GSF). Leurs signatures officielles restent 100% intactes pour éviter les crashs SafetyNet et Play Protect."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GemeauFeatureRow(
                        title = "Réplication 1:1 de la Chaîne de Confiance",
                        desc = "Chaque grappe de clés d'origine de l'OS source est analysée et projetée fidèlement sur le nouveau Keyset. Les applications partageant la même clé conservent le même lien de signature."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GemeauFeatureRow(
                        title = "Régénération mac_permissions.xml (SELinux)",
                        desc = "Mise à jour des signatures de certificats dans les politiques MMAC pour maintenir les transitions de domaines SELinux platform et privapp."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    GemeauFeatureRow(
                        title = "Purge & Invalidation dexopt (odex, vdex, oat)",
                        desc = "Élimination des caches pré-compilés incompatibles pour forcer une recompilation propre au boot, prévenant tout risque de bootloop."
                    )
                }
            }
        }

        // Dual Action Buttons: SIGNER vs SIGNER WITH VBMETA
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { onStartSignRom(false) },
                    enabled = !state.isExecuting && state.selectedRomFile != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_rom_sign_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SIGNER (Resigner la ROM)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = { onStartSignRom(true) },
                    enabled = !state.isExecuting && state.selectedRomFile != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("launch_rom_sign_vbmeta_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(imageVector = Icons.Default.Build, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("SIGNER WITH VBMETA (Resigner + Générer vbmeta.img)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Live Console Card
        if (state.isExecuting || state.progress > 0f || state.logs.isNotEmpty()) {
            item {
                GemeauConsoleCard(
                    isExecuting = state.isExecuting,
                    progress = state.progress,
                    currentStep = state.currentStepText,
                    logs = state.logs,
                    reportPath = state.lastGeneratedReportPath,
                    outputPath = state.lastSignedOutputPath,
                    onViewReport = { path -> onViewReport(path, "Rapport d'Audit & Sécurité de Resignature") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOG: CHOOSE KEYSET POPUP
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ChooseKeyDialog(
    keysets: List<KeysetInfo>,
    selectedKeyset: KeysetInfo?,
    selectedRole: String,
    showRoleSelector: Boolean,
    onSelectRole: (String) -> Unit,
    onConfirm: (KeysetInfo, String) -> Unit,
    onDismiss: () -> Unit
) {
    var chosenKeyset by remember { mutableStateOf(selectedKeyset ?: keysets.firstOrNull()) }
    var chosenRole by remember { mutableStateOf(selectedRole) }
    val roles = listOf("releasekey", "platform", "shared", "media", "networkstack")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Sélectionner le Keyset à utiliser",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (keysets.isEmpty()) {
                    Text(
                        text = "Aucun keyset disponible dans /sdcard/HEPHAISTOS/SIGNER/KEYSET/. Veuillez d'abord en créer un dans l'onglet Keymaker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "Choisissez le trousseau de clés :",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    keysets.forEach { k ->
                        val isSel = chosenKeyset?.name == k.name
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { chosenKeyset = k },
                            color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = isSel, onClick = { chosenKeyset = k })
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(k.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text("${k.algorithm} • ${k.keyCount} clés", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    if (showRoleSelector) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Rôle de la clé pour cet APK :",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            roles.take(3).forEach { r ->
                                val isRoleSel = chosenRole == r
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            chosenRole = r
                                            onSelectRole(r)
                                        },
                                    color = if (isRoleSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                                ) {
                                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                        Text(
                                            r,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isRoleSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    chosenKeyset?.let { k ->
                        onConfirm(k, chosenRole)
                    }
                },
                enabled = chosenKeyset != null
            ) {
                Text("Valider & Signer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun GemeauFeatureRow(title: String, desc: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(top = 6.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(text = desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GemeauConsoleCard(
    isExecuting: Boolean,
    progress: Float,
    currentStep: String,
    logs: List<TerminalEntry>,
    reportPath: String?,
    outputPath: String?,
    onViewReport: (path: String) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "gemeau_progress_anim"
    )

    val logListState = rememberLazyListState()
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            logListState.animateScrollToItem(logs.size - 1)
        }
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("gemeau_console_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
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
                            .background(
                                when {
                                    isExecuting -> Color(0xFFFFA000)
                                    animatedProgress >= 1f -> Color(0xFF4CAF50)
                                    else -> Color(0xFF40C4FF)
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isExecuting) "Signature en cours..." else "Opération terminée",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = currentStep.ifEmpty { "En attente d'instruction..." },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Embedded Terminal Window
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 160.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF141414)
            ) {
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                ) {
                    if (logs.isEmpty()) {
                        item {
                            Text(
                                text = "> En attente du lancement de GEMEAU...",
                                color = Color(0xFF888888),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        items(logs) { entry ->
                            val textColor = when {
                                entry.isError -> Color(0xFFFF5252)
                                entry.isSuccess -> Color(0xFF69F0AE)
                                entry.text.startsWith("[INFO]") || entry.text.startsWith("[CRYPTO]") -> Color(0xFF40C4FF)
                                entry.text.startsWith("[PRÉSERVATION]") -> Color(0xFF81C784)
                                entry.text.startsWith("[AVB") || entry.text.startsWith("[SELINUX") -> Color(0xFFFFD54F)
                                else -> Color(0xFFEEEEEE)
                            }
                            Text(
                                text = entry.text,
                                color = textColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            if (reportPath != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = { onViewReport(reportPath) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("view_security_report_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Rapport d'Audit & Sécurité", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun GemeauHubView(
    state: GemeauUiState,
    onOpenFeature: (GemeauSubScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature 1: Keymaker
        item {
            GemeauMinimalCard(
                icon = Icons.Default.Key,
                title = "1. Keymaker",
                testTag = "open_feature_keymaker",
                onClick = { onOpenFeature(GemeauSubScreen.KEYMAKER) }
            )
        }

        // Feature 2: SIGNAPK
        item {
            GemeauMinimalCard(
                icon = Icons.Default.Layers,
                title = "2. SIGNAPK",
                testTag = "open_feature_signapk",
                onClick = { onOpenFeature(GemeauSubScreen.SIGNAPK) }
            )
        }

        // Feature 3: ROMsign
        item {
            GemeauMinimalCard(
                icon = Icons.Default.Build,
                title = "3. ROMsign",
                testTag = "open_feature_romsign",
                onClick = { onOpenFeature(GemeauSubScreen.ROMSIGN) }
            )
        }

        // Feature 4: VÉRIFICATION
        item {
            GemeauMinimalCard(
                icon = Icons.Default.VerifiedUser,
                title = "4. VÉRIFICATION",
                testTag = "open_feature_verification",
                onClick = { onOpenFeature(GemeauSubScreen.VERIFICATION_HUB) }
            )
        }

        // Feature 5: R-SIGNER
        item {
            GemeauMinimalCard(
                icon = Icons.Default.Security,
                title = "5. R-SIGNER",
                testTag = "open_feature_rsigner",
                onClick = { onOpenFeature(GemeauSubScreen.RSIGNER) }
            )
        }
    }
}

@Composable
fun GemeauMinimalCard(
    icon: ImageVector,
    title: String,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(text = "Ouvrir", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun GemeauVerificationCarouselView(
    onSelectVerifyApp: () -> Unit,
    onSelectVerifyRom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val carouselPagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Carousel Horizontal Pager
        HorizontalPager(
            state = carouselPagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) { page ->
            if (page == 0) {
                CarouselVerificationCard(
                    icon = Icons.Default.VerifiedUser,
                    title = "Verify App",
                    buttonLabel = "Ouvrir Verify App",
                    testTag = "carousel_open_verify_app",
                    onClick = onSelectVerifyApp
                )
            } else {
                CarouselVerificationCard(
                    icon = Icons.Default.Security,
                    title = "Verify ROM",
                    buttonLabel = "Ouvrir Verify ROM",
                    testTag = "carousel_open_verify_rom",
                    onClick = onSelectVerifyRom
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Carousel Indicators (Dots)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(2) { index ->
                val isSelected = carouselPagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .height(8.dp)
                        .width(if (isSelected) 24.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable {
                            coroutineScope.launch {
                                carouselPagerState.animateScrollToPage(index)
                            }
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Direct Quick Switch Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onSelectVerifyApp,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify App")
            }
            OutlinedButton(
                onClick = onSelectVerifyRom,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Verify ROM")
            }
        }
    }
}

@Composable
fun CarouselVerificationCard(
    icon: ImageVector,
    title: String,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = buttonLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
fun VerifyAppSection(
    state: GemeauUiState,
    onBrowseApks: () -> Unit,
    onBrowseRomForApks: () -> Unit,
    onExtractApksFromRom: () -> Unit,
    onVerifyApks: () -> Unit,
    onRemoveApk: (File) -> Unit
) {
    Column {
        // Selection Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Vérification des signatures APK",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analyse cryptographique v1, v2, v3, v4, SHA256 et certificats",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Option 1: Direct APK Selection
                Text(
                    text = "1. Sélection directe de fichiers APK",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onBrowseApks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_browse_apks_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Parcourir et ajouter des APKs")
                }

                if (state.verifySelectedApks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "${state.verifySelectedApks.size} APK(s) sélectionné(s) :",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.verifySelectedApks.forEach { file ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    IconButton(
                                        onClick = { onRemoveApk(file) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Close, contentDescription = "Supprimer", modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                // Option 2: Extraction from ROM
                Text(
                    text = "2. Ou extraction depuis ROM / GSI / image",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.verifySelectedRomForApkExtraction?.name ?: "Aucun fichier ROM sélectionné",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (state.verifySelectedRomForApkExtraction != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onBrowseRomForApks,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("verify_browse_rom_for_apks_btn")
                    ) {
                        Text("Choisir")
                    }
                }

                if (state.verifySelectedRomForApkExtraction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onExtractApksFromRom,
                        enabled = !state.isExtractingApksFromRom,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_extract_apks_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        if (state.isExtractingApksFromRom) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extraction des APKs en cours...")
                        } else {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Extraire les APKs de la ROM")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Action Button: Verify
                Button(
                    onClick = onVerifyApks,
                    enabled = state.verifySelectedApks.isNotEmpty() && !state.isVerifyingApks,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_execute_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isVerifyingApks) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Vérification cryptographique en cours...")
                    } else {
                        Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.verifySelectedApks.isEmpty()) "Sélectionnez au moins 1 APK" else "VÉRIFIER LES SIGNATURES (${state.verifySelectedApks.size} APKs)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Export Path Notice
        if (state.lastVerificationDir != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Base de données sauvegardée dans : ${state.lastVerificationDir}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Results Section
        if (state.apkVerificationResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "RÉSULTATS DE L'AUDIT (${state.apkVerificationResults.size} APKs)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.apkVerificationResults.forEach { item ->
                    ApkVerificationCard(detail = item)
                }
            }
        }

        // Integrated Terminal Logs
        Spacer(modifier = Modifier.height(16.dp))
        GemeauConsoleCard(
            isExecuting = state.isVerifyingApks,
            progress = state.progress,
            currentStep = state.currentStepText,
            logs = state.logs,
            reportPath = null,
            outputPath = state.lastVerificationDir,
            onViewReport = {}
        )
    }
}

@Composable
fun ApkVerificationCard(detail: ApkVerificationDetail) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = detail.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (detail.isSigned) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                ) {
                    Text(
                        text = if (detail.isSigned) "SIGNÉ & VALIDE" else "NON SIGNÉ",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Taille : ${detail.fileSizeFormatted}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            // Schemes Badges
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SchemeBadge(name = "v1 (JAR)", active = detail.hasV1Scheme)
                SchemeBadge(name = "v2 (Block)", active = detail.hasV2Scheme)
                SchemeBadge(name = "v3 (Rotate)", active = detail.hasV3Scheme)
                SchemeBadge(name = "v4 (Tree)", active = detail.hasV4Scheme)
            }

            if (detail.isSigned) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(10.dp))

                DetailInfoRow(label = "Sujet", value = detail.subjectDn)
                DetailInfoRow(label = "Émetteur", value = detail.issuerDn)
                DetailInfoRow(label = "Algorithme", value = "${detail.algorithm} (${detail.keySize} bits)")
                DetailInfoRow(label = "Validité", value = "Du ${detail.validFrom} au ${detail.validUntil}")

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Empreinte SHA-256 :",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = detail.sha256Fingerprint,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (detail.errors.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        detail.errors.forEach { err ->
                            Text(text = "• $err", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SchemeBadge(name: String, active: Boolean) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            color = if (active) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun DetailInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = "$label : ",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VerifyRomSection(
    state: GemeauUiState,
    onBrowseRomForAudit: () -> Unit,
    onAuditRom: () -> Unit,
    onViewReport: (path: String, title: String) -> Unit
) {
    Column {
        // Selection Card
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Audit de la chaîne de confiance ROM",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "GSI, ZIP custom/stock, images IMG et répertoires système",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Fichier ROM / GSI / image ou répertoire :",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = state.verifySelectedRomForAudit?.name ?: "Aucune ROM sélectionnée",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(10.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (state.verifySelectedRomForAudit != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = onBrowseRomForAudit,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("verify_browse_rom_audit_btn")
                    ) {
                        Text("Parcourir")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onAuditRom,
                    enabled = state.verifySelectedRomForAudit != null && !state.isAuditingRom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_audit_rom_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (state.isAuditingRom) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Audit de la chaîne de confiance en cours...")
                    } else {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AUDITER LA ROM COMPLÈTE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Audit Result Card
        if (state.romAuditResult != null) {
            val audit = state.romAuditResult
            Spacer(modifier = Modifier.height(16.dp))

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Status
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (audit.trustChainValid) Color(0xFF1B5E20) else Color(0xFFE65100),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (audit.trustChainValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (audit.trustChainValid) "CHAÎNE DE CONFIANCE : CONFORME" else "CHAÎNE DE CONFIANCE : MIXTE",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (audit.trustChainValid) "Toutes les signatures système et XML sont alignées." else "Plusieurs clés détectées ou incohérences partielles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Statistiques de la ROM : ${audit.romFileName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetricCard(modifier = Modifier.weight(1f), title = "Total APKs", value = audit.totalApksAnalyzed.toString())
                        MetricCard(modifier = Modifier.weight(1f), title = "Signés valides", value = audit.signedApksCount.toString())
                        MetricCard(modifier = Modifier.weight(1f), title = "GAPPS", value = audit.gappsCount.toString())
                        MetricCard(modifier = Modifier.weight(1f), title = "Clés uniques", value = audit.distinctSignersCount.toString())
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    // SELinux & XML Status
                    Text(
                        text = "Contrôles de sécurité & Intégrité système :",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    DetailInfoRow(
                        label = "mac_permissions.xml (SELinux)",
                        value = if (audit.macPermissionsFound) "Présent (${if (audit.macPermissionsAligned) "Aligné avec Platform Key" else "À synchroniser"})" else "Non détecté"
                    )
                    DetailInfoRow(
                        label = "otacerts.zip (Recovery OTA)",
                        value = if (audit.otacertsFound) "Présent et valide" else "Non présent"
                    )
                    DetailInfoRow(
                        label = "Base de données exportée",
                        value = audit.databaseJsonPath
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onViewReport(audit.summaryReportPath, "Rapport d'audit : ${audit.romFileName}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_view_full_report_btn"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Consulter le Rapport d'Audit & Sécurité", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Integrated Terminal Logs
        Spacer(modifier = Modifier.height(16.dp))
        GemeauConsoleCard(
            isExecuting = state.isAuditingRom,
            progress = state.progress,
            currentStep = state.currentStepText,
            logs = state.logs,
            reportPath = state.romAuditResult?.summaryReportPath,
            outputPath = state.romAuditResult?.databaseJsonPath,
            onViewReport = { path ->
                onViewReport(path, "Rapport d'audit ROM")
            }
        )
    }
}

@Composable
fun MetricCard(modifier: Modifier = Modifier, title: String, value: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 5: R-SIGNER (ADVANCED SYSTEM & ROM RESIGNER WITH 1:1 REPLICATION)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RSignerPage(
    state: GemeauUiState,
    onBrowseTarget: () -> Unit,
    onAnalyzeTarget: () -> Unit,
    onStartSign: (withVbmeta: Boolean) -> Unit,
    onViewReport: (path: String, title: String) -> Unit
) {
    val selectedTarget = state.rsignerSelectedTarget

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Banner card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "R-SIGNER — Resignature Intelligente",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Resignature AOSP V1+V2+V3, préservation SafetyNet/GApps et reconstruction à l'identique dans sdcard/HEPHAISTOS/SIGNER/RSIGNER/.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Target File Selection Card
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fichier Cible (ROM, GSI, .img, .zip, payload.bin)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .clickable { onBrowseTarget() }
                            .padding(14.dp),
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                if (selectedTarget != null) {
                                    Text(
                                        text = selectedTarget.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${com.example.data.UkaEngine.formatFileSize(selectedTarget.length())} • ${selectedTarget.absolutePath}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                } else {
                                    Text(
                                        text = "Touchez pour sélectionner la cible via le gestionnaire flottant...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = onBrowseTarget,
                                modifier = Modifier.testTag("rsigner_browse_target_btn"),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Parcourir")
                            }
                        }
                    }

                    if (selectedTarget != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onAnalyzeTarget,
                            enabled = !state.rsignerIsAnalyzing && !state.isExecuting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rsigner_analyze_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (state.rsignerIsAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyse en cours...")
                            } else {
                                Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1. Analyser la cible (Détection 1:1 des groupes)")
                            }
                        }
                    }
                }
            }
        }

        // Detected Signature Groups Card
        if (state.rsignerDetectedGroups.isNotEmpty()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Groupes de Signatures Détectés (${state.rsignerDetectedGroups.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = "1:1 Alignement",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        state.rsignerDetectedGroups.forEach { grp ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (grp.groupName.contains("Google")) MaterialTheme.colorScheme.tertiaryContainer
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (grp.groupName.contains("Google")) Icons.Default.VerifiedUser else Icons.Default.VpnKey,
                                            contentDescription = null,
                                            tint = if (grp.groupName.contains("Google")) MaterialTheme.colorScheme.onTertiaryContainer
                                            else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = grp.groupName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${grp.apkCount} APK(s) • Schéma: ${grp.schemesDisplay} • SHA: ${grp.sha256.take(16)}...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Buttons: SIGNER & SIGNER + VBMETA
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Actions de Resignature R-SIGNER",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Sortie garantie dans sdcard/HEPHAISTOS/SIGNER/RSIGNER/. Format reflet de la cible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onStartSign(false) },
                            enabled = selectedTarget != null && !state.isExecuting && !state.rsignerIsAnalyzing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rsigner_sign_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGNER", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStartSign(true) },
                            enabled = selectedTarget != null && !state.isExecuting && !state.rsignerIsAnalyzing,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rsigner_sign_vbmeta_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.tertiary
                            )
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIGNER + VBMETA", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Output result card if available
        if (state.rsignerLastOutputPath != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Conteneur Resigné Disponible",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = state.rsignerLastOutputPath,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                state.lastGeneratedReportPath?.let {
                                    onViewReport(it, "Rapport R-SIGNER")
                                }
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consulter le Rapport Complet")
                        }
                    }
                }
            }
        }

        // Console / Terminal logs
        item {
            GemeauConsoleCard(
                isExecuting = state.isExecuting || state.rsignerIsAnalyzing,
                progress = state.progress,
                currentStep = state.currentStepText,
                logs = state.logs,
                reportPath = state.lastGeneratedReportPath,
                outputPath = state.rsignerLastOutputPath,
                onViewReport = { path -> onViewReport(path, "Rapport R-SIGNER") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RSignerKeysetDialog(
    keysets: List<KeysetInfo>,
    selectedKeyset: KeysetInfo?,
    detectedGroups: List<SignatureGroup>,
    withVbmeta: Boolean,
    onConfirmExisting: (KeysetInfo, preserveGapps: Boolean) -> Unit,
    onConfirmCreateNew: (name: String, cn: String, org: String, country: String, alg: String, years: Int, preserveGapps: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var createNewMode by remember { mutableStateOf(keysets.isEmpty()) }
    var chosenKeyset by remember { mutableStateOf(selectedKeyset ?: keysets.firstOrNull()) }
    var preserveGapps by remember { mutableStateOf(true) }

    var newName by remember { mutableStateOf("Keyset_${System.currentTimeMillis() % 10000}") }
    var cn by remember { mutableStateOf("HephaistOS") }
    var org by remember { mutableStateOf("Kitchen Community") }
    var country by remember { mutableStateOf("FR") }
    var algorithm by remember { mutableStateOf("RSA 2048") }
    var years by remember { mutableStateOf("25") }

    var expandedDropdown by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Trousseau R-SIGNER (Alignement 1:1)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tab-like Toggle between New and Existing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .padding(4.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { createNewMode = true },
                        color = if (createNewMode) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = "Créer Keyset 1:1",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (createNewMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { if (keysets.isNotEmpty()) createNewMode = false },
                        color = if (!createNewMode) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            text = "Keyset Existant (${keysets.size})",
                            modifier = Modifier.padding(vertical = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!createNewMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (createNewMode) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Les rôles seront générés à l'identique pour les ${detectedGroups.size.coerceAtLeast(1)} groupe(s) détectés (Platform, Media, Shared, Release, NetworkStack, AVB).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp)
                        )
                    }

                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nom du Keyset") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = cn,
                        onValueChange = { cn = it },
                        label = { Text("Common Name (CN)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = org,
                        onValueChange = { org = it },
                        label = { Text("Organisation (O)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = country,
                            onValueChange = { country = it.take(2).uppercase() },
                            label = { Text("Pays") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = years,
                            onValueChange = { years = it.filter { c -> c.isDigit() } },
                            label = { Text("Validité (ans)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                } else {
                    Text(
                        text = "Choisissez parmi les trousseaux déjà créés dans Keymaker :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = chosenKeyset?.name ?: "Aucun",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Trousseau") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            keysets.forEach { k ->
                                DropdownMenuItem(
                                    text = { Text(k.name) },
                                    onClick = {
                                        chosenKeyset = k
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // GApps Preservation Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { preserveGapps = !preserveGapps }
                ) {
                    Checkbox(
                        checked = preserveGapps,
                        onCheckedChange = { preserveGapps = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Préserver les signatures officielles Google / GApps (SafetyNet & Play Integrity)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (withVbmeta) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Option VBMETA activée : Une image vbmeta.img AVB 2.0 sera signée avec avb.pk8.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (createNewMode) {
                        onConfirmCreateNew(newName, cn, org, country, algorithm, years.toIntOrNull() ?: 25, preserveGapps)
                    } else {
                        chosenKeyset?.let { onConfirmExisting(it, preserveGapps) }
                    }
                },
                modifier = Modifier.testTag("rsigner_dialog_confirm_btn")
            ) {
                Text("Lancer la signature")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}

