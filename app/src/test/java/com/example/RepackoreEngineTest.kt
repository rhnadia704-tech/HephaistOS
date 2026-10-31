package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GemeauSignerEngine
import com.example.data.KeycoreEngine
import com.example.data.RepackoreEngine
import com.example.model.SignalabScanBlueprint
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepackoreEngineTest {

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
    fun testInspectUnpackedSchemaDynamicMargin() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testDir = File(context.cacheDir, "test_repackore_schema_${System.currentTimeMillis()}").apply { mkdirs() }

        val apk1 = File(testDir, "app1.apk")
        createDummyApk(apk1)
        val file2 = File(testDir, "test.txt").apply { writeText("Hello World Repackore") }

        val schema = RepackoreEngine.inspectUnpackedSchema(testDir, "ext4", "system")

        assertEquals("system", schema.partitionName)
        assertEquals(2, schema.totalFiles)
        assertEquals(1, schema.apks.size)
        assertTrue(schema.totalBytes > 0)
        assertTrue(schema.targetSizeWithMargin > schema.totalBytes)
        // Margin must be at least +10%
        assertTrue(schema.targetSizeWithMargin >= (schema.totalBytes * 1.10).toLong())

        testDir.deleteRecursively()
    }

    @Test
    fun testCompilePartitionImageZipWithResignedApk() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val baseDir = File(context.cacheDir, "test_repackore_compile_${System.currentTimeMillis()}").apply { mkdirs() }
            val unpackedDir = File(baseDir, "UNPACKED_system").apply { mkdirs() }

            // Create APK and sign it with a key
            val unsignedApk = File(unpackedDir, "TestApp.apk")
            createDummyApk(unsignedApk)

            val keyset = GemeauSignerEngine.generateKeyset(
                context = context,
                keysetName = "test_repackore_key",
                commonName = "Repackore",
                organization = "TestOrg",
                countryCode = "FR",
                algorithm = "RSA-2048",
                validityYears = 25,
                onProgress = { _, _ -> },
                onLog = {}
            )

            val signed = GemeauSignerEngine.signMultipleApks(
                context = context,
                apks = listOf(unsignedApk),
                keysetDir = File(keyset.dirPath),
                keyRole = "releasekey",
                onProgress = { _, _, _ -> },
                onLog = {}
            )
            assertTrue(signed.isNotEmpty())
            signed.first().copyTo(unsignedApk, overwrite = true)

            val blueprint = SignalabScanBlueprint(
                sourceFilePath = "",
                sourceFileName = "system.zip",
                sourceFileSize = 1024L,
                containerFormat = "zip",
                partitionName = "system",
                unpackedDirPath = unpackedDir.absolutePath,
                scanTimestamp = System.currentTimeMillis(),
                totalExtractedFiles = 1
            )

            val outputFile = File(baseDir, "system_resigned.zip")
            RepackoreEngine.compilePartitionImage(
                context = context,
                unpackedDir = unpackedDir,
                blueprint = blueprint,
                outputFile = outputFile
            )

            assertTrue("Le fichier compilé doit exister", outputFile.exists())
            assertTrue("La taille doit être non nulle", outputFile.length() > 0)

            // Audit the compiled image
            val verified = RepackoreEngine.verifyCompiledImageSignatures(context, outputFile)
            assertTrue("L'audit de la partition compilée doit réussir", verified)

            baseDir.deleteRecursively()
        }
    }

    @Test
    fun testAssembleAutonomousExt4Image() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val baseDir = File(context.cacheDir, "test_repackore_ext4_${System.currentTimeMillis()}").apply { mkdirs() }
            val unpackedDir = File(baseDir, "UNPACKED_vendor").apply { mkdirs() }

            val apk = File(unpackedDir, "VendorApp.apk")
            createDummyApk(apk)

            val vendorKeyset = GemeauSignerEngine.generateKeyset(
                context = context,
                keysetName = "vendor_key",
                commonName = "VendorRepackore",
                organization = "VendorOrg",
                countryCode = "FR",
                algorithm = "RSA-2048",
                validityYears = 25,
                onProgress = { _, _ -> },
                onLog = {}
            )
            val signed = GemeauSignerEngine.signMultipleApks(
                context = context,
                apks = listOf(apk),
                keysetDir = File(vendorKeyset.dirPath),
                keyRole = "releasekey",
                onProgress = { _, _, _ -> },
                onLog = {}
            )
            signed.first().copyTo(apk, overwrite = true)

            val outputFile = File(baseDir, "vendor_resigned.img")
            val schema = RepackoreEngine.inspectUnpackedSchema(unpackedDir, "ext4", "vendor")
            val blueprint = SignalabScanBlueprint(
                sourceFilePath = "",
                sourceFileName = "vendor.img",
                sourceFileSize = 1024L,
                containerFormat = "ext4",
                partitionName = "vendor",
                unpackedDirPath = unpackedDir.absolutePath,
                scanTimestamp = System.currentTimeMillis(),
                totalExtractedFiles = schema.totalFiles
            )

            RepackoreEngine.assembleAutonomousExt4Image(
                unpackedDir = unpackedDir,
                schema = schema,
                blueprint = blueprint,
                outputFile = outputFile,
                onLog = {}
            )

            assertTrue("L'image de partition EXT4 doit exister", outputFile.exists())
            assertTrue("L'image doit faire au moins la taille allouée", outputFile.length() >= schema.targetSizeWithMargin)

            // Verify EXT4 superblock magic (0xEF53) at offset 1024 + 56 = 1080
            RandomAccessFile(outputFile, "r").use { raf ->
                raf.seek(1024 + 56)
                val magic = raf.readUnsignedShort()
                // In little endian, 0xEF53 is read as 0x53EF with readUnsignedShort (which is big-endian)
                assertTrue("Magic EXT4 attendu à l'offset 1080", magic == 0x53EF || magic == 0xEF53)
            }

            // Verify that the signed APK was injected into the image sectors
            val verified = RepackoreEngine.verifyCompiledImageSignatures(context, outputFile)
            assertTrue("L'audit de signature sur le bloc EXT4 assemblé doit être validé", verified)

            baseDir.deleteRecursively()
        }
    }

    @Test
    fun testResolvePayloadDirectoryAndExcludeMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val baseDir = File(context.cacheDir, "test_repackore_nested_${System.currentTimeMillis()}").apply { mkdirs() }
        
        // Structure typique UNPACKED/Test/Test/system/...
        val outerDir = File(baseDir, "Test").apply { mkdirs() }
        val nestedPayload = File(outerDir, "Test").apply { mkdirs() }
        val systemDir = File(nestedPayload, "system").apply { mkdirs() }
        File(systemDir, "build.prop").writeText("ro.build.version.release=14")

        // Fichiers techniques au niveau supérieur (doivent être exclus)
        File(outerDir, "symlinks.txt").writeText("system/bin/sh -> /bin/toybox")
        File(outerDir, "fs_config").writeText("system 0 0 755")
        File(outerDir, "file_contexts").writeText("/system(/.*)? u:object_r:system_file:s0")

        // 1. Vérifier que resolvePayloadDirectory cible bien le sous-dossier direct contenant les dossiers système
        val resolved = RepackoreEngine.resolvePayloadDirectory(outerDir, "Test")
        assertEquals("Le payload doit pointer sur le sous-dossier Test/ contenant system/", nestedPayload.absolutePath, resolved.absolutePath)

        // 2. Vérifier que inspectUnpackedSchema exclut les fichiers de métadonnées
        val schema = RepackoreEngine.inspectUnpackedSchema(outerDir, "ext4", "Test")
        assertEquals("Seul le vrai payload system/build.prop doit être comptabilisé", 1, schema.totalFiles)

        baseDir.deleteRecursively()
    }
}

