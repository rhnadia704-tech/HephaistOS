# Plan d'Implémentation Révisé : Contrat d'Identification des Satellites AI Studio, Bible des Templates & Simplification de GÉMÉAU

Ce plan définit le contrat d'identification positive des satellites d'HéphaïstOS (sans aucune liste noire, compatible avec tout paquet `com.aistudio...`), enrichit la Bible des templates avec ces directives officielles, allège l'interface de GÉMÉAU (cartes avec icône et nom uniquement) et affiche les satellites directement sur l'accueil sous forme de cartes d'outils natives.

---

## Décisions Clés Confirmées avec l'Utilisateur

> [!IMPORTANT]
> Les choix d'architecture suivants ont été validés :
> - **Contrat d'identification positif (Zéro liste noire)** :
>   Pour qu'une application créée avec AI Studio (portant par exemple l'ID de package `com.aistudio.montool.kxmpzq`) soit reconnue à 100% comme un satellite officiel d'HéphaïstOS, elle doit simplement déclarer dans son `AndroidManifest.xml` :
>   1. La balise meta-data dans `<application>` : `<meta-data android:name="hephaistos.satellite" android:value="true" />`
>   2. L'Intent Filter officiel dans son Activité principale :
>      ```xml
>      <intent-filter>
>          <action android:name="org.hephaistos.action.SATELLITE" />
>          <category android:name="android.intent.category.DEFAULT" />
>      </intent-filter>
>      ```
>   Tout paquet tiers ou système (comme `com.google.android.telephony.satellite`) est automatiquement et naturellement ignoré car il ne possède pas cette signature HéphaïstOS.
> - **Bible des templates** :
>   Ce contrat officiel est gravé dans la documentation et les fichiers de démarrage dans `/sdcard/HEPHAISTOS/Templates/satellite_template/` (`AndroidManifest.xml` et `README.md`).
> - **GÉMÉAU minimaliste** :
>   Les cartes du Hub GÉMÉAU ne comportent aucun texte verbeux ni badge complexe : **uniquement l'icône et le nom de la fonctionnalité**, avec ouverture directe au tap.
> - **Affichage sur l'accueil** :
>   Les satellites sont insérés directement dans la liste des fonctionnalités d'HéphaïstOS au même format que les autres modules (icône et titre).

---

## 1. Vue d'Ensemble & Spécifications

### 1.1 Détection Intelligente & Robuste (`AddonRegistry.kt`)
Le mécanisme de détection est nettoyé de toute recherche de sous-chaîne hasardeuse :
- Suppression totale du filtre `contains("satellite")` sur le nom de paquet.
- Détection par **Intent Filter** : `pm.queryIntentActivities(Intent("org.hephaistos.action.SATELLITE"), ...)`
- Détection par **Meta-Data Manifest** : Présence de `hephaistos.satellite == true` (ou `org.hephaistos.satellite.V1 == true`).
- Détection de fichiers APKs autonomes : Analyse du Manifest des `.apk` placés dans `/sdcard/HEPHAISTOS/Satellites/` via `getPackageArchiveInfo`.
- **Résultat** : Détection infaillible des applications créées par l'utilisateur ou par des IA dans AI Studio, avec zéro faux positif.

### 1.2 La Bible des Templates (`WorkspaceManager.kt`)
Mise à jour et enrichissement du starter template généré dans `/sdcard/HEPHAISTOS/Templates/satellite_template/` :
- `AndroidManifest.xml` :
  - Métadonnée `<meta-data android:name="hephaistos.satellite" android:value="true" />`
  - Filtre d'intention `<action android:name="org.hephaistos.action.SATELLITE" />`
  - Rôle et description déclarés.
- `README.md` ("Bible des Satellites HéphaïstOS") :
  - Définition formelle des exigences de compatibilité.
  - Règles de verrouillage du thème Material 3 (`HephaistLockedTheme.kt`).
  - Protocole de communication et lancement.

### 1.3 Simplification des Cartes GÉMÉAU (`GemeauScreen.kt`)
Refonte de `GemeauHubView` :
- Remplacement des cartes volumineuses par `GemeauMinimalCard` :
  - Boîte d'icône 44.dp avec conteneur de couleur tonale.
  - Titre épuré en gras :
    - `1. Keymaker`
    - `2. SIGNAPK`
    - `3. ROMsign`
    - `4. VÉRIFICATION`
  - Chevron discret ou flèche de navigation.
  - Un tap ouvre directement l'interface de la fonctionnalité.
- Carrousel VÉRIFICATION : Allègement pour ne présenter que les deux cartes claires : "Verify App (APKs)" et "Verify ROM (Système)", avec clic immédiat vers leur écran dédié.

### 1.4 Affichage Direct des Satellites sur l'Accueil (`HomeScreen.kt`)
- Les satellites détectés sont affichés directement dans la liste principale sous forme de cartes d'action rapide `SatelliteQuickCard` :
  - Même format que `UnpackRepackQuickCard`, `ExtractorQuickCard`, `GemeauQuickCard` et `AheQuickCard`.
  - Pas de section séparée ni de texte superflu.

---

## 2. Diagramme d'Architecture

```
┌────────────────────────────────────────────────────────┐
│                   DÉTECTION SATELLITE                  │
│                                                        │
│  Application APK (ex: com.aistudio.montool.kxmpzq)     │
│  ┌──────────────────────────────────────────────────┐  │
│  │ AndroidManifest.xml                              │  │
│  │   • meta-data: "hephaistos.satellite" = true    │  │
│  │   • action: "org.hephaistos.action.SATELLITE"    │  │
│  └────────────────────────┬─────────────────────────┘  │
│                           │                            │
│                           ▼                            │
│           PackageManager / AddonRegistry               │
│         [Conforme au contrat HéphaïstOS ?]             │
│                 /                    \                 │
│               OUI                    NON               │
│               /                        \               │
│      Affiché sur l'Accueil       Ignoré (ex: Google)   │
│      comme fonctionnalité                              │
└────────────────────────────────────────────────────────┘
```

---

## 3. Plan de Modifications par Fichier

1. **`app/src/main/java/com/example/data/AddonRegistry.kt`** :
   - Remplacer les filtres approximatifs par le contrat Manifest strict (`org.hephaistos.action.SATELLITE` et `hephaistos.satellite`).
   - S'assurer que les APKs déposés dans `HEPHAISTOS/Satellites/` sont inspectés selon les mêmes critères.

2. **`app/src/main/java/com/example/data/WorkspaceManager.kt`** :
   - Intégrer les balises dans le starter template et documenter la "Bible" dans `README.md`.

3. **`app/src/main/java/com/example/ui/screens/GemeauScreen.kt`** :
   - Créer `GemeauMinimalCard` (icône + nom uniquement).
   - Simplifier `GemeauHubView` et le carrousel de vérification.

4. **`app/src/main/java/com/example/ui/screens/HomeScreen.kt`** :
   - Intégrer les cartes de satellites directement dans le flux principal de l'accueil.

5. **Validation** :
   - Compilation avec `compile_applet`.
   - Exécution des tests avec `gradle :app:testDebugUnitTest`.
