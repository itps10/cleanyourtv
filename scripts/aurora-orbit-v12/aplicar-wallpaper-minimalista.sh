#!/data/data/com.termux/files/usr/bin/bash
# Aurora Orbit: aplica o wallpaper da imagem enviada, sem a palavra GitHub.
# Atualiza apenas o wallpaper. Nao reinstala, remove ou redefine Home.
set -Eeuo pipefail
TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.orbit"
DIR="$HOME/storage/downloads"
ENCODED="$DIR/orbit_minimal_sem_texto.jpg.gz.b64"
JPG="$DIR/orbit_minimal_sem_texto.jpg"
SRC="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-v12/assets/wallpapers/orbit_minimal_sem_texto.jpg.gz.b64"

command -v adb >/dev/null || { echo "Instale ADB: pkg install android-tools"; exit 1; }
command -v curl >/dev/null || { echo "Instale curl: pkg install curl"; exit 1; }
command -v base64 >/dev/null || { echo "Instale coreutils: pkg install coreutils"; exit 1; }
command -v gzip >/dev/null || { echo "Instale gzip: pkg install gzip"; exit 1; }
mkdir -p "$DIR"
echo "Obtendo wallpaper diretamente do GitHub..."
curl --fail --location --retry 3 --silent --show-error "$SRC" -o "$ENCODED"
base64 -d "$ENCODED" | gzip -dc > "$JPG"
if [ "$(wc -c < "$JPG")" -lt 5000 ]; then
  echo "Arquivo de wallpaper pequeno demais; nenhuma alteracao aplicada."; exit 1
fi

adb connect "$TV" >/dev/null || true
[ "$(adb -s "$TV" get-state 2>/dev/null || true)" = device ] || {
  echo "TV nao conectada/autorizada em $TV"; exit 1;
}
if ! adb -s "$TV" shell pm path "$APP" | grep -q '^package:'; then
  echo "Aurora Orbit v1.2 nao esta instalada na TV. Nada foi alterado."
  exit 1
fi

BACKUP="$DIR/orbit_wallpaper_anterior_$(date +%Y%m%d-%H%M%S).jpg"
if adb -s "$TV" exec-out content read \
  --uri "content://${APP}.theme/wallpaper.jpg" > "$BACKUP" 2>/dev/null; then
  [ "$(wc -c < "$BACKUP")" -gt 5000 ] || rm -f "$BACKUP"
else
  rm -f "$BACKUP"
fi

echo "Aplicando wallpaper sem texto..."
adb -s "$TV" push "$JPG" /data/local/tmp/orbit_wallpaper_minimal.jpg >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.orbit.theme/wallpaper.jpg < /data/local/tmp/orbit_wallpaper_minimal.jpg'
adb -s "$TV" shell rm -f /data/local/tmp/orbit_wallpaper_minimal.jpg
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"
echo "Wallpaper atualizado. O Home padrao e seus aplicativos foram preservados."
echo "A imagem local esta em $JPG; o arquivo anterior foi salvo, se disponivel."
