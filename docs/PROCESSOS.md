# PROCESSOS — GymTracker

Puxado das notas do Vault (`nt-agents-*`, `nt-processo-*`, `nt-celular`, `nt-api`) em 21/11/2026, adaptado ao app Android.
O `CLAUDE.md` manda ler este arquivo ao começar a sessão. Quem esquecer, repete erro que já custou caro.

## 1. Início de sessão: o que ler (nesta ordem, só isto)
1. `CLAUDE.md` → 2. `docs/CONTEXTO_SESSAO.md` (estado, armadilhas, próximos passos) → 3. **se for mexer em interface:** `docs/DESIGN.md` (regras, mapa de referências §10) → 4. **se for trazer componente:** Vault `nt-processo-*` e os caches em `Runtime/docs/*-cache/` (OriginKit, Cult UI, Moxen, Skiper, Shader Gradient).
- O **board Runtime**, projeto **GymTracker**, é a fila de trabalho do Thiago: liste os cards (`list_tasks`), faça os fáceis primeiro, mova **Em desenvolvimento → Revisão → Concluído** (Concluído só depois de conferido no aparelho/emulador) e **comente o que fez**. Ideias e sobras viram **cards** (nada se perde para "outro dia").
- API do Runtime: `https://obmbjszdfvwhajgvkfpi.supabase.co/functions/v1/api` com `Authorization: Bearer $RUNTIME_TOKEN` e corpo JSON (`action`: `list_tasks`, `create_task`, `move_task`, `comment`). Nome de coluna com acento: use `\u00e3` no JSON (o shell Windows estraga o UTF-8).

## 2. Como verificar (obrigatório antes de dar por pronto)
1. **Compilar e testar pelo terminal real** (`mcp__terminal__run_in_terminal` + script `.ps1`): `.\gradlew.bat :app:assembleDebug :shared:testDebugUnitTest` (+ `:app:testDebugUnitTest --tests '*ThemeContrastTest*'`). O Bash do Claude **não** roda o Gradle ("Unable to establish loopback connection") e enxerga um AppData virtualizado.
2. **Olhar a tela real no emulador** (AVD `gym35`, Pixel 7 Android 35): instalar o APK, abrir, fotografar com `adb exec-out screencap -p`. Não confie só em código nem em "BUILD SUCCESSFUL".
3. **Build suspeitamente rápido (<15 s) com Kotlin alterado?** Confira a data do APK (`stat app/build/outputs/apk/debug/app-debug.apk`) e que o log é do build novo (apague o log antes de rodar).
4. Contraste dos temas: `ThemeContrastTest` (texto ≥ 4,5:1, destaque ≥ 3:1, texto do botão ≥ 4,5:1). Visual dos temas: `scratchpad/themeshots.py` (troca o tema pelas preferências via `run-as` e monta folhas de contato).
5. CI do GitHub (`.github/workflows/android.yml`) gera o APK e roda os testes do `:shared`.
6. O emulador é lento (render por software); "Skipped N frames" nele não vale para celular de verdade. Cold start: espere ~20 s antes de fotografar a Home.

## 3. Regras que já custaram caro
- **Não confie só em código:** abra a tela e olhe.
- **Contraste, movimento, anti-genérico:** ver `docs/DESIGN.md` §2–§4 e §9. Sem animação pulsando; saída mais sutil que a entrada; respeitar "reduzir animações".
- **Porte fiel de componente:** abrir o original (caches em `Runtime/docs`), manter o miolo (física, constantes, curvas), trocar só a casca (React → Compose) e as cores. Recriar "pelo nome" ou "do meu jeito" já errou no Runtime (Grid Hole, Lightfall, Shader, Parallax). Sem acesso ao original, não fazer.
- **Não disfarçar User-Agent** nem contornar proteção de site ao pesquisar referências.
- **Heredoc com aspas no shell quebra** scripts com `'`, `` ` `` e `\`: escreva scripts de edição e testes com a ferramenta **Write** em arquivo. Barra invertida em regex Kotlin gravada pelo Bash some (`"\\s+"` virou `"\s+"`): evite escapes.
- `local.properties`: `sdk.dir=C:/Android/Sdk` com **barras normais** (barra invertida é escape no `.properties`) e `SUPABASE_URL`/`SUPABASE_KEY` (chave publicável, extraída do APK do CI). Nunca versionar.
- `adb shell` pelo Git Bash converte `/sdcard` em caminho do Windows: use `MSYS_NO_PATHCONV=1`.
- `Monitor` que espera `FIM` num log pode ler o log da rodada anterior: apague o log antes de iniciar.
- Aviso vermelho no login "gerado sem o endereço do Supabase" = faltou `SUPABASE_URL/KEY` no `local.properties`.
- Não digitar senha de conta no app (CLAUDE.md); o Thiago digita. Conta de teste dele: pode criar e apagar treinos nela.
- O app/banco é de **outra conta** (projeto `ygbrrypyydxnuskrobeu`): migrações não se aplicam daqui; vão para `docs/MIGRACOES_PENDENTES.md` com o SQL pronto e a ordem.
- **Testes de galeria (Robolectric) NÃO rodam no Windows:** o `:app:testDebugUnitTest` completo falha com `UnsatisfiedLinkError ... RenderNode.nCreate` (gráficos nativos do Robolectric só existem em Linux/Mac; o CI do GitHub roda). Aqui valem: `:shared:testDebugUnitTest` e `:app:testDebugUnitTest --tests '*ThemeContrastTest*'`. Não gaste tempo caçando essas 13 falhas.
- Disco: o C: enche (qBittorrent). Antes de compilar, `df -h /c`; Gradle/emulador pedem ~10 GB livres.

## 4. Gestão da context window (economizar token)
- Sessão nova começa só por `CLAUDE.md` → `docs/CONTEXTO_SESSAO.md`. Não releia o repositório: abra só o que a tarefa toca (`Read` com `offset/limit`, `Grep`).
- Não cole saídas longas (logs, HTML, listas) na conversa: grave em arquivo no scratchpad e leia o trecho. Testes: mostre só resumo/falhas. Imagens: folhas de contato (várias telas por imagem), não uma por uma.
- Uma tarefa por sessão. Ao fechar cada bloco, escreva a entrada em `CONTEXTO_SESSAO.md`, commite e só então siga. Passando de ~60% do contexto, atualize o CONTEXTO e sugira `/clear`.
- Responda curto: sem recapitular o que já foi feito.
- Rode a suíte inteira em segundo plano e olhe só as falhas.

## 5. Roteiro de conferência no aparelho (5 minutos, antes de dar uma versão por boa)
O emulador não pega teclado real, barra de gestos, bateria dos fundos animados nem aparelhos lentos. Passe no celular do Thiago:
| # | Passo | Deve acontecer |
|---|---|---|
| 1 | Abrir o app e entrar | Tela de login; o teclado não cobre o botão Entrar; cai na Home |
| 2 | Home | Aviso "Retomar" se houver treino aberto; cartão **Próximo treino**; números rolam; nada cortado |
| 3 | Iniciar treino pelo cartão | Abre o treino do grupo sugerido; peso/reps pré-preenchidos |
| 4 | Registrar uma série (−1/+1, −2,5/+2,5) | Entra na lista; o **descanso** aparece como ilha embaixo e acompanha a rolagem |
| 5 | Pausar/fechar a ilha com o polegar | Alvos ≥ 48 dp, responde na hora |
| 6 | Finalizar o treino | Resumo; volta à Home com os números atualizados |
| 7 | Histórico → ⋮ → Apagar → **Desfazer** | Treino some e volta ao mesmo lugar |
| 8 | Aparência: trocar 3 temas de layouts diferentes | Troca na hora; texto legível; nada pesado |
| 9 | Girar a tela e fechar/abrir o app no meio de um treino | Estado preservado (retomar) |
| 10 | Deixar um treino aberto >1 h sem série | Ao voltar, o app encerra no horário da última série |

## 6. Vault: tudo que for novo aqui também vai para o Vault (regra do Thiago, 13/11)
- Todo **conhecimento novo** (seção de `docs/DESIGN.md`, este arquivo, regra que custou caro, padrão) e todo **componente portado** tem que chegar ao Vault **na mesma sessão**, antes de fechar o card.
- Escreva em Markdown, uma seção `## …` por ideia (o importador separa por seção) e rode `node ../Vault/scripts/import-runtime.mjs` (idempotente; o script também lê `GymTracker/docs/DESIGN.md` e `PROCESSOS.md`). Registre no `CONTEXTO_SESSAO.md`: «Vault: N itens (+K novos)».
- Itens de terceiros (OriginKit, Cult UI, Moxen, Skiper) mantêm o aviso de licença; o repositório do Vault fica privado. **Nunca** guardar segredo no Vault.

## 7. Banco e migrações
- Só migrações **aditivas** e idempotentes (`create ... if not exists`, `add column if not exists`); nunca quebrar o que já está em produção.
- Em toda tabela nova: **revogar permissões de `anon`** e conferir com `information_schema.role_table_grants`.
- Gerar o `id` no cliente e inserir; `insert(...) { select() }` falha com RLS quando a política só enxerga a linha depois de um gatilho.
- Código novo que usa coluna nova **não pode ir antes da migração** (quebra o insert): proteja com campo opcional/`EncodeDefault.NEVER` ou entregue junto com instrução clara.

## 8. Commit e entrega
- Commits pequenos, em português, descritivos, com a linha de coautoria. Push no branch de trabalho; PR só quando o Thiago pedir.
- Fim de sessão: atualizar `docs/CONTEXTO_SESSAO.md` (feito, testado, não testado, onde parou, pendências) antes de qualquer limpeza.
