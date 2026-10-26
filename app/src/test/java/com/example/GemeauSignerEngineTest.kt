package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
import com.example.data.WorkspaceManager
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

    @Test
    fun testRSignerEndToEndPipeline() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val testTargetDir = File(context.cacheDir, "test_rom_target_${System.currentTimeMillis()}").apply { mkdirs() }

            // 1. Create a dummy base keyset to create initially signed APKs
            val baseKeyset = GemeauSignerEngine.generateKeyset(
                context = context,
                keysetName = "OriginalRomKeys",
                commonName = "OriginalAosp",
                organization = "OriginalOem",
                countryCode = "US",
                algorithm = "RSA 2048",
                validityYears = 25,
                onProgress = { _, _ -> },
                onLog = {}
            )

            // 2. Create sample APKs representing a target ROM:
            // a) Settings.apk (Platform)
            val settingsApk = File(testTargetDir, "system/priv-app/Settings.apk").apply { parentFile?.mkdirs() }
            ZipOutputStream(FileOutputStream(settingsApk)).use { zos ->
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write("SETTINGS_DEX_BYTES".toByteArray())
                zos.closeEntry()
            }
            val signedSettings = GemeauSignerEngine.signMultipleApks(context, listOf(settingsApk), File(baseKeyset.dirPath), "platform", { _, _, _ -> }, {}).first()
            signedSettings.copyTo(settingsApk, overwrite = true)

            // b) MediaProvider.apk (Media)
            val mediaApk = File(testTargetDir, "system/priv-app/MediaProvider.apk").apply { parentFile?.mkdirs() }
            ZipOutputStream(FileOutputStream(mediaApk)).use { zos ->
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write("MEDIA_DEX_BYTES".toByteArray())
                zos.closeEntry()
            }
            val signedMedia = GemeauSignerEngine.signMultipleApks(context, listOf(mediaApk), File(baseKeyset.dirPath), "media", { _, _, _ -> }, {}).first()
            signedMedia.copyTo(mediaApk, overwrite = true)

            // c) GoogleServicesFramework.apk (Protected GApps)
            val gmsApk = File(testTargetDir, "system/priv-app/com.google.android.gsf.apk").apply { parentFile?.mkdirs() }
            ZipOutputStream(FileOutputStream(gmsApk)).use { zos ->
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write("GSF_DEX_BYTES".toByteArray())
                zos.closeEntry()
            }
            val signedGms = GemeauSignerEngine.signMultipleApks(context, listOf(gmsApk), File(baseKeyset.dirPath), "releasekey", { _, _, _ -> }, {}).first()
            signedGms.copyTo(gmsApk, overwrite = true)
            val originalGsfCertFp = GemeauSignerEngine.verifyApkSignature(gmsApk).sha256Fingerprint

            // d) mac_permissions.xml
            val macXml = File(testTargetDir, "system/etc/selinux/mac_permissions.xml").apply { parentFile?.mkdirs() }
            macXml.writeText("""
                <policy>
                    <signer signature="${baseKeyset.certFingerprints["platform"]}">
                        <seinfo value="platform" />
                    </signer>
                    <default>
                        <seinfo value="default" />
                    </default>
                </policy>
            """.trimIndent())

            // 3. Test Phase 1: analyzeTargetForRSigner
            val detectedGroups = GemeauSignerEngine.analyzeTargetForRSigner(
                targetFile = testTargetDir,
                context = context,
                onProgress = { _, _ -> },
                onLog = { println(it) }
            )

            assertTrue("Should detect at least 2 groups", detectedGroups.size >= 2)
            println("Detected Groups in R-Signer:")
            detectedGroups.forEach {
                println(" - ${it.groupName} (${it.apkCount} APKs, schemes: ${it.schemesDisplay})")
            }

            // 4. Test Phase 2: generateKeysetForDetectedGroups (1:1 mapping)
            val newKeyset1to1 = GemeauSignerEngine.generateKeysetForDetectedGroups(
                context = context,
                keysetName = "NewHephaistKeyset",
                commonName = "NewHephaistOS",
                organization = "Community",
                countryCode = "FR",
                algorithm = "RSA 2048",
                validityYears = 25,
                detectedGroups = detectedGroups,
                onLog = { println(it) }
            )
            assertNotNull(newKeyset1to1)
            assertTrue(File(newKeyset1to1.dirPath, "platform.pk8").exists())
            assertTrue(File(newKeyset1to1.dirPath, "media.pk8").exists())
            assertTrue(File(newKeyset1to1.dirPath, "avb.pk8").exists())

            // 5. Test Phase 3-6: executeRSignerPipeline
            val report = GemeauSignerEngine.executeRSignerPipeline(
                context = context,
                targetFile = testTargetDir,
                keysetDir = File(newKeyset1to1.dirPath),
                withVbmeta = true,
                preserveGapps = true,
                onProgress = { step, p, proc, tot -> println("[$p] $step ($proc/$tot)") },
                onLog = { println(it) }
            )

            assertNotNull(report)
            assertEquals(1, report.preservedGappsCount) // GSF was preserved!
            assertTrue(report.hasVbmetaGenerated)
            assertTrue(File(report.signedRomPath).exists())

            // 6. Verify GApps signature was strictly preserved for SafetyNet
            val postGsfCertFp = GemeauSignerEngine.verifyApkSignature(gmsApk).sha256Fingerprint
            assertEquals("GApps signature must be preserved 100% identically for SafetyNet", originalGsfCertFp, postGsfCertFp)

            // 7. Verify Settings.apk was signed with V1 + V2 + V3
            val settingsVerif = GemeauSignerEngine.verifyApkSignature(settingsApk)
            println("Settings.apk after R-Signer:")
            println("  v1: ${settingsVerif.hasV1Scheme}")
            println("  v2: ${settingsVerif.hasV2Scheme}")
            println("  v3: ${settingsVerif.hasV3Scheme}")
            println("  sha256: ${settingsVerif.sha256Fingerprint}")
            assertTrue("Settings.apk must have V1 scheme", settingsVerif.hasV1Scheme)
            assertTrue("Settings.apk must have V2 scheme", settingsVerif.hasV2Scheme)
            assertTrue("Settings.apk must have V3 scheme", settingsVerif.hasV3Scheme)
            assertEquals("Settings.apk fingerprint must match new platform key", newKeyset1to1.certFingerprints["platform"], settingsVerif.sha256Fingerprint)

            // 8. Verify RSIGNER/Data modular sub-directories and rich outputs
            val dataDbDir = WorkspaceManager.getSignerRSignerDatabaseDir(context)
            val dataPermsDir = WorkspaceManager.getSignerRSignerPermissionsDir(context)
            val dataMappingsDir = WorkspaceManager.getSignerRSignerMappingsDir(context)
            val dataReportsDir = WorkspaceManager.getSignerRSignerReportsDir(context)

            val dbJsonFile = File(dataDbDir, "target_signatures_db.json")
            assertTrue("target_signatures_db.json must exist in Data/database/", dbJsonFile.exists())
            val dbContent = dbJsonFile.readText()
            assertTrue("Database must contain packages array", dbContent.contains("\"packages\""))
            assertTrue("Database must record signature schemes", dbContent.contains("V1") && dbContent.contains("V2"))

            assertTrue("signatures_by_group.json must exist in Data/database/", File(dataDbDir, "signatures_by_group.json").exists())
            assertTrue("mac_permissions.xml must exist in Data/permissions/", File(dataPermsDir, "mac_permissions.xml").exists())
            assertTrue("mac_permissions_patched.xml must exist in Data/permissions/", File(dataPermsDir, "mac_permissions_patched.xml").exists())
            assertTrue("signature_redistribution_map.json must exist in Data/mappings/", File(dataMappingsDir, "signature_redistribution_map.json").exists())
            assertTrue("gapps_preservation_manifest.json must exist in Data/mappings/", File(dataMappingsDir, "gapps_preservation_manifest.json").exists())
            assertTrue("target_audit_deep_report.md must exist in Data/reports/", File(dataReportsDir, "target_audit_deep_report.md").exists())
            assertTrue("rsigner_audit_report.md must exist in Data/reports/", File(dataReportsDir, "rsigner_audit_report.md").exists())

            // Clean up
            testTargetDir.deleteRecursively()
        }
    }
}

