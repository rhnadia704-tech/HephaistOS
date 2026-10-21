# Module EXTRACTOR — Extraction Factuelle & Laboratoire de Portage GSI

Ce plan d'implémentation détaille la création du nouveau module **EXTRACTOR** directement intégré sur la page d'accueil et le rail de navigation d'HephaistOS, avec une interface identique à celle d'**Unpack / Repack**. Ce module est dédié à l'extraction exhaustive des composants de l'appareil (blobs vendor, interfaces HAL, framework, propriétés) et à l'analyse comparative des images déballées (GSI/ROM) pour préparer un portage sans faille.

---

##  User Review & Critical Decisions

> [!IMPORTANT]
> Les réponses aux questions de cadrage ont confirmé :
> - **Extraction Factuelle Totale** : Priorité absolue aux blobs vendor, interfaces HAL, framework, propriétés et dépendances croisées de l'appareil pour établir un socle factuel solide de portage.
> - **Rapports Comparatifs Structurés** : Génération conjointe de fichiers JSON exploitables par machine et de synthèses Markdown claires dans `/sdcard/HEPHAISTOS/EXTRACT/Data/`.

---

## 1. Overview & Core Concept

- **Rôle du Module** : L'EXTRACTOR permet de cartographier avec une précision chirurgicale l'architecture interne du téléphone actuel (HALs actifs, VINTF, blobs binaires, compatibilité Treble/VNDK, overlays framework) et de confronter ces données à n'importe quel GSI ou partition extraite dans `UNPACKED/`.
- **Bénéfice Clé** : Fournit une base de données factuelle et un guide de portage automatisé qui indique exactement quels blobs et configurations du téléphone doivent être injectés dans le GSI pour qu'il démarre et fonctionne parfaitement.

---

## 2. User Experience & Visual Design

### Emplacement & Navigation
- **Page d'Accueil (`HomeScreen.kt`)** : Ajout d'une carte rapide `ExtractorQuickCard` sous `UnpackRepackQuickCard`, arborant le même style soigné (titre distinctif, badge de statut, bouton d'ouverture rapide).
- **Rail de Navigation (`HephaistNavRail.kt`)** : Entrée dédiée `Screen.EXTRACTOR` ("Extractor") avec icône expressive `Icons.Default.Build` / `Memory`.

### Interface Principale (`ExtractorScreen.kt`)
Conçue selon les standards stricts de l'interface `UnpackRepackScreen` :
1. **En-tête Profilé** : Bouton retour, titre "Extracteur & Porting Lab", badge de statut dynamique (Mode Root / Mode Utilisateur).
2. **Carte d'Extraction de l'Appareil Actuel ("EXTRACT")** :
   - Bouton d'action principal `EXTRACT PHONE ASSETS`.
   - Résumé des cibles : Blobs Vendor, HALs VINTF (HIDL/AIDL), Framework & Overlays, Propriétés Système (`getprop`), Table SELinux.
   - Options de filtrage : niveau de désassemblage (analyse des dépendances `.so`, décodage des manifests XML).
3. **Carte d'Analyse Comparative du GSI / Image Unpackée ("ANALYSE UNPACKED")** :
   - **Menu Déroulant (`ExposedDropdownMenuBox`)** : Liste dynamique en temps réel des projets et partitions disponibles dans `/sdcard/HEPHAISTOS/UNPACKED/`.
   - Bouton d'analyse `ANALYSE UNPACKED`.
   - Inspection croisée : Détection des attentes du GSI vs ce que le téléphone fournit (versions de HALs, blobs manquants, patchs Treble requis).
4. **Console d'Exécution & Progression Temps Réel** :
   - Barre de progression animée avec sous-compteur d'éléments et mégaoctets extraits.
   - Terminal auto-défilant avec coloration syntaxique des opérations d'extraction.
   - Liens directs pour ouvrir les rapports générés dans le visualiseur de fichiers ou l'explorateur.

---

## 3. Architecture Technique & Moteur d'Ingénierie (`ExtractorEngine.kt`)

```text
┌────────────────────────────────────────────────────────────────────────┐
│                        MODULE EXTRACTOR HEPHAISTOS                     │
├──────────────────────────────────┬─────────────────────────────────────┤
│      BOUTON 1 : "EXTRACT"        │      BOUTON 2 : "ANALYSE UNPACKED"  │
│  Extraction du Téléphone Actuel  │   Inspection du GSI sélectionné     │
└─────────────────┬────────────────┴──────────────────┬──────────────────┘
                  │                                   │
                  ▼                                   ▼
┌──────────────────────────────────┐┌────────────────────────────────────┐
│ • Inspection VINTF (manifest.xml)││ • Scan /sdcard/HEPHAISTOS/UNPACKED │
│ • Dump getprop & build.prop      ││ • Analyse build.prop du GSI        │
│ • Blobs vendor (/vendor/lib*,etc)││ • Détection des HALs du GSI        │
│ • Parsing ELF (.so NEEDED)       ││ • Confrontation Device vs GSI      │
│ • Overlays & Framework XML       ││ • Identification des patchs Treble │
└─────────────────┬────────────────┘└─────────────────┬──────────────────┘
                  │                                   │
                  ▼                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│            DOSSIER DE DESTINATION : /sdcard/HEPHAISTOS/EXTRACT/        │
│                                                                        │
│   ├── device/          (Blobs réels, HALs, XMLs, libs du téléphone)    │
│   ├── gsi_analysis/    (Architecture détaillée du GSI analysé)        │
│   └── Data/            (Base de données factuelle pour le portage)     │
│       ├── device_specifications.json                                  │
│       ├── hal_dependency_graph.json                                   │
│       ├── blob_manifest.txt                                           │
│       ├── gsi_vs_device_compatibility.json                            │
│       └── porting_guide_generated.md                                  │
└────────────────────────────────────────────────────────────────────────┘
```

### Détail des Fonctions du Moteur :
1. **Extraction de l'Appareil (`extractDeviceAssets`)** :
   - Extrait les manifestes VINTF de l'appareil (`/vendor/etc/vintf/manifest.xml`, `/system/etc/vintf/`).
   - Collecte les propriétés matérielles complètes (`ro.board.platform`, `ro.product.vendor.device`, `ro.vndk.version`, `ro.treble.enabled`).
   - Copie les HALs et blobs cruciaux (`/vendor/bin/hw/*`, `/vendor/lib/hw/*`, `/vendor/lib64/hw/*`).
   - Parse les en-têtes ELF binaires pour extraire les bibliothèques partagées indispensables (`readelf -d` ou analyseur d'en-tête ELF Java).
   - Stocke l'ensemble dans `/sdcard/HEPHAISTOS/EXTRACT/device/`.

2. **Génération de la Base de Données (`EXTRACT/Data/`)** :
   - `device_specifications.json` : Profil matériel factuel (architecture CPU, version VNDK, niveau d'API, surface d'attaque SELinux).
   - `hal_dependency_graph.json` : Graphe des services HAL avec leurs bibliothèques requises.
   - `blob_manifest.txt` : Liste exhaustive formatée pour créer un device tree AOSP/LineageOS.

3. **Analyse de l'Image Unpackée (`analyseUnpackedImage`)** :
   - Charge le projet choisi dans `/sdcard/HEPHAISTOS/UNPACKED/<projet>/`.
   - Analyse ses fichiers système (`system/build.prop`, `/system/etc/vintf/`, services init).
   - Calcule la matrice de compatibilité croisée Device <-> GSI.
   - Produit `porting_guide_generated.md` contenant les recommandations concrètes de portage (ex : "Injecter libcamera.provider.so", "Remplacer l'overlay brightness", "Désactiver AVB verification").

---

## 4. Fichiers Concernés & Modifications

| Fichier | Modification |
|---|---|
| `app/src/main/java/com/example/model/Models.kt` | Ajout de `Screen.EXTRACTOR` dans l'énumération des écrans. |
| `app/src/main/java/com/example/data/WorkspaceManager.kt` | Création et initialisation des répertoires `EXTRACT`, `EXTRACT/device`, `EXTRACT/gsi_analysis` et `EXTRACT/Data`. |
| `app/src/main/java/com/example/data/ExtractorEngine.kt` | Nouveau moteur d'extraction factuelle du téléphone, parsing ELF/XML, et analyseur de compatibilité GSI. |
| `app/src/main/java/com/example/ui/HephaistViewModel.kt` | Gestion de l'état d'extraction, sélection de projet unpacké, progression temps réel et exécution des commandes. |
| `app/src/main/java/com/example/ui/screens/ExtractorScreen.kt` | Nouvel écran Compose avec interface calquée sur `UnpackRepackScreen` (boutons EXTRACT, sélecteur UNPACKED, ANALYSE, terminal interactif). |
| `app/src/main/java/com/example/ui/screens/HomeScreen.kt` | Ajout de la carte rapide `ExtractorQuickCard` et navigation vers l'écran Extractor. |
| `app/src/main/java/com/example/ui/components/HephaistNavRail.kt` | Ajout de l'item Extractor dans le rail de navigation. |
| `app/src/main/java/com/example/MainActivity.kt` | Intégration de la transition vers `ExtractorScreen` dans le routeur principal. |

---

## 5. Vérification & Validation

1. **Compilation** : Exécution de `compile_applet` pour valider l'absence d'erreurs de syntaxe et la cohérence de l'arbre Compose.
2. **Navigation** : Validation du passage fluide Accueil ↔ Extractor via le rail latéral et la carte d'accueil.
3. **Menu Déroulant** : Vérification du listage réactif des dossiers présents dans `/sdcard/HEPHAISTOS/UNPACKED/`.
4. **Structure des Données** : Contrôle de la création de `/sdcard/HEPHAISTOS/EXTRACT/Data/` et de la génération des rapports JSON et Markdown.
