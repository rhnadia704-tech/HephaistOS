package com.example.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PlusDrawer(
    isOpen: Boolean,
    onClose: () -> Unit,
    onAddScript: (name: String, content: String, isPython: Boolean, requiresRoot: Boolean) -> Unit,
    onAddWebApp: (name: String, htmlContent: String) -> Unit,
    onAddSampleAddon: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    var selectedTabIndex by remember { mutableIntStateOf(0) }

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
              <div class="box">
                <h2>Outil WebApp HephaistOS</h2>
                <p>Application HTML connectée à l'hôte HephaistOS et ses capacités de kitchen.</p>
                <button onclick="alert('Action exécutée !')">Tester l'outil</button>
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
            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Surface(
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 400.dp)
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
                                    text = "Scripts, WebApps & Addons",
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

                    Spacer(modifier = Modifier.height(16.dp))

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
                            text = { Text("Script", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 1,
                            onClick = { selectedTabIndex = 1 },
                            text = { Text("WebApp", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Html, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                        Tab(
                            selected = selectedTabIndex == 2,
                            onClick = { selectedTabIndex = 2 },
                            text = { Text("Addon", fontSize = 13.sp) },
                            icon = { Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (selectedTabIndex == 0) {
                            // TAB 0: SCRIPT IMPORT
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
                                        text = "Exiger la délégation Root (su)",
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
                                    label = { Text("Code du script") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp),
                                    maxLines = 10
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        onAddScript(scriptName, scriptContent, isPythonScript, isRootRequired)
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Enregistrer et Créer la Carte")
                                }
                            }
                        } else if (selectedTabIndex == 1) {
                            // TAB 1: WEBAPP IMPORT
                            item {
                                Text(
                                    text = "Ajouter une WebApp (HTML ou archive ZIP) dans /sdcard/HEPHAISTOS/WebApps/",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = webAppName,
                                    onValueChange = { webAppName = it },
                                    label = { Text("Titre de la WebApp") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = webAppHtml,
                                    onValueChange = { webAppHtml = it },
                                    label = { Text("Contenu index.html (fichiers associés auto-détectés)") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    maxLines = 12
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        onAddWebApp(webAppName, webAppHtml)
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Compiler et Intégrer la WebApp")
                                }
                            }
                        } else {
                            // TAB 2: ADDON DÉTECTION & DÉMO
                            item {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Text(
                                            text = "Détection Automatique d'Addons",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "HephaistOS scrute en tâche de fond les applications Android portant le marqueur 'org.hephaistos.action.ADDON'. Dès l'installation d'une application compatible, elle s'affiche comme carte sur l'accueil et ses outils sont partagés avec tous.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = {
                                        onAddSampleAddon()
                                        onClose()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Extension, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Générer l'Addon Kitchen Démo (GSI)")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
