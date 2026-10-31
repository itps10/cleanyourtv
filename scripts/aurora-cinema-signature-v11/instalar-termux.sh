#!/data/data/com.termux/files/usr/bin/bash
# Aurora Cinema Signature v1.1 — instalação segura no Termux.
# Não desinstala nem desativa o Google Home ou versões anteriores do Aurora.
set -Eeuo pipefail

TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.cinema"
DOWNLOADS="$HOME/storage/downloads"
APK="${1:-$DOWNLOADS/aurora-cinema-v11.apk}"
WALL="$DOWNLOADS/aurora_montanhas_lago_sem_interface.jpg"
CONFIG="$DOWNLOADS/aurora-cinema-v11-config.json"
CONFIG_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-cinema-signature-v11/themes/aurora-floating-philco/config.json"

command -v adb >/dev/null || { echo "ADB ausente. Execute: pkg install android-tools"; exit 1; }
command -v curl >/dev/null || { echo "curl ausente. Execute: pkg install curl"; exit 1; }
mkdir -p "$DOWNLOADS"
adb connect "$TV" >/dev/null || true
if [ "$(adb -s "$TV" get-state 2>/dev/null || true)" != "device" ]; then
    echo "TV não autorizada no ADB: $TV"; exit 1
fi
echo "TV: $(adb -s "$TV" shell getprop ro.product.model | tr -d '\r')"

if adb -s "$TV" shell pm path com.google.android.tvlauncher | grep -q 'package:'; then
  echo "Home original presente para recuperação."
else
  echo "AVISO: Home Google não detectado; o script NÃO vai alterar seu Home atual."
fi

if ! adb -s "$TV" shell pm path "$APP" | grep -q 'package:'; then
    if [ ! -s "$APK" ]; then
        echo "APK não encontrado: $APK"
        echo "Baixe o artefato Aurora-Cinema-v11-APK no GitHub."
        echo "Extraia app-release.apk e renomeie para aurora-cinema-v11.apk."
        exit 1
    fi
    echo "Instalando um pacote SEPARADO: $APP"
    adb -s "$TV" install -r "$APK"
else
    echo "Aurora Cinema já instalada. Reaplicando configuração e wallpaper."
fi

echo "Baixando configuração do seu repositório..."
curl --fail --location --retry 3 --silent --show-error "$CONFIG_URL" -o "$CONFIG"
[ -s "$CONFIG" ] && grep -q '"ambient_glow"' "$CONFIG" || {
    echo "Configuração inválida ou indisponível."; exit 1;
}
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora_cinema_v11_config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.cinema.theme/config.json < /data/local/tmp/aurora_cinema_v11_config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora_cinema_v11_config.json

# Reaproveita a foto da v1.0 se não houver uma cópia na pasta Downloads.
# Evita pedir novo download da imagem ao usuário.
if [ ! -s "$WALL" ]; then
    FROM_APP="com.gathin.tvhome.aurora.nativev10"
    if adb -s "$TV" shell pm path "$FROM_APP" 2>/dev/null | grep -q 'package:'; then
        echo "Procurando o wallpaper já instalado na v1.0..."
        if ! adb -s "$TV" exec-out content read --uri "content://$FROM_APP.theme/wallpaper.jpg" > "$WALL" 2>/dev/null; then
            rm -f "$WALL"
        elif [ "$(wc -c < "$WALL")" -lt 20000 ]; then
            rm -f "$WALL"
        fi
    fi
fi

if [ -s "$WALL" ]; then
    echo "Aplicando foto do lago e montanhas: $WALL"
    adb -s "$TV" push "$WALL" /data/local/tmp/aurora_cinema_v11_wallpaper.jpg >/dev/null
    adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.cinema.theme/wallpaper.jpg < /data/local/tmp/aurora_cinema_v11_wallpaper.jpg'
    adb -s "$TV" shell rm -f /data/local/tmp/aurora_cinema_v11_wallpaper.jpg
else
    echo "A foto não está em Downloads nem na v1.0."
    echo "Salve aurora_montanhas_lago_sem_interface.jpg em Downloads e execute este script novamente."
    echo "O launcher usará temporariamente a paisagem integrada."
fi

echo "Abrindo Aurora Cinema Signature para teste..."
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"

echo "Pronto. Home atual foi preservado; teste Netflix, Prime, Disney, Max e outros apps."
echo "Depois do teste, para definir este launcher como Home:"
echo "adb -s $TV shell cmd package set-home-activity --user 0 $APP/com.gathin.tvhome.MainActivity"
echo "Para obter diagnóstico de cards: adb -s $TV logcat -d -s AuroraArtwork:I | tail -40"
