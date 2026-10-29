package com.example.ui.screens

import android.os.Environment
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.UkaEngine
import com.example.data.WorkspaceManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileManagerScreen(
    initialDir: File? = null,
    onBackToHome: () -> Unit = {},
    onInspectApkWithVerisin: (File) -> Unit = {},
    onSelectForUnpack: (File) -> Unit = {},
    onSelectForSignalab: (File) -> Unit = {},
    onBack: () -> Unit = onBackToHome,
    onOpenInUnpack: (File) -> Unit = onSelectForUnpack,
    onOpenInSignalab: (File) -> Unit = onSelectForSignalab,
    onOpenInVerisin: (File) -> Unit = onInspectApkWithVerisin,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Roots
    val storageRoots = remember { WorkspaceManager.getRootLocations(context) }
    var currentDir by remember { mutableStateOf<File?>(initialDir) }
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") } // ALL, APK, IMG, ZIP, CONFIG
    var fileList by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedFileDetails by remember { mutableStateOf<File?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    BackHandler {
        if (selectedFileDetails != null) {
            selectedFileDetails = null
        } else if (currentDir != null) {
            val parent = currentDir?.parentFile
            val internalRoot = Environment.getExternalStorageDirectory()
            val workspaceRoot = WorkspaceManager.getInternalWorkspaceDir(context)

            if (currentDir?.absolutePath == internalRoot.absolutePath || currentDir?.absolutePath == workspaceRoot.absolutePath) {
                currentDir = null
            } else if (parent != null && (parent.absolutePath.startsWith(internalRoot.absolutePath) || parent.absolutePath.startsWith(workspaceRoot.absolutePath))) {
                currentDir = parent
            } else {
                currentDir = null
            }
        } else {
            onBackToHome()
        }
    }

    LaunchedEffect(currentDir, activeFilter, searchQuery, refreshTrigger) {
        val dir = currentDir
        if (dir != null && dir.exists()) {
            val all = dir.listFiles()?.toList() ?: emptyList()
            val filtered = all.filter { f ->
                val matchesQuery = searchQuery.isBlank() || f.name.contains(searchQuery, ignoreCase = true)
                val matchesFilter = when (activeFilter) {
                    "APK" -> f.isDirectory || f.name.endsWith(".apk", ignoreCase = true)
                    "IMG" -> f.isDirectory || f.name.endsWith(".img", ignoreCase = true) || f.name.endsWith(".bin", ignoreCase = true)
                    "ZIP" -> f.isDirectory || f.name.endsWith(".zip", ignoreCase = true) || f.name.endsWith(".tar", ignoreCase = true)
                    "CONFIG" -> f.isDirectory || f.extension.lowercase(Locale.ROOT) in listOf("json", "xml", "txt", "prop")
                    else -> true
                }
                matchesQuery && matchesFilter
            }
            fileList = filtered.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
        } else {
            fileList = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .testTag("file_manager_screen")
    ) {
        // TOP APP BAR
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentDir != null) {
                                currentDir = null
                            } else {
                                onBack()
                                onBackToHome()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (currentDir == null) "Gestionnaire de Fichiers" else currentDir!!.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentDir == null) "Stockage Interne & Workspace Applicatif" else currentDir!!.absolutePath,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(onClick = { refreshTrigger++ }) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Actualiser")
                    }
                }

                // Breadcrumb if inside directory
                if (currentDir != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        item {
                            AssistChip(
                                onClick = { currentDir = null },
                                label = { Text("Accueil") },
                                leadingIcon = {
                                    Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }

                        val dir = currentDir!!
                        val isWorkspace = dir.absolutePath.contains("workspace")
                        item {
                            AssistChip(
                                onClick = {
                                    currentDir = if (isWorkspace) WorkspaceManager.getInternalWorkspaceDir(context)
                                    else Environment.getExternalStorageDirectory()
                                },
                                label = { Text(if (isWorkspace) "Workspace" else "Stockage Interne") }
                            )
                        }

                        // Sub-segments
                        val segments = dir.path.split("/").filter { it.isNotBlank() }
                        val interestingSegments = segments.takeLast(3)
                        for (seg in interestingSegments) {
                            item {
                                Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
                                AssistChip(
                                    onClick = { },
                                    label = { Text(seg, maxLines = 1) }
                                )
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // Search & Filter Bar (only if inside a folder)
        if (currentDir != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Rechercher un fichier ou dossier...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val filters = listOf(
                        "ALL" to "Tous",
                        "APK" to "APKs (.apk)",
                        "IMG" to "Images (.img/.bin)",
                        "ZIP" to "Archives (.zip)",
                        "CONFIG" to "Configs (.json/.xml)"
                    )
                    items(filters) { (key, label) ->
                        FilterChip(
                            selected = activeFilter == key,
                            onClick = { activeFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        // CONTENT AREA
        if (currentDir == null) {
            // ROOT VIEW: Two prominent structured categories
            FileManagerRootView(
                storageRoots = storageRoots,
                onSelectRoot = { root ->
                    currentDir = File(root.path)
                }
            )
        } else {
            // DIRECTORY EXPLORER VIEW
            FileManagerFolderView(
                currentDir = currentDir!!,
                files = fileList,
                onFolderClick = { folder ->
                    currentDir = folder
                    searchQuery = ""
                },
                onFileClick = { file ->
                    selectedFileDetails = file
                }
            )
        }
    }

    // Modal Details for File Click
    selectedFileDetails?.let { file ->
        FileDetailsDialog(
            file = file,
            onDismiss = { selectedFileDetails = null },
            onCopyPath = {
                clipboardManager.setText(AnnotatedString(file.absolutePath))
            },
            onInspectApkWithVerisin = {
                selectedFileDetails = null
                onInspectApkWithVerisin(file)
                onOpenInVerisin(file)
            },
            onSelectForUnpack = {
                selectedFileDetails = null
                onSelectForUnpack(file)
                onOpenInUnpack(file)
            },
            onSelectForSignalab = {
                selectedFileDetails = null
                onSelectForSignalab(file)
                onOpenInSignalab(file)
            }
        )
    }
}

@Composable
private fun FileManagerRootView(
    storageRoots: List<WorkspaceManager.StorageRoot>,
    onSelectRoot: (WorkspaceManager.StorageRoot) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Emplacements Racine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Naviguez dans la mémoire interne ou explorez les projets décompactés du Workspace.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(storageRoots, key = { it.id }) { root ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectRoot(root) }
                    .testTag("root_card_${root.id}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (root.isWorkspace) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (root.isWorkspace) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.secondary
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (root.isWorkspace) Icons.Default.FolderSpecial else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = root.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (root.isWorkspace) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "HAUTE VÉLOCITÉ",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = root.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = root.path,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Quick Workspace Explanatory Card
        item {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Structure du Workspace Interne",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• UNPACKED/ : Contient chaque projet extrait avec support complet des liens symboliques et inodes.\n• Data/ : Rapports de scan cryptographiques, mappings et permissions.\n• KEYS/ : Certificats et clés forgées pour la resignature.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun FileManagerFolderView(
    currentDir: File,
    files: List<File>,
    onFolderClick: (File) -> Unit,
    onFileClick: (File) -> Unit
) {
    if (files.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Ce dossier est vide",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.absolutePath }) { file ->
                FileItemRow(
                    file = file,
                    onFolderClick = { onFolderClick(file) },
                    onFileClick = { onFileClick(file) }
                )
            }
        }
    }
}

@Composable
private fun FileItemRow(
    file: File,
    onFolderClick: () -> Unit,
    onFileClick: () -> Unit
) {
    val isDir = file.isDirectory
    val ext = file.extension.lowercase(Locale.ROOT)

    val icon: ImageVector = when {
        isDir -> Icons.Default.Folder
        ext == "apk" -> Icons.Default.Android
        ext in listOf("img", "raw", "bin") -> Icons.Default.Storage
        ext in listOf("zip", "tar", "gz") -> Icons.Default.Archive
        ext in listOf("json", "xml", "txt", "prop") -> Icons.Default.Description
        else -> Icons.Default.Description
    }

    val iconTint: Color = when {
        isDir -> MaterialTheme.colorScheme.primary
        ext == "apk" -> Color(0xFF2E7D32)
        ext in listOf("img", "raw", "bin") -> MaterialTheme.colorScheme.tertiary
        ext in listOf("zip", "tar") -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val formattedDate = remember(file.lastModified()) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isDir) onFolderClick() else onFileClick()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDir) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isDir) FontWeight.Bold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isDir) {
                        val count = file.listFiles()?.size ?: 0
                        Text(
                            text = "$count élément(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = UkaEngine.formatFileSize(file.length()),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        text = " • $formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileDetailsDialog(
    file: File,
    onDismiss: () -> Unit,
    onCopyPath: () -> Unit,
    onInspectApkWithVerisin: () -> Unit,
    onSelectForUnpack: () -> Unit,
    onSelectForSignalab: () -> Unit
) {
    val ext = file.extension.lowercase(Locale.ROOT)
    val isApk = ext == "apk"
    val isImageOrArchive = ext in listOf("img", "raw", "bin", "zip", "tar")

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = file.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Chemin absolu :",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = file.absolutePath,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Taille :", style = MaterialTheme.typography.bodySmall)
                    Text(UkaEngine.formatFileSize(file.length()), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Dernière modification :", style = MaterialTheme.typography.bodySmall)
                    Text(
                        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified())),
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Actions contextualisées
                if (isApk) {
                    Button(
                        onClick = onInspectApkWithVerisin,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Vérifier avec Sceau / Verisin")
                    }
                }

                if (isImageOrArchive) {
                    Button(
                        onClick = onSelectForSignalab,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ouvrir dans SIGNALAB")
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = onSelectForUnpack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Désassembler dans Unpack/Repack")
                    }
                }

                OutlinedButton(
                    onClick = onCopyPath,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copier le chemin absolu")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
