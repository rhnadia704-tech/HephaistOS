# Spécification Technique & Guide de Développement — Extensions Modulaires HephaistOS (.ahe)

> **Standard Officiel d'Extension HephaistOS**  
> *Remplacement souverain des applications satellites par des modules natifs, autonomes et composables.*

---

## 1. Vision & Architecture

Le format **`.ahe`** (**Android HephaistOS Extension**) constitue le socle modulaire d'HephaistOS. Il permet à tout développeur, porteur de ROM ou cuisinier système d'enrichir HephaistOS avec de nouveaux outils, sans nécessiter la compilation d'un APK Android complet.

Chaque extension `.ahe` est une archive autonome qui embarque :
- Des **binaires natifs compilés pour architecture ARM64** (`aarch64-linux-android`) ou ARM32.
- Des **scripts shell (`.sh`) exécutables** compatibles avec l'environnement Bionic, Toybox et Busybox d'Android.
- Des **outils auxiliaires** (archives JAR, tables de conversion, patchs bsp, profils SELinux).
- Une **interface Material 3 déclarative** qui s'intègre harmonieusement dans l'application hôte avec onglets, formulaires dynamiques et terminal en temps réel.
- Un **système de communication inter-extensions (IPC)** permettant aux extensions d'utiliser les outils les unes des autres.

---

## 2. Structure Standardisée d'une Archive `.ahe`

Un fichier `.ahe` est un conteneur ZIP standardisé respectant l'arborescence suivante :

```text
mon_extension.ahe (Archive ZIP)
├── manifest.json              # Définition des métadonnées, UI, permissions et IPC
├── bin/                       # Binaires natifs exécutables (ELF ARM64 / ARM32)
│   ├── super_extractor_arm64
│   └── rom_patcher
├── scripts/                   # Scripts shell pilotés par l'UI ou l'IPC
│   ├── setup.sh
│   ├── run_analysis.sh
│   └── apply_patch.sh
├── tools/                     # Outils annexes, JARs, dictionnaires de blobs, configs
│   └── fix_permissions.conf
├── docs/                      # Documentation markdown embarquée
│   └── README.md
└── icon.png                   # (Optionnel) Icône 512x512 de l'extension
```

---

## 3. Le Fichier de Configuration `manifest.json`

Le fichier `manifest.json` est le cœur de l'extension. Il est lu et validé lors de l'importation par HephaistOS.

### Exemple Complet

```json
{
  "id": "com.monprojet.ext.rompatcher",
  "name": "ROM Patcher Pro",
  "version": "1.2.0",
  "author": "HephaistOS Community",
  "description": "Optimisation, injection de correctifs et analyse comparative d'images système Android.",
  "minHephaistosVersion": 1,
  "permissions": [
    "STORAGE",
    "ROOT",
    "EXEC_BIN"
  ],
  "exportedBinaries": [
    "rom_patcher",
    "super_extractor_arm64"
  ],
  "ui": {
    "hasDedicatedPage": true,
    "addToNavRail": false,
    "tabs": [
      {
        "id": "tab_patch",
        "title": "Patch ROM",
        "icon": "Bolt",
        "sections": [
          {
            "type": "banner",
            "title": "Module d'Injection Natif",
            "subtitle": "Opère directement sur les systèmes de fichiers EXT4, EROFS et F2FS dans /sdcard/HEPHAISTOS."
          },
          {
            "type": "form",
            "title": "Paramètres d'Exécution",
            "subtitle": "Sélectionnez le projet unpacké et le profil de patch",
            "fields": [
              {
                "id": "target_project",
                "type": "dropdown",
                "label": "Projet Cible (UNPACKED)",
                "source": "unpacked_projects",
                "default": ""
              },
              {
                "id": "patch_level",
                "type": "number",
                "label": "Niveau d'Agressivité du Patch (1-5)",
                "default": "3"
              },
              {
                "id": "custom_arg",
                "type": "text",
                "label": "Arguments optionnels",
                "default": "--clean-cache"
              }
            ],
            "action": {
              "id": "apply_patch",
              "label": "Appliquer le Patch",
              "icon": "Play",
              "script": "scripts/apply_patch.sh",
              "requiresRoot": false,
              "inputs": ["target_project", "patch_level", "custom_arg"]
            }
          }
        ]
      }
    ]
  },
  "ipc": {
    "supportedCommands": [
      "check_rom",
      "inject_blob"
    ],
    "dependencies": []
  }
}
```

### Champs Principaux du Manifeste :
- `id` : Identifiant unique inversé (ex: `org.hephaistos.ext.<nom>`).
- `permissions` :
  - `STORAGE` : Accès complet à `/sdcard/HEPHAISTOS/`.
  - `ROOT` : Droit d'invoquer des commandes via `su`.
  - `EXEC_BIN` : Droit d'exécution des binaires dans `bin/`.
- `exportedBinaries` : Liste des binaires qui seront ajoutés au `$PATH` global d'HephaistOS pour être utilisables par d'autres extensions.
- `ui.tabs` : Liste des onglets de l'interface dynamique Material 3. Chaque onglet contient des `sections` (`banner`, `form`, etc.) avec des champs de formulaires et des actions associées.

---

## 4. Compilation des Binaires Natifs (C / C++ / Rust / Go)

Pour que vos binaires s'exécutent sur Android dans HephaistOS, vous devez les compiler pour l'architecture **ARM64 (`aarch64-linux-android`)**.

### Avec l'Android NDK (Recommandé) :

1. Définissez votre compilateur :
```bash
export NDK=/path/to/android-ndk-r26b
export TOOLCHAIN=$NDK/toolchains/llvm/prebuilt/linux-x86_64
export CC=$TOOLCHAIN/bin/aarch64-linux-android33-clang
export CXX=$TOOLCHAIN/bin/aarch64-linux-android33-clang++
```

2. Compilez en liant statiquement les bibliothèques C pour éviter tout problème de dépendance manquante :
```bash
$CC -O3 -static -Wall main.c -o my_native_tool
```

3. Vérifiez le format du fichier généré :
```bash
file my_native_tool
# Sortie attendue : ELF 64-bit LSB executable, ARM aarch64, version 1 (SYSV), statically linked...
```

4. Placez le binaire obtenu dans le dossier `bin/` de votre extension.

---

## 5. Environnement d'Exécution & Variables Exportées

Lorsqu'un script de votre extension est exécuté par HephaistOS, les variables d'environnement suivantes sont **automatiquement injectées** :

| Variable | Description |
| :--- | :--- |
| `$HEPHAISTOS_WORKSPACE` | Répertoire racine (`/sdcard/HEPHAISTOS`) |
| `$HEPHAISTOS_EXTENSIONS_DIR` | Répertoire de toutes les extensions (`/sdcard/HEPHAISTOS/Extensions`) |
| `$HEPHAISTOS_UNPACKED_DIR` | Dossier des ROMs/GSI déballées (`/sdcard/HEPHAISTOS/UNPACKED`) |
| `$HEPHAISTOS_PACKED_DIR` | Dossier de sortie des ROMs repackées (`/sdcard/HEPHAISTOS/PACKED`) |
| `$HEPHAISTOS_KEYSET_DIR` | Dossier des clés de signature (`/sdcard/HEPHAISTOS/SIGNER/KEYSET`) |
| `$AHE_SELF_DIR` | Répertoire d'installation de votre extension |
| `$PATH` | Chemin enrichi contenant le dossier `bin/` de **toutes** les extensions installées |
| `$AHE_INPUT_<field_id>` | Valeur saisie par l'utilisateur pour le champ `<field_id>` du formulaire |

### Exemple de Script Shell (`scripts/apply_patch.sh`) :

```bash
#!/system/bin/sh
set -e

echo "[AHE PATCHER] Démarrage du script d'application de patch..."
echo "[ENV] Workspace racine : $HEPHAISTOS_WORKSPACE"
echo "[ENV] Extension locale : $AHE_SELF_DIR"
echo "[ARG] Projet sélectionné : $AHE_INPUT_target_project"
echo "[ARG] Niveau de patch : $AHE_INPUT_patch_level"

TARGET_DIR="$HEPHAISTOS_UNPACKED_DIR/$AHE_INPUT_target_project"

if [ ! -d "$TARGET_DIR" ]; then
    echo "[ERREUR] Le projet $AHE_INPUT_target_project n'existe pas dans UNPACKED."
    exit 1
fi

# Appel du binaire natif inclus dans l'extension
"$AHE_SELF_DIR/bin/rom_patcher" --dir "$TARGET_DIR" --level "$AHE_INPUT_patch_level"

echo "[SUCCÈS] Opération terminée avec succès !"
exit 0
```

---

## 6. Communication Inter-Extensions (IPC)

HephaistOS intègre un bus de communication inter-extensions. Une extension peut invoquer directement les outils ou scripts d'une autre extension via la commande universelle **`ahe_call`** :

```bash
# Syntaxe :
ahe_call <extension_id> <binaire_ou_script> [arguments...]

# Exemple : Invoquer un outil de décompression fourni par une autre extension
ahe_call org.hephaistos.ext.decompressor unsparse_tool /sdcard/HEPHAISTOS/system.img
```

De plus, tous les binaires déclarés dans `exportedBinaries` sont directement présents dans le `$PATH` global, permettant une interopérabilité directe et transparente.

---

## 7. Packaging & Installation dans HephaistOS

1. Placez tous vos fichiers dans un dossier de travail.
2. Créez l'archive ZIP en préservant l'arborescence :
```bash
cd mon_extension
zip -r ../mon_extension.ahe manifest.json bin/ scripts/ tools/ docs/
```
3. Transférez le fichier `mon_extension.ahe` sur votre téléphone ou stockage partagé.
4. Dans HephaistOS :
   - Ouvrez le tiroir **Plus (+)** depuis le rail de navigation ou le bouton supérieur.
   - Cliquez sur **`+ IMPORTER UNE EXTENSION (.AHE)`**.
   - Sélectionnez votre fichier `.ahe`.
   - L'extension est extraite, les permissions `chmod +x` sont assignées automatiquement et l'interface Material 3 devient immédiatement accessible !

---

## 8. Template Modèle de Référence

Un template modèle officiel est généré automatiquement par HephaistOS dans :  
`/sdcard/HEPHAISTOS/Templates/ahe_template/`

Vous pouvez l'utiliser comme base de départ pour créer vos propres extensions en clonant simplement cette arborescence.

---

## 9. Interopérabilité Souveraine avec SIGNALAB & SCEAU

HephaistOS expose automatiquement les bases de données et les moteurs cryptographiques de **SIGNALAB** et **SCEAU** aux extensions `.ahe`.

### Variables d'Environnement Dédiées Injectées :
Lorsqu'un script d'extension est exécuté, les variables suivantes sont exportées dans l'environnement d'exécution :
- `HEPHAISTOS_SIGNALAB_SCAN_DIR` : `/sdcard/HEPHAISTOS/Data/Signalab/scan/` (accès aux arborescences `filesystem_tree.json`, blueprints et clusters).
- `HEPHAISTOS_SIGNALAB_LABOUT_DIR` : `/sdcard/HEPHAISTOS/SIGNER/LABOUT/` (destination des images recompilées et résignées).
- `HEPHAISTOS_SCEAU_KEYCORE_DIR` : `/sdcard/HEPHAISTOS/SCEAU/KEYCORE/` (trousseaux AOSP, clés Verisin et clones).
- `HEPHAISTOS_UNPACKED_DIR` : `/sdcard/HEPHAISTOS/UNPACKED/` (partitions et ROMs décompressées de haute précision).

### Exploitation des Données de Scan Signalab dans un Script AHE :
```bash
#!/system/bin/sh
# Récupération du dernier scan Signalab
LAST_SCAN=$(ls -td "$HEPHAISTOS_SIGNALAB_SCAN_DIR"/scan_* 2>/dev/null | head -n 1)

if [ -n "$LAST_SCAN" ] && [ -f "$LAST_SCAN/apk_signature_clusters.json" ]; then
    echo "[AHE IPC] Lecture des clusters cryptographiques depuis : $LAST_SCAN"
    CLUSTERS_COUNT=$(grep -c '"cluster_id"' "$LAST_SCAN/apk_signature_clusters.json")
    echo "[AHE IPC] $CLUSTERS_COUNT clusters de signature identifiés par Signalab."
fi
```

### Retour de Progression Dynamique dans l'Interface HephaistOS :
Pour animer dynamiquement la barre de progression de l'extension dans HephaistOS :
```bash
echo "[PROGRESS:25] Analyse des descripteurs d'inodes..."
echo "[STATUS:Vérification des signatures...]"
echo "[PROGRESS:75] Injection des patchs de permissions..."
echo "[PROGRESS:100] Terminé avec succès !"
```

---

## 10. Bonnes Pratiques pour le Référencement GitHub d'une Extension AHE

1. **Architecture Binaire ARM64 (`aarch64-linux-android`)** :
   - Tous les binaires exécutables dans `bin/` doivent être statiquement liés (`-static`) ou liés contre le runtime Bionic d'Android pour fonctionner sans dépendances externes.
2. **Gestion de SELinux & Permissions** :
   - Déclarez toujours `"permissions": ["STORAGE", "ROOT"]` si votre extension manipule des systèmes de fichiers ou monte des images en boucle locale (`loop`).
3. **Mise à Jour des Artefacts ART & fs-verity** :
   - Toute modification ou resignature d'APKs système doit systématiquement purger les fichiers `.odex`, `.vdex` et `.oat` correspondants pour permettre à Android d'exécuter un `dexopt` runtime propre sans déclencher de bootloop.
4. **Validation et Compatibilité** :
   - Assurez-vous que votre `manifest.json` respecte la casse exacte des identifiants et inclut une description concise sous 120 caractères.

