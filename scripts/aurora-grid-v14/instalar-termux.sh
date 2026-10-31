#!/data/data/com.termux/files/usr/bin/bash
# Aurora Grid v1.4: preenche os circulos com os icones nativos da TV.
# Pacote separado da v1.2, sem desinstalar nem trocar o HOME.
set -Eeuo pipefail

TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.grid"
OLD="com.gathin.tvhome.aurora.fullbleed"
DOWNLOADS="$HOME/storage/downloads"
APK="$DOWNLOADS/aurora-grid-v14.apk"
CONFIG="$DOWNLOADS/aurora-grid-v14-config.json"
WALL="$DOWNLOADS/aurora-grid-v14-wallpaper.jpg"
RELEASE_BASE="https://github.com/itps10/cleanyourtv/releases/download/aurora-orbit-grid-v14-final/"
CONFIG_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-grid-v14/themes/aurora-floating-philco/config.json"
MINIMAL_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-grid-v14/assets/wallpapers/orbit_minimal_sem_texto.jpg.gz.b64"

for cmd in adb curl base64 gzip; do
    command -v "$cmd" >/dev/null || { echo "Falta o comando $cmd"; exit 1; }
done
mkdir -p "$DOWNLOADS"
adb connect "$TV" >/dev/null || true
if [ "$(adb -s "$TV" get-state 2>/dev/null || true)" != "device" ]; then
    echo "TV nao autorizada via ADB em $TV"; exit 1
fi
echo "TV conectada: $(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"

if ! adb -s "$TV" shell pm path "$APP" 2>/dev/null | grep -q '^package:'; then
    if [ ! -s "$APK" ]; then
        echo "Baixando APK da release do GitHub..."
        curl --fail --location --retry 3 --connect-timeout 20 \
            --silent --show-error "${RELEASE_BASE}aurora-grid-v14.apk" -o "$APK"
    fi
    [ -s "$APK" ] || { echo "APK nao encontrado. Nada foi removido."; exit 1; }
    echo "Instalando Aurora FullBleed como aplicativo separado..."
    adb -s "$TV" install -r "$APK"
else
    echo "Aurora FullBleed ja esta instalada. Reaplicando tema..."
fi

curl --fail --location --retry 3 --silent --show-error "$CONFIG_URL" -o "$CONFIG"
[ -s "$CONFIG" ] && grep -q '"app_glow"' "$CONFIG" || {
    echo "Config.json nao recebido."; exit 1
}
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora_grid_v14_config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.grid.theme/config.json < /data/local/tmp/aurora_grid_v14_config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora_grid_v14_config.json

# Keep the user's CURRENT dark Octocat-inspired wallpaper from Orbit v1.2.
# If the old provider cannot expose it, use the compact GitHub wallpaper asset.
if [ ! -s "$WALL" ]; then
    if adb -s "$TV" shell pm path "$OLD" 2>/dev/null | grep -q '^package:'; then
        echo "Reaproveitando o papel de parede da Aurora FullBleed..."
        if ! adb -s "$TV" exec-out content read --uri \
            "content://${OLD}.theme/wallpaper.jpg" > "$WALL" 2>/dev/null; then
            rm -f "$WALL"
        elif [ "$(wc -c < "$WALL")" -lt 5000 ]; then
            rm -f "$WALL"
        fi
    fi
fi
if [ ! -s "$WALL" ]; then
    echo "Buscando wallpaper minimalista direto do repositorio..."
    ENCODED="$DOWNLOADS/orbit_minimal_sem_texto.jpg.gz.b64"
    if curl --fail --location --retry 3 --silent --show-error \
        "$MINIMAL_URL" -o "$ENCODED"; then
        if ! (base64 -d "$ENCODED" | gzip -dc > "$WALL"); then
            rm -f "$WALL"
        elif [ "$(wc -c < "$WALL")" -lt 5000 ]; then
            rm -f "$WALL"
        fi
    fi
fi
if [ -s "$WALL" ]; then
    echo "Aplicando o mesmo wallpaper escuro da versão anterior..."
    adb -s "$TV" push "$WALL" /data/local/tmp/aurora_grid_v14_wallpaper.jpg >/dev/null
    adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.grid.theme/wallpaper.jpg < /data/local/tmp/aurora_grid_v14_wallpaper.jpg'
    adb -s "$TV" shell rm -f /data/local/tmp/aurora_grid_v14_wallpaper.jpg
else
    echo "Wallpaper antigo nao encontrado; a paisagem integrada continua disponivel."
fi
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"

echo "Pronto. Teste: Home = apenas os 7 favoritos; BAIXO = grade vertical com 7 apps por linha; Voltar = Inicio."
echo "O HOME original e as versoes antigas do Aurora foram preservados."
echo "Para tornar a v1.3 a tela Home, apos testar:"
echo "adb shell cmd package set-home-activity --user 0 $APP/com.gathin.tvhome.MainActivity"
