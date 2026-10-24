#!/system/bin/sh
# ==============================================================================
# HEPHAISTOS GÉMÉAU - SCRIPT D'AUDIT CRYPTOGRAPHIQUE PROFOND DE ROM
# Emplacement: /sdcard/HEPHAISTOS/TOOLS/scripts/gemeau_deep_audit.sh
# ==============================================================================

set -e

ROM_TARGET="$1"
OUTPUT_DIR="${2:-/sdcard/HEPHAISTOS/DATA/GEMEAU}"
TOOLS_BIN_DIR="/sdcard/HEPHAISTOS/TOOLS/bin"
APP_BIN_DIR="/data/user/0/com.aistudio.romresigner.vtwxz/files/tools/bin"

echo "================================================================="
echo " [HEPHAISTOS GÉMÉAU] Lancement de l'audit approfondi de la ROM"
echo " Cible  : ${ROM_TARGET}"
echo " Sortie : ${OUTPUT_DIR}"
echo "================================================================="

if [ -z "$ROM_TARGET" ]; then
    echo "[!] Erreur: Veuillez spécifier le chemin vers la ROM ou partition décompressée."
    echo "    Usage: $0 <chemin_rom> [dossier_sortie]"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

# Détection de l'exécutable natif
DISSECTOR_BIN=""
if [ -x "$APP_BIN_DIR/gemeau-dissector" ]; then
    DISSECTOR_BIN="$APP_BIN_DIR/gemeau-dissector"
elif [ -x "$TOOLS_BIN_DIR/gemeau-dissector" ]; then
    DISSECTOR_BIN="$TOOLS_BIN_DIR/gemeau-dissector"
fi

if [ -n "$DISSECTOR_BIN" ]; then
    echo "[+] Exécution du dissecteur natif Rust: $DISSECTOR_BIN"
    "$DISSECTOR_BIN" --rom-path "$ROM_TARGET" --output-dir "$OUTPUT_DIR"
else
    echo "[*] Dissecteur natif absent, analyse standard avec outils système..."
    APKS_FOUND=$(find "$ROM_TARGET" -name "*.apk" 2>/dev/null | wc -l)
    echo "    APKs détectés : $APKS_FOUND"
    
    # Rapport sommaire
    cat <<EOF > "$OUTPUT_DIR/audit_quick_summary.json"
{
  "engine": "HephaistOS GÉMÉAU Shell Probe",
  "rom_target": "$ROM_TARGET",
  "total_apks": $APKS_FOUND,
  "status": "ready_for_resigning"
}
EOF
fi

echo "[✔] Audit terminé. Rapports sauvegardés dans: $OUTPUT_DIR"
exit 0
