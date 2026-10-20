# Progression Temps Réel Unpack & Rail de Navigation Moderne

Plan d'implémentation pour résoudre le blocage perçu à 25% lors du montage/déballage d'images volumineuses (GSI/ROM) dans le moteur UKA, introduire un suivi continu des Mo transférés et du nombre de fichiers avec défilement automatique du terminal, et refondre l'animation et l'ergonomie du rail de navigation latéral pour une expérience fluide, attrayante et professionnelle.

## Examen Utilisateur & Décisions Validées

> [!IMPORTANT]
> Suite à vos retours sur le succès du repack (qui a démarré avec succès sur DSU Sideloader !) et vos réponses aux clarifications :
> 1. **Progression Continue & Terminal en Direct (Choix 1 & 3 validés) :**
>    - **Cause racine du blocage à 25% :** La copie précédente exécutait une commande silencieuse `cp -a` sur les dossiers racines du système (notamment `/system` contenant plus de 3 Go et des dizaines de milliers de fichiers). Durant les 2 à 3 minutes nécessaires au transfert physique sur la mémoire flash, aucun log n'était émis et la jauge restait figée à 25% avant de basculer d'un coup à 100%.
>    - **Résolution :** Pipeline de streaming continu avec `tar -v` et sonde de débit disque en tâche de fond. Progression continue calculée en Mo réels décompressés (`X / Y Mo`), décompte précis des fichiers déballés, pulsation active d'activité (« heartbeat ») empêchant tout sentiment de gel, et défilement automatique du terminal pour suivre l'extraction ligne par ligne.
>    - **Sauvegarde des Blocs Intègres :** Remplacement de l'appel bloquant unique par un flux tamponné de 4 Mo avec progression graduelle de 88% à 96%, affichant le volume en mégaoctets en temps réel.
> 2. **Refonte du Rail de Navigation Latéral (Choix 1 validé) :**
>    - **Analyse des captures d'écran :** Les lettres des libellés (« Accueil », « Paramètres ») se retrouvaient compressées verticalement les unes sous les autres parce que le conteneur interne ne s'étendait pas de manière synchronisée avec la largeur animée, et que le texte n'était pas contraint sur une seule ligne (`maxLines = 1, softWrap = false`).
>    - **Résolution :** Transition fluide basée sur une animation ressort (`spring`), conteneur adaptatif avec libellés horizontaux sans coupure, puce active lumineuse (pill badge M3), espacement respirant et ombre douce pour un rendu premium.

---

## 1. Vue d'Ensemble & Concept Clé

* **Élimination Totale des Gels d'Interface :** Pendant la copie de partitions de 3 Go ou plus, l'utilisateur bénéficie d'une visibilité absolue : barre de progression animée qui avance de façon régulière (25% -> 30% -> 45% -> 60% -> 75% -> 90%), affichage du dossier ou fichier actuellement écrit, compteur de mégaoctets et pulsations d'activité toutes les secondes.
* **Console Terminal Réactive avec Auto-Scroll :** Le composant terminal intégré et le mode console plein écran défilent automatiquement vers la ligne la plus récente pour refléter le déballage en direct.
* **Navigation Latérale Haut de Gamme :** Remplacement du rail étriqué par un rail ergonomique à deux états (compact 72dp avec icônes centrées / étendu 230dp avec libellés horizontaux nets, sous-titres et icônes alignées), animé par une physique de ressort naturelle.

---

## 2. Expérience Utilisateur & Design Visuel

### A. Nouveau Rail de Navigation Latéral
* **État Compact (72 dp) :** 
  - Icônes Material Symbols centrées avec puces douces au survol ou à la sélection.
  - Logo « H » identitaire HephaistOS élégamment encapsulé dans un conteneur arrondi.
  - Bouton d'action rapide `(+)` avec contraste distinct.
* **État Étendu (230 dp) :**
  - Expansion horizontale fluide synchronisée (`animateDpAsState` avec amortissement `Spring.DampingRatioMediumBouncy`).
  - Libellés textuels horizontaux impeccables (`Accueil`, `Unpack & Repack`, `Paramètres`, `Ajouter un module (+)`) avec `maxLines = 1` et fondu entrant doux (`fadeIn + expandHorizontally`).
  - Ombre subtile (`tonalElevation = 2.dp` + bordure fine translucide) isolant élégamment le rail du contenu principal.

### B. Progression Visuelle Pendant l'Unpack
* **Indicateur de Progression Animé :** Progression continue adoucie par `animateFloatAsState`, évitant tout saut brusque.
* **Compteur Dynamique en Sous-Titre :** 
  - Exemple : `Déballage partition : 1 420 / 3 350 Mo (3 812 fichiers déballés)`
* **Fenêtre Terminal Embarquée :**
  - Défilement automatique au fur et à mesure que les fichiers sont découverts et extraits.
  - Coloration syntaxique claire : vert pour les réussites et volumes, bleu pour les fichiers en cours, orange pour les étapes de montage loop.

---

## 3. Décisions Clés de Conception & Arbitrages

```
┌────────────────────────────────────────────────────────────────────────┐
│                        ANCIENNE vs NOUVELLE APPROCHE                   │
├────────────────────────────────────────────────────────────────────────┤
│  ANCIENNE :                                                            │
│   cp -a /mnt/system /dest/  ──────>  Silence de 3 minutes              │
│                                       (Jauge bloquée à 25%)            │
│                                                                        │
│  NOUVELLE (Moteur UKA Stream) :                                        │
│   ├── (cd /mnt && tar -cf - .) | (cd /dest && tar -vxf -)              │
│   │    └── Émission en direct de chaque fichier extrait                │
│   ├── Sonde disque périodique (du -sm)                                 │
│   │    └── Envoi régulier des Mo écrits (ex: 850 Mo, 1 420 Mo)        │
│   └── Heartbeat Coroutine Kotlin                                       │
│        └── Pulsation active si une opération d'E/S dépasse 1 seconde   │
│        └── Rendu fluide : 25% ──> 38% ──> 52% ──> 74% ──> 90% ──> 100% │
└────────────────────────────────────────────────────────────────────────┘
```

* **Pourquoi le streaming `tar` plutôt que `cp -a` ?**
  - `tar -cf - . | tar -vxf -` préserve à 100% la structure UNIX (symlinks, permissions SELinux, horodatages) tout en écrivant le nom de chaque fichier traité dans le canal de sortie.
  - Le streaming séquentiel réduit la fragmentation et s'avère jusqu'à 2 à 3 fois plus rapide que les copies récursives standard sur le stockage flash Android.
* **Pourquoi une sonde disque en tâche de fond ?**
  - Même si un très gros fichier binaire unique (comme un fichier `system.img` ou un conteneur Apex de 800 Mo) met quelques secondes à s'écrire, la sonde disque rapporte en continu les Mo progressant sur le stockage.

---

## 4. Architecture Technique

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    ARCHITECTURE DU SUIVI TEMPS RÉEL                     │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  [ Noyau Android / Loop Device ]                                        │
│         │                                                               │
│         ▼                                                               │
│  [ Script Shell Privilégié UKA ]                                        │
│         ├── Flux Tar / Verbose Extraction (EXTR_ITEM:<path>)            │
│         └── Sonde Disque en arrière-plan (DISK_MB:<megabytes>)          │
│         │                                                               │
│         ▼                                                               │
│  [ RootDelegator & UkaEngine ]                                          │
│         ├── Filtrage & régulation (Throttling 200ms pour fluidité UI)   │
│         ├── Calcul de ratio (Mo extraits / Mo attendus)                 │
│         └── Pulsation Heartbeat Kotlin (garantie zéro gel)              │
│         │                                                               │
│         ▼                                                               │
│  [ HephaistViewModel -> UnpackUiState ]                                 │
│         ├── progress (animateFloatAsState)                              │
│         ├── currentStep (Libellé dynamique avec Mo & compteurs)         │
│         └── recentLogs (Historique avec auto-scroll)                    │
│         │                                                               │
│         ▼                                                               │
│  [ UnpackRepackScreen & ExecutionLogCard ]                              │
│         └── Affichage temps réel interactif et rassurant                │
└─────────────────────────────────────────────────────────────────────────┘
```

### Plan des Modifications de Fichiers

1. **`app/src/main/java/com/example/data/UkaEngine.kt` :**
   - Refactoriser `mountAndExtractLoopWithShortPath` avec le pipeline streaming `tar` et la sonde de taille disque en tâche de fond.
   - Intégrer la coroutine heartbeat pour garantir l'animation constante de la barre et du texte d'étape.
   - Réguler les événements pour ne pas surcharger la boucle Compose tout en garantissant une réactivité immédiate.
   - Ajouter la copie tamponnée par blocs de 4 Mo avec suivi en temps réel pour l'image brute de partition.
   - Améliorer le repack EXT4 en supportant `mke2fs` et `e2fsdroid` avant le repli sur blocs.

2. **`app/src/main/java/com/example/ui/components/HephaistNavRail.kt` :**
   - Corriger le dimensionnement interne et la synchronisation de l'animation de largeur.
   - Remplacer le saut de texte par un `expandHorizontally() + fadeIn()` avec `maxLines = 1, softWrap = false` pour éliminer le texte vertical tronqué.
   - Améliorer la cinématique ressort (`Spring.DampingRatioMediumBouncy`, `Spring.StiffnessMediumLow`) et le design des boutons (puces actives, alignement soigné, ombre portée douce).

3. **`app/src/main/java/com/example/ui/screens/UnpackRepackScreen.kt` :**
   - Ajouter l'auto-scroll automatique sur la liste des logs dans `ExecutionLogCard` à chaque nouvel élément reçu.
   - Adoucir la barre de progression linéaire avec `animateFloatAsState`.
   - Afficher un badge pulsant d'activité pendant l'extraction.
