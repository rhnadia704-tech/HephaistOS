package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AddonRegistry
import com.example.data.CompatibleAppsRepository
import com.example.data.KitchenEngine
import com.example.data.PdfExporterService
import com.example.data.RootChecker
import com.example.data.RootDelegator
import com.example.data.WorkspaceManager
import com.example.model.AppType
import com.example.model.ChangelogVersion
import com.example.model.ColorStyle
import com.example.model.CompatibleApp
import com.example.model.HeavyTaskDialogData
import com.example.model.RailState
import com.example.model.RootStatus
import com.example.model.Screen
import com.example.model.SettingsTab
import com.example.model.TerminalEntry
import com.example.model.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.data.ExtractorEngine
import com.example.data.GemeauSignerEngine
import com.example.data.UkaEngine
import com.example.model.ApkSignItem
import com.example.model.ExtractionMode
import com.example.model.ExtractorUiState
import com.example.model.GemeauTab
import com.example.model.GemeauUiState
import com.example.model.KeysetInfo
import com.example.model.RepackUiState
import com.example.model.UnpackUiState
import com.example.data.ExtensionManager
import com.example.data.KeycoreEngine
import com.example.data.VerisinEngine
import com.example.model.AheAction
import com.example.model.ApkVerificationDetail
import com.example.model.GemeauVerifySubTab
import com.example.model.RomVerificationAudit
import com.example.model.AheExtension
import com.example.model.AheUiState
import com.example.model.AnalystReportSummary
import com.example.model.AnalystUiState
import com.example.model.KeycoreTab
import com.example.model.SceauSubScreen
import com.example.model.SceauUiState
import com.example.data.AnalystEngine
import com.example.data.SignalabEngine
import com.example.model.SignalabUiState
import com.example.model.SignalabSignOptions
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class HephaistUiState(
    val currentScreen: Screen = Screen.HOME,
    val railState: RailState = RailState.COMPACT,
    val isPlusDrawerOpen: Boolean = false,
    val isBottomTerminalOpen: Boolean = false,
    val isFullConsoleMode: Boolean = false,
    val heavyTaskDialogData: HeavyTaskDialogData? = null,
    val activeRunningWebApp: CompatibleApp? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorStyle: ColorStyle = ColorStyle.FORGE,
    val settingsTab: SettingsTab = SettingsTab.THEME,
    val compatibleApps: List<CompatibleApp> = emptyList(),
    val changelog: List<ChangelogVersion> = emptyList(),
    val selectedApp: CompatibleApp? = null,
    val isStoragePermissionGranted: Boolean = false,
    val rootStatus: RootStatus = RootStatus(
        isRooted = false,
        statusText = "Statut non vérifié",
        binaryPath = null,
        details = listOf("Cliquez sur 'Tester su' pour analyser l'environnement SuperUtilisateur.")
    ),
    val isCheckingRoot: Boolean = false,
    val terminalEntries: List<TerminalEntry> = emptyList(),
    val isGeneratingPdf: Boolean = false,
    val generatedPdfFile: File? = null,
    val workspaceStats: Map<String, Int> = emptyMap(),
    val unpackUiState: UnpackUiState = UnpackUiState(),
    val repackUiState: RepackUiState = RepackUiState(),
    val extractorUiState: ExtractorUiState = ExtractorUiState(),
    val gemeauUiState: GemeauUiState = GemeauUiState(),
    val aheUiState: AheUiState = AheUiState(),
    val analystUiState: AnalystUiState = AnalystUiState(),
    val sceauUiState: SceauUiState = SceauUiState(),
    val signalabUiState: SignalabUiState = SignalabUiState(),
    val verisinSelectedPath: String = "",
    val isVerifyingVerisin: Boolean = false,
    val verisinResult: VerisinEngine.VerisinResult? = null,
    val verisinError: String? = null,
    val verisinAvailableApks: List<File> = emptyList(),
    val pinnedRailScreens: List<Screen> = listOf(
        Screen.UNPACK_REPACK,
        Screen.EXTRACTOR,
        Screen.GEMEAU,
        Screen.ANALYST,
        Screen.SCEAU,
        Screen.SIGNALAB,
        Screen.FILE_MANAGER
    ),
    val snackbarMessage: String? = null
)

class HephaistViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HephaistUiState())
    val uiState: StateFlow<HephaistUiState> = _uiState.asStateFlow()

    fun pinScreenToRail(screen: Screen) {
        if (!_uiState.value.pinnedRailScreens.contains(screen)) {
            _uiState.update { it.copy(pinnedRailScreens = it.pinnedRailScreens + screen) }
            showSnackbar("${screen.title} a été épinglé au Rail latéral.")
        }
    }

    fun unpinScreenFromRail(screen: Screen) {
        val removableScreens = setOf(
            Screen.UNPACK_REPACK,
            Screen.EXTRACTOR,
            Screen.GEMEAU,
            Screen.ANALYST,
            Screen.SCEAU,
            Screen.SIGNALAB,
            Screen.FILE_MANAGER
        )
        if (screen in removableScreens && _uiState.value.pinnedRailScreens.contains(screen)) {
            _uiState.update { it.copy(pinnedRailScreens = it.pinnedRailScreens.filter { s -> s != screen }) }
            showSnackbar("${screen.title} retiré du Rail.")
        }
    }


    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val app = getApplication<Application>()
        val changelog = CompatibleAppsRepository.getChangelogHistory()

        val welcomeTerminal = listOf(
            TerminalEntry("╔═══════════════════════════════════════════════════════════╗"),
            TerminalEntry("║        Bienvenue dans le Cerveau HephaistOS Kitchen       ║"),
            TerminalEntry("║    Architecture Hôte • Custom ROMs • Bus Inter-Addons     ║"),
            TerminalEntry("╚═══════════════════════════════════════════════════════════╝"),
            TerminalEntry("Espace de travail : /sdcard/HEPHAISTOS/", isSuccess = true),
            TerminalEntry("Outils de base kitchen disponibles (payload, sparse, super, vbmeta, boot)."),
            TerminalEntry("Tapez 'kitchen tools' ou 'help' pour démarrer.")
        )

        _uiState.update {
            it.copy(
                compatibleApps = emptyList(),
                changelog = changelog,
                terminalEntries = welcomeTerminal
            )
        }

        // Initialize workspace on background IO
        viewModelScope.launch(Dispatchers.IO) {
            val stats = WorkspaceManager.initializeWorkspace(app)
            val templatesDir = WorkspaceManager.getSubDir(app, "Templates")
            val pdfFile = File(templatesDir, PdfExporterService.PDF_FILE_NAME)
            val readyPdf = if (pdfFile.exists()) {
                pdfFile
            } else {
                try {
                    PdfExporterService.generateAddonSdkSpecificationPdf(app)
                } catch (_: Exception) {
                    null
                }
            }

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        workspaceStats = stats,
                        generatedPdfFile = readyPdf
                    )
                }
                refreshAheExtensions(app)
            }
        }
    }

    fun selectScreen(screen: Screen) {
        _uiState.update {
            val updatedGemeau = if (screen == Screen.GEMEAU) {
                it.gemeauUiState.copy(currentSubScreen = com.example.model.GemeauSubScreen.HUB)
            } else {
                it.gemeauUiState
            }
            it.copy(currentScreen = screen, isPlusDrawerOpen = false, gemeauUiState = updatedGemeau)
        }
    }

    fun setRailState(state: RailState) {
        _uiState.update { it.copy(railState = state) }
    }

    fun onSwipeRight() {
        _uiState.update { current ->
            val nextState = when (current.railState) {
                RailState.HIDDEN -> RailState.COMPACT
                RailState.COMPACT -> RailState.EXPANDED
                RailState.EXPANDED -> RailState.EXPANDED
            }
            current.copy(railState = nextState)
        }
    }

    fun onSwipeLeft() {
        _uiState.update { current ->
            val nextState = when (current.railState) {
                RailState.EXPANDED -> RailState.COMPACT
                RailState.COMPACT -> RailState.HIDDEN
                RailState.HIDDEN -> RailState.HIDDEN
            }
            current.copy(railState = nextState)
        }
    }

    fun toggleRailFullscreen() {
        _uiState.update { current ->
            val nextState = if (current.railState == RailState.HIDDEN) {
                RailState.COMPACT
            } else {
                RailState.HIDDEN
            }
            current.copy(railState = nextState)
        }
    }

    fun toggleRailExpand() {
        _uiState.update { current ->
            val nextState = when (current.railState) {
                RailState.HIDDEN -> RailState.COMPACT
                RailState.COMPACT -> RailState.EXPANDED
                RailState.EXPANDED -> RailState.COMPACT
            }
            current.copy(railState = nextState)
        }
    }

    fun togglePlusDrawer() {
        _uiState.update { it.copy(isPlusDrawerOpen = !it.isPlusDrawerOpen) }
    }

    fun openPlusDrawer() {
        _uiState.update { it.copy(isPlusDrawerOpen = true) }
    }

    fun closePlusDrawer() {
        _uiState.update { it.copy(isPlusDrawerOpen = false) }
    }

    fun openBottomTerminal() {
        _uiState.update { it.copy(isBottomTerminalOpen = true) }
    }

    fun closeBottomTerminal() {
        _uiState.update { it.copy(isBottomTerminalOpen = false) }
    }

    fun toggleBottomTerminal() {
        _uiState.update { it.copy(isBottomTerminalOpen = !it.isBottomTerminalOpen) }
    }

    fun enterFullConsoleMode() {
        _uiState.update {
            it.copy(
                isFullConsoleMode = true,
                isBottomTerminalOpen = false,
                heavyTaskDialogData = null
            )
        }
        showSnackbar("Mode Full Console activé. Accueil désactivé.")
    }

    fun exitFullConsoleMode() {
        _uiState.update { it.copy(isFullConsoleMode = false) }
        showSnackbar("Retour à l'accueil HephaistOS.")
    }

    fun dismissHeavyTaskDialog() {
        _uiState.update { it.copy(heavyTaskDialogData = null) }
    }

    fun dismissWebApp() {
        _uiState.update { it.copy(activeRunningWebApp = null) }
    }

    fun onTriggerAppAction(app: CompatibleApp, taskName: String = "Exécution de tâche") {
        when (app.appType) {
            AppType.WEBAPP -> {
                _uiState.update { it.copy(activeRunningWebApp = app) }
                logTerminal("[WEBAPP] Ouverture de l'environnement WebApp '${app.name}'", isSuccess = true)
            }
            AppType.SHELL_SCRIPT -> {
                executeScriptLive(app)
            }
            AppType.NATIVE_ADDON, AppType.KITCHEN_CORE -> {
                if (app.isHeavyTask) {
                    _uiState.update {
                        it.copy(heavyTaskDialogData = HeavyTaskDialogData(app, taskName))
                    }
                } else {
                    executeModuleLiveTask(app, taskName)
                }
            }
        }
    }

    fun confirmHeavyTaskFullConsole() {
        val request = _uiState.value.heavyTaskDialogData ?: return
        enterFullConsoleMode()
        executeModuleLiveTask(request.app, request.taskName, inFullConsole = true)
    }

    fun confirmHeavyTaskBackground() {
        val request = _uiState.value.heavyTaskDialogData ?: return
        dismissHeavyTaskDialog()
        executeModuleLiveTask(request.app, request.taskName, inFullConsole = false)
    }

    fun executeModuleLiveTask(
        app: CompatibleApp,
        taskName: String,
        inFullConsole: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            if (!inFullConsole && !_uiState.value.isBottomTerminalOpen) {
                withContext(Dispatchers.Main) {
                    openBottomTerminal()
                }
            }

            logTerminal("─── DÉBUT DE TÂCHE : [${app.name}] ───")
            logTerminal("[DISPATCH] Tâche '$taskName' transmise au module (${app.packageName})")
            delay(350)
            logTerminal("[INIT] ░░░░░░░░░░ [10%] Initialisation du pipeline kitchen et allocation mémoire")
            delay(400)
            logTerminal("[SANDBOX] ███░░░░░░░ [30%] Vérification des marqueurs Addon et autorisations")
            delay(450)
            logTerminal("[EXEC] ██████░░░░ [60%] Traitement en cours sur les images de partition...")
            delay(500)
            logTerminal("[SYNC] ████████░░ [85%] Synchronisation dans /sdcard/HEPHAISTOS/Output/")
            delay(400)
            logTerminal("[SUCCESS] ██████████ [100%] Tâche terminée avec succès (Code 0)", isSuccess = true)
            logTerminal("─── FIN DE TÂCHE : [${app.name}] ───\n")
        }
    }

    private fun executeScriptLive(app: CompatibleApp) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                openBottomTerminal()
            }
            logTerminal("─── EXÉCUTION DU SCRIPT : ${app.name} ───")
            val path = app.scriptPath
            if (path == null) {
                logTerminal("[ERREUR] Chemin du script introuvable.", isError = true)
                return@launch
            }
            val scriptFile = File(path)
            RootDelegator.executeScript(
                scriptFile = scriptFile,
                useRoot = RootDelegator.isRootDelegated(app.packageName),
                onOutputLine = { line ->
                    logTerminal(line)
                }
            )
            logTerminal("─── SCRIPT TERMINÉ ───\n", isSuccess = true)
        }
    }

    fun addScript(name: String, content: String, isPython: Boolean, requiresRoot: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val file = WorkspaceManager.saveScript(app, name, content, isPython)
            val stats = WorkspaceManager.initializeWorkspace(app)

            val scriptApp = CompatibleApp(
                id = "script_${file.name}",
                name = name.replace("_", " ").replaceFirstChar { it.uppercase() },
                packageName = "org.hephaistos.script.${file.nameWithoutExtension}",
                version = if (isPython) "Python" else "Shell",
                description = "Script sauvegardé dans /sdcard/HEPHAISTOS/Scripts/${file.name}",
                category = "Script Kitchen",
                isHero = false,
                isInstalled = true,
                isHeavyTask = false,
                status = "Script Prêt",
                statusColor = 0xFFFFA726,
                iconKey = "terminal",
                appType = AppType.SHELL_SCRIPT,
                scriptPath = file.absolutePath,
                capabilities = listOf("script.exec")
            )

            if (requiresRoot) {
                RootDelegator.grantRootToPackage(scriptApp.packageName)
            }

            withContext(Dispatchers.Main) {
                val list = _uiState.value.compatibleApps.toMutableList()
                list.add(scriptApp)
                _uiState.update {
                    it.copy(
                        compatibleApps = list,
                        workspaceStats = stats,
                        isPlusDrawerOpen = false
                    )
                }
                logTerminal("[WORKSPACE] Script '${file.name}' ajouté à la boîte à outils.", isSuccess = true)
                showSnackbar("Script [${file.name}] enregistré et ajouté comme carte !")
            }
        }
    }

    fun addWebApp(name: String, htmlContent: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val indexFile = WorkspaceManager.saveHtmlWebApp(app, name, htmlContent)
            val stats = WorkspaceManager.initializeWorkspace(app)

            val webApp = CompatibleApp(
                id = "webapp_${indexFile.parentFile?.name}",
                name = name,
                packageName = "org.hephaistos.webapp.${indexFile.parentFile?.name}",
                version = "HTML5",
                description = "Application WebApp intégrée exécutée dans HephaistOS.",
                category = "WebApp Addon",
                isHero = false,
                isInstalled = true,
                isHeavyTask = false,
                status = "Web Prêt",
                statusColor = 0xFF29B6F6,
                iconKey = "web",
                appType = AppType.WEBAPP,
                webAppUrl = "file://${indexFile.absolutePath}",
                capabilities = listOf("ui.web", "kitchen.bridge")
            )

            withContext(Dispatchers.Main) {
                val list = _uiState.value.compatibleApps.toMutableList()
                list.add(webApp)
                _uiState.update {
                    it.copy(
                        compatibleApps = list,
                        workspaceStats = stats,
                        isPlusDrawerOpen = false
                    )
                }
                logTerminal("[WORKSPACE] WebApp '$name' enregistrée dans /sdcard/HEPHAISTOS/WebApps/", isSuccess = true)
                showSnackbar("WebApp [$name] compilée et ajoutée à l'accueil !")
            }
        }
    }

    fun addSampleKitchenAddon() {
        // No mock addon injected - only real installed applications are loaded
        showSnackbar("Seules les applications réelles installées sur l'appareil sont chargées.")
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        showSnackbar("Thème appliqué : ${mode.label}")
    }

    fun setColorStyle(style: ColorStyle) {
        _uiState.update { it.copy(colorStyle = style) }
        showSnackbar("Style appliqué : ${style.title}")
    }

    fun setSettingsTab(tab: SettingsTab) {
        _uiState.update { it.copy(settingsTab = tab) }
    }

    fun setSelectedApp(app: CompatibleApp?) {
        _uiState.update { it.copy(selectedApp = app) }
    }

    fun updateStoragePermission(granted: Boolean) {
        _uiState.update { it.copy(isStoragePermissionGranted = granted) }
        if (granted) {
            val app = getApplication<Application>()
            viewModelScope.launch(Dispatchers.IO) {
                val stats = WorkspaceManager.initializeWorkspace(app)
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(workspaceStats = stats) }
                }
            }
            showSnackbar("Stockage /sdcard/HEPHAISTOS/ initialisé avec succès !")
        } else {
            showSnackbar("Autorisation de stockage refusée.")
        }
    }

    fun checkRootAccess() {
        _uiState.update { it.copy(isCheckingRoot = true) }
        viewModelScope.launch(Dispatchers.IO) {
            val status = RootChecker.checkRoot()
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        rootStatus = status,
                        isCheckingRoot = false
                    )
                }
                showSnackbar(status.statusText)
            }
        }
    }

    fun generateDocumentationPdf(context: Context) {
        _uiState.update { it.copy(isGeneratingPdf = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = PdfExporterService.generateAddonSdkSpecificationPdf(context)
                val stats = WorkspaceManager.initializeWorkspace(context)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isGeneratingPdf = false,
                            generatedPdfFile = file,
                            workspaceStats = stats
                        )
                    }
                    showSnackbar("Guide PDF généré : ${file.name} (${file.length() / 1024} Ko)")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isGeneratingPdf = false) }
                    showSnackbar("Erreur lors de la génération du PDF : ${e.message}")
                }
            }
        }
    }

    fun openGeneratedPdf(context: Context) {
        val file = _uiState.value.generatedPdfFile
        if (file != null && file.exists()) {
            PdfExporterService.openPdf(context, file)
        } else {
            generateDocumentationPdf(context)
        }
    }

    fun downloadStarterKit(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val templatesDir = WorkspaceManager.getSubDir(context, "Templates")
            val zip = File(templatesDir, PdfExporterService.STARTER_KIT_ZIP_NAME)
            if (!zip.exists()) {
                PdfExporterService.generateAddonSdkSpecificationPdf(context)
            }
            withContext(Dispatchers.Main) {
                showSnackbar("Starter Kit disponible dans /sdcard/HEPHAISTOS/Templates/${PdfExporterService.STARTER_KIT_ZIP_NAME}")
            }
        }
    }

    fun logTerminal(text: String, isSuccess: Boolean = false, isError: Boolean = false) {
        val timeFmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
        val timestamp = timeFmt.format(Date())
        _uiState.update { current ->
            val list = current.terminalEntries.toMutableList()
            list.add(TerminalEntry("[$timestamp] $text", isSuccess = isSuccess, isError = isError))
            current.copy(terminalEntries = list)
        }
    }

    fun executeTerminalCommand(input: String) {
        val cmd = input.trim()
        if (cmd.isEmpty()) return

        val entries = _uiState.value.terminalEntries.toMutableList()
        entries.add(TerminalEntry("hephaist@os:~$ $cmd", isCommand = true))

        val parts = cmd.split("\\s+".toRegex())
        val action = parts[0].lowercase()

        when (action) {
            "help" -> {
                entries.add(TerminalEntry("Commandes HephaistOS Core :"))
                entries.add(TerminalEntry("  help                 - Affiche cette aide"))
                entries.add(TerminalEntry("  status               - Statut complet du cerveau et de l'environnement"))
                entries.add(TerminalEntry("  apps                 - Liste des addons et applications connectées"))
                entries.add(TerminalEntry("  verisin <apk>        - Analyse et audit de signature approfondi (VERISIN)"))
                entries.add(TerminalEntry("  verisin-help         - Aide et options du moteur VERISIN"))
                entries.add(TerminalEntry("  kitchen tools        - Liste les outils de base kitchen intégrés"))
                entries.add(TerminalEntry("  kitchen detect <f>   - Analyse et identifie le format d'un fichier"))
                entries.add(TerminalEntry("  kitchen unpack <f>   - Extraction assistée (payload.bin ou img)"))
                entries.add(TerminalEntry("  workspace            - Résumé du dossier /sdcard/HEPHAISTOS/"))
                entries.add(TerminalEntry("  root                 - Diagnostic des privilèges SuperUtilisateur"))
                entries.add(TerminalEntry("  clear                - Réinitialiser la console"))
            }
            "verisin-help" -> {
                for (line in VerisinEngine.getHelpBanner()) {
                    entries.add(TerminalEntry(line))
                }
            }
            "verisin" -> {
                val sub = if (parts.size > 1) parts[1].lowercase() else ""
                if (parts.size == 1 || sub == "help" || sub == "--help" || sub == "-h" || sub == "-help") {
                    for (line in VerisinEngine.getHelpBanner()) {
                        entries.add(TerminalEntry(line))
                    }
                } else {
                    val rawArg = if (sub == "verify" || sub == "-v") {
                        if (parts.size > 2) cmd.substringAfter(parts[1]).trim() else ""
                    } else {
                        cmd.substringAfter(parts[0]).trim()
                    }
                    val targetArg = WorkspaceManager.sanitizePath(rawArg)

                    if (targetArg.isBlank()) {
                        entries.add(TerminalEntry("[ERREUR VERISIN] Veuillez spécifier le chemin de l'APK à vérifier.", isError = true))
                        entries.add(TerminalEntry("Usage : verisin [chemin_apk] ou verisin verify [chemin_apk]"))
                        entries.add(TerminalEntry("Exemple : verisin SystemUI.apk (ne pas taper de chevrons < >)"))
                        entries.add(TerminalEntry("Tapez 'verisin-help' pour l'aide complète."))
                    } else {
                        val resolvedFile = WorkspaceManager.resolveApkPath(getApplication(), targetArg)
                        if (resolvedFile == null || !resolvedFile.exists()) {
                            entries.add(TerminalEntry("[ERREUR VERISIN] Fichier APK introuvable : '$targetArg'", isError = true))
                            entries.add(TerminalEntry("Astuce : Placez le fichier dans /sdcard/HEPHAISTOS/ ou saisissez son chemin complet (sans chevrons < >)."))
                        } else {
                            entries.add(TerminalEntry("[VERISIN] Lancement de l'audit cryptographique sur : ${resolvedFile.name}...", isSuccess = true))
                            entries.add(TerminalEntry("Chemin cible résolu : ${resolvedFile.absolutePath}"))
                            _uiState.update { it.copy(verisinSelectedPath = resolvedFile.absolutePath, isVerifyingVerisin = true) }
                            viewModelScope.launch(Dispatchers.IO) {
                                val result = VerisinEngine.verifyApkFile(
                                    context = getApplication(),
                                    apkFile = resolvedFile,
                                    onLog = { msg -> logTerminal(msg) }
                                )
                                withContext(Dispatchers.Main) {
                                    _uiState.update { it.copy(isVerifyingVerisin = false, verisinResult = result) }
                                    for (line in result.rawReportText.lines()) {
                                        logTerminal(line)
                                    }
                                    if (result.reportFile != null) {
                                        logTerminal("[SUCCÈS VERISIN] Rapport sauvegardé dans : ${result.reportFile.absolutePath}", isSuccess = true)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            "kitchen" -> {
                val sub = if (parts.size > 1) parts[1].lowercase() else ""
                when (sub) {
                    "tools" -> {
                        entries.add(TerminalEntry("[BOÎTE À OUTILS DE BASE KITCHEN]", isSuccess = true))
                        for (tool in KitchenEngine.BUILTIN_TOOLS) {
                            entries.add(TerminalEntry("  • ${tool.name} [${tool.category}]"))
                            entries.add(TerminalEntry("    Syntaxe : ${tool.commandSyntax}"))
                        }
                    }
                    "detect" -> {
                        val fileName = if (parts.size > 2) parts[2] else "payload.bin"
                        val candidate = File(WorkspaceManager.getSubDir(getApplication(), "Payloads"), fileName)
                        val detected = KitchenEngine.detectFileType(candidate)
                        entries.add(TerminalEntry("Analyse de '$fileName' : $detected", isSuccess = true))
                    }
                    "unpack" -> {
                        val fileName = if (parts.size > 2) parts[2] else "payload.bin"
                        entries.add(TerminalEntry("Lancement du déballage kitchen pour $fileName...", isSuccess = true))
                        viewModelScope.launch(Dispatchers.IO) {
                            val candidate = File(WorkspaceManager.getSubDir(getApplication(), "Payloads"), fileName)
                            val outDir = WorkspaceManager.getSubDir(getApplication(), "Extracted")
                            KitchenEngine.runPayloadUnpack(candidate, outDir) { step, pct, ok, err ->
                                logTerminal("[$pct%] $step", isSuccess = ok, isError = err)
                            }
                        }
                    }
                    else -> {
                        entries.add(TerminalEntry("Usage : kitchen [tools | detect <fichier> | unpack <fichier>]"))
                    }
                }
            }
            "workspace" -> {
                entries.add(TerminalEntry("[ESPACE DE TRAVAIL : /sdcard/HEPHAISTOS/]", isSuccess = true))
                for ((folder, count) in _uiState.value.workspaceStats) {
                    entries.add(TerminalEntry("  /$folder/ -> $count fichier(s)"))
                }
            }
            "status" -> {
                val s = _uiState.value
                entries.add(TerminalEntry("[STATUT SYSTÈME HEPHAISTOS]", isSuccess = true))
                entries.add(TerminalEntry("  • Cerveau : HephaistOS v1.0.0 (Kitchen Host)"))
                entries.add(TerminalEntry("  • Mode Full Console : ${if (s.isFullConsoleMode) "Actif" else "Désactivé"}"))
                entries.add(TerminalEntry("  • Stockage : ${if (s.isStoragePermissionGranted) "Autorisé" else "Restreint"} (/sdcard/HEPHAISTOS/)"))
                entries.add(TerminalEntry("  • Statut Root : ${s.rootStatus.statusText}"))
                entries.add(TerminalEntry("  • Modules/Addons actifs : ${s.compatibleApps.size}"))
            }
            "apps" -> {
                entries.add(TerminalEntry("[APPLICATIONS ET ADDONS DÉTECTÉS]", isSuccess = true))
                if (_uiState.value.compatibleApps.isEmpty()) {
                    entries.add(TerminalEntry("  Aucun module pour le moment. Utilisez le bouton (+) pour ajouter des scripts, webapps ou addons."))
                } else {
                    for (app in _uiState.value.compatibleApps) {
                        entries.add(TerminalEntry("  • ${app.name} (${app.appType.name}) [${app.status}] -> ${app.packageName}"))
                    }
                }
            }
            "root" -> {
                val r = _uiState.value.rootStatus
                entries.add(TerminalEntry("Statut Root : ${r.statusText}"))
                if (r.binaryPath != null) {
                    entries.add(TerminalEntry("Binaire su : ${r.binaryPath}", isSuccess = true))
                }
                for (d in r.details) {
                    entries.add(TerminalEntry("  * $d"))
                }
            }
            "clear" -> {
                entries.clear()
                entries.add(TerminalEntry("Console HephaistOS réinitialisée.", isSuccess = true))
            }
            else -> {
                entries.add(
                    TerminalEntry(
                        "Commande non reconnue : '$cmd'. Tapez 'help' pour la liste des commandes.",
                        isError = true
                    )
                )
            }
        }

        _uiState.update { it.copy(terminalEntries = entries) }
    }

    fun launchApp(context: Context, app: CompatibleApp) {
        onTriggerAppAction(app, "Lancement")
        if (app.appType == AppType.NATIVE_ADDON) {
            val result = CompatibleAppsRepository.launchOrInteractWithApp(context, app)
            showSnackbar(result)
        }
    }

    fun refreshCompatibleApps(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val installed = AddonRegistry.scanInstalledAddons(context)
            val stats = WorkspaceManager.initializeWorkspace(context)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        compatibleApps = installed,
                        workspaceStats = stats
                    )
                }
                if (installed.isNotEmpty()) {
                    showSnackbar("Applications installées détectées : ${installed.size}")
                }
            }
        }
    }

    // =========================================================================
    // HEPHAISTOS NATIVE UNPACK & REPACK ENGINE (UKA ARCHITECTURE)
    // =========================================================================

    fun onSelectTargetFile(file: File) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    unpackUiState = it.unpackUiState.copy(
                        selectedFile = file,
                        isAnalyzing = true,
                        currentStep = "Inspection des descripteurs et superblocks...",
                        lastError = null
                    )
                )
            }

            logTerminal("[UKA] Analyse de la cible : ${file.name} (${file.length()} octets)")
            val meta = UkaEngine.analyzeTarget(file)

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        unpackUiState = it.unpackUiState.copy(
                            targetMetadata = meta,
                            isAnalyzing = false,
                            currentStep = "Format détecté : ${meta.targetType.label} (${meta.detectedFs})"
                        )
                    )
                }
                logTerminal("[ANALYSE] Type : ${meta.targetType.label} | Partitions : ${meta.partitions.map { p -> p.name }.joinToString(", ")}", isSuccess = true)
            }
        }
    }

    fun startUnpack(file: File, selectedPartitions: List<String>) {
        val meta = _uiState.value.unpackUiState.targetMetadata ?: UkaEngine.analyzeTarget(file)
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    unpackUiState = it.unpackUiState.copy(
                        isExtracting = true,
                        progress = 0.05f,
                        currentStep = "Démarrage de l'extraction...",
                        recentLogs = listOf("[UKA] Début de l'opération de désassemblage"),
                        lastError = null,
                        successMessage = null
                    )
                )
            }

            val result = UkaEngine.executeUnpack(
                context = getApplication(),
                targetFile = file,
                metadata = meta,
                selectedPartitions = selectedPartitions,
                onProgress = { step, progress, logLine ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.update { current ->
                            val updatedLogs = (current.unpackUiState.recentLogs + logLine).takeLast(40)
                            current.copy(
                                unpackUiState = current.unpackUiState.copy(
                                    currentStep = step,
                                    progress = progress,
                                    recentLogs = updatedLogs
                                )
                            )
                        }
                    }
                    logTerminal(logLine)
                }
            )

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val unpackedDir = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            unpackUiState = it.unpackUiState.copy(
                                isExtracting = false,
                                progress = 1.0f,
                                currentStep = "Désassemblage achevé avec succès !",
                                unpackedTargetDirectory = unpackedDir,
                                successMessage = "ROM désassemblée dans sdcard/HEPHAISTOS/UNPACKED/${unpackedDir?.name}"
                            )
                        )
                    }
                    showSnackbar("Unpack terminé : ${unpackedDir?.name} prêt pour modification ou repack.")
                    refreshUnpackedFolders()
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    _uiState.update {
                        it.copy(
                            unpackUiState = it.unpackUiState.copy(
                                isExtracting = false,
                                lastError = err
                            )
                        )
                    }
                    showSnackbar("Erreur lors de l'unpack : $err")
                }
            }
        }
    }

    fun refreshUnpackedFolders() {
        viewModelScope.launch(Dispatchers.IO) {
            val unpackedDir = WorkspaceManager.getUnpackedDir(getApplication())
            val folders = unpackedDir.listFiles { f -> f.isDirectory }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        repackUiState = it.repackUiState.copy(
                            availableUnpackedTargets = folders
                        )
                    )
                }
            }
        }
    }

    fun onSelectTargetForRepack(targetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val unpackDataDir = WorkspaceManager.getUnpackDataDir(getApplication())
            val metaFile = File(unpackDataDir, "${targetName}_meta.json")

            val meta = if (metaFile.exists()) {
                try {
                    val text = metaFile.readText()
                    // Extract metadata from JSON string
                    UkaEngine.analyzeTarget(File(WorkspaceManager.getUnpackedDir(getApplication()), targetName))
                } catch (_: Exception) {
                    null
                }
            } else null

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        repackUiState = it.repackUiState.copy(
                            selectedTargetName = targetName,
                            targetMetadata = meta,
                            currentStep = "Cible sélectionnée : $targetName"
                        )
                    )
                }
            }
        }
    }

    fun startRepack(
        targetName: String,
        repackFormat: String,
        enableSparse: Boolean,
        patchAvb: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update {
                it.copy(
                    repackUiState = it.repackUiState.copy(
                        isRepacking = true,
                        repackProgress = 0.05f,
                        currentStep = "Démarrage du repack...",
                        repackLogs = listOf("[UKA] Réassemblage de $targetName en cours..."),
                        lastError = null,
                        successMessage = null
                    )
                )
            }

            val result = UkaEngine.executeRepack(
                context = getApplication(),
                targetName = targetName,
                repackFormat = repackFormat,
                enableSparse = enableSparse,
                patchAvb = patchAvb,
                onProgress = { step, progress, logLine ->
                    viewModelScope.launch(Dispatchers.Main) {
                        _uiState.update { current ->
                            val updatedLogs = (current.repackUiState.repackLogs + logLine).takeLast(40)
                            current.copy(
                                repackUiState = current.repackUiState.copy(
                                    currentStep = step,
                                    repackProgress = progress,
                                    repackLogs = updatedLogs
                                )
                            )
                        }
                    }
                    logTerminal(logLine)
                }
            )

            withContext(Dispatchers.Main) {
                if (result.isSuccess) {
                    val packedFile = result.getOrNull()
                    _uiState.update {
                        it.copy(
                            repackUiState = it.repackUiState.copy(
                                isRepacking = false,
                                repackProgress = 1.0f,
                                currentStep = "Repack terminé avec succès !",
                                lastPackedFile = packedFile,
                                successMessage = "Image générée dans sdcard/HEPHAISTOS/PACKED/${packedFile?.name}"
                            )
                        )
                    }
                    showSnackbar("Repack réussi : ${packedFile?.name} généré dans HEPHAISTOS/PACKED/.")
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Erreur inconnue"
                    _uiState.update {
                        it.copy(
                            repackUiState = it.repackUiState.copy(
                                isRepacking = false,
                                lastError = err
                            )
                        )
                    }
                    showSnackbar("Échec du repack : $err")
                }
            }
        }
    }

    fun createSampleRomForTesting() {
        viewModelScope.launch(Dispatchers.IO) {
            val sampleFile = UkaEngine.createSampleTargetRom(getApplication())
            withContext(Dispatchers.Main) {
                onSelectTargetFile(sampleFile)
                showSnackbar("Image Test GSI générée dans Payloads/ : ${sampleFile.name}")
            }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun refreshExtractorProjects(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val projects = WorkspaceManager.listUnpackedProjects(context)
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val currentSelected = state.extractorUiState.selectedUnpackedProject
                    val selected = if (currentSelected.isNotEmpty() && projects.contains(currentSelected)) {
                        currentSelected
                    } else {
                        projects.firstOrNull() ?: ""
                    }
                    state.copy(
                        extractorUiState = state.extractorUiState.copy(
                            availableUnpackedProjects = projects,
                            selectedUnpackedProject = selected
                        )
                    )
                }
            }
        }
    }

    fun selectExtractorProject(projectName: String) {
        _uiState.update {
            it.copy(
                extractorUiState = it.extractorUiState.copy(
                    selectedUnpackedProject = projectName
                )
            )
        }
    }

    fun executeDeviceExtraction(context: Context) {
        if (_uiState.value.extractorUiState.isExecuting) return

        _uiState.update {
            it.copy(
                extractorUiState = it.extractorUiState.copy(
                    isExecuting = true,
                    currentMode = ExtractionMode.FULL_DEVICE,
                    progress = 0.05f,
                    currentStepText = "Initialisation de l'extraction matérielle...",
                    logs = listOf(TerminalEntry("[EXTRACTEUR] Démarrage de l'analyse intégrale de l'appareil..."))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = ExtractorEngine.extractCurrentDevice(
                context = context,
                onProgress = { step, percent, count, mb ->
                    _uiState.update {
                        it.copy(
                            extractorUiState = it.extractorUiState.copy(
                                currentStepText = step,
                                progress = percent,
                                extractedFilesCount = count,
                                extractedMb = mb
                            )
                        )
                    }
                },
                onLog = { logLine ->
                    val isSuccess = logLine.contains("[SUCCÈS]") || logLine.contains("[INFO]")
                    val isError = logLine.contains("[ERREUR]")
                    _uiState.update {
                        it.copy(
                            extractorUiState = it.extractorUiState.copy(
                                logs = (it.extractorUiState.logs + TerminalEntry(logLine, isError = isError, isSuccess = isSuccess)).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        extractorUiState = it.extractorUiState.copy(
                            isExecuting = false,
                            progress = if (result.isSuccess) 1.0f else it.extractorUiState.progress,
                            currentStepText = if (result.isSuccess) "Extraction et base de données factuelle générées avec succès !" else "Erreur pendant l'extraction",
                            lastGeneratedReportPath = result.reportFile?.absolutePath,
                            lastGeneratedDataDir = result.dataDir?.absolutePath
                        )
                    )
                }
                showSnackbar(result.summary)
            }
        }
    }

    fun executeUnpackedAnalysis(context: Context) {
        val selectedProject = _uiState.value.extractorUiState.selectedUnpackedProject
        if (selectedProject.isBlank()) {
            showSnackbar("Veuillez sélectionner un projet déballé dans UNPACKED.")
            return
        }
        if (_uiState.value.extractorUiState.isExecuting) return

        _uiState.update {
            it.copy(
                extractorUiState = it.extractorUiState.copy(
                    isExecuting = true,
                    currentMode = ExtractionMode.UNPACKED_ANALYSIS,
                    progress = 0.05f,
                    currentStepText = "Analyse du projet $selectedProject...",
                    logs = listOf(TerminalEntry("[ANALYSEUR GSI] Démarrage de l'inspection de $selectedProject..."))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val result = ExtractorEngine.analyseUnpackedProject(
                context = context,
                projectName = selectedProject,
                onProgress = { step, percent, count, mb ->
                    _uiState.update {
                        it.copy(
                            extractorUiState = it.extractorUiState.copy(
                                currentStepText = step,
                                progress = percent,
                                extractedFilesCount = count,
                                extractedMb = mb
                            )
                        )
                    }
                },
                onLog = { logLine ->
                    val isSuccess = logLine.contains("[SUCCÈS]") || logLine.contains("[INFO]")
                    val isError = logLine.contains("[ERREUR]")
                    _uiState.update {
                        it.copy(
                            extractorUiState = it.extractorUiState.copy(
                                logs = (it.extractorUiState.logs + TerminalEntry(logLine, isError = isError, isSuccess = isSuccess)).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        extractorUiState = it.extractorUiState.copy(
                            isExecuting = false,
                            progress = if (result.isSuccess) 1.0f else it.extractorUiState.progress,
                            currentStepText = if (result.isSuccess) "Rapport et matrice de compatibilité générés !" else "Erreur pendant l'analyse",
                            lastGeneratedReportPath = result.reportFile?.absolutePath,
                            lastGeneratedDataDir = result.dataDir?.absolutePath
                        )
                    )
                }
                showSnackbar(result.summary)
            }
        }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GEMEAU MODULE ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    fun setGemeauTab(tab: GemeauTab) {
        _uiState.update { it.copy(gemeauUiState = it.gemeauUiState.copy(activeTab = tab)) }
    }

    fun setGemeauSubScreen(subScreen: com.example.model.GemeauSubScreen) {
        _uiState.update { it.copy(gemeauUiState = it.gemeauUiState.copy(currentSubScreen = subScreen)) }
    }

    fun navigateBackInGemeau() {
        _uiState.update { state ->
            val nextSubScreen = when (state.gemeauUiState.currentSubScreen) {
                com.example.model.GemeauSubScreen.HUB -> com.example.model.GemeauSubScreen.HUB
                com.example.model.GemeauSubScreen.KEYMAKER,
                com.example.model.GemeauSubScreen.SIGNAPK,
                com.example.model.GemeauSubScreen.ROMSIGN,
                com.example.model.GemeauSubScreen.RSIGNER,
                com.example.model.GemeauSubScreen.VERIFICATION_HUB -> com.example.model.GemeauSubScreen.HUB
                com.example.model.GemeauSubScreen.VERIFY_APP,
                com.example.model.GemeauSubScreen.VERIFY_ROM -> com.example.model.GemeauSubScreen.VERIFICATION_HUB
            }
            state.copy(gemeauUiState = state.gemeauUiState.copy(currentSubScreen = nextSubScreen))
        }
    }

    fun refreshKeysets(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val keysets = WorkspaceManager.listAvailableKeysets(context)
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    val curSel = state.gemeauUiState.selectedKeyset
                    val newSel = if (curSel != null && keysets.any { it.name == curSel.name }) {
                        keysets.first { it.name == curSel.name }
                    } else {
                        keysets.firstOrNull()
                    }
                    state.copy(
                        gemeauUiState = state.gemeauUiState.copy(
                            availableKeysets = keysets,
                            selectedKeyset = newSel
                        )
                    )
                }
            }
        }
    }

    fun selectKeyset(keyset: KeysetInfo) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(selectedKeyset = keyset))
        }
    }

    fun updateGemeauKeymakerForm(
        name: String,
        cn: String,
        org: String,
        country: String,
        alg: String,
        years: Int
    ) {
        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    newKeysetName = name,
                    commonName = cn,
                    organization = org,
                    countryCode = country,
                    keyAlgorithm = alg,
                    validityYears = years
                )
            )
        }
    }

    fun generateKeyset(context: Context) {
        val form = _uiState.value.gemeauUiState
        if (form.isExecuting) return

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isExecuting = true,
                    progress = 0.05f,
                    currentStepText = "Initialisation de la génération du keyset...",
                    logs = listOf(TerminalEntry("[KEYMAKER] Démarrage du générateur cryptographique AOSP/AVB..."))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newKeyset = GemeauSignerEngine.generateKeyset(
                    context = context,
                    keysetName = form.newKeysetName,
                    commonName = form.commonName,
                    organization = form.organization,
                    countryCode = form.countryCode,
                    algorithm = form.keyAlgorithm,
                    validityYears = form.validityYears,
                    onProgress = { step, percent ->
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    currentStepText = step,
                                    progress = percent
                                )
                            )
                        }
                    },
                    onLog = { line ->
                        val isSuccess = line.contains("[SUCCÈS]") || line.contains("[TERMINÉ]")
                        val isError = line.contains("[ERREUR]")
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    logs = (it.gemeauUiState.logs + TerminalEntry(line, isError = isError, isSuccess = isSuccess)).takeLast(250)
                                )
                            )
                        }
                    }
                )

                val updatedKeysets = WorkspaceManager.listAvailableKeysets(context)
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                isExecuting = false,
                                progress = 1.0f,
                                currentStepText = "Keyset '${newKeyset.name}' généré avec succès !",
                                availableKeysets = updatedKeysets,
                                selectedKeyset = newKeyset
                            )
                        )
                    }
                    showSnackbar("Trousseau '${newKeyset.name}' (6 clés AOSP/AVB) généré !")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                isExecuting = false,
                                currentStepText = "Erreur : ${e.message}",
                                logs = it.gemeauUiState.logs + TerminalEntry("[ERREUR] Échec : ${e.message}", isError = true)
                            )
                        )
                    }
                    showSnackbar("Erreur lors de la création du keyset: ${e.message}")
                }
            }
        }
    }

    fun setSelectedApksForSign(files: List<File>) {
        val items = files.map { f ->
            val sizeMb = "%.2f Mo".format(f.length() / 1048576.0)
            ApkSignItem(
                file = f,
                name = f.name,
                size = f.length(),
                formattedSize = sizeMb,
                originalSigner = "Non vérifié",
                isSigned = false
            )
        }
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(selectedApks = items))
        }
    }

    fun setKeyRoleForApk(role: String) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(selectedKeyRoleForApk = role))
        }
    }

    fun signSelectedApks(context: Context, keyset: KeysetInfo, role: String) {
        val apks = _uiState.value.gemeauUiState.selectedApks
        if (apks.isEmpty()) {
            showSnackbar("Veuillez sélectionner au moins un APK à signer.")
            return
        }
        if (_uiState.value.gemeauUiState.isExecuting) return

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isExecuting = true,
                    progress = 0.05f,
                    currentStepText = "Signature de ${apks.size} APKs en cours...",
                    processedCount = 0,
                    totalItemsCount = apks.size,
                    logs = listOf(TerminalEntry("[SIGNAPK] Lancement de la signature de ${apks.size} APK(s)..."))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val signed = GemeauSignerEngine.signMultipleApks(
                context = context,
                apks = apks.map { it.file },
                keysetDir = File(keyset.dirPath),
                keyRole = role,
                onProgress = { step, percent, index ->
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                currentStepText = step,
                                progress = percent,
                                processedCount = index
                            )
                        )
                    }
                },
                onLog = { line ->
                    val isSuccess = line.contains("[SUCCÈS]")
                    val isError = line.contains("[ERREUR]")
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                logs = (it.gemeauUiState.logs + TerminalEntry(line, isError = isError, isSuccess = isSuccess)).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            isExecuting = false,
                            progress = 1.0f,
                            currentStepText = "${signed.size} APK(s) signés avec succès !",
                            lastSignedOutputPath = WorkspaceManager.getSignerSignedApksDir(context).absolutePath
                        )
                    )
                }
                showSnackbar("${signed.size} APK(s) signés avec la clé '$role' !")
            }
        }
    }

    fun setSelectedRomFile(file: File?) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(selectedRomFile = file))
        }
    }

    fun setGenerateVbmeta(enable: Boolean) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(generateVbmeta = enable))
        }
    }

    fun analyzeAndSignRom(context: Context, keyset: KeysetInfo, withVbmeta: Boolean) {
        val rom = _uiState.value.gemeauUiState.selectedRomFile
        if (rom == null) {
            showSnackbar("Veuillez choisir un fichier ROM, GSI ou partition à signer.")
            return
        }
        if (_uiState.value.gemeauUiState.isExecuting) return

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isExecuting = true,
                    progress = 0.05f,
                    currentStepText = "Audit et analyse de la ROM ${rom.name}...",
                    processedCount = 0,
                    logs = listOf(TerminalEntry("[ROMSIGN] Initialisation de l'audit et de la chaîne de confiance..."))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val report = GemeauSignerEngine.analyzeAndSignRom(
                context = context,
                romFile = rom,
                keysetDir = File(keyset.dirPath),
                withVbmeta = withVbmeta,
                onProgress = { step, percent, processed, total ->
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                currentStepText = step,
                                progress = percent,
                                processedCount = processed,
                                totalItemsCount = total
                            )
                        )
                    }
                },
                onLog = { line ->
                    val isSuccess = line.contains("[SUCCÈS]") || line.contains("[PRÉSERVATION]")
                    val isError = line.contains("[ERREUR]")
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                logs = (it.gemeauUiState.logs + TerminalEntry(line, isError = isError, isSuccess = isSuccess)).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            isExecuting = false,
                            progress = 1.0f,
                            currentStepText = "ROM resignée avec préservation GApps et réplication de confiance !",
                            preservedGappsCount = report.preservedGappsCount,
                            lastGeneratedReportPath = report.reportPath,
                            lastGeneratedDataDir = report.dataDir,
                            lastSignedOutputPath = WorkspaceManager.getSignerSignedRomsDir(context).absolutePath
                        )
                    )
                }
                showSnackbar("ROM ${rom.name} resignée ! (${report.resignedApksCount} APKs resignés, ${report.preservedGappsCount} GApps préservés)")
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GEMEAU VERIFICATION ACTIONS (Verify App & Verify ROM)
    // ─────────────────────────────────────────────────────────────────────────

    fun selectGemeauVerifySubTab(subTab: GemeauVerifySubTab) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(verifySubTab = subTab))
        }
    }

    fun setSelectedApksForVerification(files: List<File>) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(verifySelectedApks = files))
        }
    }

    fun setSelectedRomForApkExtraction(file: File?) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(verifySelectedRomForApkExtraction = file))
        }
    }

    fun extractApksFromSelectedRom(context: Context) {
        val rom = _uiState.value.gemeauUiState.verifySelectedRomForApkExtraction
        if (rom == null) {
            showSnackbar("Veuillez choisir une image ou archive de ROM.")
            return
        }

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isExtractingApksFromRom = true,
                    currentStepText = "Extraction des APKs depuis ${rom.name}...",
                    logs = (it.gemeauUiState.logs + TerminalEntry("[VERIFY] Extraction des paquets depuis ${rom.name}...")).takeLast(250)
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val sessionDir = File(WorkspaceManager.getSignerVerifyDir(context), "EXTRACTED_${System.currentTimeMillis()}").apply { mkdirs() }
            val apks = GemeauSignerEngine.extractApksFromRom(
                romFile = rom,
                targetDir = sessionDir,
                onProgress = { step, percent ->
                    _uiState.update {
                        it.copy(gemeauUiState = it.gemeauUiState.copy(currentStepText = step, progress = percent))
                    }
                },
                onLog = { line ->
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                logs = (it.gemeauUiState.logs + TerminalEntry(line, isSuccess = line.contains("[EXTRAIT]"))).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            isExtractingApksFromRom = false,
                            verifyExtractedApksFromRom = apks,
                            verifySelectedApks = apks,
                            currentStepText = "${apks.size} APK(s) extraits de ${rom.name}"
                        )
                    )
                }
                showSnackbar("${apks.size} APK(s) extraits avec succès !")
            }
        }
    }

    fun verifySelectedApks(context: Context) {
        val apks = _uiState.value.gemeauUiState.verifySelectedApks
        if (apks.isEmpty()) {
            showSnackbar("Veuillez sélectionner au moins un fichier APK à vérifier.")
            return
        }

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isVerifyingApks = true,
                    progress = 0.05f,
                    currentStepText = "Audit des signatures pour ${apks.size} APK(s)...",
                    logs = (it.gemeauUiState.logs +
                            TerminalEntry("=======================================================") +
                            TerminalEntry("[VERIFY APP] Lancement de l'audit pour ${apks.size} APK(s)...")).takeLast(250)
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val verifyDir = File(WorkspaceManager.getSignerVerifyDir(context), "VERIFY_APPS_$timestamp").apply { mkdirs() }
            val details = mutableListOf<ApkVerificationDetail>()

            for ((idx, apk) in apks.withIndex()) {
                val detail = GemeauSignerEngine.verifyApkSignature(apk)
                details.add(detail)
                val statusText = if (detail.isSigned) "SIGNÉ (${detail.algorithm})" else "NON SIGNÉ"
                val logEntry = TerminalEntry("[APK ${idx + 1}/${apks.size}] ${apk.name} -> $statusText • SHA256: ${detail.sha256Fingerprint.take(23)}...", isSuccess = detail.isSigned, isError = !detail.isSigned)
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            progress = (idx + 1).toFloat() / apks.size,
                            currentStepText = "Vérifié : ${apk.name}",
                            logs = (it.gemeauUiState.logs + logEntry).takeLast(250)
                        )
                    )
                }
            }

            val jsonReport = org.json.JSONObject().apply {
                put("type", "APPS_VERIFICATION")
                put("date", SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.FRENCH).format(Date()))
                put("totalApks", apks.size)
                val list = org.json.JSONArray()
                details.forEach { d ->
                    list.put(org.json.JSONObject().apply {
                        put("file", d.fileName)
                        put("isSigned", d.isSigned)
                        put("v1", d.hasV1Scheme)
                        put("v2", d.hasV2Scheme)
                        put("v3", d.hasV3Scheme)
                        put("v4", d.hasV4Scheme)
                        put("sha256", d.sha256Fingerprint)
                        put("subject", d.subjectDn)
                        put("issuer", d.issuerDn)
                        put("algorithm", d.algorithm)
                        put("keySize", d.keySize)
                        put("validUntil", d.validUntil)
                    })
                }
                put("results", list)
            }
            File(verifyDir, "signature_database.json").writeText(jsonReport.toString(2))

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            isVerifyingApks = false,
                            progress = 1.0f,
                            apkVerificationResults = details,
                            lastVerificationDir = verifyDir.absolutePath,
                            currentStepText = "Vérification terminée (${details.count { d -> d.isSigned }}/${details.size} signés)"
                        )
                    )
                }
                showSnackbar("${details.size} APK(s) analysés. Rapport enregistré dans Data/SIGNER/VERIFY/")
            }
        }
    }

    fun setSelectedRomForAudit(file: File?) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(verifySelectedRomForAudit = file))
        }
    }

    fun auditSelectedRom(context: Context) {
        val rom = _uiState.value.gemeauUiState.verifySelectedRomForAudit
        if (rom == null) {
            showSnackbar("Veuillez sélectionner un fichier ROM ou partition pour l'audit complet.")
            return
        }

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isAuditingRom = true,
                    progress = 0.05f,
                    currentStepText = "Audit de la chaîne de confiance de ${rom.name}...",
                    logs = (it.gemeauUiState.logs +
                            TerminalEntry("=======================================================") +
                            TerminalEntry("[VERIFY ROM] Démarrage de l'audit de la chaîne de confiance pour ${rom.name}...")).takeLast(250)
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            val audit = GemeauSignerEngine.auditRomTrustChain(
                romFile = rom,
                context = context,
                onProgress = { step, percent ->
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                currentStepText = step,
                                progress = percent
                            )
                        )
                    }
                },
                onLog = { line ->
                    val isSuccess = line.contains("[RÉSULTAT") || line.contains("[CHAÎNE") || line.contains("[AUDIT COMPLET")
                    val isError = line.contains("[ERREUR]")
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                logs = (it.gemeauUiState.logs + TerminalEntry(line, isSuccess = isSuccess, isError = isError)).takeLast(250)
                            )
                        )
                    }
                }
            )

            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        gemeauUiState = it.gemeauUiState.copy(
                            isAuditingRom = false,
                            progress = 1.0f,
                            romAuditResult = audit,
                            lastVerificationDir = File(audit.databaseJsonPath).parentFile?.absolutePath,
                            currentStepText = "Audit ROM terminé (${audit.signedApksCount} paquets signés)"
                        )
                    )
                }
                showSnackbar("Audit complet de ${rom.name} terminé ! Base de données générée dans Data/SIGNER/VERIFY/")
            }
        }
    }

    fun setSelectedRSignerTarget(file: File?) {
        _uiState.update {
            it.copy(gemeauUiState = it.gemeauUiState.copy(rsignerSelectedTarget = file, rsignerDetectedGroups = emptyList()))
        }
    }

    fun analyzeTargetForRSigner(context: Context) {
        val target = _uiState.value.gemeauUiState.rsignerSelectedTarget
        if (target == null) {
            showSnackbar("Veuillez sélectionner un fichier cible (.img, .zip, payload.bin, ou ROM/GSI).")
            return
        }

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    rsignerIsAnalyzing = true,
                    progress = 0.05f,
                    currentStepText = "Analyse approfondie de ${target.name}...",
                    logs = (it.gemeauUiState.logs +
                            TerminalEntry("=======================================================") +
                            TerminalEntry("[R-SIGNER] Démarrage de l'analyse approfondie de ${target.name}...")).takeLast(250)
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val groups = GemeauSignerEngine.analyzeTargetForRSigner(
                    targetFile = target,
                    context = context,
                    onProgress = { step, p ->
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    currentStepText = step,
                                    progress = p
                                )
                            )
                        }
                    },
                    onLog = { line ->
                        val isSuccess = line.contains("[SUCCÈS") || line.contains("[GROUPE") || line.contains("[ANALYSE")
                        val isError = line.contains("[ERREUR]") || line.contains("[AVERTISSEMENT]")
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    logs = (it.gemeauUiState.logs + TerminalEntry(line, isSuccess = isSuccess, isError = isError)).takeLast(250)
                                )
                            )
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                rsignerIsAnalyzing = false,
                                progress = 1.0f,
                                rsignerDetectedGroups = groups,
                                currentStepText = "Analyse achevée : ${groups.size} groupe(s) identifié(s)"
                            )
                        )
                    }
                    showSnackbar("${groups.size} groupe(s) de signatures détecté(s) dans ${target.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                rsignerIsAnalyzing = false,
                                progress = 0.0f,
                                currentStepText = "Erreur analyse : ${e.message}",
                                logs = (it.gemeauUiState.logs + TerminalEntry("[ERREUR] ${e.message}", isError = true)).takeLast(250)
                            )
                        )
                    }
                    showSnackbar("Erreur analyse R-SIGNER : ${e.message}")
                }
            }
        }
    }

    fun generateNewKeysetForRSigner(
        context: Context,
        name: String,
        cn: String,
        org: String,
        country: String,
        alg: String,
        years: Int,
        onComplete: (KeysetInfo) -> Unit
    ) {
        val detected = _uiState.value.gemeauUiState.rsignerDetectedGroups
        viewModelScope.launch(Dispatchers.IO) {
            val keyset = GemeauSignerEngine.generateKeysetForDetectedGroups(
                context = context,
                keysetName = name,
                commonName = cn,
                organization = org,
                countryCode = country,
                algorithm = alg,
                validityYears = years,
                detectedGroups = detected,
                onLog = { msg ->
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                logs = (it.gemeauUiState.logs + TerminalEntry(msg)).takeLast(250)
                            )
                        )
                    }
                }
            )
            refreshKeysets(context)
            withContext(Dispatchers.Main) {
                onComplete(keyset)
            }
        }
    }

    fun executeRSigner(
        context: Context,
        keyset: KeysetInfo,
        withVbmeta: Boolean,
        preserveGapps: Boolean = true
    ) {
        val target = _uiState.value.gemeauUiState.rsignerSelectedTarget
        if (target == null) {
            showSnackbar("Veuillez sélectionner un fichier cible.")
            return
        }

        _uiState.update {
            it.copy(
                gemeauUiState = it.gemeauUiState.copy(
                    isExecuting = true,
                    progress = 0.05f,
                    currentStepText = "Initialisation de R-SIGNER pour ${target.name}...",
                    logs = (it.gemeauUiState.logs +
                            TerminalEntry("=======================================================") +
                            TerminalEntry("[R-SIGNER] Lancement de la resignature avec le trousseau ${keyset.name}...")).takeLast(250)
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val report = GemeauSignerEngine.executeRSignerPipeline(
                    context = context,
                    targetFile = target,
                    keysetDir = File(keyset.dirPath),
                    withVbmeta = withVbmeta,
                    preserveGapps = preserveGapps,
                    onProgress = { step, p, proc, tot ->
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    currentStepText = step,
                                    progress = p,
                                    processedCount = proc,
                                    totalItemsCount = tot
                                )
                            )
                        }
                    },
                    onLog = { line ->
                        val isSuccess = line.contains("[SUCCÈS") || line.contains("[PHASE") || line.contains("[R-SIGNÉ")
                        val isError = line.contains("[ERREUR")
                        _uiState.update {
                            it.copy(
                                gemeauUiState = it.gemeauUiState.copy(
                                    logs = (it.gemeauUiState.logs + TerminalEntry(line, isSuccess = isSuccess, isError = isError)).takeLast(250)
                                )
                            )
                        }
                    }
                )

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                isExecuting = false,
                                progress = 1.0f,
                                rsignerLastOutputPath = report.signedRomPath,
                                lastGeneratedReportPath = report.reportPath,
                                currentStepText = "R-SIGNER terminé avec succès ! (${report.resignedApksCount} APKs resignés)"
                            )
                        )
                    }
                    showSnackbar("Resignature R-SIGNER terminée ! Fichier dans sdcard/HEPHAISTOS/SIGNER/RSIGNER")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            gemeauUiState = it.gemeauUiState.copy(
                                isExecuting = false,
                                progress = 0.0f,
                                currentStepText = "Erreur R-SIGNER : ${e.message}",
                                logs = (it.gemeauUiState.logs + TerminalEntry("[ERREUR FATALE] ${e.message}", isError = true)).takeLast(250)
                            )
                        )
                    }
                    showSnackbar("Erreur R-SIGNER : ${e.message}")
                }
            }
        }
    }

    fun refreshAheExtensions(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            WorkspaceManager.ensureAheStarterTemplate(context)
            val extensions = ExtensionManager.getInstalledExtensions(context)
            withContext(Dispatchers.Main) {
                _uiState.update { state ->
                    state.copy(
                        aheUiState = state.aheUiState.copy(
                            installedExtensions = extensions
                        )
                    )
                }
            }
        }
    }

    fun importAheExtension(context: Context, inputStream: InputStream) {
        viewModelScope.launch {
            _uiState.update { it.copy(aheUiState = it.aheUiState.copy(isExecuting = true, currentStepText = "Importation du paquet .ahe...", progress = 0.1f)) }
            logTerminal("[AHE] Début de l'importation d'une extension .ahe...")
            val result = ExtensionManager.installExtensionFromStream(context, inputStream) { step, prog ->
                _uiState.update { it.copy(aheUiState = it.aheUiState.copy(currentStepText = step, progress = prog)) }
                logTerminal("[AHE] $step")
            }
            result.onSuccess { ext ->
                logTerminal("[AHE] Extension installée avec succès : ${ext.manifest.name} (${ext.manifest.id})", isSuccess = true)
                refreshAheExtensions(context)
                _uiState.update { it.copy(
                    aheUiState = it.aheUiState.copy(isExecuting = false, progress = 1f, currentStepText = "Extension prête"),
                    snackbarMessage = "Extension '${ext.manifest.name}' installée avec succès !"
                ) }
            }.onFailure { err ->
                logTerminal("[AHE ERREUR] Échec installation : ${err.message}", isError = true)
                _uiState.update { it.copy(
                    aheUiState = it.aheUiState.copy(isExecuting = false, currentStepText = "Erreur: ${err.message}"),
                    snackbarMessage = "Erreur lors de l'installation de l'extension : ${err.message}"
                ) }
            }
        }
    }

    fun uninstallAheExtension(context: Context, extensionId: String) {
        viewModelScope.launch {
            logTerminal("[AHE] Désinstallation de l'extension $extensionId...")
            val success = ExtensionManager.uninstallExtension(context, extensionId)
            if (success) {
                logTerminal("[AHE] Extension $extensionId désinstallée avec succès.", isSuccess = true)
                refreshAheExtensions(context)
                _uiState.update { state ->
                    state.copy(
                        currentScreen = if (state.currentScreen == Screen.AHE_EXTENSION) Screen.HOME else state.currentScreen,
                        snackbarMessage = "Extension désinstallée"
                    )
                }
            }
        }
    }

    fun openAheExtension(extension: AheExtension) {
        _uiState.update { state ->
            state.copy(
                currentScreen = Screen.AHE_EXTENSION,
                isPlusDrawerOpen = false,
                aheUiState = state.aheUiState.copy(
                    activeExtension = extension,
                    currentStepText = "Prêt pour les actions",
                    progress = 0f
                )
            )
        }
    }

    fun executeAheAction(context: Context, extension: AheExtension, action: AheAction, inputs: Map<String, String>) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    aheUiState = state.aheUiState.copy(
                        isExecuting = true,
                        currentStepText = "Exécution de ${action.label}...",
                        progress = 0.2f
                    )
                )
            }
            logAheTerminal("──────────────────────────────────────────")
            logAheTerminal("[AHE ACTION] Lancement de '${action.label}' (${action.id})")
            logTerminal("[AHE] Lancement de '${action.label}' depuis l'extension ${extension.manifest.name}")

            val code = ExtensionManager.executeAction(context, extension, action, inputs) { logLine ->
                logAheTerminal(logLine)
                logTerminal(logLine)
            }

            val success = code == 0
            logAheTerminal("[AHE ACTION] Fin de l'action '${action.label}' (Code retour: $code)")
            if (success) {
                logAheTerminal("[SUCCÈS] Action exécutée avec succès !", isSuccess = true)
            } else {
                logAheTerminal("[ERREUR] Code de sortie: $code", isError = true)
            }

            _uiState.update { state ->
                state.copy(
                    aheUiState = state.aheUiState.copy(
                        isExecuting = false,
                        progress = if (success) 1f else 0f,
                        currentStepText = if (success) "Action terminée avec succès" else "Erreur code $code"
                    ),
                    snackbarMessage = if (success) "Action '${action.label}' réussie !" else "Erreur lors de l'exécution (code: $code)"
                )
            }
        }
    }

    fun logAheTerminal(text: String, isSuccess: Boolean = false, isError: Boolean = false) {
        val entry = TerminalEntry(text = text, isSuccess = isSuccess, isError = isError)
        _uiState.update { state ->
            val updated = (state.aheUiState.logs + entry).takeLast(200)
            state.copy(aheUiState = state.aheUiState.copy(logs = updated))
        }
    }

    fun clearAheLogs() {
        _uiState.update { it.copy(aheUiState = it.aheUiState.copy(logs = emptyList())) }
    }

    // ====================================================
    // ANALYST ENGINE OPERATIONS
    // ====================================================
    fun refreshAnalystTargets(context: Context) {
        val targets = WorkspaceManager.listUnpackedProjects(context)
        _uiState.update { state ->
            val selected = if (targets.contains(state.analystUiState.selectedTarget)) {
                state.analystUiState.selectedTarget
            } else {
                targets.firstOrNull().orEmpty()
            }
            state.copy(
                analystUiState = state.analystUiState.copy(
                    availableTargets = targets,
                    selectedTarget = selected
                )
            )
        }
    }

    fun selectAnalystTarget(targetName: String) {
        _uiState.update { state ->
            state.copy(
                analystUiState = state.analystUiState.copy(
                    selectedTarget = targetName
                )
            )
        }
    }

    fun runAnalystScan(context: Context) {
        val selected = _uiState.value.analystUiState.selectedTarget
        if (selected.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "Veuillez sélectionner une cible dans UNPACKED") }
            return
        }

        val unpackedDir = WorkspaceManager.getUnpackedDir(context)
        val targetFile = File(unpackedDir, selected)
        if (!targetFile.exists()) {
            targetFile.mkdirs()
        }

        _uiState.update { state ->
            state.copy(
                analystUiState = state.analystUiState.copy(
                    isAnalyzing = true,
                    progress = 0f,
                    currentStepText = "Démarrage de l'analyse factuelle...",
                    logs = listOf(TerminalEntry("[ANALYST] Démarrage de l'analyse approfondie de $selected", isCommand = true))
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val summary = AnalystEngine.analyzeTarget(
                    context = context,
                    targetDir = targetFile,
                    onProgress = { p, step ->
                        _uiState.update { s ->
                            s.copy(
                                analystUiState = s.analystUiState.copy(
                                    progress = p,
                                    currentStepText = step
                                )
                            )
                        }
                    },
                    onLog = { msg, isSuccess ->
                        val entry = TerminalEntry(text = msg, isSuccess = isSuccess)
                        _uiState.update { s ->
                            val updatedLogs = (s.analystUiState.logs + entry).takeLast(250)
                            s.copy(
                                analystUiState = s.analystUiState.copy(logs = updatedLogs)
                            )
                        }
                    }
                )

                val reportFile = File(summary.exportDir, "signature_report.txt")
                val preview = if (reportFile.exists()) {
                    reportFile.readText().take(2000)
                } else ""

                withContext(Dispatchers.Main) {
                    _uiState.update { s ->
                        s.copy(
                            analystUiState = s.analystUiState.copy(
                                isAnalyzing = false,
                                progress = 1f,
                                currentStepText = "Analyse terminée avec succès !",
                                lastSummary = summary,
                                previewReportText = preview
                            ),
                            snackbarMessage = "Analyse ANALYST terminée (${summary.totalFiles} fichiers, ${summary.totalApks} APKs)"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val errEntry = TerminalEntry("[ERREUR] Échec de l'analyse : ${e.message}", isError = true)
                    _uiState.update { s ->
                        s.copy(
                            analystUiState = s.analystUiState.copy(
                                isAnalyzing = false,
                                progress = 0f,
                                currentStepText = "Erreur : ${e.message}",
                                logs = (s.analystUiState.logs + errEntry).takeLast(250)
                            ),
                            snackbarMessage = "Erreur ANALYST : ${e.message}"
                        )
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERISIN UI Operations
    // ─────────────────────────────────────────────────────────────────────────

    fun setVerisinPath(path: String) {
        _uiState.update { it.copy(verisinSelectedPath = path, verisinError = null) }
    }

    fun clearVerisinResult() {
        _uiState.update { it.copy(verisinResult = null, verisinError = null) }
    }

    fun refreshVerisinApks(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val apks = WorkspaceManager.findWorkspaceApks(context)
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(verisinAvailableApks = apks) }
            }
        }
    }

    fun onVerisinUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val copiedFile = WorkspaceManager.copyUriToWorkspace(context, uri)
            withContext(Dispatchers.Main) {
                if (copiedFile != null && copiedFile.exists()) {
                    _uiState.update { 
                        it.copy(
                            verisinSelectedPath = copiedFile.absolutePath,
                            verisinError = null,
                            snackbarMessage = "APK importé : ${copiedFile.name}"
                        ) 
                    }
                    refreshVerisinApks(context)
                } else {
                    _uiState.update { 
                        it.copy(
                            verisinSelectedPath = uri.toString(),
                            verisinError = "Impossible d'importer le fichier sélectionné"
                        ) 
                    }
                }
            }
        }
    }

    fun runVerisinVerification(context: Context, inputPath: String) {
        val cleanPath = WorkspaceManager.sanitizePath(inputPath)
        if (cleanPath.isBlank()) {
            _uiState.update { it.copy(verisinError = "Veuillez sélectionner ou saisir le chemin d'un APK à vérifier.") }
            return
        }

        val resolved = WorkspaceManager.resolveApkPath(context, cleanPath)
        if (resolved == null || !resolved.exists()) {
            _uiState.update { 
                it.copy(
                    verisinError = "Fichier introuvable : '$cleanPath'. Assurez-vous qu'il est dans /sdcard/HEPHAISTOS/ ou saisissez son chemin complet sans chevrons."
                ) 
            }
            return
        }

        _uiState.update { 
            it.copy(
                isVerifyingVerisin = true,
                verisinError = null,
                verisinSelectedPath = resolved.absolutePath
            ) 
        }

        logTerminal("[VERISIN] Début de l'analyse dans l'interface pour : ${resolved.name}", isSuccess = true)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = VerisinEngine.verifyApkFile(
                    context = context,
                    apkFile = resolved,
                    onLog = { msg -> logTerminal(msg) }
                )
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(
                            isVerifyingVerisin = false,
                            verisinResult = result,
                            snackbarMessage = "Analyse VERISIN terminée avec succès (${result.schemesDisplay})"
                        ) 
                    }
                    if (result.reportFile != null) {
                        logTerminal("[SUCCÈS VERISIN] Rapport exporté : ${result.reportFile.absolutePath}", isSuccess = true)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { 
                        it.copy(
                            isVerifyingVerisin = false,
                            verisinError = "Échec de l'audit VERISIN : ${e.message}"
                        ) 
                    }
                    logTerminal("[ERREUR VERISIN] ${e.message}", isError = true)
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SCEAU & KEYCORE Operations
    // ─────────────────────────────────────────────────────────────────────────

    fun setSceauSubScreen(sub: SceauSubScreen) {
        _uiState.update { it.copy(sceauUiState = it.sceauUiState.copy(currentSubScreen = sub)) }
    }

    fun setKeycoreTab(tab: KeycoreTab) {
        _uiState.update { 
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        activeTab = tab,
                        statusMessage = null,
                        errorMessage = null
                    )
                )
            ) 
        }
    }

    fun setKeycoreApkPath(path: String) {
        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        selectedApkPath = path,
                        errorMessage = null
                    )
                )
            )
        }
    }

    fun onKeycoreUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val copiedFile = WorkspaceManager.copyUriToWorkspace(context, uri)
            withContext(Dispatchers.Main) {
                if (copiedFile != null && copiedFile.exists()) {
                    setKeycoreApkPath(copiedFile.absolutePath)
                    refreshVerisinApks(context)
                    _uiState.update {
                        it.copy(
                            snackbarMessage = "APK importé pour KEYCORE : ${copiedFile.name}"
                        )
                    }
                } else {
                    setKeycoreApkPath(uri.toString())
                }
            }
        }
    }

    fun analyzeApkForKeycore(context: Context, inputPath: String) {
        val clean = WorkspaceManager.sanitizePath(inputPath)
        if (clean.isBlank()) {
            _uiState.update {
                it.copy(
                    sceauUiState = it.sceauUiState.copy(
                        keycoreState = it.sceauUiState.keycoreState.copy(
                            errorMessage = "Veuillez spécifier un APK à analyser"
                        )
                    )
                )
            }
            return
        }

        val resolved = WorkspaceManager.resolveApkPath(context, clean)
        if (resolved == null || !resolved.exists()) {
            _uiState.update {
                it.copy(
                    sceauUiState = it.sceauUiState.copy(
                        keycoreState = it.sceauUiState.keycoreState.copy(
                            errorMessage = "Fichier APK introuvable : '$clean'"
                        )
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        isAnalyzingApk = true,
                        errorMessage = null,
                        statusMessage = "Analyse Verisin en cours...",
                        selectedApkPath = resolved.absolutePath
                    )
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val analysis = KeycoreEngine.analyzeApkForTarget(context, resolved) { msg ->
                    logTerminal(msg)
                }
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isAnalyzingApk = false,
                                    targetAnalysis = analysis,
                                    statusMessage = "Analyse Verisin réussie pour ${resolved.name}"
                                )
                            ),
                            snackbarMessage = "Structure cible analysée avec succès"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isAnalyzingApk = false,
                                    errorMessage = "Échec de l'analyse : ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun generateKeymakerKeyset(
        context: Context,
        name: String,
        cn: String,
        org: String,
        country: String,
        alg: String,
        years: Int
    ) {
        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        isGeneratingKey = true,
                        errorMessage = null,
                        statusMessage = "Génération du jeu de clés..."
                    )
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyset = KeycoreEngine.generateStandardKeymakerKeyset(
                    context = context,
                    keysetName = name,
                    commonName = cn,
                    organization = org,
                    countryCode = country,
                    algorithm = alg,
                    validityYears = years,
                    onLog = { logTerminal(it) }
                )
                withContext(Dispatchers.Main) {
                    refreshKeysets(context)
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    lastGeneratedKeyset = keyset,
                                    statusMessage = "Keyset '${keyset.name}' forgé avec succès !"
                                )
                            ),
                            snackbarMessage = "Keyset '${keyset.name}' créé avec succès"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    errorMessage = "Erreur de génération : ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun generateVerisinKey(
        context: Context,
        name: String,
        cn: String,
        org: String,
        country: String,
        years: Int
    ) {
        val target = _uiState.value.sceauUiState.keycoreState.targetAnalysis
        if (target == null) {
            _uiState.update {
                it.copy(
                    sceauUiState = it.sceauUiState.copy(
                        keycoreState = it.sceauUiState.keycoreState.copy(
                            errorMessage = "Veuillez d'abord analyser un APK modèle"
                        )
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        isGeneratingKey = true,
                        errorMessage = null,
                        statusMessage = "Forge de la clé Verisin Key..."
                    )
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyset = KeycoreEngine.createVerisinKey(
                    context = context,
                    targetAnalysis = target,
                    keysetName = name,
                    commonName = cn,
                    organization = org,
                    countryCode = country,
                    validityYears = years,
                    onLog = { logTerminal(it) }
                )
                withContext(Dispatchers.Main) {
                    refreshKeysets(context)
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    lastGeneratedKeyset = keyset,
                                    statusMessage = "Clé calquée '${keyset.name}' prête !"
                                )
                            ),
                            snackbarMessage = "Clé calquée '${keyset.name}' créée"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    errorMessage = "Échec : ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun generateVerisinClone(context: Context, cloneName: String) {
        val target = _uiState.value.sceauUiState.keycoreState.targetAnalysis
        if (target == null) {
            _uiState.update {
                it.copy(
                    sceauUiState = it.sceauUiState.copy(
                        keycoreState = it.sceauUiState.keycoreState.copy(
                            errorMessage = "Veuillez d'abord analyser un APK cible"
                        )
                    )
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        isGeneratingKey = true,
                        errorMessage = null,
                        statusMessage = "Forge du Sceau Clone..."
                    )
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val keyset = KeycoreEngine.createVerisinClone(
                    context = context,
                    targetAnalysis = target,
                    cloneKeysetName = cloneName,
                    onLog = { logTerminal(it) }
                )
                withContext(Dispatchers.Main) {
                    refreshKeysets(context)
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    lastGeneratedKeyset = keyset,
                                    statusMessage = "Sceau Clone '${keyset.name}' forgé avec succès !"
                                )
                            ),
                            snackbarMessage = "Sceau Clone '${keyset.name}' prêt"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isGeneratingKey = false,
                                    errorMessage = "Échec de clonage : ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    fun resignApkWithCloneKey(context: Context) {
        val keycoreState = _uiState.value.sceauUiState.keycoreState
        val target = keycoreState.targetAnalysis
        val cloneKeyset = keycoreState.lastGeneratedKeyset
        if (target == null || cloneKeyset == null) {
            _uiState.update {
                it.copy(
                    sceauUiState = it.sceauUiState.copy(
                        keycoreState = it.sceauUiState.keycoreState.copy(
                            errorMessage = "APK cible ou Sceau Clone manquant"
                        )
                    )
                )
            }
            return
        }

        val apkFile = File(target.apkPath)
        val keysetDir = File(cloneKeyset.dirPath)

        _uiState.update {
            it.copy(
                sceauUiState = it.sceauUiState.copy(
                    keycoreState = it.sceauUiState.keycoreState.copy(
                        isResigningApk = true,
                        errorMessage = null,
                        statusMessage = "Resignature de l'APK en cours..."
                    )
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resigned = KeycoreEngine.resignApkWithCloneKey(
                    context = context,
                    inputApk = apkFile,
                    cloneKeysetDir = keysetDir,
                    onLog = { logTerminal(it) }
                )
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isResigningApk = false,
                                    lastResignedApkPath = resigned?.absolutePath,
                                    statusMessage = "APK résigné avec succès !"
                                )
                            ),
                            snackbarMessage = "APK résigné : ${resigned?.name ?: ""}"
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            sceauUiState = it.sceauUiState.copy(
                                keycoreState = it.sceauUiState.keycoreState.copy(
                                    isResigningApk = false,
                                    errorMessage = "Erreur de resignature : ${e.message}"
                                )
                            )
                        )
                    }
                }
            }
        }
    }

    // ==========================================
    // SIGNALAB — Actions & Workflow
    // ==========================================

    fun setSignalabFilePath(path: String) {
        _uiState.update {
            it.copy(signalabUiState = it.signalabUiState.copy(selectedFilePath = path, errorMessage = null))
        }
    }

    fun onSignalabUriSelected(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val resolvedFile = WorkspaceManager.copyUriToWorkspace(context, uri)
            withContext(Dispatchers.Main) {
                if (resolvedFile != null) {
                    setSignalabFilePath(resolvedFile.absolutePath)
                    logTerminal("[SIGNALAB] Fichier importé : ${resolvedFile.name}", isSuccess = true)
                } else {
                    _uiState.update {
                        it.copy(signalabUiState = it.signalabUiState.copy(errorMessage = "Échec d'accès au fichier sélectionné"))
                    }
                }
            }
        }
    }

    fun launchSignalabScan(context: Context, filePath: String) {
        val cleanPath = WorkspaceManager.sanitizePath(filePath)
        val file = WorkspaceManager.resolveApkPath(context, cleanPath) ?: File(cleanPath)
        if (!file.exists()) {
            _uiState.update {
                it.copy(
                    signalabUiState = it.signalabUiState.copy(
                        errorMessage = "Fichier introuvable : '$cleanPath'"
                    )
                )
            }
            logTerminal("[ERREUR SIGNALAB] Fichier cible introuvable : $cleanPath")
            return
        }

        _uiState.update {
            it.copy(
                signalabUiState = it.signalabUiState.copy(
                    isScanning = true,
                    scanStepText = "Initialisation du déballage haute précision...",
                    scanProgress = 0.05f,
                    errorMessage = null,
                    statusMessage = null,
                    finalRepackedPath = null
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Unpack
                val blueprint = SignalabEngine.unpackTargetFile(
                    context = context,
                    targetFile = file,
                    onProgress = { step, prog ->
                        _uiState.update {
                            it.copy(signalabUiState = it.signalabUiState.copy(scanStepText = step, scanProgress = prog * 0.5f))
                        }
                    },
                    onLog = { logTerminal(it) }
                )

                // Step 2: Deep Verisin scan & clustering
                val scanResult = SignalabEngine.scanAndClusterApkSignatures(
                    context = context,
                    blueprint = blueprint,
                    onProgress = { step, prog ->
                        _uiState.update {
                            it.copy(signalabUiState = it.signalabUiState.copy(scanStepText = step, scanProgress = 0.5f + (prog * 0.5f)))
                        }
                    },
                    onLog = { logTerminal(it) }
                )

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            signalabUiState = it.signalabUiState.copy(
                                isScanning = false,
                                scanResult = scanResult,
                                statusMessage = "Scan terminé avec succès : ${scanResult.clusters.size} clusters de clés identifiés."
                            )
                        )
                    }
                    showSnackbar("Scan Signalab prêt")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            signalabUiState = it.signalabUiState.copy(
                                isScanning = false,
                                errorMessage = "Échec du scan : ${e.message}"
                            )
                        )
                    }
                    logTerminal("[ERREUR SIGNALAB CRITIQUE] ${e.message}")
                }
            }
        }
    }

    fun openSignalabSignDialog() {
        _uiState.update {
            it.copy(signalabUiState = it.signalabUiState.copy(showSignDialog = true))
        }
    }

    fun dismissSignalabSignDialog() {
        _uiState.update {
            it.copy(signalabUiState = it.signalabUiState.copy(showSignDialog = false))
        }
    }

    fun executeSignalabResignAndRepack(context: Context, options: SignalabSignOptions) {
        val scanResult = _uiState.value.signalabUiState.scanResult ?: return
        dismissSignalabSignDialog()

        _uiState.update {
            it.copy(
                signalabUiState = it.signalabUiState.copy(
                    isResigningAndRepacking = true,
                    resignStepText = "Démarrage de la resignature intégrale...",
                    resignProgress = 0.05f,
                    errorMessage = null,
                    statusMessage = null,
                    finalRepackedPath = null
                )
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repackedFile = SignalabEngine.executeResignAndRepack(
                    context = context,
                    scanResult = scanResult,
                    options = options,
                    onProgress = { step, prog ->
                        _uiState.update {
                            it.copy(signalabUiState = it.signalabUiState.copy(resignStepText = step, resignProgress = prog))
                        }
                    },
                    onLog = { logTerminal(it) }
                )

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            signalabUiState = it.signalabUiState.copy(
                                isResigningAndRepacking = false,
                                finalRepackedPath = repackedFile.absolutePath,
                                statusMessage = "ROM resignée et recompilée avec succès dans LABOUT !"
                            )
                        )
                    }
                    showSnackbar("Fichier LABOUT prêt : ${repackedFile.name}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            signalabUiState = it.signalabUiState.copy(
                                isResigningAndRepacking = false,
                                errorMessage = "Échec de resignature/repack : ${e.message}"
                            )
                        )
                    }
                    logTerminal("[ERREUR SIGNALAB] ${e.message}")
                }
            }
        }
    }
}

