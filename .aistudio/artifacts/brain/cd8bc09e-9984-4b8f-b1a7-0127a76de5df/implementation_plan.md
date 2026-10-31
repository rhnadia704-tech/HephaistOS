# Plan d'implémentation : Résolution du Bootloop DSU vers Recovery (AVB 2.0, dm-verity & Contextes SELinux)

## 1. Diagnostic Approfondi du Crash Immédiat (Écran Splash ➔ Recovery)
L'utilisateur constate un reboot immédiat vers le Recovery dès le logo de démarrage (splash screen), avant même l'animation de boot d'Android, sur un appareil avec AVB / dm-verity en mode Enforcing (Réponse 1 & 3).

Deux facteurs bloquants majeurs provoquent ce comportement sous DSU Sideloader :
1. **Absence ou Invalidation de l'Arbre de Hachage AVB 2.0 / dm-verity (Root Digest Mismatch)** :
   - Dans une partition GSI d'origine, la partition se termine par un footer `AVB0` et un arbre de blocs cryptographiques dm-verity (SHA-256).
   - Lors de la modification des APKs et de la reconstruction EXT4, les blocs de données ont changé. L'image brute résultante ne possède plus de footer AVB valide, ou possède un condensat racine (`root_digest`) discordé.
   - Lors du montage de la partition par `first_stage_mount` dans le noyau, le module `dm-verity` détecte une corruption des blocs, déclenche un `kernel panic` avec option `restart_on_corruption`, renvoyant immédiatement le téléphone en Recovery.
2. **Contextes de Sécurité SELinux Vides ou Invalides (`file_contexts` & `fs_config`)** :
   - Lors de la copie par montage loop, les fichiers extraits dans `/data/local/tmp` héritent du contexte utilisateur Android de l'application hôte (`u:object_r:app_data_file:s0`).
   - Si les étiquettes SELinux (`u:object_r:init_exec:s0`, `u:object_r:system_file:s0`, `u:object_r:shell_exec:s0`) et les UID/GID (`root:root` 0:0, `system:system` 1000:1000) ne sont pas formellement restaurés d'après `file_contexts` et `fs_config` via `setfiles` / `chcon` / `restorecon` ou un script de labelling, le binaire `/system/bin/init` est immédiatement bloqué par la politique SELinux au démarrage.
3. **Respect strict de la directive utilisateur** :
   - *"Garder la logique entière de la cible mais avec une nouvelle [signature / recalcul de l'arbre AVB]"* : recalculer l'arbre dm-verity avec un footer AVB 2.0 officiel et restaurer 100% de la fidélité de la cible (SELinux + permissions + symlinks).

---

## 2. Architecture & Modifications Proposées

### A. Moteur Autonome de Calcul dm-verity & Footer AVB 2.0 (`AvbVerityEngine.kt` / `RepackoreEngine.kt`)
- **Calcul de l'arbre de hachage dm-verity (Level-by-Level SHA-256)** :
  - Découper le système de fichiers EXT4 en blocs de 4096 octets.
  - Calculer récursivement les tables de hachage SHA-256 avec le sel (`salt`) de la partition d'origine (ou un sel sécurisé de 32 octets).
  - Obtenir le `root_digest` exact du nouveau système de fichiers.
- **Synthèse du Footer AVB 2.0 (`AVB0`)** :
  - Écrire le descripteur d'arbre de hachage (`AvbHashtreeDescriptor`) contenant le nom de la partition (`system`), la taille du système de fichiers, l'offset de l'arbre, la taille de l'arbre et le `root_digest`.
  - Apposer le footer AVB 2.0 standard (`AvbFooter` de 64 octets avec magic `AVB0`) dans le dernier bloc de 4096 octets de l'image.
  - Aligner la taille de la partition pour une conformité parfaite avec `fs_mgr` et DSU Sideloader.
- **Accompagnement `vbmeta.img` dédié (Optionnel / Automatique)** :
  - Générer un fichier `vbmeta.img` correspondant signé avec la clé `avb.pk8` du keyset, contenant le descripteur mis à jour pour les utilisateurs souhaitant flasher la chaîne complète.

### B. Restauration Rigoureuse des Contextes SELinux & Permissions (`RepackoreEngine.kt`)
- **Application automatique de `file_contexts` & `fs_config` lors du montage loop** :
  - Injecter un script de labelling SELinux qui lit `file_contexts` et applique les contextes réels (`chcon` ou `restorecon -RF /` ou commande `setfiles` si disponible).
  - Appliquer les UID/GID et modes POSIX d'après `fs_config` (notamment `root:root` 0:0 sur `/system/bin/init`, `/system/bin/sh` à `0:2000`, 0755, suid/sgid).
- **Rétablissement des Liens Symboliques Réels** :
  - Parcourir `symlinks.txt` pour recréer tous les liens relatifs et absolus (`ln -sf`) directement sur le système de fichiers monté avant le démontage et le calcul AVB.
- **Nettoyage strict des artefacts de travail** :
  - S'assurer qu'aucun fichier temporaire (`symlinks.txt`, `fs_config`, `file_contexts`, `repack_blueprint.json`) ne reste dans le système de fichiers final.

---

## 3. Plan de Vérification
1. **Tests Unitaires & Moteur Cryptographique** :
   - Tester le calcul d'un arbre dm-verity sur un jeu de blocs connu et vérifier la structure binaire du footer `AVB0`.
   - Tester la persistance des descripteurs de contextes SELinux et des permissions dans les tests de `RepackoreEngineTest`.
   - Exécuter `gradle :app:testDebugUnitTest`.
2. **Validation Globale du Build** :
   - Exécuter `compile_applet` pour certifier une compilation sans aucune erreur.
