# Détection Multi-Schémas V1+V2+V3 & Groupement Cryptographique des Signatures (ANALYST / GÉMÉAU)

Cette mise à niveau résout le défaut de détection des signatures APK modernes dans HephaistOS (ANALYST et GÉMÉAU) en restaurant l'extraction native Little-Endian des blocs de signature Android (APK Signing Block v2 & v3), en reproduisant fidèlement le gabarit `apktoolM` (`Schéma de signature : V1 + V2 + V3`, hachages complets, clé publique OpenSSL, bits de signature) et en générant une nouvelle base structurée par groupe d'identité (`signatures_by_group.json`).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Conformément aux clarifications validées :
> - **Regroupement** : Les signatures sont groupées par empreinte cryptographique SHA-256 du certificat selon leur rôle organisationnel dans la partition (Plateforme, Média, Shared UID, Release, NetworkStack).
> - **Niveau de détail** : Chaque groupe contiendra la fiche technique complète (8 algorithmes de hachage, certificats X.509, représentations hexadécimales et Base64) avec une détection multi-schémas garantie (`V1 + V2 + V3`).
> - **Interface ANALYST** : Affichage d'un badge compteur de groupes et raccourci d'export vers `signatures_by_group.json`.

---

### 1. Overview & Core Concept

- **Problème résolu** : L'implémentation précédente échouait à détecter les schémas V2 et V3 en raison d'une lecture Big-Endian sur un conteneur APK Signing Block strictement Little-Endian, masquant la présence des signatures V2 (`0x7109871a`) et V3 (`0xf05368c0`) et retournant uniquement V1 ou false.
- **Bénéfice utilisateur** : Restitution d'un audit de conformité identique aux outils de référence (`apktoolM`, `apksigner`), vérification 100% fiable des partitions déballées (`UNPACKED/`) et cartographie claire des clés de plateforme partagées par les applications système.

---

### 2. User Experience & Visual Design

- **Flux Utilisateur dans ANALYST** :
  1. L'utilisateur sélectionne un dossier de partition déballé dans `sdcard/HEPHAISTOS/UNPACKED/`.
  2. En cliquant sur « LANCER L'ANALYSE », le moteur inspecte l'arborescence, les binaires ELF/HALs et l'ensemble des APKs/APEX.
  3. À la fin de l'audit, l'écran affiche les métriques clés, dont un nouveau badge interactif indiquant le nombre de groupes de signatures identifiés (ex: `4 groupes : Platform, Media, Shared, Release`).
  4. La liste des artefacts générés dans `sdcard/HEPHAISTOS/ANALYST/<cible>/` inclut le nouveau fichier `signatures_by_group.json`.
- **Format Textuel Exact (apktoolM)** :
  ```text
  Vérification de la signature: Interface v16 (36)
  Chemin: /system_ext/priv-app/SystemUI/SystemUI.apk

  Schéma de signature : V1 + V2 + V3

  CERTIFICAT №1
  Type de certificat : X.509
  Version du certificat : 3
  Numéros de série: ...
  Propriétaire du certificat : ...
  Émetteur de certificat : Auto-signé par le propriétaire du certificat
  Validité : de ... à ...
  Algorithme de signature : SHA256withRSA

  Code de hachage : 0x... (...)
  Signature CRC32 : ... (...)
  Signature MD5 : ...
  Signature SHA-1 : ...
  Signature SHA-224 : ...
  Signature SHA-256 : ...
  Signature SHA-384 : ...
  Signature SHA-512 : ...

  Base64 du certificat : ...
  Bits de signature : ...
  Clé publique : OpenSSLRSAPublicKey{modulus=...,publicExponent=10001}
  Base64 de signature : ...
  ```

---

### 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Parseur APK Signing Block Unifié et Pur Kotlin** :
  - *Approche* : Recherche de l'EOCD (End of Central Directory) pour dériver l'offset exact du Central Directory (`cdOffset`), vérification du magic 16-octets `APK Sig Block 42` et lecture des paires ID-valeur en Little-Endian.
  - *Raison* : Évite toute dépendance externe lourde, supporte les gros APKs (>2 Go) sans débordement d'entier et garantit une détection instantanée de V1 (JAR), V2 (Block 0x7109871a), V3 (Block 0xf05368c0) et V4 (`.idsig`).
- **Décision 2 : Schéma du fichier `signatures_by_group.json`** :
  - *Approche* : Regroupement par SHA-256 avec inférence automatique du rôle AOSP (`guessKeyGroupName`) d'après les packages associés (ex: présence de `SystemUI` -> `(Platform Key)`).
  - *Raison* : Permet aux cuisiniers de ROM et auditeurs d'isoler en un clin d'œil toutes les applications partageant la même clé système ou identifiant d'utilisateur (`sharedUserId`).

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANALYST / GÉMÉAU ENGINE                         │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
              ┌────────────────────┴────────────────────┐
              ▼                                         ▼
   ┌──────────────────────┐                  ┌──────────────────────┐
   │   JAR V1 Inspector   │                  │ APK Signing Block 42 │
   │ (META-INF/*.RSA/.EC) │                  │ Little-Endian Parser │
   └──────────┬───────────┘                  └──────────┬───────────┘
              │                                         │
              │  [ V1 ]                       [ V2 ]  [ V3 ]  [ V4 ]
              └────────────────────┬────────────────────┘
                                   │
                                   ▼
              ┌─────────────────────────────────────────┐
              │     Signature Schemes Aggregation       │
              │      (ex: "V1 + V2 + V3") & X.509       │
              └────────────────────┬────────────────────┘
                                   │
         ┌─────────────────────────┼─────────────────────────┐
         ▼                         ▼                         ▼
┌──────────────────┐      ┌──────────────────┐      ┌──────────────────┐
│  signatures.json │      │ signatures_by_   │      │signature_report  │
│  (Liste unitaire)│      │ group.json       │      │.txt (apktoolM)   │
└──────────────────┘      └──────────────────┘      └──────────────────┘
```

#### Modèles de données :
- `SignaturesByGroupReport` :
  - `totalGroups: Int`
  - `totalSignedApks: Int`
  - `totalUnsignedApks: Int`
  - `groups: List<SignatureGroup>`
  - `unsignedApks: List<SignatureGroupApkEntry>`

#### Matrice de Tests :
- Validation de l'extraction sur APK multi-signé V1 + V2 + V3.
- Vérification du rapport textuel conforme au standard apktoolM.
- Contrôle de la création de `signatures_by_group.json` et de sa présence dans `summary_manifest.json`.
