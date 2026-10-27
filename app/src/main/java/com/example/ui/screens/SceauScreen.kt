package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VerisinEngine
import com.example.model.KeycoreTab
import com.example.model.SceauSubScreen
import com.example.model.SceauUiState
import java.io.File

@Composable
fun SceauScreen(
    sceauState: SceauUiState,
    // Verisin parameters
    verisinSelectedPath: String,
    isVerifyingVerisin: Boolean,
    verisinResult: VerisinEngine.VerisinResult?,
    verisinErrorMessage: String?,
    verisinAvailableApks: List<File>,
    onVerisinPathChanged: (String) -> Unit,
    onVerisinUriSelected: (Uri) -> Unit,
    onVerisinVerify: (String) -> Unit,
    onVerisinRefreshApks: () -> Unit,
    onVerisinClearResult: () -> Unit,
    // Keycore parameters
    onKeycoreSelectTab: (KeycoreTab) -> Unit,
    onKeycoreSelectApkPath: (String) -> Unit,
    onKeycoreUriSelected: (Uri) -> Unit,
    onKeycoreAnalyzeApk: (String) -> Unit,
    onKeycoreGenerateKeymaker: (name: String, cn: String, org: String, country: String, alg: String, years: Int) -> Unit,
    onKeycoreGenerateVerisinKey: (name: String, cn: String, org: String, country: String, years: Int) -> Unit,
    onKeycoreGenerateVerisinClone: (cloneName: String) -> Unit,
    onKeycoreResignWithClone: () -> Unit,
    onKeycoreRefreshApks: () -> Unit,
    // Navigation
    onNavigateSubScreen: (SceauSubScreen) -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        if (sceauState.currentSubScreen != SceauSubScreen.HUB) {
            onNavigateSubScreen(SceauSubScreen.HUB)
        } else {
            onBackToHome()
        }
    }

    AnimatedContent(
        targetState = sceauState.currentSubScreen,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sceau_sub_screen_anim"
    ) { subScreen ->
        when (subScreen) {
            SceauSubScreen.HUB -> {
                SceauHubView(
                    onOpenVerisin = { onNavigateSubScreen(SceauSubScreen.VERISIN) },
                    onOpenKeycore = { onNavigateSubScreen(SceauSubScreen.KEYCORE) },
                    onBackToHome = onBackToHome,
                    modifier = modifier
                )
            }

            SceauSubScreen.VERISIN -> {
                VerisinScreen(
                    selectedPath = verisinSelectedPath,
                    isVerifying = isVerifyingVerisin,
                    result = verisinResult,
                    errorMessage = verisinErrorMessage,
                    availableApks = verisinAvailableApks,
                    onPathChanged = onVerisinPathChanged,
                    onUriSelected = onVerisinUriSelected,
                    onVerify = onVerisinVerify,
                    onRefreshAvailableApks = onVerisinRefreshApks,
                    onClearResult = onVerisinClearResult,
                    onBackToHome = { onNavigateSubScreen(SceauSubScreen.HUB) },
                    modifier = modifier
                )
            }

            SceauSubScreen.KEYCORE -> {
                KeycoreScreen(
                    keycoreState = sceauState.keycoreState,
                    availableApks = verisinAvailableApks,
                    onSelectTab = onKeycoreSelectTab,
                    onSelectApkPath = onKeycoreSelectApkPath,
                    onUriSelected = onKeycoreUriSelected,
                    onAnalyzeApk = onKeycoreAnalyzeApk,
                    onGenerateKeymaker = onKeycoreGenerateKeymaker,
                    onGenerateVerisinKey = onKeycoreGenerateVerisinKey,
                    onGenerateVerisinClone = onKeycoreGenerateVerisinClone,
                    onResignWithClone = onKeycoreResignWithClone,
                    onRefreshApks = onKeycoreRefreshApks,
                    onOpenInVerisin = { apkPath ->
                        onVerisinPathChanged(apkPath)
                        onVerisinVerify(apkPath)
                        onNavigateSubScreen(SceauSubScreen.VERISIN)
                    },
                    onBackToSceau = { onNavigateSubScreen(SceauSubScreen.HUB) },
                    modifier = modifier
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SCEAU HUB VIEW — Simple & Épuré
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SceauHubView(
    onOpenVerisin: () -> Unit,
    onOpenKeycore: () -> Unit,
    onBackToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        // Top Bar
        Surface(
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToHome,
                    modifier = Modifier.testTag("sceau_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Retour à l'accueil"
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SCEAU",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Moteur Cryptographique & Gestion des Sceaux",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Vertical List — Simple, clean and uncluttered
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Option 1 : VERISIN
            item {
                SceauModuleCard(
                    title = "1. VERISIN",
                    subtitle = "Audit Cryptographique & Vérification de Signature",
                    description = "Analyse forensique complète des signatures d'applications (Schémas V1, V2, V3, V3.1, détails X.509, hachages MD5/SHA/CRC32, condensats et clés publiques OpenSSL).",
                    badge = "Moteur d'Audit",
                    icon = Icons.Default.VerifiedUser,
                    buttonLabel = "Ouvrir VERISIN",
                    testTag = "sceau_open_verisin_button",
                    onClick = onOpenVerisin
                )
            }

            // Option 2 : KEYCORE
            item {
                SceauModuleCard(
                    title = "2. KEYCORE",
                    subtitle = "Forge de Clés, Clés Cibles & Signatures Parallèles",
                    description = "Création de trousseaux complets avec 3 sous-outils puissants :\n• Keymaker : Forge universelle de clés AOSP standard\n• Verisin Key : Clé calquée sur l'architecture d'un APK cible\n• Verisin Clone : Signature parallèle avec reproduction des DNs & resignature directe",
                    badge = "Forge Cryptographique",
                    icon = Icons.Default.VpnKey,
                    buttonLabel = "Ouvrir KEYCORE",
                    testTag = "sceau_open_keycore_button",
                    onClick = onOpenKeycore
                )
            }
        }
    }
}

@Composable
private fun SceauModuleCard(
    title: String,
    subtitle: String,
    description: String,
    badge: String,
    icon: ImageVector,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = onClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = buttonLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
