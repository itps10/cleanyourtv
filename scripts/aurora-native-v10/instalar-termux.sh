#!/data/data/com.termux/files/usr/bin/bash
# Aurora Native v1.0 — sem remover os launchers anteriores.
set -Eeuo pipefail
TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.nativev10"
OLD_GOOGLE="com.google.android.tvlauncher"
DOWNLOADS="$HOME/storage/downloads"
APK="${1:-$DOWNLOADS/aurora-native-v10.apk}"
WALL="$DOWNLOADS/aurora_montanhas_lago_sem_interface.jpg"
CONFIG="$DOWNLOADS/aurora-native-v10-config.json"
URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-native-logos-v10/themes/aurora-floating-philco/config.json"
mkdir -p "$DOWNLOADS"
command -v adb >/dev/null || { echo "Instale ADB: pkg install android-tools"; exit 1; }
command -v curl >/dev/null || { echo "Instale curl: pkg install curl"; exit 1; }
adb connect "$TV" >/dev/null || true
if [ "$(adb -s "$TV" get-state 2>/dev/null || true)" != "device" ]; then
    echo "TV nao autorizada no ADB: $TV"
    exit 1
fi
echo "TV: $(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"
adb -s "$TV" shell pm path "$OLD_GOOGLE" || echo "Aviso: Home Google nao identificado; nenhum pacote sera desativado."
if ! adb -s "$TV" shell pm path "$APP" 2>/dev/null | grep -q '^package:'; then
    if [ ! -s "$APK" ]; then
        echo "APK nao encontrado: $APK"
        echo "Baixe o artefato Aurora-Native-v10-APK e extraia app-release.apk."
        echo "Coloque na pasta Downloads com o nome aurora-native-v10.apk"
        exit 1
    fi
    echo "Instalando Aurora Native como novo aplicativo, sem trocar Home..."
    adb -s "$TV" install -r "$APK"
fi
echo "Aplicando configuracao de logomarcas e iluminacao..."
curl --fail --silent --show-error --location --retry 3 -o "$CONFIG" "$URL"
[ -s "$CONFIG" ] || { echo "Nao foi possivel baixar o config.json"; exit 1; }
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora-native-v10-config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.nativev10.theme/config.json < /data/local/tmp/aurora-native-v10-config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora-native-v10-config.json

if [ -s "$WALL" ]; then
    echo "Aplicando imagem de fundo das montanhas e lago..."
    adb -s "$TV" push "$WALL" /data/local/tmp/aurora-native-v10-wallpaper.jpg >/dev/null
    adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.nativev10.theme/wallpaper.jpg < /data/local/tmp/aurora-native-v10-wallpaper.jpg'
    adb -s "$TV" shell rm -f /data/local/tmp/aurora-native-v10-wallpaper.jpg
else
    echo "Wallpaper JPG ainda nao esta na pasta Downloads."
    echo "Baixe aurora_montanhas_lago_sem_interface.jpg pelo ChatGPT."
    echo "Depois rode este mesmo script para aplicar sem reinstalar o aplicativo."
fi
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"
echo "Teste os apps com o controle remoto. HOME padrao nao foi modificado."
echo "Depois de testar, se desejar:"
echo "adb -s $TV shell cmd package set-home-activity --user 0 $APP/com.gathin.tvhome.MainActivity"
