package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.KeycoreTab
import com.example.model.KeycoreUiState
import com.example.model.KeysetInfo
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeycoreScreen(
    keycoreState: KeycoreUiState,
    availableApks: List<File>,
    onSelectTab: (KeycoreTab) -> Unit,
    onSelectApkPath: (String) -> Unit,
    onUriSelected: (Uri) -> Unit,
    onAnalyzeApk: (String) -> Unit,
    onGenerateKeymaker: (name: String, cn: String, org: String, country: String, alg: String, years: Int) -> Unit,
    onGenerateVerisinKey: (name: String, cn: String, org: String, country: String, years: Int) -> Unit,
    onGenerateVerisinClone: (cloneName: String) -> Unit,
    onResignWithClone: () -> Unit,
    onRefreshApks: () -> Unit,
    onOpenInVerisin: (String) -> Unit,
    onBackToSceau: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToSceau()
    }

    val clipboard = LocalClipboardManager.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUriSelected(uri)
        }
    }

    LaunchedEffect(Unit) {
        onRefreshApks()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Header
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToSceau,
                    modifier = Modifier.testTag("keycore_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour à Sceau"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "KEYCORE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Forge Cryptographique & Gestionnaire de Clés",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefreshApks,
                    modifier = Modifier.testTag("keycore_refresh_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Actualiser les APKs"
                    )
                }
            }
        }

        // Sub Tabs
        TabRow(
            selectedTabIndex = keycoreState.activeTab.ordinal,
            modifier = Modifier.fillMaxWidth()
        ) {
            KeycoreTab.values().forEach { tab ->
                Tab(
                    selected = keycoreState.activeTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (keycoreState.activeTab == tab) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        // Status or Error Banners
        if (keycoreState.statusMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = keycoreState.statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (keycoreState.errorMessage != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = keycoreState.errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // Body Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (keycoreState.activeTab) {
                KeycoreTab.KEYMAKER -> {
                    item {
                        KeymakerTabContent(
                            isGenerating = keycoreState.isGeneratingKey,
                            lastGeneratedKeyset = keycoreState.lastGeneratedKeyset,
                            onGenerate = onGenerateKeymaker
                        )
                    }
                }

                KeycoreTab.VERISIN_KEY -> {
                    item {
                        VerisinKeyTabContent(
                            selectedPath = keycoreState.selectedApkPath,
                            availableApks = availableApks,
                            isAnalyzing = keycoreState.isAnalyzingApk,
                            isGenerating = keycoreState.isGeneratingKey,
                            targetAnalysis = keycoreState.targetAnalysis,
                            lastGeneratedKeyset = keycoreState.lastGeneratedKeyset,
                            onPathChanged = onSelectApkPath,
                            onBrowseClick = { filePickerLauncher.launch("*/*") },
                            onAnalyzeClick = { onAnalyzeApk(keycoreState.selectedApkPath) },
                            onGenerate = onGenerateVerisinKey
                        )
                    }
                }

                KeycoreTab.VERISIN_CLONE -> {
                    item {
                        VerisinCloneTabContent(
                            selectedPath = keycoreState.selectedApkPath,
                            availableApks = availableApks,
                            isAnalyzing = keycoreState.isAnalyzingApk,
                            isGenerating = keycoreState.isGeneratingKey,
                            isResigning = keycoreState.isResigningApk,
                            targetAnalysis = keycoreState.targetAnalysis,
                            lastGeneratedKeyset = keycoreState.lastGeneratedKeyset,
                            lastResignedApkPath = keycoreState.lastResignedApkPath,
                            onPathChanged = onSelectApkPath,
                            onBrowseClick = { filePickerLauncher.launch("*/*") },
                            onAnalyzeClick = { onAnalyzeApk(keycoreState.selectedApkPath) },
                            onGenerateClone = onGenerateVerisinClone,
                            onResignClick = onResignWithClone,
                            onOpenInVerisin = onOpenInVerisin
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. KEYMAKER TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KeymakerTabContent(
    isGenerating: Boolean,
    lastGeneratedKeyset: KeysetInfo?,
    onGenerate: (name: String, cn: String, org: String, country: String, alg: String, years: Int) -> Unit
) {
    var keysetName by remember { mutableStateOf("aosp_keyset") }
    var commonName by remember { mutableStateOf("Android") }
    var organization by remember { mutableStateOf("HephaistOS") }
    var countryCode by remember { mutableStateOf("US") }
    var algorithm by remember { mutableStateOf("RSA 2048 bits") }
    var validityYears by remember { mutableIntStateOf(25) }

    val algorithms = listOf("RSA 2048 bits", "RSA 4096 bits", "ECDSA P-256")

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Forge de Jeu de Clés Standard",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Génère 6 clés AOSP (releasekey, platform, shared, media, networkstack, avb)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = keysetName,
                    onValueChange = { keysetName = it },
                    label = { Text("Nom du Keyset") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("keymaker_name_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = commonName,
                        onValueChange = { commonName = it },
                        label = { Text("CN (Common Name)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = organization,
                        onValueChange = { organization = it },
                        label = { Text("O (Organisation)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = countryCode,
                        onValueChange = { countryCode = it.take(2).uppercase() },
                        label = { Text("Pays (ex: US, FR)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = validityYears.toString(),
                        onValueChange = { validityYears = it.toIntOrNull() ?: 25 },
                        label = { Text("Validité (années)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Algorithme Cryptographique :",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    algorithms.forEach { alg ->
                        FilterChip(
                            selected = algorithm == alg,
                            onClick = { algorithm = alg },
                            label = { Text(alg) }
                        )
                    }
                }

                Button(
                    onClick = {
                        onGenerate(keysetName, commonName, organization, countryCode, algorithm, validityYears)
                    },
                    enabled = keysetName.isNotBlank() && !isGenerating,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("keymaker_generate_button")
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Forge des 6 clés en cours...")
                    } else {
                        Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("FORGER LE JEU DE CLÉS", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Keyset Result Card
        if (lastGeneratedKeyset != null) {
            KeysetSuccessCard(lastGeneratedKeyset)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. VERISIN KEY TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VerisinKeyTabContent(
    selectedPath: String,
    availableApks: List<File>,
    isAnalyzing: Boolean,
    isGenerating: Boolean,
    targetAnalysis: com.example.model.KeycoreTargetAnalysis?,
    lastGeneratedKeyset: KeysetInfo?,
    onPathChanged: (String) -> Unit,
    onBrowseClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onGenerate: (name: String, cn: String, org: String, country: String, years: Int) -> Unit
) {
    var customName by remember { mutableStateOf("calqued_key") }
    var customCn by remember { mutableStateOf("CustomKey") }
    var customOrg by remember { mutableStateOf("HephaistOS") }
    var customCountry by remember { mutableStateOf("FR") }
    var customYears by remember { mutableIntStateOf(25) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Step 1: Target APK Picker
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "1. Sélectionner l'APK Modèle",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verisin va extraire l'architecture, typologie et schémas requis",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedPath,
                    onValueChange = onPathChanged,
                    label = { Text("Chemin de l'APK cible") },
                    placeholder = { Text("ex: SystemUI.apk") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onBrowseClick) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Parcourir")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick APK chips
                if (availableApks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableApks, key = { it.absolutePath }) { apk ->
                            AssistChip(
                                onClick = { onPathChanged(apk.absolutePath) },
                                label = { Text(apk.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Button(
                    onClick = onAnalyzeClick,
                    enabled = selectedPath.isNotBlank() && !isAnalyzing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyse Verisin en cours...")
                    } else {
                        Text("ANALYSER LA STRUCTURE CIBLE")
                    }
                }
            }
        }

        // Step 2: Target Analysis Badge & Custom Identity Form
        if (targetAnalysis != null) {
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Structure Verisin Détectée",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Cible : ${targetAnalysis.apkName}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Typologie Clé : ${targetAnalysis.keyType} ${targetAnalysis.keySizeBits} bits", fontSize = 12.sp)
                            Text("Algorithme : ${targetAnalysis.sigAlgName}", fontSize = 12.sp)
                            Text("Schémas requis : ${targetAnalysis.schemesDisplay}", fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider()

                    Text(
                        text = "2. Définir votre Identité Personnalisée (DN)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Nom du Keyset généré") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customCn,
                            onValueChange = { customCn = it },
                            label = { Text("CN (Common Name)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customOrg,
                            onValueChange = { customOrg = it },
                            label = { Text("O (Organisation)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customCountry,
                            onValueChange = { customCountry = it.take(2).uppercase() },
                            label = { Text("Pays (ex: FR)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = customYears.toString(),
                            onValueChange = { customYears = it.toIntOrNull() ?: 25 },
                            label = { Text("Validité (ans)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            onGenerate(customName, customCn, customOrg, customCountry, customYears)
                        },
                        enabled = customName.isNotBlank() && !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verisin_key_generate_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Forge de la clé calquée...")
                        } else {
                            Icon(imageVector = Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FORGER LA CLÉ VERISIN KEY", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Keyset Result Card
        if (lastGeneratedKeyset != null) {
            KeysetSuccessCard(lastGeneratedKeyset)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. VERISIN CLONE TAB
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun VerisinCloneTabContent(
    selectedPath: String,
    availableApks: List<File>,
    isAnalyzing: Boolean,
    isGenerating: Boolean,
    isResigning: Boolean,
    targetAnalysis: com.example.model.KeycoreTargetAnalysis?,
    lastGeneratedKeyset: KeysetInfo?,
    lastResignedApkPath: String?,
    onPathChanged: (String) -> Unit,
    onBrowseClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onGenerateClone: (cloneName: String) -> Unit,
    onResignClick: () -> Unit,
    onOpenInVerisin: (String) -> Unit
) {
    var cloneName by remember { mutableStateOf("clone_target") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Step 1: Target APK Picker
        ElevatedCard(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Signature Parallèle & Clone",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Reproduit les DNs, le type, la typologie et la logique de l'APK cible",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = selectedPath,
                    onValueChange = onPathChanged,
                    label = { Text("APK cible à cloner") },
                    placeholder = { Text("ex: SystemUI.apk") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = onBrowseClick) {
                            Icon(imageVector = Icons.Default.FolderOpen, contentDescription = "Parcourir")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                if (availableApks.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(availableApks, key = { it.absolutePath }) { apk ->
                            AssistChip(
                                onClick = { onPathChanged(apk.absolutePath) },
                                label = { Text(apk.name, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }

                Button(
                    onClick = onAnalyzeClick,
                    enabled = selectedPath.isNotBlank() && !isAnalyzing,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isAnalyzing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyse Verisin approfondie...")
                    } else {
                        Text("EXTRAIRE LES MÉTADONNÉES DE SIGNATURE")
                    }
                }
            }
        }

        // Step 2: Target Clone Blueprint & Generation
        if (targetAnalysis != null) {
            OutlinedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Empreinte Reçue pour Clonage",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Sujet original (DN) :", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(
                                text = targetAnalysis.subjectDn,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text("Algorithme : ${targetAnalysis.sigAlgName} (${targetAnalysis.keyType} ${targetAnalysis.keySizeBits} bits)", fontSize = 12.sp)
                            Text("Schémas cible : ${targetAnalysis.schemesDisplay}", fontSize = 12.sp)
                        }
                    }

                    OutlinedTextField(
                        value = cloneName,
                        onValueChange = { cloneName = it },
                        label = { Text("Identifiant du Sceau Clone") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = { onGenerateClone(cloneName) },
                        enabled = cloneName.isNotBlank() && !isGenerating,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("verisin_clone_generate_button")
                    ) {
                        if (isGenerating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Forge du Sceau Clone en cours...")
                        } else {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("FORGER LE SCEAU CLONE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Step 3: Resign Direct Action
        if (lastGeneratedKeyset != null) {
            KeysetSuccessCard(lastGeneratedKeyset)

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Resignature Parallèle Immédiate",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "Applique directement la nouvelle signature clonée sur l'APK cible pour préserver la cohérence système sans bootloop.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Button(
                        onClick = onResignClick,
                        enabled = !isResigning,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verisin_clone_resign_button")
                    ) {
                        if (isResigning) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Resignature de l'APK en cours...")
                        } else {
                            Icon(imageVector = Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("RESIGNER DIRECTEMENT L'APK CIBLE", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (lastResignedApkPath != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                                    Text("APK résigné avec succès !", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                                }
                                Text(
                                    text = lastResignedApkPath,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Button(
                                    onClick = { onOpenInVerisin(lastResignedApkPath) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Vérifier le résultat dans VERISIN")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Success Card for Keyset
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun KeysetSuccessCard(keyset: KeysetInfo) {
    val clipboard = LocalClipboardManager.current
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Text("Trousseau Prêt : ${keyset.name}", fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = { clipboard.setText(AnnotatedString(keyset.dirPath)) }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copier chemin", fontSize = 12.sp)
                }
            }
            Text(
                text = "Emplacement : ${keyset.dirPath}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${keyset.keyCount / 2} clés AOSP/AVB générées • Algorithme : ${keyset.algorithm}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
