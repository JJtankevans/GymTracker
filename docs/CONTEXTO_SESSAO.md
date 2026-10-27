# GymTracker — contexto para retomar (atualizado em 27/10/2026)

Leia isto primeiro. O histórico longo de decisões, rodada por rodada, está em `docs/PROXIMOS_PASSOS.md` (as notas detalhadas das sessões de 21 a 27/10 estão no anexo do fim); referências de design em `docs/REFERENCIAS_DESIGN.md`; visão geral do projeto no `README.md` da raiz.

**Onde registrar cada coisa:** estado atual e próximos passos → este arquivo; o que foi feito em cada rodada → `PROXIMOS_PASSOS.md`; erros encontrados e como corrigir → `ERROS_CONHECIDOS.md`; tabelas/SQL → `app/docs/supabaseSchema.md` + `docs/supabase/`; social → `SOCIAL_DESIGN.md`; imagens → `IMAGENS_EXERCICIOS.md`. Regras de fim de sessão (avisar, atualizar este arquivo, pedir autorização para commit) estão no `CLAUDE.md` da raiz.

## Sessão de 27/10/2026 (4ª, nuvem): correção dos erros de média prioridade (5 a 13) — COMPILADO E TESTADO NO CI, NÃO VISTO NO APARELHO
- **Branch `claude/correcoes-media-prioridade`** (sai do `master` já com o PR #7 mergeado), com PR aberto para o `master`. CI da execução 40508191130: APK gerado e os 260 `@Test` do `:shared` passando (+11). **Aviso do CI: os secrets `SUPABASE_URL`/`SUPABASE_KEY` não estão configurados no repositório**, então o APK do Actions abre mas o login não funciona; cadastrar em Settings → Secrets and variables → Actions (ver "APK pelo GitHub Actions").
- **O que mudou** (detalhes na seção "Corrigidos" de `ERROS_CONHECIDOS.md`):
  - 5: nada no código — o Thiago rodou a `009`.
  - 6: cadastro sem sessão (confirmação de e-mail ligada) mostra "Confirme seu e-mail" com "Ir para o login" e "Reenviar e-mail". Como o Supabase envia esse e-mail: seção "Confirmação de e-mail" abaixo.
  - 8: botão "Registrar série" desligado enquanto a série grava; o circuito enfileira as séries.
  - 9: preparação do circuito preenche a última carga quando ela chega.
  - 10: **migração `012_datas_no_fuso_de_brasilia.sql` — PENDENTE no Supabase** (testada no Postgres local).
  - 11: fim do treino/circuito troca a Home antiga por uma nova.
  - 12: na lista de um grupo, "Tirar do grupo" (mantém exercício e histórico) separado de "Apagar de vez".
  - 13: recordes recalculados a cada série registrada/editada/apagada; confete só para recorde novo.
- **Não testado no aparelho:** nada disso. Roteiro: (a) rodar a `012` e conferir em Recordes/detalhe do exercício a data de um treino feito depois das 21h; (b) criar conta nova com a confirmação ligada (ver abaixo) e ver o aviso; (c) tocar duas vezes rápido em "Registrar série" (lista e Focus) e ver uma série só; (d) circuito de um grupo com histórico: a carga de cada exercício deve vir preenchida; (e) terminar um treino e apertar "voltar" na Home (deve sair do app); (f) num grupo, "Remover" um exercício que está em dois grupos → "Tirar do grupo" → ele continua no outro com o histórico; (g) registrar 200 kg por engano, ver o confete, editar para 20 kg: o selo some.

## Sessão de 27/10/2026 (3ª, nuvem): correção dos erros 1 a 4 (e 7) — COMPILADO E TESTADO NO CI, NÃO VISTO NO APARELHO
- **Branch `claude/correcoes-erros-1-4`** (sai do `master`; inclui o commit da revisão de documentação da 2ª sessão) virou o PR #7, **mergeado no `master` em 27/10/2026**. CI da execução 40505937212: APK gerado e todos os testes do `:shared` passando (agora 249 `@Test`).
- **O que mudou** (detalhes na seção "Corrigidos" de `ERROS_CONHECIDOS.md`):
  1. **Sair da conta apaga os dados do usuário no aparelho** (peso, perfil corporal, medidas, agenda, favoritos/recentes do catálogo, ritmo do circuito, músicas não enviadas, CSV exportado), depois de tentar enviar o peso pendente para a nuvem (até 5 s). Interface `LocalUserData` no `:shared`, implementação `ui/LocalUserDataImpl.kt`. O diálogo de sair avisa o que é apagado. **As medidas corporais se perdem ao sair** (ainda não têm tabela na nuvem). Tema e preferências do aparelho ficam.
  2. **Rotas:** `routeArg()`/`routeText()` em `Navigation.kt` (`Uri.encode`, sem `URLDecoder`): "/", "?", "#" e "%" em nomes não fecham mais o app.
  3. **Limite de séries mantido** (plano + 2), com mensagem dizendo exercício e limite; o circuito mostra os avisos (Snackbar) e não deixa planejar mais séries que o limite.
  4. **Fim do treino espera as séries em gravação** (última série do circuito entra no resumo e no post).
  7. Sair sem internet não fecha o app e apaga a sessão local (`clearSession`).
- **Não testado no aparelho:** nada disso. Roteiro: (a) com a conta A, registrar peso, medida e agenda; sair; entrar com a conta B e ver que nada de A aparece (e que o peso de A volta ao entrar de novo com A); (b) sair com o celular em modo avião; (c) criar o grupo "Peito/Tríceps" e iniciar treino e circuito nele; exercício com "%" no nome e abrir o detalhe; (d) circuito: tentar passar do limite de séries na preparação e ver a mensagem; terminar um circuito e conferir no resumo/Histórico/feed que a última série está lá.
- **Risco a olhar:** a correção 2 confia que o Navigation decodifica o argumento da rota (comportamento documentado do Navigation); se um nome aparecer com `%20` na tela, é isso.

## Sessão de 27/10/2026 (2ª, nuvem): revisão de código e documentação — NENHUM CÓDIGO DO APP ALTERADO
- **Pedido:** analisar o projeto, alinhar a documentação ao código e listar os possíveis erros com a correção.
- **Feito:** leitura de todo o `:shared`, das telas principais do `:app` e de todos os SQL. Resultado em **`docs/ERROS_CONHECIDOS.md`** (30 itens, cada um com onde está, o que acontece e como corrigir). `README.md` criado (o projeto não tinha). Este arquivo reescrito com o estado real; as notas longas das sessões anteriores foram para o anexo de `PROXIMOS_PASSOS.md`. Ajustados: `SOCIAL_DESIGN.md` (status), `IMAGENS_EXERCICIOS.md` (o script de corte está versionado), `app/docs/PlanoDeAcao.md` (aviso de documento histórico), `app/docs/supabaseSchema.md` (colunas posteriores e alerta de datas em UTC).
- **Testado:** só no Postgres 16 local: `'now()'::timestamptz` é aceito (o `finishWorkout` grava a hora do servidor, ok) e `started_at::date` devolve o dia em UTC (treino às 22h30 de Brasília cai no dia seguinte; erro nº 10).
- **Não testado:** nada foi compilado nem rodado no aparelho nesta sessão (não houve mudança de código). Os erros listados vêm da leitura do código; nenhum foi reproduzido no celular.
- **Os 4 erros mais graves** (corrigidos na 3ª sessão, acima; detalhes em `ERROS_CONHECIDOS.md`): (1) dados corporais (peso, altura, medidas) ficam no aparelho sem separar por conta e são enviados para a conta que estiver logada — com as duas contas de teste no mesmo celular pode já ter havido mistura; (2) grupo com `/` no nome (ex.: "Peito/Tríceps") fecha o app ao iniciar o treino ou o circuito; (3) no circuito, séries acima de "padrão + 2" são descartadas sem aviso; (4) no circuito, a última série pode ficar fora do resumo e do post no feed.

## Estado atual
- **PR aberto:** `claude/correcoes-media-prioridade` (erros 5 a 13), sem teste no aparelho. O PR #7 (erros 1 a 4 e 7) já está no `master`, também sem teste no aparelho.
- **Branch `master` tem o resto.** Os PRs #5 (`claude/instrucoes-sessao`: nome do grupo no histórico, bottom sheet, desafios, catálogo de 70) e #6 (`claude/melhorias`: as 15 melhorias de 26/10 + compartilhar treinos, temas Loucos e extras de 27/10) foram **mergeados em 27/10/2026** sem o teste no aparelho que estava previsto. O CI do `master` (commit `dffb4af`) passou: APK gerado e os 242 testes do `:shared`. Os dois branches podem ser apagados.
- **Migrações pendentes no Supabase** (ver a tabela em "Supabase"): `010`, `011`, `012` e talvez a `008`.

**Testado no celular (Xiaomi, adb) e funcionando** (até 21/10): login/cadastro, Home, treino ativo, seletor de temas (eram 21), catálogo de exercícios (eram 56) com seleção múltipla e favoritos, treinos prontos, Recordes com dado real, Circuito (anel, descanso, botões), exclusão de treino no Histórico (toque longo), ilustrações por exercício, Social com duas contas (perfil público, seguir, feed, reações, grupo por código, desafio e placar, editar post, privacidade, pedido de seguir em perfil "Só seguidores"), exercício em vários grupos (006), foto de perfil (007).

**Nunca visto no aparelho** (compila e os testes do `:shared` passam, mas ninguém viu funcionando):
- 21/10: nome do grupo no Histórico depois de apagar o grupo (precisa da 009); bottom sheet "Iniciar treino" arrastando para cima/baixo, com teclado aberto e muitos grupos; desafio com "Falhas permitidas" e "Dias seguidos" (precisa da 010), fotos no placar, mensagens (layouts alerta/faixa/carimbo), título no perfil; imagens por apelido ("Agachamento Hack", "Rosca Scot") e os 14 exercícios novos do catálogo; tempo da tela de Recordes depois da 008.
- 26/10: aviso de fim do descanso com a tela bloqueada (e o pedido de permissão de notificação); compartilhar treino (texto) e exportar CSV (precisa da 009); sincronização do peso em dois celulares/reinstalação; agenda da semana e "Sugerido" no sheet; calculadora de anilhas e tabela de 1RM; conquistas; medidas corporais; séries por músculo na semana; fotos e títulos no feed e no placar; anotação do treino.
- 27/10: compartilhar/importar grupo por código entre duas contas (precisa da 011); os 7 temas Loucos e as decorações novas de Oceano/Floresta/Ouro (fluidez e legibilidade; Matrix desenha ~300 letras por quadro); mapa de calor no Histórico; aquecimento na calculadora; confete ao bater recorde; conquistas especiais.
- Mais antigos: aceitar o pedido de seguir (logar na v.oliveira, Pessoas > pedidos) e ver perfil/posts/números da Deus na thiagobr; desafio em equipes; privacidade de música; sair de grupo/desafio; tela de erro "Tentar de novo" (Recordes e Social); apagar um treino que tem séries; "Descartar e sair" ao encerrar o Circuito sem série (só teste unitário).

## O app em 5 linhas
Android (Jetpack Compose) + módulo KMP `:shared` (regras, ViewModels, repositórios) + `:wear` (relógio, ainda não sincroniza). Backend: Supabase (projeto `ygbrrypyydxnuskrobeu`). Login por e-mail/senha. Dono: Thiago (`thiagobrito74@gmail.com`). Repo: `github.com/JJtankevans/GymTracker`, branch `master`.

## Números (27/10/2026)
- Catálogo: **70 exercícios** (`shared/.../data/catalog/ExerciseCatalog.kt`, com apelidos para achar a imagem), **9 treinos prontos** (`WorkoutTemplates.kt`).
- Imagens: **41 de 70** em `app/src/main/res/drawable-nodpi/ex_<nome>.png`; as 29 que faltam (15 antigas + 14 novas) estão em `docs/PROMPTS_IMAGENS_EXERCICIOS.md` (`python3 tools/cortar_folha.py --faltando` confere). Sem imagem, cai no ícone do equipamento.
- Temas: **28** em 4 categorias (Profissionais, Ousados, Abstratos, Loucos), 5 famílias de fonte, 6 layouts de Home e 2 de treino ativo (`ui/theme/Themes.kt`).
- Testes: **260** `@Test` no `:shared` no branch `claude/correcoes-media-prioridade` (249 no `master`; todos passando no CI em 27/10), **15** no `:app` (galerias Robolectric/Roborazzi, não rodam no CI) e **1** no `:wear`. Rodar: `./gradlew.bat :shared:testDebugUnitTest`; galerias: `./gradlew.bat :app:testDebugUnitTest -Proborazzi.test.record=true`.

## Supabase
| Migração | O que faz | Situação | Sem ela |
|---|---|---|---|
| `APLICAR_TUDO.sql` (= 001+002+003) | lacunas do esquema, social, dados corporais | aplicada — **não rodar de novo** | — |
| `004` | `public_profiles()` como função | aplicada (não rode depois da 005: desfaria a 005) | — |
| `005` | perfis "Só seguidores" aparecem na busca | aplicada | — |
| `006` | exercício em vários grupos (`group_exercises`) | aplicada | — |
| `007` | foto de perfil (Storage) | aplicada | — |
| `008` | `records_summary()` (Recordes em 1 chamada) | **sem confirmação** | Recordes usa o caminho antigo (mais lento), sem erro |
| `009` | `workouts.muscle_group_name` + gatilho | aplicada (27/10/2026) | — |
| `010` | títulos de desafio | **pendente** | sem títulos (sem erro); criar desafio com "Falhas"/"Dias seguidos" preenchidos falha |
| `011` | compartilhar treino por código | **pendente** | compartilhar/importar dá erro |
| `012` | dia do treino no fuso de Brasília (progressão, recordes, dias ativos) | **pendente** | treino depois das 21h conta no dia seguinte (sem erro) |
- Consultas só de leitura: `consulta_exercicios_de_todos_os_usuarios.sql`, `consulta_uso_do_storage.sql`. `test_social.sql` é para banco de teste.
- O conector Supabase da sessão não enxerga esse projeto; só dá para sondar por REST com a chave publicável. Não dá para conferir RLS por aqui.
- `local.properties` (fora do git) precisa de `SUPABASE_URL` e `SUPABASE_KEY` (chave `sb_publishable_...`).
- **Bug de RLS já corrigido no cliente:** `insert(...) { select() }` (INSERT ... RETURNING) falhava com "Você não tem permissão" em grupos e desafios, porque a política de leitura só enxerga a linha depois do trigger que põe o dono como membro. O app gera o `id` (Uuid), insere e depois busca (`SocialRepositoryImpl.createGroup/createChallenge`). Vale para qualquer tabela nova com política parecida.
- **Datas no servidor:** a view de progressão, `records_summary()` e `profile_stats()` usam `started_at::date` (UTC); desafios e títulos usam o fuso do desafio. Ver erro nº 10 em `ERROS_CONHECIDOS.md`.

## Confirmação de e-mail no cadastro (Supabase)
- **É automático:** com **Authentication → Sign In / Providers → Email → "Confirm email"** ligado, o próprio Supabase manda o e-mail com o link quando o app chama `signUp`. O app não envia nada e **não precisa de tela de validação**: a pessoa toca no link (o Supabase confirma a conta nesse momento) e depois entra no app com e-mail e senha. Antes de confirmar, o login responde "Confirme seu e-mail antes de entrar.". Desligado, a conta já nasce confirmada e o cadastro entra direto na Home.
- **Para usuários reais, configurar SMTP próprio** (Authentication → Emails → SMTP Settings): o servidor de e-mail embutido do Supabase é só para testes (manda poucos e-mails por hora e, nos projetos atuais, só para endereços da equipe do projeto). Serviços com plano grátis: Resend, Brevo, SendGrid, Amazon SES. Sem isso, cadastros de outras pessoas podem não receber o link.
- **Para onde o link leva:** depois de confirmar, o Supabase redireciona para a **Site URL** (Authentication → URL Configuration), que por padrão é `http://localhost:3000` e abre uma página de erro no celular (a conta já fica confirmada mesmo assim). Vale apontar a Site URL para uma página simples ("Conta confirmada, volte ao app"). Abrir o app direto pelo link (deep link `gymtracker://...` + `intent-filter` + `handleDeeplinks` do supabase-kt) é opcional e fica para depois.
- **Texto do e-mail:** Authentication → Emails → Templates → "Confirm signup" (dá para traduzir para português).
- **Reenviar:** o aviso do cadastro tem "Reenviar e-mail" (`resendEmail(OtpType.Email.SIGNUP)`); o limite de envio do Supabase vale também aqui ("Muitas tentativas...").
- **Não conferido daqui:** se o projeto `ygbrrypyydxnuskrobeu` está com "Confirm email" ligado (o conector do Supabase não acessa esse projeto).

## APK pelo GitHub Actions
A nuvem não compila (sem Android SDK), então `.github/workflows/android.yml` compila nos servidores do GitHub a cada push/PR: gera o **APK debug** (artefato `gymtracker-debug-apk`; no celular, baixe pelo navegador do GitHub e instale permitindo "fontes desconhecidas") e roda `:shared:testDebugUnitTest` (relatório no artefato `relatorio-testes-shared`). Precisa dos **secrets** `SUPABASE_URL` e `SUPABASE_KEY` em Settings → Secrets and variables → Actions; sem eles o app abre, mas o login avisa que as chaves não foram configuradas. Os logs do Actions não abrem da nuvem: o workflow escreve o erro como **anotação** (`gh api repos/JJtankevans/GymTracker/check-runs/<job_id>/annotations`). Runner fixo em `ubuntu-24.04` (no Ubuntu 26 a instalação do SDK falhou). O APK é assinado com a chave de debug do runner: se o celular recusar instalar por cima, desinstale o app antigo antes (os dados de treino ficam no Supabase; peso e medidas ficam no aparelho e o peso tem cópia na nuvem).

## Contas de teste (no celular real, banco real)
- `thiagobr` (usuário @AqueleQueTrazALuz, aparece como "Você") e `v.oliveira042997` (@Deus). **Eu (Claude) não digito senha no app** (regra de segurança, vale mesmo para contas de teste em projeto Supabase real); o Thiago troca a conta no celular (Perfil > Sair da conta). As senhas NÃO ficam neste arquivo.
- Estado do Social (21/10): a Deus está em "Só seguidores", com "compartilhar treinos/músicas" ligados, dona do grupo privado "Time Teste" (código `e3aab77a`) e do "Desafio Teste" (treinos feitos, 30 dias, mín. 1 min, aberto). A thiagobr tem um **pedido de seguir pendente** para a Deus, está no grupo e no desafio.
- Dados de teste nas duas contas: posts, treinos curtos, grupo, desafio. No Histórico da conta Thiago há "Push 1min" (recorde 10 kg, útil para testes) e "Sem grupo 3h18min" (2 séries 12×0); um treino ativo vazio no grupo "teste" pode reaparecer como "Treino em andamento" ("Iniciar Novo" descarta).
- **Atenção (erro nº 1, corrigido daqui para a frente):** antes da correção, como as duas contas usam o mesmo celular, os pesos/dados corporais de uma podem ter sido enviados para a outra. Conferir com `select profile_id, count(*), min(measured_on), max(measured_on) from body_weights group by 1;` antes de confiar nesses dados.

## Como trabalhar neste PC
- Windows, sem Python (usar sed/awk/PowerShell). Gradle: `./gradlew.bat :app:assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, pacote `com.vitoroliveira.gymtracker`, `adb install -r <apk>`. Se aparecer "offline"/"no devices": `adb kill-server`, reconectar o cabo, reativar a depuração USB.
- Toques por adb: tela 1080x2400; as capturas vêm reduzidas (fator 1,2: coordenada da imagem × 1,2 = toque real). Cuidado: toques na faixa de cima podem puxar a aba de notificações, que mostra WhatsApp pessoal do Thiago; não interagir. Pop-ups da operadora (TIM / "Launch Browser?") já apareceram; recusar com Cancel.
- Trocar tema sem UI: `adb shell run-as <pacote> sed -i 's/name="theme_id">[a-z]*</name="theme_id">NOVO</' shared_prefs/gymtracker_prefs.xml`. O tema do Thiago é `magma`, intensidade "Insano".
- `build/` e `.kotlin/` estão no `.gitignore`, mas `.kotlin/` ainda está versionado (erro nº 26: `git rm -r --cached .kotlin`).

## Armadilhas já encontradas
- `import X as Y` esconde o nome original no arquivo → usar `typealias`.
- Robolectric trava (`AppNotIdleException`) com `AlertDialog`; `ModalBottomSheet` funciona.
- ViewModel: iniciar `loadX()` depois de setar erro apaga o erro (recarga assíncrona). Usar `loadExercises(clearError = false)`.
- Nome de exercício continua único por usuário, mas um exercício pode estar em vários grupos (006): quem adiciona exercício deve usar `ExerciseRepository.addToGroups` (reaproveita o existente) em vez de `insert` direto, senão estoura a restrição de nome. O treino pronto e a importação desfazem o grupo se falharem.
- Catálogo marca "já adicionado" só o que já está no grupo filtrado (`ExerciseUiState.namesInScope`); sem filtro, olha todos.
- `ExerciseRepository.update` deixa os vínculos exatamente em `Exercise.groupIds`: um `Exercise` montado à mão (sem `groupIds`) apagaria os vínculos. Sempre `copy` de um exercício vindo do repositório.
- **Rotas de navegação:** texto livre (nome de grupo/exercício) na rota passa por `routeArg()` e é lido com `routeText()` (`Navigation.kt`); nunca montar a rota com o texto cru nem decodificar de novo com `URLDecoder`.
- **Dados locais não são por usuário** (peso, medidas, agenda, recentes): são apagados ao sair da conta por `LocalUserDataImpl`. Qualquer dado novo do usuário guardado no aparelho precisa entrar no `clear()` dela.
- Layout: três `FilterChip` em `Row` quebram letra por letra; usar `FlowRow`. Números em `Row(SpaceBetween)` colam; usar `weight(1f)`.
- Playfair precisa de `lnum`; fonte variável = um arquivo com pesos por eixo (`ThemeFonts.variable`).
- Git Bash `sed` não lida com acentos (`y///`); para normalizar acentos use PowerShell. PowerShell lê scripts com acento mal sem BOM: usar `[char]0xED` etc.
- Com treino ativo aberto, "Iniciar" na Home pula o sheet e vai direto ao treino (scripts por coordenada quebram).
- Ambiente de nuvem (claude.ai/code): sem Android SDK e sem `dl.google.com`; quem compila é o GitHub Actions. Dá para testar SQL com o Postgres 16 em `/usr/lib/postgresql/16/bin` (iniciar como usuário `postgres` num diretório próprio, ex. `/var/tmp`). O contêiner é descartado: commit + push antes de encerrar.

## Próximos passos (ordem sugerida)
0. **Supabase:** rodar `010`, `011` e `012` (e a `008`, se não foi; a `012` também recria a `records_summary()`) no SQL Editor; são idempotentes. Conferir `body_weights` por conta (erro nº 1). Decidir sobre "Confirm email" e SMTP próprio (seção acima).
1. **Validar no celular** as correções dos erros 1 a 4 (roteiro na 3ª sessão, já no `master`) e revisar/mergear o PR dos erros 5 a 13 (roteiro na 4ª sessão). Depois, os de baixa prioridade em `ERROS_CONHECIDOS.md`. Cada correção com teste no `:shared` quando for lógica.
2. **Validar no celular** a lista "Nunca visto no aparelho" acima, começando pelo que tem mais risco: aviso de descanso com tela bloqueada, compartilhar/importar entre as duas contas, exportar CSV, temas Loucos (fluidez), bottom sheet.
3. Terminar o Social: aceitar o pedido (logar na v.oliveira), conferir o perfil na thiagobr; equipes, música, sair do grupo/desafio, "Tentar de novo".
4. Imagens: o Thiago gera as 29 que faltam (`docs/PROMPTS_IMAGENS_EXERCICIOS.md`; de preferência regerar tudo no mesmo estilo) e cortamos com `tools/cortar_folha.py`. Confirmar os apelidos "Fly" (voador) e "Fly Costas" (voador inverso).
5. Conferir RLS no painel do Supabase (Authentication/Policies), inclusive `group_exercises` e `shared_routines`, e o alerta "Security Definer View". Considerar um Supabase local (Docker) para testes automáticos com login.
6. Produto pendente: notificações (pedido de seguir, desafio), ligas semanais, sincronizar o relógio, medidas corporais na nuvem (precisa de tabela nova), "versão insana" própria por tema, mais exercícios (cardio, mobilidade), link clicável para importar treino (domínio + App Links), recorte interativo da foto.
7. Limpeza: dados de teste das contas; `.kotlin/` e `.artifacts/` versionados; `exercises.muscle_group_id` (legado) pode sair depois que tudo estiver estável; erros 26 a 30.
8. Segurança: as senhas das contas de teste apareceram em conversa; trocar antes de ter usuários reais.

## Mapa rápido do código
Caminhos curtos: `shared/...` = `shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/`; `app/...` = `app/src/main/java/com/vitoroliveira/gymtracker/`.
- **Injeção:** `shared/.../di/AppModule.kt` (repositórios e ViewModels comuns), `app/.../ui/UiModule.kt` (o que precisa de `Context`: temas, música, armazenamento local, circuito, corpo, agenda). Cliente Supabase: `di/SupabaseClient.kt`.
- **Navegação:** `app/.../ui/Navigation.kt`; Home e barra: `ui/home/HomeScreen.kt`, `HomeLayouts.kt`.
- **Sair da conta / dados locais:** `domain/repository/LocalUserData.kt`, `app/.../ui/LocalUserDataImpl.kt`, `ProfileViewModel.signOut`.
- **Treino ativo:** `presentation/viewmodel/WorkoutViewModel.kt` (limite de séries: `maxSetsFor`), `presentation/TimerManager.kt`; telas `ui/workout/ActiveWorkoutScreen.kt`, `FocusWorkout.kt`, `StartWorkoutSheet.kt` (sugestão/agenda/peso do dia), `RestNotifier.kt` (notificações do descanso), `RecordCelebration.kt` (confete).
- **Circuito:** motor `presentation/circuit/` (`CircuitEngine`, `SetTimingLearner`, `ReferenceTimes`), tela `ui/circuit/CircuitScreen.kt`.
- **Grupos/exercícios:** `data/model/Exercise.kt` (`groupIds`), `GroupExerciseLink.kt`, `data/repository/ExerciseRepositoryImpl.kt`, `domain/usecase/AddExerciseToGroups.kt`, `CreateWorkoutFromTemplate.kt`; telas `ui/muscleGroup/MuscleGroupScreen.kt`, `TemplateSheet.kt`, `ScheduleUi.kt` (agenda), `ui/exercise/ExerciseScreen.kt` (`ExerciseDialog`), `ExerciseCatalogSheet.kt` (inclui `ExerciseArt`).
- **Compartilhar treino:** `presentation/share/RoutineShareViewModel.kt`, `domain/usecase/ImportSharedRoutine.kt`, `data/repository/RoutineShareRepositoryImpl.kt`, UI `ui/muscleGroup/ShareUi.kt`.
- **Histórico e detalhe:** `presentation/viewmodel/HistoryViewModel.kt`, `WorkoutDetailViewModel.kt`, `presentation/workout/WorkoutSummary.kt` (resumo e texto de compartilhar), `TrainingCalendar.kt`; telas `ui/history/HistoryScreen.kt` (toque longo apaga), `TrainingHeatmap.kt`, `ui/workout/WorkoutDetailScreen.kt`. Nome do grupo: `domain/WorkoutGroupName.kt`.
- **Recordes e exercício:** `presentation/records/`, `ui/records/RecordsScreen.kt`, `presentation/viewmodel/ExerciseDetailViewModel.kt`, `ui/exercise/ExerciseDetailScreen.kt`; contas de anilhas/1RM/aquecimento em `presentation/workout/GymMath.kt`.
- **Corpo e evolução:** `presentation/body/` (`BodyViewModel` com `BodyRepository`, `EvolutionViewModel`, `LoadSuggester`, `StrengthStandards`, `MuscleVolume`), `data/repository/BodyCloudImpl.kt`, `ui/body/EvolutionScreen.kt`, `PrefsBodyStore.kt`.
- **Conquistas e exportação:** `presentation/achievements/`, `ui/achievements/AchievementsScreen.kt`; `presentation/workout/CsvExport.kt`, `ExportViewModel.kt`, `ui/profile/CsvShare.kt`.
- **Social:** modelos `data/model/Social.kt`, repositório `data/repository/SocialRepositoryImpl.kt`, regras `presentation/social/SocialRules.kt`, `SocialViewModel.kt`, `ChallengeMessages.kt`, `ProfileTitles.kt`; telas `ui/social/SocialScreen.kt`, `SocialDialogs.kt` (`ChallengeDialog`, `ChallengeSheet`), `ChallengeMessageCard.kt`, `TitleChip.kt`.
- **Perfil e foto:** `presentation/viewmodel/ProfileViewModel.kt`, `domain/AvatarRules.kt`, `ProfileRepositoryImpl.uploadAvatar/removeAvatar`, `ui/profile/{ProfileScreen,UserAvatar,AvatarImageProcessor}.kt`.
- **Música:** `ui/music/MusicController.kt` (sessão de mídia, cor da capa), `MiniPlayer.kt`, `WorkoutTrackRecorder.kt` (grava as faixas do treino, desligado por padrão).
- **Temas:** `ui/theme/Themes.kt` (lista), `ThemeSpec.kt`, `ThemeBuilders.kt`, `ThemeDecor.kt`, `ThemeDecorCrazy.kt` (Loucos), `ThemeFonts.kt`, `ThemePickerScreen.kt`, `ThemePreferences.kt`.
- **Erros amigáveis:** `presentation/ErrorMessages.kt` (`toUserMessage`).
- **SQL:** `docs/supabase/` (situação na tabela acima). Esquema base e views: `app/docs/supabaseSchema.md`. Imagens: `tools/cortar_folha.py`.

## Preferências do Thiago
Quer autonomia ("yolo, sem perguntas"), mas **antes de commitar/encerrar a sessão ou limpar disco, avisar e pedir autorização** (ver `CLAUDE.md`); temas variados e profissionais (contidos e impactantes), referências de design levadas a sério, e relatório honesto do que não foi testado no aparelho. Gosta de ícones/desenhos em vez de emojis. Baixar arquivos externos ele autorizou uma vez; pedir de novo para outros. Não apagar dados dele. Mensagens em português do Brasil, diretas.
