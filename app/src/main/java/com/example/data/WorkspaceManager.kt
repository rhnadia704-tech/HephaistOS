package com.example.data

import android.content.Context
import android.os.Environment
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
        "Templates"
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
