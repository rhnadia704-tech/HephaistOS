# Moteur UKA Unpack & Repack Haute Fidélité (Inspiré de blackeangel/UKA)

Plan d'implémentation pour refondre le moteur de désassemblage et réassemblage d'images Android HephaistOS selon l'architecture du projet de référence `blackeangel/UKA`, avec exécution intelligente multi-niveaux (avec ou sans root), contournement de la limite de 64 octets du noyau (`losetup`), extraction totale et fidèle de tous les fichiers et sous-dossiers, et préservation intégrale des liens symboliques et contextes SELinux.

## Examen Utilisateur & Décisions Critiques

> [!IMPORTANT]
> Suite à vos réponses et à l'analyse de votre capture d'écran d'erreur terminal (`losetup: > 64 bytes ; mount: losetup failed`) :
> 1. **Cause racine de l'échec de montage :** Le noyau Linux et la commande `losetup` (toybox/busybox Android) imposent une limite stricte de 64 octets (`LO_NAME_SIZE = 64`) sur le chemin du fichier image. Le chemin complet de votre ROM (`/storage/emulated/0/MT2/Axion-2.8-GSI_treble_arm64-ab-VANILLA-20260719.img`) mesure 73 caractères, ce qui provoquait l'échec immédiat de `losetup` et laissait les dossiers créés vides.
> 2. **Résolution inspirée de blackeangel/UKA :** 
>    - Création d'un alias de chemin ultra-court dans `/data/local/tmp/img.raw` (< 25 caractères) ou résolution directe vers `/data/media/0/...`.
>    - Attribution dynamique d'un périphérique loop libre via `losetup -f`.
>    - Montage loop en lecture seule sur un point temporaire, puis copie intégrale par `tar`/`cp -a` vers le dossier cible pour ne rien omettre (fichiers, symlinks, permissions, UID/GID).
> 3. **Mode Sans Root & Fallbacks Universels :**
>    - Si le terminal n'est pas rooté ou si le noyau restreint les loop devices, déclenchement automatique d'extracteurs d'images user-space (détection de binaires `7z`, décompression sparse native, extraction complète des archives ZIP et Payload).
>    - Aucun dossier vide simulé ne sera généré : si une étape rencontre une anomalie, elle est explicitement signalée et relayée avec une solution alternative sans tronquer les données.

---

## 1. Vue d'Ensemble & Concept Clé

* **Extraction Réelle et Complète :** Remplacer le système précédent par un pipeline robuste calqué sur les scripts éprouvés de UKA (`blackeangel/UKA`). Les images EXT4, EROFS, F2FS, Super et ZIP sont désassemblées dans leur intégralité. Tous les répertoires (`app`, `priv-app`, `framework`, `etc`, `bin`, `lib64`, `media`, `overlay`, `fonts`) contiennent désormais leurs vrais fichiers décompressés.
* **Gestion Avancée des Symlinks & Métadonnées :** Lors de l'extraction, tous les liens symboliques et permissions SELinux sont enregistrés dans `symlinks.txt` et `fs_config` conformément au standard UKA, garantissant un repack bit-à-bit parfait ultérieur.
* **Exécution Hybride Robuste (Root / Sans Root) :** HephaistOS détecte l'état des privilèges et bascule automatiquement sur la stratégie optimale (Loop Mount Privilégié avec bypass de longueur de chemin, ou extracteur de flux utilisateur).

---

## 2. Expérience Utilisateur & Design Visuel

### Flux Utilisateur
1. **Sélection de l'Image Cible :**
   - L'utilisateur sélectionne un fichier d'image (ex : partition GSI de 3,35 Go, Recovery ZIP, Payload).
   - L'écran Unpack affiche les détails détectés : Type, Système de fichiers (EXT4/EROFS/Sparse), taille réelle et somme de contrôle.
2. **Lancement de l'Unpack Total :**
   - L'utilisateur clique sur « Démarrer l'Unpack ».
   - Le Terminal affiche en temps réel les étapes techniques :
     - Résolution du chemin court pour contourner la limite de 64 octets de `losetup`.
     - Liaison du périphérique loop (`/dev/block/loopX`).
     - Montage du système de fichiers et extraction récursive de l'ensemble des fichiers.
     - Sauvegarde des symlinks et de la cartographie `_meta.json`.
3. **Exploration des Fichiers Décompressés :**
   - À l'issue de l'unpack, l'utilisateur ouvre ZArchiver, MT Manager ou le gestionnaire de fichiers dans `/sdcard/HEPHAISTOS/UNPACKED/<cible>/` : tous les dossiers contiennent leurs véritables fichiers d'origine (APKs, bibliothèques `.so`, configurations `.xml`, binaires exécutables).

---

## 3. Décisions Clés de Conception & Arbitrages

* **Contournement de la Limite 64 octets de `losetup` :**
  - *Choix retenu :* Avant d'invoquer `losetup` ou `mount`, créer un lien symbolique court : `ln -sf "<chemin_complet>" /data/local/tmp/uka_in.raw`.
  - *Pourquoi :* `/data/local/tmp/uka_in.raw` fait 25 octets, contournant systématiquement l'erreur `> 64 bytes` observée sur les noyaux Android récents.
* **Mécanisme de Copie Totale :**
  - *Choix retenu :* Utilisation de `tar -cf - -C /data/local/tmp/hephaistos_mnt . | tar -xf - -C "$DEST_DIR"` ou `cp -a -r`.
  - *Pourquoi :* Préserve rigoureusement les permissions POSIX, les horodatages, les nœuds spéciaux et tous les liens symboliques (symlinks).
* **Extraction Sans Root :**
  - *Choix retenu :* Pour les environnements non-rootés, détection des utilitaires de décompression installés sur l'appareil (ex: `7z` qui extrait nativement les images de disque EXT4/ISO/FAT, ou extraction de flux ZIP/payload) couplée à une détection claire des capacités de l'appareil.

---

## 4. Architecture Technique & Pipeline d'Extraction UKA

```
┌─────────────────────────────────────────────────────────────┐
│                    HEPHAISTOS UKA PIPELINE                  │
├─────────────────────────────────────────────────────────────┤
│  [Étape 1 : Analyse & Normalisation Chemin]                 │
│   ├── Vérification Root (RootChecker / su)                  │
│   └── Création lien court : /data/local/tmp/uka_in.raw     │
│                                                             │
│  [Étape 2 : Montage & Déballage (Mode Root)]                │
│   ├── losetup -f -> /dev/block/loopX                        │
│   ├── losetup -r /dev/block/loopX /data/local/tmp/uka_in.raw│
│   ├── mount -t ext4|erofs -o ro loopX /data/local/tmp/mnt   │
│   └── Transfert intégral vers /UNPACKED/<target>/           │
│                                                             │
│  [Étape 3 : Mode Sans Root / Fallback Utilisateur]          │
│   ├── Archives ZIP -> Décompression intégrale ZipFile       │
│   ├── Sparse Images -> simg2img unsparse                    │
│   └── Images RAW -> Décompression 7z / flux sans simulation │
│                                                             │
│  [Étape 4 : Métadonnées & Repack Fidèle]                    │
│   ├── Capture symlinks.txt & fs_config                      │
│   └── Sauvegarde JSON dans HEPHAISTOS/Data/unpack/          │
└─────────────────────────────────────────────────────────────┘
```

### Plan d'Exécution Fichier par Fichier
1. **`app/src/main/java/com/example/data/UkaEngine.kt` :**
   - Implémenter la méthode `mountAndExtractLoopWithShortPath` avec création du lien symbolique court dans `/data/local/tmp/` et commande `losetup -f`.
   - Remplacer le système précédent de dossiers vides par une copie intégrale par flux ou commande d'archive.
   - Ajouter le support du déballage complet sans root (archives ZIP et flux de partitions).
   - Mettre à jour `executeRepack` pour reconstruire l'image à partir des données réelles du dossier décompressé.
2. **`app/src/main/java/com/example/ui/HephaistViewModel.kt` :**
   - Transmettre les logs détaillés de chaque phase (loop setup, montage, copie récursive des éléments) directement dans la console Terminal.
