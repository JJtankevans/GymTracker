# Erros conhecidos e como corrigir (revisão de código de 27/10/2026)

Revisão feita lendo todo o `:shared`, as telas principais do `:app` e os SQL de `docs/supabase/`. **Nada aqui foi reproduzido no aparelho** (a nuvem não compila nem roda o app); a coluna "Certeza" diz como cada item foi verificado:
- **confirmado no código**: o caminho do erro está explícito no código lido;
- **confirmado no Postgres local**: reproduzido no Postgres 16 do contêiner;
- **provável / depende**: depende de comportamento de biblioteca ou de configuração do Supabase que não deu para conferir daqui.

Quando um item for corrigido, mova-o para a seção "Corrigidos" no fim, com a data e o commit.

---

## Alta prioridade (mistura/perda de dados ou o app fecha)

Os quatro itens desta seção (1 a 4) foram corrigidos em 27/10/2026; ver "Corrigidos" no fim.

---

## Média prioridade

### 5. CSV de exportação falha se a migração 009 não foi rodada
- **Certeza:** confirmado no código (PostgREST devolve erro de coluna inexistente).
- **Onde:** `shared/.../data/repository/WorkoutRepositoryImpl.kt` (`getAllSetsForExport` seleciona `workouts.muscle_group_name`).
- **O que acontece:** a documentação dizia que as melhorias de 26/10 não precisavam de migração nova, mas a exportação lê a coluna criada pela 009. Sem ela: "Erro ao exportar os treinos".
- **Como corrigir:** rodar a `009` (já é o próximo passo); ou tirar a coluna do `select` quando ela não existir (tentar de novo sem ela).

### 6. Cadastro com confirmação de e-mail ligada entra na Home sem sessão
- **Certeza:** depende da configuração do Supabase (Authentication > Sign In / Providers > Email > "Confirm email").
- **Onde:** `shared/.../presentation/viewmodel/AuthViewModel.kt:55`.
- **O que acontece:** depois de `signUp` o ViewModel marca `isLoggedIn = true` sem conferir se veio sessão. Com a confirmação ligada não vem sessão, a Home abre e tudo falha com "Sua sessão expirou".
- **Como corrigir:** depois de `signUp`, usar `authRepository.isLoggedIn()`; se for `false`, mostrar "Enviamos um e-mail de confirmação" e voltar para o Login.

### 7. "Sair da conta" sem internet pode fechar o app
Corrigido junto com o nº 1 em 27/10/2026; ver "Corrigidos".

### 8. Toque duplo em "Registrar série" grava a série duas vezes
- **Certeza:** confirmado no código.
- **Onde:** `WorkoutViewModel.addSet` (`:199`; o número da série é calculado antes do INSERT voltar) e o botão em `ActiveWorkoutScreen.kt` / `FocusWorkout.kt` (sem bloqueio enquanto grava).
- **Como corrigir:** um conjunto `savingExerciseIds` no estado; ignorar `addSet` do mesmo exercício enquanto houver gravação pendente e desabilitar o botão nesse tempo.

### 9. Circuito começa com carga 0 (ou a sugestão) em vez da última carga
- **Certeza:** confirmado no código.
- **Onde:** `app/.../ui/circuit/CircuitScreen.kt:152` (`remember(exercises)`).
- **O que acontece:** os "baselines" (última vez/recorde) chegam depois que a tela de preparação já montou as linhas, e as linhas não são refeitas.
- **Como corrigir:** quando `lastWeights` mudar, preencher o peso das linhas que o usuário ainda não mexeu (ex.: `LaunchedEffect(lastWeights)` atualizando só as linhas com `kg == 0.0`), ou esperar os baselines antes de mostrar a preparação.

### 10. Datas do servidor em UTC: treino depois das 21h (Brasília) conta no dia seguinte
- **Certeza:** confirmado no Postgres local (`'2026-10-27 22:30-03'::timestamptz::date` = `2026-10-28` com fuso UTC, que é o do Supabase).
- **Onde:** `vw_exercise_progression` (`started_at::DATE`, `app/docs/supabaseSchema.md`), `records_summary()` (`docs/supabase/008_records_summary.sql:30,36`) e `profile_stats()` (`active_days`, `002_social.sql`).
- **O que acontece:** gráfico de progressão, data do recorde, "recordes recentes", sessões do exercício e "dias ativos" do perfil usam o dia em UTC. Dois treinos em dias diferentes podem virar a mesma "sessão" e vice-versa. (Os desafios já usam `c.tz` e estão certos.)
- **Como corrigir:** migração nova (012) trocando `w.started_at::date` por `(w.started_at at time zone 'America/Sao_Paulo')::date` nas três (o ideal é um `profiles.tz`). A view precisa de `create or replace view ... with (security_invoker = true)` mantendo as mesmas colunas.

### 11. Voltar na Home depois de terminar um treino abre outra Home
- **Certeza:** confirmado no código.
- **Onde:** `app/.../ui/Navigation.kt:235` e `:253` (`navigate(Home) { popUpTo(ActiveWorkout/Circuit) { inclusive = true } }` deixa a Home antiga embaixo).
- **Como corrigir:** `navController.popBackStack(Screen.Home.route, inclusive = false)` (volta para a Home que já existe, que recarrega o painel) ou `navigate(Home) { popUpTo(Screen.Home.route) { inclusive = true } }`.

### 12. "Remover" um exercício dentro de um grupo apaga ele de todos os grupos e todo o histórico
- **Certeza:** confirmado no código (o diálogo avisa do histórico, mas não que sai de todos os grupos).
- **Onde:** `app/.../ui/exercise/ExerciseScreen.kt:239`; `workout_sets.exercise_id` é `ON DELETE CASCADE`.
- **Como corrigir:** na lista de um grupo, oferecer "Tirar deste grupo" (atualiza `groupIds` sem o grupo, mantendo o histórico) e deixar "Apagar exercício" só na lista "Todos", com o aviso atual.

### 13. Recorde marcado por engano não é desfeito
- **Certeza:** confirmado no código.
- **Onde:** `WorkoutViewModel.updateSet` (`:250`) e `deleteSet` (`:269`).
- **O que acontece:** se o usuário digita 200 kg por engano, ganha o selo e o confete; ao corrigir para 20 kg o selo continua, o resumo diz "novo recorde" e o recorde da sessão fica em 200 (recordes reais depois disso não são detectados). Editar uma série para uma carga maior também não detecta recorde.
- **Como corrigir:** guardar o recorde original (antes do treino) separado e recalcular `newRecordExerciseIds` a partir de `completedSets` depois de cada adicionar/editar/apagar.

---

## Baixa prioridade

| # | Problema | Onde | Como corrigir |
|---|----------|------|---------------|
| 14 | Exportação paginada ordena só por `completed_at`; empates podem repetir ou pular linhas entre páginas | `WorkoutRepositoryImpl.kt:144` | acrescentar `order("id", Order.ASCENDING)` como desempate |
| 15 | Peso na nuvem é "apaga e insere" (não atômico): se o INSERT falhar, o peso do dia some da nuvem até a próxima sincronização | `BodyCloudImpl.kt:56` | `upsert(..., onConflict = "profile_id,measured_on")` |
| 16 | Listas sem paginação: com mais de 1000 treinos (limite padrão do PostgREST) Histórico, Home e Conquistas calculam com dados cortados | `WorkoutRepositoryImpl.getHistory` (`:79`) | paginar como no CSV ou agregar no servidor |
| 17 | Slider da meta semanal usa `toInt()` (trunca 2,9999 para 2) | `ProfileScreen.kt:238` | `roundToInt()` |
| 18 | Reação com toque duplo dá "Já existe um item com esse nome" (o comentário diz que a atualização é otimista, mas não é) | `SocialViewModel.toggleReaction` (`:226`) | atualizar a lista na hora e ignorar `23505`, ou bloquear enquanto grava |
| 19 | Conquista "Recordes" conta todo exercício com carga > 0, não recordes de fato | `AchievementsViewModel.kt:50` | contar só linhas com `previousBestKg != null` (bateu a própria marca) ou renomear a medalha |
| 20 | Retomar um treino de outro grupo: título e texto de compartilhar usam o grupo escolhido agora, não o do treino retomado | `ActiveWorkoutScreen.kt` (`muscleGroupName` vem da rota) | resolver o nome pelo `activeWorkout.muscleGroupId` |
| 21 | Diálogo "Treino em andamento" não tem como cancelar (voltar não fecha) | `ActiveWorkoutScreen.kt:369`, `CircuitScreen.kt` (`RecoverDialog`) | `onDismissRequest = onBack` e um botão "Voltar" |
| 22 | Comparação de nomes ignora só alguns acentos (`è`, `ü`, `ñ`, `î`... não são tratados), diferente de `slug()` | `ExerciseCatalog.kt:173` | usar a mesma tabela do `slug()` |
| 23 | Duas classes `WorkoutSummary` (fim do treino e detalhe do treino) em pacotes diferentes | `presentation/viewmodel/WorkoutViewModel.kt:29`, `presentation/workout/WorkoutSummary.kt:12` | renomear a do fim do treino (ex.: `FinishedWorkoutSummary`) |
| 24 | Anti-trapaça: o dono pode alterar `started_at`/`finished_at` dos próprios treinos pela API (RLS libera tudo ao dono), o que mexe em placar e títulos de desafio | RLS de `workouts` | gatilho BEFORE UPDATE que impede mudar `started_at` e força `finished_at = now()` |
| 25 | `android:allowBackup="true"` leva o token de sessão e os dados locais para o backup do Google | `AndroidManifest.xml:14` | `allowBackup="false"` ou excluir `shared_prefs` nas regras de backup |

## Configuração e limpeza do repositório

| # | Problema | Como corrigir |
|---|----------|---------------|
| 26 | A pasta `.kotlin/` (317 arquivos, ~6 MB de cache do compilador) está versionada, apesar do `.gitignore` | `git rm -r --cached .kotlin` e commitar |
| 27 | Arquivo solto de outra ferramenta: `.artifacts/1c22306b-.../implementation_plan.artifact.md` (plano antigo de "ícone de voltar") | apagar se não for mais útil |
| 28 | `kotlinx-datetime = "0.6.1"` no catálogo, mas o código usa a API da 0.7 (`kotlin.time.Instant`, `kotlin.time.Clock.todayIn`); só compila porque outra dependência puxa uma versão mais nova | declarar a versão que de fato é usada (0.7.x) |
| 29 | Repositório `maven.pkg.jetbrains.space/.../compose/dev` e plugin `compose-multiplatform` declarados mas não usados | remover de `settings.gradle.kts` e `build.gradle.kts` |
| 30 | Alvos iOS configurados no `:shared` sem nenhum código iOS | manter só se houver plano para iOS (deixam o Gradle mais lento) |

---

## Corrigidos

Corrigidos em 27/10/2026, branch `claude/correcoes-erros-1-4`. Compilados e testados no CI (testes novos no `:shared`); **ainda não vistos no aparelho**.

### 1. Dados corporais de uma conta iam parar em outra no mesmo celular
- **Era:** peso, perfil corporal e medidas ficavam no aparelho sem separar por conta, nada era limpo ao sair, e a sincronização enviava esses dados para a próxima conta que entrasse.
- **Correção:** interface `domain/repository/LocalUserData` (`flush()` e `clear()`), implementada no app por `ui/LocalUserDataImpl.kt`. `ProfileViewModel.signOut` agora: (1) envia o peso/perfil corporal pendente para a nuvem enquanto a sessão vale (até 5 s; sem internet segue em frente); (2) encerra a sessão; (3) apaga do aparelho peso, perfil corporal e medidas (`BodyRepository.clear`), agenda (`ScheduleRepository.clear`), favoritos e recentes do catálogo (`CatalogPrefs.clear`), ritmo aprendido do circuito, músicas anotadas e não enviadas (`WorkoutTrackRecorder.discard`) e o CSV exportado no cache. Tema e preferências do aparelho ficam. `BodyRepository` ganhou uma "geração": uma sincronização da conta anterior que ainda estava em andamento para de gravar depois da troca. O diálogo "Sair da conta" avisa o que é apagado.
- **Atenção:** as **medidas corporais** ainda não têm tabela na nuvem, então se perdem ao sair da conta. Os dados já misturados antes desta correção continuam no banco: conferir com `select profile_id, count(*), min(measured_on), max(measured_on) from body_weights group by 1;`.
- **Testes:** `BodySyncTest` (limpar e próxima conta só vê os dela; sincronização interrompida pela troca não envia nada), `WeeklyScheduleTest` (limpar a agenda), `ProfileViewModelTest` (ordem enviar → sair → apagar; sem internet sai e apaga mesmo assim; toque duplo não repete).

### 2. Nome de grupo com `/` fechava o app ao iniciar o treino
- **Correção:** em `ui/Navigation.kt`, todo texto livre na rota passa por `routeArg()` (`Uri.encode`, com `"null"` para valor ausente) e é lido por `routeText()`, já decodificado pelo Navigation; os `URLEncoder`/`URLDecoder` saíram. Vale para treino ativo, circuito, exercícios do grupo, detalhe do exercício e detalhe do treino. O treino livre continua aparecendo como "Treino Livre".
- **Sem teste automático** (navegação só roda no aparelho). Testar: grupo "Peito/Tríceps", exercício "Rosca 21 + 21", exercício "Supino 100%".

### 3. Circuito: séries acima do limite sumiam sem aviso
- **Correção (limite mantido, como pedido):** o limite continua "séries do plano + 2" (`maxSetsFor`, `EXTRA_SETS_ALLOWED` em `WorkoutViewModel.kt`) e a mensagem agora diz o exercício e o limite: "Você chegou ao limite de 5 séries em Supino (3 do plano + 2 extras)." (`setLimitMessage`). O circuito ganhou Snackbar para os avisos do treino, e na preparação o "+" das séries para no limite e mostra a mesma mensagem.
- **Teste:** `WorkoutViewModelTest.respeitaLimiteDeSeries` confere o texto.

### 4. Circuito: a última série podia ficar fora do resumo e do post no feed
- **Correção:** `WorkoutViewModel` guarda as gravações de série em andamento (`pendingSaves`) e `finishWorkout` espera todas (`joinAll`) antes de fechar o treino no banco, então o resumo e o gatilho do feed já veem a última série.
- **Teste:** `WorkoutViewModelTest.finalizarEsperaASerieQueAindaEstaGravando`.

### 7. "Sair da conta" sem internet podia fechar o app
- **Correção:** `ProfileViewModel.signOut` trata as falhas de cada etapa, e `AuthRepositoryImpl.signOut` apaga a sessão salva no aparelho (`clearSession`) quando o pedido de logout ao servidor falha; assim a tela de abertura não entra de novo na conta.
