package com.example

import com.example.data.AvbVerityEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
class AvbVerityEngineTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAvbFooterDetectionOnPlainFileReturnsFalse() {
        val plainFile = tempFolder.newFile("plain_ext4.img")
        plainFile.writeBytes(ByteArray(8192) { 0x55.toByte() })
        assertFalse(AvbVerityEngine.hasAvbFooter(plainFile))
    }

    @Test
    fun testProcessAndSignPartitionAvbCalculatesHashtreeAndAppendsValidFooter() = runBlocking {
        val testPartition = tempFolder.newFile("test_system.img")
        // Créer un fichier de partition de 64 Ko aligné sur 4 Ko
        val rawSize = 64 * 1024L
        RandomAccessFile(testPartition, "rw").use { raf ->
            raf.setLength(rawSize)
            raf.seek(0)
            raf.write("TEST_FILESYSTEM_BLOCK_DATA".toByteArray())
        }

        val logs = mutableListOf<String>()
        val success = AvbVerityEngine.processAndSignPartitionAvb(
            imageFile = testPartition,
            partitionName = "system",
            disableVerity = false,
            onLog = { logs.add(it) }
        )

        assertTrue("Le calcul AVB et dm-verity doit réussir: \n" + logs.joinToString("\n"), success)
        assertTrue("Le fichier doit contenir le footer AVB 2.0 officiel", AvbVerityEngine.hasAvbFooter(testPartition))

        val footer = AvbVerityEngine.readAvbFooter(testPartition)
        assertNotNull("Le footer AVB doit être décodable", footer)
        assertEquals(1, footer!!.versionMajor)
        assertEquals(2, footer.versionMinor)
        assertEquals(rawSize, footer.originalImageSize)
        assertTrue(footer.vbmetaOffset >= rawSize)
        assertTrue(footer.vbmetaSize > 0)
    }

    @Test
    fun testGenerateCompanionVbmeta() = runBlocking {
        val testPartition = tempFolder.newFile("test_vendor.img")
        val rawSize = 32 * 1024L
        RandomAccessFile(testPartition, "rw").use { raf ->
            raf.setLength(rawSize)
            raf.seek(0)
            raf.write("VENDOR_BLOCK_DATA".toByteArray())
        }

        AvbVerityEngine.processAndSignPartitionAvb(
            imageFile = testPartition,
            partitionName = "vendor",
            disableVerity = true
        )

        val vbmetaOutput = tempFolder.newFile("vbmeta_test.img")
        val footer = AvbVerityEngine.readAvbFooter(testPartition)
        assertNotNull(footer)

        val result = AvbVerityEngine.HashtreeCalculationResult(
            originalImageSize = footer!!.originalImageSize,
            treeOffset = footer.originalImageSize,
            treeSize = 4096L,
            salt = ByteArray(32) { 0x01 },
            rootDigest = ByteArray(32) { 0x02 },
            totalPartitionSize = testPartition.length()
        )

        AvbVerityEngine.generateCompanionVbmeta(
            outputVbmetaFile = vbmetaOutput,
            partitionName = "vendor",
            result = result,
            disableVerity = true
        )

        assertTrue(vbmetaOutput.exists())
        assertTrue(vbmetaOutput.length() >= 256)
        val magic = String(vbmetaOutput.readBytes().copyOfRange(0, 4), Charsets.US_ASCII)
        assertEquals("AVB0", magic)
    }
}
