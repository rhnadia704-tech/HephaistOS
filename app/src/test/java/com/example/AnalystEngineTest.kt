package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.AnalystEngine
import com.example.data.GemeauSignerEngine
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AnalystEngineTest {

    @Test
    fun testAnalystFullTargetInspectionAndReportGeneration() {
        runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testTargetDir = File(context.cacheDir, "unpacked_sample_rom_${System.currentTimeMillis()}").apply { mkdirs() }

        // 1. Create simulated ROM structure
        val systemPrivAppDir = File(testTargetDir, "system_ext/priv-app/SystemUI").apply { mkdirs() }
        val vendorHwDir = File(testTargetDir, "vendor/lib64/hw").apply { mkdirs() }
        val systemLibDir = File(testTargetDir, "system/lib64").apply { mkdirs() }

        // Generate keyset for signing SystemUI.apk
        val keyset = GemeauSignerEngine.generateKeyset(
            context = context,
            keysetName = "LunarisKeyset",
            commonName = "Lunaris",
            organization = "Lunaris",
            countryCode = "ES",
            algorithm = "RSA-2048",
            validityYears = 25,
            onProgress = { _, _ -> },
            onLog = {}
        )

        // Create unsigned SystemUI.apk
        val rawApk = File(systemPrivAppDir, "SystemUI_raw.apk")
        ZipOutputStream(FileOutputStream(rawApk)).use { zos ->
            val dexEntry = ZipEntry("classes.dex")
            zos.putNextEntry(dexEntry)
            zos.write("DEX_CODE_PAYLOAD_TEST".toByteArray())
            zos.closeEntry()

            val xmlEntry = ZipEntry("AndroidManifest.xml")
            zos.putNextEntry(xmlEntry)
            zos.write("MANIFEST_BINARY_XML_TEST".toByteArray())
            zos.closeEntry()
        }

        // Sign with GemeauSignerEngine
        val signedApks = GemeauSignerEngine.signMultipleApks(
            context = context,
            apks = listOf(rawApk),
            keysetDir = File(keyset.dirPath),
            keyRole = "platform",
            onProgress = { _, _, _ -> },
            onLog = {}
        )
        assertEquals(1, signedApks.size)
        val signedProduced = signedApks[0]
        val signedApk = File(systemPrivAppDir, "SystemUI.apk")
        signedProduced.copyTo(signedApk, overwrite = true)
        rawApk.delete()

        // Create simulated HAL libraries (ELF format)
        val audioHal = File(vendorHwDir, "audio.primary.default.so")
        writeFakeElf(audioHal, is64Bit = true, isAarch64 = true)

        val powerHal = File(systemLibDir, "android.hardware.power-service.so")
        writeFakeElf(powerHal, is64Bit = true, isAarch64 = true)

        val otherBlob = File(systemLibDir, "libcutils.so")
        writeFakeElf(otherBlob, is64Bit = true, isAarch64 = true)

        // 2. Run AnalystEngine scan
        val logs = mutableListOf<String>()
        val summary = AnalystEngine.analyzeTarget(
            context = context,
            targetDir = testTargetDir,
            onProgress = { _, _ -> },
            onLog = { msg, _ -> logs.add(msg) }
        )

        assertNotNull(summary)
        assertEquals("Target name should match directory", testTargetDir.name, summary.targetName)
        assertTrue("Total files should be >= 4", summary.totalFiles >= 4)
        assertTrue("Total APKs should be 1", summary.totalApks >= 1)
        assertTrue("Signed APK count should be 1", summary.signedApksCount >= 1)
        assertTrue("HALs found should be >= 2", summary.totalHalsFound >= 2)

        // 3. Verify all 5 modular database files in export directory
        val exportDir = File(summary.exportDir)
        assertTrue("Export directory should exist", exportDir.exists())

        // a) tree.json
        val treeJson = File(exportDir, "tree.json")
        assertTrue("tree.json must exist", treeJson.exists())
        val treeArray = JSONArray(treeJson.readText())
        assertTrue("tree.json should contain entries", treeArray.length() >= 4)

        // b) signatures.json
        val signaturesJson = File(exportDir, "signatures.json")
        assertTrue("signatures.json must exist", signaturesJson.exists())
        val sigArray = JSONArray(signaturesJson.readText())
        assertEquals("signatures.json should have 1 apk entry", 1, sigArray.length())
        val apk0 = sigArray.getJSONObject(0)
        assertTrue("Should be marked signed", apk0.getBoolean("isSigned"))
        assertEquals("Interface v16 (36)", apk0.getString("interfaceVersion"))

        // c) signatures_by_group.json
        val sigByGroupJson = File(exportDir, "signatures_by_group.json")
        assertTrue("signatures_by_group.json must exist", sigByGroupJson.exists())
        val groupsObj = JSONObject(sigByGroupJson.readText())
        assertEquals(1, groupsObj.getInt("totalGroups"))
        assertEquals(1, groupsObj.getInt("totalSignedApks"))
        val group0 = groupsObj.getJSONArray("groups").getJSONObject(0)
        assertTrue(group0.getString("groupName").contains("Lunaris"))
        assertEquals(1, group0.getInt("apkCount"))

        // d) signature_report.txt (User exact required format)
        val sigReport = File(exportDir, "signature_report.txt")
        assertTrue("signature_report.txt must exist", sigReport.exists())
        val reportContent = sigReport.readText()

        assertTrue("Must include interface line", reportContent.contains("Vérification de la signature: Interface v16 (36)"))
        assertTrue("Must include Chemin line", reportContent.contains("Chemin: /system_ext/priv-app/SystemUI/SystemUI.apk"))
        assertTrue("Must include Schéma de signature with V1 and V2", reportContent.contains("Schéma de signature\u00a0: V1 + V2"))
        assertTrue("Must include CERTIFICAT №1", reportContent.contains("CERTIFICAT №1"))
        assertTrue("Must include Type de certificat", reportContent.contains("Type de certificat\u00a0: X.509"))
        assertTrue("Must include Code de hachage", reportContent.contains("Code de hachage\u00a0: 0x"))
        assertTrue("Must include Signature CRC32", reportContent.contains("Signature CRC32\u00a0:"))
        assertTrue("Must include Signature MD5", reportContent.contains("Signature MD5\u00a0:"))
        assertTrue("Must include Signature SHA-1", reportContent.contains("Signature SHA-1\u00a0:"))
        assertTrue("Must include Signature SHA-224", reportContent.contains("Signature SHA-224\u00a0:"))
        assertTrue("Must include Signature SHA-256", reportContent.contains("Signature SHA-256\u00a0:"))
        assertTrue("Must include Signature SHA-384", reportContent.contains("Signature SHA-384\u00a0:"))
        assertTrue("Must include Signature SHA-512", reportContent.contains("Signature SHA-512\u00a0:"))
        assertTrue("Must include Base64 du certificat", reportContent.contains("Base64 du certificat\u00a0:"))
        assertTrue("Must include Bits de signature", reportContent.contains("Bits de signature\u00a0:"))
        assertTrue("Must include Clé publique", reportContent.contains("Clé publique\u00a0: OpenSSLRSAPublicKey"))
        assertTrue("Must include Base64 de signature", reportContent.contains("Base64 de signature\u00a0:"))

        // e) hals_blobs.json
        val halsJson = File(exportDir, "hals_blobs.json")
        assertTrue("hals_blobs.json must exist", halsJson.exists())
        val halsArray = JSONArray(halsJson.readText())
        assertTrue("hals_blobs.json should contain items", halsArray.length() >= 3)

        // f) summary_manifest.json
        val summaryJson = File(exportDir, "summary_manifest.json")
        assertTrue("summary_manifest.json must exist", summaryJson.exists())
        val summaryObj = JSONObject(summaryJson.readText())
        assertEquals(summary.totalFiles, summaryObj.getInt("totalFiles"))
        assertEquals(1, summaryObj.getInt("totalApks"))
        assertEquals(1, summaryObj.getInt("signedApksCount"))
        assertTrue(summaryObj.getJSONArray("files").length() == 6)

        // Clean up
        testTargetDir.deleteRecursively()
        exportDir.deleteRecursively()
        }
    }

    @Test
    fun testMultiSchemeV1V2V3Detection() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val apkFile = File(context.cacheDir, "sample_v1_v2_v3.apk")

        // Construct synthetic APK with V1 and APK Signing block having V2 (0x7109871a) and V3 (0xf05368c0)
        val dummyCertDer = byteArrayOf(
            0x30, 0x82.toByte(), 0x01, 0x00,
            0x30, 0x82.toByte(), 0x00, 0x80.toByte()
        ) + ByteArray(250)

        // Build signers block
        val neededSize = 24 + dummyCertDer.size
        val signerData = ByteBuffer.allocate(neededSize).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        signerData.putInt(neededSize - 4) // signers sequence length
        signerData.putInt(neededSize - 8) // signer length
        signerData.putInt(neededSize - 12) // signed data length
        signerData.putInt(0)   // digests length = 0
        signerData.putInt(dummyCertDer.size + 4) // certs sequence length
        signerData.putInt(dummyCertDer.size) // cert length
        signerData.put(dummyCertDer)

        val v2Value = signerData.array()
        val v3Value = signerData.array().copyOf()

        // Build pairs
        val pairV2 = ByteBuffer.allocate(8 + 4 + v2Value.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putLong(4L + v2Value.size)
            .putInt(0x7109871a) // V2
            .put(v2Value)
            .array()

        val pairV3 = ByteBuffer.allocate(8 + 4 + v3Value.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putLong(4L + v3Value.size)
            .putInt(-0x0fac9740) // V3 (0xf05368c0.toInt())
            .put(v3Value)
            .array()

        val pairsBytes = pairV2 + pairV3
        val blockSize = pairsBytes.size.toLong() + 24L
        val magic = "APK Sig Block 42".toByteArray(Charsets.US_ASCII)

        val apkSigBlock = ByteBuffer.allocate((blockSize + 8).toInt()).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            .putLong(blockSize)
            .put(pairsBytes)
            .putLong(blockSize)
            .put(magic)
            .array()

        // Create zip with central directory and EOCD
        java.io.FileOutputStream(apkFile).use { fos ->
            // Minimal zip entry: local file header
            val entryData = "TEST_PAYLOAD".toByteArray()
            val lfh = ByteBuffer.allocate(30 + 8 + entryData.size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(0x04034b50) // LFH signature
                .putShort(20) // version
                .putShort(0)  // flags
                .putShort(0)  // method: stored
                .putShort(0).putShort(0) // time/date
                .putInt(12345) // crc
                .putInt(entryData.size) // comp size
                .putInt(entryData.size) // uncomp size
                .putShort(8)  // name length
                .putShort(0)  // extra length
                .put("test.txt".toByteArray())
                .put(entryData)
                .array()
            fos.write(lfh)

            // Inject APK Signing Block
            fos.write(apkSigBlock)

            val cdOffset = (lfh.size + apkSigBlock.size)

            // Central directory header
            val cdh = ByteBuffer.allocate(46 + 8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(0x02014b50) // CD signature
                .putShort(20).putShort(20)
                .putShort(0).putShort(0).putShort(0).putShort(0)
                .putInt(12345)
                .putInt(entryData.size).putInt(entryData.size)
                .putShort(8).putShort(0).putShort(0)
                .putShort(0).putShort(0).putInt(0).putInt(0)
                .put("test.txt".toByteArray())
                .array()
            fos.write(cdh)

            // EOCD
            val eocd = ByteBuffer.allocate(22).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                .putInt(0x06054b50)
                .putShort(0).putShort(0)
                .putShort(1).putShort(1)
                .putInt(cdh.size)
                .putInt(cdOffset)
                .putShort(0)
                .array()
            fos.write(eocd)
        }

        val detectedSchemes = mutableListOf<String>()
        AnalystEngine.extractCertsFromApkSigningBlock(apkFile, detectedSchemes)

        assertTrue("V2 should be detected", detectedSchemes.contains("V2"))
        assertTrue("V3 should be detected", detectedSchemes.contains("V3"))

        val sorted = AnalystEngine.sortSchemes(listOf("V1", "V2", "V3"))
        assertEquals("V1 + V2 + V3", sorted.joinToString(" + "))

        apkFile.delete()
    }

    private fun writeFakeElf(file: File, is64Bit: Boolean, isAarch64: Boolean) {
        val bytes = ByteArray(64)
        bytes[0] = 0x7F.toByte()
        bytes[1] = 'E'.code.toByte()
        bytes[2] = 'L'.code.toByte()
        bytes[3] = 'F'.code.toByte()
        bytes[4] = (if (is64Bit) 2 else 1).toByte() // EI_CLASS
        bytes[5] = 1.toByte() // EI_DATA (Little Endian)
        bytes[6] = 1.toByte() // EI_VERSION

        // Machine at offset 18
        if (isAarch64) {
            bytes[18] = 183.toByte()
            bytes[19] = 0.toByte()
        } else {
            bytes[18] = 40.toByte() // ARM
            bytes[19] = 0.toByte()
        }
        file.writeBytes(bytes)
    }
}
