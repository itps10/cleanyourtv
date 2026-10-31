# Aurora FullBleed v1.3 — círculos totalmente preenchidos

Correção visual para PHILCO Android TV 11 no fork `itps10/cleanyourtv`.

## O que foi corrigido

O Aurora Orbit v1.2 desenhava um círculo cromático com um `ImageView` de tamanho menor no centro, usando `FIT_CENTER` e, em alguns casos, uma arte `Leanback banner` retangular.

A v1.3:

- **Sempre usa o ícone real instalado** para os cards circulares, com fallback do PackageManager.
- Redimensiona o `ImageView` para a área completa do card, sem `padding`.
- Usa `CENTER_CROP` e `clipToOutline` circular: os cantos da imagem são cortados pela própria borda circular.
- Desenha o anel neon **no foreground**, acima do ícone, e muda para a cor do aplicativo.
- Preserva foco 1,23x, animações, sombra cromática do wallpaper, relógio e menu superior.
- Preserva **↓ em um círculo = biblioteca com todos os apps iniciáveis**. OK abre; Voltar retorna ao círculo.

Ajustes são gráficos; não envolvem remover apps, root nem serviço de acessibilidade.

## Instalar pelo Termux, sem ZIP

```bash
pkg install curl android-tools coreutils gzip -y
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-fullbleed.sh \
  "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-fullbleed-v13/scripts/aurora-fullbleed-v13/instalar-termux.sh"
bash instalar-fullbleed.sh
```

O instalador baixa `aurora-fullbleed-v13.apk` da Release pública do GitHub, aplica `config.json`, copia o wallpaper escuro do Aurora Orbit v1.2 quando possível e só então abre a nova versão para teste. Os aplicativos anteriores e o Home atual são preservados.

Se a nova interface estiver funcionando, habilitar Home:

```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.fullbleed/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Reversão:

```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.orbit/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

## Detalhe

Um ícone que, no próprio APK, contém margens transparentes ou uma área gráfica reduzida pode continuar com espaço interno na imagem. O launcher corta o **quadrado externo** para um círculo sem fazer OCR, remoção de fundo ou reconstrução do logotipo. O próximo diagnóstico, se necessário, é identificar esses pacotes pelo ADB e criar artes próprias para eles.
