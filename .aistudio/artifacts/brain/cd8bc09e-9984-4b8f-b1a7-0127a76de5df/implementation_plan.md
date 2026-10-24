# Plan d'Implémentation : Outillage Natif Rust Haute Performance via GitHub Actions & Analyse Approfondie GÉMÉAU

Ce plan définit l'infrastructure permettant à HéphaïstOS de disposer d'outils natifs ultra-puissants développés en **Rust**, compilés lors du push via **GitHub Actions**, intégrés directement dans l'APK et déployés de manière structurée dans `/sdcard/HEPHAISTOS/TOOLS/`. Ces outils alimenteront une base de données d'audit riche et exhaustive de plusieurs mégaoctets dans `/sdcard/HEPHAISTOS/DATA/GEMEAU/`.

---

## 1. Architecture Générale

```
┌─────────────────────────────────────────────────────────────┐
│                       GITHUB ACTIONS                        │
│  - Cross-compilation Rust (aarch64, armv7, x86_64)          │
│  - Génération des binaires strip/LTO haute performance      │
│  - Injection dans app/src/main/assets/tools/bin/            │
│  - Compilation de l'APK HéphaïstOS avec outillage inclus    │
└──────────────────────────────┬──────────────────────────────┘
                               │ (Installation / Premier lancement)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│                 HÉPHAÏSTOS WORKSPACE MANAGER                │
│  - Auto-extraction vers /sdcard/HEPHAISTOS/TOOLS/           │
│    ├── bin/       (Exécutables natifs : gemeau-dissector...) │
│    ├── scripts/   (Scripts shell / bash d'orchestration)    │
│    ├── profiles/  (Profils de cibles AOSP, MIUI, Lineage...)│
│    └── rules/     (Schémas SELinux, MMAC, fs-verity)        │
│  - Synchronisation miroir exécutable (context.filesDir/bin) │
└──────────────────────────────┬──────────────────────────────┘
                               │ (Lancement d'audit ROM / GÉMÉAU)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│           ANALYSE INTÉGRALE & BASE DE DONNÉES GÉMÉAU        │
│  - Analyse cryptographique v1, v2, v3, v4                   │
│  - Mappage des permissions, contextes SELinux & MMAC        │
│  - Cartographie des dépendances ART (.vdex, .odex, .oat)    │
│  - Génération de la base dans /sdcard/HEPHAISTOS/DATA/GEMEAU│
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Composants à Développer

### A. Projet Natif Rust (`tools/gemeau-dissector/`)
Création d'un projet Rust de niveau professionnel dans le dépôt :
- **`Cargo.toml`** : Configuration du workspace, dépendances (`sha2`, `rsa`, `zip`, `byteorder`, `serde_json`, etc.) avec profil de release optimisé (`opt-level = 3`, `lto = true`, `strip = true`).
- **`src/main.rs`** : CLI acceptant les arguments `--rom-path <PATH>`, `--output-dir <PATH>`, `--format <json|markdown|db>`, `--deep-audit`.
- **`src/crypto.rs`** :
  - Décodeur d'APK Signing Block v2/v3/v4.
  - Décodeur ASN.1 / X.509 de certificats et hachages Merkle tree fs-verity.
- **`src/selinux.rs`** :
  - Parseur de `mac_permissions.xml`, `file_contexts.bin`, `seapp_contexts`.
  - Mappage des `seinfo` (`platform`, `media`, `shared`, `privapp`, `default`) et certificats signataires.
- **`src/axml.rs`** :
  - Parseur binaire rapide du StringPool et des éléments d'`AndroidManifest.xml` (paquet, `sharedUserId`, autorisations requises et définies).
- **`src/art.rs`** :
  - Inspection des fichiers `.vdex`, `.odex`, `.oat` (en-têtes, checksums DEX, architecture machine).
- **`src/report.rs`** :
  - Générateur de la base de données exhaustive JSON et du rapport Markdown complet d'audit.

### B. Workflow GitHub Actions (`.github/workflows/build-tools.yml`)
Workflow automatisé déclenché sur chaque push :
1. **Étape 1 : Setup de l'environnement** :
   - Installation de Rust stable et des cibles Android (`aarch64-linux-android`, `armv7-linux-androideabi`, `x86_64-linux-android`).
   - Configuration de `cargo-ndk` ou Android NDK cross-compilers.
2. **Étape 2 : Compilation des outils natifs** :
   - Compilation en mode release stripée pour les 3 architectures.
   - Placement automatique des binaires produits dans `app/src/main/assets/tools/bin/<arch>/gemeau-dissector`.
3. **Étape 3 : Empaquetage des ressources complémentaires** :
   - Injection des scripts d'orchestration (`tools/scripts/`) dans `assets/tools/scripts/`.
   - Injection des profils et règles de détection dans `assets/tools/profiles/` et `assets/tools/rules/`.
4. **Étape 4 : Compilation de l'APK Android** :
   - Exécution de `gradle assembleDebug` ou `assembleRelease` intégrant tous les assets natifs.
   - Publication de l'APK et des binaires natifs en tant qu'artefacts de build téléchargeables.

### C. Gestionnaire d'Outillage & Déploiement (`WorkspaceManager.kt`)
Mise à niveau de `WorkspaceManager` pour administrer `/sdcard/HEPHAISTOS/TOOLS/` :
- Création de l'arborescence structurée :
  - `/sdcard/HEPHAISTOS/TOOLS/bin/`
  - `/sdcard/HEPHAISTOS/TOOLS/scripts/`
  - `/sdcard/HEPHAISTOS/TOOLS/profiles/`
  - `/sdcard/HEPHAISTOS/TOOLS/rules/`
- Extraction automatique des assets de l'APK vers `sdcard/HEPHAISTOS/TOOLS/` lors du premier lancement ou mise à jour.
- Mise en miroir exécutable dans `context.filesDir/tools/bin/` avec permissions d'exécution (`rwxr-xr-x` / `chmod 755`) pour contourner la restriction `noexec` du stockage externe Android.
- Création de scripts d'audit et de profils de test intégrés.

### D. Interopérabilité et Base de Données GÉMÉAU (`GemeauSignerEngine.kt` & `GemeauScreen.kt`)
- Définition du répertoire de données structurées `/sdcard/HEPHAISTOS/DATA/GEMEAU/` :
  - `trust_chain_deep_audit.json` (toutes les clés, empreintes, chaînes de certificats).
  - `permissions_graph.json` (relations sharedUserId, privilèges système, signature-level permissions).
  - `selinux_mmac_audit.json` (politiques mac_permissions, seinfo, contextes).
  - `art_compilation_map.json` (cartographie des fichiers vdex/odex/oat et dépendances dex).
  - `fsverity_audit.json` (arbres Merkle et descripteurs fsvmeta).
  - `audit_report.md` (rapport global complet et lisible).
- Détection automatique :
  - Si le binaire natif `gemeau-dissector` est présent dans `context.filesDir/tools/bin/` ou `/sdcard/HEPHAISTOS/TOOLS/bin/`, l'analyse l'exécute directement pour un traitement instantané et une base de données ultra-riche.
  - Si absent (ou environnement restreint), le moteur Kotlin intégré prend le relais de manière transparente avec le même schéma de données.

---

## 3. Fichiers à Créer et Modifier

1. **`tools/gemeau-dissector/Cargo.toml`** & **`tools/gemeau-dissector/src/*.rs`** : Code source complet de l'outil natif en Rust.
2. **`.github/workflows/build-tools.yml`** : Workflow d'intégration et cross-compilation continue GitHub Actions.
3. **`tools/scripts/*.sh`** : Scripts d'aide et d'orchestration pour le système.
4. **`tools/profiles/*.json`** & **`tools/rules/*.xml`** : Profils de signatures d'OS et modèles SELinux.
5. **`app/src/main/java/com/example/data/WorkspaceManager.kt`** : Définition des dossiers TOOLS, DATA/GEMEAU et logique d'extraction automatique.
6. **`app/src/main/java/com/example/data/GemeauSignerEngine.kt`** : Intégration de l'appel du binaire natif et écriture de la base de données enrichie.
7. **`app/src/main/java/com/example/ui/screens/GemeauScreen.kt`** : Affichage des rapports enrichis et du statut de l'outillage natif.

---

## 4. Étapes de Validation
1. Lancer `compile_applet` pour valider la compilation Android.
2. Vérifier les tests unitaires via `gradle :app:testDebugUnitTest`.
3. Valider la structure des dossiers et l'extraction dans `/sdcard/HEPHAISTOS/TOOLS/` et la génération des rapports dans `/sdcard/HEPHAISTOS/DATA/GEMEAU/`.
