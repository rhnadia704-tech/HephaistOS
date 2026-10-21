package org.hephaistos.ipc;

interface IHephaistSatelliteService {
    /**
     * Retourne le nom officiel du module satellite.
     */
    String getSatelliteName();

    /**
     * Retourne la liste des capacités JSON exposées par ce satellite.
     */
    String getSatelliteCapabilitiesJson();

    /**
     * Exécute un outil spécifique fourni par ce satellite.
     * @param toolId Identifiant de l'outil exposé
     * @param jsonParams Paramètres d'exécution
     * @return Résultat structuré JSON
     */
    String executeSatelliteTool(String toolId, String jsonParams);
}
