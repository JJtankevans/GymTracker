# Erros conhecidos e como corrigir (revisão de código de 27/10/2026)

Revisão feita lendo todo o `:shared`, as telas principais do `:app` e os SQL de `docs/supabase/`. **Nada aqui foi reproduzido no aparelho** (a nuvem não compila nem roda o app); a coluna "Certeza" diz como cada item foi verificado:
- **confirmado no código**: o caminho do erro está explícito no código lido;
- **confirmado no Postgres local**: reproduzido no Postgres 16 do contêiner;
- **provável / depende**: depende de comportamento de biblioteca ou de configuração do Supabase que não deu para conferir daqui.

Quando um item for corrigido, mova-o para a seção "Corrigidos" no fim, com a data e o commit.

---

## Alta prioridade (mistura/perda de dados ou o app fecha)

### 1. Dados corporais de uma conta vão parar em outra no mesmo celular
- **Certeza:** confirmado no código.
- **Onde:** `app/.../ui/body/PrefsBodyStore.kt` (um único arquivo `body_data` para o aparelho), `app/.../ui/UiModule.kt:29` (`BodyRepository` é `single`), `shared/.../presentation/body/BodyViewModel.kt:62-70` (`sync`).
- **O que acontece:** peso, altura, ano de nascimento, sexo e medidas ficam em SharedPreferences sem separar por usuário e nada é limpo no "Sair da conta". Ao entrar com outra conta, a sincronização envia para a conta nova **todos os pesos do aparelho** que não existem na nuvem dela (linha 69) e grava o perfil corporal local por cima do dela (linha 70). Como `synced` fica `true` no singleton, depois de trocar de conta sem fechar o app nem há nova sincronização, mas cada peso novo vai para a conta logada. As contas de teste `thiagobr` e `v.oliveira042997` foram usadas no mesmo celular, então vale conferir se já houve mistura: `select profile_id, count(*), min(measured_on), max(measured_on) from body_weights group by 1;`.
- **Como corrigir:** guardar os dados por usuário (chave `data_<userId>` no `PrefsBodyStore`, ou um arquivo por usuário) e trocar de "gaveta" quando o usuário muda; no `signOut` (`ProfileViewModel`/`AuthViewModel`) chamar um `BodyRepository.reset()` que zera `_data`, `synced` e o que estiver em memória. O mesmo vale, com menos impacto, para a agenda da semana (`PrefsScheduleStore`), os recentes/favoritos do catálogo e o tema.

### 2. Nome de grupo com `/` (ex.: "Peito/Tríceps") fecha o app ao iniciar o treino
- **Certeza:** confirmado no código (comportamento conhecido do Navigation Compose: rota sem destino lança `IllegalArgumentException`).
- **Onde:** `app/.../ui/Navigation.kt:62` e `:67` (`Circuit.createRoute` e `ActiveWorkout.createRoute` não codificam o nome) e `:169`, `:186`, `:273` (`URLDecoder.decode`).
- **O que acontece:** a rota vira `active_workout/<id>/Peito/Tríceps`, com uma barra a mais, e não casa com `active_workout/{muscleGroupId}/{muscleGroupName}` → o app fecha. `?` e `#` no nome cortam o nome. Nas outras rotas o nome passa por `URLEncoder`, mas o Navigation já decodifica o argumento, e o `URLDecoder.decode` da segunda decodificação lança exceção se o nome tiver `%` (ex.: "Supino 100%") e troca `+` por espaço.
- **Como corrigir:** passar só os ids na rota e buscar o nome no ViewModel (melhor), ou usar `android.net.Uri.encode(nome)` em **todas** as `createRoute` e remover os `URLDecoder.decode` (o Navigation entrega o texto já decodificado). Teste: grupo "Peito/Tríceps", exercício "Rosca 21 + 21", exercício "100%".

### 3. Circuito: séries acima de "séries padrão + 2" somem sem aviso
- **Certeza:** confirmado no código.
- **Onde:** `shared/.../presentation/viewmodel/WorkoutViewModel.kt:217` (limite `defaultSets + 2`) e `app/.../ui/circuit/CircuitScreen.kt:212` (o circuito deixa escolher até 10 séries).
- **O que acontece:** no circuito com, por exemplo, 6 séries de um exercício com padrão 3, a 6ª série é recusada por `addSet` com "Limite de séries atingido". A tela do circuito não tem Snackbar, então o usuário não vê o erro e a série não é gravada.
- **Como corrigir:** subir o limite (ex.: 20, ou só validar no máximo absoluto) ou aceitar um parâmetro `ignoreLimit` vindo do circuito; e mostrar `errorMessage` na `CircuitScreen` (Snackbar como no treino ativo).

### 4. Circuito: a última série pode ficar fora do resumo e do post no feed
- **Certeza:** confirmado no código (condição de corrida; depende da rede).
- **Onde:** `app/.../ui/circuit/CircuitScreen.kt:71-73`.
- **O que acontece:** o evento `SetCommitted` da última série chama `addSet` (uma coroutine com INSERT) e logo em seguida `Finished` chama `finishWorkout` (outra coroutine com UPDATE). Se o UPDATE terminar primeiro, o resumo é montado sem a última série, e o gatilho do banco (`trg_workout_to_post`) calcula as estatísticas do post sem ela.
- **Como corrigir:** no `WorkoutViewModel`, guardar os `Job`s de `addSet` pendentes e fazer `finishWorkout` esperar (`pending.joinAll()`) antes de chamar o repositório; ou serializar as gravações com um `Mutex`.

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
- **Certeza:** provável (depende de o `supabase.auth.signOut()` lançar exceção sem rede).
- **Onde:** `ProfileViewModel.signOut` e `AuthViewModel.signOut` (sem `try/catch` dentro de `viewModelScope.launch`; exceção não tratada em coroutine fecha o app no Android).
- **Como corrigir:** `try { authRepository.signOut() } catch (e: Exception) { if (e is CancellationException) throw e }` e seguir para o Login mesmo assim (a sessão local deve ser apagada).

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
(nenhum ainda)
