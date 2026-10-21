# Spécification Canonique et Guide d'Architecture des Applications Satellites HephaistOS (HSAS v1.0)

Ce plan d'implémentation structure l'écosystème modulaire d'HephaistOS pour garantir qu'aucune application satellite ne soit ignorée, que son interface soit immédiatement harmonisée avec l'hôte (thème sombre/clair et ADN visuel HephaistOS), et que les outils soient partagés et orchestrés de manière fluide via un protocole d'interopérabilité standardisé (AIDL & Services Android).

---

## 1. Diagnostic de l'Existant & Causes Racine

1. **Échec de Détection des Satellites Actuels** :
   - `AddonRegistry.scanInstalledAddons()` n'interrogeait que `queryIntentActivities` avec le flag `0`, omettant les applications qui exposent uniquement des services, ou qui n'ont pas la catégorie `DEFAULT`, ou restreintes par le filtre de visibilité de paquets sur Android 11+ (API 30+).
   - L'absence d'écouteur d'événements système (`BroadcastReceiver` pour `ACTION_PACKAGE_ADDED`, `ACTION_PACKAGE_REMOVED`, `ACTION_PACKAGE_REPLACED`) empêchait la mise à jour automatique de la liste lors de l'installation ou désinstallation d'un satellite sur le téléphone.
2. **Divergence Visuelle de l'Interface** :
   - Les anciens templates ne fournissaient pas les définitions de tokens visuels Jetpack Compose de HephaistOS (schéma de couleurs sombre/clair, formes, typographie technique, barres d'outils, structure de cartes). Un développeur ou une IA créait donc une interface générique non assortie.
3. **Absence d'Interopérabilité Fonctionnelle Réelle** :
   - Les modèles précédents proposaient un simple broadcast sans contrat d'interface formalisé. Les satellites ne pouvaient pas exécuter les commandes d'ingénierie de HephaistOS (UkaEngine, workspace, accès root délégué) ni offrir leurs propres fonctions à l'hôte.
4. **Documentation GitHub & Starter Kit Incomplets** :
   - Le dépôt ne comportait pas de `README.md` racine détaillant la spécification HSAS ni de bible canonique permettant à une IA (comme AI Studio sur un autre projet) de générer un satellite parfait en un seul prompt.

---

## 2. Architecture & Composants Cibles (HSAS v1.0)

### Phase 1 : Détection Automatique et Temps Réel des Satellites
- **`PackageReceiver.kt`** : Création d'un récepteur d'événements système (`BroadcastReceiver`) écoutant `Intent.ACTION_PACKAGE_ADDED`, `Intent.ACTION_PACKAGE_REMOVED`, `Intent.ACTION_PACKAGE_REPLACED` avec le schéma de données `package`. Déclaré dans `AndroidManifest.xml` et enregistré dynamiquement dans l'activité pour un rafraîchissement réactif instantané du `HephaistViewModel`.
- **Évolution de `AddonRegistry.kt`** :
  - Détection multi-vecteurs : scanne les `Activity` et les `Service` déclarés avec l'action `org.hephaistos.action.SATELLITE` ou `org.hephaistos.action.ADDON`.
  - Prise en charge des métadonnées de manifeste : `org.hephaistos.satellite.V1`, `org.hephaistos.addon.CAPABILITIES`, `org.hephaistos.satellite.ROLE`.
  - Détection de l'état de synchronisation en direct (connecté, installé, version, capacités déclarées).

### Phase 2 : Protocole d'Interopérabilité Hôte-Satellites (AIDL & Service Dédié)
- **Contrat AIDL Standardisé (`IHephaistHostService.aidl`)** :
  - `executeHostTool(String toolId, String jsonArgs)` : permet à un satellite d'invoquer les moteurs d'HephaistOS (déballage d'images, analyse payload, outils root, lecture des dossiers).
  - `getWorkspacePaths()` : fournit les chemins certifiés (`/sdcard/HEPHAISTOS/UNPACKED/`, `PACKED/`, etc.).
  - `logToHostTerminal(String tag, String message)` : permet au satellite d'écrire directement dans la console terminal d'HephaistOS.
  - `getRegisteredSatellites()` : permet la découverte mutuelle entre satellites.
- **Service Hôte `HephaistHostService.kt`** :
  - Service bound exporté avec action `org.hephaistos.service.HOST_IPC`.
  - Gestion sécurisée des connexions satellites, dispatch des commandes d'ingénierie, enregistrement des outils partagés dans `AddonRegistry`.

### Phase 3 : Refonte Complète des Templates dans Paramètres (`SettingsScreen.kt`)
- Remplacement des anciens aperçus par une interface pédagogique structurée en 4 volets interactifs :
  1. **Déclaration Canonique (Manifest & ADN)** : XML prêt pour `AndroidManifest.xml`, intent-filters, meta-data des capacités et permissions.
  2. **Charte Visuelle & Thème Jetpack Compose** : Code source complet des couleurs, formes et composants de base pour que le satellite arbore rigoureusement le même style (mode sombre/clair automatique, cartes profilées, polices monospace techniques).
  3. **Code Client AIDL & Communication** : Exemple Kotlin clé en main pour se lier à `HephaistHostService` et exécuter une tâche en quelques lignes de code.
  4. **Prompt Clé-en-Main pour IA & AI Studio** : Un bloc de prompt optimisé copiable en un clic, à coller dans AI Studio pour créer immédiatement un satellite autonome et conforme sans ambiguïté.

### Phase 4 : Bible Canonique HSAS v1.0 & Déploiement GitHub / Starter Kit
- **`README.md` à la racine** : Présentation du projet HephaistOS avec badge et lien d'accès direct vers le guide canonique HSAS.
- **`docs/HSAS_SPECIFICATION.md`** : La bible complète d'architecture contenant le dictionnaire des capacités standards, la spécification d'interopérabilité IPC, les contrats d'interface, et les règles d'ergonomie visuelle.
- **Mise à jour de `PdfExporterService.kt` & Starter Kit ZIP** :
  - Génération d'un kit de démarrage `.zip` enrichi contenant :
    - `README.md` complet et téléchargeable.
    - Code source Compose d'un satellite complet (`MainActivity.kt`, `HephaistTheme.kt`, `IHephaistHostService.aidl`, `SatelliteService.kt`).
    - `AndroidManifest.xml` pré-configuré.
    - Guide prompt pour agents d'IA.
  - Document PDF officiel HSAS v1.0 actualisé avec les schémas AIDL et la charte d'interopérabilité.

---

## 3. Fichiers Concernés & Modifications

| Fichier | Modification |
|---|---|
| `app/src/main/AndroidManifest.xml` | Déclaration des actions de requêtes `<queries>`, permission de lecture de paquets, et déclaration du récepteur de paquets et de `HephaistHostService`. |
| `app/src/main/aidl/org/hephaistos/ipc/IHephaistHostService.aidl` | Définition de l'interface AIDL d'interopérabilité hôte-satellites. |
| `app/src/main/java/com/example/data/HephaistHostService.kt` | Implémentation du service hôte Android pour le partage des outils et l'exécution distante. |
| `app/src/main/java/com/example/data/AddonRegistry.kt` | Enrichissement du moteur de découverte automatique (Activités + Services + Meta-data) et gestion du registre des outils. |
| `app/src/main/java/com/example/data/PackageReceiver.kt` | BroadcastReceiver écoutant l'installation et la désinstallation de packages en temps réel. |
| `app/src/main/java/com/example/ui/HephaistViewModel.kt` | Enregistrement du récepteur de paquets et déclenchement réactif de `refreshCompatibleApps()`. |
| `app/src/main/java/com/example/ui/screens/SettingsScreen.kt` | Refonte intégrale de l'onglet Modèles avec la documentation HSAS v1.0, les blocs de code Compose, AIDL et le prompt IA. |
| `app/src/main/java/com/example/data/PdfExporterService.kt` | Génération du nouveau ZIP Starter Kit complet avec l'arborescence projet Android et la documentation HSAS v1.0. |
| `README.md` | Page d'accueil GitHub avec lien vers la spécification HSAS. |
| `docs/HSAS_SPECIFICATION.md` | Bible architecturale canonique complète et exhaustive pour développeurs et intelligences artificielles. |

---

## 4. Vérification & Validation

1. **Compilation Android** : Exécution de `compile_applet` pour valider l'absence d'erreurs de syntaxe, la conformité AIDL / Service et la cohérence de l'arbre Compose.
2. **Cycle de Vie Paquet** : Vérification de la détection dynamique d'un nouveau satellite sans redémarrage de l'application hôte.
3. **Génération & Export** : Contrôle de la création du Starter Kit ZIP et de la présence des fichiers `README.md`, `HSAS_SPECIFICATION.md` et modèles Kotlin/Compose.
4. **Fidélité Visuelle & UX** : Validation de la cohérence de l'interface dans l'onglet Paramètres avec le thème et le rail de navigation de l'hôte.
