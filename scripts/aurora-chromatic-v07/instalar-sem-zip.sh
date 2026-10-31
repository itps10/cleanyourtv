#!/usr/bin/env bash
# Aurora Chromatic v0.7 — instalador para Termux, sem ZIP externo.
# Nunca remove pacotes nem troca a tela HOME padrão.
set -Eeuo pipefail
TV="${TV:-192.168.1.70:5555}"
PKG="com.gathin.tvhome.aurora.chromatic"
OLD="com.gathin.tvhome.aurora"
OUT="$HOME/storage/downloads"
APK="${1:-$OUT/aurora-chromatic-v07.apk}"
SOURCE_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-chromatic-v07/themes/aurora-floating-philco/config.json"
mkdir -p "$OUT"
command -v adb >/dev/null || { echo 'Falta adb: pkg install android-tools'; exit 1; }
command -v curl >/dev/null || { echo 'Falta curl: pkg install curl'; exit 1; }
adb connect "$TV" >/dev/null || true
[ "$(adb -s "$TV" get-state 2>/dev/null || true)" = "device" ] || {
  echo "A TV nao esta autorizada no ADB: $TV"; exit 1;
}
MODEL="$(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"
echo "TV encontrada: $MODEL"
if ! adb -s "$TV" shell pm path "$PKG" | grep -q 'package:'; then
  [ -s "$APK" ] || { echo "APK nao encontrado: $APK"; echo "Baixe o APK v0.7 pelo GitHub Actions e informe o caminho como argumento."; exit 1; }
  echo "Instalando Aurora Chromatic separadamente (sem remover o anterior)..."
  adb -s "$TV" install -r "$APK"
  adb -s "$TV" shell pm path "$PKG" | grep -q 'package:' || { echo "Pacote nao encontrado apos instalacao"; exit 1; }
else
  echo "Aurora Chromatic ja esta instalada; somente o tema sera atualizado."
fi
CONFIG="$OUT/aurora-v07-config.json"
curl --fail --location --silent --show-error --retry 3 "$SOURCE_URL" -o "$CONFIG"
[ -s "$CONFIG" ] || { echo 'Falha ao baixar config.json do seu fork'; exit 1; }
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora-v07-config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.chromatic.theme/config.json < /data/local/tmp/aurora-v07-config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora-v07-config.json

# Reaproveita a foto de fundo da variante v0.6, se presente. Nenhum download de imagem necessario.
WALL="$OUT/aurora-v07-wallpaper.jpg"
if adb -s "$TV" shell pm path "$OLD" 2>/dev/null | grep -q 'package:'; then
  if adb -s "$TV" exec-out content read --uri "content://$OLD.theme/wallpaper.jpg" > "$WALL" 2>/dev/null && [ "$(wc -c < "$WALL")" -gt 3000 ]; then
    echo 'Copiando papel de parede da sua versao anterior...'
    adb -s "$TV" push "$WALL" /data/local/tmp/aurora-v07-wallpaper.jpg >/dev/null
    adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.chromatic.theme/wallpaper.jpg < /data/local/tmp/aurora-v07-wallpaper.jpg'
    adb -s "$TV" shell rm -f /data/local/tmp/aurora-v07-wallpaper.jpg
  else
    echo 'Wallpaper antigo nao encontrado; o tema de cores dinamicas ainda sera aplicado.'
    rm -f "$WALL"
  fi
fi
adb -s "$TV" shell am force-stop "$PKG"
adb -s "$TV" shell am start -W -n "$PKG/com.gathin.tvhome.MainActivity"
echo 'Aurora Chromatic aberta para teste. Seu launcher anterior e o Google Home foram preservados.'
