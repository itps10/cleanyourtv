# Aurora Orbit v1.2 — cartões circulares

Projeto: PHILCO Android TV 11, desempenho leve para ~1.3 GB RAM.

- Os favoritos são círculos reais: card e contorno circular, sem retângulos.
- Ícone oficial instalado no Android TV ocupa ~84% do círculo; banners oficiais são exibidos sem corte de texto.
- O item focado cresce 23% com animação curta e borda neon na cor da plataforma.
- Fundo panorâmico de montanhas e lago continua, e seu sombreamento muda com o app selecionado.
- Setas esquerda/direita: navegar; **↓ em qualquer círculo: biblioteca de aplicativos**; OK na biblioteca abre o app; Back volta ao mesmo círculo.
- Seta ↑ leva ao menu superior: Entradas, Wi-Fi, Apps, Configurações.
- Relógio sem segundos. YouTube Music mantém o painel contextual.
- Sem vídeos nem blur em tempo real; shaders estáticos por card para TV com pouca RAM.

## Instalação sem ZIP nem download do ChatGPT

No Termux, após disponibilização da Release:
```bash
pkg install curl android-tools -y
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-orbit.sh \
  "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-orbit-v12/scripts/aurora-orbit-v12/instalar-termux.sh"
bash instalar-orbit.sh
```

O APK (`com.gathin.tvhome.aurora.orbit`) e o wallpaper são obtidos automaticamente da Release pública. Nenhum launcher anterior é removido, e o Home padrão não é alterado durante a instalação.

Após testar:
```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.orbit/com.gathin.tvhome.MainActivity
adb shell input keyevent 3
```

Para voltar ao Aurora Cinema v1.1:
```bash
adb shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.cinema/com.gathin.tvhome.MainActivity
```

A biblioteca lista apps que oferecem atividade iniciável para TV/Android e que não estão excluídos pelo catálogo; não promete abrir serviços internos do sistema sem interface.
