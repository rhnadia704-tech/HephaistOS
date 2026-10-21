package org.hephaistos.ipc;

interface IHephaistHostService {
    /**
     * Exécute un outil d'ingénierie ou une commande via le moteur central d'HephaistOS.
     * @param toolId Identifiant de l'outil (ex: "kitchen.unpack", "root.exec", "workspace.stat")
     * @param jsonParams Paramètres d'exécution encodés en JSON
     * @return Résultat structuré encodé en JSON
     */
    String executeHostTool(String toolId, String jsonParams);

    /**
     * Retourne le chemin absolu du workspace certifié (/sdcard/HEPHAISTOS/).
     */
    String getWorkspaceRootPath();

    /**
     * Écrit directement une ligne de log dans le terminal temps réel de l'application hôte.
     * @param senderTag Nom ou ID du satellite émetteur
     * @param message Contenu du message
     */
    void logToHostTerminal(String senderTag, String message);

    /**
     * Retourne l'annuaire des outils et capacités partagées par tous les satellites installés.
     */
    String getEcosystemRegistryJson();

    /**
     * Vérifie si le root est accordé à l'hôte HephaistOS.
     */
    boolean isHostRootAvailable();
}
