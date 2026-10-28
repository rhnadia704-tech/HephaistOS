package com.example.model

enum class Screen(val title: String) {
    HOME("Accueil"),
    UNPACK_REPACK("Unpack / Repack"),
    EXTRACTOR("Extractor"),
    GEMEAU("Géméau"),
    ANALYST("Analyst"),
    SCEAU("Sceau"),
    SIGNALAB("Signalab"),
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

enum class SceauSubScreen {
    HUB,
    VERISIN,
    KEYCORE
}

enum class KeycoreTab(val label: String, val subtitle: String) {
    KEYMAKER("Keymaker", "Forge standard de clés"),
    VERISIN_KEY("Verisin Key", "Clé calquée sur un APK"),
    VERISIN_CLONE("Verisin Clone", "Signature parallèle & clone")
}

data class KeycoreTargetAnalysis(
    val apkPath: String = "",
    val apkName: String = "",
    val schemesDisplay: String = "",
    val keyType: String = "RSA",
    val keySizeBits: Int = 2048,
    val sigAlgName: String = "SHA256withRSA",
    val subjectDn: String = "",
    val issuerDn: String = "",
    val validityYears: Int = 25,
    val isSelfSigned: Boolean = true
)

data class KeycoreUiState(
    val activeTab: KeycoreTab = KeycoreTab.KEYMAKER,
    val selectedApkPath: String = "",
    val isAnalyzingApk: Boolean = false,
    val targetAnalysis: KeycoreTargetAnalysis? = null,
    val isGeneratingKey: Boolean = false,
    val isResigningApk: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val lastGeneratedKeyset: KeysetInfo? = null,
    val lastResignedApkPath: String? = null
)

data class SceauUiState(
    val currentSubScreen: SceauSubScreen = SceauSubScreen.HUB,
    val keycoreState: KeycoreUiState = KeycoreUiState()
)

// ==========================================
// SIGNALAB — Suite de décompaction, scan et resignature
// ==========================================

enum class SignalabModule(val title: String, val description: String) {
    IMG_SIGNER("IMG Signer", "Décompaction haute précision, audit Verisin & resignature intégrale")
}

data class SignalabScanBlueprint(
    val scanId: String = "",
    val sourceFilePath: String = "",
    val sourceFileName: String = "",
    val sourceFileSize: Long = 0L,
    val containerFormat: String = "", // "ext4", "erofs", "payload.bin", "zip", "raw_img", "sparse_img"
    val partitionName: String = "",
    val unpackedDirPath: String = "",
    val scanDataDirPath: String = "",
    val totalExtractedFiles: Int = 0,
    val totalApksFound: Int = 0,
    val blockSize: Int = 4096,
    val partitionSizeBytes: Long = 0L,
    val hasSelinuxContexts: Boolean = false,
    val hasFsConfig: Boolean = false,
    val scanTimestamp: Long = System.currentTimeMillis()
)

data class ApkSignatureCluster(
    val clusterId: String,
    val clusterRole: String, // "PLATFORM", "MEDIA", "SHARED", "NETWORKSTACK", "GOOGLE_SERVICES", "THIRD_PARTY"
    val certificateFingerprintSha256: String,
    val subjectDn: String,
    val issuerDn: String,
    val keyType: String,
    val keySizeBits: Int,
    val sigAlgName: String,
    val schemesDisplay: String,
    val isGoogleServices: Boolean = false,
    val associatedApks: List<String> = emptyList() // Relative paths of APKs
)

enum class SignalabSignMode(val label: String, val description: String) {
    CUSTOM_SIGN("Custom Sign", "Nouvelle identité personnalisée, en conservant le mapping et l'architecture exacte de la cible"),
    CLONE_SIGN("Clone Sign", "Reproduction miroir des Subject DNs et de la chaîne de confiance d'origine")
}

data class SignalabSignOptions(
    val mode: SignalabSignMode = SignalabSignMode.CLONE_SIGN,
    val customCommonName: String = "Signalab Certified",
    val customOrganization: String = "HephaistOS Sovereign",
    val customCountry: String = "FR",
    val preserveGoogleApps: Boolean = true,
    val cleanArtArtifacts: Boolean = true, // Clear old odex/vdex/oat and fsvmeta to prevent bootloop
    val outputFormat: String = "AUTO" // Auto-detect from blueprint
)

data class SignalabScanResult(
    val blueprint: SignalabScanBlueprint = SignalabScanBlueprint(),
    val clusters: List<ApkSignatureCluster> = emptyList(),
    val keyMapping: Map<String, String> = emptyMap(), // apkPath -> clusterId
    val permissionFilesCount: Int = 0,
    val artFilesCount: Int = 0, // Count of .odex, .vdex, .oat found
    val fsvmetaCount: Int = 0
)

data class SignalabUiState(
    val activeModule: SignalabModule = SignalabModule.IMG_SIGNER,
    val selectedFilePath: String = "",
    val isScanning: Boolean = false,
    val scanStepText: String = "",
    val scanProgress: Float = 0f,
    val scanResult: SignalabScanResult? = null,
    val showSignDialog: Boolean = false,
    val signOptions: SignalabSignOptions = SignalabSignOptions(),
    val isResigningAndRepacking: Boolean = false,
    val resignStepText: String = "",
    val resignProgress: Float = 0f,
    val finalRepackedPath: String? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)


