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
import androidx.compose.material.icons.filled.ContentCopy
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
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
    val context = LocalContext.current
    var selectedTemplateCategory by remember { mutableIntStateOf(0) }

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
                            Text("Guide PDF")
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

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val file = com.example.data.PdfExporterService.exportStandaloneReadme(context)
                            Toast.makeText(context, "README officiel exporté dans ${file.name} !", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exporter le README.md de Référence")
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val categories = listOf(
                            Triple("1. ADN Manifest", Icons.Default.Security, 0),
                            Triple("2. Thème Compose", Icons.Default.Palette, 1),
                            Triple("3. IPC & AIDL", Icons.Default.Code, 2),
                            Triple("4. Prompt IA", Icons.Default.Android, 3),
                            Triple("5. Scripts & Web", Icons.Default.Terminal, 4)
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
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = label.split(" ")[1],
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 10.5.sp,
                                        maxLines = 1,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTemplateCategory) {
                        0 -> HsasManifestTemplateView()
                        1 -> HsasDesignSystemTemplateView()
                        2 -> HsasIpcAidlTemplateView()
                        3 -> HsasAiStudioPromptTemplateView()
                        4 -> HsasScriptsAndWebTemplateView()
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
private fun CodeSnippetCard(
    title: String,
    code: String,
    languageColor: Color = Color(0xFFA5D6A7),
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        clipboard.setText(AnnotatedString(code))
                        Toast.makeText(context, "Code copié dans le presse-papiers !", Toast.LENGTH_SHORT).show()
                    },
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copier",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Copier",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF14161A))
                .border(1.dp, Color(0xFF282C34), RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = languageColor,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun HsasManifestTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "ADN CANONIQUE HSAS v1.0 POUR DÉTECTION IMMÉDIATE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dès que l'APK est installé sur l'appareil, HephaistOS l'intercepte via son écouteur de paquets et son analyseur d'intention. Votre satellite s'affichera immédiatement sur l'écran d'accueil sans redémarrage de l'hôte.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 16.sp
                )
            }
        }

        val manifestSnippet = """
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="com.example.satellite">

                <!-- Requête obligatoire pour communiquer avec le service hôte -->
                <queries>
                    <intent>
                        <action android:name="org.hephaistos.service.HOST_IPC" />
                    </intent>
                </queries>

                <application
                    android:label="Satellite HephaistOS"
                    android:theme="@style/Theme.Material3.DayNight.NoActionBar">

                    <!-- Empreinte ADN Canonique HSAS v1.0 -->
                    <meta-data
                        android:name="org.hephaistos.satellite.V1"
                        android:value="true" />
                    <meta-data
                        android:name="org.hephaistos.satellite.ROLE"
                        android:value="Éditeur de Configuration & Outils Système" />

                    <!-- Activité Principale du Satellite -->
                    <activity
                        android:name=".MainActivity"
                        android:exported="true">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />

                            <!-- Actions officielles de reconnaissance HephaistOS -->
                            <action android:name="org.hephaistos.action.SATELLITE" />
                            <action android:name="org.hephaistos.action.ADDON" />
                            <category android:name="android.intent.category.DEFAULT" />
                        </intent-filter>

                        <!-- Capacités partagées déclarées dans l'annuaire -->
                        <meta-data
                            android:name="org.hephaistos.addon.CAPABILITIES"
                            android:value="kitchen.unpack,kitchen.repack,prop.editor" />
                    </activity>
                </application>
            </manifest>
        """.trimIndent()

        CodeSnippetCard(
            title = "AndroidManifest.xml Canonique :",
            code = manifestSnippet,
            languageColor = Color(0xFFA5D6A7)
        )
    }
}

@Composable
private fun HsasDesignSystemTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "CHARTE VISUELLE UNIFIÉE (ADN HEPHAISTOS)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Pour former une extension naturelle, le satellite doit adopter les mêmes teintes : fond sombre #0D0E11, cartes #1C1F26 avec contour fin #282C34 et accent Orange Forge #E67E22. En mode clair, il s'adapte automatiquement.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        val themeSnippet = """
            package com.example.satellite.ui.theme

            import androidx.compose.foundation.isSystemInDarkTheme
            import androidx.compose.material3.*
            import androidx.compose.runtime.Composable
            import androidx.compose.ui.graphics.Color

            val HephaistOrange = Color(0xFFE67E22)
            val DarkBackground = Color(0xFF0D0E11)
            val DarkSurface = Color(0xFF14161A)
            val DarkSurfaceContainer = Color(0xFF1C1F26)
            val TextPrimary = Color(0xFFF0F2F5)
            val TextSecondary = Color(0xFF9AA0A6)

            private val DarkColorScheme = darkColorScheme(
                primary = HephaistOrange,
                background = DarkBackground,
                surface = DarkSurface,
                surfaceContainer = DarkSurfaceContainer,
                onPrimary = Color.White,
                onBackground = TextPrimary,
                onSurface = TextPrimary,
                onSurfaceVariant = TextSecondary
            )

            private val LightColorScheme = lightColorScheme(
                primary = Color(0xFFD35400),
                background = Color(0xFFF8F9FA),
                surface = Color(0xFFFFFFFF),
                surfaceContainer = Color(0xFFF1F3F4),
                onPrimary = Color.White,
                onBackground = Color(0xFF1F1F1F),
                onSurface = Color(0xFF1F1F1F),
                onSurfaceVariant = Color(0xFF5F6368)
            )

            @Composable
            fun HephaistSatelliteTheme(
                darkTheme: Boolean = isSystemInDarkTheme(),
                content: @Composable () -> Unit
            ) {
                MaterialTheme(
                    colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
                    content = content
                )
            }
        """.trimIndent()

        CodeSnippetCard(
            title = "Thème Jetpack Compose (HephaistTheme.kt) :",
            code = themeSnippet,
            languageColor = Color(0xFFFFCC80)
        )
    }
}

@Composable
private fun HsasIpcAidlTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PROTOCOLE D'INTEROPÉRABILITÉ IPC & SERVICE BOUND",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "HephaistOS expose le service 'org.hephaistos.service.HOST_IPC'. Le satellite s'y lie via AIDL et peut invoquer les outils de l'hôte (déballage, permissions, root, workspace) et écrire dans la console terminal d'HephaistOS.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }
        }

        val aidlSnippet = """
            package org.hephaistos.ipc;

            interface IHephaistHostService {
                String executeHostTool(String toolId, String jsonParams);
                String getWorkspaceRootPath();
                void logToHostTerminal(String senderTag, String message);
                String getEcosystemRegistryJson();
                boolean isHostRootAvailable();
            }
        """.trimIndent()

        CodeSnippetCard(
            title = "Interface AIDL (IHephaistHostService.aidl) :",
            code = aidlSnippet,
            languageColor = Color(0xFF90CAF9)
        )

        val connectorSnippet = """
            class HostConnector(private val context: Context) {
                private var hostService: IHephaistHostService? = null
                var isConnected: Boolean = false

                private val connection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        hostService = IHephaistHostService.Stub.asInterface(service)
                        isConnected = true
                        hostService?.logToHostTerminal("MonSatellite", "Connecté avec succès à l'hôte HephaistOS !")
                    }
                    override fun onServiceDisconnected(name: ComponentName?) {
                        hostService = null
                        isConnected = false
                    }
                }

                fun bind() {
                    val intent = Intent("org.hephaistos.service.HOST_IPC")
                    context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
                }

                fun triggerTool(toolId: String, paramsJson: String): String? {
                    return hostService?.executeHostTool(toolId, paramsJson)
                }
            }
        """.trimIndent()

        CodeSnippetCard(
            title = "Connexion & Invocation Kotlin (HostConnector.kt) :",
            code = connectorSnippet,
            languageColor = Color(0xFFCE93D8)
        )
    }
}

@Composable
private fun HsasAiStudioPromptTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "PROMPT CLÉ-EN-MAIN POUR GOOGLE AI STUDIO",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Copiez ce prompt dans Google AI Studio (même sur un autre compte) pour générer automatiquement une application satellite 100% conforme à HephaistOS, avec détection immédiate et harmonie visuelle sans faille.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    lineHeight = 16.sp
                )
            }
        }

        val promptSnippet = """
            Tu es chargé de créer une Application Satellite officielle pour le système HephaistOS, conforme à la norme HSAS v1.0 (HephaistOS Satellite Application Standard).

            Directives impératives d'architecture :
            1. AndroidManifest.xml :
               - Déclare dans l'activité principale :
                 <action android:name="org.hephaistos.action.SATELLITE" />
                 <action android:name="org.hephaistos.action.ADDON" />
               - Déclare les métadonnées obligatoires :
                 <meta-data android:name="org.hephaistos.satellite.V1" android:value="true" />
                 <meta-data android:name="org.hephaistos.satellite.ROLE" android:value="<Role_de_ton_satellite>" />
                 <meta-data android:name="org.hephaistos.addon.CAPABILITIES" android:value="kitchen.unpack,kitchen.repack,prop.editor" />
               - Ajoute la requête de service :
                 <queries><intent><action android:name="org.hephaistos.service.HOST_IPC" /></intent></queries>

            2. Interface Jetpack Compose & ADN HephaistOS :
               - Adopte fidèlement la palette d'HephaistOS :
                 Fond sombre : #0D0E11, Cartes : #1C1F26 avec contour fin #282C34, Accent : #E67E22 (Orange Forge).
               - Cartes M3 avec coins arrondis de 16.dp.
               - Boutons tactiles d'au moins 48.dp de hauteur.
               - Logs et commandes en police Monospace (10.sp).
               - Barre d'en-tête épurée avec statut vert 'Satellite HephaistOS Connecté'.

            3. Logique & Partage d'Outils :
               - Opère dans le workspace standard /sdcard/HEPHAISTOS/ (UNPACKED, PACKED, Data/unpack).
               - Intègre la liaison AIDL vers org.hephaistos.service.HOST_IPC (IHephaistHostService).
               - Fournis un README.md récapitulant les fonctionnalités du satellite.
        """.trimIndent()

        CodeSnippetCard(
            title = "Prompt Système à copier dans Google AI Studio :",
            code = promptSnippet,
            languageColor = Color(0xFF80DEEA)
        )
    }
}

@Composable
private fun HsasScriptsAndWebTemplateView() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val shContent = """
            #!/system/bin/sh
            # HEPHAISTOS_SCRIPT_V1
            # NAME: Custom EROFS Builder
            # DESCRIPTION: Repack une partition avec compression EROFS
            # REQUIRE_ROOT: true
            # CAPABILITIES: erofs.compress,partition.repack

            TARGET_DIR="${'$'}1"   # Dossier source dans UNPACKED/
            OUTPUT_IMG="${'$'}2"   # Destination dans PACKED/
            CONFIG_DIR="${'$'}3"   # Métadonnées dans Data/unpack/

            echo "[SCRIPT] Traitement de : ${'$'}TARGET_DIR vers ${'$'}OUTPUT_IMG"
            exit 0
        """.trimIndent()

        CodeSnippetCard(
            title = "Script Shell Satellite (/sdcard/HEPHAISTOS/Scripts/) :",
            code = shContent,
            languageColor = Color(0xFFFFCC80)
        )

        val htmlContent = """
            <!DOCTYPE html>
            <html lang="fr">
            <head>
                <meta charset="utf-8">
                <title>Satellite WebApp HephaistOS</title>
                <style>
                    body { font-family: sans-serif; background: #0D0E11; color: #F0F2F5; padding: 20px; }
                    button { background: #E67E22; color: #fff; border: none; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; }
                    .card { background: #1C1F26; padding: 16px; border-radius: 12px; margin-top: 16px; border: 1px solid #282C34; }
                </style>
            </head>
            <body>
                <h2>Satellite WebApp HephaistOS</h2>
                <div class="card">
                    <p>Contrôlez l'environnement hôte directement depuis cette WebApp.</p>
                    <button onclick="triggerAction()">Lancer Commande Hôte</button>
                </div>
                <script>
                    function triggerAction() {
                        if (window.HephaistosBridge) {
                            window.HephaistosBridge.log('[WEBAPP] Action déclenchée depuis la WebApp !');
                            window.HephaistosBridge.executeCommand('kitchen tools');
                        } else {
                            alert('Bridge HephaistosBridge non disponible hors de HephaistOS');
                        }
                    }
                </script>
            </body>
            </html>
        """.trimIndent()

        CodeSnippetCard(
            title = "WebApp Locale Satellite (/sdcard/HEPHAISTOS/WebApps/) :",
            code = htmlContent,
            languageColor = Color(0xFFCE93D8)
        )
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
