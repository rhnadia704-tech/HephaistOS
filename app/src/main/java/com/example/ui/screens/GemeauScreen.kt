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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApkSignItem
import com.example.model.GemeauTab
import com.example.model.GemeauUiState
import com.example.model.KeysetInfo
import com.example.model.TerminalEntry
import com.example.ui.components.InternalFilePickerDialog
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
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToHome()
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })

    var showApkFilePicker by remember { mutableStateOf(false) }
    var showRomFilePicker by remember { mutableStateOf(false) }

    var showChooseKeyDialogForApk by remember { mutableStateOf(false) }
    var showChooseKeyDialogForRom by remember { mutableStateOf(false) }
    var pendingWithVbmeta by remember { mutableStateOf(false) }

    var showReportDialog by remember { mutableStateOf(false) }
    var reportDialogContent by remember { mutableStateOf("") }
    var reportDialogTitle by remember { mutableStateOf("") }

    val tabTitles = listOf("1. Keymaker", "2. SIGNAPK", "3. ROMsign")

    // Synchronize tab index with pager state
    LaunchedEffect(pagerState.currentPage) {
        val tab = when (pagerState.currentPage) {
            0 -> GemeauTab.KEYMAKER
            1 -> GemeauTab.SIGNAPK
            else -> GemeauTab.ROMSIGN
        }
        onTabSelected(tab)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar — Matching Unpack/Repack and Extractor design language
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
                    onClick = onBackToHome,
                    modifier = Modifier.testTag("gemeau_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour à l'accueil"
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
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = "GEMEAU",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GÉMEAU",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
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
                        text = "Keysets AOSP, Signature Multi-APKs & Resignature de ROMs",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // SecondaryTabRow (M3) — 3 Pages
        SecondaryTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.Key
                                    1 -> Icons.Default.Layers
                                    else -> Icons.Default.Security
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("gemeau_tab_$index")
                )
            }
        }

        // Horizontal Pager for 3 Pages
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> KeymakerPage(
                    state = gemeauState,
                    onUpdateForm = onUpdateKeymakerForm,
                    onGenerateKeyset = onGenerateKeyset,
                    onRefreshKeysets = onRefreshKeysets,
                    onSelectKeyset = onSelectKeyset
                )
                1 -> SignApkPage(
                    state = gemeauState,
                    onBrowseClick = { showApkFilePicker = true },
                    onStartSignClick = {
                        if (gemeauState.availableKeysets.isEmpty()) {
                            onRefreshKeysets()
                        }
                        showChooseKeyDialogForApk = true
                    }
                )
                2 -> RomSignPage(
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
