# GymTracker — o que foi feito e o que falta

> Registro **histórico**, rodada por rodada. O estado atual e os próximos passos estão em `docs/CONTEXTO_SESSAO.md`. Itens marcados *(superado)* já foram resolvidos depois.

## Feito (sessão noturna)
- **Sistema de temas**: 14 temas *(hoje 21, ver Sexta rodada)* (Profissional: Sistema/Material You, Studio, Carbon (padrão), Oceano, Tinta · Ousados: Papel, Blueprint, Brutal, Neon, Terminal · Abstratos: Aurora, Synthwave, Confete, Magma). Fundos animados em Canvas, tela "Aparência" com preview ao vivo, escolha salva localmente. Para esconder um tema: `enabled = false` em `Themes.kt`.
- **Todas as telas** usam `ThemedScaffold/ThemedTopAppBar/ThemedCard`.
- **Sessão**: Splash restaura login (antes sempre abria no Login).
- **Erros amigáveis** em pt-BR (`toUserMessage`) em todos os ViewModels.
- **Treino ativo** reescrito: pré-preenche última carga/reps, recorde (PR) com badge, editar/apagar série, timer de descanso por prazo (sem drift) com vibração+beep, tela sempre ligada, resumo final (duração, volume, recordes).
- **Gráficos próprios** (sem Vico): linha com gradiente e seleção por toque (progressão), barras com meta (frequência 8 semanas na Home).
- `EmptyState` reutilizável; `DashboardCalculator` puro e testado.
- **Testes**: 40 no `:shared` (`./gradlew :shared:testDebugUnitTest`); galerias de screenshot (Robolectric+Roborazzi) em `app/src/test/.../ui/` → `app/build/outputs/{theme,screen}-gallery`.
- `docs/REFERENCIAS_DESIGN.md` (sites de inspiração) e `docs/supabase/001_lacunas_do_schema.sql`.

## Adicionado depois
- **Layouts de Home por tema** (`HomeLayout` em `ThemeSpec.kt`, telas em `ui/home/HomeLayouts.kt`): Clássico, Bento, Pôster, Terminal, Orbit, Editorial. O tema escolhe em `extras.homeLayout`. Falta estender a mesma ideia ao treino ativo, histórico e perfil.
- **Música** (`ui/music/`): `MusicController` lê a sessão de mídia ativa (exige "acesso a notificações", ligado em Aparência → Cores da música); `MiniPlayer` na Home e no treino; a cor da capa troca a cor primária e a decoração dos temas animados. Não existe visualizador de batida real (Android não oferece de forma confiável), a animação segue a capa e o play/pause.
- **Relógio** (`:wear`, Compose for Wear OS, standalone): descanso 60/90/120 s com vibração, registrar série com coroa giratória, resumo e 4 cores. Instalar: `adb connect IP:PORTA` (Depuração sem fio no relógio) e `adb install wear-debug.apk`.
  - Ainda NÃO sincroniza com o celular: os dados do relógio ficam só nele. Próximo passo: Data Layer (play-services-wearable) para o relógio mandar as séries pro treino ativo do celular.

## Terceira rodada (circuito, social, layouts)
- **Modo Circuito (beta)** — `presentation/circuit/` (motor puro, 17 testes) + `ui/circuit/`. Liga em Iniciar Treino → "Modo circuito". Aprende segundos/repetição por exercício (mediana das últimas 10, descarta absurdos), entra no descanso sozinho, "AINDA NÃO TERMINEI" soma o tempo e reinicia o descanso, voz (TTS pt-BR) + vibração. Ideias ainda não feitas: notificação fixa com botão "Terminei" (espelha no relógio), botão do fone/coroa do relógio como toque único, ajuste de descanso por esforço (fácil/ok/difícil), progressão automática de carga.
- **Referências de tempo**: `docs/REFERENCIAS_TEMPOS.md` e `shared/.../presentation/circuit/ReferenceTimes.kt`.
- **Social**: `docs/SOCIAL_DESIGN.md`, `docs/supabase/002_social.sql` e `test_social.sql` (testados num Postgres 16 local). *(Superado na Sétima rodada: o lado do app existe.)* Antes faltava todo o lado do app (telas e repositórios de grupos/desafios/feed/perfil). Já existe a gravação das músicas do treino (`ui/music/WorkoutTrackRecorder.kt`, desligada por padrão).
- **Layout do treino ativo por tema**: `WorkoutLayout.FOCUS` (um exercício por vez, números gigantes, registrar já inicia o descanso) em Oceano, Tinta, Brutal, Neon, Aurora, Synthwave, Magma. Histórico, perfil, grupos e exercícios ainda têm a mesma estrutura em todos os temas.
- **Segurança (achado)**: as views do schema original rodavam sem `security_invoker` e ignoravam a RLS; `001_lacunas_do_schema.sql` corrige.

## Supabase — atenção *(superado: o projeto é `ygbrrypyydxnuskrobeu`; migrações 001–006 aplicadas; ver CONTEXTO_SESSAO)*
Nenhum projeto Supabase da sua conta se chama GymTracker, então **não alterei nada** em banco nenhum. Rode `docs/supabase/001_lacunas_do_schema.sql` no projeto certo: adiciona `workouts.finished_exercise_ids` e a view `vw_exercise_progression`, que o código usa mas `supabaseSchema.md` não tem.

## Falta / ideias
1. Rodar no aparelho/emulador (só validei por screenshots Robolectric e testes unitários). *(Superado: testado num Xiaomi via adb; ver CONTEXTO_SESSAO.)*
2. Testes de ViewModel para Home, History, Exercise, MuscleGroup, Profile, Auth.
3. Usar `EmptyState` também em Exercícios, Grupos, Home; criar `ErrorState` com "Tentar de novo".
4. Polimento de Login/Cadastro; fontes embarcadas por tema (Space Grotesk, JetBrains Mono…).
5. Notas por treino, repetir último treino, templates, lista de recordes, notificação do timer em segundo plano.
6. Picker de temas: teste de screenshot; mais temas.
7. Migrar `kotlinx.datetime.Instant` deprecado no HistoryViewModel (só warnings).
8. Build: Maven Central deu 429 aqui (usei espelho do Google via init script, não incluído no projeto).

## Quarta rodada — Intensidade
- Aparência > Intensidade (Calmo / Normal / Insano), vale para todos os temas.
- Insano: fundo 3x mais rápido, faixa de luz varrendo + vinheta pulsando, confete em cada série (Focus e Circuito), halter animado no treino, glitch RGB em Neon/Terminal, equalizador no mini player.
- Calmo: decoração mais discreta, confete mínimo, sem equalizador.
- Pendente: temas com "versão" própria mais absurda (ex.: chuva de código, fogo real no Magma), variação nas telas de histórico/perfil/grupos.

## Quinta rodada — dados corporais, sugestão de carga e evolução
Feito:
- Olhinho para mostrar/ocultar a senha (login e cadastro).
- "Meus dados" (altura, ano de nascimento, sexo; tudo opcional) e histórico de peso corporal, guardados no aparelho. Opção de perguntar o peso ao iniciar cada treino.
- Sugestão de carga ao registrar série (modos Focus, clássico e Circuito): com histórico usa dupla progressão (bateu a meta de reps → sobe ~2,5% arredondado ao degrau de 2,5 kg; muito abaixo → reduz ~5%; no meio → mantém). Sem histórico, só para os 4 básicos, parte de ~70% da carga de um Iniciante pelo peso corporal (mais conservador acima de 50 anos).
- Tela Evolução: peso corporal com faixa de IMC normal (OMS) e, para supino/agachamento/terra/desenvolvimento, 1RM estimado por sessão com linhas de nível (Iniciante→Elite) e quanto falta para o próximo.
- Referências: StrengthLog (percentis 5/25/50/75/95). Os múltiplos do peso corporal foram calculados para ~80 kg (homens) e ~60 kg (mulheres); mais pesado = múltiplo um pouco menor.
- SQL 003 (peso/altura/nascimento/sexo no Supabase, RLS só do dono) validado localmente. O app ainda não sincroniza isso com a nuvem.

Pendente / ideias:
- Sincronizar dados corporais com o Supabase (003) quando houver projeto.
- Sugestão considerando RPE/fadiga e deload automático a cada N semanas.
- Referências por faixa etária e por peso corporal exato (hoje fixas por sexo).
- Mais exercícios com padrão de força (remada, rosca, leg press) — precisam de fonte confiável.
- 1RM usa média de reps por dia (a view só guarda carga máxima e reps totais); melhorar com a melhor série real.

## Sexta rodada — catálogo, treinos prontos, temas e fontes
- **Catálogo de exercícios** (`data/catalog/ExerciseCatalog.kt`, 54 itens *na época; hoje 56*): nome padrão, músculo, equipamento, séries/reps padrão. O "+" de Exercícios abre o seletor (`ui/exercise/ExerciseCatalogSheet.kt`): busca sem acento, filtro por músculo, seleção múltipla, ícone desenhado por equipamento (sem imagens). 1 selecionado abre o formulário pra ajustar; 2+ adiciona direto. Quem escolhe do catálogo grava sempre o mesmo nome, então dá pra comparar entre treinos e pessoas. Não mexe no banco.
- **Treinos prontos** (`WorkoutTemplates.kt`, `CreateWorkoutFromTemplate`): Push, Pull, Pernas, Superior, Inferior, Full body e ABC. Em Grupos, "Treinos prontos" cria o grupo + exercícios. Nome repetido vira "Push 2".
- **Temas**: 21 (novos: Nórdico, Obsidiana, Cobalto, Floresta, Arena, Ouro, Ártico) + decorações STRIPES/DOTS/HALO.
- **Fontes embutidas** (`res/font`, Google Fonts, OFL): Inter, Space Grotesk, Playfair Display (com `lnum`, senão o 0 parece "o"), JetBrains Mono, Barlow Condensed. Famílias em `ThemeFonts.kt`.
- **Correções**: anel de progresso (`ProgressRing`) não reinicia mais a cada tick (parava em 0); preview do seletor de temas não corta o botão; Synthwave/Confete/Aurora com mais contraste.
- **Login** avisa quando o app foi gerado sem `SUPABASE_URL`/`SUPABASE_KEY` (antes só dizia "sem internet").
- Ideias: foto/ilustração por exercício, favoritos e "exercícios recentes" no catálogo, treino pronto escolhendo dias da semana, mais exercícios (cardio, mobilidade).

## Sétima rodada — recordes, favoritos e a camada social no app
- **Recordes pessoais** (`presentation/records/`, `ui/records/RecordsScreen.kt`, ícone de troféu na Home): melhor carga por exercício, dia em que bateu, 1RM estimado, quanto subiu sobre o recorde anterior e selo NOVO (30 dias). Exercícios sem carga (peso do corpo) não entram. Usa a view `vw_exercise_progression`.
- **Favoritos e recentes** no catálogo (`CatalogPrefs`, só no aparelho): estrela em cada card, filtros Favoritos/Recentes, favoritos aparecem primeiro.
- **Social** (`presentation/social/`, `ui/social/`, ícone de grupo na Home; Evolução e Aparência foram pro menu ⋮): 
  - **Feed** com reações (💪🔥👏🎧), **Grupos** (criar, público/convite, entrar por código, sair), **Desafios** (criar com 8 métricas e regras anti-trapaça, entrar/sair, placar via `challenge_standings`), **Pessoas** (buscar, seguir, aceitar/recusar pedidos, perfil com números e músicas mais tocadas) e **Privacidade** (bio, visibilidade, compartilhar treinos/músicas; tudo começa privado).
  - Cada seção carrega sozinha: se uma tabela falhar, as outras seguem. Regras de visibilidade ficam no banco (RLS); o app só consome.
  - `SocialContent` (visual) é separado do `SocialViewModel` por `SocialActions`, o que permite testar a tela sem rede.
- **Testes**: +47 no `:shared` (catálogo, treinos prontos, recordes, social; a suíte do `:shared` tinha 115 *na época; hoje 142*) e `SocialGalleryTest` (capturas em `app/build/outputs/social-gallery`). Os diálogos não têm teste de captura (o cursor piscando trava o Robolectric).
- **Não testado no aparelho ainda** *(superado: a maior parte já foi testada; o que falta está em CONTEXTO_SESSAO)* (o celular estava desconectado): telas Social, Recordes com dados reais, favoritos. Os diálogos (criar grupo/desafio, privacidade) só compilam; nunca foram abertos.
- Depois: **desafio em equipes** (formulário, escolha de equipe ao entrar, ranking por equipe), **barra de progresso até a meta**, **editar legenda/visibilidade** dos próprios posts e `ErrorState` com "Tentar de novo" (Recordes e Social). Revisão visual dos 21 temas com as fontes novas nas galerias (Obsidiana, Papel, Terminal, Floresta conferidos).
- **Bug achado por teste**: ao falhar no meio de `ExerciseViewModel.createMany`, a recarga da lista apagava a mensagem de erro; `loadExercises(clearError = false)` resolve.
- **Pendente**: notificações de desafio/pedido de seguir, upload de avatar, ligas semanais, notificação do descanso em segundo plano, testar tudo no aparelho.

## Supabase real (ygbrrypyydxnuskrobeu)
- Sondagem só leitura com a chave publicável: tabelas base (profiles, muscle_groups, exercises, workouts, workout_sets e as 2 views) existem e respondem vazio para anônimo; social e body_weights ainda NÃO existiam *(superado: `APLICAR_TUDO.sql` foi rodado)*.
- O conector do Supabase da sessão não enxerga esse projeto, então as migrations não foram aplicadas por mim. Rodar `docs/supabase/APLICAR_TUDO.sql` no SQL Editor (testado do zero num Postgres 16 local; transação única).
- Depois: conferir a RLS (anon não deve ler nada) e o cadastro (trigger handle_new_user).
- Alerta "Security Definer View" em public.public_profiles (painel do Supabase): a view era SECURITY DEFINER de propósito. Trocada por função `public_profiles()` (004; 002 já atualizado). No app: rpc("public_profiles").

## Oitava rodada (21/10/2026, sessão na nuvem) — exercício em vários grupos
- **Bug:** apagar um grupo deixava o exercício sem grupo (`ON DELETE SET NULL`), mas o nome seguia único por usuário; não dava para pôr o mesmo exercício (ex.: Cadeira extensora) num grupo novo. Reproduzido num Postgres local.
- **Mudança:** relação N:N em `group_exercises` (SQL `docs/supabase/006_exercicio_em_varios_grupos.sql`, testado local). `Exercise.groupIds`; `ExerciseRepositoryImpl` grava e sincroniza vínculos; `ExerciseRepository.addToGroups` (use case) reaproveita o exercício de mesmo nome; treino pronto reaproveita exercícios que o usuário já tem; diálogo de exercício com seleção múltipla de grupos; catálogo marca "já adicionado" só dentro do grupo filtrado.
- **Testes:** +7 em `CatalogViewModelsTest` (mesmo exercício em dois grupos, órfão reaproveitado, nome com caixa/acento, vários grupos de uma vez, repetir no mesmo grupo, escopo do catálogo, treino pronto reaproveitando) e fakes ajustados.
- **Validação:** o Thiago aplicou o 006 e testou no celular; funcionou. Os 7 testes novos não foram vistos rodando (o ambiente de nuvem não tem Android SDK); confirmar com `./gradlew.bat :shared:testDebugUnitTest`. Detalhes em `docs/CONTEXTO_SESSAO.md`.
- **Documentação** revisada nesta rodada: números conferidos com o código (56 exercícios, 9 treinos prontos, 21 temas, 41/56 imagens, 142 `@Test` no `:shared`); `SOCIAL_DESIGN.md`, `IMAGENS_EXERCICIOS.md`, `REFERENCIAS_TEMPOS.md` e `app/docs/` atualizados.

## Nona rodada (21/10/2026, 2ª sessão na nuvem) — foto de perfil e consulta de exercícios
- **Foto de perfil** no Supabase Storage (bucket público `user_profile_images`): `docs/supabase/007_foto_de_perfil_storage.sql` (bucket com limite de 2 MB e só imagem; RLS em `storage.objects` por pasta do usuário; testado local com stub do Storage). App: `AvatarRules`, `ProfileRepository.uploadAvatar/removeAvatar`, `ProfileViewModel` (+`isUpdatingAvatar`), `AvatarImageProcessor` (recorte quadrado 512 px, EXIF, JPEG q85), `UserAvatar` (cache em memória, sem biblioteca de imagem), tela de Perfil com Photo Picker e "Remover foto", avatar nas linhas de Pessoas do Social. Nome do arquivo novo a cada troca (evita cache) e limpeza das fotos antigas.
- **Pendente:** rodar o 007, compilar (nomes da API do `storage-kt` 3.6.0 não verificados: sem Android SDK aqui), rodar os 6 testes novos de `ProfileViewModelTest`, testar no celular; usar `UserAvatar` no feed, placar de desafios, perfil público e Home; recorte interativo; considerar Coil se o download manual ficar limitado.
- **Consulta** `docs/supabase/consulta_exercicios_de_todos_os_usuarios.sql` (só leitura): exercícios distintos de todos os usuários, marcando os que não estão no catálogo, para gerar as imagens que faltam.
- Branch `claude/instrucoes-sessao` reiniciado a partir do `master` depois do merge do PR #1.
