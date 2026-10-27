# Fonctionnalité Visuelle & Moteur de Vérification VERISIN

Ce plan détaille la mise en place de la fonctionnalité visuelle **VERISIN** accessible directement depuis l'écran d'accueil d'HephaistOS, avec sélecteur de fichiers, déclencheur d'analyse et affichage approfondi des signatures, tout en renforçant l'interpréteur de commandes du terminal contre les erreurs de saisie (chevrons `<...>` et guillemets).

---

## Décisions Clés & Retours Utilisateur

> [!IMPORTANT]
> - **Emplacement dans l'interface** : Intégration sous la forme d'une carte rapide standard sur l'écran d'accueil (aux côtés d'Unpack/Repack, Extractor, Gemeau et Analyst) ouvrant l'écran dédié VERISIN, ainsi qu'une entrée directe dans le rail de navigation si nécessaire.
> - **Consultation des résultats** : Fiche structurée avec onglets/cartes par certificat, condensats (MD5, SHA, CRC32), clé publique OpenSSL, bits bruts et bouton de copie rapide du rapport intégral.
> - **Sélecteur de fichier APK** : Intégration d'un bouton ouvrant le gestionnaire de fichiers Android (Storage Access Framework) combiné à un champ de saisie directe et des puces d'accès rapide aux APKs détectés dans l'espace de travail `/sdcard/HEPHAISTOS/`.
> - **Résolution du bug visualisé en capture** : Nettoyage automatique des caractères chevrons `<` et `>` saisis par l'utilisateur dans le terminal (`verisin <chemin.apk>`), éliminant l'erreur de fichier introuvable.

---

## 1. Vue d'Ensemble & Fonctionnalités

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             ÉCRAN D'ACCUEIL                                 │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │ 🛡️ VERISIN — Signature Inspection                 [ Ouvrir ➔ ]        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Clic sur Ouvrir
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            ÉCRAN DÉDIÉ VERISIN                              │
│                                                                             │
│  ┌─ Saisie & Sélection APK ──────────────────────────────────────────────┐  │
│  │  [ Chemin de l'APK...                                ] [ 📁 Parcourir ]│  │
│  │  Puces rapides : [ SystemUI.apk ] [ framework-res.apk ] [ ... ]        │  │
│  │  [ 🛡️ VÉRIFIER AVEC VERISIN ]                                          │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
│                                                                             │
│  ┌─ Résultats de l'Analyse Cryptographique ──────────────────────────────┐  │
│  │  Badge : Schéma V1 + V2 + V3 (Valide)                                 │  │
│  │  [ Certificat №1 ] [ Condensats & Hashes ] [ Clé Publique & Base64 ]  │  │
│  │  - Numéro de série : 164328817835266820093013058790104210090404248394  │  │
│  │  - Propriétaire / Émetteur / Validité                                 │  │
│  │  - SHA-256, CRC32, MD5, SHA-512... avec boutons Copier                │  │
│  │  - Clé OpenSSLRSAPublicKey { modulus, publicExponent }                │  │
│  │  - Fichiers générés : /sdcard/HEPHAISTOS/VERISIN/reports/...          │  │
│  │  [ 📋 Copier tout le rapport ] [ 📂 Ouvrir le fichier rapport ]        │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

1. **Carte d'accès rapide sur l'accueil (`HomeScreen.kt`)** :
   - Design rigoureusement aligné sur les autres fonctionnalités (`UnpackRepackQuickCard`, `ExtractorQuickCard`, etc.).
   - Titre, description claire, icône de sécurité et bouton d'action `Ouvrir ➔`.
2. **Écran complet VERISIN (`VerisinScreen.kt`)** :
   - Champ de texte modifiable pour le chemin du fichier APK.
   - Bouton d'ouverture du gestionnaire de fichiers système (`ActivityResultContracts.GetContent()`).
   - Puces de raccourci listant les APKs trouvés dans `/sdcard/HEPHAISTOS/` (notamment `SIGNER/RSIGNER/`, `UNPACKED/`, etc.).
   - Bouton de lancement de la vérification avec retour visuel animé et gestion des erreurs explicite.
   - Restitution exhaustive identique aux spécifications du terminal :
     - Interface Android et chemin complet.
     - Schémas détectés (V1, V2, V3, V3.1, V4).
     - Détails X.509 (Série décimal, Sujet, Émetteur, Validité, Algorithme).
     - Condensats (Code de hachage signé/non-signé, CRC32 décimal/hex, MD5, SHA-1, SHA-224, SHA-256, SHA-384, SHA-512).
     - Données de signature (Base64 certificat, Bits bruts en hexadécimal, Clé publique `OpenSSLRSAPublicKey`, Base64 de signature).
3. **Correctif du Terminal (`HephaistViewModel.kt` & `WorkspaceManager.kt`)** :
   - Filtrage et assainissement automatique des chaînes de commande terminal : suppression des `<` et `>`, des guillemets doubles et simples entourant le chemin.
   - Résolution fiable même si l'utilisateur saisit `verisin <SystemUI.apk>` ou `verisin <sdcard/HEPHAISTOS/SIGNER/RSIGNER/SystemUI.apk>`.

---

## 2. Expérience Utilisateur & Design Visuel

- **Palette & Thème Material 3** :
  - Teinte primaire d'HephaistOS avec accent de sécurité (tons bleu cybernétique / émeraude de validation).
  - Cartes avec surélévation tonale et coins arrondis standardisés (16.dp).
- **Interactions fluides** :
  - Sélection d'un APK via le gestionnaire de fichiers qui remplit instantanément le champ et active le bouton de vérification.
  - Copie instantanée de n'importe quel hachage (ex. SHA-256) ou du rapport complet d'un simple toucher avec confirmation haptique/Snackbar.
  - Sauvegarde persistante du rapport dans `/sdcard/HEPHAISTOS/VERISIN/reports/<apk>_verisin.txt` et dans `Data/<apk>_verisin.json`.

---

## 3. Architecture Technique & Modifications

```
┌────────────────────────────────────────────────────────┐
│                    HephaistOS UI                       │
│  ┌───────────────────────┐  ┌───────────────────────┐  │
│  │      HomeScreen       │  │     VerisinScreen     │  │
│  │ (VerisinQuickCard)    │  │ (Picker + Résultat)   │  │
│  └───────────┬───────────┘  └───────────┬───────────┘  │
└──────────────┼──────────────────────────┼──────────────┘
               │                          │
               ▼                          ▼
┌────────────────────────────────────────────────────────┐
│                   HephaistViewModel                    │
│  - currentScreen: Screen.Verisin                       │
│  - verisinSelectedPath, verisinResult, isVerifying     │
│  - executeTerminalCommand (sanitization <...> & "...") │
└──────────────┬──────────────────────────┬──────────────┘
               │                          │
               ▼                          ▼
┌──────────────────────────┐  ┌──────────────────────────┐
│     WorkspaceManager     │  │      VerisinEngine       │
│  - resolveApkPath        │  │  - verifyApkFile         │
│  - sanitizePath          │  │  - formatReport          │
│  - listAvailableApks     │  │  - saveReports           │
└──────────────────────────┘  └──────────────────────────┘
```

1. **`Screen.kt`** :
   - Ajout de `data object Verisin : Screen("verisin")`.
2. **`WorkspaceManager.kt`** :
   - Amélioration de `resolveApkPath` avec assainissement systématique (`cleanPath = rawPath.trim().removeSurrounding("<", ">").removeSurrounding("\"", "\"").trim()`).
   - Ajout d'une méthode `findWorkspaceApks(context: Context): List<File>` pour proposer des suggestions rapides d'APKs déjà présents dans l'espace HephaistOS.
3. **`VerisinScreen.kt`** :
   - Nouvelle interface composable Material 3 pour l'inspection de signature avec champ de fichier, bouton sélecteur SAF, bouton d'exécution et accordéons/onglets de résultats.
4. **`HomeScreen.kt`** :
   - Ajout du composable `VerisinQuickCard` dans la liste verticale principale.
5. **`MainActivity.kt`** :
   - Gestion de la navigation vers `Screen.Verisin` avec gestion du retour arrière (`BackHandler`).
6. **Tests & Validation** :
   - Tests unitaires Robolectric validant le parsing assaini dans le terminal, la résolution de chemin avec chevrons et le bon déroulement de l'analyse.
