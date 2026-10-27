package com.example.model

enum class Screen(val title: String) {
    HOME("Accueil"),
    UNPACK_REPACK("Unpack / Repack"),
    EXTRACTOR("Extractor"),
    GEMEAU("Géméau"),
    ANALYST("Analyst"),
    VERISIN("Verisin"),
    AHE_EXTENSION("Extension .ahe"),
    SETTINGS("Paramètres")
}

enum class RailState {
    HIDDEN,
    COMPACT,
    EXPANDED
}

enum class ThemeMode(val label: String, val description: String) {
    LIGHT("Clair", "Interface lumineuse et contrastée"),
    DARK("Sombre", "Confort visuel et économie d'énergie"),
    SYSTEM("Système", "S'adapte automatiquement aux réglages de l'appareil")
}

enum class ColorStyle(
    val title: String,
    val description: String,
    val primaryHex: Long,
    val secondaryHex: Long
) {
    FORGE("Forge Héphaïstos", "Ambre doré, cuivre & feu d'artisan", 0xFFE67E22, 0xFFD35400),
    COBALT("Cobalt Industriel", "Bleu saphir technique & moderne", 0xFF005FB0, 0xFF004786),
    EMERALD("Émeraude Cyber", "Vert forêt technologique & vital", 0xFF006C4C, 0xFF005138),
    TERRACOTTA("Terracotta Cuivrée", "Terre cuite royale & chaleur brute", 0xFFA43C2D, 0xFF832619),
    AMETHYST("Améthyste Profonde", "Violet impérial & cybernétique", 0xFF6E4EA1, 0xFF563687)
}

enum class SettingsTab(val title: String) {
    THEME("Thème"),
    PERMISSIONS("Autorisations & Root"),
    KITCHEN_TOOLS("Boîte à Outils"),
    ADDON_TEMPLATE("Templates Satellites & PDF"),
    SKELETON_INFO("Squelette & Infos")
}

enum class AppType {
    NATIVE_ADDON,
    WEBAPP,
    SHELL_SCRIPT,
    KITCHEN_CORE
}

data class CompatibleApp(
    val id: String,
    val name: String,
    val packageName: String,
    val version: String,
    val description: String,
    val category: String,
    val isHero: Boolean = false,
    val isInstalled: Boolean = true,
    val isHeavyTask: Boolean = false,
    val status: String = "Prêt",
    val statusColor: Long = 0xFF4CAF50,
    val iconKey: String = "forge",
    val appType: AppType = AppType.NATIVE_ADDON,
    val webAppUrl: String? = null,
    val scriptPath: String? = null,
    val capabilities: List<String> = emptyList()
)

data class KitchenTool(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val supportedFormats: List<String>,
    val requiresRoot: Boolean = false,
    val isReady: Boolean = true,
    val commandSyntax: String
)

data class WebAppPackage(
    val id: String,
    val name: String,
    val directoryPath: String,
    val entryPoint: String,
    val iconKey: String = "web",
    val isZip: Boolean = false
)

data class HephaistScript(
    val id: String,
    val name: String,
    val filePath: String,
    val interpreter: String, // "sh" or "python"
    val isRootRequired: Boolean = false
)

data class RootStatus(
    val isRooted: Boolean,
    val statusText: String,
    val binaryPath: String?,
    val details: List<String>,
    val delegatedPackages: List<String> = emptyList()
)

data class ChangelogVersion(
    val version: String,
    val releaseDate: String,
    val isCurrent: Boolean = false,
    val changes: List<String>
)

data class TerminalEntry(
    val text: String,
    val isCommand: Boolean = false,
    val isError: Boolean = false,
    val isSuccess: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class HeavyTaskDialogData(
    val app: CompatibleApp,
    val taskName: String
)

enum class ExtractionMode(val title: String, val description: String) {
    FULL_DEVICE("Appareil Actuel", "Blobs, HALs, Framework & Base de données factuelle"),
    UNPACKED_ANALYSIS("Analyse GSI / ROM", "Analyse croisée d'un projet déballé dans UNPACKED")
}

data class ExtractorUiState(
    val isExecuting: Boolean = false,
    val currentMode: ExtractionMode = ExtractionMode.FULL_DEVICE,
    val selectedUnpackedProject: String = "",
    val availableUnpackedProjects: List<String> = emptyList(),
    val progress: Float = 0f,
    val currentStepText: String = "Prêt pour l'extraction",
    val extractedFilesCount: Int = 0,
    val extractedMb: Double = 0.0,
    val logs: List<TerminalEntry> = emptyList(),
    val lastGeneratedReportPath: String? = null,
    val lastGeneratedDataDir: String? = null
)

data class KeysetInfo(
    val name: String,
    val dirPath: String,
    val keyCount: Int,
    val algorithm: String,
    val creationDate: String,
    val certFingerprints: Map<String, String> = emptyMap(),
    val hasAvbKey: Boolean = false
)

data class ApkSignItem(
    val file: java.io.File,
    val name: String,
    val size: Long,
    val formattedSize: String,
    val originalSigner: String = "Inconnu",
    val isSigned: Boolean = false
)

data class RomTrustChainReport(
    val romName: String,
    val totalApks: Int,
    val preservedGappsCount: Int,
    val resignedApksCount: Int,
    val distinctKeyClusters: Int,
    val macPermissionsUpdated: Boolean,
    val odexPurgedCount: Int,
    val hasVbmetaGenerated: Boolean,
    val dataDir: String,
    val reportPath: String,
    val signedRomPath: String = ""
)

enum class GemeauTab(val title: String) {
    KEYMAKER("Keymaker"),
    SIGNAPK("SIGNAPK"),
    ROMSIGN("ROMsign"),
    VERIFICATION("VÉRIFICATION")
}

enum class GemeauSubScreen {
    HUB,
    KEYMAKER,
    SIGNAPK,
    ROMSIGN,
    VERIFICATION_HUB,
    VERIFY_APP,
    VERIFY_ROM,
    RSIGNER
}

enum class GemeauVerifySubTab(val label: String) {
    VERIFY_APP("Verify app"),
    VERIFY_ROM("Verify rom")
}

data class ApkVerificationDetail(
    val fileName: String,
    val filePath: String,
    val fileSizeFormatted: String,
    val isSigned: Boolean,
    val hasV1Scheme: Boolean,
    val hasV2Scheme: Boolean,
    val hasV3Scheme: Boolean,
    val hasV4Scheme: Boolean,
    val sha256Fingerprint: String,
    val subjectDn: String,
    val issuerDn: String,
    val algorithm: String,
    val keySize: Int,
    val validFrom: String,
    val validUntil: String,
    val isExpired: Boolean,
    val isSelfSigned: Boolean,
    val errors: List<String> = emptyList()
)

data class RomVerificationAudit(
    val romFileName: String,
    val romFilePath: String,
    val auditDate: String,
    val totalApksAnalyzed: Int,
    val signedApksCount: Int,
    val unsignedApksCount: Int,
    val gappsCount: Int,
    val distinctSignersCount: Int,
    val keyDistribution: Map<String, Int> = emptyMap(),
    val macPermissionsFound: Boolean = false,
    val macPermissionsAligned: Boolean = true,
    val otacertsFound: Boolean = false,
    val sepolicyAligned: Boolean = true,
    val trustChainValid: Boolean = true,
    val databaseJsonPath: String = "",
    val summaryReportPath: String = "",
    val apksSummary: List<ApkVerificationDetail> = emptyList(),
    val xmlEntriesFound: List<String> = emptyList()
)

data class GemeauUiState(
    val isExecuting: Boolean = false,
    val currentSubScreen: GemeauSubScreen = GemeauSubScreen.HUB,
    val activeTab: GemeauTab = GemeauTab.KEYMAKER,
    val availableKeysets: List<KeysetInfo> = emptyList(),
    val selectedKeyset: KeysetInfo? = null,
    // Page 1: Keymaker inputs
    val newKeysetName: String = "HephaistOS_Keyset",
    val commonName: String = "HephaistOS",
    val organization: String = "Kitchen Community",
    val countryCode: String = "FR",
    val keyAlgorithm: String = "RSA 2048",
    val validityYears: Int = 25,
    // Page 2: SIGNAPK inputs
    val selectedApks: List<ApkSignItem> = emptyList(),
    val selectedKeyRoleForApk: String = "releasekey",
    // Page 3: ROMsign inputs
    val selectedRomFile: java.io.File? = null,
    val generateVbmeta: Boolean = false,
    // Page 4: VÉRIFICATION inputs and results
    val verifySubTab: GemeauVerifySubTab = GemeauVerifySubTab.VERIFY_APP,
    val verifySelectedApks: List<java.io.File> = emptyList(),
    val verifySelectedRomForApkExtraction: java.io.File? = null,
    val verifyExtractedApksFromRom: List<java.io.File> = emptyList(),
    val isExtractingApksFromRom: Boolean = false,
    val apkVerificationResults: List<ApkVerificationDetail> = emptyList(),
    val isVerifyingApks: Boolean = false,
    val verifySelectedRomForAudit: java.io.File? = null,
    val isAuditingRom: Boolean = false,
    val romAuditResult: RomVerificationAudit? = null,
    val lastVerificationDir: String? = null,
    // Page 5: R-SIGNER inputs and results
    val rsignerSelectedTarget: java.io.File? = null,
    val rsignerDetectedGroups: List<SignatureGroup> = emptyList(),
    val rsignerIsAnalyzing: Boolean = false,
    val rsignerAnalysisAudit: RomVerificationAudit? = null,
    val rsignerLastOutputPath: String? = null,
    // Execution Progress & Terminal
    val progress: Float = 0f,
    val currentStepText: String = "Prêt pour la signature",
    val processedCount: Int = 0,
    val totalItemsCount: Int = 0,
    val preservedGappsCount: Int = 0,
    val logs: List<TerminalEntry> = emptyList(),
    val lastGeneratedReportPath: String? = null,
    val lastGeneratedDataDir: String? = null,
    val lastSignedOutputPath: String? = null
)

data class AheField(
    val id: String,
    val type: String = "text", // "text", "number", "dropdown", "file_picker"
    val label: String,
    val default: String = "",
    val source: String = "" // "unpacked_projects", "keysets", "files"
)

data class AheAction(
    val id: String,
    val label: String,
    val icon: String = "Bolt",
    val script: String,
    val requiresRoot: Boolean = false,
    val inputs: List<String> = emptyList()
)

data class AheSection(
    val type: String = "form", // "banner", "form", "info", "actions"
    val title: String = "",
    val subtitle: String = "",
    val fields: List<AheField> = emptyList(),
    val action: AheAction? = null,
    val actions: List<AheAction> = emptyList()
)

data class AheUiTab(
    val id: String,
    val title: String,
    val icon: String = "Dashboard",
    val sections: List<AheSection> = emptyList()
)

data class AheUiConfig(
    val hasDedicatedPage: Boolean = true,
    val addToNavRail: Boolean = false,
    val tabs: List<AheUiTab> = emptyList()
)

data class AheIpcConfig(
    val supportedCommands: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

data class AheManifest(
    val id: String,
    val name: String,
    val version: String = "1.0.0",
    val author: String = "HephaistOS Dev",
    val description: String = "",
    val minHephaistosVersion: Int = 1,
    val permissions: List<String> = listOf("STORAGE"),
    val exportedBinaries: List<String> = emptyList(),
    val ui: AheUiConfig = AheUiConfig(),
    val ipc: AheIpcConfig = AheIpcConfig()
)

data class AheExtension(
    val manifest: AheManifest,
    val installDir: java.io.File,
    val iconFile: java.io.File? = null,
    val isEnabled: Boolean = true,
    val installDate: String = ""
)

data class AheUiState(
    val installedExtensions: List<AheExtension> = emptyList(),
    val activeExtension: AheExtension? = null,
    val isExecuting: Boolean = false,
    val currentStepText: String = "Prêt",
    val progress: Float = 0f,
    val logs: List<TerminalEntry> = emptyList(),
    val formValues: Map<String, String> = emptyMap(),
    val selectedTabId: String = ""
)


