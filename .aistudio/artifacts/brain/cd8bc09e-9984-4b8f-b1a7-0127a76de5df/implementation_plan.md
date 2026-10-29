# Refonte Souveraine : Moteur UNPACK/REPACK Unifié, Migration du Workspace & Gestionnaire de Fichiers (Révisé)

Refonte complète du cycle de décompaction et recompilation d'HephaistOS pour éliminer définitivement les lenteurs de l'injection sectorielle, migration du stockage de travail vers le répertoire interne haute performance (`context.filesDir/workspace`), affichage du Gestionnaire de Fichiers sur l'accueil avec double racine (Stockage Interne & Workspace applicatif structuré), et personnalisation dynamique du Navigation Rail (drag-and-drop et appui long).

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Les décisions critiques validées et complétées sont intégrées comme principes cardinaux :

- **Accueil du Gestionnaire de Fichiers avec Double Racine** :
  - **Décision confirmée** : L'accueil du Gestionnaire de Fichiers présente une liste d'accès claire :
    1. **Stockage Interne** (`/storage/emulated/0`) : Accès aux fichiers du téléphone, cartes SD, téléchargements et dossiers publics `/sdcard/HEPHAISTOS/PACKED/`.
    2. **Workspace (Espace de travail applicatif)** (`context.filesDir/workspace`) : Arborescence structurée permettant d'explorer directement chaque projet décompacté (`UNPACKED/<nom_projet>`), ses fichiers extraits, ses APKs, et ses métadonnées de scan (`Data/Signalab/`).
- **Migration du Workspace de Travail (`context.filesDir/workspace`)** :
  - **Décision confirmée** : Tous les décompactages (`UNPACKED/`), métadonnées et fichiers de staging sont désormais hébergés dans le stockage interne de l'application.
  - **Bénéfices immédiats** : Élimination totale des erreurs de liens symboliques (`can't link`), contournement des limitations FUSE/FAT d'Android sur la carte SD, et vitesse d'E/S décuplée.
  - **Sortie publique préservée** : Le dossier `/sdcard/HEPHAISTOS/PACKED/` (et `LABOUT/`) reste le lieu de livraison final des ROMs compilées.
- **Suppression Définitive de l'Injection Sectorielle** :
  - **Décision confirmée** : Zéro scan répétitif d'image brute. Recompilation directe, propre et instantanée à partir des fichiers modifiés et résignés dans le dossier `UNPACKED/`.
- **Navigation Rail Dynamique & Personnalisable** :
  - **Décision confirmée** : Glisser-déposer (drag-and-drop) d'une tuile de l'accueil vers le Rail latéral pour l'épingler ; appui long sur un élément du rail pour le détacher (applicable aux éléments synchronisés avec l'accueil).

---

## 1. Vue d'Ensemble & Objectifs

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             HEPHAISTOS PIPELINE                             │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│   [Source ROM / IMG / ZIP sélectionnée via Gestionnaire]                    │
│              │                                                              │
│              ▼                                                              │
│   ┌───────────────────────────────────┐                                     │
│   │   UNPACK STRUCTURÉ HAUT DÉBIT     │ ──▶ Enregistrement enrichi :        │
│   │   (context.filesDir/workspace/)   │     • symlinks.txt                  │
│   └───────────────────────────────────┘     • fs_config & file_contexts     │
│              │                              • partition_blueprint.json      │
│              ▼                              • inodes_manifest.json          │
│   ┌───────────────────────────────────┐                                     │
│   │   SCAN & RESIGNATION (SCEAU)      │                                     │
│   │   (Mise à jour directe UNPACKED)  │                                     │
│   └───────────────────────────────────┘                                     │
│              │                                                              │
│              ▼                                                              │
│   ┌───────────────────────────────────┐                                     │
│   │   REPACK DIRECT & SOUVERAIN       │ ──▶ ZÉRO INJECTION SECTORIELLE      │
│   │   (Compilation directe du dossier)│     Reconstruction native propre    │
│   └───────────────────────────────────┘                                     │
│              │                                                              │
│              ▼                                                              │
│   [Sortie Finale : /sdcard/HEPHAISTOS/PACKED/ & LABOUT/]                    │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Expérience Utilisateur & Design

### 1. Gestionnaire de Fichiers Réinventé
- **Vue Racine / Accueil du Gestionnaire** :
  - **Section Stockage Interne** : Carte dédiée avec barre de jauge d'espace libre, icône disque/téléphone, accès immédiat à `/storage/emulated/0` et aux répertoires de téléchargement/ROMs.
  - **Section Workspace (Données de l'Application)** : Carte dédiée avec badge "Haute Performance", accès direct aux projets décompactés :
    - `UNPACKED/` : Liste de chaque projet avec nom de partition, nombre de fichiers et taille.
    - `Data/Signalab/` : Rapports de scan cryptographiques, JSONs de signatures et de correspondances.
    - `KEYS/` : Trousseau de clés locales forgées.
- **Navigation et Actions** :
  - Fil d'Ariane dynamique (Breadcrumb) permettant de basculer instantanément entre le Stockage Interne et le Workspace.
  - Options contextuelles : Sélection directe de partition, visualisation rapide d'APK ou fichier texte, copie vers la carte SD.

### 2. Navigation Rail Dynamique (Accueil)
- Glisser-déposer fluide d'une carte de fonctionnalité de l'accueil vers la bordure gauche pour l'épingler dans le Rail.
- Appui long sur une icône du Rail pour afficher un popover de détachement ou la retirer instantanément.

---

## 3. Décisions d'Architecture & Stratégie Technique

### Décision 1 : `WorkspaceManager` Unifié & Structure du Workspace
- `context.filesDir.resolve("workspace")` :
  - `workspace/UNPACKED/<target_name>/` : Répertoire d'extraction principal (support natif des symlinks sans contrainte FUSE).
  - `workspace/Data/` : Blueprints, arborescences JSON et métadonnées d'inodes.
  - `workspace/KEYS/` : Trousseaux de clés privées et certificats.
- Méthode `WorkspaceManager.getRootLocations(context)` retournant :
  - `StorageRoot("Stockage Interne", File("/storage/emulated/0"), icon)`
  - `StorageRoot("Workspace", context.filesDir.resolve("workspace"), icon)`

### Décision 2 : Suppression Radicale de l'Injection Sectorielle
- Remplacement du scan répétitif d'image de 4 Go par :
  - Pour les archives **ZIP / OTA** : Streaming direct depuis `UNPACKED/` vers le fichier cible final dans `PACKED/` ou `LABOUT/`.
  - Pour les partitions **EXT4 / EROFS** : Appel des utilitaires système natifs (`make_ext4fs`, `mkfs.erofs`) ciblant directement le dossier `UNPACKED/`, ou synthèse séquentielle propre à partir de l'arborescence, garantissant que **seuls les éléments de `UNPACKED/` sont packés**.

---

## 4. Architecture des Composants & Modules

```
┌────────────────────────────────────────────────────────────────────────┐
│                          COUCHE DONNÉES / DATA                         │
├────────────────────────────────────────────────────────────────────────┤
│ • WorkspaceManager.kt     : getWorkspaceDir(), getInternalUnpackDir(), │
│                             getRootLocations(context)                  │
│ • RepackoreEngine.kt      : Moteur de compilation directe sans scan     │
│ • UkaEngine.kt            : Extraction & repack redirigés vers         │
│                             l'espace interne rapide                    │
│ • SignalabEngine.kt       : Pipeline complet aligné sur le workspace   │
├────────────────────────────────────────────────────────────────────────┤
│                          COUCHE VUE / UI                               │
├────────────────────────────────────────────────────────────────────────┤
│ • HomeScreen.kt           : Tuile Gestionnaire, drag/drop vers Rail    │
│ • NavigationRail.kt       : Épinglage et appui long pour retirer       │
│ • FileManagerScreen.kt    : Vue accueil à 2 racines (Stockage/Workspace)│
│ • SignalabScreen.kt       : Suivi de compilation fluide sans blocage   │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 5. Plan d'Exécution par Étapes

1. **`WorkspaceManager.kt` & Modèle des Racines** :
   - Mise à jour pour structurer le workspace dans `context.filesDir/workspace`.
   - Fourniture des deux racines d'accès : Stockage Interne & Workspace.
2. **Refonte de `RepackoreEngine.kt`** :
   - Suppression totale des boucles de scan d'APK dans les images brutes.
   - Compilation directe à partir de `unpackedDir`.
3. **Mise à Jour de `SignalabEngine.kt` & `UkaEngine.kt`** :
   - Redirection de l'unpack vers le workspace interne rapide.
   - Sauvegarde enrichie des configurations (`fs_config`, `file_contexts`, `symlinks.txt`).
4. **Gestionnaire de Fichiers & Navigation Rail** :
   - Écran du gestionnaire avec les 2 sections d'accueil : Stockage Interne et Workspace.
   - Ajout de la tuile Gestionnaire sur l'accueil (`HomeScreen.kt`).
   - Implémentation du drag-and-drop vers le Rail et appui long pour détacher.
5. **Vérification & Validation** :
   - Tests unitaires Robolectric (`RepackoreEngineTest.kt`).
   - Compilation complète de l'applet (`compile_applet`).
