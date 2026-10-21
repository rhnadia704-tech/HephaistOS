package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.example.model.AppType
import com.example.model.CompatibleApp

object AddonRegistry {

    private const val TAG = "AddonRegistry"

    const val ACTION_HEPHAISTOS_ADDON = "org.hephaistos.action.ADDON"
    const val ACTION_HEPHAISTOS_SATELLITE = "org.hephaistos.action.SATELLITE"
    const val SERVICE_HEPHAISTOS_SATELLITE = "org.hephaistos.service.SATELLITE_IPC"

    const val METADATA_SATELLITE_V1 = "org.hephaistos.satellite.V1"
    const val METADATA_CAPABILITIES = "org.hephaistos.addon.CAPABILITIES"
    const val METADATA_ROLE = "org.hephaistos.satellite.ROLE"
    const val METADATA_COLOR_ACCENT = "org.hephaistos.satellite.ACCENT_COLOR"

    private val sharedEcosystemTools = mutableMapOf<String, MutableList<String>>()

    fun registerTool(providerPackage: String, toolIdentifier: String) {
        val list = sharedEcosystemTools.getOrPut(providerPackage) { mutableListOf() }
        if (!list.contains(toolIdentifier)) {
            list.add(toolIdentifier)
        }
    }

    fun getAllSharedTools(): Map<String, List<String>> = sharedEcosystemTools

    private fun isIgnoredPackage(packageName: String, label: String): Boolean {
        val lowerPkg = packageName.lowercase()
        val lowerLabel = label.lowercase()
        if (lowerPkg.contains("mtaas") || lowerLabel.contains("mtaas")) return true
        if (lowerPkg.contains("google.android.apps.mtaas")) return true
        if (lowerPkg.startsWith("com.android.test") || lowerPkg.startsWith("androidx.test")) return true
        if (lowerPkg.contains("emulator") || lowerPkg.contains("goldfish") || lowerPkg.contains("ranchu")) return true
        if (lowerPkg == "com.android.shell" || lowerPkg == "com.android.cts") return true
        if (lowerPkg == "com.android.fallback" || lowerPkg == "com.android.traceur") return true
        return false
    }

    /**
     * Scans and auto-discovers all HephaistOS Satellites installed on the device using
     * multi-vector resolution:
     * 1. Activities declaring ACTION_HEPHAISTOS_SATELLITE or ACTION_HEPHAISTOS_ADDON
     * 2. Services declaring SERVICE_HEPHAISTOS_SATELLITE or ACTION_HEPHAISTOS_SATELLITE
     * 3. Applications declaring metadata org.hephaistos.satellite.V1 or org.hephaistos.addon.CAPABILITIES
     * 4. Applications matching naming scheme com.hephaistos.* or org.hephaistos.*
     */
    fun scanInstalledAddons(context: Context): List<CompatibleApp> {
        val discovered = mutableListOf<CompatibleApp>()
        val pm = context.packageManager
        val seenPackages = mutableSetOf<String>()

        val actionsToScan = listOf(
            ACTION_HEPHAISTOS_SATELLITE,
            ACTION_HEPHAISTOS_ADDON,
            SERVICE_HEPHAISTOS_SATELLITE
        )

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PackageManager.MATCH_ALL
        } else {
            0
        }

        // 1. Check Activities with Intent Filters
        for (action in actionsToScan) {
            try {
                val intent = Intent(action)
                val resolveList = pm.queryIntentActivities(intent, flags)
                for (resolveInfo in resolveList) {
                    val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
                    inspectAndRegisterPackage(context, pm, appInfo, seenPackages, discovered)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Erreur queryIntentActivities pour $action: ${e.message}")
            }
        }

        // 2. Check Services with Intent Filters
        for (action in actionsToScan) {
            try {
                val intent = Intent(action)
                val serviceList = pm.queryIntentServices(intent, flags)
                for (resolveInfo in serviceList) {
                    val appInfo = resolveInfo.serviceInfo?.applicationInfo ?: continue
                    inspectAndRegisterPackage(context, pm, appInfo, seenPackages, discovered)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Erreur queryIntentServices pour $action: ${e.message}")
            }
        }

        // 3. Check All Installed Applications for HephaistOS Meta-Data DNA or Package Naming
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (appInfo in installedApps) {
                val pkgName = appInfo.packageName
                if (seenPackages.contains(pkgName) || pkgName == context.packageName) continue

                val metaData = appInfo.metaData
                val isSatelliteByMeta = metaData != null && (
                    metaData.getBoolean(METADATA_SATELLITE_V1, false) ||
                    metaData.containsKey(METADATA_CAPABILITIES) ||
                    metaData.containsKey(METADATA_ROLE)
                )

                val isSatelliteByPackage = pkgName.startsWith("org.hephaistos.") ||
                        pkgName.startsWith("com.hephaistos.") ||
                        pkgName.contains(".hephaistos.")

                if (isSatelliteByMeta || isSatelliteByPackage) {
                    inspectAndRegisterPackage(context, pm, appInfo, seenPackages, discovered)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erreur inspection getInstalledApplications: ${e.message}")
        }

        return discovered
    }

    private fun inspectAndRegisterPackage(
        context: Context,
        pm: PackageManager,
        appInfo: ApplicationInfo,
        seenPackages: MutableSet<String>,
        outputList: MutableList<CompatibleApp>
    ) {
        val pkgName = appInfo.packageName
        if (pkgName == context.packageName || seenPackages.contains(pkgName)) return

        val label = try {
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            pkgName
        }

        if (isIgnoredPackage(pkgName, label)) return
        seenPackages.add(pkgName)

        val version = try {
            pm.getPackageInfo(pkgName, 0).versionName ?: "v1.0"
        } catch (_: Exception) {
            "v1.0"
        }

        var capabilities = listOf("satellite.standard")
        var role = "Module Satellite"

        try {
            val fullInfo = pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
            val metaData = fullInfo.metaData
            if (metaData != null) {
                val metaCaps = metaData.getString(METADATA_CAPABILITIES)
                if (!metaCaps.isNullOrBlank()) {
                    capabilities = metaCaps.split(",").map { it.trim() }
                }
                val metaRole = metaData.getString(METADATA_ROLE)
                if (!metaRole.isNullOrBlank()) {
                    role = metaRole
                }
            }
        } catch (_: Exception) {}

        for (cap in capabilities) {
            registerTool(pkgName, cap)
        }

        outputList.add(
            CompatibleApp(
                id = "satellite_$pkgName",
                name = label,
                packageName = pkgName,
                version = version,
                description = "$role synchronisé via HSAS v1.0. Outils : ${capabilities.joinToString(", ")}",
                category = "Satellite HephaistOS",
                isHero = false,
                isInstalled = true,
                isHeavyTask = false,
                status = "Connecté (HSAS)",
                statusColor = 0xFF00E676,
                iconKey = "extension",
                appType = AppType.NATIVE_ADDON,
                capabilities = capabilities
            )
        )
    }
}
