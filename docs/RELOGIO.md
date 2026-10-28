# GymTracker no relógio (Wear OS) — versão 2.0 (+ extras de 28/10/2026)

## O que o app do relógio faz
- **Início:** os seus grupos de treino (vêm do celular), "Continuar treino", "Treino rápido", fila de envio, "Sincronizar" e "Ajustes".
- **Treino** (3 telas, deslize para os lados):
  - **meio – série:** nome do exercício, "série 2 de 4", repetições × carga já preenchidas com a última vez (ou a série anterior), dica "tente 42,5" quando você fez todas as repetições da última vez. Toque em *reps* ou *kg* para escolher o que a **coroa/bezel** muda; botões − / + também. **REGISTRAR** vibra, e o descanso começa sozinho.
  - **meio – descanso:** anel com contagem, −15 / +15, "PULAR", batimentos, a próxima série. Vibra de leve faltando 10 s e forte no fim — **mesmo com a tela apagada ou o app fechado** (alarme exato do sistema). Bater recorde mostra "RECORDE!" com vibração própria.
  - **esquerda – exercícios:** progresso de cada um (toque para ir a outro), "Desfazer última série", "Terminar treino", "Descartar".
  - **direita – números:** tempo, séries, volume, batimentos agora/máximo, recordes.
- **Ao terminar:** resumo (tempo, séries, volume, batimento médio/máximo, recordes) e o treino vai para o celular, que grava no Supabase: aparece no Histórico, conta nos recordes, no feed e nos desafios. Se o celular estiver longe, o treino fica **na fila** e vai sozinho quando ele aparecer.
- **Tela sempre ligada (modo ambiente):** ao baixar o pulso, o app continua na tela em preto e branco ("Descanso até 14:32").
- **Atalho no mostrador:** durante o treino aparece o ícone do halter com o cronômetro; tocar volta ao app.
- **Ajustes:** 6 cores, descanso padrão (45 s a 4 min), passo da carga (1 / 2,5 / 5 kg), descanso automático, batimentos.
- **Extras de 28/10/2026:**
  - **Semana na tela inicial:** anel "2/4 treinos na semana" (meta do Perfil) e "N dias seguidos"; o grupo **sugerido** (rotação, igual ao "Sugerido" do celular) vem primeiro, marcado "Sugerido hoje".
  - **Tile** (deslize a partir do mostrador e adicione "GymTracker"): semana, sequência, "Hoje: Peito" e botão **Treinar**, que já abre o treino desse grupo. Com treino aberto, mostra o treino e "Continuar".
  - **Complicação** "Treinos na semana" para o mostrador (toque e segure o mostrador → Editar → Complicações): anel ou texto "2/4"; tocar abre o app.
  - **Anilhas:** na tela da série, com a carga selecionada, toque em "anilhas" (aparece a partir de 20 kg): cada lado da barra desenhado com as cores olímpicas; toque na barra para trocar 20/15/10 kg.
  - **Melhor série** no resumo final (1RM estimado, fórmula de Epley).
  - **Adicionar exercício** fora do grupo durante o treino (lista de exercícios → "Adicionar exercício").
  - **Últimos treinos** (5, vindos do celular) na tela inicial; quando o celular grava um treino do relógio, mostra a notificação "Treino do relógio salvo".
  - **Intervalos** (tela inicial → "Intervalos"): timer de HIIT/Tabata/EMOM. Prontos (Tabata 20/10 ×8, HIIT 40/20 e 30/30, EMOM 10 min, Prancha 45/15) e "Meu timer" (esforço, pausa, rodadas e preparo com − / +, fica salvo). Anel vermelho no esforço e azul na pausa, vibra na troca (longa = vai, duas curtas = pausa), conta 3-2-1 com toques antes do esforço, mostra batimentos, pausar/seguir/parar. A tela fica acesa enquanto o timer roda; **com o app fechado o timer não vibra** (diferente do descanso, que usa alarme).
  - O celular atualiza o relógio sozinho ao abrir o app (com relógio conectado, no máximo a cada 15 min); no **Perfil → Relógio** há "Sincronizar com o relógio".

## Como funciona a sincronização
O relógio não fala com o Supabase. Ele conversa com o app do celular pela Data Layer do Wear OS:
1. Ao abrir (ou em "Sincronizar"), o relógio pede as rotinas; o `WearSyncService` do celular (acorda sozinho, mesmo com o app fechado) monta grupos + exercícios + última carga + recorde e publica.
2. Ao terminar, o relógio publica o treino; o celular grava (`ImportWatchWorkout`: cria o treino com o id do relógio, grava as séries, fecha com a hora do relógio) e apaga o item — é assim que o relógio sabe que foi salvo. Entregar duas vezes não duplica.

**Regra importante:** celular e relógio só conversam se os dois apps tiverem o mesmo pacote **e a mesma assinatura**. Os APKs do GitHub Actions são assinados com uma chave nova a cada execução, então **instale os dois APKs da mesma execução**. Para não precisar disso, configure o secret `DEBUG_KEYSTORE_BASE64` (ver o fim deste arquivo).

## Instalar (PC com adb)
**Atalho:** o script `tools\instalar_apks.ps1` faz os passos 2 a 5 sozinho (acha os APKs em Downloads, instala no celular pelo cabo, pareia/conecta o relógio pelo Wi-Fi e instala nele; se a assinatura for diferente, pergunta antes de desinstalar). Rodar: `powershell -ExecutionPolicy Bypass -File tools\instalar_apks.ps1`.

Precisa: o PC com o adb (`%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`), o celular no cabo USB e o relógio no **mesmo Wi-Fi** do PC.

### 1. Baixar os dois APKs da mesma execução
GitHub → aba **Actions** → a execução verde mais recente do branch → **Artifacts**: `gymtracker-debug-apk` (celular) e `gymtracker-wear-apk` (relógio). Descompactar os dois zips.

### 2. Celular
```
adb install -r app-debug.apk
```
Se aparecer `INSTALL_FAILED_UPDATE_INCOMPATIBLE` (o app instalado foi assinado com outra chave), desinstale antes com `adb uninstall com.vitoroliveira.gymtracker` e instale de novo. Isso apaga o que só existe no aparelho (medidas corporais, agenda, tema); treinos, grupos e peso estão na nuvem. Pelo adb o Play Protect não bloqueia.

### 3. Ligar a depuração no relógio (uma vez)
1. **Configurações → Sobre o relógio → Software** (ou "Informações do software") → tocar **7 vezes** em **Versão do software** até aparecer "Modo de desenvolvedor ativado".
2. **Configurações → Opções do desenvolvedor** → ligar **Depuração ADB** e **Depuração por Wi-Fi** (aceite o aviso).
3. Confira que o relógio está no Wi-Fi: **Configurações → Conexões → Wi-Fi** (se ele insistir em ficar só no Bluetooth do celular, desligue o Bluetooth do relógio por alguns minutos durante a instalação).

### 4. Parear e conectar o PC ao relógio
1. No relógio: **Opções do desenvolvedor → Depuração por Wi-Fi → Parear novo dispositivo**. Aparecem um **código de 6 dígitos** e um **IP:porta de pareamento**.
2. No PC: `adb pair 192.168.0.50:37099` (o IP:porta da tela de pareamento) e digite o código.
3. Volte uma tela no relógio: a tela de Depuração por Wi-Fi mostra **outro** IP:porta (o de conexão). No PC: `adb connect 192.168.0.50:41235`.
4. `adb devices` deve listar o celular (USB) e o relógio (`192.168.0.50:41235  device`).

### 5. Instalar no relógio
```
adb -s 192.168.0.50:41235 install -r wear-debug.apk
```
Mesma regra do celular: se der `INSTALL_FAILED_UPDATE_INCOMPATIBLE`, `adb -s 192.168.0.50:41235 uninstall com.vitoroliveira.gymtracker` e instale de novo (no relógio não há nada importante, só treinos ainda na fila).

### 6. Primeiro uso
1. Abra o GymTracker no **celular** e entre na conta.
2. Abra o GymTracker no **relógio** (lista de apps, ícone do halter limão). Em alguns segundos os grupos aparecem; se não, toque em **Sincronizar**.
3. No primeiro treino o relógio pede **Batimentos** (ou "Sensores corporais") e **Notificações**: permita as duas (os batimentos e o aviso/atalho dependem delas).
4. Para a tela sempre ligada funcionar, deixe ligado "Tela sempre ativa" (Always On Display) nas configurações de tela do relógio.
5. Depois de instalar, pode desligar a **Depuração por Wi-Fi** (gasta bateria).

### Sem PC (não testado)
Apps de celular como **Bugjaeger** ou **Wear Installer 2** instalam APK no relógio pela Depuração por Wi-Fi (mesmos passos 3 e 4, só que o "PC" é o celular). O APK do celular continua precisando do adb enquanto o Play Protect bloquear a instalação pelo WhatsApp (ver `CONTEXTO_SESSAO.md`).

## Roteiro de teste no aparelho
1. Relógio abre e mostra os grupos do celular; "Sincronizar" mostra "Atualizado às HH:MM". Celular sem conta → "Entre na sua conta no celular".
2. Iniciar um grupo: o primeiro exercício vem com a última carga; a coroa/bezel muda a carga; tocar em *reps* e girar muda as repetições.
3. REGISTRAR → vibra, entra no descanso; −15/+15 funcionam; apagar a tela (cobrir com a mão) e esperar: **vibra no fim** mesmo apagado.
4. Ao completar as séries de um exercício, passa sozinho ao próximo. Desfazer a última série volta para o exercício dela.
5. Registrar uma carga acima do recorde → "RECORDE!" e vibração diferente.
6. Batimentos aparecem na série, no descanso e nos números.
7. Baixar o pulso: tela em preto e branco com "Descanso até ...". Sair para o mostrador: o ícone do halter com cronômetro aparece e volta ao app.
8. Terminar → resumo "Salvo no celular"; no celular, o treino aparece no Histórico com as séries e a hora certa, e no feed.
9. Modo avião no celular, terminar outro treino → "Na fila"; tirar do modo avião, abrir o app no relógio → a fila some e o treino aparece no Histórico.
10. Tile: adicionar no carrossel, ver a semana e tocar em "Treinar" (deve abrir direto no treino do grupo sugerido).
11. Complicação: colocar "Treinos na semana" num mostrador que aceite complicações (anel ou texto curto).
12. Anilhas: carga 62,5 kg → cada lado 20 + 1,25 (barra 20); trocar a barra para 15.
13. Adicionar um exercício de fora do grupo, registrar uma série e conferir no Histórico do celular.
14. Intervalos → Tabata: 5 s de preparo com 3 toques, vibração longa ao começar, duas curtas na pausa, "FIM!" depois de 3:55. Pausar e seguir não perdem o tempo. Baixar o pulso: a tela de sempre ligada mostra "INTERVALOS até HH:MM".

## Chave de assinatura fixa (opcional, recomendado)
Gere uma vez no PC (pasta qualquer, fora do repositório):
```
keytool -genkeypair -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```
Converta para base64 (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes("debug.keystore")) | Set-Clipboard`) e cadastre em GitHub → Settings → Secrets and variables → Actions → **New repository secret** com o nome `DEBUG_KEYSTORE_BASE64`. A partir daí todos os APKs saem com a mesma chave: dá para atualizar sem desinstalar e misturar celular e relógio de execuções diferentes. Guarde o `debug.keystore` (nunca no git).
