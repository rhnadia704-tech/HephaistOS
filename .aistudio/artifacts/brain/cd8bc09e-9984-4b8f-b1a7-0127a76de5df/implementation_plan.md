# Plan d'Implémentation : Module ANALYST (Audit d'Arborescence, Signatures Exhaustives & HALs)

Ce document formalise la conception et l'implémentation de la nouvelle fonctionnalité **ANALYST** dans HephaistOS. Ce module permet de disséquer complètement une partition ou ROM déballée dans `UNPACKED/`, d'auditer l'intégralité des signatures APK/APEX selon le gabarit précis demandé par l'utilisateur, et de cartographier tous les HALs, bibliothèques ELF et blobs propriétaires dans une base de données factuelle stockée dans `sdcard/HEPHAISTOS/ANALYST/`.

---

## 1. Objectifs et Spécifications

1. **Interface Utilisateur Épurée (Conforme au prompt)** :
   - Champ déroulant listant tous les projets et partitions présents dans `sdcard/HEPHAISTOS/UNPACKED/`.
   - Bouton d'action principal « LANCER L'ANALYSE ».
   - Affichage de l'état d'avancement (progression, logs de scan en temps réel) et récapitulatif factuel avec raccourcis vers les fichiers générés.
   - Enregistrement dans la navigation principale de l'application (HephaistNavRail, HomeScreen, Screen enum) aux côtés des autres fonctionnalités.

2. **Audit Cryptographique Exhaustif des APKs et APEX** :
   - Scan de 100% des APKs et APEX du système (`/system`, `/system_ext`, `/product`, `/vendor`, `/apex`).
   - Détection des schémas de signature actifs : V1 (JAR Scheme), V2 (APK Signature Scheme v2), V3 (APK Signature Scheme v3).
   - Extraction de chaque certificat X.509 présent et restitution selon le gabarit textuel exact :
     - *Vérification de la signature: Interface v16 (36)*
     - *Chemin: /path/to/app.apk*
     - *Schéma de signature : V1 + V2 + V3*
     - *CERTIFICAT №1 : Type, Version, Numéro de série, Propriétaire (Subject DN), Émetteur (Issuer DN), Validité (dates), Algorithme.*
     - *Code de hachage, Signature CRC32, MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512.*
     - *Base64 du certificat, Bits de signature (hexadécimal), Clé publique (OpenSSL / RSA / EC), Base64 de signature.*

3. **Cartographie Complète de l'Arborescence & Fichiers** :
   - Arborescence totale avec métadonnées : chemin relatif, taille en octets, type MIME/format, permissions UNIX (`fs_config`), contextes SELinux (`file_contexts`).

4. **Dissection ELF des HALs, Bibliothèques (.so) et Blobs Propriétaires** :
   - Analyseur ELF natif pur Kotlin :
     - Magic `\x7fELF`, classe 32-bit / 64-bit, endianness, type (Shared Library `ET_DYN`, Executable `ET_EXEC`), architecture (ARM, AArch64, x86, x86_64).
     - Lecture de la section dynamique : `DT_SONAME` (nom interne de la bibliothèque) et dépendances `DT_NEEDED` (liste de toutes les libs requises).
   - Catégorisation des HALs (HIDL, AIDL, Passthrough) et blobs matériels (Caméra, Audio, Display/DRM, Radio/Modem, Capteurs, Connectivité).

5. **Structuration de la Base de Données dans `sdcard/HEPHAISTOS/ANALYST/<cible>/`** :
   - `tree.json` : Arborescence complète et métadonnées fichiers.
   - `signatures.json` : Base de données JSON riche de toutes les signatures.
   - `signature_report.txt` : Rapport textuel formatté selon le gabarit exact spécifié.
   - `hals_blobs.json` : Cartographie exhaustive des HALs, blobs et dépendances ELF.
   - `summary_manifest.json` : Résumé d'inventaire directement consommable par les fonctionnalités futures.

---

## 2. Architecture Technique et Composants

### A. Gestionnaire de Répertoire (`WorkspaceManager.kt`)
- Ajout de `getAnalystDir(context: Context): File` (`/sdcard/HEPHAISTOS/ANALYST`).
- Ajout de `getAnalystTargetDir(context: Context, targetName: String): File`.
- Inclusion de `"ANALYST"` dans la liste `SUB_DIRS` de HephaistOS.

### B. Moteur d'Analyse (`AnalystEngine.kt`)
- `AnalystEngine.analyzeTarget(context, targetDir, onProgress, onLog): AnalystReport`
- **Composant Signatures** :
  - Détection V1, V2, V3 et parsing ASN.1 des blocs de signature.
  - Calcul des 8 fonctions de hachage cryptographique (CRC32, MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512).
  - Formatage strict du texte selon le gabarit utilisateur.
- **Composant ELF & HALs** :
  - Parseur de header ELF (32-bit ELF32 et 64-bit ELF64).
  - Résolution des tables de chaînes et extraction de `DT_SONAME` et `DT_NEEDED`.
  - Classification automatique des modules HAL (HIDL `@`, AIDL `-service`, `hw/`, etc.).
- **Générateur d'Arborescence & Exporteurs JSON** :
  - Sérialisation modulaire dans `sdcard/HEPHAISTOS/ANALYST/<cible>/`.

### C. Modèle de Données & Écrans
- `com.example.model.Screen` : Ajout de l'entrée `ANALYST("Analyst")`.
- `AnalystScreen.kt` :
  - Composable respectant le design demandé : sélecteur déroulant des dossiers `UNPACKED/`, bouton « LANCER L'ANALYSE », barre de progression M3 et panneau de logs/résumé.
- Intégration dans `HephaistNavRail.kt`, `HomeScreen.kt` et `MainActivity.kt`.

---

## 3. Plan d'Exécution Étape par Étape

1. **Étape 1 : Modèles & WorkspaceManager**
   - Ajouter `Screen.ANALYST` dans `Models.kt`.
   - Ajouter `getAnalystDir` et `getAnalystTargetDir` dans `WorkspaceManager.kt`.
   - Créer `AnalystModels.kt` pour modéliser les résultats d'analyse (certificats, hashs, HALs, arborescence).

2. **Étape 2 : Implémentation du moteur `AnalystEngine.kt`**
   - Implémenter le parseur de signatures APK/APEX (V1/V2/V3), le calcul complet des checksums et le formateur conforme au gabarit.
   - Implémenter le parseur ELF pur Kotlin pour `.so` et exécutables.
   - Implémenter l'explorateur d'arborescence et l'écriture des fichiers (`tree.json`, `signatures.json`, `signature_report.txt`, `hals_blobs.json`, `summary_manifest.json`).

3. **Étape 3 : Création de l'interface `AnalystScreen.kt` & Câblage Navigation**
   - Créer l'interface Material 3 minimale et fonctionnelle demandée.
   - Câbler l'écran dans `MainActivity.kt`, `HomeScreen.kt` et `HephaistNavRail.kt`.

4. **Étape 4 : Tests & Validation**
   - Créer un test unitaire `AnalystEngineTest.kt` validant l'extraction des certificats, le calcul des 8 hashs et le formatage exact du gabarit.
   - Compiler l'applet via `compile_applet` et exécuter `testDebugUnitTest`.

---

## 4. Vérification et Critères d'Acceptation

- [ ] L'écran ANALYST propose la liste déroulante des dossiers `UNPACKED/` et le bouton de lancement.
- [ ] Le rapport textuel `signature_report.txt` correspond rigoureusement à l'exemple fourni par l'utilisateur.
- [ ] Tous les APKs et APEX sont scannés sans exception.
- [ ] Les fichiers `.so` et HALs sont inspectés (SONAME, NEEDED, blobs).
- [ ] Tous les exports JSON modulaires sont créés dans `sdcard/HEPHAISTOS/ANALYST/<cible>/`.
- [ ] Le build compile (`compile_applet`) et les tests unitaires réussissent.
