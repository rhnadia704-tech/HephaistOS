# Spécification Canonique et Guide d'Architecture des Applications Satellites HephaistOS (HSAS v1.0)
*HephaistOS Satellite Application Standard — Version 1.0 (Octobre 2026)*

---

## 📑 Sommaire
1. [Vision & Rôle Fondamental des Satellites](#1-vision--rôle-fondamental-des-satellites)
2. [L'ADN Obligatoire du Manifeste (Détection Automatique)](#2-ladn-obligatoire-du-manifeste-détection-automatique)
3. [Charte Graphique & Tokens Jetpack Compose (ADN Visuel)](#3-charte-graphique--tokens-jetpack-compose-adn-visuel)
4. [Protocole d'Interopérabilité IPC (AIDL & Service d'Hôte)](#4-protocole-dinteropérabilité-ipc-aidl--service-dhôte)
5. [Dictionnaire Canonique des Capacités & Outils Satellites](#5-dictionnaire-canonique-des-capacités--outils-satellites)
6. [Organisation du Workspace Commun (/sdcard/HEPHAISTOS/)](#6-organisation-du-workspace-commun-sdcardhephaistos)
7. [Guide Clé-en-Main pour Agents IA & AI Studio](#7-guide-clé-en-main-pour-agents-ia--ai-studio)
8. [Code Source de Référence (Starter Kit Minimal)](#8-code-source-de-référence-starter-kit-minimal)

---

## 1. Vision & Rôle Fondamental des Satellites

Dans l'écosystème **HephaistOS**, l'application hôte représente le **système nerveux central** et la **colonne vertébrale** de l'ingénierie mobile :
- **L'Hôte HephaistOS** fournit le terminal Linux, le gestionnaire de permissions, l'accès privilégié (root su), les moteurs d'analyse de bas niveau (UkaEngine, extraction, super.img, EROFS, sparse) et la fenêtre principale de contrôle.
- **Une Application Satellite** est une extension modulaire autonome installée sur l'appareil. Elle enrichit la boîte à outils globale (ex : éditeur de `build.prop`, injecteur Magisk/KernelSU, convertisseur d'images sparse, extracteur de firmwares bootloader, simulateur DSU, analyseur AVB/vbmeta).

### Principes Directeurs Impératifs :
1. **Zéro Configuration Utilisateur** : Dès qu'un satellite est installé sur le téléphone via un APK, HephaistOS le détecte immédiatement sans redémarrage. S'il est désinstallé, il disparaît immédiatement.
2. **Harmonie Visuelle Totale** : Le satellite ne doit PAS inventer un design divergent. Il doit ressembler trait pour trait à une sous-section native d'HephaistOS (palette sombre/claire synchronisée, cartes à bords arrondis de 16dp, topbar profilée, typographie technique monospace pour les logs).
3. **Mutualisation des Capacités** : Tout satellite peut exploiter les outils d'HephaistOS et solliciter les autres satellites via l'annuaire d'outils partagés.

---

## 2. L'ADN Obligatoire du Manifeste (Détection Automatique)

Pour qu'une application soit instantanément reconnue comme **Satellite HephaistOS Officiel**, son fichier `AndroidManifest.xml` doit obligatoirement déclarer l'empreinte génétique suivante :

### 2.1 Intent-Filter et Métadonnées
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Permissions recommandées pour le partage de fichiers avec l'hôte -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />
    <uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:label="HephaistOS Tool - Mon Satellite"
        android:theme="@style/Theme.MonSatellite">

        <!-- Métadonnée globale indiquant la conformité HSAS v1.0 -->
        <meta-data
            android:name="org.hephaistos.satellite.V1"
            android:value="true" />

        <meta-data
            android:name="org.hephaistos.satellite.ROLE"
            android:value="Éditeur de Configuration & Modules" />

        <!-- 1. Activité Principale du Satellite -->
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />

                <!-- ACTION OFFICIELLE HEPHAISTOS OBLIGATOIRE -->
                <action android:name="org.hephaistos.action.SATELLITE" />
                <action android:name="org.hephaistos.action.ADDON" />
                <category android:name="android.intent.category.DEFAULT" />
            </intent-filter>

            <!-- Déclaration des capacités et outils exportés (séparés par des virgules) -->
            <meta-data
                android:name="org.hephaistos.addon.CAPABILITIES"
                android:value="prop.editor,magisk.patch,avb.verify" />
        </activity>

        <!-- 2. Service Optionnel pour l'Exécution Distante en Tâche de Fond (AIDL) -->
        <service
            android:name=".service.SatelliteIpcService"
            android:exported="true">
            <intent-filter>
                <action android:name="org.hephaistos.service.SATELLITE_IPC" />
            </intent-filter>
        </service>

    </application>
</manifest>
```

---

## 3. Charte Graphique & Tokens Jetpack Compose (ADN Visuel)

Pour garantir que le satellite s'intègre comme une partie intégrante d'HephaistOS, respectez scrupuleusement la charte Material Design 3 suivante :

### 3.1 Palette Chromatique Sombre (Dark Mode — Défaut Recommandé)
- **Fond d'écran principal (`background`)** : `#0D0E11`
- **Surface de premier niveau (`surface`)** : `#14161A`
- **Cartes & Conteneurs (`surfaceContainer`)** : `#1C1F26`
- **Conteneurs Élevés (`surfaceContainerHigh`)** : `#252932`
- **Couleur Primaire (Accent HephaistOS)** : `#E67E22` (Orange Forge) ou `#FF8F00`
- **Texte Primaire (`onSurface`)** : `#F0F2F5`
- **Texte Secondaire (`onSurfaceVariant`)** : `#9AA0A6`
- **Succès / Actif** : `#00E676`
- **Avertissement** : `#FFB300`
- **Erreur** : `#FF5252`

### 3.2 Palette Chromatique Claire (Light Mode)
- **Fond d'écran principal (`background`)** : `#F8F9FA`
- **Surface (`surface`)** : `#FFFFFF`
- **Cartes & Conteneurs (`surfaceContainer`)** : `#F1F3F4`
- **Conteneurs Élevés (`surfaceContainerHigh`)** : `#E8EAED`
- **Couleur Primaire (`primary`)** : `#D35400`
- **Texte Primaire (`onSurface`)** : `#1F1F1F`

### 3.3 Règles de Mise en Page Compose
1. **Éléments tactiles** : Hauteur minimale de `48.dp` sur tous les boutons et éléments cliquables.
2. **Rayons de courbure (Corner Radius)** : `16.dp` pour les cartes principales, `10.dp` pour les sous-panneaux, `CircleShape` ou `20.dp` pour les boutons.
3. **Typographie des Logs & Chemins** : Toujours utiliser `FontFamily.Monospace` avec une taille de `10.sp` à `11.sp` sur fond `#121316` avec des bordures subtiles (`Color(0xFF2B2E35)`).
4. **Header Épuré** : Barre supérieure simple avec bouton retour circulaire (`42.dp`), titre en gras (`titleLarge`), et statut de connexion vert (`Connecté à HephaistOS`).

---

## 4. Protocole d'Interopérabilité IPC (AIDL & Service d'Hôte)

HephaistOS expose un service bound Android standard avec l'action `org.hephaistos.service.HOST_IPC`.
Tout satellite peut s'y connecter en quelques lignes de code pour bénéficier de l'infrastructure hôte :

### 4.1 Interface AIDL : `IHephaistHostService.aidl`
```aidl
package org.hephaistos.ipc;

interface IHephaistHostService {
    // Exécuter un outil ou commande d'ingénierie sur l'hôte (UkaEngine, extraction, root, inspection)
    String executeHostTool(String toolId, String jsonParams);

    // Obtenir le chemin certifié du workspace (/sdcard/HEPHAISTOS/)
    String getWorkspaceRootPath();

    // Écrire directement dans le terminal temps réel d'HephaistOS
    void logToHostTerminal(String senderTag, String message);

    // Découvrir les outils offerts par l'ensemble des satellites installés
    String getEcosystemRegistryJson();

    // Vérifier si l'hôte dispose des droits SuperUtilisateur (root)
    boolean isHostRootAvailable();
}
```

### 4.2 Connexion Kotlin depuis le Satellite
```kotlin
class HostConnector(private val context: Context) {
    private var hostService: IHephaistHostService? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            hostService = IHephaistHostService.Stub.asInterface(service)
            // Notifier l'hôte de notre présence
            hostService?.logToHostTerminal("MonSatellite", "Satellite connecté avec succès via HSAS v1.0")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            hostService = null
        }
    }

    fun bind() {
        val intent = Intent("org.hephaistos.service.HOST_IPC").apply {
            setPackage("com.aistudio.hephaistos.vznwkd") // ou découverte dynamique
        }
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    fun callTool(toolId: String, paramsJson: String): String? {
        return hostService?.executeHostTool(toolId, paramsJson)
    }
}
```

---

## 5. Dictionnaire Canonique des Capacités & Outils Satellites

Pour permettre la coopération entre satellites, utilisez les identifiants de capacités standardisés suivants dans votre métadonnée `org.hephaistos.addon.CAPABILITIES` :

| Identifiant de Capacité | Rôle Fonctionnel |
|---|---|
| `kitchen.unpack` | Déballage d'images Sparse, EROFS ou EXT4 |
| `kitchen.repack` | Recompilation bit-à-bit d'une partition |
| `payload.dumper` | Extraction des partitions depuis un `payload.bin` OTA |
| `erofs.tools` | Outils de manipulation et inspection d'images EROFS |
| `prop.editor` | Éditeur et injecteur de propriétés système `build.prop` |
| `avb.signer` | Signature et calcul des descripteurs AVB / vbmeta |
| `magisk.patch` | Patching automatique de partition `boot.img` ou `init_boot.img` |
| `dsu.installer` | Montage dynamique dans DSU Sideloader |
| `metadata.inspect` | Analyse des permissions SELinux et tables de symlinks |
| `super.unpack` | Éclatement de `super.img` en partitions logiques dynamiques |

---

## 6. Organisation du Workspace Commun (`/sdcard/HEPHAISTOS/`)

Tous les satellites opèrent de façon unifiée dans le dossier partagé `/sdcard/HEPHAISTOS/` :

```text
/sdcard/HEPHAISTOS/
├── UNPACKED/       # Les dossiers contenant les fichiers extraits des partitions
├── PACKED/         # Les images finales reconstruites prêtes pour le flash
├── Data/unpack/    # Métadonnées critiques (file_contexts SELinux, fs_config, symlinks)
├── Payloads/       # Fichiers sources volumineux (payload.bin, super.img)
├── Projects/       # Fichiers de configuration de projets spécifiques
├── Scripts/        # Scripts d'automatisation (.sh, .py)
├── WebApps/        # Applications web modulaires locales
└── Templates/      # Documentation, spécifications et Kits
```

---

## 7. Guide Clé-en-Main pour Agents IA & AI Studio

Si vous demandez à **Google AI Studio** ou à tout autre agent d'ingénierie logicielle de créer un satellite pour HephaistOS, transmettez-lui ce prompt système standardisé :

```text
Tu es chargé de créer une Application Satellite officielle pour le système HephaistOS, conforme à la norme HSAS v1.0 (HephaistOS Satellite Application Standard).

Règles impératives :
1. Dans AndroidManifest.xml :
   - Ajoute dans l'activité principale l'action d'intention :
     <action android:name="org.hephaistos.action.SATELLITE" />
     <action android:name="org.hephaistos.action.ADDON" />
   - Ajoute les meta-data :
     <meta-data android:name="org.hephaistos.satellite.V1" android:value="true" />
     <meta-data android:name="org.hephaistos.addon.CAPABILITIES" android:value="<tes_capacites_separees_par_virgule>" />
     <meta-data android:name="org.hephaistos.satellite.ROLE" android:value="<description_courte_du_role>" />

2. Dans l'Interface Utilisateur (Jetpack Compose) :
   - Adopte l'ADN visuel d'HephaistOS :
     - Fond sombre : #0D0E11, Surface de carte : #1C1F26, Accent : #E67E22 (Orange Forge).
     - Cartes M3 avec bordures arrondies de 16.dp et contour fin #2B2E35.
     - Boutons tactiles minimum 48.dp de hauteur.
     - Terminal de logs avec police FontFamily.Monospace (taille 10-11sp).
   - Propose une barre supérieure épurée avec icône et statut "Satellite HephaistOS Actif".

3. Dans la Logique Applicative :
   - Opère dans le workspace standard /sdcard/HEPHAISTOS/ (dossiers UNPACKED, PACKED, Data/unpack).
   - Intègre la connexion au service hôte HephaistOS via l'interface AIDL org.hephaistos.ipc.IHephaistHostService (action "org.hephaistos.service.HOST_IPC").
   - Fournis un README.md téléchargeable récapitulant les fonctionnalités du satellite et les outils partagés.
```

---

## 8. Code Source de Référence (Starter Kit Minimal)

### Thème Jetpack Compose Minimaliste (`HephaistTheme.kt`)
```kotlin
package com.example.satellite.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val HephaistOrange = Color(0xFFE67E22)
val DarkBackground = Color(0xFF0D0E11)
val DarkSurface = Color(0xFF14161A)
val DarkSurfaceContainer = Color(0xFF1C1F26)
val TextPrimary = Color(0xFFF0F2F5)
val TextSecondary = Color(0xFF9AA0A6)

private val DarkColorScheme = darkColorScheme(
    primary = HephaistOrange,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceContainer = DarkSurfaceContainer,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary
)

@Composable
fun HephaistSatelliteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
```

---
*Fin de la Spécification HSAS v1.0 — HephaistOS Core Engineering Group.*
