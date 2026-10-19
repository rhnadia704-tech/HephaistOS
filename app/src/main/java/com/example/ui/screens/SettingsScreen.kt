package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.KitchenEngine
import com.example.model.ChangelogVersion
import com.example.model.ColorStyle
import com.example.model.KitchenTool
import com.example.model.RootStatus
import com.example.model.SettingsTab
import com.example.model.ThemeMode
import com.example.ui.components.PdfViewerComponent
import java.io.File

@Composable
fun SettingsScreen(
    currentTab: SettingsTab,
    themeMode: ThemeMode,
    colorStyle: ColorStyle,
    isStorageGranted: Boolean,
    rootStatus: RootStatus,
    isCheckingRoot: Boolean,
    changelog: List<ChangelogVersion>,
    isGeneratingPdf: Boolean,
    generatedPdfFile: File?,
    workspaceStats: Map<String, Int>,
    onTabSelected: (SettingsTab) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorStyleChange: (ColorStyle) -> Unit,
    onStoragePermissionResult: (Boolean) -> Unit,
    onRequestRootCheck: () -> Unit,
    onGeneratePdf: (Context) -> Unit,
    onOpenPdf: (Context) -> Unit,
    onDownloadStarterKit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onStoragePermissionResult(isGranted)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour à l'accueil",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Paramètres & Cerveau",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Environnement HephaistOS • Kitchen & Addon SDK",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = currentTab.ordinal,
            edgePadding = 16.dp,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsTab.values().forEach { tab ->
                val selected = currentTab == tab
                Tab(
                    selected = selected,
                    onClick = { onTabSelected(tab) },
                    text = {
                        Text(
                            text = tab.title,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    icon = {
                        val icon = when (tab) {
                            SettingsTab.THEME -> Icons.Default.Palette
                            SettingsTab.PERMISSIONS -> Icons.Default.Security
                            SettingsTab.KITCHEN_TOOLS -> Icons.Default.Build
                            SettingsTab.ADDON_TEMPLATE -> Icons.Default.PictureAsPdf
                            SettingsTab.SKELETON_INFO -> Icons.Default.Info
                        }
                        Icon(icon, contentDescription = tab.title)
                    },
                    modifier = Modifier.testTag("tab_${tab.name.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Content
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                (slideInHorizontally { width -> width / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { width -> -width / 3 } + fadeOut())
            },
            label = "settings_tab_content",
            modifier = Modifier.fillMaxSize()
        ) { targetTab ->
            when (targetTab) {
                SettingsTab.THEME -> ThemeSettingsTab(
                    currentThemeMode = themeMode,
                    currentColorStyle = colorStyle,
                    onThemeModeChange = onThemeModeChange,
                    onColorStyleChange = onColorStyleChange
                )
                SettingsTab.PERMISSIONS -> PermissionsTab(
                    isStorageGranted = isStorageGranted,
                    rootStatus = rootStatus,
                    isCheckingRoot = isCheckingRoot,
                    workspaceStats = workspaceStats,
                    onRequestStorage = {
                        val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Manifest.permission.READ_MEDIA_IMAGES
                        } else {
                            Manifest.permission.READ_EXTERNAL_STORAGE
                        }
                        storageLauncher.launch(perm)
                    },
                    onRequestRootCheck = onRequestRootCheck
                )
                SettingsTab.KITCHEN_TOOLS -> KitchenToolsTab()
                SettingsTab.ADDON_TEMPLATE -> AddonTemplateTab(
                    pdfFile = generatedPdfFile,
                    isGenerating = isGeneratingPdf,
                    onRegeneratePdf = { onGeneratePdf(context) },
                    onOpenPdf = { onOpenPdf(context) },
                    onDownloadStarterKit = onDownloadStarterKit
                )
                SettingsTab.SKELETON_INFO -> SkeletonInfoTab(
                    changelog = changelog
                )
            }
        }
    }
}

// ----------------------------------------------------
// TAB 1: THÈME
// ----------------------------------------------------
@Composable
private fun ThemeSettingsTab(
    currentThemeMode: ThemeMode,
    currentColorStyle: ColorStyle,
    onThemeModeChange: (ThemeMode) -> Unit,
    onColorStyleChange: (ColorStyle) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Mode d'affichage",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(ThemeMode.values()) { mode ->
            val isSelected = currentThemeMode == mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onThemeModeChange(mode) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                border = if (isSelected) {
                    borderWidth(MaterialTheme.colorScheme.primary)
                } else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (mode) {
                                ThemeMode.LIGHT -> Icons.Default.LightMode
                                ThemeMode.DARK -> Icons.Default.DarkMode
                                ThemeMode.SYSTEM -> Icons.Default.Palette
                            },
                            contentDescription = null,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = mode.label,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Palette Chromatique Material 3",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        items(ColorStyle.values()) { style ->
            val isSelected = currentColorStyle == style
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onColorStyleChange(style) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                ),
                border = if (isSelected) {
                    borderWidth(MaterialTheme.colorScheme.primary)
                } else null,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(style.primaryHex))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = style.title,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = style.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 2: AUTORISATIONS & ROOT
// ----------------------------------------------------
@Composable
private fun PermissionsTab(
    isStorageGranted: Boolean,
    rootStatus: RootStatus,
    isCheckingRoot: Boolean,
    workspaceStats: Map<String, Int>,
    onRequestStorage: () -> Unit,
    onRequestRootCheck: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Workspace Directory Info
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Espace de travail : /sdcard/HEPHAISTOS/",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Toutes les opérations d'extraction (payload.bin, img), les scripts, les webapps et les templates sont centralisés dans ce répertoire unifié.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Folder Stats
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(10.dp)
                    ) {
                        val entries = workspaceStats.entries.toList()
                        for (entry in entries) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• /${entry.key}/",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${entry.value} fichier(s)",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Storage Access
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (isStorageGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Accès au Stockage SD",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isStorageGranted) "Accès autorisé" else "Autorisation requise",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isStorageGranted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        if (!isStorageGranted) {
                            Button(onClick = onRequestStorage) {
                                Text("Autoriser")
                            }
                        }
                    }
                }
            }
        }

        // Root Access & SuBridge
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (rootStatus.isRooted) Color(0xFF4CAF50) else Color(0xFFFFA000)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Délégation Root & SuBridge",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = rootStatus.statusText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (rootStatus.isRooted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Button(
                            onClick = onRequestRootCheck,
                            enabled = !isCheckingRoot
                        ) {
                            if (isCheckingRoot) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Tester su")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "HephaistOS est capable de déléguer les privilèges root (Magisk/KernelSU) aux scripts et aux applications addons enregistrés. Tout accès est tracé en direct dans le terminal.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    for (detail in rootStatus.details) {
                        Text(
                            text = "• $detail",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 3: BOÎTE À OUTILS KITCHEN
// ----------------------------------------------------
@Composable
private fun KitchenToolsTab() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Outils de Base du Cerveau Kitchen",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Ces outils fondamentaux sont intégrés dans HephaistOS pour manipuler directement les images et firmwares déjà compilés. Ils sont invocables par les futurs modules, les webapps et la console.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        items(KitchenEngine.BUILTIN_TOOLS) { tool ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tool.name,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tool.category,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = tool.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1E2024))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "$ ${tool.commandSyntax}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color(0xFF64B5F6)
                        )
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// TAB 4: TEMPLATES SATELLITES & GUIDE ENRICHI (PDF + STARTER KIT)
// ----------------------------------------------------
@Composable
private fun AddonTemplateTab(
    pdfFile: File?,
    isGenerating: Boolean,
    onRegeneratePdf: () -> Unit,
    onOpenPdf: () -> Unit,
    onDownloadStarterKit: () -> Unit
) {
    var selectedTemplateCategory by remember { mutableIntStateOf(0) } // 0: APK, 1: Script, 2: WebApp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Quick Download & Actions Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Guide Officiel des Satellites (PDF)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Spécification enrichie • 4 pages d'architecture",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onOpenPdf,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Télécharger / Ouvrir")
                        }

                        OutlinedButton(
                            onClick = onDownloadStarterKit,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Starter Kit (ZIP)")
                        }
                    }
                }
            }
        }

        // 2. Interactive Template Category Selector (APK, Script, WebApp)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Modèles de Satellites pour HephaistOS",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Choisissez une catégorie pour consulter sa structure obligatoire et ses templates de code :",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val categories = listOf(
                            Triple("APK Satellite", Icons.Default.Android, 0),
                            Triple("Script Satellite", Icons.Default.Terminal, 1),
                            Triple("WebApp Satellite", Icons.Default.Language, 2)
                        )

                        for ((label, icon, index) in categories) {
                            val isSelected = selectedTemplateCategory == index
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedTemplateCategory = index }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(10.dp)
                                    ),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = label.split(" ")[0],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTemplateCategory) {
                        0 -> ApkSatelliteTemplateView()
                        1 -> ScriptSatelliteTemplateView()
                        2 -> WebAppSatelliteTemplateView()
                    }
                }
            }
        }

        // 3. Integrated Native PDF Viewer Component
        item {
            PdfViewerComponent(
                pdfFile = pdfFile,
                isGenerating = isGenerating,
                onRegeneratePdf = onRegeneratePdf,
                onOpenExternal = onOpenPdf,
                onDownloadStarterKit = onDownloadStarterKit
            )
        }
    }
}

@Composable
private fun ApkSatelliteTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Directives Banner on HephaistOS DNA
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ADN REQUIS POUR ÊTRE DÉTECTÉ PAR HEPHAISTOS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HephaistOS applique un filtrage strict par signature. Les applications ordinaires du téléphone (Gmail, Téléphone, banques...) sont systématiquement exclues de l'accueil. Pour être reconnu comme satellite, votre APK doit obligatoirement déclarer l'action d'intention ci-dessous.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 16.sp
                )
            }
        }

        Text(
            text = "1. Marqueur AndroidManifest.xml obligatoire (ADN) :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2024))
                .padding(10.dp)
        ) {
            Text(
                text = """
                <activity android:name=".MainActivity" android:exported="true">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <!-- ADN UNIQUE HEPHAISTOS OBLIGATOIRE -->
                        <action android:name="org.hephaistos.action.ADDON" />
                        <category android:name="org.hephaistos.category.ADDON" />
                        <category android:name="android.intent.category.DEFAULT" />
                    </intent-filter>
                    
                    <!-- Liste des capacités déclarées séparées par des virgules -->
                    <meta-data
                        android:name="org.hephaistos.addon.CAPABILITIES"
                        android:value="kitchen.unpack,kitchen.repack,payload.dumper,erofs.tools" />
                </activity>
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = Color(0xFFA5D6A7)
            )
        }

        Text(
            text = "2. Réception des requêtes IPC et exécution (Kotlin) :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2024))
                .padding(10.dp)
        ) {
            Text(
                text = """
                class HephaistosSatelliteReceiver : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        if (intent.action == "org.hephaistos.action.INVOKE_TOOL") {
                            val targetPath = intent.getStringExtra("TARGET_DIR") ?: return
                            val command = intent.getStringExtra("COMMAND") ?: "inspect"
                            
                            // 1. Exécution de la tâche modulaire
                            val success = processSatelliteTask(targetPath, command)
                            
                            // 2. Notification de résultat à l'hôte HephaistOS
                            val response = Intent("org.hephaistos.action.TASK_RESULT").apply {
                                putExtra("STATUS", if (success) "SUCCESS" else "ERROR")
                                putExtra("LOGS", "Tâche [${'$'}command] terminée.")
                            }
                            context.sendBroadcast(response)
                        }
                    }
                }
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = Color(0xFF90CAF9)
            )
        }
    }
}

@Composable
private fun ScriptSatelliteTemplateView() {
    Column {
        Text(
            text = "1. En-tête normalisé Shell (.sh) dans /sdcard/HEPHAISTOS/Scripts/ :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2024))
                .padding(10.dp)
        ) {
            Text(
                text = """
                #!/system/bin/sh
                # HEPHAISTOS_SCRIPT_V1
                # NAME: Custom EROFS Builder
                # DESCRIPTION: Repack une partition avec compression EROFS
                # REQUIRE_ROOT: true
                # CAPABILITIES: erofs.compress,partition.repack

                TARGET_DIR="${'$'}1"   # Dossier source dans UNPACKED/
                OUTPUT_IMG="${'$'}2"   # Destination dans PACKED/
                CONFIG_DIR="${'$'}3"   # Métadonnées dans Data/unpack/

                echo "[SCRIPT] Repack de ${'$'}TARGET_DIR vers ${'$'}OUTPUT_IMG..."
                # Exécution des commandes kitchen...
                exit 0
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = Color(0xFFFFCC80)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "2. Conventions des arguments transmis par l'hôte :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "• ${'$'}1 = Dossier cible dans /sdcard/HEPHAISTOS/UNPACKED/\n• ${'$'}2 = Destination dans /sdcard/HEPHAISTOS/PACKED/\n• ${'$'}3 = Métadonnées préservées dans /sdcard/HEPHAISTOS/Data/unpack/\n• Code 0 = Succès (logs affichés dans le Terminal en direct).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WebAppSatelliteTemplateView() {
    Column {
        Text(
            text = "1. Arborescence dans /sdcard/HEPHAISTOS/WebApps/<NomModule>/ :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2024))
                .padding(10.dp)
        ) {
            Text(
                text = """
                MonModuleWeb/
                ├── index.html        (Point d'entrée principal)
                ├── app.js            (Logique applicative et Bridge)
                ├── style.css         (Styles personnalisés)
                └── manifest.json     ({"name": "Mon Module", "version": "1.0"})
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = Color(0xFFCE93D8)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "2. Appel du Bridge JavaScript 'window.HephaistosBridge' :",
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2024))
                .padding(10.dp)
        ) {
            Text(
                text = """
                // Déclencher une commande hôte et écrire dans le terminal :
                function declencherRepack() {
                    if (window.HephaistosBridge) {
                        window.HephaistosBridge.log("[WEBAPP] Lancement du repack demandé");
                        window.HephaistosBridge.executeCommand("kitchen repack system");
                    }
                }
                """.trimIndent(),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.5.sp,
                color = Color(0xFF80DEEA)
            )
        }
    }
}

// ----------------------------------------------------
// TAB 5: SQUELETTE & INFOS DU CERVEAU HEPHAISTOS
// ----------------------------------------------------
@Composable
private fun SkeletonInfoTab(
    changelog: List<ChangelogVersion>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Le Squelette d'HephaistOS",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "HephaistOS est une application hôte, un lanceur et le nerf central d'un atelier complet d'ingénierie inverse et de création de Custom ROMs (GSI, IMG, stock et custom).",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = """
                        • Rivaliser avec AOSP : Plutôt que de nécessiter un serveur de compilation lourd pour compiler tout AOSP à partir des sources, HephaistOS décompile, patch et recompile intelligemment des OS déjà compilés directement sur l'appareil.
                        
                        • Cerveau & Addons : HephaistOS est la colonne vertébrale. Les autres applications sont des addons spécialisés. Chaque addon installé apporte ses propres outils, qui sont instantanément partagés et utilisables par tous dans l'application hôte.
                        
                        • Intelligence Évolutive : Plus vous installez d'addons ou importez de scripts/webapps, plus le cerveau HephaistOS devient intelligent, précis et puissant.
                        
                        • Espace de travail : Centralisé dans /sdcard/HEPHAISTOS/ pour garantir un accès universel sans barrière de sandbox fermée.
                        """.trimIndent(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        item {
            Text(
                text = "Historique des Versions & Évolution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items(changelog) { versionItem ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version ${versionItem.version}",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = versionItem.releaseDate,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    for (ch in versionItem.changes) {
                        Text(
                            text = "• $ch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun borderWidth(color: Color) = androidx.compose.foundation.BorderStroke(1.5.dp, color)
