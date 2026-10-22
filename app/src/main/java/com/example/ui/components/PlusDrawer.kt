package com.example.ui.components

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkspaceManager
import com.example.model.AheExtension
import java.io.File
import java.io.InputStream

@Composable
fun PlusDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    installedExtensions: List<AheExtension> = emptyList(),
    onImportAheStream: (InputStream) -> Unit = {},
    onOpenExtension: (AheExtension) -> Unit = {},
    onUninstallExtension: (String) -> Unit = {},
    onAddScript: (name: String, content: String, isPython: Boolean, requiresRoot: Boolean) -> Unit = { _, _, _, _ -> },
    onAddWebApp: (name: String, htmlContent: String) -> Unit = { _, _ -> },
    onAddSampleAddon: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showDocsDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val ahePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val stream = context.contentResolver.openInputStream(uri)
                if (stream != null) {
                    onImportAheStream(stream)
                    Toast.makeText(context, "Analyse et installation de l'extension .ahe...", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Impossible de lire le fichier sélectionné", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Script form state
    var scriptName by remember { mutableStateOf("analyse_partition") }
    var scriptContent by remember {
        mutableStateOf(
            """
            echo "[HEPHAISTOS SCRIPT] Démarrage du script d'analyse..."
            echo "[INFO] Vérification des en-têtes et descripteurs de partitions"
            echo "[SUCCESS] Script exécuté avec succès"
            """.trimIndent()
        )
    }
    var isPythonScript by remember { mutableStateOf(false) }
    var isRootRequired by remember { mutableStateOf(false) }

    // WebApp form state
    var webAppName by remember { mutableStateOf("Kitchen Web Tool") }
    var webAppHtml by remember {
        mutableStateOf(
            """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8">
              <style>
                body { background: #181a1f; color: #fff; font-family: sans-serif; padding: 16px; }
                h2 { color: #e67e22; }
                .box { background: #22252b; border: 1px solid #333; padding: 12px; border-radius: 8px; }
                button { background: #e67e22; border: none; color: white; padding: 8px 16px; border-radius: 6px; cursor: pointer; }
              </style>
            </head>
            <body>
              <h2>Extension Web HephaistOS</h2>
              <div class="box">
                <p>Outil d'inspection interactif connecté à l'écosystème HephaistOS.</p>
                <button onclick="alert('Connexion Cerveau HephaistOS active !')">Tester le bus</button>
              </div>
            </body>
            </html>
            """.trimIndent()
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClose
            )
    ) {
        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally { -it } + fadeIn(),
            exit = slideOutHorizontally { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 340.dp, max = 440.dp)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Intercept click */ }
                    ),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
                shape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
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
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Ajout au Cerveau",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Extensions .ahe, Scripts & Outils",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .testTag("plus_drawer_close")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Prominent Import .ahe Action Button
                    Button(
                        onClick = { ahePickerLauncher.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("plus_import_ahe_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "+ IMPORTER UNE EXTENSION (.AHE)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab selector
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTabIndex == 0,
                            onClick = { selectedTabIndex = 0 },
                            text = { Text("Extensions", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("Scripts", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("WebApps", fontSize = 12.sp) },
                            icon = { Icon(Icons.Default.Html, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (selectedTabIndex == 0) {
                            // TAB 0: AHE EXTENSIONS
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showDocsDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Doc .ahe", fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            WorkspaceManager.ensureAheStarterTemplate(context)
                                            Toast.makeText(context, "Template prêt dans sdcard/HEPHAISTOS/Templates/ahe_template/", Toast.LENGTH_LONG).show()
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Template", fontSize = 12.sp)
                                    }
                                }
                            }

                            if (installedExtensions.isEmpty()) {
                                item {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.FolderZip,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(36.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "Aucune extension .ahe installée",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Les fichiers .ahe sont des modules complets intégrant binaires ARM64, scripts shell, UI Material 3 et communication inter-extensions.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(installedExtensions) { ext ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = ext.manifest.name,
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "v${ext.manifest.version} • ${ext.manifest.author}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onUninstallExtension(ext.manifest.id) },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Supprimer",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }

                                            if (ext.manifest.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = ext.manifest.description,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            if (ext.manifest.exportedBinaries.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = "Binaires : ${ext.manifest.exportedBinaries.joinToString(", ")}",
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.End
                                            ) {
                                                Button(
                                                    onClick = {
                                                        onOpenExtension(ext)
                                                        onClose()
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                                ) {
                                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ouvrir", fontSize = 12.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (selectedTabIndex == 1) {
                            // TAB 1: SCRIPT IMPORT
                            item {
                                Text(
                                    text = "Ajouter un script dans /sdcard/HEPHAISTOS/Scripts/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = scriptName,
                                    onValueChange = { scriptName = it },
                                    label = { Text("Nom du script (ex: extract_boot)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isPythonScript) "Interpréteur : Python (.py)" else "Interpréteur : Shell (.sh)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isPythonScript,
                                        onCheckedChange = { isPythonScript = it }
                                    )
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Exiger les droits SuperUtilisateur (Root)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isRootRequired,
                                        onCheckedChange = { isRootRequired = it }
                                    )
                                }
                            }

                            item {
                                OutlinedTextField(
                                    value = scriptContent,
                                    onValueChange = { scriptContent = it },
                                    label = { Text("Contenu du script exécutable") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        onAddScript(scriptName, scriptContent, isPythonScript, isRootRequired)
                                        Toast.makeText(context, "Script enregistré avec succès dans HEPHAISTOS/Scripts/ !", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Terminal, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enregistrer & Intégrer le Script")
                                }
                            }
                        } else {
                            // TAB 2: WEBAPPS
                            item {
                                Text(
                                    text = "Ajouter une WebApp autonome dans /sdcard/HEPHAISTOS/WebApps/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = webAppName,
                                    onValueChange = { webAppName = it },
                                    label = { Text("Nom de l'application Web") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = webAppHtml,
                                    onValueChange = { webAppHtml = it },
                                    label = { Text("Code HTML / JS / CSS (Single Page)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        onAddWebApp(webAppName, webAppHtml)
                                        Toast.makeText(context, "WebApp enregistrée dans HEPHAISTOS/WebApps/ !", Toast.LENGTH_SHORT).show()
                                        onClose()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Html, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Déployer la WebApp")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDocsDialog) {
        AheDocsDialog(onDismiss = { showDocsDialog = false })
    }
}

@Composable
fun AheDocsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val sampleSnippet = """
{
  "id": "com.monprojet.ext.sample",
  "name": "Mon Outil Rom",
  "version": "1.0.0",
  "author": "HephaistOS Dev",
  "permissions": ["STORAGE", "ROOT", "EXEC_BIN"],
  "exportedBinaries": ["my_tool"],
  "ui": {
    "hasDedicatedPage": true,
    "tabs": [
      {
        "id": "tab_1",
        "title": "Principal",
        "icon": "Dashboard",
        "sections": [
          {
            "type": "form",
            "title": "Action",
            "fields": [
              { "id": "proj", "type": "dropdown", "label": "Projet", "source": "unpacked_projects" }
            ],
            "action": { "id": "run", "label": "Lancer", "script": "scripts/run.sh" }
          }
        ]
      }
    ]
  }
}
    """.trimIndent()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Documentation des Extensions (.ahe)")
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            ) {
                item {
                    Text(
                        text = "Qu'est-ce qu'un fichier .ahe ?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Un fichier .ahe (Android HephaistOS Extension) est une archive standard ZIP contenant un manifest.json, des binaires natifs ARM64 dans bin/, des scripts shell dans scripts/ et des outils dans tools/.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Structure d'une extension :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = """
                        extension.ahe
                        ├── manifest.json
                        ├── bin/ (exécutables natifs ARM64)
                        ├── scripts/ (scripts .sh)
                        ├── tools/ (jar, patchs, data)
                        └── docs/README.md
                        """.trimIndent(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Exemple manifest.json :",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sampleSnippet,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Pour plus de détails, consultez DOC_AHE_EXTENSIONS.md.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(sampleSnippet))
                    Toast.makeText(context, "Modèle manifest copié !", Toast.LENGTH_SHORT).show()
                }
            ) {
                Text("Copier Modèle")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}
