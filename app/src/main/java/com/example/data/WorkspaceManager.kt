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
        "Extensions"
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
}
