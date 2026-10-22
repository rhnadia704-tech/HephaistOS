package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.ExtensionManager
import com.example.data.WorkspaceManager
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
class AheExtensionTest {

    @Test
    fun testManifestParsing() {
        val json = """
        {
          "id": "com.test.ext.supertool",
          "name": "Super Tool",
          "version": "1.0.0",
          "author": "HephaistOS Dev",
          "description": "Test extension description",
          "minHephaistosVersion": 1,
          "permissions": ["STORAGE", "ROOT", "EXEC_BIN"],
          "exportedBinaries": ["tool_a", "tool_b"],
          "ui": {
            "hasDedicatedPage": true,
            "addToNavRail": false,
            "tabs": [
              {
                "id": "main_tab",
                "title": "Principal",
                "icon": "Bolt",
                "sections": [
                  {
                    "type": "form",
                    "title": "Action Test",
                    "fields": [
                      { "id": "proj", "type": "dropdown", "label": "Projet", "source": "unpacked_projects" }
                    ],
                    "action": {
                      "id": "run_test",
                      "label": "Lancer",
                      "script": "scripts/test.sh",
                      "requiresRoot": false
                    }
                  }
                ]
              }
            ]
          },
          "ipc": {
            "supportedCommands": ["ping", "run"],
            "dependencies": []
          }
        }
        """.trimIndent()

        val manifest = ExtensionManager.parseManifest(json)
        assertEquals("com.test.ext.supertool", manifest.id)
        assertEquals("Super Tool", manifest.name)
        assertEquals(3, manifest.permissions.size)
        assertEquals(2, manifest.exportedBinaries.size)
        assertEquals(1, manifest.ui.tabs.size)
        assertEquals("Principal", manifest.ui.tabs[0].title)
        assertEquals(1, manifest.ui.tabs[0].sections.size)
        assertEquals("run_test", manifest.ui.tabs[0].sections[0].action?.id)
    }

    @Test
    fun testStarterTemplateCreation() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkspaceManager.initializeWorkspace(context)

        val tplDir = WorkspaceManager.ensureAheStarterTemplate(context)
        assertTrue(tplDir.exists())
        assertTrue(tplDir.isDirectory)

        val manifestFile = File(tplDir, "manifest.json")
        assertTrue(manifestFile.exists())

        val scriptFile = File(tplDir, "scripts/main.sh")
        assertTrue(scriptFile.exists())

        val binFile = File(tplDir, "bin/starter_tool")
        assertTrue(binFile.exists())

        val parsed = ExtensionManager.parseManifest(manifestFile.readText())
        assertEquals("com.hephaistos.ext.starter", parsed.id)
        assertTrue(parsed.permissions.contains("STORAGE"))
    }
}
