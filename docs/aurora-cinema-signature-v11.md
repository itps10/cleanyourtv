# Aurora Cinema Signature v1.1 — PHILCO Android TV 11

Nova variante independente de `com.gathin.tvhome.aurora.cinema`.

## Problema corrigido

Na v1.0 a rotina usava `loadLogo()` para cada app e o `ImageView` continuava preso ao formato legado do grid: sete cards faziam a imagem ficar pequena e em alguns aparelhos ela parecia ausente. Nesta versão:

- O Android fornece o **banner próprio do aplicativo** (quando existe e tem tamanho válido).
- Quando não há banner, usa o **ícone oficial `ResolveInfo.loadIcon()`**, com fallback para `getApplicationIcon()`.
- Ícones são posicionados no centro e têm 58% da largura e 84% da altura do card, sem caption sobreposta.
- Banners são encaixados usando `FIT_CENTER`, sem `CENTER_CROP` que cortava logos.
- Mensagens de diagnóstico `AuroraArtwork` mostram se cada aplicativo possui banner/ícone.
- O tema mantém sete favoritos, wallpaper de montanhas/lago (JPG real), brilho cromático e menu **Entradas / Wi-Fi / Apps / Configurações**.

Nenhum código de leitura de dados privados dos streaming apps é incorporado. A seção "Continuar assistindo" do mockup é ilustrativa, não uma integração real.

## Instalação

1. No GitHub Actions, baixe `Aurora-Cinema-v11-APK`, extraia `app-release.apk` e renomeie `aurora-cinema-v11.apk`.
2. Mova para Downloads no Termux do Chromebook.
3. Baixe e execute:

```bash
cd ~/storage/downloads
curl -fL --retry 3 \
  -o instalar-cinema.sh \
  "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-cinema-signature-v11/scripts/aurora-cinema-signature-v11/instalar-termux.sh"
bash instalar-cinema.sh "$HOME/storage/downloads/aurora-cinema-v11.apk"
```

A foto de wallpaper será usada se estiver em `Downloads/aurora_montanhas_lago_sem_interface.jpg`. Caso contrário, o script tentará recuperá-la do Aurora Native v1.0 instalado anteriormente.

**Segurança**: não desinstala os launchers anteriores, não modifica o Home padrão e não desativa pacotes do sistema.

Após testar no controle remoto:

```bash
adb shell cmd package set-home-activity --user 0 com.gathin.tvhome.aurora.cinema/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Para voltar ao Google Home:

```bash
adb shell cmd package set-home-activity --user 0 com.google.android.tvlauncher/.MainActivity
```

## Se uma logo continuar vazia

```bash
adb -s 192.168.1.70:5555 logcat -d -s AuroraArtwork:I | tail -40
```

Mande o resultado. As linhas identificam qual app não forneceu banner/ícone; não exibem dados pessoais.
