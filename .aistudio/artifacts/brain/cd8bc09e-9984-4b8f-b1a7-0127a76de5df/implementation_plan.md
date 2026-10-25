# Module R-SIGNER : Resignature Professionnelle de ROMs avec Schémas V1+V2+V3 & Préservation de Confiance

R-SIGNER est le nouveau moteur professionnel de resignature de ROMs, GSIs et conteneurs d'images (`.img`, `.zip`, `payload.bin`) intégré dans GÉMÉAU sous VÉRIFICATION. Il implémente la signature multi-schémas native complète (**V1 + V2 + V3**), une analyse amont exhaustive calquée sur ANALYST, un popup intelligent de génération de keyset 1:1 épousant les groupes détectés, la préservation des GApps (SafetyNet / Play Integrity), l'alignement des fichiers de permissions (`mac_permissions.xml`), la régénération ART/fsvmeta et la reconstruction miroir dans `sdcard/HEPHAISTOS/SIGNER/RSIGNER`.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Conformément à vos confirmations :
> - **Navigation** : R-SIGNER sera positionné dans le menu GÉMÉAU directement en dessous de l'option « 4. VÉRIFICATION » (sous forme de 5e option « 5. R-SIGNER »).
> - **Génération de clés 1:1 dans le popup** : Si aucune clé n'est choisie ou en cas de création recommandée, le popup créera automatiquement un jeu de clés calqué 1:1 sur les groupes de signatures identifiés dans la cible (Platform, Media, Shared UID, Release, NetworkStack, etc.).
> - **Format de sortie final** : Le fichier généré dans `sdcard/HEPHAISTOS/SIGNER/RSIGNER` conservera le format et type exact d'origine de la cible (.zip, .img sparse/raw, payload.bin reconstruit).

---

### 1. Overview & Core Concept

- **Problème résolu** : Keymaker et Signapk ne produisaient jusqu'ici que des blocs V1 et V2, empêchant la validation V3 (`0xf05368c0`) requise par les ROMs Android récentes (Android 9 à 16). De plus, resigner sans respecter la distribution exacte des clés de la ROM de base casse les permissions SELinux (`mac_permissions.xml`) et les signatures des GApps (SafetyNet).
- **Solution apportée** : R-SIGNER fournit un pipeline complet en 6 phases avec deux boutons d'action (`SIGNER` et `SIGNER + VBMETA`), un sélecteur flottant multi-formats, l'injection de signatures V1+V2+V3, la préservation GApps et la reconstruction de conteneur miroir.

---

### 2. User Experience & Visual Design

- **Interface dans GÉMÉAU (Option 5 : R-SIGNER)** :
  - **Sélecteur de cible** : Champ affichant le chemin sélectionné avec bouton d'ouverture du gestionnaire de fichiers flottant (filtrant `.img`, `.zip`, `payload.bin`, `.bin`, `.tar`).
  - **Boutons d'action jumelés** :
    - `SIGNER` (Resignature complète de la ROM)
    - `SIGNER + VBMETA` (Resignature complète + synthèse / resignature du descripteur AVB vbmeta)
  - **Popup de sélection & création de Keyset** :
    - Liste déroulante des keysets existants de Keymaker.
    - Option « Créer un nouveau Keyset (Recommandé) » avec formulaire dynamique (Nom, CN, Organisation, Pays, Validité, Algorithme RSA-2048/4096/EC).
    - Génération automatique calquée sur les groupes cryptographiques découverts dans la ROM cible.
  - **Journal d'exécution & progression en direct** : Visualisation en temps réel des 6 étapes (Audit amont -> Distribution de clés -> Resignature V1+V2+V3 -> Permissions & SELinux -> ART/fsvmeta -> Conteneur final).
  - **Rapport de sortie** : Carte récapitulative avec lien direct vers le conteneur final dans `sdcard/HEPHAISTOS/SIGNER/RSIGNER/`.

---

### 3. Key Product Decisions & Trade-Offs

- **Décision 1 : Implémentation du Schéma V3 (Block ID 0xf05368c0)** :
  - *Approche* : Enrichir le générateur de blocs APK Signing Block pour injecter conjointement le bloc v2 (`0x7109871a`) et le bloc v3 (`0xf05368c0`) avec les descripteurs `minSdkVersion` et `maxSdkVersion`.
  - *Raison* : Permet une vérification sans faille `V1 + V2 + V3` dans ANALYST et `apktoolM`.
- **Décision 2 : Préservation ciblée des signatures GApps / SafetyNet** :
  - *Approche* : Détecter et préserver intacts les packages Google (`com.google.android.gms`, `GoogleServicesFramework`, `Phonesky`, etc.) ainsi que les applications pré-signées OEM dont la resignature briserait le certificat d'attestation ou la compatibilité Play Protect.
  - *Raison* : Évite les bootloops et garantit le passage des tests d'intégrité de l'appareil.
- **Décision 3 : Mise à jour de `mac_permissions.xml` & Fichiers de Signature** :
  - *Approche* : Extraire les certificats des nouvelles clés et mettre à jour les balises `<signer signature="...">` de `mac_permissions.xml`, ainsi que les certificats OTA (`otacerts.zip`).
  - *Raison* : Garantit la cohérence du mécanisme de chaîne de confiance SELinux sous Android.

---

### 4. Architecture Technique & Pipeline des 6 Phases

```
┌────────────────────────────────────────────────────────────────────────┐
│                        R-SIGNER WORKFLOW PIPELINE                      │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
              ┌────────────────────┴────────────────────┐
              ▼                                         ▼
      [ Action : SIGNER ]                   [ Action : SIGNER + VBMETA ]
              │                                         │
              └────────────────────┬────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 1. ANALYSE FACTUELLE APPROFONDIE                              │
   │ • Arborescence complète & extraction des permissions          │
   │ • Détection des groupes de signatures (Platform, Media...)    │
   │ • Cartographie des schémas d'origine (V1, V2, V3, V4)         │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 2. POPUP KEYSET & MAPPAGE INTELLIGENT 1:1                     │
   │ • Sélection keyset existant OU Création nouveau keyset        │
   │ • Synthèse de clés adaptées aux groupes détectés              │
   │ • Préservation automatique GApps / SafetyNet                  │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 3. RESIGNATURE RÉELLE MULTI-SCHÉMAS (V1 + V2 + V3 [+ V4])     │
   │ • MANIFEST.MF + CERT.SF + CERT.RSA (v1)                       │
   │ • Injection APK Sig Block 42 (v2 0x7109871a + v3 0xf05368c0)  │
   │ • Zéro corruption ZIP, alignement 4-octets                    │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 4. ALIGNEMENT SELINUX & PERMISSIONS                           │
   │ • Mise à jour mac_permissions.xml avec nouveaux certificats   │
   │ • Mise à jour otacerts.zip & privapp-permissions              │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 5. GESTION ART, ODEX, VDEX & FSVMETA                          │
   │ • Nettoyage / régénération caches OAT/ART selon arborescence  │
   │ • Recalcul et réalignement fsvmeta si présent                 │
   └───────────────────────────────┬───────────────────────────────┘
                                   │
   ┌───────────────────────────────▼───────────────────────────────┐
   │ 6. CONTENEUR FINAL MIROIR DANS SD CARD                        │
   │ • sdcard/HEPHAISTOS/SIGNER/RSIGNER/<target_reflet>            │
   │ • (Optionnel) Recalcul AVB vbmeta (digest / signature)        │
   └───────────────────────────────────────────────────────────────┘
```

#### Nouveaux Composants de Code :
1. **`WorkspaceManager.kt`** : Ajout de `getRSignerDir(context): File` (`sdcard/HEPHAISTOS/SIGNER/RSIGNER`).
2. **`GemeauSignerEngine.kt`** :
   - Enrichissement du bloc APK Signing Block pour supporter **V3** (`0xf05368c0`).
   - Moteur `executeRSignerPipeline(targetFile, keyset, withVbmeta, onProgress, onLog)`.
3. **`GemeauScreen.kt`** :
   - Ajout de la carte `GemeauSubScreen.RSIGNER` dans `GemeauHubView` sous VÉRIFICATION.
   - Interface dédiée `RSignerPage` avec sélecteur flottant, double bouton (`SIGNER`, `SIGNER + VBMETA`), popup de keyset 1:1 et terminal de progression.
4. **`RSignerTest.kt`** : Tests Robolectric validant la signature V1+V2+V3, le respect des groupes et l'export dans `RSIGNER`.
