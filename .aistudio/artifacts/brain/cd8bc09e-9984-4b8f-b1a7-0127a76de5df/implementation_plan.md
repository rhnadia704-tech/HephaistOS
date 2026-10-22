# Plan d'Implémentation — Moteur Ultime d'Extensions .AHE & Intégration sur l'Accueil d'HephaistOS

Ce plan décrit les spécifications et les travaux pour transformer les extensions **`.ahe`** en véritables moteurs autonomes ultra-puissants surpassant les outils AOSP traditionnels, visibles directement depuis la page d'accueil d'HephaistOS, connectés au terminal global, et accompagnés d'un pont SDK universel.

---

## 1. Visibilité des Extensions .AHE sur l'Accueil d'HephaistOS (`HomeScreen.kt`)

### Section Dédiée aux Extensions .AHE
- Ajout d'une section visuelle moderne **"Extensions .AHE Installées"** sur l'écran d'accueil (`HomeScreen.kt`) :
  - **Cartes Interactives Material 3** : pour chaque extension installée, affichage de son icône, nom, version, auteur, description, badges de capacités (ROOT, STORAGE, BIN) et nombre d'outils exportés.
  - **Actions Directes** :
    - Bouton principal `Ouvrir le Module` : bascule instantanément vers l'interface Material 3 dédiée de l'extension (`AheDynamicScreen`).
    - Indicateur de statut en direct : vert (prêt / connecté) ou animé (en cours d'exécution).
  - **État Vide Invité** : si aucune extension n'est installée, affichage d'une carte d'invitation avec bouton `+ Installer une Extension (.ahe)` et `Découvrir le Template Modèle`.
- **Lien vers la Documentation GitHub des Extensions** :
  - Présence d'une bannière / bouton d'action rapide sur l'accueil : **`Documentation des Extensions (.ahe) [GitHub]`**.
  - Ouvre une visionneuse Markdown intégrée haute définition et propose un bouton pour ouvrir l'URL officielle GitHub dans le navigateur de l'appareil.

---

## 2. Accès au Terminal Global depuis n'importe quelle Extension .AHE (`AheDynamicScreen.kt`)

- Ajout d'un bouton d'action dédié au **Terminal Global** dans la barre supérieure de `AheDynamicScreen` :
  - Clic sur l'icône de terminal ➔ Ouvre ou étire le panneau de terminal coulissant inférieur (`BottomTerminalPanel`) par-dessus l'écran de l'extension.
  - Geste de glissement vers le haut (swipe up) également supporté depuis l'extension pour révéler le terminal.
  - Les logs générés par l'extension continuent d'être injectés en temps réel dans le terminal global d'HephaistOS en plus du terminal local de l'extension.

---

## 3. Super-Moteur d'Extensions .AHE (HephaistOS Ultimate SDK)

Pour permettre aux extensions `.ahe` de dépasser les outils AOSP et d'embarquer des moteurs lourds et complexes (compilateurs, analyseurs de code natif, patchers de partitions, environnements Python/Toybox) :

### A. Pont SDK Universel CLI (`hephaistos_api`) & Shell (`hephaistos_sdk.sh`)
- Génération et injection automatique dans le `$PATH` global d'un exécutable binaire et script `hephaistos_api` permettant à n'importe quel script ou binaire d'une extension d'invoquer HephaistOS :
  - `hephaistos_api unpack <cible_img>` : ordonne au moteur UKA d'HephaistOS de décompresser une image.
  - `hephaistos_api repack <dossier> [format]` : reconstruit une image de partition.
  - `hephaistos_api sign <apk_ou_rom> [keyset]` : commande à GEMEAU de signer un fichier avec les clés système.
  - `hephaistos_api extract [options]` : lance l'extraction de blobs, HALs et dépendances.
  - `hephaistos_api log <message>` : transmet une ligne de log colorée au terminal principal d'HephaistOS.
  - `hephaistos_api notify <titre> <message>` : affiche une notification ou snackbar système.
  - `hephaistos_api get_workspace` : renvoie le chemin `/sdcard/HEPHAISTOS`.

### B. Prise en Charge des Bibliothèques Partagées (`lib/` & `lib64/`)
- Support de dossiers `lib/` et `lib64/` dans l'archive `.ahe`.
- Injection automatique de `LD_LIBRARY_PATH` pointant vers ces répertoires pour charger des bibliothèques C++ dynamiques complexes sans root et sans modifier `/system/lib64`.

### C. Dépendances et Appel Réciproque Inter-Extensions
- Chaque extension peut déclarer des dépendances dans `ipc.dependencies`.
- La commande `ahe_call <extension_id> <action> [args...]` permet une composition modulaire totale (ex: une extension de décompression appelle une extension de patch qui appelle une extension de signature).

---

## 4. Documentation GitHub Mise à Jour (`DOC_AHE_EXTENSIONS.md`)

- Mise à jour de `DOC_AHE_EXTENSIONS.md` avec :
  - La documentation complète de `hephaistos_api` et des variables d'environnement.
  - Le tutoriel d'intégration des bibliothèques partagées (`lib64/`).
  - Des exemples concrets de super-outils de modification d'OS et de portage.
  - Le lien officiel vers le dépôt GitHub.

---

## 5. Calendrier d'Exécution

- [x] **Phase 1** : Clarification et cadrage avec l'utilisateur.
- [x] **Phase 2** : Intégration de la section "Extensions .AHE" et du lien GitHub sur `HomeScreen.kt`.
- [x] **Phase 3** : Ajout du bouton d'ouverture du terminal global dans `AheDynamicScreen.kt` et synchronisation avec `HephaistViewModel`.
- [x] **Phase 4** : Implémentation du pont SDK `hephaistos_api` et `hephaistos_sdk.sh` dans `ExtensionManager.kt` avec support de `lib64/` et `LD_LIBRARY_PATH`.
- [x] **Phase 5** : Enrichissement de `DOC_AHE_EXTENSIONS.md` et du template dans `Templates/ahe_template/`.
- [x] **Phase 6** : Compilation (`compile_applet`) et validation complète par tests unitaires.
