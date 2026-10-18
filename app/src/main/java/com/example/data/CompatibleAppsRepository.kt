package com.example.data

import android.content.Context
import android.content.Intent
import com.example.model.ChangelogVersion
import com.example.model.CompatibleApp

object CompatibleAppsRepository {

    fun getInitialCompatibleApps(): List<CompatibleApp> {
        return emptyList()
    }

    fun createSampleNewModule(currentCount: Int): CompatibleApp {
        val index = currentCount + 1
        return CompatibleApp(
            id = "module_custom_$index",
            name = "Module d'Extension #$index",
            packageName = "com.hephaistos.ext$index",
            version = "v1.0.$index",
            description = "Module compatible HephaistOS chargé dynamiquement dans l'environnement.",
            category = "Extension",
            isHero = false,
            isInstalled = true,
            isHeavyTask = (index % 3 == 0),
            status = "Actif",
            statusColor = 0xFF4CAF50,
            iconKey = when (index % 5) {
                0 -> "build"
                1 -> "memory"
                2 -> "speed"
                3 -> "psychology"
                else -> "storage"
            }
        )
    }

    fun getChangelogHistory(): List<ChangelogVersion> {
        return listOf(
            ChangelogVersion(
                version = "1.0.0",
                releaseDate = "17 Octobre 2026",
                isCurrent = true,
                changes = listOf(
                    "Lancement officiel de l'environnement et lanceur modulaire HephaistOS.",
                    "Implémentation du Rail de Navigation expressif et animé (étendu & compact).",
                    "Disposition d'accueil asymétrique M3 avec carte Maître Hero et conteneur Quad.",
                    "Gestionnaire de thèmes complet avec modes Clair/Sombre/Système et 5 styles chromatiques.",
                    "Module d'autorisations avec validation stockage et diagnostic d'accès root.",
                    "Génération dynamique et téléchargement de documentation PDF officielle avec changelog.",
                    "Console Terminal interactive intégrée avec support des commandes d'inspection système."
                )
            ),
            ChangelogVersion(
                version = "0.9.5-rc2",
                releaseDate = "05 Octobre 2026",
                isCurrent = false,
                changes = listOf(
                    "Calibrage des courbes d'animation à ressort sur les volets latéraux.",
                    "Amélioration des contrastes Material 3 sur les schémas Cobalt et Améthyste.",
                    "Optimisation des temps de rendu sur les listes asymétriques."
                )
            ),
            ChangelogVersion(
                version = "0.9.0-alpha",
                releaseDate = "18 Septembre 2026",
                isCurrent = false,
                changes = listOf(
                    "Prototype initial du launcher et de la structure de sandboxing.",
                    "Spécification des protocoles de communication entre modules compatibles."
                )
            )
        )
    }

    fun launchOrInteractWithApp(context: Context, app: CompatibleApp): String {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
        return if (launchIntent != null) {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Lancement de l'application ${app.name}..."
            } catch (e: Exception) {
                "Erreur lors du lancement de ${app.name}: ${e.message}"
            }
        } else {
            "Module HephaistOS [${app.name}] actif en environnement sécurisé (Statut: ${app.status})."
        }
    }
}
