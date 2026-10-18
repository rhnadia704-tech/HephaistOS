package com.example.data

import com.example.model.RootStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

object RootDelegator {

    private val delegatedPackages = mutableSetOf<String>()

    fun grantRootToPackage(packageName: String): Boolean {
        delegatedPackages.add(packageName)
        return true
    }

    fun revokeRootFromPackage(packageName: String): Boolean {
        return delegatedPackages.remove(packageName)
    }

    fun isRootDelegated(packageName: String): Boolean {
        return delegatedPackages.contains(packageName)
    }

    fun getDelegatedPackages(): List<String> = delegatedPackages.toList()

    suspend fun executePrivilegedCommand(
        command: String,
        onOutputLine: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let { onOutputLine(it) }
            }

            while (errorReader.readLine().also { line = it } != null) {
                line?.let { onOutputLine("[STDERR] $it") }
            }

            process.waitFor()
        } catch (e: Exception) {
            onOutputLine("[ROOT EXCEPTION] Échec de l'accès su : ${e.message}")
            -1
        }
    }

    suspend fun executeScript(
        scriptFile: File,
        useRoot: Boolean,
        onOutputLine: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        if (!scriptFile.exists()) {
            onOutputLine("[ERREUR] Script introuvable : ${scriptFile.absolutePath}")
            return@withContext -1
        }

        try {
            val cmd = if (scriptFile.name.endsWith(".py")) {
                "python3 \"${scriptFile.absolutePath}\""
            } else {
                "sh \"${scriptFile.absolutePath}\""
            }

            if (useRoot) {
                executePrivilegedCommand(cmd, onOutputLine)
            } else {
                val process = Runtime.getRuntime().exec(arrayOf("/system/bin/sh", scriptFile.absolutePath))
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { onOutputLine(it) }
                }
                while (errorReader.readLine().also { line = it } != null) {
                    line?.let { onOutputLine("[STDERR] $it") }
                }
                process.waitFor()
            }
        } catch (e: Exception) {
            onOutputLine("[EXECUTION ERREUR] ${e.message}")
            -1
        }
    }
}
