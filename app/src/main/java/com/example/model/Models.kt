package com.example.model

enum class Screen(val title: String) {
    HOME("Accueil"),
    UNPACK_REPACK("Unpack / Repack"),
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

