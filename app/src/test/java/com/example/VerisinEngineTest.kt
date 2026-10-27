package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
import com.example.data.VerisinEngine
import com.example.data.WorkspaceManager
import com.example.model.Screen
import com.example.ui.HephaistViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VerisinEngineTest {

    @Test
    fun testVerisinHelpBanner() {
        val banner = VerisinEngine.getHelpBanner()
        assertTrue("Help banner must not be empty", banner.isNotEmpty())
        val fullText = banner.joinToString("\n")
        assertTrue("Banner must mention VERISIN", fullText.contains("VERISIN Core Engine"))
        assertTrue("Banner must document verisin-help", fullText.contains("verisin-help"))
        assertTrue("Banner must document verisin command", fullText.contains("verisin") && fullText.contains("fichier.apk"))
        assertTrue("Banner must mention V1, V2, V3", fullText.contains("Schéma V1") && fullText.contains("Schéma V2") && fullText.contains("Schéma V3"))
    }

    @Test
    fun testVerisinDeepApkVerification() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = File(context.cacheDir, "verisin_test_${System.currentTimeMillis()}").apply { mkdirs() }

        // 1. Generate Keyset with RSA-2048
        val keysetInfo = GemeauSignerEngine.generateKeyset(
            context = context,
            keysetName = "VerisinKeyset",
            commonName = "VerisinTest",
            organization = "HephaistOS",
            countryCode = "FR",
            algorithm = "RSA-2048",
            validityYears = 25,
            onProgress = { _, _ -> },
            onLog = { println(it) }
        )
        assertNotNull(keysetInfo)

        // 2. Build a valid test APK with classes.dex and resources.arsc
        val rawApk = File(tempDir, "SystemUI_test.apk")
        ZipOutputStream(FileOutputStream(rawApk)).use { zos ->
            val dexData = "DEX_BYTECODE_FOR_VERISIN_TEST".toByteArray()
            zos.putNextEntry(ZipEntry("classes.dex"))
            zos.write(dexData)
            zos.closeEntry()

            val arscData = "ARSC_RESOURCES_DATA".toByteArray()
            val arscEntry = ZipEntry("resources.arsc").apply {
                method = ZipEntry.STORED
                size = arscData.size.toLong()
                compressedSize = arscData.size.toLong()
                crc = CRC32().apply { update(arscData) }.value
            }
            zos.putNextEntry(arscEntry)
            zos.write(arscData)
            zos.closeEntry()
        }

        // 3. Sign APK with V1 + V2 + V3
        val signedApks = GemeauSignerEngine.signMultipleApks(
            context = context,
            apks = listOf(rawApk),
            keysetDir = File(keysetInfo.dirPath),
            keyRole = "platform",
            onProgress = { _, _, _ -> },
            onLog = { println(it) }
        )
        assertEquals(1, signedApks.size)
        val signedApk = signedApks[0]
        assertTrue(signedApk.exists() && signedApk.length() > 0)

        // 4. Test VerisinEngine.verifyApkFile
        val result = VerisinEngine.verifyApkFile(context, signedApk) { println(it) }

        assertTrue("APK must be signed", result.isSigned)
        println("VERISIN Detected Schemes: ${result.schemesDisplay}")
        assertTrue("Must detect V1", result.signatureSchemes.contains("V1"))
        assertTrue("Must detect V2", result.signatureSchemes.contains("V2"))
        assertTrue("Must detect V3", result.signatureSchemes.contains("V3"))
        assertEquals("V1 + V2 + V3", result.schemesDisplay)

        assertEquals("Must have 1 certificate", 1, result.certificates.size)
        val cert = result.certificates[0]

        println("VERISIN Certificate Details:")
        println("  Type: ${cert.certType}")
        println("  Version: ${cert.certVersion}")
        println("  Serial: ${cert.serialNumberDecimal}")
        println("  Subject: ${cert.subjectDn}")
        println("  Issuer: ${cert.issuerDn}")
        println("  Validity: ${cert.validityFromFormatted}")
        println("  Alg: ${cert.sigAlgName}")
        println("  HashCode: ${cert.hashCodeFormatted}")
        println("  CRC32: ${cert.crc32Formatted}")
        println("  MD5: ${cert.md5Hex}")
        println("  SHA-1: ${cert.sha1Hex}")
        println("  SHA-224: ${cert.sha224Hex}")
        println("  SHA-256: ${cert.sha256Hex}")
        println("  SHA-384: ${cert.sha384Hex}")
        println("  SHA-512: ${cert.sha512Hex}")
        println("  PubKey: ${cert.publicKeyFormatted.take(60)}...")
        println("  SigBits: ${cert.signatureBitsHex.take(60)}...")
        println("  SigBase64: ${cert.signatureBase64.take(60)}...")

        assertEquals("X.509", cert.certType)
        assertEquals(3, cert.certVersion)
        assertTrue(cert.serialNumberDecimal.isNotBlank())
        assertTrue(cert.subjectDn.contains("VerisinTest"))
        assertTrue(cert.isSelfSigned)
        assertTrue(cert.hashCodeFormatted.startsWith("0x"))
        assertTrue(cert.crc32Formatted.isNotBlank())
        assertEquals(32, cert.md5Hex.length)
        assertEquals(40, cert.sha1Hex.length)
        assertEquals(56, cert.sha224Hex.length)
        assertEquals(64, cert.sha256Hex.length)
        assertEquals(96, cert.sha384Hex.length)
        assertEquals(128, cert.sha512Hex.length)
        assertTrue(cert.certBase64.isNotBlank())
        assertTrue(cert.signatureBitsHex.isNotBlank())
        assertTrue("Public key must have OpenSSL format", cert.publicKeyFormatted.startsWith("OpenSSLRSAPublicKey{modulus="))
        assertTrue("Signature base64 must not be empty", cert.signatureBase64.isNotBlank())

        // 5. Verify the formatted report text matches the user's exact template
        val report = result.rawReportText
        assertTrue(report.contains("Vérification de la signature:"))
        assertTrue(report.contains("Schéma de signature : V1 + V2 + V3"))
        assertTrue(report.contains("CERTIFICAT №1"))
        assertTrue(report.contains("Type de certificat : X.509"))
        assertTrue(report.contains("Version du certificat : 3"))
        assertTrue(report.contains("Numéros de série: ${cert.serialNumberDecimal}"))
        assertTrue(report.contains("Propriétaire du certificat : ${cert.subjectDn}"))
        assertTrue(report.contains("Émetteur de certificat : Auto-signé par le propriétaire du certificat"))
        assertTrue(report.contains("Code de hachage : ${cert.hashCodeFormatted}"))
        assertTrue(report.contains("Signature CRC32 : ${cert.crc32Formatted}"))
        assertTrue(report.contains("Signature MD5 : ${cert.md5Hex}"))
        assertTrue(report.contains("Signature SHA-1 : ${cert.sha1Hex}"))
        assertTrue(report.contains("Signature SHA-224 : ${cert.sha224Hex}"))
        assertTrue(report.contains("Signature SHA-256 : ${cert.sha256Hex}"))
        assertTrue(report.contains("Signature SHA-384 : ${cert.sha384Hex}"))
        assertTrue(report.contains("Signature SHA-512 : ${cert.sha512Hex}"))
        assertTrue(report.contains("Base64 du certificat : ${cert.certBase64}"))
        assertTrue(report.contains("Bits de signature : ${cert.signatureBitsHex}"))
        assertTrue(report.contains("Clé publique : ${cert.publicKeyFormatted}"))
        assertTrue(report.contains("Base64 de signature : ${cert.signatureBase64}"))

        // 6. Verify automatic report file saving
        assertNotNull("Report file should be generated", result.reportFile)
        assertTrue("Report file must exist", result.reportFile?.exists() == true)
        assertNotNull("JSON file should be generated", result.jsonFile)
        assertTrue("JSON file must exist", result.jsonFile?.exists() == true)

        // 7. Test WorkspaceManager.resolveApkPath
        val resolvedDirect = WorkspaceManager.resolveApkPath(context, signedApk.absolutePath)
        assertNotNull("Direct path should resolve", resolvedDirect)
        assertEquals(signedApk.absolutePath, resolvedDirect?.absolutePath)

        // Clean up
        tempDir.deleteRecursively()
        }
    }

    @Test
    fun testTerminalIntegrationVerisinCommands() {
        runBlocking {
            val app = ApplicationProvider.getApplicationContext<Application>()
            val viewModel = HephaistViewModel(app)

            // Test 1: verisin-help
            viewModel.executeTerminalCommand("verisin-help")
            val entries1 = viewModel.uiState.value.terminalEntries
            assertTrue(entries1.any { it.text.contains("VERISIN Core Engine") })
            assertTrue(entries1.any { it.text.contains("verisin") })

            // Test 2: verisin (no args -> shows help)
            viewModel.executeTerminalCommand("verisin")
            val entries2 = viewModel.uiState.value.terminalEntries
            assertTrue(entries2.any { it.text.contains("VERISIN Core Engine") })

            // Test 3: verisin non-existent file with angle brackets (strips < > automatically)
            viewModel.executeTerminalCommand("verisin </tmp/non_existent_app_12345.apk>")
            val entries3 = viewModel.uiState.value.terminalEntries
            assertTrue(entries3.any { it.text.contains("[ERREUR VERISIN]") && it.text.contains("'/tmp/non_existent_app_12345.apk'") })

            // Test 4: WorkspaceManager.sanitizePath
            assertEquals("SystemUI.apk", WorkspaceManager.sanitizePath("<SystemUI.apk>"))
            assertEquals("/sdcard/HEPHAISTOS/SystemUI.apk", WorkspaceManager.sanitizePath("</sdcard/HEPHAISTOS/SystemUI.apk>"))
            assertEquals("/path/to/app.apk", WorkspaceManager.sanitizePath("\"/path/to/app.apk\""))

            // Test 5: UI state and methods
            viewModel.setVerisinPath("/sdcard/HEPHAISTOS/test.apk")
            assertEquals("/sdcard/HEPHAISTOS/test.apk", viewModel.uiState.value.verisinSelectedPath)
            viewModel.selectScreen(Screen.VERISIN)
            assertEquals(Screen.VERISIN, viewModel.uiState.value.currentScreen)
        }
    }
}

