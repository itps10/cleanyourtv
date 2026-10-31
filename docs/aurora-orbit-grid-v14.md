# Aurora Orbit Grid v1.4 — PHILCO Android TV 11

A tela inicial permanece minimalista, com **somente sete aplicativos favoritos**, sem uma fileira horizontal infinita. A configuração preserva os mesmos sete `dock_apps` já escolhidos: Netflix, Prime Video, Disney+, Max, YouTube, YouTube Music e Spotify.

## Controle remoto

- **←/→**: navega pelos sete círculos do Home.
- **↓**: exibe uma **nova tela de biblioteca completa**, com sete aplicativos por linha e rolagem vertical.
- **↑/↓ dentro da biblioteca**: move por linhas de sete; a lista rola de verdade quando há mais itens.
- **OK**: abre o aplicativo selecionado.
- **Voltar**: fecha a biblioteca e retorna aos sete favoritos.
- **↑ na primeira linha**: também retorna à tela inicial.
- **Home**: sempre restaura a tela com sete favoritos.
- **Apps** (botão do menu superior): abre a mesma biblioteca vertical.

A biblioteca mostra apps com atividade iniciável via `LEANBACK_LAUNCHER` ou `LAUNCHER`. Os sistemas internos sem tela de abertura não são incluídos. Não é necessário root nem desligar aplicativos.

## Remoção dos muitos ícones “Início” na interface

As variantes anteriores do Aurora têm identificadores sob `com.gathin.tvhome`. **Continuam instaladas** como opção de reversão, mas deixam de ser exibidas como apps na grade da v1.4. O launcher Google também não ocupa um círculo da biblioteca. O seletor de HOME do próprio Android poderá continuar mostrando launchers instalados — isso é normal e não deve ser tratado com desinstalação automática.

## Interface preservada

- Ícones oficiais preenchendo os círculos, sem retângulo interno.
- Zoom do favorito selecionado, anel neon e sombreamento de fundo conforme a cor do streaming.
- Wallpaper escuro minimalista atual, reaproveitado da v1.3 quando disponível.
- Relógio sem segundos.
- Barra superior: Entradas → Wi-Fi → Apps → Configurações.

## Instalar direto pelo GitHub / Termux

```bash
pkg install curl android-tools coreutils gzip -y
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-aurora-grid.sh \
 "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-grid-v14/scripts/aurora-grid-v14/instalar-termux.sh"
bash instalar-aurora-grid.sh
```

O instalador busca `aurora-grid-v14.apk` diretamente da Release, com pacote independente `com.gathin.tvhome.aurora.grid`, aplica o tema e tenta reaproveitar o wallpaper da v1.3. **Não apaga as outras versões nem muda o Home padrão**.

Somente depois de testar, para definir como Home:

```bash
adb shell cmd package set-home-activity --user 0 \
 com.gathin.tvhome.aurora.grid/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Retornar à v1.3:

```bash
adb shell cmd package set-home-activity --user 0 \
 com.gathin.tvhome.aurora.fullbleed/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```
