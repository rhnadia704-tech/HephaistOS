package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.RailState
import com.example.model.Screen
import com.example.ui.HephaistViewModel
import com.example.ui.components.AppDetailBottomSheet
import com.example.ui.components.BottomTerminalPanel
import com.example.ui.components.HeavyTaskDialog
import com.example.ui.components.HephaistNavRail
import com.example.ui.components.PlusDrawer
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TerminalScreen
import com.example.ui.screens.UnpackRepackScreen
import com.example.ui.theme.HephaistOSTheme

class MainActivity : ComponentActivity() {

    private val viewModel: HephaistViewModel by viewModels()

    // Real-time package monitoring BroadcastReceiver (detects installs/uninstalls instantly)
    private val packageChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val packageName = intent.data?.schemeSpecificPart ?: return
            when (action) {
                Intent.ACTION_PACKAGE_ADDED -> {
                    viewModel.logTerminal("[DÉTECTION] Application installée sur l'appareil : $packageName", isSuccess = true)
                    viewModel.refreshCompatibleApps(context)
                }
                Intent.ACTION_PACKAGE_REMOVED -> {
                    viewModel.logTerminal("[DÉTECTION] Application désinstallée de l'appareil : $packageName")
                    viewModel.refreshCompatibleApps(context)
                }
                Intent.ACTION_PACKAGE_REPLACED -> {
                    viewModel.logTerminal("[DÉTECTION] Application mise à jour sur l'appareil : $packageName", isSuccess = true)
                    viewModel.refreshCompatibleApps(context)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register dynamic package listener for installed/uninstalled APK detection without manual refresh
        val packageFilter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageChangeReceiver, packageFilter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageChangeReceiver, packageFilter)
        }

        // Connect IPC host service log listener and PackageReceiver static event
        com.example.data.HephaistHostService.addTerminalLogListener { sender, msg ->
            viewModel.logTerminal("[$sender] $msg")
        }
        com.example.data.PackageReceiver.addListener { pkg, action ->
            viewModel.logTerminal("[ÉCOSYSTÈME] Événement détecté sur paquet $pkg ($action)")
            viewModel.refreshCompatibleApps(this@MainActivity)
        }

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val context = LocalContext.current
            val snackbarHostState = remember { SnackbarHostState() }
            var totalDragX by remember { mutableFloatStateOf(0f) }

            var showStorageDialog by remember { mutableStateOf(false) }
            var showNonRootDialog by remember { mutableStateOf(false) }
            var hasPromptedRoot by remember { mutableStateOf(false) }

            val storagePermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                viewModel.updateStoragePermission(allGranted)
            }

            // Automatic startup checks: installed apps scan, storage permissions & root diagnostic
            LaunchedEffect(Unit) {
                // 1. Instant scan of phone's installed packages
                viewModel.refreshCompatibleApps(this@MainActivity)

                // 2. Storage permission check
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val isManager = Environment.isExternalStorageManager()
                    viewModel.updateStoragePermission(isManager)
                    if (!isManager) {
                        showStorageDialog = true
                    }
                } else {
                    storagePermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    )
                }

                // 3. Root privilege check at launch
                viewModel.checkRootAccess()
            }

            // Show non-root warning dialog once initial check finishes if not rooted
            LaunchedEffect(uiState.isCheckingRoot, uiState.rootStatus) {
                if (!uiState.isCheckingRoot && !uiState.rootStatus.isRooted && !hasPromptedRoot && uiState.rootStatus.statusText != "Statut non vérifié") {
                    hasPromptedRoot = true
                    showNonRootDialog = true
                }
            }

            // Handle snackbar messages from ViewModel
            LaunchedEffect(uiState.snackbarMessage) {
                uiState.snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.dismissSnackbar()
                }
            }

            HephaistOSTheme(
                themeMode = uiState.themeMode,
                colorStyle = uiState.colorStyle
            ) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .pointerInput(uiState.isPlusDrawerOpen, uiState.isBottomTerminalOpen) {
                                // Disable horizontal rail gestures when modal drawer or bottom terminal is active
                                if (!uiState.isPlusDrawerOpen && !uiState.isBottomTerminalOpen) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDragX = 0f },
                                        onHorizontalDrag = { _, dragAmount ->
                                            totalDragX += dragAmount
                                        },
                                        onDragEnd = {
                                            if (totalDragX > 55f) {
                                                // Swipe right -> reveal or expand rail
                                                viewModel.onSwipeRight()
                                            } else if (totalDragX < -55f) {
                                                // Swipe left -> collapse or hide rail
                                                viewModel.onSwipeLeft()
                                            }
                                            totalDragX = 0f
                                        },
                                        onDragCancel = { totalDragX = 0f }
                                    )
                                }
                            }
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // 1. Far Left: Expressive Animated Navigation Rail
                            HephaistNavRail(
                                currentScreen = uiState.currentScreen,
                                railState = uiState.railState,
                                isPlusDrawerOpen = uiState.isPlusDrawerOpen,
                                onScreenSelected = { screen ->
                                    if (uiState.isFullConsoleMode) {
                                        viewModel.exitFullConsoleMode()
                                    }
                                    viewModel.selectScreen(screen)
                                },
                                onTogglePlusDrawer = {
                                    viewModel.togglePlusDrawer()
                                },
                                onToggleExpand = {
                                    viewModel.toggleRailExpand()
                                }
                            )

                            // 2. Main Content Area
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                if (uiState.isFullConsoleMode) {
                                    // Full Terminal Mode
                                    TerminalScreen(
                                        terminalEntries = uiState.terminalEntries,
                                        isFullConsoleMode = true,
                                        onExecuteCommand = { cmd ->
                                            viewModel.executeTerminalCommand(cmd)
                                        },
                                        onExitFullConsole = {
                                            viewModel.exitFullConsoleMode()
                                        }
                                    )
                                } else {
                                    // Standard Mode
                                    AnimatedContent(
                                        targetState = uiState.currentScreen,
                                        transitionSpec = {
                                            (slideInHorizontally { width -> width / 4 } + fadeIn()) togetherWith
                                                    (slideOutHorizontally { width -> -width / 4 } + fadeOut())
                                        },
                                        label = "screen_transition"
                                    ) { screen ->
                                        when (screen) {
                                            Screen.HOME -> HomeScreen(
                                                compatibleApps = uiState.compatibleApps,
                                                railState = uiState.railState,
                                                onToggleRailFullscreen = {
                                                    viewModel.toggleRailFullscreen()
                                                },
                                                onSwipeUpToOpenTerminal = {
                                                    viewModel.openBottomTerminal()
                                                },
                                                onAppClick = { app ->
                                                    viewModel.onTriggerAppAction(app)
                                                },
                                                onNavigateToUnpackRepack = {
                                                    viewModel.selectScreen(Screen.UNPACK_REPACK)
                                                },
                                                onRefresh = {
                                                    viewModel.refreshCompatibleApps(context)
                                                }
                                            )

                                            Screen.UNPACK_REPACK -> UnpackRepackScreen(
                                                unpackState = uiState.unpackUiState,
                                                repackState = uiState.repackUiState,
                                                onFileSelected = { file ->
                                                    viewModel.onSelectTargetFile(file)
                                                },
                                                onStartUnpack = { file, parts ->
                                                    viewModel.startUnpack(file, parts)
                                                },
                                                onRefreshUnpackedFolders = {
                                                    viewModel.refreshUnpackedFolders()
                                                },
                                                onTargetSelectedForRepack = { target ->
                                                    viewModel.onSelectTargetForRepack(target)
                                                },
                                                onStartRepack = { target, format, sparse, avb ->
                                                    viewModel.startRepack(target, format, sparse, avb)
                                                },
                                                onCreateSampleRom = {
                                                    viewModel.createSampleRomForTesting()
                                                },
                                                onOpenFullTerminal = {
                                                    viewModel.openBottomTerminal()
                                                },
                                                onBackToHome = {
                                                    viewModel.selectScreen(Screen.HOME)
                                                }
                                            )

                                            Screen.SETTINGS -> SettingsScreen(
                                                currentTab = uiState.settingsTab,
                                                themeMode = uiState.themeMode,
                                                colorStyle = uiState.colorStyle,
                                                isStorageGranted = uiState.isStoragePermissionGranted,
                                                rootStatus = uiState.rootStatus,
                                                isCheckingRoot = uiState.isCheckingRoot,
                                                changelog = uiState.changelog,
                                                isGeneratingPdf = uiState.isGeneratingPdf,
                                                generatedPdfFile = uiState.generatedPdfFile,
                                                workspaceStats = uiState.workspaceStats,
                                                onTabSelected = { tab ->
                                                    viewModel.setSettingsTab(tab)
                                                },
                                                onThemeModeChange = { mode ->
                                                    viewModel.setThemeMode(mode)
                                                },
                                                onColorStyleChange = { style ->
                                                    viewModel.setColorStyle(style)
                                                },
                                                onStoragePermissionResult = { granted ->
                                                    viewModel.updateStoragePermission(granted)
                                                },
                                                onRequestRootCheck = {
                                                    viewModel.checkRootAccess()
                                                },
                                                onGeneratePdf = { ctx ->
                                                    viewModel.generateDocumentationPdf(ctx)
                                                },
                                                onOpenPdf = { ctx ->
                                                    viewModel.openGeneratedPdf(ctx)
                                                },
                                                onDownloadStarterKit = {
                                                    viewModel.downloadStarterKit(context)
                                                },
                                                onBack = {
                                                    viewModel.selectScreen(Screen.HOME)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Subtle Edge Tab when rail is completely hidden
                        AnimatedVisibility(
                            visible = uiState.railState == RailState.HIDDEN,
                            enter = fadeIn() + slideInHorizontally { -it },
                            exit = fadeOut() + slideOutHorizontally { -it },
                            modifier = Modifier.align(Alignment.CenterStart)
                        ) {
                            Surface(
                                modifier = Modifier
                                    .padding(start = 2.dp)
                                    .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))
                                    .clickable { viewModel.onSwipeRight() }
                                    .testTag("edge_reveal_rail_btn"),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                tonalElevation = 4.dp
                            ) {
                                Box(
                                    modifier = Modifier.size(width = 20.dp, height = 52.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "Révéler la barre latérale",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // 4. Bottom Terminal Panel (Continuously stretchable)
                        BottomTerminalPanel(
                            isOpen = uiState.isBottomTerminalOpen && !uiState.isFullConsoleMode,
                            terminalEntries = uiState.terminalEntries,
                            onClose = { viewModel.closeBottomTerminal() },
                            onExecuteCommand = { cmd -> viewModel.executeTerminalCommand(cmd) }
                        )

                        // 5. Plus Drawer Overlay
                        PlusDrawer(
                            isOpen = uiState.isPlusDrawerOpen,
                            onClose = { viewModel.closePlusDrawer() },
                            onAddScript = { name, content, isPy, reqRoot ->
                                viewModel.addScript(name, content, isPy, reqRoot)
                            },
                            onAddWebApp = { name, html ->
                                viewModel.addWebApp(name, html)
                            },
                            onAddSampleAddon = {
                                viewModel.addSampleKitchenAddon()
                            }
                        )

                        // 6. Modal Bottom Sheet for App Details
                        AppDetailBottomSheet(
                            app = uiState.selectedApp,
                            onDismiss = { viewModel.setSelectedApp(null) },
                            onLaunchApp = { app ->
                                viewModel.launchApp(context, app)
                            }
                        )

                        // 7. Heavy Task Confirmation Dialog
                        HeavyTaskDialog(
                            data = uiState.heavyTaskDialogData,
                            onConfirmFullConsole = { viewModel.confirmHeavyTaskFullConsole() },
                            onConfirmBackground = { viewModel.confirmHeavyTaskBackground() },
                            onDismiss = { viewModel.dismissHeavyTaskDialog() }
                        )

                        // 8. Full-screen WebApp Runner Dialog
                        uiState.activeRunningWebApp?.let { webApp ->
                            com.example.ui.components.WebAppRunnerDialog(
                                app = webApp,
                                onDismiss = { viewModel.dismissWebApp() },
                                onLogToTerminal = { log -> viewModel.logTerminal(log) }
                            )
                        }

                        // 9. Startup Storage Permission Dialog
                        if (showStorageDialog) {
                            AlertDialog(
                                onDismissRequest = { showStorageDialog = false },
                                title = {
                                    Text("Accès au Stockage SD Requis", fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Text("Pour déballer (unpack) et réassembler (repack) les ROMs et GSIs dans /sdcard/HEPHAISTOS/, HephaistOS a besoin de l'autorisation d'accès complet au stockage.")
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            showStorageDialog = false
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                                try {
                                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                                        data = Uri.parse("package:${context.packageName}")
                                                    }
                                                    context.startActivity(intent)
                                                } catch (_: Exception) {
                                                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                                                    context.startActivity(fallback)
                                                }
                                            }
                                        }
                                    ) {
                                        Text("Accorder l'accès")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showStorageDialog = false }) {
                                        Text("Plus tard")
                                    }
                                }
                            )
                        }

                        // 10. Startup Non-Root Restricted Mode Dialog
                        if (showNonRootDialog) {
                            AlertDialog(
                                onDismissRequest = { showNonRootDialog = false },
                                title = {
                                    Text("Mode Restreint (Non-Root)", fontWeight = FontWeight.Bold)
                                },
                                text = {
                                    Text("Aucun accès SuperUtilisateur (su) n'a été détecté.\n\nHephaistOS fonctionnera en Mode Restreint : l'unpack, le repack standard, l'édition de scripts et les webapps restent fonctionnels. Seules les commandes directes de flash et de montage avancé requièrent les privilèges root.")
                                },
                                confirmButton = {
                                    Button(onClick = { showNonRootDialog = false }) {
                                        Text("Continuer en mode restreint")
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(packageChangeReceiver)
        } catch (_: Exception) {}
    }
}
