# Plan d'Implémentation Révisé — Module GEMO Réel & Écosystème d'Extensions .AHE

Ce document définit les spécifications complètes et le plan de travail pour :
1. **La correction intégrale de GEMO (Kitchen Signer & Trust Chain Engine)** pour traiter avec fidélité absolue tout fichier sans exception (images `.img` de toute taille, partitions brutes, sparse, EXT4, EROFS, F2FS, archives ZIP ou dossiers unpackés), en préservant l'intégralité des éléments critiques et liens symboliques, et en ne régénérant que les éléments strictement rendus caducs par le changement de signature.
2. **Le système universel d'extensions modulaires `.ahe` (Android HephaistOS Extension)** opérant dans `/sdcard/HEPHAISTOS`, avec délégation totale des autorisations, interface Material 3 fidèle aux standards d'HephaistOS (avec possibilité d'apporter de nouveaux layouts, pages ou entrées de navigation), communication inter-extensions avancée, import par le tiroir Plus (+), et documentation GitHub avec template complet.

---

## 1. Correction GEMO : Préservation Totale & Resignature Fidèle

### Objectif
Reproduire le reflet exact du fichier de base sans la moindre corruption ni fichier factice de 170 octets. Tous les éléments non impactés par la signature (binaires, HAL, bibliothèques `.so`, scripts init, liens symboliques, structures de dossiers, permissions POSIX et contextes SELinux) sont **conservés strictement à l'identique**. Seuls les éléments liés à la signature sont régénérés ou assainis.

### Pipeline Détaillé
1. **Inspection & Extraction Sans Perte** :
   - Support des images `.img` (EXT4 sparse/raw, EROFS, F2FS), des ZIP de ROM ou des dossiers déjà extraits (`UNPACKED/`).
   - Extraction via `UkaEngine.unpackSingleImage` en conservant l'index exact des permissions (`fs_config`), contextes SELinux (`file_contexts`) et liens symboliques (symlinks et hardlinks).
2. **Analyse de Cohérence & Mappage de Confiance** :
   - Scan exhaustif de tous les APKs présents dans toutes les partitions (`system`, `system_ext`, `product`, `vendor`, `apex`).
   - **Détection et Préservation Stricte des GApps** : Identification des paquets Google et services critiques (`com.google.*`, Play Store, GMS Core, microG, DRM stubs). Ces applications conservent rigoureusement leur signature d'origine.
   - Les APKs système propriétaires ou AOSP sont resignés avec les clés correspondantes du Keyset sélectionné.
3. **Mise à Jour Ciblée des Éléments Dépendants de la Signature** :
   - **SELinux MAC** : Mise à jour des signatures de certificats dans `mac_permissions.xml` (`<signer signature="...">`) pour garantir que les transitions SELinux de privilèges (`platform`, `privapp`) restent 100% valides sous le nouveau Keyset.
   - **Purge Ciblée des Caches Incompatibles** : Suppression sélective des fichiers pré-compilés liés à l'ancienne signature (`.odex`, `.vdex`, `.oat`, `.art`, `.fsvmeta`) afin que le runtime Android (ART) recompile proprement les APKs modifiés au premier démarrage sans causer de bootloop.
   - **Zéro Altération Collatérale** : Aucun fichier ELF `.so`, configuration `.rc` ou lien symbolique n'est touché ou supprimé s'il n'est pas lié à la signature.
4. **Reconstruction (Repack) au Format Exact Source** :
   - Détection automatique du système de fichiers source (EXT4, EROFS, F2FS) et de son mode (Sparse ou Raw).
   - Appel de `UkaEngine.repackTarget` pour recréer l'image avec les mêmes blocs, mêmes tables `fs_config` et contextes.
   - Si l'option `SIGNER with vbmeta` est activée : calcul des hachages de descripteurs de partition et création d'un `vbmeta.img` AVB 2.0 signé avec les clés du Keyset et flags de désactivation de la vérification.
   - Sortie dans `/sdcard/HEPHAISTOS/SIGNER/signed_roms/<nom_base>_resigned.img` (taille réelle conforme au système de fichiers).

---

## 2. Architecture des Extensions .AHE

### Emplacement & Autorisations Déléguées
- Espace de travail racine : `/sdcard/HEPHAISTOS/`.
- Répertoire des extensions installées : `/sdcard/HEPHAISTOS/Extensions/<extension_id>/`.
- HephaistOS délègue à chaque extension installée :
  - L'accès complet en lecture/écriture à `/sdcard/HEPHAISTOS/` (UNPACKED, PACKED, EXTRACT, SIGNER, WORK, Data).
  - L'accès aux droits Root (via `su` de `TerminalEngine`) si demandé dans le manifeste.
  - L'exécution de binaires natifs ARM64/ARM32 et de scripts shell (`chmod +x` automatique).

### Structure d'une Archive `.ahe`
```
nom_extension.ahe (Archive ZIP standard)
├── manifest.json            # Configuration, métadonnées, layouts, actions, dépendances
├── bin/                     # Binaires natifs compilés (ARM64-v8a / armeabi-v7a)
├── scripts/                 # Scripts shell exécutables (.sh)
├── tools/                   # Outils auxiliaires (jar, configs, patchs)
├── docs/                    # Documentation interne
│   └── README.md
└── icon.png                 # (Optionnel) Icône de l'extension
```

### Manifeste `manifest.json` avec M3 Layouts
Le manifeste permet de définir la structure visuelle Material 3 de l'extension :
```json
{
  "id": "com.hephaistos.ext.supertool",
  "name": "SuperTool",
  "version": "1.0.0",
  "author": "HephaistOS Dev",
  "description": "Outil natif d'optimisation et d'analyse système",
  "minHephaistosVersion": 1,
  "permissions": ["ROOT", "STORAGE", "EXEC_BIN"],
  "exportedBinaries": ["super_tool_arm64"],
  "ui": {
    "hasDedicatedPage": true,
    "addToNavRail": false,
    "tabs": [
      {
        "id": "tab_dashboard",
        "title": "Tableau de Bord",
        "icon": "Dashboard",
        "sections": [
          {
            "type": "banner",
            "title": "Optimiseur Système",
            "subtitle": "Analyse et patch en temps réel"
          },
          {
            "type": "form",
            "fields": [
              { "id": "target_project", "type": "dropdown", "label": "Projet Cible", "source": "unpacked_projects" },
              { "id": "compression_level", "type": "number", "label": "Niveau de Compression", "default": "9" }
            ],
            "action": {
              "id": "run_optimization",
              "label": "Lancer l'Optimisation",
              "icon": "Bolt",
              "script": "scripts/optimize.sh",
              "requiresRoot": true
            }
          }
        ]
      }
    ]
  },
  "ipc": {
    "supportedCommands": ["optimize", "query_status"],
    "dependencies": []
  }
}
```

### Communication Inter-Extensions (IPC) & `$PATH` Unifié
1. **Registre des Extensions (`ExtensionManager.kt`)** :
   - Compile en direct le `$PATH` global incluant `/sdcard/HEPHAISTOS/Extensions/*/bin/`.
   - Fournit un script helper standard `ahe_call` injecté dans l'environnement :
     `ahe_call <extension_id> <action_ou_binaire> [arguments...]`
   - Injection de variables d'environnement dans chaque exécution :
     - `$HEPHAISTOS_WORKSPACE=/sdcard/HEPHAISTOS`
     - `$HEPHAISTOS_EXTENSIONS_DIR=/sdcard/HEPHAISTOS/Extensions`
     - `$HEPHAISTOS_KEYSET_DIR=/sdcard/HEPHAISTOS/SIGNER/KEYSET`
     - `$AHE_SELF_DIR=/sdcard/HEPHAISTOS/Extensions/<current_id>`
2. **Bus de Partage d'Outils** :
   - Une extension peut invoquer les binaires d'une autre extension présente sur le système sans duplication.

---

## 3. Interface Utilisateur dans le Tiroir Plus (+) & Navigation

1. **Bouton d'Importation `.ahe`** :
   - Un bouton principal "+ Importer une Extension (.ahe)" ouvre le sélecteur de documents Android (`.ahe`, `.zip`).
   - Modal d'installation affichant le détail du paquet, la compatibilité et les permissions sollicitées.
   - Extraction instantanée et assignation des droits d'exécution.
2. **Gestionnaire d'Extensions** :
   - Liste des extensions installées avec carte Material 3 :
     - Icône, Nom, Version, Auteur.
     - Bouton "Ouvrir l'Interface" (affiche l'interface dynamique complète conforme au style M3 d'HephaistOS avec onglets et console live).
     - Bouton "Désinstaller".
3. **Lecteur de Documentation Intégré** :
   - Bouton d'accès direct à la documentation de création des extensions, avec visionneuse Markdown intégrée.

---

## 4. Documentation GitHub & Template Prêt à l'Emploi

1. **`DOC_AHE_EXTENSIONS.md`** (fichier markdown complet à la racine du projet prêt pour GitHub) :
   - Guide d'architecture exhaustif.
   - Structure des répertoires et fichiers du format `.ahe`.
   - Spécification du `manifest.json` (déclaration d'onglets, champs, actions, hooks de navigation).
   - Guide de compilation croisée de binaires ARM64 avec Android NDK (`aarch64-linux-android-clang`).
   - Bonnes pratiques pour l'interopérabilité (scripts bash compatibles toybox/busybox, IPC `ahe_call`).
2. **Template Modèle dans `/sdcard/HEPHAISTOS/Templates/ahe_template/`** :
   - `manifest.json` complet avec commentaires explicatifs.
   - `scripts/sample_action.sh` avec gestion des variables HephaistOS et codes de retour.
   - `bin/sample_tool` (script binaire d'exemple avec autorisations).
   - `README.md` d'explication.

---

## 5. Calendrier d'Exécution

- [x] **Phase 1** : Clarification des exigences avec l'utilisateur.
- [ ] **Phase 2** : Refonte de `GemeauSignerEngine.kt` :
  - Extraction fidèle `.img` et détection de structure avec `UkaEngine`.
  - Mappage 1:1 de confiance, préservation stricte des GApps, mise à jour `mac_permissions.xml`, invalidation propre des odex/vdex/oat/fsvmeta.
  - Repack sans perte reproduisant le format exact du fichier source.
- [ ] **Phase 3** : Création du moteur d'extensions `ExtensionManager.kt` et des modèles `AheManifest`, `AheExtension`, `AheUiTab`, `AheField`, etc.
- [ ] **Phase 4** : Interface Material 3 du tiroir Plus (`PlusDrawer.kt`) et rendu dynamique d'écran d'extension (`AheDynamicScreen.kt`).
- [ ] **Phase 5** : Rédaction de `DOC_AHE_EXTENSIONS.md` et génération du template dans `/sdcard/HEPHAISTOS/Templates/ahe_template/`.
- [ ] **Phase 6** : Raccordement dans `HephaistViewModel.kt` et `MainActivity.kt`.
- [ ] **Phase 7** : Compilation et validation finale avec `compile_applet`.
