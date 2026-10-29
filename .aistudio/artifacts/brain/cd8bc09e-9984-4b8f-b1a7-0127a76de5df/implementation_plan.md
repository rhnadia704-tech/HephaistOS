# Résolution de l'Erreur EACCES & Sécurisation Symlinks du Repack

Correction définitive des échecs de resignature et de repack (`open failed: EACCES (Permission denied)`) sur les fichiers décompactés, causés par la traversée aveugle des liens symboliques vers le système hôte (comme `system_ext`) et les restrictions de permissions consécutives à l'extraction root.

## Décisions Clés & Confirmation Utilisateur

> [!IMPORTANT]
> Les deux choix confirmés lors de la phase de clarification sont intégrés au cœur de l'architecture :
> 1. **Isolation stricte du Workspace & Préservation des Symlinks** : Le moteur n'échappe plus jamais du dossier décompacté. Les liens symboliques (`system_ext -> /system_ext`, etc.) sont traités comme des métadonnées de redirection et ne sont jamais ouverts comme des flux de fichiers normaux (`FileInputStream`).
> 2. **Normalisation automatique des permissions & Propriété** : Après extraction et avant toute manipulation (audit, resignature, repack), un `chown` sur l'UID/GID applicatif et un `chmod -R 777` sont systématiquement exécutés pour garantir un accès en lecture/écriture total sans blocage SELinux ou restriction root.

---

## 1. Vue d'Ensemble & Diagnostic Technique

### Origine exacte de l'erreur `EACCES (Permission denied)` :
1. **Fuite hors du Workspace via les symlinks de ROM** :
   Dans les ROMs Android récentes (MIUI, HyperOS, ColorOS, GSI), `system/system_ext` est un lien symbolique pointant vers `/system_ext` (chemin absolu de la partition racine). Lors de la traversée récursive (`walkTopDown()`), Java/Kotlin résolvait le lien symbolique et tentait de lire `/system_ext/etc/build.prop` sur le **téléphone réel** de l'utilisateur. Le bac à sable Android (SELinux `untrusted_app`) bloquait immédiatement l'accès au fichier système hôte avec `EACCES (errno 13)`.
2. **Droits d'accès des fichiers créés par `tar` sous `su`** :
   Lorsque l'extraction est réalisée via les commandes root (`su`), les fichiers sont créés avec `uid:gid = 0:0` et certains fichiers conservent des permissions restreintes (`0600` ou `0640`), devenant illisibles pour l'application tournant sous son UID utilisateur standard.

---

## 2. Expérience Utilisateur & Flux Opérationnel

```
┌────────────────────────────────────────────────────────────────────────┐
│                          FLUX SÉCURISÉ REPACK                         │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│  [UNPACK / STAGING]                                                    │
│     │                                                                  │
│     ▼                                                                  │
│  Extraction vers Workspace interne (ext4/f2fs)                         │
│     │                                                                  │
│     ▼                                                                  │
│  Normalisation des Permissions (chown app_uid + chmod 777)             │
│     │                                                                  │
│  [SIGNALAB / GEMEAU]                                                   │
│     │                                                                  │
│     ▼                                                                  │
│  Resignature des APKs et enregistrement sans échec                       │
│     │                                                                  │
│  [REPACKORE]                                                           │
│     │                                                                  │
│     ▼                                                                  │
│  Traversée isolée : Filtre !Files.isSymbolicLink(dir)                   │
│     │                                                                  │
│     ├─► Fichiers réguliers ──► Écriture séquentielle dans la ROM       │
│     └─► Liens symboliques ──► Enregistrement table symlinks EXT4       │
│     │                                                                  │
│     ▼                                                                  │
│  Génération réussie dans sdcard/HEPHAISTOS/PACKED & LABOUT             │
│                                                                        │
└────────────────────────────────────────────────────────────────────────┘
```

1. **Expérience dans SIGNALAB** :
   - Le scan cryptographique cartographie les applications et clés sans tenter de lire les fichiers hors du workspace.
   - La resignature s'exécute de façon fluide sur chaque cluster et enregistre les modifications dans l'espace de travail.
   - Le repack final assemble l'image sans lever d'exception `EACCES`.
2. **Expérience dans Unpack / Repack (UKA)** :
   - Le réassemblage direct lit exclusivement les fichiers locaux appartenant au projet décompacté.
   - Les indicateurs de progression atteignent 100% avec génération du fichier flashable dans `/sdcard/HEPHAISTOS/PACKED/`.

---

## 3. Décisions d'Architecture & Stratégie Technique

### Décision 1 : `onEnter` & Exclusion des Liens Symboliques de Répertoire
- **Implémentation** : Remplacement de `walkTopDown()` brut par un parcours contrôlé qui n'entre jamais dans les liens symboliques :
  ```kotlin
  unpackedDir.walkTopDown().onEnter { dir ->
      !Files.isSymbolicLink(dir.toPath())
  }.filter { file ->
      !Files.isSymbolicLink(file.toPath()) && file.isFile
  }
  ```
- **Bénéfice** : Empêche toute évasion vers le système de fichiers réel de l'appareil hôte Android.

### Décision 2 : Séparation des Liens Symboliques et des Fichiers Réguliers
- **Implémentation** : Les liens symboliques sont capturés séparément via `Files.readSymbolicLink()` et encodés dans les métadonnées de partition (inode EXT4 ou fichier `symlinks.txt` de déploiement) sans jamais appeler `FileInputStream(file)`.
- **Bénéfice** : Zéro risque de tentative d'ouverture d'un lien brisé ou externe via un flux binaire.

### Décision 3 : Routine Système de Réassignation des Droits (`ensureWorkspaceAccessible`)
- **Implémentation** : Une fonction dédiée vérifie et applique récursivement :
  ```bash
  chown -R <app_uid>:<app_gid> /data/user/0/.../workspace/UNPACKED/<project>
  chmod -R 777 /data/user/0/.../workspace/UNPACKED/<project>
  ```
  Exécutée automatiquement à la fin de chaque extraction UKA, avant le scan Signalab, et au démarrage de chaque repack.
- **Bénéfice** : Résolution proactive de tout refus d'accès causé par un résidu root ou une archive tar à permissions restrictives.

---

## 4. Architecture des Composants Modifiés

```
┌───────────────────────────────────────────────────────────────────────────┐
│                          ARCHITECTURE TECHNIQUE                           │
├───────────────────────────────────────────────────────────────────────────┤
│                                                                           │
│   UkaEngine.kt                                                            │
│     ├── mountAndExtractLoopWithShortPath()                                │
│     │     └── Post-extraction : chown app_uid + chmod -R 777              │
│     └── ensureProjectReadable(projectDir)                                 │
│                                                                           │
│   RepackoreEngine.kt                                                      │
│     ├── assembleAutonomousExt4Image()                                     │
│     │     ├── onEnter { !Files.isSymbolicLink(it) }                       │
│     │     ├── Filtrage strict des fichiers réguliers                      │
│     │     └── Gestion des symlinks sans FileInputStream                   │
│     └── compilePartitionImage()                                           │
│           └── Vérification des permissions avant flux                      │
│                                                                           │
│   SignalabEngine.kt                                                       │
│     ├── scanAndMapPartition()                                             │
│     │     └── Scan sécurisé des APKs sans traversée symlinks              │
│     └── executeResignAndRepack()                                          │
│           └── Enregistrement et normalisation avant synthèse finale       │
│                                                                           │
└───────────────────────────────────────────────────────────────────────────┘
```

## Étapes de Validation & Tests
1. **Vérification compilation** via `compile_applet`.
2. **Exécution des tests unitaires** (`gradle :app:testDebugUnitTest --tests com.example.RepackoreEngineTest`) validant le comportement avec des symlinks et des fichiers à permissions restreintes.
3. **Contrôle de conformité** : Confirmation de la génération de l'image de sortie dans `sdcard/HEPHAISTOS/PACKED/` sans exception `EACCES`.
