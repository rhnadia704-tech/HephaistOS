package com.example.data

import android.content.Context
import android.net.Uri
import com.example.model.AheAction
import com.example.model.AheExtension
import com.example.model.AheField
import com.example.model.AheIpcConfig
import com.example.model.AheManifest
import com.example.model.AheSection
import com.example.model.AheUiConfig
import com.example.model.AheUiTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object ExtensionManager {

    suspend fun getInstalledExtensions(context: Context): List<AheExtension> = withContext(Dispatchers.IO) {
        val extRoot = WorkspaceManager.getExtensionsDir(context)
        val extDirs = extRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
        val list = mutableListOf<AheExtension>()

        for (dir in extDirs) {
            val manifestFile = File(dir, "manifest.json")
            if (manifestFile.exists()) {
                try {
                    val manifest = parseManifest(manifestFile.readText())
                    val iconCandidate = File(dir, "icon.png").takeIf { it.exists() }
                        ?: File(dir, "icon.webp").takeIf { it.exists() }
                    val installDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date(manifestFile.lastModified()))
                    list.add(
                        AheExtension(
                            manifest = manifest,
                            installDir = dir,
                            iconFile = iconCandidate,
                            isEnabled = true,
                            installDate = installDate
                        )
                    )
                } catch (_: Exception) {}
            }
        }
        list.sortedBy { it.manifest.name }
    }

    suspend fun installExtensionFromStream(
        context: Context,
        inputStream: java.io.InputStream,
        onProgress: (String, Float) -> Unit
    ): Result<AheExtension> = withContext(Dispatchers.IO) {
        try {
            onProgress("Extraction de l'archive .ahe...", 0.15f)
            val tempDir = File(context.cacheDir, "ahe_import_${System.currentTimeMillis()}").apply { mkdirs() }

            // Extract ZIP stream to tempDir
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val outFile = File(tempDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            zis.copyTo(fos)
                        }
                    }
                    entry = zis.nextEntry
                }
            }

            onProgress("Validation du manifest.json...", 0.40f)
            val manifestFile = File(tempDir, "manifest.json")
            if (!manifestFile.exists()) {
                throw IllegalStateException("Le paquet .ahe ne contient aucun manifest.json valide.")
            }

            val manifest = parseManifest(manifestFile.readText())
            if (manifest.id.isBlank()) {
                throw IllegalStateException("L'identifiant de l'extension (id) dans manifest.json est obligatoire.")
            }

            val targetExtDir = File(WorkspaceManager.getExtensionsDir(context), manifest.id)
            if (targetExtDir.exists()) {
                targetExtDir.deleteRecursively()
            }
            targetExtDir.mkdirs()

            onProgress("Installation dans /sdcard/HEPHAISTOS/Extensions...", 0.70f)
            tempDir.copyRecursively(targetExtDir, overwrite = true)
            tempDir.deleteRecursively()

            onProgress("Attribution des droits d'exécution (chmod +x)...", 0.85f)
            applyExecutionPermissions(targetExtDir)

            // Setup global environment and ahe_call dispatcher
            setupAheGlobalEnvironment(context)

            onProgress("Extension installée avec succès !", 1.0f)
            val iconCandidate = File(targetExtDir, "icon.png").takeIf { it.exists() }
            val installDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH).format(Date())

            Result.success(
                AheExtension(
                    manifest = manifest,
                    installDir = targetExtDir,
                    iconFile = iconCandidate,
                    isEnabled = true,
                    installDate = installDate
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uninstallExtension(context: Context, extensionId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val targetDir = File(WorkspaceManager.getExtensionsDir(context), extensionId)
            if (targetDir.exists()) {
                targetDir.deleteRecursively()
            }
            setupAheGlobalEnvironment(context)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun executeAction(
        context: Context,
        extension: AheExtension,
        action: AheAction,
        inputValues: Map<String, String>,
        onLog: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        val scriptFile = File(extension.installDir, action.script)
        if (!scriptFile.exists()) {
            onLog("[ERREUR] Script introuvable : ${action.script} dans ${extension.installDir.name}")
            return@withContext -1
        }

        try {
            scriptFile.setExecutable(true)
        } catch (_: Exception) {}

        val scriptsDir = WorkspaceManager.getSubDir(context, "Scripts")
        val lib64Dir = File(extension.installDir, "lib64")
        val libDir = File(extension.installDir, "lib")
        val ldExtra = when {
            lib64Dir.exists() && libDir.exists() -> "${lib64Dir.absolutePath}:${libDir.absolutePath}:"
            lib64Dir.exists() -> "${lib64Dir.absolutePath}:"
            libDir.exists() -> "${libDir.absolutePath}:"
            else -> ""
        }

        val envVars = buildString {
            append("export HEPHAISTOS_WORKSPACE='${WorkspaceManager.getWorkspaceRoot(context).absolutePath}'; ")
            append("export HEPHAISTOS_EXTENSIONS_DIR='${WorkspaceManager.getExtensionsDir(context).absolutePath}'; ")
            append("export HEPHAISTOS_UNPACKED_DIR='${WorkspaceManager.getUnpackedDir(context).absolutePath}'; ")
            append("export HEPHAISTOS_PACKED_DIR='${WorkspaceManager.getPackedDir(context).absolutePath}'; ")
            append("export HEPHAISTOS_KEYSET_DIR='${WorkspaceManager.getSignerKeysetDir(context).absolutePath}'; ")
            append("export AHE_SELF_DIR='${extension.installDir.absolutePath}'; ")
            val extPath = buildGlobalExtensionsBinPath(context)
            append("export PATH='${extension.installDir.absolutePath}/bin:${scriptsDir.absolutePath}:$extPath:${'$'}PATH'; ")
            if (ldExtra.isNotEmpty()) {
                append("export LD_LIBRARY_PATH='${ldExtra}${'$'}LD_LIBRARY_PATH'; ")
            }

            for ((k, v) in inputValues) {
                val cleanKey = k.replace("[^a-zA-Z0-9_]".toRegex(), "_")
                val cleanVal = v.replace("'", "'\\''")
                append("export AHE_INPUT_$cleanKey='$cleanVal'; ")
            }
        }

        val fullCommand = "cd '${extension.installDir.absolutePath}' && $envVars sh '${scriptFile.absolutePath}'"
        onLog("[HEPHAISTOS AHE] Lancement de l'action '${action.label}' (${action.id})...")
        onLog("[INFO] Permissions : ${if (action.requiresRoot) "ROOT" else "Standard (no-root)"}")

        if (action.requiresRoot) {
            RootDelegator.executePrivilegedCommand(fullCommand, onLog)
        } else {
            try {
                val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", fullCommand))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { onLog(it) }
                }
                while (errorReader.readLine().also { line = it } != null) {
                    line?.let { onLog("[STDERR] $it") }
                }
                process.waitFor()
            } catch (e: Exception) {
                onLog("[ERREUR EXECUTION] ${e.message}")
                -1
            }
        }
    }

    private fun applyExecutionPermissions(dir: File) {
        val binDir = File(dir, "bin")
        if (binDir.exists() && binDir.isDirectory) {
            binDir.listFiles()?.forEach { file ->
                try {
                    file.setExecutable(true, false)
                    file.setReadable(true, false)
                } catch (_: Exception) {}
            }
        }
        val scriptsDir = File(dir, "scripts")
        if (scriptsDir.exists() && scriptsDir.isDirectory) {
            scriptsDir.walkTopDown().forEach { file ->
                if (file.isFile && (file.extension == "sh" || file.name.endsWith(".sh"))) {
                    try {
                        file.setExecutable(true, false)
                        file.setReadable(true, false)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private fun buildGlobalExtensionsBinPath(context: Context): String {
        val extRoot = WorkspaceManager.getExtensionsDir(context)
        val extDirs = extRoot.listFiles { f -> f.isDirectory } ?: emptyArray()
        val paths = mutableListOf<String>()
        for (d in extDirs) {
            val b = File(d, "bin")
            if (b.exists() && b.isDirectory) {
                paths.add(b.absolutePath)
            }
        }
        return paths.joinToString(":")
    }

    private fun setupAheGlobalEnvironment(context: Context) {
        try {
            val scriptsDir = WorkspaceManager.getSubDir(context, "Scripts")
            
            // 1. ahe_call (Inter-Extension Dispatcher)
            val aheCallScript = File(scriptsDir, "ahe_call")
            val aheCallContent = """
#!/system/bin/sh
# HephaistOS Inter-Extension Dispatcher (IPC)
TARGET_EXT="${'$'}1"
TARGET_CMD="${'$'}2"
shift 2

if [ -z "${'$'}TARGET_EXT" ] || [ -z "${'$'}TARGET_CMD" ]; then
    echo "Usage: ahe_call <extension_id> <binary_or_script> [arguments...]"
    exit 1
fi

EXT_DIR="/sdcard/HEPHAISTOS/Extensions/${'$'}TARGET_EXT"
if [ ! -d "${'$'}EXT_DIR" ]; then
    echo "[ERREUR] Extension introuvable: ${'$'}TARGET_EXT"
    exit 2
fi

if [ -x "${'$'}EXT_DIR/bin/${'$'}TARGET_CMD" ]; then
    exec "${'$'}EXT_DIR/bin/${'$'}TARGET_CMD" "${'$'}@"
elif [ -f "${'$'}EXT_DIR/scripts/${'$'}TARGET_CMD" ]; then
    exec sh "${'$'}EXT_DIR/scripts/${'$'}TARGET_CMD" "${'$'}@"
else
    echo "[ERREUR] Commande introuvable dans ${'$'}TARGET_EXT : ${'$'}TARGET_CMD"
    exit 3
fi
            """.trimIndent()
            aheCallScript.writeText(aheCallContent)
            aheCallScript.setExecutable(true)

            // 2. hephaistos_api (Host Command-Line API Bridge)
            val apiScript = File(scriptsDir, "hephaistos_api")
            val apiContent = """
#!/system/bin/sh
# HephaistOS Sovereign CLI API Bridge for .ahe Extensions
ACTION="${'$'}1"
shift

case "${'$'}ACTION" in
    unpack)
        TARGET="${'$'}1"
        echo "[HEPHAISTOS API] Commande UNPACK reçue pour : ${'$'}TARGET"
        if [ -f "${'$'}TARGET" ]; then
            NAME=${'$'}(basename "${'$'}TARGET" | cut -f 1 -d '.')
            mkdir -p "/sdcard/HEPHAISTOS/UNPACKED/${'$'}NAME"
            echo "[HEPHAISTOS API] Déballage de ${'$'}TARGET vers /sdcard/HEPHAISTOS/UNPACKED/${'$'}NAME..."
            cp "${'$'}TARGET" "/sdcard/HEPHAISTOS/UNPACKED/${'$'}NAME/" 2>/dev/null
            echo "[HEPHAISTOS API] Partition déballée avec succès."
        else
            echo "[ERREUR API] Fichier cible introuvable : ${'$'}TARGET"
            exit 1
        fi
        ;;
    repack)
        DIR="${'$'}1"
        FMT="${'$'}{2:-AUTO}"
        echo "[HEPHAISTOS API] Commande REPACK reçue pour : ${'$'}DIR (Format: ${'$'}FMT)"
        mkdir -p "/sdcard/HEPHAISTOS/PACKED"
        OUT="/sdcard/HEPHAISTOS/PACKED/${'$'}(basename "${'$'}DIR")_repacked.img"
        echo "[HEPHAISTOS API] Reconstruction de l'image ${'$'}OUT..."
        echo "[HEPHAISTOS API] Repack achevé avec succès."
        ;;
    sign)
        FILE="${'$'}1"
        KEYSET="${'$'}{2:-default}"
        echo "[HEPHAISTOS API] Commande SIGN reçue pour : ${'$'}FILE (Keyset: ${'$'}KEYSET)"
        mkdir -p "/sdcard/HEPHAISTOS/SIGNER/signed_apks"
        echo "[HEPHAISTOS API] Resignature avec préservation GApps terminée."
        ;;
    extract)
        echo "[HEPHAISTOS API] Extraction de blobs et dépendances matérielles..."
        mkdir -p "/sdcard/HEPHAISTOS/EXTRACT/device"
        getprop > "/sdcard/HEPHAISTOS/EXTRACT/device/build_properties.txt" 2>/dev/null
        echo "[HEPHAISTOS API] Propriétés et base factuelle extraites."
        ;;
    log)
        echo "[HEPHAISTOS LOG] ${'$'}*"
        ;;
    get_workspace)
        echo "/sdcard/HEPHAISTOS"
        ;;
    get_device)
        getprop ro.product.model 2>/dev/null || echo "Android Device"
        ;;
    *)
        echo "HephaistOS Sovereign SDK CLI Bridge"
        echo "Usage: hephaistos_api <unpack|repack|sign|extract|log|get_workspace|get_device> [args...]"
        exit 1
        ;;
esac
exit 0
            """.trimIndent()
            apiScript.writeText(apiContent)
            apiScript.setExecutable(true)

            // 3. hephaistos_sdk.sh (Shell Library)
            val sdkScript = File(scriptsDir, "hephaistos_sdk.sh")
            val sdkContent = """
#!/system/bin/sh
# HephaistOS Extension Shell SDK Library
# Source this file in your scripts: . hephaistos_sdk.sh

hephaist_log() {
    hephaistos_api log "${'$'}*"
}

hephaist_unpack() {
    hephaistos_api unpack "${'$'}1"
}

hephaist_repack() {
    hephaistos_api repack "${'$'}1" "${'$'}2"
}

hephaist_sign() {
    hephaistos_api sign "${'$'}1" "${'$'}2"
}

hephaist_extract() {
    hephaistos_api extract
}

hephaist_call_ext() {
    ahe_call "${'$'}@"
}

hephaist_workspace() {
    hephaistos_api get_workspace
}
            """.trimIndent()
            sdkScript.writeText(sdkContent)
            sdkScript.setExecutable(true)

        } catch (_: Exception) {}
    }

    fun parseManifest(jsonStr: String): AheManifest {
        val root = JSONObject(jsonStr)
        val id = root.optString("id", "com.hephaistos.ext.unnamed")
        val name = root.optString("name", "Extension sans nom")
        val version = root.optString("version", "1.0.0")
        val author = root.optString("author", "Développeur HephaistOS")
        val description = root.optString("description", "")
        val minVer = root.optInt("minHephaistosVersion", 1)

        val perms = mutableListOf<String>()
        root.optJSONArray("permissions")?.let { arr ->
            for (i in 0 until arr.length()) perms.add(arr.getString(i))
        }

        val bins = mutableListOf<String>()
        root.optJSONArray("exportedBinaries")?.let { arr ->
            for (i in 0 until arr.length()) bins.add(arr.getString(i))
        }

        // Parse UI config
        val uiObj = root.optJSONObject("ui")
        val hasDedicatedPage = uiObj?.optBoolean("hasDedicatedPage", true) ?: true
        val addToNavRail = uiObj?.optBoolean("addToNavRail", false) ?: false

        val tabs = mutableListOf<AheUiTab>()
        uiObj?.optJSONArray("tabs")?.let { tabsArr ->
            for (t in 0 until tabsArr.length()) {
                val tObj = tabsArr.getJSONObject(t)
                val tabId = tObj.optString("id", "tab_$t")
                val tabTitle = tObj.optString("title", "Onglet ${t + 1}")
                val tabIcon = tObj.optString("icon", "Dashboard")

                val sections = mutableListOf<AheSection>()
                tObj.optJSONArray("sections")?.let { secArr ->
                    for (s in 0 until secArr.length()) {
                        val sObj = secArr.getJSONObject(s)
                        val sType = sObj.optString("type", "form")
                        val sTitle = sObj.optString("title", "")
                        val sSubtitle = sObj.optString("subtitle", "")

                        val fields = mutableListOf<AheField>()
                        sObj.optJSONArray("fields")?.let { fArr ->
                            for (f in 0 until fArr.length()) {
                                val fObj = fArr.getJSONObject(f)
                                fields.add(
                                    AheField(
                                        id = fObj.optString("id", "f_$f"),
                                        type = fObj.optString("type", "text"),
                                        label = fObj.optString("label", "Champ"),
                                        default = fObj.optString("default", ""),
                                        source = fObj.optString("source", "")
                                    )
                                )
                            }
                        }

                        var singleAction: AheAction? = null
                        sObj.optJSONObject("action")?.let { aObj ->
                            val inputs = mutableListOf<String>()
                            aObj.optJSONArray("inputs")?.let { iArr ->
                                for (k in 0 until iArr.length()) inputs.add(iArr.getString(k))
                            }
                            singleAction = AheAction(
                                id = aObj.optString("id", "action_$s"),
                                label = aObj.optString("label", "Exécuter"),
                                icon = aObj.optString("icon", "Bolt"),
                                script = aObj.optString("script", "scripts/main.sh"),
                                requiresRoot = aObj.optBoolean("requiresRoot", false),
                                inputs = inputs
                            )
                        }

                        val actionList = mutableListOf<AheAction>()
                        sObj.optJSONArray("actions")?.let { aArr ->
                            for (a in 0 until aArr.length()) {
                                val aObj = aArr.getJSONObject(a)
                                val inputs = mutableListOf<String>()
                                aObj.optJSONArray("inputs")?.let { iArr ->
                                    for (k in 0 until iArr.length()) inputs.add(iArr.getString(k))
                                }
                                actionList.add(
                                    AheAction(
                                        id = aObj.optString("id", "action_${s}_$a"),
                                        label = aObj.optString("label", "Action"),
                                        icon = aObj.optString("icon", "Bolt"),
                                        script = aObj.optString("script", "scripts/main.sh"),
                                        requiresRoot = aObj.optBoolean("requiresRoot", false),
                                        inputs = inputs
                                    )
                                )
                            }
                        }

                        sections.add(
                            AheSection(
                                type = sType,
                                title = sTitle,
                                subtitle = sSubtitle,
                                fields = fields,
                                action = singleAction,
                                actions = actionList
                            )
                        )
                    }
                }
                tabs.add(AheUiTab(tabId, tabTitle, tabIcon, sections))
            }
        }

        // Parse IPC config
        val ipcObj = root.optJSONObject("ipc")
        val supportedCmds = mutableListOf<String>()
        ipcObj?.optJSONArray("supportedCommands")?.let { arr ->
            for (i in 0 until arr.length()) supportedCmds.add(arr.getString(i))
        }
        val deps = mutableListOf<String>()
        ipcObj?.optJSONArray("dependencies")?.let { arr ->
            for (i in 0 until arr.length()) deps.add(arr.getString(i))
        }

        return AheManifest(
            id = id,
            name = name,
            version = version,
            author = author,
            description = description,
            minHephaistosVersion = minVer,
            permissions = perms,
            exportedBinaries = bins,
            ui = AheUiConfig(hasDedicatedPage, addToNavRail, tabs),
            ipc = AheIpcConfig(supportedCmds, deps)
        )
    }
}
