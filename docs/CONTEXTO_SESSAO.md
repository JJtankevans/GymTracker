# GymTracker — contexto para retomar (atualizado em 20/10/2026, fim do dia)

Leia isto primeiro. O histórico longo de decisões está em `docs/PROXIMOS_PASSOS.md`; referências de design em `docs/REFERENCIAS_DESIGN.md`.

## O app em 5 linhas
Android (Jetpack Compose) + módulo KMP `:shared` (regras, ViewModels, repositórios) + `:wear` (relógio, ainda não sincroniza). Backend: Supabase (projeto `ygbrrypyydxnuskrobeu`). Login por e-mail/senha. Dono: Thiago (`thiagobrito74@gmail.com`). Repo: `github.com/JJtankevans/GymTracker`, branch `master`.

## Estado atual (compila, testes do `:shared` passam)
**Testado no celular (Xiaomi, adb) e funcionando:** login/cadastro, Home, treino ativo, seletor de temas (21 temas, 5 famílias de fonte), catálogo de exercícios (56) com seleção múltipla e favoritos, treinos prontos (Push/Pull/Pernas...), Recordes com dado real, Circuito (anel, descanso, botões), exclusão de treino no Histórico (toque longo), ilustrações por exercício (41 de 56), Social com duas contas: perfil público, seguir, feed, reações (ícones desenhados), grupo por código, desafio e placar, editar post, privacidade, pedido de seguir em perfil "Só seguidores".

**Só validado por teste unitário (nunca visto no aparelho):** "Descartar e sair" ao encerrar o Circuito sem série (`WorkoutViewModel.discardActiveWorkout`).

**Ainda NÃO testado no celular:**
- Aceitar o pedido de seguir (logar na v.oliveira, Pessoas > pedidos) e depois ver, na thiagobr, perfil/posts/números da Deus.
- Desafio em equipes, privacidade de música, sair de grupo/desafio, tela de erro "Tentar de novo" (Recordes e Social).
- Apagar um treino que tem séries (depende de `workout_sets` ter ON DELETE CASCADE no banco; o esquema base não está em `docs/`).

## Números
- Catálogo: 56 exercícios (`shared/.../data/catalog/ExerciseCatalog.kt`), 9 treinos prontos (`WorkoutTemplates.kt`).
- Imagens: 41 de 56 em `app/src/main/res/drawable-nodpi/ex_<nome>.png` (formato e lista de chaves em `docs/IMAGENS_EXERCICIOS.md`; folhas originais em `docs/folhas/`). Sem imagem (caem no ícone do equipamento): abdominal na polia, agachamento no Smith, cadeira abdutora, desenvolvimento na máquina, elevação pélvica, glúteo na polia, hack squat, panturrilha sentado, panturrilha no leg press, rosca martelo, rosca na polia, supino fechado, supino na máquina, remada na máquina, tríceps testa. As duas folhas têm estilos diferentes (1 realista/sombreada, 2 plana).
- Testes: `./gradlew.bat :shared:testDebugUnitTest`; galerias de captura do app: `./gradlew.bat :app:testDebugUnitTest -Proborazzi.test.record=true`.

## Supabase
- Tabelas e funções existem (base + social + peso corporal). `APLICAR_TUDO.sql` já rodado; **não rodar de novo**. Migração **005** (`docs/supabase/005_perfis_so_seguidores_descobriveis.sql`) também já rodada pelo Thiago: perfis "Só seguidores" aparecem na busca para poder pedir para seguir; bio/números/posts continuam escondidos até aceitar.
- O conector Supabase da sessão não enxerga esse projeto; só dá para sondar por REST com a chave publicável. Não dá para conferir RLS por aqui (ver Próximos passos).
- `local.properties` (fora do git) precisa de `SUPABASE_URL` e `SUPABASE_KEY` (chave `sb_publishable_...`).
- **Bug de RLS já corrigido no cliente:** `insert(...) { select() }` (INSERT ... RETURNING) falhava com "Você não tem permissão" em grupos e desafios, porque a política de leitura só enxerga a linha depois do trigger que põe o dono como membro. O app agora gera o `id` (Uuid), insere e depois busca (`SocialRepositoryImpl.createGroup/createChallenge`). Vale para qualquer tabela nova com política parecida.

## Contas de teste (no celular real, banco real)
- `thiagobr` (usuário @AqueleQueTrazALuz, aparece como "Você") e `v.oliveira042997` (@Deus). **Eu (Claude) não digito senha no app** (regra de segurança, vale mesmo para contas de teste em projeto Supabase real); o Thiago troca a conta no celular (Perfil > Sair da conta). As senhas NÃO ficam neste arquivo.
- Estado atual do Social: a Deus está em "Só seguidores", com "compartilhar treinos/músicas" ligados, dona do grupo privado "Time Teste" (código `e3aab77a`) e do "Desafio Teste" (treinos feitos, 30 dias, mín. 1 min, aberto). A thiagobr tem um **pedido de seguir pendente** para a Deus, está no grupo e no desafio (1º no placar com 1 treino).
- Dados de teste ficaram nas duas contas: posts, treinos curtos, grupo, desafio. No Histórico da conta Thiago há "Push 1min" (recorde 10 kg, útil para testes) e "Sem grupo 3h18min" (2 séries 12×0); um treino ativo vazio no grupo "teste" pode reaparecer como "Treino em andamento" (escolher "Iniciar Novo" descarta).

## Como trabalhar neste PC
- Windows, sem Python (usar sed/awk/PowerShell). Gradle: `./gradlew.bat :app:assembleDebug`. APK: `app/build/outputs/apk/debug/app-debug.apk`.
- adb: `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`, pacote `com.vitoroliveira.gymtracker`, `adb install -r <apk>`. Se aparecer "offline"/"no devices": `adb kill-server`, reconectar o cabo, reativar a depuração USB.
- Toques por adb: tela 1080x2400; as capturas vêm reduzidas (fator 1,2: coordenada da imagem × 1,2 = toque real). Cuidado: toques na faixa de cima podem puxar a aba de notificações, que mostra WhatsApp pessoal do Thiago; não interagir. Pop-ups da operadora (TIM / "Launch Browser?") já apareceram; recusar com Cancel.
- Trocar tema sem UI: `adb shell run-as <pacote> sed -i 's/name="theme_id">[a-z]*</name="theme_id">NOVO</' shared_prefs/gymtracker_prefs.xml`. O tema do Thiago é `magma`, intensidade "Insano".
- `build/` e `.kotlin/` estão no `.gitignore` (antes `shared/build` era rastreado).

## Armadilhas já encontradas
- `import X as Y` esconde o nome original no arquivo → usar `typealias`.
- Robolectric trava (`AppNotIdleException`) com `AlertDialog`; `ModalBottomSheet` funciona.
- ViewModel: iniciar `loadX()` depois de setar erro apaga o erro (recarga assíncrona). Usar `loadExercises(clearError = false)`.
- Nome de exercício é único por usuário e 1 exercício pertence a 1 grupo → treino pronto pula exercícios que já existem e desfaz o grupo se falhar (`CreateWorkoutFromTemplate`).
- Catálogo marca "já adicionado" olhando todos os exercícios do usuário (`ExerciseUiState.allNames`), não só os do grupo.
- Layout: três `FilterChip` em `Row` quebram letra por letra; usar `FlowRow`. Números em `Row(SpaceBetween)` colam; usar `weight(1f)`.
- Playfair precisa de `lnum`; fonte variável = um arquivo com pesos por eixo (`ThemeFonts.variable`).
- Git Bash `sed` não lida com acentos (`y///`); para normalizar acentos use PowerShell. PowerShell lê scripts com acento mal sem BOM: usar `[char]0xED` etc.
- Com treino ativo aberto, "Iniciar" na Home pula o sheet e vai direto ao treino (scripts por coordenada quebram).

## Próximos passos (ordem sugerida)
1. Terminar o Social: aceitar o pedido (logar na v.oliveira), conferir perfil na thiagobr; equipes, música, sair do grupo/desafio, "Tentar de novo".
2. Imagens dos exercícios: o Thiago gera as 15 que faltam e, de preferência, regerar tudo no mesmo estilo; eu corto/encaixo (o script de corte era temporário e não foi versionado: flood fill do fundo, tapa bolsões, agrupa peças por vizinhança, centraliza em 384×384).
3. Conferir RLS no painel do Supabase (Authentication/Policies) e o alerta "Security Definer View". Considerar um Supabase local (Docker, `127.0.0.1`) para eu poder logar sozinho em testes automáticos.
4. Produto pendente: notificações (pedido de seguir, desafio), foto de perfil, ligas semanais, aviso do descanso com o app em segundo plano, sincronizar relógio, sincronizar dados corporais com a nuvem, "versão insana" própria por tema, mais exercícios (cardio, mobilidade), detalhe de treino no Histórico, trocar o "joinha" por um ícone de palmas.
5. Limpeza: dados de teste das contas; migrar `kotlinx.datetime.Instant` deprecado no `HistoryViewModel`.
6. Segurança: as senhas das contas de teste apareceram em conversa; trocar antes de ter usuários reais.

## Mapa rápido do código
- Catálogo/treinos prontos: `shared/.../data/catalog/`, UI `app/.../ui/exercise/ExerciseCatalogSheet.kt` (inclui `ExerciseArt`), `ui/muscleGroup/TemplateSheet.kt`.
- Recordes: `shared/.../presentation/records/`, `app/.../ui/records/RecordsScreen.kt`.
- Social: modelos `data/model/Social.kt`, repositório `data/repository/SocialRepositoryImpl.kt`, regras `presentation/social/SocialRules.kt`, `SocialViewModel.kt`, telas `app/.../ui/social/SocialScreen.kt` e `SocialDialogs.kt`.
- Histórico: `presentation/viewmodel/HistoryViewModel.kt`, `ui/history/HistoryScreen.kt` (toque longo apaga; `ThemedCard(onLongClick)`).
- Circuito: `ui/circuit/CircuitScreen.kt`. Treino ativo: `WorkoutViewModel`.
- Temas: `ui/theme/Themes.kt`, `ThemeDecor.kt`, `ThemeFonts.kt`, `ThemeBuilders.kt`. Navegação: `ui/Navigation.kt`; barra da Home: `ui/home/HomeScreen.kt`.
- SQL: `docs/supabase/` (`APLICAR_TUDO.sql` e `005_...` aplicados).

## Preferências do Thiago
Quer autonomia ("yolo, sem perguntas"), temas variados e profissionais (contidos e impactantes), referências de design levadas a sério, e relatório honesto do que não foi testado no aparelho. Gosta de ícones/desenhos em vez de emojis. Baixar arquivos externos ele autorizou uma vez; pedir de novo para outros. Não apagar dados dele. Mensagens em português do Brasil, diretas.
