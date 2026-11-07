# Aurora Champions BT v2.0 — PHILCO Android TV 11

Esta versão adiciona um botão de **Bluetooth** na barra do Aurora, imediatamente entre Wi-Fi e Apps. Preserva o funcionamento do Aurora Champions Fix v1.9, incluindo o painel da Champions e os sete favoritos.

## Interface

Barra superior:
**Entradas → Wi-Fi → Bluetooth → Apps → Configurações**.

O botão Bluetooth é circular com ícone vetorial, suporta foco por controle remoto e o atalho OK. O menu suspenso oferece:

1. **Ligar/Desligar Bluetooth** conforme o estado. No Android 11 da PHILCO, usa `BluetoothAdapter.enable/disable` por ação explícita do usuário, com permissão legada `BLUETOOTH_ADMIN`.
2. **Até cinco dispositivos pareados**, em ordem alfabética, com nome real lido do Android.
3. **Buscar / gerenciar dispositivos**, que abre a tela Bluetooth de configurações da própria TV para buscar, emparelhar, conectar ou desconectar.

**Limitação da API:** selecionar um dispositivo pareado no popup **abre a página oficial de configurações**, pois o Android não oferece uma API pública universal a um launcher comum para forçar conexão/desconexão de acessórios A2DP/HID. Assim não são usados serviços ocultos, root ou comandos Bluetooth privilegiados.

No Android 12 ou superior, a primeira utilização pode solicitar a permissão `BLUETOOTH_CONNECT`. No Android 13+ a API desautoriza liga/desliga programático para aplicativos comuns com `targetSdk=34`, então o Aurora usa a interface oficial do sistema.

A aparência dos botões acompanha a barra atual. A indicação do Bluetooth fica menos opaca quando desligado e é atualizada quando o estado muda.

## Champions League

Ajuste de alinhamento do painel ao focar a Max:
- Equipes sempre **à esquerda**.
- Placar (ou data/hora) sempre **à direita**, em sua própria coluna.
- Separadores discretos entre partidas, cabeçalho, recuo lateral e largura responsiva.
- Fonte, horários, resultados e atualização da ESPN preservados da v1.9.

## Instalação via Termux e GitHub

```bash
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-champions-bt.sh \
  "https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-champions-bluetooth-v20/scripts/aurora-champions-v20/instalar-termux.sh"
bash instalar-champions-bt.sh
```

O APK `com.gathin.tvhome.aurora.championsbt` é instalado **em paralelo** e mantém o Home original selecionado. A versão v1.9 não é desinstalada; o wallpaper da versão v1.9 é reaproveitado quando possível.

Para alternar para a v2.0 **após testar**:

```bash
adb -s 192.168.1.70:5555 shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.championsbt/com.gathin.tvhome.MainActivity
adb -s 192.168.1.70:5555 shell input keyevent 3
```

Para retornar à v1.9:

```bash
adb -s 192.168.1.70:5555 shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.championsfix/com.gathin.tvhome.MainActivity
```

**Não reative nem desative o Google TV Launcher neste procedimento.** A instalação preserva a configuração de Home que já está funcionando na PHILCO.

### Diagnósticos

```bash
adb -s 192.168.1.70:5555 shell dumpsys bluetooth_manager | head -35
adb -s 192.168.1.70:5555 logcat -d -s AndroidRuntime:E AuroraUCL:D | tail -45
```
