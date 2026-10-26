# R-SIGNER Pro : Résignature Intelligente Haute Fidélité & Architecture Anti-Bootloop

Plan d'action architectural pour résoudre le crash mémoire sur GSI volumineux, corriger la structure binaire de l'APK Signature Scheme v3 (élimination du warning `W: V3 - Malformed APK Signature Scheme v3 signature record #1` sur apktoolM), auto-déballer les images brutes, et restructurer complètement `sdcard/HEPHAISTOS/SIGNER/RSIGNER/Data/` avec une base de données de signatures et des fichiers de redistribution 1:1 garantissant un système sans bootloop.

---

## Décisions Clés Validées (Clarifications Utilisateur)

> [!IMPORTANT]
> Les trois options confirmées lors de la phase de clarification régissent directement cette architecture :
> 1. **Auto-déballage transparent des cibles brutes (GSI, ROM .img)** : Si une image n'est pas encore déballée, R-SIGNER orchestre son extraction automatique dans `UNPACKED/` via `UkaEngine` avant d'engager l'analyse.
> 2. **Organisation modulaire de `sdcard/HEPHAISTOS/SIGNER/RSIGNER/Data/`** : Structuration en 4 sous-dossiers dédiés (`database/`, `permissions/`, `mappings/`, `reports/`).
> 3. **Protection anti-crash mémoire stricte** : I/O en streaming par blocs de 4 Mo et gestion cyclique de la mémoire pour traiter sereinement les paquets système de plusieurs centaines de mégaoctets sans dépassement de tas (OOM).

---

## 1. Vue d'Ensemble & Objectifs

L'outil **R-SIGNER** dans GÉMÉAU doit offrir un niveau de fiabilité équivalent aux outils de build AOSP officiels (`apksigner`, `signapk`) tout en s'adaptant à des ROMs hétérogènes, des GSI et des environnements moddés :
- **Intégrité cryptographique absolue (V1 + V2 + V3)** : Aucune altération binaire, reconnaissance sans avertissement par `apktoolM` et validation par le vérificateur système Android (`android.util.apk.ApkSignatureSchemeV3Verifier`).
- **Résistance aux charges lourdes** : Capacité à signer plus de 150 applications système d'un GSI sans fuite de mémoire ni fermeture inopinée de l'application.
- **Zéro Bootloop** : Respect scrupuleux de la chaîne de confiance Android (`sharedUserId`, SELinux MMAC dans `mac_permissions.xml`, certificats OTA dans `otacerts.zip`, cohérence des caches ART `.vdex`/`.odex` et des signatures fs-verity).
- **Audit et traçabilité exhaustifs** : Extraction de toutes les signatures, regroupement méthodique et génération de fichiers de mapping JSON permettant de reproduire ou d'auditer la distribution des clés.

---

## 2. Architecture & Organisation des Données

### Structure de sortie dans `sdcard/HEPHAISTOS/SIGNER/RSIGNER/`

```text
sdcard/HEPHAISTOS/SIGNER/RSIGNER/
├── Data/
│   ├── database/
│   │   ├── target_signatures_db.json       <-- Base complète de tous les APKs (8 hashs, DN, clés)
│   │   └── signatures_by_group.json        <-- Regroupement par empreinte et rôle détecté
│   ├── permissions/
│   │   ├── mac_permissions.xml             <-- Politique SELinux MMAC extraite de la cible
│   │   ├── mac_permissions_patched.xml     <-- Politique mise à jour avec nouvelles signatures
│   │   ├── privapp-permissions-*.xml       <-- Permissions privilégiées extraites
│   │   └── otacerts.zip / otacerts.pem     <-- Certificats de recovery extraits
│   ├── mappings/
│   │   ├── signature_redistribution_map.json <-- Table 1:1 associant ancien cert -> nouvelle clé
│   │   └── gapps_preservation_manifest.json  <-- Liste des paquets Google préservés 100%
│   └── reports/
│       ├── rsigner_audit_report.md         <-- Rapport humain détaillé d'analyse et de résignature
│       └── rsigner_execution_manifest.json <-- Métadonnées d'exécution et hachages SHA256
├── <cible>_resigned.img                    <-- Image finale reconstruite (si source .img/GSI)
├── <cible>_resigned.zip                    <-- Archive flashable finale (si source .zip)
└── vbmeta_<cible>.img                      <-- Image AVB 2.0 synthétisée (si option avec VBMETA)
```

---

## 3. Analyse Technique des Problèmes & Solutions

### A. Correction Binaire du Bloc V3 (`W: V3 - Malformed APK Signature Scheme v3 signature record #1`)

#### Cause Racine
Dans le standard AOSP (`com.android.apksig.internal.apk.v3.V3SchemeVerifier`), la séquence des signatures d'un signataire V3 est structurée sous forme de liste de sous-blocs préfixés par leur propre longueur :
```text
[uint32 totalSignaturesLength]
  └── [uint32 singleSignatureRecordLength]  <-- Était omis dans notre code précédent !
        ├── [uint32 algorithmId (0x0103)]
        ├── [uint32 rawSignatureLength]
        └── [rawSignature bytes]
```
L'absence du préfixe de longueur individuel `singleSignatureRecordLength` conduisait `apksig` et `apktoolM` à interpréter `algorithmId` (259) comme la longueur du sous-bloc, déclenchant l'avertissement `V3_SIG_MALFORMED_SIGNATURE`.

De plus, pour le schéma V3 standard d'Android 9+ (API 28), les champs `minSdkVersion` doivent être initialisés à **28** (et non 24) pour respecter les contraintes de rotation de clé.

#### Solution Précise
Correction du constructeur de `signerBlockV3` :
1. `singleSigRecord` = `[uint32 0x0103] + [uint32 rawSig.size] + [rawSig bytes]`
2. `sigEntryV3` = `[uint32 singleSigRecord.size] + [singleSigRecord]`
3. `sigsSeqV3` = `[uint32 sigEntryV3.size] + [sigEntryV3]`
4. `minSdkVersion = 28`, `maxSdkVersion = 0x7FFFFFFF` dans `signedData` et dans le corps du signataire.

---

### B. Élimination des Crashs Mémoire (OOM) sur Gros APKs et GSI

#### Cause Racine
Lors du traitement d'un GSI (qui contient `SystemUI.apk`, `framework-res.apk`, `webview.apk`, etc., dépassant souvent 100 Mo chacun), le code chargeait `ByteArray(cdOffset.toInt())` et `ByteArray(cdSize.toInt())` directement dans le tas JVM Android. Avec plusieurs buffers alloués simultanément, le processus Android déclenchait un `OutOfMemoryError` fatal fermant subitement l'application.

#### Solution Précise
1. **Streaming par blocs de 4 Mo** : Remplacement des allocations massives de `ByteArray` par des écritures directes de fichier à fichier (`RandomAccessFile` et `FileChannel` / streams bufferisés de 4096 Ko).
2. **Calcul de condensat sans saturation du tas** : Lecture par flux dans le `MessageDigest` pour les sections 1, 3 et 4 de l'APK.
3. **Gestion du cycle de vie mémoire** : Appel de `System.gc()` régulier entre les paquets lourds et libération explicite des buffers temporaires.
4. **Cloisonnement d'erreur** : Protection de chaque signature individuelle par un bloc de reprise évitant qu'une anomalie sur un APK n'interrompe le pipeline global.

---

### C. Déballage Automatique Transparent des GSI et ROMs Brutes

#### Cause Racine
Lorsqu'un utilisateur sélectionne directement `system.img` ou une image GSI non déballée, R-SIGNER ne trouvait pas de dossier déballé dans `UNPACKED/` et tentait une extraction partielle sans le système de fichiers réel.

#### Solution Précise
1. Détection automatique du type de cible (fichier `.img`, GSI sparse/raw).
2. Vérification dans `sdcard/HEPHAISTOS/UNPACKED/<nom_cible>` :
   - Si déjà déballé : réutilisation immédiate avec message informatif.
   - Si non déballé : exécution automatique de `UkaEngine.executeUnpack` dans la console R-SIGNER avec barre de progression dédiée, puis enchaînement direct avec l'analyse.

---

### D. Prévention du Bootloop : Cohérence SELinux, sharedUserId et ART

1. **Cohérence `sharedUserId`** :
   - Tous les APKs partageant un même identifiant de sécurité (`android.uid.system`, `android.uid.shared`, `android.uid.media`, `android.uid.networkstack`) sont garantis d'être signés avec la même clé correspondante du trousseau.
2. **Mise à jour SELinux `mac_permissions.xml`** :
   - Les signatures dans les balises `<signer signature="...">` sont remplacées par la signature hexadécimale de la nouvelle clé Platform/Media/Shared, tout en conservant scrupuleusement les balises `<seinfo value="...">` et `<package name="...">`.
3. **Préservation 100% GApps (SafetyNet / Play Integrity)** :
   - Détection des packages Google officiels (`com.google.android.gms`, `com.android.vending`, `com.google.android.gsf`, etc.) et des certificats émis par Google : signature originale conservée à l'identique.
4. **Synchronisation ART et fs-verity** :
   - Caches `.vdex`, `.odex`, `.oat` et fichiers `.fsvmeta` réalignés pour éviter le rejet au premier boot.

---

## 4. Diagramme de Flux du Pipeline R-SIGNER

```text
┌──────────────────────────────────────────────────────────────┐
│                    SÉLECTION DE LA CIBLE                     │
│               (GSI, ROM .img, .zip, payload)                 │
└──────────────────────────────┬───────────────────────────────┘
                               │
               [La cible est-elle déballée ?]
                ├── Non ──> [UkaEngine.executeUnpack] ──┐
                └── Oui ───────────────────────────────┤
                                                       ▼
┌──────────────────────────────────────────────────────────────┐
│               PHASE 1 : ANALYSE APPROFONDIE                  │
│  - Recensement exhaustif de tous les APKs système            │
│  - Extraction des 8 hashs, DN, clés publiques et bits X.509  │
│  - Extraction permissions (mac_permissions.xml, otacerts)    │
│  - Sauvegarde dans RSIGNER/Data/database/ et permissions/    │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             PHASE 2 : POPUP KEYSET 1:1 & MAPPING             │
│  - Choix trousseau existant OU Création 1:1 automatique      │
│  - Création de signature_redistribution_map.json             │
│  - Enregistrement du manifeste de préservation GApps         │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             PHASE 3 : RÉSIGNATURE PAR FLUX (4 Mo)            │
│  - Traitement sécurisé sans dépassement mémoire (OOM)        │
│  - Génération conforme V1 + V2 + V3 (format binaire AOSP)    │
│  - Préservation des applications GApps / Play Services       │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             PHASE 4 : POLITIQUES & SYNCHRO CACHES            │
│  - Patch mac_permissions.xml (SELinux platform / media)      │
│  - Mise à jour otacerts.zip recovery                         │
│  - Synchronisation caches ART (.vdex/.odex) & fs-verity      │
└──────────────────────────────┬───────────────────────────────┘
                               ▼
┌──────────────────────────────────────────────────────────────┐
│             PHASE 5 : RECONSTRUCTION & EXPORT FINAL          │
│  - Recompilation de partition (.img) ou archive (.zip)       │
│  - Synthèse vbmeta.img AVB 2.0 (si option VBMETA activée)    │
│  - Rapports finaux rsigner_audit_report.md & manifest.json   │
└──────────────────────────────────────────────────────────────┘
```

---

## 5. Plan de Vérification & Tests

1. **Test unitaire de format V3** :
   - Valider que le bloc V3 généré par `computeApkSigningBlockV2AndV3` respecte la séquence de longueur AOSP et que `AnalystEngine.inspectApkSignature` ainsi que le parser de blocs valident `V1: true`, `V2: true`, `V3: true` sans erreur de structure.
2. **Test unitaire du pipeline R-SIGNER complet avec GSI volumineux simulé** :
   - Vérifier le fonctionnement en flux mémoire sans allocation de grands tableaux d'octets.
   - Vérifier l'écriture correcte dans les 4 sous-dossiers de `sdcard/HEPHAISTOS/SIGNER/RSIGNER/Data/`.
3. **Compilation et intégrité de l'applet** :
   - Exécuter `compile_applet` pour garantir l'absence totale de régression ou d'erreur de compilation.
