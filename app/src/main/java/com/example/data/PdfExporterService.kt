package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object PdfExporterService {

    const val PDF_FILE_NAME = "HephaistOS_Guide_Architectural_Satellites.pdf"
    const val STARTER_KIT_ZIP_NAME = "HephaistOS_Satellite_StarterKit.zip"

    fun generateAddonSdkSpecificationPdf(context: Context): File {
        val document = PdfDocument()
        val pageWidth = 595 // A4 standard points
        val pageHeight = 842

        val dateStr = SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH).format(Date())

        // ═══════════════════════════════════════════════════════════════════════
        // PAGE 1 : ARCHITECTURE GÉNÉRALE & ARBORESCENCE OBLIGATOIRE HEPHAISTOS
        // ═══════════════════════════════════════════════════════════════════════
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page1 = document.startPage(pageInfo1)
        val c1 = page1.canvas
        drawPageHeader(c1, pageWidth, "1. ARCHITECTURE GLOBALE & ARBORESCENCE SYSTÈME", dateStr)

        var y = 70f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        drawSectionTitle(c1, "1.1 Le Squelette Central & Rôle d'Hôte", 40f, y)
        y += 18f
        val textIntro = listOf(
            "HephaistOS est conçu comme l'application hôte, le lanceur modulaire et le cerveau central d'ingénierie",
            "pour Android. Son objectif fondamental est de permettre la décompilation, modification et recompilation",
            "complète d'OS (ROM Stock, Custom ROM, images GSI, payload.bin, super.img) directement sur l'appareil,",
            "rivalisant avec les environnements de compilation AOSP sans nécessiter de serveur Linux externe.",
            "• Rôle de la Colonne Vertébrale : L'hôte gère le terminal, l'ordonnancement, la mémoire et le root.",
            "• Écosystème Satellites : Toute fonctionnalité externe s'intègre sous forme de Satellite (APK, Script ou WebApp).",
            "• Intelligence Évolutive : Chaque satellite détecté enrichit automatiquement la boîte à outils globale."
        )
        y = drawTextParagraph(c1, textIntro, 40f, y)
        y += 12f

        drawSectionTitle(c1, "1.2 Arborescence Obligatoire du Workspace (/sdcard/HEPHAISTOS/)", 40f, y)
        y += 18f
        val textWorkspace = listOf(
            "Toutes les opérations d'extraction, reconstruction et métadonnées doivent respecter cette structure :",
            "  • /sdcard/HEPHAISTOS/UNPACKED/     : Répertoire cible des partitions et images décompressées.",
            "  • /sdcard/HEPHAISTOS/PACKED/       : Répertoire recevant les ROMs, GSIs et images finales réassemblées.",
            "  • /sdcard/HEPHAISTOS/Data/unpack/  : Métadonnées critiques sauvegardées lors de l'unpack (tailles exactes,",
            "                                       contextes SELinux file_contexts, table des symlinks, superblocks,",
            "                                       tables AVB/vbmeta) permettant un repack rigoureusement bit-à-bit.",
            "  • /sdcard/HEPHAISTOS/Payloads/     : Fichiers sources volumineux (payload.bin, super.img, ZIPs OTA).",
            "  • /sdcard/HEPHAISTOS/Projects/     : Fiches projets par architecture ou version d'Android cible.",
            "  • /sdcard/HEPHAISTOS/Scripts/      : Scripts satellites (.sh / .py) exécutables avec délégation su.",
            "  • /sdcard/HEPHAISTOS/WebApps/      : Applications web satellites (HTML5/JS) chargées localement.",
            "  • /sdcard/HEPHAISTOS/Templates/    : Spécifications officielles, kits de démarrage et documentation."
        )
        y = drawTextParagraph(c1, textWorkspace, 40f, y)
        y += 14f

        drawSectionTitle(c1, "1.3 Cycle de Vie Unpack & Repack Bit-à-Bit", 40f, y)
        y += 18f
        val textUnpackCycle = listOf(
            "1. DÉTECTION : L'hôte inspecte les magics bytes (Android Sparse, EROFS, EXT4, Payload OTA, Super LP).",
            "2. UNPACK SÉCURISÉ : Extraction vers sdcard/HEPHAISTOS/UNPACKED/<nom_projet>/.",
            "3. PRESERVATION : Sauvegarde JSON complète des symlinks et autorisations dans HEPHAISTOS/Data/unpack/.",
            "4. REPACK AUTOMATISÉ : Reconstruction depuis sdcard/HEPHAISTOS/PACKED/ conforme à l'original."
        )
        y = drawTextParagraph(c1, textUnpackCycle, 40f, y)

        drawPageFooter(c1, pageWidth, pageHeight, 1, 4)
        document.finishPage(page1)

        // ═══════════════════════════════════════════════════════════════════════
        // PAGE 2 : SPÉCIFICATION & MODÈLE DES APK SATELLITES (ADDONS NATIVE)
        // ═══════════════════════════════════════════════════════════════════════
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
        val page2 = document.startPage(pageInfo2)
        val c2 = page2.canvas
        drawPageHeader(c2, pageWidth, "2. TEMPLATE DES APK SATELLITES (MODULES ANDROID)", dateStr)

        y = 70f
        drawSectionTitle(c2, "2.1 Déclaration Obligatoire dans AndroidManifest.xml", 40f, y)
        y += 18f
        val textApkIntro = listOf(
            "Une application Android Satellite est un APK autonome installé sur l'appareil. Dès son installation,",
            "HephaistOS la détecte instantanément via BroadcastReceiver dynamique sans nécessiter de redémarrage.",
            "Le Manifeste de l'APK satellite DOIT comporter les filtres suivants :"
        )
        y = drawTextParagraph(c2, textApkIntro, 40f, y)
        y += 8f

        val codeManifest = listOf(
            "<activity android:name=\".SatelliteMainActivity\" android:exported=\"true\">",
            "    <intent-filter>",
            "        <action android:name=\"android.intent.action.MAIN\" />",
            "        <!-- Marqueur officiel requis pour HephaistOS -->",
            "        <action android:name=\"org.hephaistos.action.ADDON\" />",
            "        <category android:name=\"org.hephaistos.category.ADDON\" />",
            "        <category android:name=\"android.intent.category.DEFAULT\" />",
            "    </intent-filter>",
            "    <meta-data",
            "        android:name=\"org.hephaistos.addon.CAPABILITIES\"",
            "        android:value=\"gsi.unpack,rom.repack,payload.dumper,erofs.tools\" />",
            "</activity>"
        )
        y = drawCodeBlock(c2, codeManifest, 40f, y, pageWidth - 80f)
        y += 14f

        drawSectionTitle(c2, "2.2 Protocole IPC & Broadcast de Découverte", 40f, y)
        y += 18f
        val textIpc = listOf(
            "Les satellites communiquent avec HephaistOS via des Intents explicites ou un BroadcastReceiver dédié.",
            "Exemple de classe de réception satellite :"
        )
        y = drawTextParagraph(c2, textIpc, 40f, y)
        y += 8f

        val codeReceiver = listOf(
            "class HephaistSatelliteReceiver : BroadcastReceiver() {",
            "    override fun onReceive(context: Context, intent: Intent) {",
            "        if (intent.action == \"org.hephaistos.action.INVOKE_TOOL\") {",
            "            val tool = intent.getStringExtra(\"TOOL_NAME\")",
            "            val targetDir = intent.getStringExtra(\"TARGET_DIR\")",
            "            // Exécuter l'action satellite et renvoyer le résultat à HephaistOS",
            "        }",
            "    }",
            "}"
        )
        y = drawCodeBlock(c2, codeReceiver, 40f, y, pageWidth - 80f)

        drawPageFooter(c2, pageWidth, pageHeight, 2, 4)
        document.finishPage(page2)

        // ═══════════════════════════════════════════════════════════════════════
        // PAGE 3 : SPÉCIFICATION & MODÈLE DES SCRIPTS SATELLITES (SHELL & PYTHON)
        // ═══════════════════════════════════════════════════════════════════════
        val pageInfo3 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 3).create()
        val page3 = document.startPage(pageInfo3)
        val c3 = page3.canvas
        drawPageHeader(c3, pageWidth, "3. TEMPLATE DES SCRIPTS SATELLITES (SHELL & PYTHON)", dateStr)

        y = 70f
        drawSectionTitle(c3, "3.1 Emplacement et En-Têtes Normalisés HephaistOS", 40f, y)
        y += 18f
        val textScriptIntro = listOf(
            "Tous les scripts satellites doivent résider dans /sdcard/HEPHAISTOS/Scripts/.",
            "HephaistOS parse automatiquement la première section du script pour en extraire le nom,",
            "la description, le besoin en privilèges root (su) et les capacités déclarées."
        )
        y = drawTextParagraph(c3, textScriptIntro, 40f, y)
        y += 8f

        val codeShellHeader = listOf(
            "#!/system/bin/sh",
            "# HEPHAISTOS_SCRIPT_V1",
            "# NAME: Repack EROFS Partition",
            "# DESCRIPTION: Recompile un dossier extrait au format EROFS optimisé",
            "# REQUIRE_ROOT: true",
            "# CAPABILITIES: erofs.compress,fs.repack",
            "",
            "TARGET_DIR=\"$1\"       # Dossier source dans /sdcard/HEPHAISTOS/UNPACKED/",
            "OUTPUT_IMG=\"$2\"       # Fichier destination dans /sdcard/HEPHAISTOS/PACKED/",
            "CONFIG_DIR=\"$3\"       # Métadonnées dans /sdcard/HEPHAISTOS/Data/unpack/",
            "",
            "echo \"[SCRIPT] Démarrage du repack EROFS pour : \$TARGET_DIR\"",
            "if [ ! -d \"\$TARGET_DIR\" ]; then",
            "    echo \"[ERREUR] Dossier cible introuvable\" >&2",
            "    exit 1",
            "fi",
            "# Code d'assemblage utilisant les binaries kitchen...",
            "echo \"[SUCCESS] Repack achevé avec succès\"",
            "exit 0"
        )
        y = drawCodeBlock(c3, codeShellHeader, 40f, y, pageWidth - 80f)
        y += 12f

        drawSectionTitle(c3, "3.2 Conventions d'Arguments et Sortie Terminal", 40f, y)
        y += 18f
        val textScriptArgs = listOf(
            "• Argument 1 : Chemin absolu vers le dossier cible à traiter dans /sdcard/HEPHAISTOS/UNPACKED/.",
            "• Argument 2 : Chemin absolu du fichier ou dossier de sortie dans /sdcard/HEPHAISTOS/PACKED/.",
            "• Argument 3 : Chemin absolu du dossier des métadonnées (/sdcard/HEPHAISTOS/Data/unpack/).",
            "• Codes de sortie : 0 = Succès intégral, 1..255 = Échec (avec message redirigé sur stderr).",
            "• Redirection Terminal : Tous les échos 'echo' sont capturés et affichés en temps réel dans le Terminal."
        )
        y = drawTextParagraph(c3, textScriptArgs, 40f, y)

        drawPageFooter(c3, pageWidth, pageHeight, 3, 4)
        document.finishPage(page3)

        // ═══════════════════════════════════════════════════════════════════════
        // PAGE 4 : SPÉCIFICATION & MODÈLE DES WEBAPPS SATELLITES (HTML5 / JS)
        // ═══════════════════════════════════════════════════════════════════════
        val pageInfo4 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 4).create()
        val page4 = document.startPage(pageInfo4)
        val c4 = page4.canvas
        drawPageHeader(c4, pageWidth, "4. TEMPLATE DES WEBAPPS SATELLITES & JS BRIDGE", dateStr)

        y = 70f
        drawSectionTitle(c4, "4.1 Structure et Déploiement dans /sdcard/HEPHAISTOS/WebApps/", 40f, y)
        y += 18f
        val textWebAppIntro = listOf(
            "Une WebApp Satellite permet d'offrir une interface graphique riche (HTML5/CSS3/JavaScript) pour",
            "piloter des routines kitchen ou visualiser l'analyse d'un firmware. Chaque WebApp réside dans",
            "son propre sous-dossier contenant au minimum un fichier index.html et un manifest.json :"
        )
        y = drawTextParagraph(c4, textWebAppIntro, 40f, y)
        y += 8f

        val codeWebAppFiles = listOf(
            "/sdcard/HEPHAISTOS/WebApps/MonModuleWeb/",
            "  ├── index.html        (Point d'entrée de l'application)",
            "  ├── app.js            (Logique applicative et appels au Bridge)",
            "  ├── style.css         (Feuille de style Material 3 ou personnalisée)",
            "  └── manifest.json     (Identité du module : name, version, icon)"
        )
        y = drawCodeBlock(c4, codeWebAppFiles, 40f, y, pageWidth - 80f)
        y += 12f

        drawSectionTitle(c4, "4.2 API JavaScript Bridge 'window.HephaistosBridge'", 40f, y)
        y += 18f
        val textBridge = listOf(
            "HephaistOS injecte automatiquement l'objet JavaScript 'HephaistosBridge' dans le WebView de l'application :",
            "• HephaistosBridge.executeCommand(cmd) : Exécute une commande terminal et renvoie le log.",
            "• HephaistosBridge.getUnpackedProjects() : Retourne la liste JSON des dossiers dans UNPACKED/.",
            "• HephaistosBridge.readWorkspaceFile(path) : Lit le contenu d'un fichier de métadonnées.",
            "• HephaistosBridge.log(message) : Écrit directement un message horodaté dans le Terminal hôte."
        )
        y = drawTextParagraph(c4, textBridge, 40f, y)
        y += 8f

        val codeBridgeExample = listOf(
            "// Exemple d'interaction depuis app.js :",
            "async function lancerRepackWeb(dossierCible) {",
            "    console.log('Appel HephaistosBridge pour repack...');",
            "    if (window.HephaistosBridge) {",
            "        window.HephaistosBridge.log('[WEBAPP] Demande de repack pour ' + dossierCible);",
            "        window.HephaistosBridge.executeCommand('kitchen repack ' + dossierCible);",
            "    }",
            "}"
        )
        y = drawCodeBlock(c4, codeBridgeExample, 40f, y, pageWidth - 80f)

        drawPageFooter(c4, pageWidth, pageHeight, 4, 4)
        document.finishPage(page4)

        // ═══════════════════════════════════════════════════════════════════════
        // SAUVEGARDE DANS LE WORKSPACE & TÉLÉCHARGEMENT
        // ═══════════════════════════════════════════════════════════════════════
        val templatesDir = WorkspaceManager.getSubDir(context, "Templates")
        val pdfFile = File(templatesDir, PDF_FILE_NAME)
        FileOutputStream(pdfFile).use { out ->
            document.writeTo(out)
        }
        document.close()

        // Génération du Starter Kit ZIP complet
        generateStarterKitZip(templatesDir)

        // Copier également dans le dossier public Téléchargements si accessible
        try {
            val publicDownloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (publicDownloads.exists() && publicDownloads.canWrite()) {
                val copyFile = File(publicDownloads, PDF_FILE_NAME)
                pdfFile.copyTo(copyFile, overwrite = true)
            }
        } catch (_: Exception) {}

        return pdfFile
    }

    private fun drawPageHeader(canvas: Canvas, pageWidth: Int, title: String, dateStr: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Accent Bar
        paint.color = Color.rgb(230, 126, 34)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 10f, paint)

        // Title
        paint.color = Color.rgb(25, 27, 30)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("HephaistOS — Guide des Satellites (APK, Script, WebApp)", 40f, 32f, paint)

        // Subtitle
        paint.color = Color.rgb(120, 125, 135)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9.5f
        canvas.drawText("$title • $dateStr", 40f, 46f, paint)

        // Separator line
        paint.color = Color.rgb(225, 228, 235)
        canvas.drawLine(40f, 54f, (pageWidth - 40).toFloat(), 54f, paint)
    }

    private fun drawPageFooter(canvas: Canvas, pageWidth: Int, pageHeight: Int, pageNum: Int, totalPages: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(225, 228, 235)
        canvas.drawLine(40f, pageHeight - 34f, (pageWidth - 40).toFloat(), pageHeight - 34f, paint)

        paint.color = Color.rgb(130, 135, 145)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f
        canvas.drawText("HephaistOS Hôte Kitchen • Spécification Satellites • /sdcard/HEPHAISTOS/", 40f, pageHeight - 20f, paint)
        canvas.drawText("Page $pageNum / $totalPages", (pageWidth - 95).toFloat(), pageHeight - 20f, paint)
    }

    private fun drawSectionTitle(canvas: Canvas, title: String, x: Float, y: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(230, 126, 34)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11.5f
        canvas.drawText(title, x, y, paint)
    }

    private fun drawTextParagraph(canvas: Canvas, lines: List<String>, x: Float, startY: Float): Float {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.rgb(45, 50, 60)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 9f
        var y = startY
        for (line in lines) {
            canvas.drawText(line, x, y, paint)
            y += 12.5f
        }
        return y
    }

    private fun drawCodeBlock(canvas: Canvas, lines: List<String>, x: Float, startY: Float, width: Float): Float {
        val height = (lines.size * 10.5f) + 14f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Background
        paint.color = Color.rgb(243, 245, 249)
        canvas.drawRoundRect(x, startY, x + width, startY + height, 6f, 6f, paint)

        // Code text
        paint.color = Color.rgb(28, 33, 40)
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 7.5f
        var curY = startY + 12f
        for (line in lines) {
            canvas.drawText(line, x + 10f, curY, paint)
            curY += 10.5f
        }
        return startY + height
    }

    private fun generateStarterKitZip(destDir: File): File {
        val zipFile = File(destDir, STARTER_KIT_ZIP_NAME)
        FileOutputStream(zipFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. APK Satellite: AndroidManifest.xml
                val manifestContent = """
                    <?xml version="1.0" encoding="utf-8"?>
                    <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                        package="org.hephaistos.satellite.template">

                        <application
                            android:allowBackup="true"
                            android:icon="@mipmap/ic_launcher"
                            android:label="Satellite HephaistOS Template">

                            <activity
                                android:name=".MainActivity"
                                android:exported="true">
                                <intent-filter>
                                    <action android:name="android.intent.action.MAIN" />
                                    <!-- Marqueur officiel HephaistOS Addon / Satellite -->
                                    <action android:name="org.hephaistos.action.ADDON" />
                                    <category android:name="org.hephaistos.category.ADDON" />
                                    <category android:name="android.intent.category.DEFAULT" />
                                </intent-filter>
                                <meta-data
                                    android:name="org.hephaistos.addon.CAPABILITIES"
                                    android:value="gsi.unpack,rom.repack,payload.dumper,erofs.tools" />
                            </activity>
                        </application>
                    </manifest>
                """.trimIndent()
                zos.putNextEntry(ZipEntry("apk_satellite/AndroidManifest.xml"))
                zos.write(manifestContent.toByteArray())
                zos.closeEntry()

                // 2. APK Satellite: HephaistSatelliteClient.kt
                val kotlinContent = """
                    package org.hephaistos.satellite.template

                    object HephaistSatelliteClient {
                        const val WORKSPACE_PATH = "/sdcard/HEPHAISTOS/"
                        const val UNPACKED_DIR = "/sdcard/HEPHAISTOS/UNPACKED/"
                        const val PACKED_DIR = "/sdcard/HEPHAISTOS/PACKED/"
                        const val DATA_UNPACK_DIR = "/sdcard/HEPHAISTOS/Data/unpack/"

                        fun getCapabilities(): List<String> = listOf(
                            "gsi.unpack",
                            "rom.repack",
                            "payload.dumper",
                            "erofs.tools"
                        )
                    }
                """.trimIndent()
                zos.putNextEntry(ZipEntry("apk_satellite/HephaistSatelliteClient.kt"))
                zos.write(kotlinContent.toByteArray())
                zos.closeEntry()

                // 3. Shell Script Template
                val shContent = """
                    #!/system/bin/sh
                    # HEPHAISTOS_SCRIPT_V1
                    # NAME: Custom Partitions Repack
                    # DESCRIPTION: Script exemple de recompilation de partition
                    # REQUIRE_ROOT: true
                    # CAPABILITIES: unpack,repack

                    TARGET_DIR="${'$'}1"
                    OUTPUT_DIR="${'$'}2"
                    DATA_DIR="${'$'}3"

                    echo "[SCRIPT] Traitement de : ${'$'}TARGET_DIR vers ${'$'}OUTPUT_DIR"
                    exit 0
                """.trimIndent()
                zos.putNextEntry(ZipEntry("scripts/sample_repack.sh"))
                zos.write(shContent.toByteArray())
                zos.closeEntry()

                // 4. Python Script Template
                val pyContent = """
                    #!/usr/bin/env python3
                    # HEPHAISTOS_SCRIPT_V1
                    # NAME: Metadata Inspector
                    # DESCRIPTION: Analyse les métadonnées unpack sauvegardées dans Data/unpack/
                    # REQUIRE_ROOT: false
                    # CAPABILITIES: metadata.inspect

                    import sys, os, json

                    print("[PYTHON] Inspection des métadonnées HephaistOS...")
                    print("Arguments:", sys.argv)
                """.trimIndent()
                zos.putNextEntry(ZipEntry("scripts/sample_inspector.py"))
                zos.write(pyContent.toByteArray())
                zos.closeEntry()

                // 5. WebApp Satellite Template
                val htmlContent = """
                    <!DOCTYPE html>
                    <html lang="fr">
                    <head>
                        <meta charset="utf-8">
                        <title>Satellite WebApp HephaistOS</title>
                        <style>
                            body { font-family: sans-serif; background: #121418; color: #fff; padding: 20px; }
                            button { background: #e67e22; color: #fff; border: none; padding: 10px 18px; border-radius: 8px; font-weight: bold; cursor: pointer; }
                            .card { background: #1c2026; padding: 16px; border-radius: 12px; margin-top: 16px; }
                        </style>
                    </head>
                    <body>
                        <h2>Satellite WebApp HephaistOS</h2>
                        <div class="card">
                            <p>Contrôlez l'environnement hôte directement depuis cette WebApp.</p>
                            <button onclick="triggerAction()">Lancer Commande Hôte</button>
                        </div>
                        <script>
                            function triggerAction() {
                                if (window.HephaistosBridge) {
                                    window.HephaistosBridge.log('[WEBAPP] Action déclenchée depuis la WebApp !');
                                    window.HephaistosBridge.executeCommand('kitchen tools');
                                } else {
                                    alert('Bridge HephaistosBridge non disponible hors de HephaistOS');
                                }
                            }
                        </script>
                    </body>
                    </html>
                """.trimIndent()
                zos.putNextEntry(ZipEntry("webapps/sample_webapp/index.html"))
                zos.write(htmlContent.toByteArray())
                zos.closeEntry()

                // 6. README.md
                val readmeContent = """
                    # HephaistOS Starter Kit Satellites
                    Ce kit contient l'ensemble des squelettes et modèles nécessaires pour créer :
                    - Des APK Satellites Android
                    - Des Scripts Satellites (.sh et .py)
                    - Des WebApps Satellites (HTML5/JS avec HephaistosBridge)

                    Tous ces satellites communiquent harmonieusement avec l'application hôte HephaistOS via /sdcard/HEPHAISTOS/.
                """.trimIndent()
                zos.putNextEntry(ZipEntry("README.md"))
                zos.write(readmeContent.toByteArray())
                zos.closeEntry()
            }
        }
        return zipFile
    }

    fun openPdf(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Ouvrir le guide officiel Satellites HephaistOS"))
        } catch (_: Exception) {
            try {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Partager le guide PDF des Satellites"))
            } catch (_: Exception) {}
        }
    }
}
