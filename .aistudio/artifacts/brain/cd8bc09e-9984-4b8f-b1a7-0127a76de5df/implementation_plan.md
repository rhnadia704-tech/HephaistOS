# Moteur & Suite Cryptographique SCEAU (VERISIN & KEYCORE)

Ce plan détaille la mise en place de la fonctionnalité maîtresse **SCEAU** au cœur d'HephaistOS, intégrant en présentation verticale épurée le moteur d'inspection **VERISIN** (conservé sans modification) et le nouveau moteur puissant de forge cryptographique **KEYCORE** (avec ses trois volets : *Keymaker*, *Verisin Key* et *Verisin Clone*).

---

## Retours & Choix Utilisateur Confirmés

> [!IMPORTANT]
> - **Accueil HephaistOS** : La fonctionnalité maîtresse **SCEAU** est ajoutée sur l'accueil (carte rapide `SceauQuickCard`) et dans le rail de navigation, devenant le pôle de sécurité cryptographique.
> - **Navigation interne SCEAU** : Présentation verticale simple et épurée listant clairement **1. VERISIN** et **2. KEYCORE**.
> - **VERISIN** : Conservé intégralement tel quel sans aucune régression fonctionnelle ni esthétique.
> - **KEYCORE** :
>   - **Keymaker** : Générateur universel de jeux de clés (RSA 2048/4096, EC, validité et DN).
>   - **Verisin Key** : Création d'une clé alignée sur les métadonnées techniques d'un APK analysé par Verisin (algorithme, taille, structure) avec personnalisation des identités (DN).
>   - **Verisin Clone** : Création d'une signature parallèle / clone reproduisant l'architecture et les DN originaux de l'APK cible, avec proposition de resignature directe.
> - **Stockage et compatibilité** : Clés stockées dans `/sdcard/HEPHAISTOS/SCEAU/KEYCORE/` tout en restant synchronisées avec le trousseau global d'HephaistOS (`SIGNER/keysets/` et GÉMEAU).

---

## 1. Vue d'Ensemble & Parcours Utilisateur

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           ÉCRAN D'ACCUEIL HEPHAISTOS                        │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ 🛡️ SCEAU — Moteur & Pôle Cryptographique                 [ Ouvrir ➔ ] │  │
│  │    Inspection forensique VERISIN • Forge de clés KEYCORE              │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Clic sur Ouvrir
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ESPACE SCEAU (HUB CENTRAL)                          │
│                                                                             │
│  ┌─ 1. VERISIN ──────────────────────────────────────────────────────────┐  │
│  │  🔍 Audit Forensique & Inspection Complète de Signatures APK          │  │
│  │     Détection V1, V2, V3, V3.1, certificats X.509, hachages & OpenSSL │  │
│  │     [ Accéder à VERISIN ➔ ]                                           │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─ 2. KEYCORE ──────────────────────────────────────────────────────────┐  │
│  │  🔑 Forge Cryptographique Avancée de Jeux de Clés & Signatures        │  │
│  │     • Keymaker     : Création rapide de clés personnalisées            │  │
│  │     • Verisin Key  : Clé calquée sur la structure d'un APK cible      │  │
│  │     • Verisin Clone: Clé miroir reproduisant les DN & signatures       │  │
│  │     [ Accéder à KEYCORE ➔ ]                                           │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                   ┌───────────────────┴───────────────────┐
                   ▼                                       ▼
┌─────────────────────────────────────┐ ┌─────────────────────────────────────┐
│           ÉCRAN VERISIN             │ │           ÉCRAN KEYCORE             │
│ (Conservé à l'identique)            │ │ Onglets / Modes :                   │
│ - Sélecteur d'APK SAF / Espace      │ │ 1. [Keymaker]                       │
│ - Badge V1/V2/V3                    │ │ 2. [Verisin Key]                    │
│ - Certificats, Hashes & OpenSSL     │ │ 3. [Verisin Clone]                  │
│ - Export automatique des rapports   │ │ - Aperçu technique                  │
│                                     │ │ - Bouton "Générer la clé"           │
│                                     │ │ - Bouton "Signer directement l'APK" │
└─────────────────────────────────────┘ └─────────────────────────────────────┘
```

---

## 2. Conception de KEYCORE (Les 3 Sous-Outils)

### A. Keymaker (Forge Standard)
- Création rapide de clés de signature Android (PK8 + X.509 PEM / DER / BKS).
- Paramètres :
  - Nom du keyset (ex: `my_rom_keyset`).
  - Common Name (CN), Organisation (O), Unité d'organisation (OU), Localité (L), Région/État (ST), Pays (C).
  - Algorithme : RSA 2048 (standard), RSA 4096 (haute sécurité), ECDSA P-256.
  - Période de validité : de 1 à 30 ans.

### B. Verisin Key (Clé Calquée sur Cible)
- L'utilisateur sélectionne un APK cible (via explorateur SAF ou puces d'accès rapide de `/sdcard/HEPHAISTOS/`).
- **Inspection Verisin automatique** en arrière-plan :
  - Extraction de l'algorithme exact de la cible (ex: `SHA256withRSA`, RSA 2048, ou `SHA1withRSA`).
  - Détection du schéma de signature requis (V1, V2, V3).
- **Formulaire assisté** :
  - Les spécifications techniques (taille de clé, algo de hachage, schéma) sont pré-remplies et verrouillées pour garantir la stricte compatibilité.
  - L'utilisateur saisit uniquement ses identités propres (DN : CN, O, C...).
- Création d'une clé neuve parfaitement conforme aux exigences de l'APK cible.

### C. Verisin Clone (Signature Parallèle & Resignature Directe)
- Analyse complète de l'APK cible avec Verisin.
- **Récupération des métadonnées du certificat original** :
  - Extraction exacte du `Subject DN` (ex: `CN=Android, OU=Android, O=Android, L=Mountain View, ST=California, C=US`).
  - Algorithme identique (`SHA256withRSA`, etc.).
  - Période de validité et propriétés X.509 répliquées.
- **Génération de la paire de clés clonée** :
  - Forge d'une nouvelle clé privée et d'un certificat X.509 miroir portant exactement les mêmes identifiants et attributs que la signature cible.
- **Flux d'action direct** :
  - Sauvegarde du keyset dans `/sdcard/HEPHAISTOS/SCEAU/KEYCORE/clones/<apk_name>_clone/`.
  - Bouton d'action immédiat : **« Resigner l'APK cible avec cette signature clone »** avec support multi-schémas (V1 + V2 + V3).

---

## 3. Architecture Technique & Nouveaux Composants

```
┌────────────────────────────────────────────────────────────────────────┐
│                              Composants UI                             │
│  - HomeScreen.kt         -> SceauQuickCard                             │
│  - HephaistNavRail.kt    -> NavRailItem(SCEAU)                         │
│  - SceauScreen.kt        -> Hub vertical avec sous-écrans :            │
│                             ├─ SceauHubView (Liste Verisin + Keycore)  │
│                             ├─ VerisinScreen (Intégré sans régression) │
│                             └─ KeycoreScreen (Keymaker/Key/Clone)      │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                           HephaistViewModel                            │
│  - sceauUiState (sous-écran actif, état Keycore, clés disponibles)     │
│  - verisinUiState (inchangé et synchronisé)                            │
│  - generateKeycoreKeyset(), cloneApkSignature(), signWithCloneKey()    │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
┌───────────────────────────────────┐ ┌───────────────────────────────────┐
│           VerisinEngine           │ │           KeycoreEngine           │
│  - verifyApkFile (existant)       │ │  - generateStandardKeyset         │
│  - extractCertMetadata            │ │  - createVerisinKeyFromApk        │
│  - parseSignatureSchemes          │ │  - cloneApkSignatureKeyset        │
│                                   │ │  - signApkWithCloneKey            │
└───────────────────────────────────┘ └───────────────────────────────────┘
```

1. **`KeycoreEngine.kt` (`com.example.data`)** :
   - Moteur cryptographique complet s'appuyant sur BouncyCastle / Android Java Security.
   - Génération de paires de clés RSA (2048/4096) et EC.
   - Forge de certificats X.509 auto-signés conformes aux standards Android APK Signing.
   - Export aux formats standardisés : `.pk8` (clé privée DER PKCS#8), `.x509.pem` (certificat), et intégration avec `GemeauSignerEngine`.
2. **`SceauScreen.kt` & `KeycoreScreen.kt` (`com.example.ui.screens`)** :
   - `SceauScreen.kt` : Hub épuré avec navigation fluide (BackHandler vers le Hub, puis vers l'Accueil).
   - `KeycoreScreen.kt` : Sélecteur d'APK, affichage des caractéristiques extraites par Verisin, formulaires DN et boutons d'action unifiés.
3. **`WorkspaceManager.kt`** :
   - Ajout des répertoires dédiés :
     - `getSceauDir()` -> `/sdcard/HEPHAISTOS/SCEAU/`
     - `getKeycoreDir()` -> `/sdcard/HEPHAISTOS/SCEAU/KEYCORE/`
     - `getKeycoreClonesDir()` -> `/sdcard/HEPHAISTOS/SCEAU/KEYCORE/clones/`
4. **`Screen.kt` & `MainActivity.kt`** :
   - Déclaration de `Screen.SCEAU`.
   - Câblage de l'écran avec gestion du retour arrière et synchronisation avec le terminal.

---

## 4. Tests & Vérification

- Tests unitaires Robolectric (`KeycoreEngineTest.kt` et `VerisinEngineTest.kt`) :
  - Validation de la génération de clés Keymaker.
  - Validation de l'extraction et du clonage de certificat via Verisin Clone.
  - Validation de la signature d'un APK test avec la clé clonée.
  - Validation de la navigation et de la persistance des clés.
- Compilation continue via `compile_applet`.
