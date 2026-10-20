# GymTracker — contexto para retomar (20/10/2026)

Arquivo feito para começar uma conversa nova sem perder o fio. Leia isto primeiro; o histórico longo está em `docs/PROXIMOS_PASSOS.md`.

## O app em 5 linhas
Android (Jetpack Compose) + módulo KMP `:shared` (regras, ViewModels, repositórios) + `:wear` (relógio, não sincroniza ainda). Backend: Supabase (projeto `ygbrrypyydxnuskrobeu`, região do usuário). Login por e-mail/senha. Dono: Thiago (`thiagobrito74@gmail.com`).

## Estado atual (tudo compila e os testes passam)
**Funciona e foi visto no celular (Xiaomi, Android conectado por adb):** login e cadastro, Home, treino ativo (modo Foco), anel do descanso corrigido, seletor de temas, catálogo de exercícios (criar 1 exercício e salvar no Supabase), lista de Grupos e a folha "Treinos prontos", fontes embutidas em Carbon/Arena/Ouro/Nórdico/Magma.

**Feito, mas só validado por testes unitários e capturas Robolectric (NUNCA aberto no celular):**
- Tela **Recordes** (troféu na Home) com dados reais; só vimos o estado vazio no aparelho.
- **Favoritos e Recentes** no catálogo (estrela, filtros).
- Seleção **múltipla** no catálogo e **criar treino pronto** de verdade (Push/Pull/Pernas...).
- **Social inteiro** (ícone de grupo na Home): Feed + reações, Grupos (criar, código, entrar/sair), Desafios (criar, entrar, placar, equipes, meta), Pessoas (seguir, pedidos, perfil com músicas), Privacidade, editar post.
- Diálogos (criar grupo/desafio, privacidade, editar post): **só compilam**, nunca foram abertos.
- Tela de erro "Tentar de novo" (Recordes e Social).
- Modo Circuito: usa o mesmo anel corrigido, mas não foi aberto depois da correção.

## Números
- 21 temas (7 novos: Nórdico, Obsidiana, Cobalto, Floresta, Arena, Ouro, Ártico) com 5 famílias de fonte embutidas (`res/font`, `ThemeFonts.kt`).
- Catálogo: 56 exercícios (`data/catalog/ExerciseCatalog.kt`), 9 treinos prontos (`WorkoutTemplates.kt`).
- Testes: 115+ no `:shared` (rodar: `./gradlew :shared:testDebugUnitTest`), galerias de captura no app (`./gradlew :app:testDebugUnitTest -Proborazzi.test.record=true`, imagens em `app/build/outputs/{theme,screen,social,circuit}-gallery`).

## Supabase
- Todas as tabelas e funções existem (checado com a chave publicável): base + social + peso corporal. `APLICAR_TUDO.sql` já foi rodado pelo Thiago; **não rodar de novo**.
- O conector Supabase da sessão **não enxerga** esse projeto; só dá pra sondar por REST com a chave publicável. Então não dá pra conferir políticas (RLS) por aqui. Visitante sem login lê lista vazia (bom) e `public_profiles()` nega (bom).
- `local.properties` (fora do git) precisa de `SUPABASE_URL` e `SUPABASE_KEY` (a chave publicável `sb_publishable_...`). Sem isso o app builda com URL vazia e o login diz "sem internet". Agora o login avisa isso.
- Dados de teste na conta real do Thiago (não apagados): grupo "teste", exercícios "aaaaa" e "Supino Reto com Barra", treino ativo de 1h30+ com 2 séries "12 × 0", peso corporal 130 kg registrado. Nada de social foi criado.

## Como trabalhar neste PC
- Windows, sem Python (usar sed/awk). Gradle: `./gradlew.bat :app:assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, pacote `com.vitoroliveira.gymtracker`. Instalar: `adb install -r <apk>`.
- Trocar tema sem UI: `adb shell run-as <pacote> sed -i 's/name="theme_id">[a-z]*</name="theme_id">NOVO</' shared_prefs/gymtracker_prefs.xml` e reabrir. **Devolver `magma`** (tema do Thiago) no fim. A intensidade dele está em "Insano".
- Toques por adb: tela 1080x2400; as capturas de tela aparecem reduzidas (fator 1,2).
- O git rastreia pastas `build/` (status vem sujo de arquivos gerados). **Nada foi commitado.**

## Armadilhas já encontradas
- `import X as Y` esconde o nome original no arquivo → usar `typealias`.
- Robolectric trava (`AppNotIdleException`) com `AlertDialog`; `ModalBottomSheet` funciona.
- Em ViewModel: iniciar `loadX()` depois de setar erro apaga o erro (a recarga é assíncrona). Usar `loadExercises(clearError = false)`.
- Playfair precisa de `lnum` (números alinhados), senão o 0 parece "o".
- Fonte variável: um arquivo, pesos por eixo (`ThemeFonts.variable`).

## Próximos passos (ordem sugerida)
1. **Testar no celular** o que nunca foi aberto (lista acima). Prioridade: criar treino pronto, catálogo com várias seleções + favoritos, Recordes com séries de carga real, abrir cada diálogo do Social.
2. **Social com duas contas** (criar a 2ª conta no app): seguir (perfil público x privado), aceitar pedido, feed (ativar "compartilhar treinos", finalizar um treino e ver o post), reações, grupo por código, desafio individual e em equipes (conferir o placar no banco), editar post.
3. Conferir **RLS** no painel do Supabase (Authentication/Policies) e o alerta "Security Definer View".
4. Modo Circuito: abrir com um grupo e conferir o anel e o descanso (precisa descartar ou finalizar o treino ativo de teste).
5. Pendentes de produto: notificações (pedido de seguir, desafio), foto de perfil, ligas semanais, aviso do descanso com o app em segundo plano, sincronizar relógio, sincronizar dados corporais com a nuvem, temas com "versão insana" própria, foto/ilustração por exercício no catálogo, mais exercícios (cardio, mobilidade).
6. Limpeza: dados de teste da conta, commit (separar código de `build/`), migrar `kotlinx.datetime.Instant` deprecado no HistoryViewModel.

## Mapa rápido do código
- Catálogo/treinos prontos: `shared/.../data/catalog/`, UI `app/.../ui/exercise/ExerciseCatalogSheet.kt`, `ui/muscleGroup/TemplateSheet.kt`.
- Recordes: `shared/.../presentation/records/`, `app/.../ui/records/RecordsScreen.kt`.
- Social: modelos `data/model/Social.kt`, repositório `data/repository/SocialRepositoryImpl.kt`, regras `presentation/social/SocialRules.kt`, `SocialViewModel.kt`, telas `app/.../ui/social/SocialScreen.kt` e `SocialDialogs.kt`.
- Temas: `ui/theme/Themes.kt` (lista), `ThemeDecor.kt` (fundos), `ThemeFonts.kt`, `ThemeBuilders.kt`.
- Navegação: `ui/Navigation.kt`; barra da Home: `ui/home/HomeScreen.kt` (Recordes, Social, Perfil, menu ⋮ com Evolução e Aparência).
- SQL: `docs/supabase/` (`APLICAR_TUDO.sql` já aplicado).

## Preferências do Thiago
Quer autonomia ("yolo, sem perguntas"), temas variados e profissionais (contidos e impactantes), referências de design levadas a sério (`docs/REFERENCIAS_DESIGN.md`), e relatório honesto do que não foi testado no aparelho. Baixar arquivos (ex.: fontes) ele autorizou uma vez; pedir de novo para outros. Não apagar dados dele.

## Atualização (sessão no celular, 20/10/2026)
- Celular testado via adb (build + instalação ok, login ok, Home ok).
- **Treino pronto** testado no aparelho: achou bug (nome de exercício é único por usuário → "Já existe um item com esse nome" e deixava grupo vazio). Corrigido: `CreateWorkoutFromTemplate` pula exercícios que já existem e apaga o grupo se der erro no meio. Validado no celular (Push criado com 5 de 6 exercícios). Mensagem avisa quantos já existiam.
- Limitação de modelo: 1 exercício = 1 grupo, então exercícios repetidos entre treinos prontos (ex.: Supino Reto com Halteres em Push e Superior) só ficam no primeiro grupo criado.
- Ainda NÃO testado no celular: catálogo com seleção múltipla/favoritos, Recordes com dados, Social (todos os diálogos), Circuito.
- Imagens por exercício: Thiago mandou referências (silhuetas roxas com músculo em vermelho, 2 sheets). O card de exercício hoje usa ícone genérico de halter/polia. Plano: ele gera as imagens a partir de uma lista de 54 nomes e o app carrega por nome de arquivo.

## Atualização 2 (testes no celular, 20/10/2026)
Validado no aparelho: Grupos + Treinos prontos (corrigido), catálogo com seleção múltipla e favoritos (favorito persiste), Social (Feed/Grupos/Desafios/Pessoas abrem; diálogos Novo grupo, Novo desafio e Privacidade abrem; nada foi criado no social), Circuito (configuração, anel da série, anel do descanso, encerrar e salvar), Recordes (só o estado vazio).
Corrigido nesta rodada:
- Catálogo marcava "já adicionado" só o que estava no grupo atual; agora considera todos os exercícios (nome é único por usuário) via `ExerciseUiState.allNames`.
- Privacidade/Editar post: chips quebravam "Privado" letra por letra → `FlowRow`.
- Plurais: "1 dia", "1 treino no total", "Falta 1 treino", "faltam 1 dia".
- Circuito: botões do descanso ("Pular", "+15s descanso") com `maxLines = 1` — NÃO reconferido no aparelho depois do ajuste (o texto "Pular descanso" estava cortado).
Efeito colateral: o treino de teste antigo (198 min, 2 séries 12×0) foi finalizado ao encerrar o Circuito; agora há 1 treino no histórico e streak de 1 dia.
Ainda não testado no celular: Recordes com dados (precisa de um treino com carga >0 salvo), Social com duas contas (seguir, feed, reações, grupo por código, desafios), Editar post, tela de erro "Tentar de novo".

## Atualização 3 (Recordes e Circuito no celular, 20/10/2026)
Validado no aparelho: **Recordes com dado real** (Supino Inclinado com Halteres 10 kg × 10, 1RM Epley 13,33 kg, badge NOVO), **botão "Pular" do descanso** (cabe), **texto "próxima:" dentro do anel do descanso** (agora com largura máxima 150dp e reticências), pular descanso conta a série (1/16), resumo do treino (100 kg de volume = 10×10).
Corrigido: encerrar o Circuito sem nenhuma série gravava um treino vazio ("Treino concluído!" com tudo 0) que contava na meta/sequência. Agora o diálogo diz "Descartar e sair" e apaga o treino (`WorkoutViewModel.discardActiveWorkout`, teste unitário `descartarTreinoAtivoApagaEAvisa`). NÃO validado no aparelho (só unitário).
Sujeira de teste na conta do Thiago: no Histórico há 1 treino vazio de 0 min (Push) e o "Sem grupo" de 3h18; a Home conta 3 treinos. O Histórico não tem detalhe nem exclusão de treino (item de produto a considerar). Há também 1 treino ativo vazio no grupo "teste" aberto (o app oferece continuar/iniciar novo).
Armadilha: com treino ativo aberto, "Iniciar" na Home pula o sheet e vai direto ao treino ativo (coordenadas de script quebram).

## Atualização 4 (exclusão de treino no Histórico, 20/10/2026)
- Histórico: **toque longo** num treino abre "Apagar treino?" (confirmação; avisa que as séries somem e os recordes podem mudar). Código: `HistoryViewModel.deleteWorkout`, `ThemedCard(onLongClick)`, `HistoryScreen`. Testes: `HistoryViewModelTest` (2). Validado no celular com o treino vazio de 0 min (apagou e a lista atualizou).
- Não validado: apagar um treino COM séries (depende de `workout_sets` ter ON DELETE CASCADE no Supabase; o esquema base não está em `docs/`). Se falhar, aparece "Erro ao apagar o treino" e a lista não muda.
- Sobraram no Histórico: "Push 1min" (o do recorde de 10 kg, útil para testes) e "Sem grupo 3h18min" (o antigo de teste, 2 séries 12×0). Há também 1 treino ativo vazio no grupo "teste".
- Durante os testes o celular exibiu pop-ups do SIM da operadora (TIM: oferta de plano / "Launch Browser?") por cima do app; foram recusados com Cancel.

## Atualização 5 (imagens por exercício, 20/10/2026)
- O catálogo tem **56** exercícios (a contagem "54" acima estava desatualizada).
- `shared/.../ExerciseCatalog.kt`: `CatalogExercise.imageKey` (`ex_<nome sem acento>`), `slug()`; teste `chavesDeImagemValidasEUnicas`.
- `ExerciseArt` (ExerciseCatalogSheet.kt) usa o drawable `ex_<nome>` se existir e senão o ícone do equipamento; usado no catálogo, na lista de exercícios e em Recordes. Validado no celular com um PNG de teste (já removido).
- `docs/IMAGENS_EXERCICIOS.md`: formato (PNG quadrado, fundo transparente, `res/drawable-nodpi/`) e tabela com as 56 chaves. O Thiago vai gerar as imagens (referências: silhueta roxa com o músculo em vermelho).
- `shared/build` deixou de ser rastreado pelo git; `.gitignore` ignora `build/` e `.kotlin/`.

## Atualização 6 (primeiras ilustrações, 20/10/2026)
- Folha 1 do Thiago (`docs/folhas/folha1.jpeg`, 30 desenhos 5×6) cortada com fundo transparente em 384×384 e colocada em `app/src/main/res/drawable-nodpi/` com a chave do exercício: **24 de 56** exercícios já têm imagem. Validado no catálogo no celular.
- Associação feita pela pose do desenho. Ficaram de fora 6 desenhos ambíguos (r2c3 elevação inclinada, r2c5 barra no peito, r4c5 remada no banco inclinado, r5c2 extensão lombar, r5c4 sumô, r1c4 flexão diamante): `docs/folhas/folha1.jpeg` guarda a folha para revisão.
- Folha 2 do Thiago (WhatsApp 12.35.54 em Downloads) ainda NÃO usada; a pedido dele ficou para depois.
- Script de corte (flood fill do fundo + bolsões + agrupamento de peças): estava no scratchpad da sessão e não foi versionado; se precisar de novo, refazer ou pedir.

## Atualização 7 (folha 2, 20/10/2026)
- Folha 2 (`docs/folhas/folha2.jpeg`, estilo mais simples, 30 desenhos) cortada e associada pela pose: +17 exercícios. Agora **41 de 56** têm imagem (supinos, crucifixo, agachamento livre, leg press, extensora, mesa flexora, terra, terra romeno, puxada, remada baixa, remada curvada, desenvolvimento com barra, rosca direta, prancha).
- Correção: o desenho sentado da folha 1 (r6c2) era a **cadeira flexora**, não a mesa flexora; renomeado. A mesa flexora (deitada) vem da folha 2.
- Duas folhas têm estilos diferentes (a 1 é mais realista/sombreada, a 2 é mais plana). Vão aparecer misturados no catálogo até gerar tudo no mesmo estilo.
- NÃO conferido no celular: o aparelho ficou sem rede (dados móveis caíram) e o app abre vazio sem internet. Conferir o catálogo (Grupos > um grupo > +) quando a rede voltar.
- Sem imagem ainda: abdominal na polia, agachamento no Smith, cadeira abdutora, desenvolvimento na máquina, elevação pélvica, glúteo na polia, hack squat, panturrilha sentado e no leg press, rosca martelo, rosca na polia, supino fechado, supino na máquina, tríceps testa, remada na máquina (confira `docs/IMAGENS_EXERCICIOS.md` contra a pasta).

## Atualização 8 (Social com a conta v.oliveira042997, 20/10/2026)
- O Thiago logou a 2ª conta no celular (eu não digito senha: regra de segurança, vale para contas de teste em projeto Supabase real). Conta "Deus" (v.oliveira042997) agora: perfil **público**, compartilhar treinos e músicas **ligados**, 1 treino concluído (Ombro/Peito/Tríceps, 12×8 = 96 kg), grupo **"Time Teste"** (privado, dono, código de convite `e3aab77a`), desafio **"Desafio Teste"** (treinos feitos, 30 dias, mín. 1 min, aberto, "Participando").
- **Bug achado e corrigido:** criar grupo falhava com "Você não tem permissão" (RLS). Causa: `insert(...) { select() }` = INSERT ... RETURNING, e a política de leitura (`groups_select`/`can_view_challenge`) só enxerga a linha depois do trigger que põe o dono como membro/participante. Correção no cliente (sem mexer no banco): o app gera o `id` (Uuid), insere sem RETURNING e depois busca a linha (`SocialRepositoryImpl.createGroup/createChallenge`, `NewGroup/NewChallenge.id`). Validado no celular para grupo e desafio.
- O post do treino apareceu no feed sozinho (trigger do banco ao finalizar). Corrigido "1 séries" → "1 série" (`formatFeedStats`, teste novo).
- Falta (precisa trocar para a conta thiagobr): seguir a Deus, ver o treino dela no feed, reagir, entrar no grupo pelo código `e3aab77a`, entrar no desafio e conferir o placar, perfil privado + pedido de seguir/aceitar, editar post.
- Dados de teste ficaram nas duas contas (grupo, desafio, post, treinos curtos).
