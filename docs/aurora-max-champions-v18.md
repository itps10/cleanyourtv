# Aurora Champions v1.8 — painel contextual da Champions League

Parte do fork `itps10/cleanyourtv`, derivado do Aurora Compact v1.7 para PHILCO Android 11.

## Comportamento

Ao mover o foco do controle remoto para a Max/HBO (`com.wbd.stream`, `com.hbo.hbonow`, `com.hbo.hbonow.tv`, e variantes reconhecidas), o Aurora exibe um painel à direita da tela, em cima do wallpaper:

- **AO VIVO:** placares, equipes e tempo/status fornecidos pelo feed;
- **ÚLTIMOS RESULTADOS:** jogos encerrados do período consultado;
- **PRÓXIMOS JOGOS:** horários convertidos automaticamente para o fuso horário da TV;
- **Atualizado HH:mm:** mostra quando a última consulta foi recebida.

A fonte de resultados é a API pública de placares da ESPN (`https://site.api.espn.com/apis/site/v2/sports/soccer/uefa.champions/scoreboard`). **É um endpoint não oficial/documentado por terceiros**, sujeito a mudanças, indisponibilidade e atrasos; portanto não é garantido atualização segundo a segundo nem integração com a Max. Nenhuma conta, login, stream ou dado privado da Max é acessado.

## Atualização e memória

- Somente enquanto o launcher estiver em primeiro plano **e** o ícone da Max selecionado.
- Uma requisição HTTPS de placares para o período relevante (17 dias anteriores, 9 seguintes).
- Reconsulta **a cada 75 segundos** durante jogos marcados como ao vivo; fora dos jogos, a cada 5 minutos, enquanto houver foco.
- Os resultados são processados em uma thread dedicada e renderizados somente na thread de interface; timeout de 6/7 segundos, resposta limitada a 2 MB.
- Ao trocar de aplicativo, abrir outro app ou desligar o Home, o painel e as consultas agendadas são interrompidos.
- Sem acesso permanente, serviço em segundo plano, notificações ou polling quando você não está no card Max.
- Mensagem clara quando o feed não responde; **não há resultados fabricados**.

Preservados: sete favoritos, círculos menores, visual Dark, biblioteca vertical de oito ícones por linha, botão ↓, wallpaper escuro e Home original como alternativa de recuperação.

## Instalar com o Termux via GitHub

```bash
cd ~/storage/downloads
curl -fL --retry 3 -o instalar-champions.sh \
"https://raw.githubusercontent.com/itps10/cleanyourtv/aurora-max-champions-v18/scripts/aurora-champions-v18/instalar-termux.sh"
bash instalar-champions.sh
```

O script baixa `aurora-champions-v18.apk` da Release, instala o pacote **separado** `com.gathin.tvhome.aurora.champions`, aplica o mesmo tema e reaproveita o wallpaper da v1.7 quando possível. Não altera o Home escolhido nem desativa outros launchers.

Depois de validar o painel, para definir como Home:

```bash
TV=192.168.1.70:5555
adb -s "$TV" shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.champions/com.gathin.tvhome.MainActivity
adb -s "$TV" shell input keyevent 3
```

Se preferir recuperar o Aurora Compact:

```bash
adb -s 192.168.1.70:5555 shell cmd package set-home-activity --user 0 \
  com.gathin.tvhome.aurora.compact/com.gathin.tvhome.MainActivity
adb -s 192.168.1.70:5555 shell input keyevent 3
```

Não habilite ou desabilite o Google TV Launcher sem necessidade. No aparelho configurado anteriormente, a desativação do Google Launcher já foi realizada para o Home customizado, e esta versão não altera isso.
