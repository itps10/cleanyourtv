#!/data/data/com.termux/files/usr/bin/bash
# Aurora Neon v0.9: instala outro pacote para manter o Luxe/Chromatic intactos.
set -Eeuo pipefail
TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.neon"
DOWNLOADS="$HOME/storage/downloads"
APK="${1:-$DOWNLOADS/aurora-neon-v09.apk}"
URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-neon-cards-v09/themes/aurora-floating-philco/config.json"
mkdir -p "$DOWNLOADS"
command -v adb >/dev/null || { echo "Instale o ADB: pkg install android-tools"; exit 1; }
command -v curl >/dev/null || { echo "Instale curl: pkg install curl"; exit 1; }
adb connect "$TV" >/dev/null || true
[ "$(adb -s "$TV" get-state 2>/dev/null || true)" = "device" ] || {
  echo "TV $TV nao autorizada no ADB."; exit 1;
}
echo "TV conectada: $(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"
echo "Conferindo launcher Google (nao sera desabilitado)..."
adb -s "$TV" shell pm path com.google.android.tvlauncher
if ! adb -s "$TV" shell pm path "$APP" 2>/dev/null | grep -q '^package:'; then
  [ -s "$APK" ] || {
    echo "APK nao encontrado em: $APK"
    echo "Baixe o artefato Aurora-Neon-v09-APK e extraia app-release.apk."
    echo "Renomeie-o para aurora-neon-v09.apk na pasta Downloads."
    exit 1;
  }
  echo "Instalando launcher Neon em paralelo, sem trocar o HOME..."
  adb -s "$TV" install -r "$APK"
fi
echo "Baixando tema leve direto do GitHub..."
curl --fail --location --silent --show-error --retry 3 \
  "$URL" -o "$DOWNLOADS/aurora-neon-v09-config.json"
[ -s "$DOWNLOADS/aurora-neon-v09-config.json" ] || {
  echo "Config nao baixada."; exit 1;
}
adb -s "$TV" push "$DOWNLOADS/aurora-neon-v09-config.json" \
  /data/local/tmp/aurora-neon-v09-config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.neon.theme/config.json < /data/local/tmp/aurora-neon-v09-config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora-neon-v09-config.json
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"
echo "Aurora Neon aberta para testar; Home atual foi preservado."
echo "Para torna-la Home depois de testar:"
echo "adb -s $TV shell cmd package set-home-activity --user 0 $APP/com.gathin.tvhome.MainActivity"
