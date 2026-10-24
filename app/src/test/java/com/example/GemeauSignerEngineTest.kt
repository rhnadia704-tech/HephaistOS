package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
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
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GemeauSignerEngineTest {

    @Test
    fun testKeysetGenerationAndApkSigningV1V2() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempDir = File(context.cacheDir, "gemeau_test_${System.currentTimeMillis()}").apply { mkdirs() }

        // 1. Generate Keyset
        val keysetInfo = GemeauSignerEngine.generateKeyset(
            context = context,
            keysetName = "CustomRomKeys",
            commonName = "CustomRom",
            organization = "HephaistOS",
            countryCode = "FR",
            algorithm = "RSA-2048",
            validityYears = 25,
            onProgress = { _, _ -> },
            onLog = { println(it) }
        )

        assertNotNull(keysetInfo)
        val platformFp = keysetInfo.certFingerprints["platform"]
        assertNotNull(platformFp)
        println("Generated Platform Key Fingerprint: $platformFp")

        // 2. Create sample test APK with classes.dex and resources.arsc (STORED mode)
        val testApk = File(tempDir, "Settings_test.apk")
        ZipOutputStream(FileOutputStream(testApk)).use { zos ->
            // classes.dex
            val dexData = "FAKE_DEX_BYTECODE_CONTENT_123456789".toByteArray()
            val dexEntry = ZipEntry("classes.dex")
            zos.putNextEntry(dexEntry)
            zos.write(dexData)
            zos.closeEntry()

            // resources.arsc in STORED mode
            val arscData = "SAMPLE_RESOURCES_ARSC_DATA".toByteArray()
            val arscEntry = ZipEntry("resources.arsc").apply {
                method = ZipEntry.STORED
                size = arscData.size.toLong()
                compressedSize = arscData.size.toLong()
                crc = java.util.zip.CRC32().apply { update(arscData) }.value
            }
            zos.putNextEntry(arscEntry)
            zos.write(arscData)
            zos.closeEntry()
        }

        assertTrue(testApk.exists() && testApk.length() > 0)

        // 3. Resign APK with platform key
        val signedApks = GemeauSignerEngine.signMultipleApks(
            context = context,
            apks = listOf(testApk),
            keysetDir = File(keysetInfo.dirPath),
            keyRole = "platform",
            onProgress = { _, _, _ -> },
            onLog = { println(it) }
        )

        assertEquals(1, signedApks.size)
        val signedApk = signedApks[0]
        assertTrue(signedApk.exists() && signedApk.length() > 0)

        // 4. Verify the newly signed APK with verifyApkSignature
        val verification = GemeauSignerEngine.verifyApkSignature(signedApk)
        println("Verification Result:")
        println("  isSigned: ${verification.isSigned}")
        println("  hasV1: ${verification.hasV1Scheme}")
        println("  hasV2: ${verification.hasV2Scheme}")
        println("  sha256: ${verification.sha256Fingerprint}")
        println("  subject: ${verification.subjectDn}")

        assertTrue("APK must be signed", verification.isSigned)
        assertTrue("APK must have v1 scheme", verification.hasV1Scheme)
        assertTrue("APK must have v2 scheme", verification.hasV2Scheme)
        assertEquals("Fingerprint must match generated Keyset platform certificate", platformFp, verification.sha256Fingerprint)

        // 5. Verify byte fidelity of classes.dex and compression of resources.arsc
        ZipFile(signedApk).use { zf ->
            val dexEntry = zf.getEntry("classes.dex")
            assertNotNull("classes.dex must be preserved", dexEntry)
            val extractedDex = zf.getInputStream(dexEntry).readBytes()
            assertEquals("classes.dex content must be 100% intact", "FAKE_DEX_BYTECODE_CONTENT_123456789", String(extractedDex))

            val arscEntry = zf.getEntry("resources.arsc")
            assertNotNull("resources.arsc must be preserved", arscEntry)
            assertEquals("resources.arsc must remain STORED", ZipEntry.STORED, arscEntry.method)
        }

        // 6. Test ART (.vdex / .odex) regeneration
        val oatDir = File(signedApk.parentFile, "oat/arm64").apply { mkdirs() }
        val dummyVdex = File(oatDir, "${signedApk.nameWithoutExtension}.vdex").apply { writeText("OLD_VDEX") }
        val dummyOdex = File(oatDir, "${signedApk.nameWithoutExtension}.odex").apply { writeBytes(ByteArray(64)) }

        GemeauSignerEngine.regenerateVdexAndOdex(signedApk) { println(it) }

        assertTrue(dummyVdex.exists())
        val vdexHeader = dummyVdex.readBytes().take(4).toByteArray()
        assertEquals("vdex", String(vdexHeader, Charsets.US_ASCII))

        // Clean up
        tempDir.deleteRecursively()
        }
    }

    @Test
    fun testPatchMacPermissionsXmlPreservesStructureAndReplacesTargetSigners() {
        val originalOemXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <!-- Custom OEM SELinux MMAC Policy definition -->
            <policy>
                <!-- Platform Key -->
                <signer signature="ORIGINAL_OLD_PLATFORM_HEX_SIG">
                    <seinfo value="platform" />
                </signer>

                <!-- Media Key -->
                <signer signature="ORIGINAL_OLD_MEDIA_HEX_SIG">
                    <seinfo value="media" />
                </signer>

                <!-- OEM Protected Telecom Service -->
                <signer signature="OFFICIAL_OEM_TELECOM_HEX_SIG">
                    <seinfo value="oem_telecom" />
                    <package name="com.oem.telecom">
                        <seinfo value="oem_telecom" />
                    </package>
                </signer>

                <!-- Shared Key -->
                <signer signature="ORIGINAL_OLD_SHARED_HEX_SIG">
                    <seinfo value="shared" />
                </signer>

                <default>
                    <seinfo value="default" />
                </default>
            </policy>
        """.trimIndent()

        val keysetHexMap = mapOf(
            "platform" to "NEW_HEPHAISTOS_PLATFORM_HEX_123456",
            "media" to "NEW_HEPHAISTOS_MEDIA_HEX_654321",
            "shared" to "NEW_HEPHAISTOS_SHARED_HEX_789012",
            "network_stack" to "NEW_HEPHAISTOS_NETSTACK_HEX_345678"
        )

        val patchedXml = GemeauSignerEngine.patchMacPermissionsXml(originalOemXml, keysetHexMap)

        // 1. Verify Platform signature is updated
        assertTrue("Platform signature must be updated", patchedXml.contains("NEW_HEPHAISTOS_PLATFORM_HEX_123456"))
        assertTrue("Platform seinfo must be preserved", patchedXml.contains("<seinfo value=\"platform\" />"))

        // 2. Verify Media signature is updated
        assertTrue("Media signature must be updated", patchedXml.contains("NEW_HEPHAISTOS_MEDIA_HEX_654321"))

        // 3. Verify Shared signature is updated
        assertTrue("Shared signature must be updated", patchedXml.contains("NEW_HEPHAISTOS_SHARED_HEX_789012"))

        // 4. Verify OEM Telecom signature is 100% PRESERVED
        assertTrue("OEM Telecom signature must remain intact", patchedXml.contains("OFFICIAL_OEM_TELECOM_HEX_SIG"))
        assertTrue("OEM Telecom package rule must remain intact", patchedXml.contains("<package name=\"com.oem.telecom\">"))

        // 5. Verify comments and header are preserved
        assertTrue("Comments must remain intact", patchedXml.contains("<!-- Custom OEM SELinux MMAC Policy definition -->"))
        assertTrue("Default tag must remain intact", patchedXml.contains("<seinfo value=\"default\" />"))
    }
}
