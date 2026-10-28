# Plan d'Implémentation — Fonctionnalité SIGNALAB & Module IMG Signer

Ce plan décrit l'intégration de la suite **SIGNALAB** dans HephaistOS, propulsée par les moteurs **SCEAU**, **VERISIN** et **ANALYST**. Il répond aux exigences d'analyse de signatures ultra-détaillée, de décompaction/recompilation de haute précision (ext4, erofs, payload.bin, zip...), de clustering de clés, de resignature personnalisée ou clone (avec préservation intelligente des GApps), de synchronisation des permissions/contextes SELinux/ART (odex, vdex, oat, fsvmeta), et d'enrichissement de la documentation AHE.

---

## 1. Architecture des Données et Modèles (`Models.kt`)

### Nouvelles Structures de Données :
- **`Screen.SIGNALAB("Signalab")`** : Ajout à la navigation principale et à l'écran d'accueil.
- **`SignalabModule`** : Énumération (`IMG_SIGNER`, et futures extensions).
- **`SignalabScanBlueprint`** :
  - Métadonnées du conteneur d'origine (format ext4, erofs, payload, zip, taille de blocs, taille de partition, descripteurs d'inodes, xattrs).
  - Arborescence complète (`filesystem_tree.json`).
  - Contextes de sécurité SELinux (`file_contexts`, `fs_config`).
- **`ApkSignatureCluster`** :
  - Clé / Empreinte SHA-256 du certificat.
  - Rôle détecté (`PLATFORM`, `MEDIA`, `SHARED`, `NETWORKSTACK`, `GOOGLE_SERVICES`, `THIRD_PARTY`).
  - Liste des applications associées (chemins relatifs dans la partition, noms de paquets, versions).
  - Détails cryptographiques complets issus de `VerisinResult` (schémas V1/V2/V3/V4, algorithmes, tailles de clés, DNs).
- **`SignalabSignChoice`** :
  - Mode : `CUSTOM_SIGN` (identités personnalisées créées selon les spécifications cibles) vs `CLONE_SIGN` (reproduction miroir de la chaîne de confiance et des Subject DNs).
  - Préservation des GApps / Google Services (option activée par défaut pour garantir SafetyNet / Play Protect).
  - Gestion des artefacts ART : Nettoyage / suppression des anciens `.odex`, `.vdex`, `.oat` et nettoyage `fsvmeta` (pour éviter les bootloops de somme de contrôle Dalvik au démarrage) avec régénération runtime.
- **`SignalabUiState`** :
  - Chemin du fichier sélectionné, état du scan en cours, progression détaillée.
  - Résultat du scan : blueprint, clusters détectés, liste des APKs répertoriés, dépendances de permissions.
  - État de la boîte de dialogue de resignature (Popup de choix CUSTOM vs CLONE).
  - État de la resignature et du repack final vers `sdcard/HEPHAISTOS/SIGNER/LABOUT/`.

---

## 2. Gestionnaire d'Espace de Travail (`WorkspaceManager.kt`)

Création et maintenance automatique des nouveaux répertoires :
- `/sdcard/HEPHAISTOS/Data/Signalab/scan/<scan_id>/` :
  - `repack_blueprint.json` (géométrie, format de bloc, xattrs, table de liens).
  - `filesystem_tree.json` (arborescence exhaustive de chaque chemin).
  - `apk_signature_clusters.json` (regroupement des applications par clé).
  - `key_mapping.json` (mapping précis des clés détectées).
  - `permissions_database.json` et `signature_dependencies.json` (fichiers de permissions XML et dépendances de signatures).
- `/sdcard/HEPHAISTOS/SIGNER/LABOUT/` : Répertoire de destination du fichier repacké final.
- `/sdcard/HEPHAISTOS/UNPACKED/<target_name>/` : Extraction haute précision du contenu.

---

## 3. Moteur Principal `SignalabEngine.kt`

Le moteur orchestrera l'ensemble du workflow en 4 phases majeures :

### Phase A : Décompaction Haute Précision
- Prise en charge universelle : `ext4`, `erofs`, `payload.bin`, `zip`, images raw/sparse.
- Détection automatique du système de fichiers et des attributs sous-jacents (modes root et non-root via wrappers natifs existants : `KitchenEngine`, `simg2img`, `payload-dumper-go`, `erofs-utils`, `e2fsdroid`).
- Extraction récursive en conservant :
  - Les liens symboliques et liens physiques (hardlinks).
  - Les permissions UID / GID et les modes octaux.
  - Les descripteurs de sécurité SELinux (`security.selinux`) et les `fs_config`.
- Écriture instantanée du `repack_blueprint.json`.

### Phase B : Scan Approfondi & Audit Cryptographique VERISIN
- Parcours complet de l'arborescence extraite (`app/`, `priv-app/`, `framework/`, `apex/`, etc.).
- Exécution de `VerisinEngine.verifyApkFile` sur chaque fichier APK :
  - Extraction de l'ensemble des schémas de signature (V1, V2, V3, V4).
  - Certificats X.509 : DN du propriétaire, DN de l'émetteur, numéro de série, dates de validité.
  - Condensats : MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512, CRC32, HashCode.
  - Clés publiques, bits de signatures et représentations Base64.
- Regroupement des APKs en clusters cryptographiques (`apk_signature_clusters.json`) et cartographie (`key_mapping.json`).
- Extraction et analyse des permissions dépendantes de la signature (`privapp-permissions-*.xml`, `platform.xml`).

### Phase C : Resignature Intelligente & Synchronisation
- Forge des clés via le moteur `SCEAU` / `KEYCORE` :
  - **Option CUSTOM SIGN** : Génération de clés personnalisées (mêmes algorithmes, même typologie de clé, même schéma) pour chaque cluster de clés, avec conservation scrupuleuse du mapping original.
  - **Option CLONE SIGN** : Clonage miroir du Subject DN et de l'architecture de clé de chaque cluster.
  - **Préservation GApps** : Détection des certificats Google (`CN=Android,O=Google Inc.` / `android@android.com`) pour les laisser intacts si l'option est activée.
- Resignature des APKs via `GemeauSignerEngine`.
- Mise à jour et nettoyage des dépendances :
  - Correction des balises de signature dans les fichiers de permissions XML.
  - Suppression/nettoyage des artefacts précompilés obsolètes (`.odex`, `.vdex`, `.oat`) pour éviter les échecs de boot (bootloop par désynchronisation de checksum Dalvik/ART).
  - Nettoyage / actualisation des métadonnées fs-verity (`fsvmeta`).
  - Restauration et alignement des contextes SELinux et droits Unix.

### Phase D : Recompilation / Repack Haute Fidélité
- Lecture du `repack_blueprint.json`.
- Recompilation dans le format d'origine (ext4 sparse/raw, erofs, zip) en utilisant les contextes de sécurité d'origine et la géométrie de blocs enregistrée.
- Exportation du fichier final vers `/sdcard/HEPHAISTOS/SIGNER/LABOUT/<nom_fichier_signe>.<ext>`.

---

## 4. Interface Utilisateur & Intégration (`SignalabScreen.kt`, `HomeScreen.kt`, `HephaistNavRail.kt`)

- **Écran d'Accueil (`HomeScreen.kt`)** :
  - Ajout d'une carte maîtresse `SignalabQuickCard` dans la liste verticale principale avec indicateur d'état, icône technologique et tag de test `home_signalab_card`.
- **Barre de Navigation Latérale (`HephaistNavRail.kt`)** :
  - Ajout du raccourci `SIGNALAB`.
- **Écran Dédié `SignalabScreen.kt`** :
  - En-tête avec état de l'environnement et sélecteur de module (`IMG Signer`).
  - Champ de sélection de fichier avec sélecteur SAF et chips d'accès rapide (`/sdcard/HEPHAISTOS/`).
  - Bouton d'action **LANCER** déclenchant le scan et l'unpacking haute précision.
  - Zone de progression interactive en direct (étapes : Unpack, Scan Verisin, Clustering, Mapping).
  - Affichage structuré du résumé du scan :
    - Format détecté, taille, géométrie, nombre total de fichiers.
    - Nombre d'APKs scannés, liste des clusters de clés identifiés avec badges de rôles.
  - **Boîte de dialogue (POPUP) de Resignature** :
    - Choix entre **CUSTOM SIGN** et **CLONE SIGN**.
    - Champs de personnalisation d'identité (CN, Org, Pays) si CUSTOM SIGN est sélectionné.
    - Toggles : Préservation des GApps (recommandé), Gestion des fichiers ART (Nettoyer/Régénérer au premier démarrage).
    - Bouton de lancement de la resignature et du repack vers LABOUT.
  - Carte de résultat final avec chemin du fichier généré, boutons pour copier le chemin et inspecter le rapport dans le Terminal.

---

## 5. Enrichissement de la Documentation AHE (`DOC_AHE_EXTENSIONS.md`)

- Enrichissement complet de `DOC_AHE_EXTENSIONS.md` et de la documentation GitHub :
  - Ajout d'une section dédiée à l'intégration avec **SIGNALAB** et **SCEAU**.
  - Spécification des métadonnées étendues de décompaction/recompilation.
  - Exemples concrets d'extensions `.ahe` manipulant des images système et exploitant les résultats de scan de Signalab via l'IPC.
  - Bonnes pratiques pour la conformité SELinux et le dexopt runtime sur Android moderne.

---

## 6. Tests et Validation

- Écriture d'un test unitaire Robolectric complet dans `SignalabEngineTest.kt` vérifiant :
  - L'analyse d'image et le calcul du blueprint.
  - L'arborescence `filesystem_tree.json`.
  - Le clustering des APKs et le mapping des clés.
  - Le flux de resignature CUSTOM et CLONE.
  - L'écriture vers `LABOUT/`.
- Compilation complète via `compile_applet`.
