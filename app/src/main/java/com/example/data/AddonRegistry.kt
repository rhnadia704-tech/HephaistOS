package com.example.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.model.AppType
import com.example.model.CompatibleApp

object AddonRegistry {

    const val ACTION_HEPHAISTOS_ADDON = "org.hephaistos.action.ADDON"
    const val METADATA_CAPABILITIES = "org.hephaistos.addon.CAPABILITIES"

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
     * Scans ONLY real applications installed on the phone:
     * 1. Official HephaistOS addons/satellites declaring ACTION_HEPHAISTOS_ADDON
     * 2. Any non-system applications installed by the user on the device (excluding emulator test tools)
     */
    fun scanInstalledAddons(context: Context): List<CompatibleApp> {
        val discovered = mutableListOf<CompatibleApp>()
        val pm = context.packageManager
        val seenPackages = mutableSetOf<String>()

        // 1. Check for specific HephaistOS Addons / Satellites
        try {
            val addonIntent = Intent(ACTION_HEPHAISTOS_ADDON)
            val resolveList = pm.queryIntentActivities(addonIntent, 0)
            for (resolveInfo in resolveList) {
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
                val pkgName = appInfo.packageName
                if (pkgName == context.packageName || seenPackages.contains(pkgName)) continue
                val label = pm.getApplicationLabel(appInfo).toString()
                if (isIgnoredPackage(pkgName, label)) continue
                seenPackages.add(pkgName)

                val version = try {
                    pm.getPackageInfo(pkgName, 0).versionName ?: "v1.0"
                } catch (_: Exception) {
                    "v1.0"
                }

                var capabilities = listOf("addon.standard")
                try {
                    val fullInfo = pm.getApplicationInfo(pkgName, PackageManager.GET_META_DATA)
                    val metaCaps = fullInfo.metaData?.getString(METADATA_CAPABILITIES)
                    if (!metaCaps.isNullOrBlank()) {
                        capabilities = metaCaps.split(",").map { it.trim() }
                    }
                } catch (_: Exception) {}

                for (cap in capabilities) {
                    registerTool(pkgName, cap)
                }

                discovered.add(
                    CompatibleApp(
                        id = "addon_$pkgName",
                        name = label,
                        packageName = pkgName,
                        version = version,
                        description = "Satellite HephaistOS détecté. Outils : ${capabilities.joinToString(", ")}",
                        category = "Satellite Natif",
                        isHero = false,
                        isInstalled = true,
                        isHeavyTask = false,
                        status = "Connecté",
                        statusColor = 0xFF00E676,
                        iconKey = "extension",
                        appType = AppType.NATIVE_ADDON,
                        capabilities = capabilities
                    )
                )
            }
        } catch (_: Exception) {}

        // 2. Discover user-installed non-system launchable apps
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val launchableList = pm.queryIntentActivities(mainIntent, 0)
            for (resolveInfo in launchableList) {
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: continue
                val pkgName = appInfo.packageName
                if (pkgName == context.packageName || seenPackages.contains(pkgName)) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!isSystem) {
                    val label = pm.getApplicationLabel(appInfo).toString()
                    if (isIgnoredPackage(pkgName, label)) continue
                    seenPackages.add(pkgName)

                    val version = try {
                        pm.getPackageInfo(pkgName, 0).versionName ?: "v1.0"
                    } catch (_: Exception) {
                        "v1.0"
                    }

                    discovered.add(
                        CompatibleApp(
                            id = "app_$pkgName",
                            name = label,
                            packageName = pkgName,
                            version = version,
                            description = "Application installée ($pkgName)",
                            category = "App Installée",
                            isHero = false,
                            isInstalled = true,
                            isHeavyTask = false,
                            status = "Prêt",
                            statusColor = 0xFF4CAF50,
                            iconKey = "build",
                            appType = AppType.NATIVE_ADDON,
                            capabilities = listOf("app.launch")
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return discovered
    }
}
