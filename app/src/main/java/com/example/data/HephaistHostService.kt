package com.example.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.hephaistos.ipc.IHephaistHostService
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class HephaistHostService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        const val ACTION_HOST_IPC = "org.hephaistos.service.HOST_IPC"
        const val PERMISSION_HEPHAISTOS_HOST = "org.hephaistos.permission.HOST_ACCESS"

        private val listeners = mutableListOf<(String, String) -> Unit>()

        fun addTerminalLogListener(listener: (String, String) -> Unit) {
            synchronized(listeners) {
                listeners.add(listener)
            }
        }

        fun removeTerminalLogListener(listener: (String, String) -> Unit) {
            synchronized(listeners) {
                listeners.remove(listener)
            }
        }

        fun broadcastTerminalLog(senderTag: String, message: String) {
            synchronized(listeners) {
                for (l in listeners) {
                    try {
                        l(senderTag, message)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private val binder = object : IHephaistHostService.Stub() {

        override fun executeHostTool(toolId: String?, jsonParams: String?): String {
            val safeToolId = toolId ?: "unknown"
            val response = JSONObject()

            try {
                when (safeToolId) {
                    "workspace.stat" -> {
                        val stats = WorkspaceManager.initializeWorkspace(applicationContext)
                        val statsJson = JSONObject()
                        stats.forEach { (k, v) -> statsJson.put(k, v) }
                        response.put("status", "SUCCESS")
                        response.put("data", statsJson)
                        response.put("rootPath", WorkspaceManager.getWorkspaceRoot(applicationContext).absolutePath)
                    }

                    "root.check" -> {
                        val rootStatus = RootChecker.checkRoot()
                        response.put("status", "SUCCESS")
                        response.put("isRooted", rootStatus.isRooted)
                        response.put("binaryPath", rootStatus.binaryPath ?: "")
                        response.put("statusText", rootStatus.statusText)
                    }

                    "root.exec" -> {
                        val params = JSONObject(jsonParams ?: "{}")
                        val command = params.optString("command", "")
                        if (command.isBlank()) {
                            response.put("status", "ERROR")
                            response.put("message", "Paramètre 'command' manquant")
                        } else {
                            val output = StringBuilder()
                            var exitCode = -1
                            try {
                                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                                val reader = BufferedReader(InputStreamReader(process.inputStream))
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    output.append(line).append("\n")
                                }
                                exitCode = process.waitFor()
                                response.put("status", if (exitCode == 0) "SUCCESS" else "ERROR")
                                response.put("exitCode", exitCode)
                                response.put("output", output.toString())
                            } catch (e: Exception) {
                                response.put("status", "ERROR")
                                response.put("message", e.message ?: "Échec d'exécution su")
                            }
                        }
                    }

                    "uka.status" -> {
                        response.put("status", "SUCCESS")
                        response.put("engine", "UkaEngine v2.5")
                        response.put("features", JSONArray(listOf("sparse.unpack", "erofs.unpack", "ext4.repack", "payload.bin", "dsu.ready")))
                    }

                    else -> {
                        response.put("status", "ERROR")
                        response.put("message", "Outil inconnu : $safeToolId")
                    }
                }
            } catch (e: Exception) {
                response.put("status", "ERROR")
                response.put("message", e.message ?: "Exception interne hôte")
            }

            return response.toString()
        }

        override fun getWorkspaceRootPath(): String {
            return WorkspaceManager.getWorkspaceRoot(applicationContext).absolutePath
        }

        override fun logToHostTerminal(senderTag: String?, message: String?) {
            val tag = senderTag ?: "SATELLITE"
            val msg = message ?: ""
            broadcastTerminalLog(tag, msg)
        }

        override fun getEcosystemRegistryJson(): String {
            val root = JSONObject()
            val tools = AddonRegistry.getAllSharedTools()
            val toolsObj = JSONObject()
            for ((pkg, list) in tools) {
                val arr = JSONArray()
                list.forEach { arr.put(it) }
                toolsObj.put(pkg, arr)
            }
            root.put("sharedTools", toolsObj)
            return root.toString()
        }

        override fun isHostRootAvailable(): Boolean {
            return RootChecker.checkRoot().isRooted
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
}
