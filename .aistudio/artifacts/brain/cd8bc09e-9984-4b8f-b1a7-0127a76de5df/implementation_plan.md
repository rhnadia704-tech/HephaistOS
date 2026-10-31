# Plan de Reconstitution Intégrale & Restructuration du Moteur de Repack

## Contexte & Diagnostic des Problèmes Observés

1. **Origine de l'image vide (3,25 Go avec seulement `[SYS]` et `lost+found`)** :
   - Dans `RepackoreEngine` et `UkaEngine`, la commande exécutée était :
     `make_ext4fs ... || mke2fs -t ext4 -b 4096 'outputFile.img'`
   - Sur Android moderne (Android 9+), `make_ext4fs` n'existe plus dans le système. La commande bascule donc sur `mke2fs`.
   - `mke2fs` initialise un système de fichiers EXT4 vierge (qui ne contient que `lost+found` et les métadonnées système `[SYS]`), mais n'y insère **aucun** fichier de `UNPACKED`.
   - Comme `mke2fs` retourne le code de succès `0`, le moteur considérait à tort la partition comme terminée et sautait l'assemblage. L'image finale était donc totalement vide de contenu ROM !

2. **Origine du blocage à 94% ("Audit de certification des signatures post-compilation...")** :
   - `verifyCompiledImageSignatures` effectuait une recherche linéaire brute secteur par secteur sur les 3,25 Go de l'image vide via `RandomAccessFile`.
   - Comme l'image ne contenait aucun APK, la boucle scannait la totalité des 3 250 mégaoctets de zéros sur le stockage flash, saturant le bus d'E/S et bloquant l'application.

---

## Architecture de la Solution Proposée

### 1. Pipeline de Reconstitution Souveraine avec Remplissage Réel (Loop Mount RW)
- **Création du conteneur dimensionné** : Création de l'image aux dimensions exactes calculées avec la marge dynamique (+10%).
- **Formatage initial** : `mke2fs -F -t ext4 -b 4096 <img_path>` pour générer la table d'inodes et le superbloc propre.
- **Montage loop read-write sécurisé** : Utilisation du contournement 64-octets déjà validé dans HephaistOS (`/data/local/tmp/uka_repack.raw` -> point de montage `/data/local/tmp/uka_repack_mnt`).
- **Transfert intégral des contenus de `UNPACKED/`** :
  - Pipeline de flux `tar` haute performance : `(cd '<unpackedDir>' && tar -cf - .) | (cd /data/local/tmp/uka_repack_mnt && tar -xf -)` ou copie récursive native avec préservation des métadonnées.
  - Tous les APKs résignés, `build.prop`, binaires, frameworks et fichiers de configuration sont réellement écrits dans le système de fichiers.
- **Synchronisation & Démontage propre** :
  - Exécution de `sync` pour garantir que tous les blocs en cache sont écrits sur le disque.
  - Démontage avec `umount` et libération du périphérique loop.
  - Résultat : ZArchiver et le noyau Android peuvent monter et lire l'intégralité des 7 219 fichiers.

### 2. Sécurisation du Fallback Non-Root & EROFS
- Pour EROFS : Utilisation de `mkfs.erofs -z lz4hc` pointant directement vers le répertoire source `unpackedDir` complet.
- Pour EXT4 sans root : Implémentation d'un constructeur EXT4 autonome écrivant les structures d'inodes, répertoires racines (`ext4_dir_entry_2`) et données de fichiers réels sans laisser l'image vide.

### 3. Audit Cryptographique Rapide & Non-Bloquant
- Remplacer le scan linéaire aveugle de 3 Go par une vérification directe et immédiate des APKs résignés dans le workspace ou par un scan borné à un temps strict (maximum 3 secondes / 16 Mo) avec reprise immédiate en cas de fin de bloc.
- Garantie de progression fluide jusqu'à 100% sans aucun gel.

### 4. Unification de `RepackoreEngine` et `UkaEngine`
- Aligner le code de repack de `UkaEngine.executeRepack` pour utiliser ce même pipeline souverain fiable, évitant tout écart entre le menu UKA et SIGNALAB.

---

## Étapes d'Exécution Prévues

1. **Refonte de `RepackoreEngine.kt`** :
   - Réécrire `synthesizeExt4Partition` pour créer, formater, monter en RW via loop, transférer l'intégralité de `unpackedDir`, synchroniser et démonter proprement.
   - Supprimer le faux-succès de `mke2fs` vide.
   - Optimiser `verifyCompiledImageSignatures` pour éviter tout gel I/O.
2. **Refonte de `UkaEngine.kt`** :
   - Aligner `writeReconstructedImage` sur le moteur de reconstitution avec montage RW et copie réelle des fichiers de `sourceTargetDir`.
3. **Vérification et Compilation** :
   - Compiler avec `compile_applet`.
   - Exécuter la suite de tests unitaires pour valider la chaîne de reconstitution.
