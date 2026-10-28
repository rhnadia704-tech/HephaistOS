package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
import com.example.data.SignalabEngine
import com.example.data.WorkspaceManager
import com.example.model.SignalabSignMode
import com.example.model.SignalabSignOptions
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
class SignalabEngineTest {

    private fun createDummyApk(destFile: File) {
        ZipOutputStream(FileOutputStream(destFile)).use { zos ->
            val manifestData = "Fake AndroidManifest binary content".toByteArray()
            val entry1 = ZipEntry("AndroidManifest.xml").apply {
                method = ZipEntry.STORED
                size = manifestData.size.toLong()
                compressedSize = manifestData.size.toLong()
                crc = CRC32().apply { update(manifestData) }.value
            }
            zos.putNextEntry(entry1)
            zos.write(manifestData)
            zos.closeEntry()

            val dexData = "Fake classes.dex content".toByteArray()
            val entry2 = ZipEntry("classes.dex").apply {
                method = ZipEntry.STORED
                size = dexData.size.toLong()
                compressedSize = dexData.size.toLong()
                crc = CRC32().apply { update(dexData) }.value
            }
            zos.putNextEntry(entry2)
            zos.write(dexData)
            zos.closeEntry()
        }
    }

    private fun createDummyRomZip(destZip: File, apkFile1: File, apkFile2: File) {
        ZipOutputStream(FileOutputStream(destZip)).use { zos ->
            // Entry 1: system/app/SampleApp1/SampleApp1.apk
            val entry1 = ZipEntry("system/app/SampleApp1/SampleApp1.apk")
            zos.putNextEntry(entry1)
            zos.write(apkFile1.readBytes())
            zos.closeEntry()

            // Entry 2: system/priv-app/SampleApp2/SampleApp2.apk
            val entry2 = ZipEntry("system/priv-app/SampleApp2/SampleApp2.apk")
            zos.putNextEntry(entry2)
            zos.write(apkFile2.readBytes())
            zos.closeEntry()

            // Entry 3: system/etc/permissions/privapp-permissions-sample.xml
            val permData = "<permissions><privapp-permissions package=\"com.sample\"/></permissions>".toByteArray()
            val entry3 = ZipEntry("system/etc/permissions/privapp-permissions-sample.xml")
            zos.putNextEntry(entry3)
            zos.write(permData)
            zos.closeEntry()

            // Entry 4: system/app/SampleApp1/oat/arm64/SampleApp1.odex (stale ART file)
            val odexData = "STALE_ODEX_BYTECODE".toByteArray()
            val entry4 = ZipEntry("system/app/SampleApp1/oat/arm64/SampleApp1.odex")
            zos.putNextEntry(entry4)
            zos.write(odexData)
            zos.closeEntry()
        }
    }

    @Test
    fun testDetectContainerFormat() {
        val zipFile = File.createTempFile("test_rom", ".zip")
        zipFile.writeText("PK\u0003\u0004dummy")
        assertEquals("zip", SignalabEngine.detectContainerFormat(zipFile))
        zipFile.delete()
    }

    @Test
    fun testSignalabFullWorkflow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val tempDir = File(context.cacheDir, "signalab_test_${System.currentTimeMillis()}").apply { mkdirs() }

            // 1. Prepare two signed APKs
            val rawApk1 = File(tempDir, "Sample1.apk")
            createDummyApk(rawApk1)
            val rawApk2 = File(tempDir, "Sample2.apk")
            createDummyApk(rawApk2)

            val keysetInfo = GemeauSignerEngine.generateKeyset(
                context = context,
                keysetName = "SignalabBaseKey",
                commonName = "SignalabTarget",
                organization = "TargetDev",
                countryCode = "FR",
                algorithm = "RSA-2048",
                validityYears = 20,
                onProgress = { _, _ -> },
                onLog = {}
            )

            val signedApks = GemeauSignerEngine.signMultipleApks(
                context = context,
                apks = listOf(rawApk1, rawApk2),
                keysetDir = File(keysetInfo.dirPath),
                keyRole = "releasekey",
                onProgress = { _, _, _ -> },
                onLog = {}
            )
            assertEquals(2, signedApks.size)

            // 2. Package into a test ROM ZIP
            val targetRomZip = File(tempDir, "TargetRom.zip")
            createDummyRomZip(targetRomZip, signedApks[0], signedApks[1])
            assertTrue("ROM Zip must exist", targetRomZip.exists())

            // 3. Step 1: Unpack Target File
            val blueprint = SignalabEngine.unpackTargetFile(
                context = context,
                targetFile = targetRomZip,
                onProgress = { _, _ -> },
                onLog = { println(it) }
            )
            assertNotNull(blueprint)
            assertEquals("zip", blueprint.containerFormat)
            assertTrue("Total extracted files > 0", blueprint.totalExtractedFiles >= 4)
            val scanDataDir = File(blueprint.scanDataDirPath)
            assertTrue("repack_blueprint.json must exist", File(scanDataDir, "repack_blueprint.json").exists())
            assertTrue("filesystem_tree.json must exist", File(scanDataDir, "filesystem_tree.json").exists())

            // 4. Step 2: Scan and Cluster APK Signatures
            val scanResult = SignalabEngine.scanAndClusterApkSignatures(
                context = context,
                blueprint = blueprint,
                onProgress = { _, _ -> },
                onLog = { println(it) }
            )
            assertNotNull(scanResult)
            assertEquals(2, scanResult.blueprint.totalApksFound)
            assertTrue("Must detect at least 1 cluster", scanResult.clusters.isNotEmpty())
            assertTrue("apk_signature_clusters.json must exist", File(scanDataDir, "apk_signature_clusters.json").exists())
            assertTrue("key_mapping.json must exist", File(scanDataDir, "key_mapping.json").exists())
            assertTrue("permissions_database.json must exist", File(scanDataDir, "permissions_database.json").exists())
            assertTrue("signature_dependencies.json must exist", File(scanDataDir, "signature_dependencies.json").exists())

            // 5. Step 3: Resign and Repack with CLONE_SIGN
            val options = SignalabSignOptions(
                mode = SignalabSignMode.CLONE_SIGN,
                preserveGoogleApps = true,
                cleanArtArtifacts = true
            )
            val repackedFile = SignalabEngine.executeResignAndRepack(
                context = context,
                scanResult = scanResult,
                options = options,
                onProgress = { _, _ -> },
                onLog = { println(it) }
            )
            assertNotNull(repackedFile)
            assertTrue("Repacked file must exist", repackedFile.exists() && repackedFile.length() > 0)
            assertTrue("Repacked file must be in LABOUT", repackedFile.parentFile?.name == "LABOUT")
        }
    }
}
