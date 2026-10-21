package com.example.ui.screens

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import com.example.model.ExtractionMode
import com.example.model.ExtractorUiState
import com.example.model.TerminalEntry
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractorScreen(
    extractorState: ExtractorUiState,
    onStartDeviceExtraction: () -> Unit,
    onStartUnpackedAnalysis: () -> Unit,
    onRefreshUnpackedProjects: () -> Unit,
    onSelectUnpackedProject: (String) -> Unit,
    onNavigateToUnpackRepack: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onBackToHome()
    }

    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    var showReportDialog by remember { mutableStateOf(false) }
    var reportDialogContent by remember { mutableStateOf("") }
    var reportDialogTitle by remember { mutableStateOf("") }

    val tabTitles = listOf("1. EXTRACTION APPAREIL", "2. ANALYSE GSI / PROJET")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar — Identical design language to UnpackRepackScreen
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
                    modifier = Modifier.testTag("extractor_back_btn")
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
                        imageVector = Icons.Default.Build,
                        contentDescription = "Extractor",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "EXTRACTOR",
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
                                text = "Factual v1.0",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = "Extraction système, HALs, Blobs & Analyse comparative GSI",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Swipeable Tab Row (M3)
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
                                imageVector = if (index == 0) Icons.Default.PhoneAndroid else Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    },
                    modifier = Modifier.testTag("extractor_tab_$index")
                )
            }
        }

        // Horizontal Pager with Swipe support
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> DeviceExtractionPage(
                    state = extractorState,
                    onStartExtract = onStartDeviceExtraction,
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
                1 -> UnpackedAnalysisPage(
                    state = extractorState,
                    onRefreshProjects = onRefreshUnpackedProjects,
                    onSelectProject = onSelectUnpackedProject,
                    onStartAnalysis = onStartUnpackedAnalysis,
                    onNavigateToUnpackRepack = onNavigateToUnpackRepack,
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
// PAGE 1: EXTRACTION APPAREIL
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun DeviceExtractionPage(
    state: ExtractorUiState,
    onStartExtract: () -> Unit,
    onViewReport: (path: String, title: String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Device Spec Overview Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Android ${Build.VERSION.RELEASE} • SDK ${Build.VERSION.SDK_INT} • ${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "Appareil Hôte",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Destination d'extraction & documentation :",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• Fichiers & Blobs : /sdcard/HEPHAISTOS/EXTRACT/device/\n• Base Factuelle & Graph : /sdcard/HEPHAISTOS/EXTRACT/Data/",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Scope & Deep Disassembly Details Card
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
                        text = "Périmètre de l'Extraction Factuelle",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "L'extracteur va désassembler et cataloguer l'intégralité des briques système de votre appareil en vue d'un futur portage GSI / Custom ROM sans bug :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ExtractionFeatureRow(
                        icon = Icons.Default.Memory,
                        title = "Blobs Vendor & Bibliothèques Partagées",
                        desc = "Copie des .so propriétaires (/vendor, /odm) et désassemblage des en-têtes ELF pour mapper toutes les dépendances DT_NEEDED."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.Security,
                        title = "Manifestes Matériels VINTF (HIDL / AIDL)",
                        desc = "Extraction des matrices de compatibilité matérielle et déclarations HAL pour mapper les interfaces caméra, audio, radio, capteurs."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.Folder,
                        title = "Configs & Overlays Framework",
                        desc = "Désassemblage des XML de permissions (/etc/permissions), sysconfig, overlays RRO et configurations d'interface utilisateur."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.Description,
                        title = "Base de Connaissances & Rapport de Portage",
                        desc = "Génération de device_specifications.json, hal_dependency_graph.json, blob_manifest.txt et architecture_report.md."
                    )
                }
            }
        }

        // Action Button EXTRACT
        item {
            Button(
                onClick = onStartExtract,
                enabled = !state.isExecuting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_device_extract_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isExecuting && state.currentMode == ExtractionMode.FULL_DEVICE) {
                        "Extraction du téléphone en cours..."
                    } else {
                        "EXTRACT (Extraire les composants du téléphone)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Live Execution / Progress Console
        if (state.isExecuting || state.progress > 0f || state.logs.isNotEmpty()) {
            item {
                ExtractorConsoleCard(
                    isExecuting = state.isExecuting,
                    progress = state.progress,
                    currentStep = state.currentStepText,
                    filesCount = state.extractedFilesCount,
                    megabytes = state.extractedMb,
                    logs = state.logs,
                    reportPath = state.lastGeneratedReportPath,
                    dataDir = state.lastGeneratedDataDir,
                    onViewReport = { path -> onViewReport(path, "Rapport d'Extraction Matérielle") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PAGE 2: ANALYSE GSI / PROJET UNPACKÉ
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnpackedAnalysisPage(
    state: ExtractorUiState,
    onRefreshProjects: () -> Unit,
    onSelectProject: (String) -> Unit,
    onStartAnalysis: () -> Unit,
    onNavigateToUnpackRepack: () -> Unit,
    onViewReport: (path: String, title: String) -> Unit
) {
    var expandedDropdown by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Project Selector Card
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
                            text = "Sélectionner le GSI / Projet Unpacké",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(
                            onClick = onRefreshProjects,
                            modifier = Modifier.testTag("refresh_unpacked_projects_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rafraîchir les projets"
                            )
                        }
                    }

                    Text(
                        text = "Parcourt le répertoire /sdcard/HEPHAISTOS/UNPACKED/",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.availableUnpackedProjects.isEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Aucun projet déballé trouvé dans UNPACKED",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Décompressez d'abord une image ou un GSI via le module Unpack / Repack pour l'analyser ici.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedButton(
                                    onClick = onNavigateToUnpackRepack,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Ouvrir Unpack / Repack")
                                }
                            }
                        }
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = expandedDropdown,
                            onExpandedChange = { expandedDropdown = !expandedDropdown }
                        ) {
                            OutlinedTextField(
                                value = state.selectedUnpackedProject.ifEmpty { "Choisir un projet..." },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Projet / GSI Déballé") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                                    .testTag("unpacked_project_selector")
                            )

                            ExposedDropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false }
                            ) {
                                state.availableUnpackedProjects.forEach { projectName ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Folder,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                    tint = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(projectName, fontWeight = FontWeight.Medium)
                                            }
                                        },
                                        onClick = {
                                            onSelectProject(projectName)
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Analysis Scope & Comparison Card
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
                        text = "Objectif de l'Analyse Totale GSI / ROM",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Analyse approfondie de la structure du GSI et mise en relation directe avec les spécificités de votre téléphone pour assurer le portage :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    ExtractionFeatureRow(
                        icon = Icons.Default.Analytics,
                        title = "Architecture & Arborescence du GSI",
                        desc = "Cartographie complète des partitions, build.prop, versions Android cibles et bibliothèques embarquées."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.Memory,
                        title = "Interaction HALs & Vendor Compatibility",
                        desc = "Vérification des interfaces HAL attendues par le framework du GSI versus celles fournies par le vendor de votre téléphone."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.CheckCircle,
                        title = "Patches Treble & Phh Détectés",
                        desc = "Détection des scripts d'adaptation Treble (rw-system, phh-on-boot, fix overlays, sepolicy bypass)."
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ExtractionFeatureRow(
                        icon = Icons.Default.Description,
                        title = "Guide de Portage & Fichiers Générés",
                        desc = "Écriture de gsi_structure_report.json, gsi_vs_device_compatibility.json et porting_guide_generated.md dans /sdcard/HEPHAISTOS/EXTRACT/Data/."
                    )
                }
            }
        }

        // Action Button ANALYSE UNPACKED
        item {
            Button(
                onClick = onStartAnalysis,
                enabled = !state.isExecuting && state.selectedUnpackedProject.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("launch_unpacked_analysis_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (state.isExecuting && state.currentMode == ExtractionMode.UNPACKED_ANALYSIS) {
                        "Analyse du GSI en cours..."
                    } else {
                        "ANALYSE UNPACKED (Analyser le GSI sélectionné)"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        // Live Execution / Progress Console
        if (state.isExecuting || state.progress > 0f || state.logs.isNotEmpty()) {
            item {
                ExtractorConsoleCard(
                    isExecuting = state.isExecuting,
                    progress = state.progress,
                    currentStep = state.currentStepText,
                    filesCount = state.extractedFilesCount,
                    megabytes = state.extractedMb,
                    logs = state.logs,
                    reportPath = state.lastGeneratedReportPath,
                    dataDir = state.lastGeneratedDataDir,
                    onViewReport = { path -> onViewReport(path, "Guide de Portage & Rapport GSI") }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun ExtractionFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ExtractorConsoleCard(
    isExecuting: Boolean,
    progress: Float,
    currentStep: String,
    filesCount: Int,
    megabytes: Double,
    logs: List<TerminalEntry>,
    reportPath: String?,
    dataDir: String?,
    onViewReport: (path: String) -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow),
        label = "extractor_progress_anim"
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
            .testTag("extractor_console_card"),
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
                        text = if (isExecuting) "Extraction / Analyse en cours..." else "Opération terminée",
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
                    .clip(RoundedCornerShape(3.dp)),
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = currentStep.ifEmpty { "En attente d'instruction..." },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (filesCount > 0) {
                    Text(
                        text = "$filesCount fichiers • ${"%.1f".format(megabytes)} Mo",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

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
                                text = "> En attente du lancement de l'extracteur...",
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
                                entry.text.startsWith("[INFO]") || entry.text.startsWith("[PROPRIÉTÉS]") || entry.text.startsWith("[VINTF]") -> Color(0xFF40C4FF)
                                entry.text.startsWith("[HAL]") || entry.text.startsWith("[BLOBS]") -> Color(0xFFFFD54F)
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

            // Quick Open Report Button if generated
            if (reportPath != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onViewReport(reportPath) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("view_generated_report_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Consulter le Rapport & Guide Généré", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
