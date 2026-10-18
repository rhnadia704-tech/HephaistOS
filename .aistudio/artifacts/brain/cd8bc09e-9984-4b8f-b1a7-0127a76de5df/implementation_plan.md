# Plan d'Implémentation : Écosystème HephaistOS, Accueil & Satellites

Ce plan d'implémentation adresse l'ensemble des besoins exprimés par l'utilisateur :
1. **Fiabilisation de l'ouverture du Terminal** pour éliminer tout affichage intempestif au simple clic sur l'accueil, en imposant un geste intentionnel (pression longue puis glissement vertical net).
2. **Simplification et épuration de la carte Unpack / Repack** sur l'accueil avec un design Material 3 sobre intitulé "Unpack / Repack" sans verbiage superflu.
3. **Normalisation de l'intitulé** en "Terminal" sur tous les composants de l'application.
4. **Enrichissement des templates dans les Paramètres** avec des modèles complets de scripts Bash et de WebApps HTML5 intégrant le pont système HephaistOS.
5. **Génération d'un guide PDF enrichi** prêt à télécharger regroupant les templates et structures obligatoires des satellites (APK, WebApp, Script).
6. **Détection automatique en temps réel** des applications satellites installées et désinstallées via un `BroadcastReceiver` sans rechargement manuel.
7. **Gestion des permissions de stockage et vérification Root au démarrage** avec affichage d'un avertissement informatif et bascule élégante en mode restreint si non rooté.
8. **Intégration d'un workflow GitHub Actions** prêt à l'emploi compilant et publiant automatiquement l'APK Debug lors des pushs.

---

## Architecture Proposée

```
app/src/main/
├── AndroidManifest.xml                        # Ajout MANAGE_EXTERNAL_STORAGE + queries + receiver intent-filters
├── java/com/example/
│   ├── MainActivity.kt                        # Initialisation cycle de vie, BroadcastReceiver packages, vérification stockage & root
│   ├── data/
│   │   ├── AddonRepository.kt                 # Gestion réactive des satellites avec Flow dynamique
│   │   ├── PdfExporterService.kt              # Moteur PDF enrichi : specs satellites, templates bash & webapp
│   │   ├── RootService.kt                     # Détection su, exécution shell avec fallback sans root
│   │   └── TemplateGenerator.kt               # Modèles de code (Bash, WebApp, Manifest)
│   ├── model/
│   │   └── SystemStatus.kt                    # État stockage, privilèges root, statut satellites
│   └── ui/
│       ├── components/
│       │   ├── BottomTerminalPanel.kt         # Intitulé simplifié "Terminal", interface épurée
│       │   └── RootWarningBanner.kt           # Bandeau / Dialog explicatif si terminal non-rooté
│       └── screens/
│           ├── HomeScreen.kt                  # Gestuelle robuste (appui long + swipe), carte Unpack/Repack épurée
│           └── SettingsScreen.kt              # Onglet templates étendu (Bash & WebApp) + export PDF direct
.github/
└── workflows/
    └── build.yml                              # Workflow GitHub Actions : Java 21, Gradle 9, compilation APK Debug et upload artefact
```

---

## Détail des Tâches Prévues

### 1. Fiabilisation Ergonomique du Terminal & Correction du Bug de Clic
- **Problème identifié** : L'utilisation de `detectVerticalDragGestures` sur le conteneur racine de `HomeScreen` intercepte les clics et micros-glissements de l'utilisateur, provoquant des ouvertures impromptues du panneau inférieur.
- **Solution** :
  - Supprimer le détecteur trop permissif sur la racine.
  - Implémenter un geste intentionnel à double palier : `detectDragGesturesAfterLongPress` avec un seuil de déplacement vertical vers le haut significatif (> 70 dp).
  - Maintenir le clic direct et accessible sur le bouton dédié du `BottomNavigationPillBar` pour garantir l'accessibilité à 100%.
  - Remplacer partout l'intitulé "Console & Logs" par simplement **"Terminal"**.

### 2. Épuration de la Carte Unpack / Repack sur l'Accueil
- Remplacer la carte verbeuse actuelle par une carte Material 3 compacte et élégante :
  - Titre épuré : **Unpack / Repack**.
  - Mention sobre des dossiers surveillés (`sdcard/HEPHAISTOS/UNPACKED` et `PACKED`).
  - Boutons d'action clairs : "Ouvrir l'Atelier" (bascule vers l'écran 2 pages existant).
  - Suppression de tous les textes d'aide superflus et labels redondants.

### 3. Enrichissement des Modèles dans Paramètres (Scripts & WebApps)
- **Modèle Script Bash** :
  - Variables d'environnement standardisées injectées par l'hôte (`$HEPHAISTOS_ROOT`, `$HEPHAISTOS_UNPACKED_DIR`, `$HEPHAISTOS_DATA_DIR`).
  - Gestion des logs, codes de sortie et notifications.
- **Modèle WebApp HTML5 / JS** :
  - Structure HTML5 moderne Material 3.
  - Initialisation sécurisée du pont `window.HephaistBridge` avec gestion des promesses/callbacks (`executeCommand`, `showToast`, `getSystemInfo`, `unpackImage`).
- **Fonctionnalités UI** : Visualisation syntaxique, bouton "Copier dans le presse-papier" et bouton "Exporter le fichier modèle".

### 4. Génération & Téléchargement du PDF Enrichi des Spécifications Satellites
- Évolution de `PdfExporterService` pour générer un document PDF multi-pages haute résolution :
  - **Chapitre 1** : Architecture d'un satellite HephaistOS (contrat d'intentions `org.hephaistos.action.ADDON`, métadonnées obligatoires).
  - **Chapitre 2** : Structure obligatoire d'un APK Satellite (`AndroidManifest.xml`, icônes adaptatives, `launchMode`).
  - **Chapitre 3** : Structure d'une WebApp compatible (`manifest.json`, script pont `hephaist-bridge.js`, mise en page responsive).
  - **Chapitre 4** : Spécification des Scripts Bash (headers `#!/system/bin/sh`, gestion des privilèges root, conventions de dossiers).
  - **Actions** : Téléchargement dans `/sdcard/Download/` ou `HEPHAISTOS/Documents/` et ouverture automatique via le lecteur PDF natif.

### 5. Détection Automatique en Temps Réel des APKs (Installations / Désinstallations)
- Déclarer et enregistrer dynamiquement un `BroadcastReceiver` dans `MainActivity` :
  - Filtres d'intention : `Intent.ACTION_PACKAGE_ADDED`, `Intent.ACTION_PACKAGE_REMOVED`, `Intent.ACTION_PACKAGE_REPLACED`.
  - Schéma de données : `package`.
- Dès qu'un paquet est ajouté, mis à jour ou supprimé, actualiser immédiatement et de façon réactive le `StateFlow` des satellites dans `AddonRepository`.
- La grille des applications sur `HomeScreen` se met à jour instantanément sans qu'aucune action manuelle d'actualisation ne soit nécessaire.

### 6. Vérification des Permissions et Accès Root au Démarrage
- **Autorisations de Stockage** :
  - Sous Android 11+ (API 30+) : Vérification de `Environment.isExternalStorageManager()`. Si non accordé, invitation directe à l'écran système `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`.
  - Déclaration de `MANAGE_EXTERNAL_STORAGE` dans `AndroidManifest.xml`.
- **Privilèges Root** :
  - Test d'accès `su` au lancement.
  - Conformément à la préférence choisie : affichage d'un bandeau/dialogue explicatif non bloquant si l'appareil n'est pas rooté, avec activation transparente du mode restreint / espace utilisateur.

### 7. Workflow GitHub Actions CI (`.github/workflows/build.yml`)
- Création du fichier `.github/workflows/build.yml` :
  - Déclenchement sur `push` (branches `main`, `master`) et `pull_request`.
  - Configuration de la machine virtuelle `ubuntu-latest`.
  - Installation de Java 21 (`actions/setup-java@v4`).
  - Mise en place du runner Gradle (`gradle/actions/setup-gradle@v4`).
  - Exécution de la commande `gradle assembleDebug`.
  - Téléversement de l'artefact produit `app-debug.apk` via `actions/upload-artifact@v4`.
- Ajout d'un script d'amorçage `gradlew` standard pour garantir la compatibilité universelle lors d'un clone local ou sur GitHub.

---

## Plan de Validation & Vérification
1. **Compilation** : Exécuter `compile_applet` pour garantir l'absence de toute erreur de syntaxe ou d'incompatibilité de dépendances.
2. **Tests Comportementaux** :
   - Vérifier que les clics ordinaires sur l'accueil n'ouvrent plus le Terminal par erreur.
   - Vérifier que la carte Unpack / Repack est compacte et lisible.
   - Vérifier la génération du PDF enrichi et sa disponibilité.
   - Valider la syntaxe et la conformité du workflow GitHub Actions.
