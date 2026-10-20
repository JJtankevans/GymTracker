# GymTracker — o que foi feito e o que falta

## Feito (sessão noturna)
- **Sistema de temas**: 14 temas (Profissional: Sistema/Material You, Studio, Carbon (padrão), Oceano, Tinta · Ousados: Papel, Blueprint, Brutal, Neon, Terminal · Abstratos: Aurora, Synthwave, Confete, Magma). Fundos animados em Canvas, tela "Aparência" com preview ao vivo, escolha salva localmente. Para esconder um tema: `enabled = false` em `Themes.kt`.
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
- **Referências de tempo**: `docs/REFERENCIAS_TEMPOS.md` e `ReferenceTimes.kt`.
- **Social**: `docs/SOCIAL_DESIGN.md`, `docs/supabase/002_social.sql` e `test_social.sql` (testados num Postgres 16 local). Falta TODO o lado do app (telas e repositórios de grupos/desafios/feed/perfil). Já existe a gravação das músicas do treino (`ui/music/WorkoutTrackRecorder.kt`, desligada por padrão).
- **Layout do treino ativo por tema**: `WorkoutLayout.FOCUS` (um exercício por vez, números gigantes, registrar já inicia o descanso) em Oceano, Tinta, Brutal, Neon, Aurora, Synthwave, Magma. Histórico, perfil, grupos e exercícios ainda têm a mesma estrutura em todos os temas.
- **Segurança (achado)**: as views do schema original rodavam sem `security_invoker` e ignoravam a RLS; `001_lacunas_do_schema.sql` corrige.

## Supabase — atenção
Nenhum projeto Supabase da sua conta se chama GymTracker, então **não alterei nada** em banco nenhum. Rode `docs/supabase/001_lacunas_do_schema.sql` no projeto certo: adiciona `workouts.finished_exercise_ids` e a view `vw_exercise_progression`, que o código usa mas `supabaseSchema.md` não tem.

## Falta / ideias
1. Rodar no aparelho/emulador (só validei por screenshots Robolectric e testes unitários).
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
