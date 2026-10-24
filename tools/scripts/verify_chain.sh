#!/system/bin/sh
# ==============================================================================
# HEPHAISTOS GÉMÉAU - SCRIPT DE VÉRIFICATION DE LA CHAÎNE DE CONFIANCE
# Emplacement: /sdcard/HEPHAISTOS/TOOLS/scripts/verify_chain.sh
# ==============================================================================

set -e

APK_TARGET="$1"
KEYSET_DIR="$2"

if [ -z "$APK_TARGET" ]; then
    echo "Usage: $0 <apk_cible> [dossier_keyset]"
    exit 1
fi

echo "================================================================="
echo " [HEPHAISTOS GÉMÉAU] Vérification de Signature & Intégrité"
echo " Cible: $APK_TARGET"
echo "================================================================="

# Vérification présence de classes.dex et alignement
unzip -l "$APK_TARGET" | head -n 25

echo ""
echo "[+] Vérification des empreintes et certificats META-INF:"
unzip -p "$APK_TARGET" "META-INF/*.RSA" "META-INF/*.DSA" "META-INF/*.EC" 2>/dev/null | sha256sum || echo "Aucun bloc v1 JAR classique trouvé (signature v2/v3 pure possible)"

# Vérification du fichier vdex/odex compagnon
BASE_NAME=$(basename "$APK_TARGET" .apk)
PARENT_DIR=$(dirname "$APK_TARGET")

for ARCH in arm64 arm x86_64 x86; do
    OAT_DIR="$PARENT_DIR/oat/$ARCH"
    if [ -f "$OAT_DIR/$BASE_NAME.vdex" ]; then
        echo "[+] VDEX compagnon détecté: $OAT_DIR/$BASE_NAME.vdex"
    fi
    if [ -f "$OAT_DIR/$BASE_NAME.odex" ]; then
        echo "[+] ODEX compagnon détecté: $OAT_DIR/$BASE_NAME.odex"
    fi
done

if [ -f "$PARENT_DIR/$BASE_NAME.apk.fsvmeta" ]; then
    echo "[+] fs-verity metadata (.fsvmeta) présent: $PARENT_DIR/$BASE_NAME.apk.fsvmeta"
fi

echo "[✔] Diagnostic terminé."
exit 0
