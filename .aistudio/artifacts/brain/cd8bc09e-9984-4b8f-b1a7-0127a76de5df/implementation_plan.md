# Plan d'Implémentation — Moteur VERISIN dans HEPHAISTOS

## 1. Contexte & Objectifs
Le projet vise à créer dans HEPHAISTOS un moteur cryptographique indépendant et évolutif nommé **VERISIN** (`VerisinEngine`).
Conformément aux directives de l'utilisateur :
- Aucun élément graphique supplémentaire n'est ajouté sur l'écran d'accueil ni dans GÉMEAU.
- L'accès se fait exclusivement via le **Terminal HEPHAISTOS** avec les commandes `verisin`, `verisin-help`, `verisin <chemin>` et `verisin verify <chemin>`.
- Le moteur fournit une analyse de signature d'APK ultra-détaillée et exhaustive (Schémas V1, V2, V3, V3.1, détails X.509 complets, hachages CRC32/MD5/SHA1/SHA224/SHA256/SHA384/SHA512, code de hachage signed/unsigned, Base64 du certificat, bits de signature bruts, clé publique OpenSSL avec modulus/exponent, Base64 de signature, extensions X.509 et cible interface/SDK).
- Sauvegarde automatique des rapports dans `/sdcard/HEPHAISTOS/VERISIN/reports/` et résolution automatique des chemins relatifs dans les répertoires d'HephaistOS.

---

## 2. Architecture & Composants

### A. Nouveau Moteur : `VerisinEngine.kt` (`com.example.data`)
- **Inspection de haut niveau des schémas de signature** :
  - Détection V1 (JAR META-INF/*.SF + *.RSA/DSA/EC).
  - Détection V2 (APK Signature Scheme v2, bloc `0x7109871a`).
  - Détection V3 (APK Signature Scheme v3, bloc `0xf05368c0`).
  - Détection V3.1 (APK Signature Scheme v3.1, bloc `0x1b93ad61`).
  - Détection V4 (Fichier `.idsig` si présent).
- **Extraction cryptographique X.509 approfondie** :
  - Parsing de chaque certificat avec `java.security.cert.CertificateFactory`.
  - Serial Number au format BigInteger décimal complet.
  - Propriétaire (Subject DN) et Émetteur (Issuer DN) avec statut d'auto-signature.
  - Validité temporelle formatée : `de d MMM yyyy à d MMM yyyy`.
  - Algorithme de signature (ex: `SHA256withRSA`, `SHA1withRSA`, `ECDSAwithSHA256`).
  - **Condensats et sommes de contrôle de sécurité** :
    - Code de hachage Java (`hashCode` signé et non-signé hex/dec).
    - CRC32 en hexadécimal et entier décimal non signé.
    - MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512 en hexadécimal minuscules.
  - **Représentations binaires complètes** :
    - Base64 du certificat complet encodé en DER.
    - Bits de signature bruts (hexadécimal continu).
    - Clé publique au format OpenSSL (`OpenSSLRSAPublicKey{modulus=...,publicExponent=...}`).
    - Base64 de signature (charge utile brute de signature signée par la clé privée).
- **Inspection du Manifeste & Environnement Android** :
  - Lecture des en-têtes Android : `Interface v16 (36)` ou SDK cible/min/compil.
  - Package name, versionCode, versionName, sharedUserId.
- **Rapports & Persistance** :
  - Export texte formaté rigoureusement conforme à la structure demandée par l'utilisateur.
  - Enregistrement dans `/sdcard/HEPHAISTOS/VERISIN/reports/<apk_name>_verisin_report.txt`.
  - Export JSON détaillé dans `/sdcard/HEPHAISTOS/VERISIN/Data/<apk_name>_verisin.json`.

### B. Gestion des Répertoires : `WorkspaceManager.kt`
- Ajout de :
  - `getVerisinDir(context)` -> `/sdcard/HEPHAISTOS/VERISIN/`
  - `getVerisinReportsDir(context)` -> `/sdcard/HEPHAISTOS/VERISIN/reports/`
  - `getVerisinDataDir(context)` -> `/sdcard/HEPHAISTOS/VERISIN/Data/`
- Méthode de résolution intelligente de fichier :
  - Recherche du fichier par chemin absolu.
  - Si inexistant, recherche dans `UNPACKED/`, `SIGNER/`, `SIGNER/RSIGNER/`, `APKs/`, ou recherche récursive du nom de fichier dans l'espace de travail `/sdcard/HEPHAISTOS/`.

### C. Intégration Terminal : `HephaistViewModel.kt`
- Dans `executeTerminalCommand(input: String)` :
  - Interception des commandes :
    - `verisin` (sans argument) ou `verisin-help` ou `verisin --help` ou `verisin -h` :
      Affiche la bannière VERISIN, la description du moteur, les commandes disponibles et les exemples d'utilisation.
    - `verisin <chemin>` et `verisin verify <chemin>` :
      Résout le chemin de l'APK, invoque `VerisinEngine.verifyApkFile`, affiche le rapport complet dans le terminal, et confirme la sauvegarde dans `/sdcard/HEPHAISTOS/VERISIN/reports/`.
  - Mise à jour de la commande globale `help` pour référencer `verisin`.

---

## 3. Plan de Tests & Validation
- **Tests Unitaires / Robolectric (`VerisinEngineTest.kt`)** :
  - Test de vérification d'un APK signé en V1 + V2 + V3.
  - Validation du format exact de sortie (hachages, OpenSSLRSAPublicKey, bits de signature, base64).
  - Validation de l'aide et de l'interception des commandes `verisin` et `verisin-help`.
- **Compilation finale** :
  - Validation avec `gradle :app:testDebugUnitTest` et `compile_applet`.
