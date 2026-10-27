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

Os itens desta seção (5 a 13) foram resolvidos em 27/10/2026; ver "Corrigidos" no fim. O 10 depende de rodar a migração `012` no Supabase.

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

### Média prioridade (27/10/2026, branch `claude/correcoes-media-prioridade`)
Compilados e testados no CI (testes novos no `:shared`); **ainda não vistos no aparelho**. O nº 10 é SQL e precisa ser rodado no Supabase.

#### 5. CSV de exportação falhava sem a migração 009
- **Resolvido sem mudar código:** o Thiago rodou a `009` no Supabase em 27/10/2026.

#### 6. Cadastro com confirmação de e-mail ligada entrava na Home sem sessão
- **Correção:** depois do `signUp`, `AuthViewModel` confere `authRepository.isLoggedIn()`. Se não veio sessão ("Confirm email" ligado no Supabase), a tela de cadastro mostra o aviso "Confirme seu e-mail" com o endereço, "Ir para o login" e "Reenviar e-mail" (`AuthRepository.resendSignUpConfirmation` → `supabase.auth.resendEmail(OtpType.Email.SIGNUP, email)`). Quem tenta entrar antes de confirmar já recebia "Confirme seu e-mail antes de entrar.".
- **Não precisa de tela de validação no app:** quem confirma é o link do e-mail (o Supabase manda sozinho); depois o usuário entra com e-mail e senha. Como configurar o envio: seção "Confirmação de e-mail" em `CONTEXTO_SESSAO.md`.
- **Testes:** `AuthViewModelTest` (sem confirmação entra direto; com confirmação não entra e guarda o e-mail; reenviar; erro de limite de envio; fechar o aviso).

#### 8. Toque duplo em "Registrar série" gravava a série duas vezes
- **Correção:** `WorkoutUiState.savingExerciseIds`: enquanto a série de um exercício está sendo gravada, o botão "Registrar série" fica desligado (treino ativo e layout Focus) e um segundo `addSet` do mesmo exercício é ignorado. O circuito chama `addSet(..., queueIfSaving = true)` porque as séries dele chegam em sequência e nenhuma pode se perder; as gravações passam por um `Mutex` e o número da série é calculado já com as anteriores salvas. O limite de séries conta também as que estão sendo gravadas.
- **Testes:** `WorkoutViewModelTest.toqueDuploNaoGravaASerieDuasVezes`, `circuitoEnfileiraAsSeriesENumeraCerto`.

#### 9. Circuito começava com carga 0 em vez da última carga
- **Correção:** `CircuitSetup` (`ui/circuit/CircuitScreen.kt`) observa `lastWeights` e, quando a "última vez" chega do servidor, preenche a carga das linhas em que o usuário ainda não mexeu (`Row.kgEdited`).
- **Sem teste automático** (é tela). Testar: abrir o circuito de um grupo com histórico e ver a última carga em cada exercício.

#### 10. Datas do servidor em UTC: treino depois das 21h (Brasília) contava no dia seguinte
- **Correção:** migração **`docs/supabase/012_datas_no_fuso_de_brasilia.sql` (PENDENTE: rodar no SQL Editor)**: `vw_exercise_progression`, `records_summary()` e `profile_stats()` passam a usar `(started_at at time zone 'America/Sao_Paulo')::date`, com as mesmas colunas (o app não muda). Idempotente.
- **Testado no Postgres 16 local:** treinos às 22h30 de 27/10 e 7h de 28/10 (Brasília) eram uma "sessão" só em 28/10 antes; depois da 012 são duas sessões (27 e 28), recorde em 28/10 com o anterior em 27/10, 2 dias ativos; isolamento entre usuários e `security_invoker` mantidos, `anon` sem acesso, rodar 2 vezes não dá erro.

#### 11. Voltar na Home depois de terminar um treino abria outra Home
- **Correção:** `NavController.backToFreshHome()` em `ui/Navigation.kt`: no fim do treino ou do circuito, a Home antiga sai da pilha e entra uma nova (que recarrega o painel).
- **Sem teste automático.** Testar: terminar um treino, ver o painel atualizado e apertar "voltar" (deve sair do app).

#### 12. "Remover" dentro de um grupo apagava o exercício de todos os grupos e o histórico
- **Correção:** na lista filtrada por um grupo, o diálogo oferece "Tirar do grupo" (`ExerciseViewModel.removeFromGroup`: o exercício sai só daquele grupo e o histórico fica; o texto diz em quantos outros grupos ele continua) e "Apagar de vez" (o comportamento antigo). Na lista "Todos" continua só "Apagar", com aviso de que não dá para desfazer.
- **Teste:** `CatalogViewModelsTest.tirarDoGrupoMantemOExercicioNosOutrosGrupos`.

#### 13. Recorde marcado por engano não era desfeito
- **Correção:** `WorkoutViewModel` guarda o recorde de antes do treino (`recordsBefore`) e recalcula os recordes a partir das séries a cada registrar, editar ou apagar (`withRecords`). Corrigir um "200 kg" digitado por engano desfaz o selo, o "Recorde: X kg" volta ao de antes, e editar uma série para cima detecta o recorde. O confete passou a usar `recordBursts` (só sobe quando uma série registrada ou editada agora bate um recorde novo), então retomar um treino não solta confete.
- **Testes:** `WorkoutViewModelTest.recordeDigitadoPorEnganoEDesfeitoAoCorrigir`, `apagarASerieDoRecordeDesfazORecorde`, `editarParaCimaDetectaRecorde`.

### Alta prioridade (27/10/2026, PR #7, já no `master`)
Compilados e testados no CI; **ainda não vistos no aparelho**.

#### 1. Dados corporais de uma conta iam parar em outra no mesmo celular
- **Era:** peso, perfil corporal e medidas ficavam no aparelho sem separar por conta, nada era limpo ao sair, e a sincronização enviava esses dados para a próxima conta que entrasse.
- **Correção:** interface `domain/repository/LocalUserData` (`flush()` e `clear()`), implementada no app por `ui/LocalUserDataImpl.kt`. `ProfileViewModel.signOut` agora: (1) envia o peso/perfil corporal pendente para a nuvem enquanto a sessão vale (até 5 s; sem internet segue em frente); (2) encerra a sessão; (3) apaga do aparelho peso, perfil corporal e medidas (`BodyRepository.clear`), agenda (`ScheduleRepository.clear`), favoritos e recentes do catálogo (`CatalogPrefs.clear`), ritmo aprendido do circuito, músicas anotadas e não enviadas (`WorkoutTrackRecorder.discard`) e o CSV exportado no cache. Tema e preferências do aparelho ficam. `BodyRepository` ganhou uma "geração": uma sincronização da conta anterior que ainda estava em andamento para de gravar depois da troca. O diálogo "Sair da conta" avisa o que é apagado.
- **Atenção:** as **medidas corporais** ainda não têm tabela na nuvem, então se perdem ao sair da conta. Os dados já misturados antes desta correção continuam no banco: conferir com `select profile_id, count(*), min(measured_on), max(measured_on) from body_weights group by 1;`.
- **Testes:** `BodySyncTest` (limpar e próxima conta só vê os dela; sincronização interrompida pela troca não envia nada), `WeeklyScheduleTest` (limpar a agenda), `ProfileViewModelTest` (ordem enviar → sair → apagar; sem internet sai e apaga mesmo assim; toque duplo não repete).

#### 2. Nome de grupo com `/` fechava o app ao iniciar o treino
- **Correção:** em `ui/Navigation.kt`, todo texto livre na rota passa por `routeArg()` (`Uri.encode`, com `"null"` para valor ausente) e é lido por `routeText()`, já decodificado pelo Navigation; os `URLEncoder`/`URLDecoder` saíram. Vale para treino ativo, circuito, exercícios do grupo, detalhe do exercício e detalhe do treino. O treino livre continua aparecendo como "Treino Livre".
- **Sem teste automático** (navegação só roda no aparelho). Testar: grupo "Peito/Tríceps", exercício "Rosca 21 + 21", exercício "Supino 100%".

#### 3. Circuito: séries acima do limite sumiam sem aviso
- **Correção (limite mantido, como pedido):** o limite continua "séries do plano + 2" (`maxSetsFor`, `EXTRA_SETS_ALLOWED` em `WorkoutViewModel.kt`) e a mensagem agora diz o exercício e o limite: "Você chegou ao limite de 5 séries em Supino (3 do plano + 2 extras)." (`setLimitMessage`). O circuito ganhou Snackbar para os avisos do treino, e na preparação o "+" das séries para no limite e mostra a mesma mensagem.
- **Teste:** `WorkoutViewModelTest.respeitaLimiteDeSeries` confere o texto.

#### 4. Circuito: a última série podia ficar fora do resumo e do post no feed
- **Correção:** `WorkoutViewModel` guarda as gravações de série em andamento (`pendingSaves`) e `finishWorkout` espera todas (`joinAll`) antes de fechar o treino no banco, então o resumo e o gatilho do feed já veem a última série.
- **Teste:** `WorkoutViewModelTest.finalizarEsperaASerieQueAindaEstaGravando`.

#### 7. "Sair da conta" sem internet podia fechar o app
- **Correção:** `ProfileViewModel.signOut` trata as falhas de cada etapa, e `AuthRepositoryImpl.signOut` apaga a sessão salva no aparelho (`clearSession`) quando o pedido de logout ao servidor falha; assim a tela de abertura não entra de novo na conta.
