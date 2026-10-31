# Respect Strict de la Chaîne de Confiance & Détection Dynamique des Signatures du GSI

Ce plan d'implémentation révisé intègre fidèlement les directives de l'utilisateur : abandonner toute fausse piste et respecter scrupuleusement la logique, la structure et la chaîne de confiance native du GSI cible.

## Synthèse des Décisions & Choix Confirmés

> [!IMPORTANT]
> - **Chaîne de confiance de la cible** : Le clonage des clés doit être automatique et intelligent. Il doit cartographier les rôles et clusters de la cible (Platform, Media, Shared, NetworkStack, Vendor) et reproduire fidèlement chaque certificat (DN, algorithmes, tailles de clés).
> - **Préservation des GApps** : Détection automatique et préservation intégrale des applications Google (GApps / Google Play Services) sans altérer leur signature d'origine.
> - **Détection dynamique des fichiers de signature** : Aucun nom de fichier ni chemin en dur. Le moteur effectue une analyse croisée dynamique de l'ensemble de l'arborescence pour repérer tous les fichiers déclarant ou référençant les signatures des APKs d'origine, et opère une mise à jour chirurgicale de leur contenu en préservant 100% de leur structure XML/texte d'origine.
> - **Structure de l'image** : Respect strict du système de fichiers d'origine (EXT4 read-only sans journal ni artefacts de travail), restauration des symlinks natifs et labels `file_contexts` exacts de la cible.

---

## 1. Architecture & Chaîne de Confiance Native

```
┌────────────────────────────────────────────────────────────────────────┐
│                   GSI CIBLE D'ORIGINE DÉBALLÉ                          │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 1. Cartographie Dynamique des Clusters de Signatures                   │
│    - Scan de tous les APKs système                                     │
│    - Détection intelligente des GApps -> Marqué "PRÉSERVÉ INTACT"      │
│    - Regroupement des APKs par certificat d'origine (empreintes SHA256)│
│    - Synthèse de clés miroirs (Verisin Clone) reproduisant fidèlement  │
│      les Distinguished Names (DN), algorithmes et tailles de clés      │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 2. Analyse Croisée & Mutation Dynamique des Déclarations de Signatures │
│    - Recherche de toutes les occurrences des signatures originales     │
│      dans TOUS les fichiers texte/XML/cert de la partition             │
│      (sans liste fermée : plat_mac_permissions, permissions, etc.)    │
│    - Substitution chirurgicale des empreintes/hexadécimaux             │
│    - Préservation intégrale de la structure, balises et indentation    │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 3. Resignature Cohérente & Nettoyage des Caches Dépassés                │
│    - Signature de chaque APK avec la clé miroir de son cluster         │
│    - Purge des caches ART (oat/*.odex, *.vdex) des APKs résignés       │
│      (permettant la compilation JIT saine au premier boot DSU)         │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
                                   ▼
┌────────────────────────────────────────────────────────────────────────┐
│ 4. Reconstitution Souveraine de la Partition Cible                     │
│    - Respect de la géométrie, du format EXT4 et de la racine /         │
│    - Application des contextes exacts issus de file_contexts et        │
│      des UID/GID de fs_config                                          │
│    - Calcul et injection de l'arbre AVB 2.0 dm-verity avec footer AVB0 │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Plan Technique Détaillé

### Étape 1 : Cartographie Intelligente des Clusters & Préservation des GApps
- **Détection des GApps & Packages Délicats** :
  - Identification automatique basée sur l'émetteur du certificat (`Google Inc.`, `Android`, `Google LLC`), le package name (`com.google.android.*`, `com.android.vending`) ou l'emplacement (`*/priv-app/Google*`, `*/priv-app/GmsCore*`, `*/app/Google*`).
  - Ces applications sont exclues de toute resignature pour préserver l'intégrité de Google Play Services et de la chaîne de confiance OEM.
- **Groupement par Empreinte de Certificat** :
  - Extraction de l'ensemble des certificats X.509 de la partition.
  - Regroupement des APKs partageant le même certificat en clusters cohérents (ex: cluster principal système/platform, cluster media, cluster networkstack).
  - Génération automatique pour chaque cluster d'une clé clone miroir conservant le DN complet (`C=US, ST=California, O=Google, CN=Android...`), l'algorithme (`SHA256withRSA`, `SHA256withECDSA`), et la taille de clé (2048/4096 bits ou P-256).

### Étape 2 : Moteur d'Analyse Croisée Dynamique des Fichiers Déclaratifs
- **Zéro liste codée en dur** :
  - Le système extrait la représentation hexadécimale brute (DER encodé en hex), l'empreinte SHA-256 et la signature base64 de chaque certificat original trouvé dans les APKs.
  - Parcours récursif de tous les fichiers de configuration de la partition (`/etc/`, `/system/etc/`, `/system_ext/etc/`, `/product/etc/`, etc.).
  - Analyse du contenu textuel et XML : dès qu'une signature originale est détectée dans un fichier (par exemple à l'intérieur d'un attribut `signature="..."`, d'une balise `<certificate>`, ou d'une liste de certificats), le moteur remplace cette valeur par la nouvelle signature correspondante au cluster de l'APK.
  - **Respect absolu du format** : La casse hexadécimale, les retours à la ligne et la structure XML initiale sont strictement conservés.

### Étape 3 : Nettoyage Ciblé des Caches Précompilés
- Pour chaque APK dont la signature a été modifiée par son clone miroir :
  - Si un dossier `oat/` ou des fichiers `.odex` / `.vdex` lui sont associés dans son sous-répertoire, les supprimer afin d'éviter le rejet de checksum au premier démarrage.
  - Les APKs préservés (notamment les GApps) conservent leurs fichiers `.odex` / `.vdex` intacts.

### Étape 4 : Assemblage et Signature AVB 2.0 Conforme à la Cible
- Lors de la compilation EXT4 par `RepackoreEngine` :
  - Fichiers placés directement à la racine de la partition.
  - Restauration des liens symboliques depuis `symlinks.txt`.
  - Application des permissions d'origine depuis `fs_config`.
  - Application des labels SELinux stricts d'après `file_contexts`.
  - Calcul de l'arbre de hachage dm-verity et apposition du descripteur `AvbHashtreeDescriptor` (180 octets) et du footer `AVB0` (64 octets).

---

## 3. Matrice de Validation & Tests

1. **Test unitaire de détection croisée dynamique** :
   - Vérifier que l'analyseur dynamique localise et remplace les signatures d'origine dans des fichiers XML arbitraires sans connaître leurs noms à l'avance.
2. **Test unitaire du filtre de préservation GApps** :
   - Confirmer qu'un APK signé par Google ou appartenant à la suite GApps est identifié et exempté de toute altération.
3. **Compilation globale et exécution de la suite de tests** :
   - Exécution de `gradle :app:testDebugUnitTest` et vérification avec `compile_applet`.
