package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.UkaEngine
import com.example.data.WorkspaceManager
import com.example.model.RomTargetType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UkaEngineTest {

    @Test
    fun testSampleTargetRomCreationAndAnalysis() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkspaceManager.initializeWorkspace(context)

        val sampleFile = UkaEngine.createSampleTargetRom(context)
        assertTrue(sampleFile.exists())
        assertTrue(sampleFile.length() > 0)

        val metadata = UkaEngine.analyzeTarget(sampleFile)
        assertEquals(RomTargetType.SPARSE_IMG, metadata.targetType)
        assertTrue(metadata.isSparse)
        assertTrue(metadata.partitions.isNotEmpty())
    }

    @Test
    fun testUnpackAndRepackPipeline() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkspaceManager.initializeWorkspace(context)

        val sampleFile = UkaEngine.createSampleTargetRom(context)
        val metadata = UkaEngine.analyzeTarget(sampleFile)

        // 1. Unpack
        val unpackResult = UkaEngine.executeUnpack(
            context = context,
            targetFile = sampleFile,
            metadata = metadata,
            selectedPartitions = emptyList(),
            onProgress = { _, _, _ -> }
        )

        assertTrue(unpackResult.isSuccess)
        val unpackedDir = unpackResult.getOrNull()
        assertNotNull(unpackedDir)
        assertTrue(unpackedDir!!.exists())
        assertTrue(unpackedDir.isDirectory)

        // Check symlinks and fs_config
        val symlinks = File(unpackedDir, "symlinks.txt")
        val fsConfig = File(unpackedDir, "fs_config")
        assertTrue(symlinks.exists())
        assertTrue(fsConfig.exists())

        // Check metadata in Data/unpack/
        val unpackDataDir = WorkspaceManager.getUnpackDataDir(context)
        val metaJson = File(unpackDataDir, "${metadata.targetName}_meta.json")
        assertTrue(metaJson.exists())

        // 2. Repack
        val repackResult = UkaEngine.executeRepack(
            context = context,
            targetName = metadata.targetName,
            repackFormat = "AUTO",
            enableSparse = true,
            patchAvb = false,
            onProgress = { _, _, _ -> }
        )

        assertTrue(repackResult.isSuccess)
        val repackedFile = repackResult.getOrNull()
        assertNotNull(repackedFile)
        assertTrue(repackedFile!!.exists())
        assertTrue(repackedFile.length() > 0)

        // Verify packed location
        val packedDir = WorkspaceManager.getPackedDir(context)
        assertEquals(packedDir.absolutePath, repackedFile.parentFile?.absolutePath)
    }
}
