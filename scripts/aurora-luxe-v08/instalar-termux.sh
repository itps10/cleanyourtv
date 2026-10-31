#!/usr/bin/env bash
# Aurora Luxe v0.8 - instalacao independente via Chromebook Termux.
# Nao desinstala nada, nao muda HOME padrao e nao precisa do ZIP desta conversa.
set -Eeuo pipefail
TV="${TV:-192.168.1.70:5555}"
PKG="com.gathin.tvhome.aurora.luxe"
DOWNLOADS="$HOME/storage/downloads"
APK="${1:-$DOWNLOADS/aurora-luxe-v08.apk}"
CONFIG="$DOWNLOADS/aurora-luxe-v08-config.json"
CONFIG_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-luxe-v08/themes/aurora-floating-philco/config.json"

command -v adb >/dev/null || { echo 'Instale o ADB: pkg install android-tools'; exit 1; }
command -v curl >/dev/null || { echo 'Instale o curl: pkg install curl'; exit 1; }
mkdir -p "$DOWNLOADS"
adb connect "$TV" >/dev/null || true
[ "$(adb -s "$TV" get-state 2>/dev/null || true)" = "device" ] || {
  echo "Falha de conexao com a TV em $TV. Verifique a autorizacao ADB."
  exit 1
}
echo "TV: $(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"
if ! adb -s "$TV" shell pm path "$PKG" 2>/dev/null | grep -q 'package:'; then
  [ -s "$APK" ] || {
    echo "APK nao encontrado: $APK"
    echo 'Baixe Aurora-Luxe-v08-APK no GitHub Actions e renomeie para aurora-luxe-v08.apk.'
    exit 1
  }
  echo "Instalando em paralelo; a interface atual nao sera removida..."
  adb -s "$TV" install -r "$APK"
fi
curl --fail --location --silent --show-error --retry 3 "$CONFIG_URL" -o "$CONFIG"
[ -s "$CONFIG" ] || { echo 'O arquivo config.json nao foi baixado.'; exit 1; }
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora-luxe-v08-config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.luxe.theme/config.json < /data/local/tmp/aurora-luxe-v08-config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora-luxe-v08-config.json
adb -s "$TV" shell am force-stop "$PKG"
adb -s "$TV" shell am start -W -n "$PKG/com.gathin.tvhome.MainActivity"
echo 'Aurora Luxe aberta em modo de teste. Home Google e Auroras anteriores permanecem disponiveis.'
