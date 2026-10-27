package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
import com.example.data.KeycoreEngine
import com.example.data.VerisinEngine
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
class KeycoreEngineTest {

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

    @Test
    fun testKeymakerGeneration() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val keyset = KeycoreEngine.generateStandardKeymakerKeyset(
                context = context,
                keysetName = "TestAospKeys",
                commonName = "AOSP Release",
                organization = "Android Open Source",
                countryCode = "US",
                algorithm = "RSA-2048",
                validityYears = 25,
                onLog = { println(it) }
            )
            assertNotNull(keyset)
            val dir = File(keyset.dirPath)
            assertTrue("Keymaker keyset dir must exist", dir.exists())

            // Releasekey must be present
            assertTrue("releasekey.pk8 must exist", File(dir, "releasekey.pk8").exists())
            assertTrue("releasekey.x509.pem must exist", File(dir, "releasekey.x509.pem").exists())

            // Platform, shared, media should also be generated
            assertTrue("platform.pk8 must exist", File(dir, "platform.pk8").exists())
            assertTrue("shared.pk8 must exist", File(dir, "shared.pk8").exists())
            assertTrue("media.pk8 must exist", File(dir, "media.pk8").exists())
        }
    }

    @Test
    fun testVerisinKeyAndCloneWorkflow() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val tempDir = File(context.cacheDir, "keycore_test_${System.currentTimeMillis()}").apply { mkdirs() }
            val dummyApk = File(tempDir, "TargetSample.apk")
            createDummyApk(dummyApk)

            // Sign dummy APK using Gemeau
            val baseKeyset = GemeauSignerEngine.generateKeyset(
                context = context,
                keysetName = "OriginalTargetKey",
                commonName = "TargetDev",
                organization = "OriginalCorp",
                countryCode = "ES",
                algorithm = "RSA-2048",
                validityYears = 15,
                onProgress = { _, _ -> },
                onLog = {}
            )
            val signedApks = GemeauSignerEngine.signMultipleApks(
                context = context,
                apks = listOf(dummyApk),
                keysetDir = File(baseKeyset.dirPath),
                keyRole = "releasekey",
                onProgress = { _, _, _ -> },
                onLog = {}
            )
            assertEquals(1, signedApks.size)
            val signedApk = signedApks[0]
            assertTrue("Signed APK must exist", signedApk.exists() && signedApk.length() > 0)

            // Step 1: Analyze APK with Keycore
            val analysis = KeycoreEngine.analyzeApkForTarget(context, signedApk) { println(it) }
            assertNotNull("Analysis must succeed", analysis)
            assertTrue("Subject must contain OriginalCorp", analysis.subjectDn.contains("OriginalCorp"))
            assertEquals("RSA", analysis.keyType)
            assertEquals(2048, analysis.keySizeBits)

            // Step 2: Create Verisin Key (same structure, custom identity)
            val verisinKey = KeycoreEngine.createVerisinKey(
                context = context,
                targetAnalysis = analysis,
                keysetName = "CustomVerisinKey",
                commonName = "CustomVerisinDev",
                organization = "CustomVerisinOrg",
                countryCode = "FR",
                validityYears = 20,
                onLog = { println(it) }
            )
            assertNotNull(verisinKey)
            assertTrue(File(verisinKey.dirPath, "releasekey.pk8").exists())

            // Step 3: Create Verisin Clone (replicates original Subject DN)
            val cloneKeyset = KeycoreEngine.createVerisinClone(
                context = context,
                targetAnalysis = analysis,
                cloneKeysetName = "CloneOfTarget",
                onLog = { println(it) }
            )
            assertNotNull(cloneKeyset)
            val cloneDir = File(cloneKeyset.dirPath)
            assertTrue(File(cloneDir, "releasekey.pk8").exists())
            assertTrue(File(cloneDir, "releasekey.x509.pem").exists())

            // Step 4: Resign APK using Clone Key
            val resignedApk = KeycoreEngine.resignApkWithCloneKey(
                context = context,
                inputApk = signedApk,
                cloneKeysetDir = cloneDir,
                onLog = { println(it) }
            )
            assertNotNull("Resigned clone APK must not be null", resignedApk)
            assertTrue("Resigned clone APK must exist", resignedApk!!.exists() && resignedApk.length() > 0)

            // Step 5: Verify resigned clone APK with Verisin
            val audit = VerisinEngine.verifyApkFile(context, resignedApk) { println(it) }
            assertTrue("Verisin verification of clone must succeed", audit.isSigned)
            assertTrue("Must contain certificate", audit.certificates.isNotEmpty())
            val cloneCert = audit.certificates.first()
            assertTrue("Clone cert must mirror original Subject", cloneCert.subjectDn.contains("OriginalCorp"))
        }
    }
}
