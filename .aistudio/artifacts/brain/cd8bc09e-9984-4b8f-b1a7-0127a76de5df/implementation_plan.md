# Plan d'Alignement Structurel de l'Image Repackée

## Constat & Diagnostic Précis

Sur les captures fournies par l'utilisateur :
1. **Imbrication dans un sous-dossier `Test/`** :
   - Lors de l'unpack, l'outil décompresse les données dans `UNPACKED/<projet>/<partition>/` (ex. `UNPACKED/Test/Test/`).
   - Lors du repack, le moteur prenait comme répertoire source la racine du projet `UNPACKED/Test/`.
   - Par conséquent, la partition générée contenait un dossier racine `Test/` au lieu de positionner `system/`, `vendor/`, `bin`, `etc`, `init` directement à la racine du système de fichiers.
2. **Pollution par les fichiers techniques** :
   - `file_contexts`, `fs_config` et `symlinks.txt` (générés pour le travail interne) se retrouvaient copiés à la racine du système de fichiers final.

---

## Solutions Architecturales

### 1. Détection Automatique du Véritable Répertoire Payload de Partition
- Déterminer intelligemment la racine effective du système de fichiers à injecter :
  - Si `unpackedDir` contient un sous-dossier correspondant au nom de la partition (ex: `unpackedDir/Test/` ou `unpackedDir/system/`), et que celui-ci contient les fichiers du système d'exploitation (`system/`, `bin`, `etc`, `init`, etc.), alors ce sous-dossier est désigné comme **véritable racine source (`payloadDir`)**.
  - Si les fichiers se trouvent déjà à la racine de `unpackedDir`, c'est `unpackedDir` qui est utilisé directement.
- Résultat : Tous les répertoires et fichiers Android (`system/`, `vendor/`, `bin`, `etc`) sont copiés **directement à la racine `/` de la partition compilée**.

### 2. Filtrage Strict des Fichiers Techniques Internes
- Exclusion systématique des métadonnées internes du flux de copie vers l'image :
  - `symlinks.txt`
  - `fs_config`
  - `file_contexts`
  - `filesystem_tree.json`
  - `repack_blueprint.json`
  - `*.meta.json`
- Ces fichiers sont uniquement utilisés en amont par `RepackoreEngine` pour dimensionner la partition et configurer les descripteurs, mais ne sont **jamais** écrits dans l'image compilée finale.

### 3. Application Unifiée dans `RepackoreEngine`
- L'alignement structurel sera appliqué directement dans `RepackoreEngine.compilePartitionImage`, `synthesizeExt4Partition`, `synthesizeErofsPartition` et `inspectUnpackedSchema`.
- De cette façon, les deux points d'entrée (**SIGNALAB** et **Unpack & Repack / UKA**) produiront instantanément des images impeccables conformes à la structure exacte de la ROM source.

---

## Étapes de Mise en Œuvre

1. **Mise à jour de `RepackoreEngine.kt`** :
   - Implémenter la résolution de `resolvePayloadDirectory(unpackedDir, partitionName): File`.
   - Adapter le pipeline de transfert loop-mount RW pour injecter les contenus de `payloadDir` directement à la racine `$mountDir/` en excluant les fichiers techniques.
   - Aligner `inspectUnpackedSchema` pour calculer le dimensionnement sur les vrais fichiers utiles du système.
2. **Validation & Compilation** :
   - Compiler avec `compile_applet`.
   - Exécuter la suite de tests unitaires avec `gradle :app:testDebugUnitTest`.
