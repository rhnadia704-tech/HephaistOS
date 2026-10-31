# Plan d'implémentation : Synchronisation Intelligente des Permissions & Fidélité Système Cible (Boot DSU)

## 1. Contexte & Diagnostic du Crash DSU vers Recovery
Lors du test de boot avec DSU Sideloader, l'image repoussée vers Recovery immédiatement après la resignature pour deux causes fondamentales :
1. **Désynchronisation SELinux MMAC (`plat_mac_permissions.xml`)** :
   - Dans Android moderne et GSI, les politiques de sécurité MAC (`etc/selinux/plat_mac_permissions.xml`, `system_ext_mac_permissions.xml`, `product_mac_permissions.xml`) définissent l'association entre les certificats de signature des applications système et leurs étiquettes SELinux (`seinfo="platform"`).
   - L'ancien moteur ne mettait à jour que l'ancien `mac_permissions.xml` standard sans analyser récursivement les fichiers `plat_mac_permissions.xml` ni les déclarations de signatures dans `etc/permissions/*.xml`.
   - Résultat au démarrage : Android init et `system_server` constatent une discordance entre la signature du nouveau certificat Platform et celle enregistrée dans la politique SELinux. Le `system_server` crash, déclenchant `RescueParty` qui bascule le périphérique en Recovery.
2. **Artefact virtuel `[SYS]` sous ZArchiver & Compatibilité Filesystem** :
   - Le dossier `[SYS]` affiché par ZArchiver correspond à l'inode de journalisation EXT4 (`<journal>`). Les partitions système Android d'origine (GSI/Treble) sont montées en lecture seule (`ro`) et n'ont pas de journal actif (`-O ^has_journal`).
   - L'utilisation d'un système sans journal (`^has_journal`) restaure la fidélité exacte de l'image d'origine, élimine l'artefact `[SYS]` et évite les erreurs de montage sous DSU.
3. **Respect des directives utilisateur** :
   - Détection automatique et intelligente de tous les fichiers de permissions nécessitant une mise à jour suite au changement de clés, avec préservation stricte de la syntaxe et de la structure XML.
   - Respect strict de la cohérence de la cible sans forcer de modification des drapeaux AVB (Option 2 choisie).

---

## 2. Modifications Proposées

### A. Moteur d'Audit & Mutation SELinux / Permissions (`GemeauSignerEngine.kt` & `RepackoreEngine.kt`)
- **Détection Automatique & Exhaustive** :
  - Scanner tous les fichiers XML de politique et de permissions dans le dossier de travail :
    - `etc/selinux/plat_mac_permissions.xml`
    - `etc/selinux/system_ext_mac_permissions.xml`
    - `etc/selinux/product_mac_permissions.xml`
    - `etc/selinux/vendor_mac_permissions.xml`
    - `etc/selinux/mac_permissions.xml`
    - Tous les fichiers `etc/permissions/*.xml` et `privapp-permissions-*.xml` contenant des balises `<signer signature="...">` ou des certificats déclarés.
- **Remplacement intelligent des Signatures** :
  - Pour chaque signature originale détectée (ancienne clé Platform, Media, Shared, etc.) remplacée lors de la phase de resignature, mapper automatiquement son empreinte/clé publique hexadécimale vers le certificat du nouveau trousseau.
  - Remplacer les valeurs dans les balises `<signer signature="..."/>` en conservant l'exacte casse, les indentations, commentaires XML et l'attribut `<seinfo value="..."/>`.
  - Mettre à jour les fichiers directement dans l'arborescence déballée avant le repack.

### B. Suppression du dossier `[SYS]` et Cohérence EXT4 Lecture Seule (`RepackoreEngine.kt`)
- Dans la commande de création `mke2fs` et `make_ext4fs` :
  - Désactiver expressément la journalisation EXT4 (`-O ^has_journal`) pour correspondre au format d'origine d'un GSI Android read-only.
  - Spécifier les paramètres conformes Android : `-b 4096 -O ^has_journal,extent,huge_file,flex_bg,uninit_bg,dir_nlink,extra_isize`.
  - Cela élimine le pseudo-dossier `[SYS]` dans ZArchiver et garantit un montage propre par le noyau lors du boot DSU.

### C. Respect des Flags AVB d'Origine
- Ne pas altérer la table des descripteurs AVB ni injecter de patch `disable-verity` forcé (respect du choix utilisateur #2).

---

## 3. Plan de Vérification
1. **Tests Unitaires & Intégration (Robolectric)** :
   - Tester la détection et la mutation automatique d'un fichier `plat_mac_permissions.xml` avec balises `<signer signature="...">` et `<seinfo value="platform"/>`.
   - Tester le repack EXT4 sans journal (`^has_journal`) et vérifier l'absence d'inode de journal et de structure `[SYS]`.
   - Exécuter la suite complète via `gradle :app:testDebugUnitTest`.
2. **Compilation Globale** :
   - Exécuter `compile_applet` pour certifier l'absence de régression.
