#!/data/data/com.termux/files/usr/bin/bash
# Aurora Orbit v1.2 — instalação segura no Termux.
# Não desinstala nem desativa o Google Home ou versões anteriores do Aurora.
set -Eeuo pipefail

TV="${TV:-192.168.1.70:5555}"
APP="com.gathin.tvhome.aurora.orbit"
DOWNLOADS="$HOME/storage/downloads"
APK="${1:-$DOWNLOADS/aurora-orbit-v12.apk}"
WALL="$DOWNLOADS/aurora_montanhas_lago_sem_interface.jpg"
CONFIG="$DOWNLOADS/aurora-orbit-v12-config.json"
CONFIG_URL="https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-v12/themes/aurora-floating-philco/config.json"
RELEASE_BASE="https://github.com/itps10/cleanyourtv/releases/download/aurora-orbit-v12/"

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
        echo "Baixando APK direto da Release no GitHub..."
        curl --fail --location --retry 3 --connect-timeout 20 \
            --silent --show-error \
            -o "$APK" "${RELEASE_BASE}aurora-orbit-v12.apk"
        if [ ! -s "$APK" ]; then
            echo "Não foi possível baixar o APK da Release."
            echo "Acompanhe: https://github.com/itps10/cleanyourtv/releases/tag/aurora-cinema-signature-v11"
            exit 1
        fi
    fi
    echo "Instalando um pacote SEPARADO: $APP"
    adb -s "$TV" install -r "$APK"
else
    echo "Aurora Orbit já instalada. Reaplicando configuração e wallpaper."
fi

echo "Baixando configuração do seu repositório..."
curl --fail --location --retry 3 --silent --show-error "$CONFIG_URL" -o "$CONFIG"
[ -s "$CONFIG" ] && grep -q '"ambient_glow"' "$CONFIG" || {
    echo "Configuração inválida ou indisponível."; exit 1;
}
adb -s "$TV" push "$CONFIG" /data/local/tmp/aurora_orbit_v12_config.json >/dev/null
adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.orbit.theme/config.json < /data/local/tmp/aurora_orbit_v12_config.json'
adb -s "$TV" shell rm -f /data/local/tmp/aurora_orbit_v12_config.json

# A imagem de fundo está na própria Release e será obtida se não existir localmente.
# Mantenha a cópia local caso o usuário tenha escolhido outra foto com esse nome.
# A Release pública também disponibiliza um wallpaper estático, caso
# a versão anterior não tenha a foto original. Não bloquear a instalação por isso.
if [ ! -s "$WALL" ]; then
    echo "Baixando wallpaper panorâmico diretamente do GitHub..."
    if ! curl --fail --location --retry 3 --connect-timeout 20 \
        --silent --show-error -o "$WALL" \
        "${RELEASE_BASE}aurora_montanhas_lago_sem_interface.jpg"; then
        rm -f "$WALL"
        echo "Wallpaper indisponível; a paisagem integrada será utilizada."
    fi
fi

if [ -s "$WALL" ]; then
    echo "Aplicando foto do lago e montanhas: $WALL"
    adb -s "$TV" push "$WALL" /data/local/tmp/aurora_orbit_v12_wallpaper.jpg >/dev/null
    adb -s "$TV" shell 'content write --uri content://com.gathin.tvhome.aurora.orbit.theme/wallpaper.jpg < /data/local/tmp/aurora_orbit_v12_wallpaper.jpg'
    adb -s "$TV" shell rm -f /data/local/tmp/aurora_orbit_v12_wallpaper.jpg
else
    echo "A foto não está em Downloads nem na v1.0."
    echo "A versão padrão inclui uma paisagem integrada, sem precisar copiar imagens."
    echo "O launcher usará temporariamente a paisagem integrada."
fi

echo "Abrindo Aurora Orbit Signature para teste..."
adb -s "$TV" shell am force-stop "$APP"
adb -s "$TV" shell am start -W -n "$APP/com.gathin.tvhome.MainActivity"

echo "Pronto. Home atual preservado. Esquerda/Direita navega nos circulos; BAIXO abre todos os apps; Voltar retorna."
echo "Depois do teste, para definir este launcher como Home:"
echo "adb -s $TV shell cmd package set-home-activity --user 0 $APP/com.gathin.tvhome.MainActivity"
echo "Para obter diagnóstico de cards: adb -s $TV logcat -d -s AuroraArtwork:I | tail -40"
