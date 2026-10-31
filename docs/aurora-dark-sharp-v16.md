# Aurora Sharp v1.6 — PHILCO Android TV 11

Evolução segura do **Aurora Dark v1.5**, mantendo os sete favoritos na tela inicial e a biblioteca vertical com oito apps por linha.

## Correções

- **Ícones dark mais nítidos:** geração a **256×256 px** (antes 144×144), com filtragem bilinear e antialiasing. O símbolo personalizado de TV Aberta e Play Filmes também é renderizado em 256 px. A resolução intrínseca do ícone original fornecido por cada APK ainda limita o nível de detalhes recuperáveis.
- **Biblioteca: sem bordas cortadas:** círculos da grade de todos os apps diminuídos de aproximadamente 82dp para até 70dp; ampliação de 10% para 5,5%; mais espaçamento superior/inferior e padding na área rolável. A grade continua vertical, com 8 colunas.
- **TV Aberta duplicado:** ao listar atividades iniciáveis, entradas de nomes equivalentes a **TV Aberta** são exibidas apenas uma vez; a primeira atividade encontrada é mantida. Não há alteração, desinstalação ou desativação dos aplicativos subjacentes. O app *TV ao Vivo* com nome diferente é preservado.
- **Memória:** cache de 28 imagens de 256 px, limitando o uso de RAM ao navegar na grade.
- **Tudo preservado:** fundo escuro e sombras cromáticas, anel colorido somente no ícone focado, sete favoritos na Home, controles Entradas/Wi-Fi/Apps/Configurações, botão ↓ para todos os apps e Voltar para Home.

## Instalação sem ZIP

Com Termux no Chromebook e PHILCO autorizada por ADB:

```bash
pkg install curl android-tools coreutils gzip -y
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-sharp.sh \
 "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-dark-sharp-v16/scripts/aurora-sharp-v16/instalar-termux.sh"
bash instalar-sharp.sh
```

O script baixa o APK `aurora-sharp-v16.apk` da Release pública, instala `com.gathin.tvhome.aurora.sharp` **em paralelo**, aplica o tema e tenta recuperar o wallpaper anterior. **Não troca o aplicativo Home automaticamente.**

Apenas depois de conferir:

```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.sharp/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Para retornar à v1.5:

```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.dark/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Se a versão da TV Abrerta mantida não for a desejada, envie os nomes dos pacotes; pode-se priorizar uma atividade específica sem precisar desinstalar os dois apps.
