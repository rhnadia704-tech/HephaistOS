# HephaistOS — Système & Lanceur Modulaire d'Ingénierie ROM Android

[![Standard HSAS](https://img.shields.io/badge/Standard-HSAS%20v1.0-orange.svg)](docs/HSAS_SPECIFICATION.md)
[![Android](https://img.shields.io/badge/Platform-Android%208.0%2B-blue.svg)](https://developer.android.com)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Licence](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSE)

**HephaistOS** est une application hôte Android de pointe conçue comme la **colonne vertébrale et le cerveau central d'ingénierie mobile**. Elle permet le déballage, la modification, la personnalisation bit-à-bit et le réassemblage d'images de partitions Android (ROMs constructeurs, GSI, `payload.bin`, `super.img`, `system.img`, EROFS, EXT4) directement sur l'appareil, sans nécessiter d'ordinateur hôte.

---

## 🌟 Écosystème des Applications Satellites (HSAS v1.0)

HephaistOS n'est pas un système fermé : il orchestre un écosystème dynamique d'**Applications Satellites**. Toute application satellite installée sur l'appareil est **détectée en temps réel** et s'intègre harmonieusement comme une extension naturelle d'HephaistOS.

### 📖 Spécification Canonique pour Développeurs & IA
Pour créer une nouvelle application satellite HephaistOS autonome ou via un agent d'IA (**Google AI Studio**), consultez notre guide de référence absolue :

👉 **[Consulter la Spécification Canonique HSAS v1.0 (Bible Développeur & IA)](docs/HSAS_SPECIFICATION.md)**

---

## 🚀 Capacités Clés d'HephaistOS

1. **Moteur UKA Intégré (Unpack / Repack)** :
   - Support d'images Sparse Android, EROFS haute performance, et EXT4.
   - Préservation stricte bit-à-bit des métadonnées (SELinux `file_contexts`, permissions Unix `fs_config`, tables de symlinks et superblocks).
   - Suivi en direct du déballage avec affichage continu des mégaoctets et auto-défilement terminal.
   - Compatibilité immédiate avec les chargeurs DSU (Dynamic System Update).

2. **Détection Temps Réel des Satellites** :
   - Découverte instantanée à l'installation (`ACTION_PACKAGE_ADDED`) et nettoyage automatique à la désinstallation (`ACTION_PACKAGE_REMOVED`).
   - Aucune intervention manuelle requise de l'utilisateur.

3. **Interopérabilité & IPC Standard (AIDL)** :
   - Service d'hôte sécurisé `IHephaistHostService` permettant aux satellites d'invoquer les outils d'HephaistOS, d'accéder au workspace commun `/sdcard/HEPHAISTOS/`, et d'écrire dans la console terminal partagée.
   - Annuaire partagé des outils et capacités découverts sur l'appareil.

4. **Design Unifié & Épuré (Material Design 3)** :
   - Palette dynamique sombre / claire adaptée au thème de l'hôte.
   - Rail de navigation latéral fluide avec animations douces et typographie technique soignée.

---

## 📁 Organisation du Workspace Partagé (`/sdcard/HEPHAISTOS/`)

Tous les satellites partagent la même arborescence standardisée sur le stockage de l'appareil :

```text
/sdcard/HEPHAISTOS/
├── UNPACKED/       # Partitions et ROMs décompressées prêtes à l'édition
├── PACKED/         # Images finales reconstruites et prêtes au flash
├── Data/unpack/    # Métadonnées critiques sauvegardées (SELinux, permissions, symlinks)
├── Payloads/       # Images sources volumineuses (payload.bin, OTA zips)
├── Projects/       # Fiches d'analyses de projets
├── Scripts/        # Scripts exécutables satellites (.sh / .py)
├── WebApps/        # Applications web satellites locales (HTML5 / JS)
└── Templates/      # Spécifications officielles et Starter Kits
```

---

## 🛠️ Contribuer & Développer un Satellite

Vous souhaitez développer votre propre satellite ?
Téléchargez le **Starter Kit Officiel** depuis l'application HephaistOS (`Paramètres > Modèles > Télécharger le Starter Kit (ZIP)`) ou suivez le tutoriel pas-à-pas dans [docs/HSAS_SPECIFICATION.md](docs/HSAS_SPECIFICATION.md).
