package com.example.data

import com.example.model.RootStatus
import java.io.File

object RootChecker {
    private val SU_PATHS = listOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su"
    )

    fun checkRoot(): RootStatus {
        val details = mutableListOf<String>()
        var foundBinary: String? = null

        // 1. Check physical binary paths
        for (path in SU_PATHS) {
            try {
                val file = File(path)
                if (file.exists()) {
                    foundBinary = path
                    details.add("Binaire su localisé : $path")
                    break
                }
            } catch (e: Exception) {
                // Ignore permission denial on path access
            }
        }

        // 2. Check 'which su' or test execution
        var canExecuteSu = false
        try {
            val process = Runtime.getRuntime().exec(arrayOf("which", "su"))
            val exitCode = process.waitFor()
            if (exitCode == 0) {
                canExecuteSu = true
                details.add("Commande 'which su' a retourné un chemin valide.")
            }
        } catch (e: Exception) {
            details.add("Exécution directe 'which su' non disponible.")
        }

        val isRooted = foundBinary != null || canExecuteSu

        val statusText = if (isRooted) {
            "Accès Root Détecté et Actif"
        } else {
            "Non Rooté (Environnement Sandbox Sécurisé)"
        }

        if (!isRooted) {
            details.add("Aucun binaire SuperUtilisateur (su/Magisk/KernelSU) détecté dans les chemins système standard.")
            details.add("HephaistOS opère actuellement avec les privilèges utilisateur standard Android.")
        }

        return RootStatus(
            isRooted = isRooted,
            statusText = statusText,
            binaryPath = foundBinary,
            details = details
        )
    }
}
