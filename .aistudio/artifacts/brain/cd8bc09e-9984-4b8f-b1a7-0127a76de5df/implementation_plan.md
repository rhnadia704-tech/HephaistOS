# Filtrage Strict Satellites HephaistOS & Extraction Réelle Unpack

Plan d'implémentation pour éliminer les applications génériques du téléphone de l'accueil en n'affichant que les satellites dotés de l'ADN HephaistOS, enrichir les directives et modèles de développement dans les Paramètres, et refondre le moteur Unpack pour procéder à une extraction réelle et complète des fichiers d'images ROM.

## Examen Utilisateur & Décisions Critiques

> [!IMPORTANT]
> Décisions confirmées suite à vos réponses aux questions de cadrage :
> 1. **Filtrage Strict de l'Accueil :** Aucune application généraliste du téléphone (Gmail, Téléphone, Drive, etc.) ne sera affichée sur l'accueil. Seules les applications déclarant explicitement le protocole Satellite HephaistOS (`org.hephaistos.action.ADDON` ou métadonnées `org.hephaistos.addon.*`) apparaîtront.
> 2. **Enrichissement des Directives et Templates :** La section Paramètres > Modèles / Satellites sera considérablement renforcée avec des spécifications architecturales complètes, des exemples de code Kotlin/Manifest prêts à l'emploi et des règles précises pour concevoir des satellites conformes.
> 3. **Moteur Unpack Réel :** Remplacement de la génération de structures factices par une extraction réelle intégrale des fichiers (décompression sparse via `simg2img`, extraction des archives, copie et désassemblage réel des fichiers de partitions dans `/storage/emulated/0/HEPHAISTOS/UNPACKED/<nom_cible>/`).

---

## 1. Vue d'Ensemble & Concept Clé

* **Filtrage par ADN HephaistOS :** HephaistOS agit comme un environnement d'accueil et un système hôte modulaire. Son accueil doit uniquement refléter son écosystème propriétaire : les modules satellites déclarés et connectés via l'action d'intention standardisée `org.hephaistos.action.ADDON`. Si aucun satellite n'est installé sur l'appareil, l'accueil reste net, centré uniquement sur le module **Unpack Repack**.
* **Directives & Spécifications Renforcées :** Fournir dans l'onglet Paramètres une documentation exhaustive et téléchargeable expliquant aux développeurs comment donner « l'ADN HephaistOS » à leur APK satellite (filtres d'intent, permissions partagées, bridge d'interaction hôte/satellite, descripteurs de capacités).
* **Extraction Réelle UKA Unpack :** Lorsqu'une image réelle est sélectionnée (ex: `Infinity312.img` de 3.35 Go ou images GSI/EXT4/EROFS/ZIP), le moteur UKA doit extraire l'intégralité du contenu réel (fichiers système, configurations, propriétés, binaires) soit directement via les utilitaires de système de fichiers / commandes privilégiées root si disponibles, soit via un désassemblage bloc par bloc fidèle, sans jamais substituer le contenu par des dossiers vides.

---

## 2. Expérience Utilisateur & Design Visuel

### Flux Utilisateur
1. **Accueil HephaistOS :**
   - Présentation de la carte principale épurée **Unpack Repack** en tête.
   - En dessous : affichage exclusif des applications possédant le protocole HephaistOS (`Satellite HephaistOS`). Les 101 applications système ou tierces non-satellites du téléphone sont exclues.
   - Si aucun satellite n'est présent : l'accueil est épuré, sans liste encombrante d'applications non désirées.
2. **Paramètres > Modèles de Satellites :**
   - Directives claires et interactives détaillant « L'ADN Obligatoire d'un Satellite HephaistOS ».
   - Code prêt à copier pour `AndroidManifest.xml`, `BroadcastReceiver` et classes clientes Kotlin.
   - Téléchargement du guide enrichi PDF et du Starter Kit ZIP complet.
3. **Module Unpack :**
   - Sélection du fichier cible dans `/sdcard/HEPHAISTOS/TARGET/` ou via le sélecteur.
   - Déroulement de l'analyse et extraction réelle avec suivi pas à pas en console Terminal.
   - Les fichiers réels de l'image sont créés et inspectables dans `/sdcard/HEPHAISTOS/UNPACKED/<nom_cible>/`.

---

## 3. Décisions Clés de Conception & Arbitrages

* **Arbitrage Filtrage Applicatif :**
  - *Choix retenu :* Détection exclusive par `queryIntentActivities` sur `org.hephaistos.action.ADDON` et vérification des métadonnées de manifeste. Suppression définitive du scan large de tous les launchers non-système (`FLAG_SYSTEM == 0`).
  - *Pourquoi :* L'utilisateur souhaite un OS modulaire dédié et non un lanceur d'applications génériques Android.
* **Arbitrage Extraction Réelle Unpack :**
  - *Choix retenu :* Double pipeline d'extraction :
    1. Si l'accès root est disponible : exécution des binaires d'extraction (`simg2img`, montage loop en lecture temporaire pour copier l'arborescence réelle des fichiers, ou extraction par utilitaire archive).
    2. En mode standard / non-root : extraction directe des archives ZIP/Payload et décodage réel des blocs d'images plutôt que la création de stubs factices.
  - *Pourquoi :* Les images de plusieurs gigaoctets contenaient des données réelles qui n'étaient pas extraites dans le dossier de travail.

---

## 4. Architecture Technique & Flux de Données

```
┌─────────────────────────────────────────────────────────────┐
│                       HEPHAISTOS HÔTE                       │
├─────────────────────────────────────────────────────────────┤
│  [Accueil]                                                  │
│   ├── UnpackRepackQuickCard (Carte maîtresse)               │
│   └── SatellitesDétectés (Filtre strict: ACTION_ADDON)      │
│                                                             │
│  [Paramètres - Templates & ADN]                             │
│   ├── Spécifications AndroidManifest (Filtres, Métadonnées) │
│   ├── Contrats d'interfaces Kotlin (Broadcast / IPC)        │
│   └── Kit d'Intégration PDF / Zip téléchargeable            │
│                                                             │
│  [Moteur UKA Unpack Réel]                                   │
│   ├── Analyse Cible (Magic, Sparse, Super, EXT4, EROFS, Zip)│
│   ├── Désassemblage Réel (simg2img / loop / stream extract) │
│   └── Écriture Arborescence Réelle dans /UNPACKED/<target>/ │
└─────────────────────────────────────────────────────────────┘
```

### Plan d'Exécution Fichier par Fichier
1. **`app/src/main/java/com/example/data/AddonRegistry.kt` :**
   - Restreindre `scanInstalledAddons` strictement aux applications répondant à `ACTION_HEPHAISTOS_ADDON`.
   - Supprimer le scan 2 qui ajoutait toutes les applications du lanceur Android du téléphone.
2. **`app/src/main/java/com/example/ui/screens/HomeScreen.kt` :**
   - Mettre à jour l'intitulé de la section : « Satellites HephaistOS connectés ».
   - N'afficher la section que si des satellites possédant l'ADN HephaistOS sont installés.
3. **`app/src/main/java/com/example/data/UkaEngine.kt` :**
   - Remplacer `createSimulatedPartitionContent` par un moteur d'extraction réelle des fichiers pour les images de ROM réelles.
   - Pour les archives ZIP et Payload : extraire les vrais fichiers et partitions.
   - Pour les images EXT4/EROFS : prise en charge de l'extraction réelle des structures de répertoires et fichiers via commandes privilégiées root ou extraction séquentielle de données, afin que le dossier décompressé contienne les vrais fichiers de la ROM.
4. **`app/src/main/java/com/example/ui/screens/SettingsScreen.kt` & `PdfExporterService.kt` :**
   - Mettre à jour et enrichir la documentation de « L'ADN Satellite HephaistOS » dans les modèles de paramètres pour détailler les impératifs d'enregistrement des APK satellites.
