# Plan d'Implémentation Révisé — Module GEMEAU (Kitchen Signer & Trust Chain Engine)

Le module **GEMEAU** est une suite de cryptographie et de resignature pour HephaistOS, permettant de générer des trousseaux complets (Keysets AOSP + AVB), de signer des APKs individuels ou multiples, et d'exécuter une **resignature intelligente d'une Custom ROM ou d'un GSI**. 

Il intègre une détection automatique des applications protégées (**GApps / Play Services**), dont les signatures sont rigoureusement préservées, ainsi qu'un **moteur de réplication 1:1 de la chaîne de confiance** qui cartographie le schéma de sécurité de l'OS d'origine pour le reproduire avec les nouvelles clés sans bootloop.

---

## 1. Arborescence & Stockage Dédié

Tous les fichiers seront structurés dans l'arborescence standardisée d'HephaistOS :
- `/sdcard/HEPHAISTOS/SIGNER/` : Répertoire racine du module.
- `/sdcard/HEPHAISTOS/SIGNER/KEYSET/<nom_keyset>/` : Trousseaux générés (`releasekey`, `platform`, `shared`, `media`, `networkstack`, `avb.pem`/`avb.pk8` et certificats `.x509.pem`).
- `/sdcard/HEPHAISTOS/SIGNER/Data/` : Base de données factuelle et rapports de sécurité :
  - `rom_trust_chain_analysis.json` (empreintes des clés d'origine, graphe de confiance, groupements `sharedUserId`)
  - `gapps_preservation_manifest.json` (liste des APKs détectés comme GApps/externes dont la signature originale est préservée)
  - `security_audit_report.md` (rapport lisible par l'utilisateur)
- `/sdcard/HEPHAISTOS/SIGNER/signed_apks/` : Sortie des APKs signés unitairement ou par lot.
- `/sdcard/HEPHAISTOS/SIGNER/signed_roms/` : Sortie des ROMs et images resignées (accompagnées de `vbmeta.img` si l'option est sélectionnée).

---

## 2. Les 3 Pages de l'Interface GEMEAU

L'interface reprend à l'identique l'ergonomie M3 de `UnpackRepackScreen` et `ExtractorScreen` (bandeau supérieur avec statut, 3 onglets `SecondaryTabRow`, `HorizontalPager` et console temps réel auto-scrollable) :

### Page 1 : Keymaker (Générateur de Keyset)
- **Champs de configuration** :
  - Nom du Keyset, Common Name (CN), Organisation (O), Unité d'organisation (OU), Pays (C).
  - Algorithme cryptographique : RSA 2048, RSA 4096 ou EC P-256.
  - Durée de validité (10 à 30 ans).
- **Trousseau AOSP & AVB complet généré** :
  - `releasekey` (signature globale système)
  - `platform` (signature du framework Android, SystemUI, Settings)
  - `shared` (processus partagés et synchronisation)
  - `media` (applications et providers multimédia)
  - `networkstack` (pile réseau et connectivité)
  - `avb.pem` & `avb.pk8` (clé privée Android Verified Boot 2.0 pour la signature `vbmeta`)
- Bouton : `GÉNÉRER LE KEYSET COMPLET`.
- Liste des trousseaux existants dans `KEYSET/` avec empreintes SHA-256 et détails.

### Page 2 : SIGNAPK (Signature Multi-APKs)
- Explorateur de fichiers avec support de la **sélection multiple d'APKs**.
- Liste des APKs sélectionnés avec badge de statut.
- Bouton `SIGNER LES APKS`.
- **Boîte de dialogue de sélection de clé** :
  - Choix du keyset dans `KEYSET/`.
  - Choix du rôle (`releasekey`, `platform`, `shared`, `media`).
- Signature réelle (v1 JAR + v2/v3 APK Signature Scheme blocks) et export dans `signed_apks/`.

### Page 3 : ROMsign (Resignature Intégrale de ROM & Réplication de Confiance)
- Sélecteur de ROM, GSI ou partition `.img` (ZIP, EXT4, Sparse, Raw, payload).
- Deux boutons d'action :
  1. `SIGNER` (resignature complète de l'archive/image)
  2. `SIGNER WITH VBMETA` (resignature complète + génération du fichier `vbmeta.img` signé avec la clé AVB et flags `disable-verity`)
- **Popup de sélection du Keyset** à utiliser.
- **Analyse Amont & Préservation Intelligente des GApps** :
  - Détection heuristique des applications Google et services propriétaires (`com.google.*`, `com.android.vending`, Google Play Services, microG, DRM stubs).
  - **Exclusion et préservation stricte** : ces applications conservent leur signature Google d'origine certifiée, évitant les crashs Play Services, Play Protect et SafetyNet.
  - Sauvegarde du manifeste dans `/sdcard/HEPHAISTOS/SIGNER/Data/gapps_preservation_manifest.json`.
- **Moteur de Réplication 1:1 de la Chaîne de Confiance** :
  - Identification des certificats uniques signant les APKs système dans l'OS source.
  - Association 1:1 : Chaque certificat d'origine est mappé vers son équivalent dans le nouveau Keyset (`Key_Source_1` ➔ `platform`, `Key_Source_2` ➔ `media`, etc.). Les applications partageant le même certificat d'origine continuent de partager la même clé.
  - Régénération de `mac_permissions.xml` : Remplacement des anciennes signatures de certificats par les nouvelles signatures SHA-256 afin que les transitions SELinux (`platform`, `privapp`) restent fonctionnelles.
  - Purge des caches dexopt pré-compilés (`.odex`, `.vdex`, `.oat`, `fsvmeta`) pour forcer l'OS à recompiler proprement les APKs resignés au premier démarrage.
  - Si `SIGNER with vbmeta` : recalcul des descripteurs AVB et synthèse de `vbmeta.img`.
- Console temps réel avec jauge de progression, compteur d'APKs traités / préservés et bouton pour afficher le rapport de sécurité.

---

## 3. Architecture Technique (`GemeauSignerEngine.kt`)

Création du composant cryptographique autonome `com.example.data.GemeauSignerEngine` :
1. **Générateur Cryptographique JCA** :
   - Génération des paires de clés RSA/EC et certificats auto-signés X.509 v3.
   - Encodage PKCS#8 DER pour les clés privées `.pk8` et PEM pour les certificats `.x509.pem`.
2. **Moteur de Signature APK v1/v2/v3** :
   - Signature JAR v1 (`META-INF/MANIFEST.MF`, `CERT.SF`, `CERT.RSA`).
   - Insertion du bloc d'authentification APK v2/v3 (ID `0x7109871a` / `0xf05368c0`) avec hachage Merkle Tree.
3. **Moteur d'Audit & Mapping de Confiance** :
   - Analyse des signatures existantes dans les APKs de la ROM.
   - Détection des paquets protégés (`GAppsDetector`).
   - Table de correspondance 1:1 entre clés sources et cibles.
4. **Moteur de Transformation de ROM & AVB** :
   - Remplacement et injection des APKs resignés.
   - Mise à jour des balises `<signer signature="...">` dans `mac_permissions.xml`.
   - Invalidation des fichiers `.odex`, `.vdex`, `.oat`.
   - Synthèse de l'en-tête AVB 2.0 pour `vbmeta.img` avec flags `0x1` (Verification disabled).

---

## 4. Intégration dans l'Application HephaistOS

1. **`Models.kt`** :
   - Ajout de `Screen.GEMEAU("Géméau")`.
   - Modèles : `KeysetInfo`, `ApkSignTarget`, `RomSignTarget`, `GemeauUiState`, `TrustChainReport`.
2. **`WorkspaceManager.kt`** :
   - Création des répertoires `SIGNER/KEYSET`, `SIGNER/Data`, `SIGNER/signed_apks`, `SIGNER/signed_roms`.
3. **`HephaistViewModel.kt`** :
   - État réactif `gemeauUiState`.
   - Actions : `generateKeyset()`, `signApks()`, `analyzeAndSignRom()`, `refreshKeysets()`.
4. **`GemeauScreen.kt`** :
   - Écran Compose à 3 onglets (Keymaker, SIGNAPK, ROMsign) avec modals de choix de clés, rapport de confiance et console live.
5. **`HomeScreen.kt` & `HephaistNavRail.kt`** :
   - Carte rapide `GemeauQuickCard` sur l'écran d'accueil à la suite d'Unpack/Repack et Extractor.
   - Entrée dédiée dans la barre de navigation latérale.
6. **`MainActivity.kt`** :
   - Routage de `Screen.GEMEAU`.

---

## 5. Vérification & Tests
- Compilation complète avec `compile_applet`.
- Exécution des tests unitaires `gradle :app:testDebugUnitTest`.
- Validation de la préservation des GApps et de la réplication 1:1 des certificats.
