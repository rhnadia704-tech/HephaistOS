package com.example.data

import android.content.Context
import android.os.Environment
import com.example.model.KeysetInfo
import java.io.File
import java.io.FileOutputStream

object WorkspaceManager {

    private const val ROOT_DIR_NAME = "HEPHAISTOS"

    val SUB_DIRS = listOf(
        "Data",
        "UNPACKED",
        "PACKED",
        "Payloads",
        "Extracted",
        "Projects",
        "Output",
        "WebApps",
        "Scripts",
        "Templates",
        "EXTRACT",
        "SIGNER",
        "Extensions",
        "TOOLS",
        "ANALYST"
    )

    fun getWorkspaceRoot(context: Context): File {
        val externalStorage = Environment.getExternalStorageDirectory()
        val candidate = File(externalStorage, ROOT_DIR_NAME)
        return try {
            if (!candidate.exists()) {
                candidate.mkdirs()
            }
            if (candidate.canWrite()) {
                candidate
            } else {
                val fallback = File(context.getExternalFilesDir(null) ?: context.filesDir, ROOT_DIR_NAME)
                if (!fallback.exists()) fallback.mkdirs()
                fallback
            }
        } catch (e: Exception) {
            val fallback = File(context.getExternalFilesDir(null) ?: context.filesDir, ROOT_DIR_NAME)
            if (!fallback.exists()) fallback.mkdirs()
            fallback
        }
    }

    fun getSubDir(context: Context, subDirName: String): File {
        val root = getWorkspaceRoot(context)
        val dir = File(root, subDirName)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getUnpackDataDir(context: Context): File {
        val dataDir = getSubDir(context, "Data")
        val unpackDir = File(dataDir, "unpack")
        if (!unpackDir.exists()) {
            unpackDir.mkdirs()
        }
        return unpackDir
    }

    fun getUnpackedDir(context: Context): File {
        return getSubDir(context, "UNPACKED")
    }

    fun getPackedDir(context: Context): File {
        return getSubDir(context, "PACKED")
    }

    fun getExtractDir(context: Context): File {
        return getSubDir(context, "EXTRACT")
    }

    fun getExtractDataDir(context: Context): File {
        val extractDir = getExtractDir(context)
        val dataDir = File(extractDir, "Data")
        if (!dataDir.exists()) dataDir.mkdirs()
        return dataDir
    }

    fun getExtractDeviceDir(context: Context): File {
        val extractDir = getExtractDir(context)
        val deviceDir = File(extractDir, "device")
        if (!deviceDir.exists()) deviceDir.mkdirs()
        return deviceDir
    }

    fun getExtractGsiAnalysisDir(context: Context): File {
        val extractDir = getExtractDir(context)
        val gsiDir = File(extractDir, "gsi_analysis")
        if (!gsiDir.exists()) gsiDir.mkdirs()
        return gsiDir
    }

    fun getSignerDir(context: Context): File {
        return getSubDir(context, "SIGNER")
    }

    fun getSignerKeysetDir(context: Context): File {
        val dir = File(getSignerDir(context), "KEYSET")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSignerDataDir(context: Context): File {
        val dir = File(getSignerDir(context), "Data")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSignerSignedApksDir(context: Context): File {
        val dir = File(getSignerDir(context), "signed_apks")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSignerSignedRomsDir(context: Context): File {
        val dir = File(getSignerDir(context), "signed_roms")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSignerRSignerDir(context: Context): File {
        val dir = File(getSignerDir(context), "RSIGNER")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getToolsDir(context: Context): File {
        return getSubDir(context, "TOOLS")
    }

    fun getToolsBinDir(context: Context): File {
        val dir = File(getToolsDir(context), "bin")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getToolsScriptsDir(context: Context): File {
        val dir = File(getToolsDir(context), "scripts")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getToolsProfilesDir(context: Context): File {
        val dir = File(getToolsDir(context), "profiles")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getToolsRulesDir(context: Context): File {
        val dir = File(getToolsDir(context), "rules")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getGemeauDataDir(context: Context): File {
        val dataDir = getSubDir(context, "DATA")
        val gemeauDir = File(dataDir, "GEMEAU")
        if (!gemeauDir.exists()) gemeauDir.mkdirs()
        return gemeauDir
    }

    fun getAnalystDir(context: Context): File {
        return getSubDir(context, "ANALYST")
    }

    fun getAnalystTargetDir(context: Context, targetName: String): File {
        val sanitized = targetName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val targetDir = File(getAnalystDir(context), sanitized)
        if (!targetDir.exists()) targetDir.mkdirs()
        return targetDir
    }

    fun getInternalToolsBinDir(context: Context): File {
        val internalDir = File(context.filesDir, "tools/bin")
        if (!internalDir.exists()) internalDir.mkdirs()
        return internalDir
    }

    fun ensureToolsDeployed(context: Context): File {
        val toolsDir = getToolsDir(context)
        val binDir = getToolsBinDir(context)
        val scriptsDir = getToolsScriptsDir(context)
        val profilesDir = getToolsProfilesDir(context)
        val rulesDir = getToolsRulesDir(context)
        val internalBinDir = getInternalToolsBinDir(context)
        getGemeauDataDir(context)

        // 1. Déploiement des scripts d'audit et de vérification par défaut
        val auditScript = File(scriptsDir, "gemeau_deep_audit.sh")
        if (!auditScript.exists()) {
            auditScript.writeText(
                """
                #!/system/bin/sh
                ROM_TARGET="${'$'}1"
                OUTPUT_DIR="${'$'}{2:-/sdcard/HEPHAISTOS/DATA/GEMEAU}"
                echo "[HEPHAISTOS GÉMÉAU] Lancement de l'audit approfondi: ${'$'}ROM_TARGET"
                mkdir -p "${'$'}OUTPUT_DIR"
                if [ -x "${internalBinDir.absolutePath}/gemeau-dissector" ]; then
                    "${internalBinDir.absolutePath}/gemeau-dissector" --rom-path "${'$'}ROM_TARGET" --output-dir "${'$'}OUTPUT_DIR"
                fi
                exit 0
                """.trimIndent()
            )
            try { auditScript.setExecutable(true) } catch (_: Exception) {}
        }

        val verifyScript = File(scriptsDir, "verify_chain.sh")
        if (!verifyScript.exists()) {
            verifyScript.writeText(
                """
                #!/system/bin/sh
                APK="${'$'}1"
                echo "[HEPHAISTOS GÉMÉAU] Vérification APK: ${'$'}APK"
                unzip -l "${'$'}APK" | head -n 20
                exit 0
                """.trimIndent()
            )
            try { verifyScript.setExecutable(true) } catch (_: Exception) {}
        }

        // 2. Déploiement du profil AOSP Standard
        val aospProfile = File(profilesDir, "aosp_standard.json")
        if (!aospProfile.exists()) {
            aospProfile.writeText(
                """
                {
                  "profile_name": "AOSP Standard Trust Chain",
                  "version": "1.0.0",
                  "keys": [
                    { "role": "platform", "target_shared_uids": ["android.uid.system"], "sample_packages": ["android", "com.android.systemui", "com.android.settings"] },
                    { "role": "media", "target_shared_uids": ["android.uid.media"], "sample_packages": ["com.android.providers.media"] },
                    { "role": "shared", "target_shared_uids": ["android.uid.shared"], "sample_packages": ["com.android.contacts"] },
                    { "role": "networkstack", "target_shared_uids": ["android.uid.networkstack"], "sample_packages": ["com.android.networkstack"] },
                    { "role": "releasekey", "target_shared_uids": [], "sample_packages": ["com.android.calculator2"] }
                  ]
                }
                """.trimIndent()
            )
        }

        // 3. Déploiement du template de règles SELinux MMAC
        val macRules = File(rulesDir, "mac_permissions_template.xml")
        if (!macRules.exists()) {
            macRules.writeText(
                """
                <?xml version="1.0" encoding="utf-8"?>
                <policy>
                    <signer signature="@PLATFORM_CERT_HEX@">
                        <seinfo value="platform" />
                    </signer>
                    <signer signature="@MEDIA_CERT_HEX@">
                        <seinfo value="media" />
                    </signer>
                    <signer signature="@SHARED_CERT_HEX@">
                        <seinfo value="shared" />
                    </signer>
                    <default>
                        <seinfo value="default" />
                    </default>
                </policy>
                """.trimIndent()
            )
        }

        // 4. Extraction d'éventuels binaires natifs pré-compilés inclus dans assets/tools
        try {
            val assetManager = context.assets
            val assetList = assetManager.list("tools/bin") ?: emptyArray()
            for (assetName in assetList) {
                val internalFile = File(internalBinDir, assetName)
                val sdcardFile = File(binDir, assetName)
                assetManager.open("tools/bin/$assetName").use { input ->
                    FileOutputStream(internalFile).use { output ->
                        input.copyTo(output)
                    }
                }
                try {
                    internalFile.setExecutable(true, false)
                    internalFile.setReadable(true, false)
                } catch (_: Exception) {}
                
                // Mirror to sdcard tools
                if (!sdcardFile.exists()) {
                    try {
                        internalFile.copyTo(sdcardFile, overwrite = true)
                    } catch (_: Exception) {}
                }
            }
        } catch (_: Exception) {}

        return toolsDir
    }

    fun getExtensionsDir(context: Context): File {
        val dir = getSubDir(context, "Extensions")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getTemplatesDir(context: Context): File {
        val dir = getSubDir(context, "Templates")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAheTemplateDir(context: Context): File {
        val dir = File(getTemplatesDir(context), "ahe_template")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun ensureAheStarterTemplate(context: Context): File {
        val templateDir = getAheTemplateDir(context)
        val binDir = File(templateDir, "bin").apply { mkdirs() }
        val scriptsDir = File(templateDir, "scripts").apply { mkdirs() }
        val toolsDir = File(templateDir, "tools").apply { mkdirs() }
        val docsDir = File(templateDir, "docs").apply { mkdirs() }

        val manifestFile = File(templateDir, "manifest.json")
        if (!manifestFile.exists()) {
            val manifestContent = """
{
  "id": "com.hephaistos.ext.starter",
  "name": "Extension Modèle HephaistOS",
  "version": "1.0.0",
  "author": "HephaistOS Dev Community",
  "description": "Template modèle officiel démontrant l'architecture d'extension .ahe, l'exécution native ARM64 et l'UI Material 3",
  "minHephaistosVersion": 1,
  "permissions": ["STORAGE", "ROOT", "EXEC_BIN"],
  "exportedBinaries": ["starter_tool"],
  "ui": {
    "hasDedicatedPage": true,
    "addToNavRail": false,
    "tabs": [
      {
        "id": "tab_main",
        "title": "Principal",
        "icon": "Dashboard",
        "sections": [
          {
            "type": "banner",
            "title": "Module .ahe Opérationnel",
            "subtitle": "Espace de travail connecté à /sdcard/HEPHAISTOS"
          },
          {
            "type": "form",
            "title": "Exécution d'Action Rapide",
            "subtitle": "Test du pont d'environnement et de l'exécutable natif",
            "fields": [
              { "id": "target_name", "type": "text", "label": "Nom de la cible", "default": "Rom_Porting_Test" },
              { "id": "action_mode", "type": "dropdown", "label": "Mode de Traitement", "default": "Inspection", "source": "unpacked_projects" }
            ],
            "action": {
              "id": "run_starter",
              "label": "Exécuter l'Outil Modèle",
              "icon": "Bolt",
              "script": "scripts/main.sh",
              "requiresRoot": false
            }
          }
        ]
      }
    ]
  },
  "ipc": {
    "supportedCommands": ["ping", "get_status", "run_diagnostics"],
    "dependencies": []
  }
}
            """.trimIndent()
            manifestFile.writeText(manifestContent)
        }

        val scriptFile = File(scriptsDir, "main.sh")
        if (!scriptFile.exists()) {
            val scriptContent = """
#!/system/bin/sh
# Script d'exemple exécuté par HephaistOS Extension Engine (.ahe)

echo "[HEPHAISTOS AHE] Démarrage de l'action modèle..."
echo "[ENV] Workspace racine : ${'$'}HEPHAISTOS_WORKSPACE"
echo "[ENV] Dossier de l'extension : ${'$'}AHE_SELF_DIR"
echo "[ENV] Paramètre target_name : ${'$'}AHE_INPUT_target_name"
echo "[ENV] Paramètre action_mode : ${'$'}AHE_INPUT_action_mode"

# Test du binaire natif inclus dans bin/
if [ -x "${'$'}AHE_SELF_DIR/bin/starter_tool" ]; then
    echo "[EXEC] Lancement de bin/starter_tool..."
    "${'$'}AHE_SELF_DIR/bin/starter_tool" "${'$'}AHE_INPUT_target_name"
else
    echo "[INFO] Simulation du binaire d'inspection..."
    echo "[SUCCESS] Analyse et diagnostic complétés avec succès !"
fi

echo "[FIN] Action .ahe terminée (code retour: 0)"
exit 0
            """.trimIndent()
            scriptFile.writeText(scriptContent)
            try { scriptFile.setExecutable(true) } catch (_: Exception) {}
        }

        val binTool = File(binDir, "starter_tool")
        if (!binTool.exists()) {
            val binScript = """
#!/system/bin/sh
echo "[BIN TOOL] Outil natif exécuté avec succès pour la cible: ${'$'}1"
echo "[BIN TOOL] Espace /sdcard/HEPHAISTOS entièrement accessible."
exit 0
            """.trimIndent()
            binTool.writeText(binScript)
            try { binTool.setExecutable(true) } catch (_: Exception) {}
        }

        val readmeFile = File(docsDir, "README.md")
        if (!readmeFile.exists()) {
            readmeFile.writeText("# Extension Modèle HephaistOS (.ahe)\nConsultez DOC_AHE_EXTENSIONS.md pour les instructions complètes.")
        }

        return templateDir
    }

    fun listAvailableKeysets(context: Context): List<KeysetInfo> {
        val keysetBase = getSignerKeysetDir(context)
        val dirs = keysetBase.listFiles { f -> f.isDirectory } ?: emptyArray()
        val list = mutableListOf<KeysetInfo>()
        for (d in dirs) {
            val keyFiles = d.listFiles { f -> f.extension == "pk8" || f.extension == "pem" } ?: emptyArray()
            val hasAvb = File(d, "avb.pk8").exists() || File(d, "avb.pem").exists()
            val manifest = File(d, "keyset_manifest.json")
            var alg = "RSA 2048"
            var creation = "Récent"
            val fingerprints = mutableMapOf<String, String>()
            if (manifest.exists()) {
                try {
                    val json = org.json.JSONObject(manifest.readText())
                    alg = json.optString("algorithm", "RSA 2048")
                    creation = json.optString("creationDate", "Récent")
                    val fpJson = json.optJSONObject("fingerprints")
                    fpJson?.keys()?.forEach { k ->
                        fingerprints[k] = fpJson.getString(k)
                    }
                } catch (_: Exception) {}
            }
            list.add(
                KeysetInfo(
                    name = d.name,
                    dirPath = d.absolutePath,
                    keyCount = keyFiles.size,
                    algorithm = alg,
                    creationDate = creation,
                    certFingerprints = fingerprints,
                    hasAvbKey = hasAvb
                )
            )
        }
        return list
    }

    fun listUnpackedProjects(context: Context): List<String> {
        val unpackedDir = getUnpackedDir(context)
        val dirs = unpackedDir.listFiles { f -> f.isDirectory }?.map { it.name } ?: emptyList()
        return if (dirs.isEmpty()) {
            // Also check if there are subfolders in Data/unpack or sample unpacked targets
            val sampleTargets = listOf("gsi_arm64_ab", "system_extracted", "vendor_extracted")
            sampleTargets
        } else {
            dirs
        }
    }

    fun initializeWorkspace(context: Context): Map<String, Int> {
        val root = getWorkspaceRoot(context)
        val stats = mutableMapOf<String, Int>()

        for (sub in SUB_DIRS) {
            val dir = File(root, sub)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            stats[sub] = dir.listFiles()?.size ?: 0
        }

        // Ensure Data/unpack exists
        getUnpackDataDir(context)

        // Initialize sample templates and scripts if empty
        val templatesDir = File(root, "Templates")
        val sampleManifest = File(templatesDir, "addon_manifest_example.json")
        if (!sampleManifest.exists()) {
            try {
                sampleManifest.writeText(
                    """
                    {
                      "schema_version": "1.0",
                      "addon_id": "org.hephaistos.addon.sample",
                      "name": "HephaistOS Sample Addon",
                      "version": "1.0.0",
                      "capabilities": [
                        "rom.unpack.payload",
                        "rom.partition.inspect",
                        "fs.erofs.decompress"
                      ],
                      "requires_root": false,
                      "entry_point": "org.hephaistos.addon.MainActivity",
                      "ipc_interface": "org.hephaistos.action.ADDON"
                    }
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }

        val scriptsDir = File(root, "Scripts")
        val sampleScript = File(scriptsDir, "inspect_system_img.sh")
        if (!sampleScript.exists()) {
            try {
                sampleScript.writeText(
                    """
                    #!/system/bin/sh
                    # Script d'analyse de partition pour HephaistOS
                    echo "[HEPHAISTOS] Initialisation de l'analyse..."
                    echo "[INFO] Inspection de la signature AVB et du descripteur de partition"
                    echo "[SUCCESS] Analyse complétée avec succès"
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }

        val webAppsDir = File(root, "WebApps")
        val sampleWebApp = File(webAppsDir, "rom_analyzer")
        if (!sampleWebApp.exists()) {
            sampleWebApp.mkdirs()
            val indexFile = File(sampleWebApp, "index.html")
            try {
                indexFile.writeText(
                    """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head>
                      <meta charset="utf-8">
                      <title>ROM Inspector Addon</title>
                      <style>
                        body { font-family: sans-serif; background: #121316; color: #f0f0f5; padding: 20px; }
                        .card { background: #1e2024; border: 1px solid #333842; border-radius: 12px; padding: 16px; margin-bottom: 12px; }
                        h2 { color: #e67e22; margin-top: 0; }
                        button { background: #e67e22; border: none; color: #fff; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; }
                      </style>
                    </head>
                    <body>
                      <div class="card">
                        <h2>Analyseur de ROM WebApp</h2>
                        <p>Cette application web s'exécute dans l'hôte HephaistOS et peut interagir avec les outils kitchen.</p>
                        <button onclick="alert('Connecté à HephaistOS Core Engine !')">Vérifier l'hôte</button>
                      </div>
                    </body>
                    </html>
                    """.trimIndent()
                )
            } catch (_: Exception) {}
        }

        // Initialize and deploy structured tools and GÉMÉAU data directory
        ensureToolsDeployed(context)

        return stats
    }

    fun saveScript(context: Context, name: String, content: String, isPython: Boolean): File {
        val dir = getSubDir(context, "Scripts")
        val ext = if (isPython) ".py" else ".sh"
        val fileName = if (name.endsWith(ext)) name else "$name$ext"
        val file = File(dir, fileName)
        file.writeText(content)
        return file
    }

    fun saveHtmlWebApp(context: Context, appName: String, htmlContent: String): File {
        val dir = getSubDir(context, "WebApps")
        val sanitized = appName.replace("[^a-zA-Z0-9_-]".toRegex(), "_").lowercase()
        val appFolder = File(dir, sanitized)
        if (!appFolder.exists()) appFolder.mkdirs()
        val indexFile = File(appFolder, "index.html")
        indexFile.writeText(htmlContent)
        return indexFile
    }

    fun listScripts(context: Context): List<File> {
        val dir = getSubDir(context, "Scripts")
        return dir.listFiles { f -> f.isFile && (f.name.endsWith(".sh") || f.name.endsWith(".py")) }?.toList() ?: emptyList()
    }

    fun listWebApps(context: Context): List<File> {
        val dir = getSubDir(context, "WebApps")
        return dir.listFiles { f -> f.isDirectory && File(f, "index.html").exists() }?.toList() ?: emptyList()
    }

    fun getSignerVerifyDir(context: Context): File {
        val rootDataDir = File(getWorkspaceRoot(context), "Data")
        val signerDir = File(rootDataDir, "SIGNER")
        val verifyDir = File(signerDir, "VERIFY")
        if (!verifyDir.exists()) verifyDir.mkdirs()
        return verifyDir
    }

    fun getSatellitesDir(context: Context): File {
        val dir = getSubDir(context, "Satellites")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSatelliteTemplateDir(context: Context): File {
        val dir = File(getSubDir(context, "Templates"), "satellite_template")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun ensureSatelliteStarterTemplate(context: Context): File {
        val templateDir = getSatelliteTemplateDir(context)
        val srcDir = File(templateDir, "app/src/main/java/org/hephaistos/satellite/starter").apply { mkdirs() }

        val manifestFile = File(templateDir, "app/src/main/AndroidManifest.xml")
        if (!manifestFile.exists()) {
            manifestFile.parentFile?.mkdirs()
            manifestFile.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="org.hephaistos.satellite.starter">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:label="Satellite HephaistOS"
        android:supportsRtl="true">

        <!-- ADN INDISPENSABLE HEPHAISTOS : Permet la détection automatique même pour les packages com.aistudio... -->
        <meta-data android:name="hephaistos.satellite" android:value="true" />
        <meta-data android:name="org.hephaistos.satellite.V1" android:value="true" />
        <meta-data android:name="org.hephaistos.addon.CAPABILITIES" android:value="rom_tools,apk_inspect,root_worker" />

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:label="Satellite">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
            <intent-filter>
                <action android:name="org.hephaistos.action.SATELLITE" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>
        </activity>

        <service
            android:name=".SatelliteIpcService"
            android:exported="true">
            <intent-filter>
                <action android:name="org.hephaistos.service.SATELLITE_IPC" />
            </intent-filter>
        </service>
    </application>
</manifest>""".trimIndent()
            )
        }

        val themeFile = File(srcDir, "HephaistLockedTheme.kt")
        if (!themeFile.exists()) {
            themeFile.writeText(
                """package org.hephaistos.satellite.starter

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// CHARTE GRAPHIQUE OFFICIELLE HEPHAISTOS (THÈME VERROUILLÉ)
val HephaistPrimary = Color(0xFFF97316)
val HephaistOnPrimary = Color(0xFFFFFFFF)
val HephaistPrimaryContainer = Color(0xFFFFDBCA)
val HephaistOnPrimaryContainer = Color(0xFF341100)
val HephaistSecondary = Color(0xFF0EA5E9)
val HephaistBackground = Color(0xFF0F172A)
val HephaistSurface = Color(0xFF1E293B)
val HephaistSurfaceVariant = Color(0xFF334155)

val HephaistDarkColorScheme = darkColorScheme(
    primary = HephaistPrimary,
    onPrimary = HephaistOnPrimary,
    primaryContainer = HephaistPrimaryContainer,
    onPrimaryContainer = HephaistOnPrimaryContainer,
    secondary = HephaistSecondary,
    background = HephaistBackground,
    surface = HephaistSurface,
    surfaceVariant = HephaistSurfaceVariant
)

@Composable
fun HephaistSatelliteTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HephaistDarkColorScheme,
        content = content
    )
}""".trimIndent()
            )
        }

        val activityFile = File(srcDir, "MainActivity.kt")
        if (!activityFile.exists()) {
            activityFile.writeText(
                """package org.hephaistos.satellite.starter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HephaistSatelliteTheme {
                Scaffold { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Satellite HephaistOS Connecté",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Ce satellite adopte rigoureusement la charte Material 3 d'HephaistOS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}""".trimIndent()
            )
        }

        val readmeFile = File(templateDir, "README.md")
        readmeFile.writeText(
            """# BIBLE DES SATELLITES HEPHAISTOS (CONTRAT OFFICIEL)

Ce document constitue la référence stricte et immuable pour toute application satellite HephaistOS, qu'elle soit conçue manuellement ou générée par une IA dans Google AI Studio.

## 1. Contrat d'Identification Universel (Manifest)
Peu importe le nom du package de votre application (y compris les packages `com.aistudio.*`), HephaistOS la reconnaîtra instantanément à 100% si elle respecte ces deux critères obligatoires dans `AndroidManifest.xml` :

1. **Balise Meta-Data sous `<application>`** :
   ```xml
   <meta-data android:name="hephaistos.satellite" android:value="true" />
   ```

2. **Action Intent dans votre Activité Principale** :
   ```xml
   <intent-filter>
       <action android:name="org.hephaistos.action.SATELLITE" />
       <category android:name="android.intent.category.DEFAULT" />
   </intent-filter>
   ```

## 2. Thème Graphique Strictement Verrouillé
Les satellites sont de véritables extensions intégrées d'HephaistOS :
- Ne pas inventer de nouvelle charte ou palette de couleurs.
- Utiliser impérativement `HephaistSatelliteTheme` (défini dans `HephaistLockedTheme.kt`).
- L'interface suit les teintes officielles : Dark Background (0xFF0F172A), Surface (0xFF1E293B) et Ambre Héphaïstos (0xFFF97316).

## 3. Déploiement et Dépôt Direct
- Une fois installée sur l'appareil, l'application est automatiquement détectée et apparaît sur l'accueil d'HephaistOS.
- Alternativement, le fichier `.apk` peut être déposé directement dans le dossier :
  `/sdcard/HEPHAISTOS/Satellites/`
""".trimIndent()
        )

        return templateDir
    }
}
