// Camada de dados: tudo que o app web pede ao Supabase fica aqui, com as mesmas tabelas e funções do app de celular.
let sb = null;

export function init(url, key) {
  sb = window.supabase.createClient(url, key, { auth: { persistSession: true, autoRefreshToken: true, detectSessionInUrl: false } });
  return sb;
}
export const client = () => sb;

/** Erro do Supabase -> Error com mensagem em português (o texto original fica em .raw). */
function fail(error, fallback) {
  const raw = String(error?.message || error || "");
  const low = raw.toLowerCase();
  let msg = fallback;
  if (low.includes("invalid login credentials")) msg = "E-mail ou senha incorretos.";
  else if (low.includes("email not confirmed")) msg = "Confirme seu e-mail antes de entrar.";
  else if (low.includes("already registered") || low.includes("already been registered")) msg = "Já existe uma conta com esse e-mail.";
  else if (low.includes("password should be at least") || low.includes("weak_password")) msg = "A senha é muito curta. Use pelo menos 6 caracteres.";
  else if (low.includes("rate limit") || low.includes("too many")) msg = "Muitas tentativas. Aguarde um pouco e tente de novo.";
  else if (low.includes("failed to fetch") || low.includes("networkerror") || low.includes("load failed")) msg = "Sem conexão com a internet. Verifique sua rede e tente de novo.";
  else if (low.includes("could not find the function") || low.includes("schema cache") || low.includes("does not exist")) {
    msg = low.includes("ranked_") ? "A ranqueada ainda não está ativada no servidor (falta rodar a migração 018)." : fallback;
  } else if (/^[A-ZÀ-Ú]/.test(raw) && raw.length < 140 && !low.includes("pgrst") && !low.includes("violates")) {
    // mensagens de regra do banco já vêm em português (raise exception)
    if (/[áéíóúãõç]/i.test(raw) || low.startsWith("x1") || low.startsWith("entre na") || low.startsWith("já existe") || low.startsWith("essa ")) msg = raw;
  }
  const e = new Error(msg);
  e.raw = raw;
  return e;
}
async function run(q, fallback) {
  const { data, error } = await q;
  if (error) throw fail(error, fallback);
  return data;
}

// ───────────────────────── Conta ─────────────────────────
export async function session() {
  const { data } = await sb.auth.getSession();
  return data.session;
}
export async function signIn(email, password) {
  const { error } = await sb.auth.signInWithPassword({ email, password });
  if (error) throw fail(error, "Não foi possível entrar.");
}
/** Devolve true se já entrou; false se o servidor pede para confirmar o e-mail antes. */
export async function signUp(email, password, username) {
  const { data, error } = await sb.auth.signUp({ email, password, options: { data: { username } } });
  if (error) throw fail(error, "Não foi possível criar a conta.");
  return !!data.session;
}
export async function signOut() { await sb.auth.signOut(); }
export async function deleteAccount() {
  await run(sb.rpc("delete_my_account"), "Não foi possível excluir a conta.");
  await sb.auth.signOut();
}

export async function profile(uid) {
  return run(sb.from("profiles").select("*").eq("id", uid).single(), "Erro ao carregar o perfil.");
}
export async function saveWeeklyGoal(uid, goal) {
  await run(sb.from("profiles").update({ weekly_goal: goal }).eq("id", uid), "Erro ao salvar a meta.");
}

// ───────────────────────── Grupos e exercícios ─────────────────────────
export async function muscleGroups() {
  return run(sb.from("muscle_groups").select("*").order("created_at", { ascending: true }), "Erro ao carregar os grupos.");
}
export async function createGroup(uid, name, colorHex) {
  return run(sb.from("muscle_groups").insert({ profile_id: uid, name, color_hex: colorHex }).select().single(), "Erro ao criar o grupo.");
}
export async function renameGroup(id, name, colorHex) {
  await run(sb.from("muscle_groups").update({ name, color_hex: colorHex }).eq("id", id), "Erro ao salvar o grupo.");
}
export async function deleteGroup(id) {
  await run(sb.from("muscle_groups").delete().eq("id", id), "Erro ao apagar o grupo.");
}

/** Todos os exercícios com os grupos em `groupIds` (N:N pela tabela group_exercises). */
export async function exercises() {
  const [rows, links] = await Promise.all([
    run(sb.from("exercises").select("*").order("name", { ascending: true }), "Erro ao carregar os exercícios."),
    run(sb.from("group_exercises").select("group_id, exercise_id"), "Erro ao carregar os exercícios."),
  ]);
  const by = new Map();
  for (const l of links) { if (!by.has(l.exercise_id)) by.set(l.exercise_id, []); by.get(l.exercise_id).push(l.group_id); }
  return rows.map((e) => ({ ...e, groupIds: by.get(e.id) || [] }));
}
export async function createExercise(uid, { name, sets = 3, reps = 12, notes = null, groupIds = [] }) {
  const saved = await run(sb.from("exercises").insert({ profile_id: uid, name, default_sets: sets, default_reps: reps, notes }).select().single(), "Erro ao criar o exercício.");
  if (groupIds.length) await linkExercise(saved.id, groupIds);
  return { ...saved, groupIds };
}
export async function linkExercise(exerciseId, groupIds) {
  // profile_id o banco preenche com auth.uid() (migração 006)
  const rows = [...new Set(groupIds)].map((g) => ({ group_id: g, exercise_id: exerciseId }));
  await run(sb.from("group_exercises").insert(rows), "Erro ao colocar o exercício no grupo.");
}
export async function unlinkExercise(exerciseId, groupId) {
  await run(sb.from("group_exercises").delete().eq("exercise_id", exerciseId).eq("group_id", groupId), "Erro ao tirar o exercício do grupo.");
}
export async function deleteExercise(id) {
  await run(sb.from("exercises").delete().eq("id", id), "Erro ao apagar o exercício.");
}

// ───────────────────────── Treinos ─────────────────────────
export async function activeWorkout(uid) {
  const rows = await run(sb.from("workouts").select("*").eq("profile_id", uid).is("finished_at", null).order("started_at", { ascending: false }).limit(1), "Erro ao carregar o treino.");
  return rows[0] || null;
}
export async function startWorkout(uid, groupId) {
  return run(sb.from("workouts").insert({ profile_id: uid, muscle_group_id: groupId }).select().single(), "Erro ao iniciar o treino.");
}
export async function finishWorkout(id) {
  await run(sb.from("workouts").update({ finished_at: new Date().toISOString() }).eq("id", id), "Erro ao finalizar o treino.");
}
export async function discardWorkout(id) {
  await run(sb.from("workouts").delete().eq("id", id), "Erro ao descartar o treino.");
}
export async function saveFinishedExercises(id, ids) {
  await run(sb.from("workouts").update({ finished_exercise_ids: ids }).eq("id", id), "Erro ao salvar.");
}
export async function saveNotes(id, notes) {
  await run(sb.from("workouts").update({ notes: notes?.trim() || null }).eq("id", id), "Erro ao salvar a anotação.");
}
export async function workout(id) {
  return run(sb.from("workouts").select("*").eq("id", id).single(), "Erro ao carregar o treino.");
}
export async function setsForWorkout(id) {
  return run(sb.from("workout_sets").select("*").eq("workout_id", id).order("set_number", { ascending: true }), "Erro ao carregar as séries.");
}
export async function addSet({ workoutId, exerciseId, setNumber, reps, weightKg, restSeconds = null }) {
  return run(sb.from("workout_sets").insert({
    workout_id: workoutId, exercise_id: exerciseId, set_number: setNumber, reps, weight_kg: weightKg, rest_seconds: restSeconds, completed_at: new Date().toISOString(),
  }).select().single(), "Erro ao registrar a série.");
}
export async function updateSet(id, reps, weightKg) {
  return run(sb.from("workout_sets").update({ reps, weight_kg: weightKg }).eq("id", id).select().single(), "Erro ao editar a série.");
}
export async function deleteSet(id) {
  await run(sb.from("workout_sets").delete().eq("id", id), "Erro ao apagar a série.");
}

/** Histórico completo de treinos terminados, em páginas de 1000 (limite do Supabase). */
export async function history() {
  const page = 1000;
  const all = [];
  for (let from = 0; ; from += page) {
    const chunk = await run(sb.from("workouts").select("*").not("finished_at", "is", null).order("started_at", { ascending: false }).order("id", { ascending: true }).range(from, from + page - 1), "Erro ao carregar o histórico.");
    all.push(...chunk);
    if (chunk.length < page) break;
  }
  return all;
}

/** Última carga/repetições e o recorde de carga de um exercício, sem contar o treino atual. */
export async function baseline(exerciseId, excludeWorkoutId) {
  const base = () => sb.from("workout_sets").select("weight_kg, reps").eq("exercise_id", exerciseId).neq("workout_id", excludeWorkoutId);
  const [rec, last] = await Promise.all([
    run(base().order("weight_kg", { ascending: false }).limit(1), "Erro ao carregar o histórico do exercício."),
    run(base().order("completed_at", { ascending: false }).limit(1), "Erro ao carregar o histórico do exercício."),
  ]);
  return { lastKg: last[0]?.weight_kg ?? null, lastReps: last[0]?.reps ?? null, recordKg: rec[0]?.weight_kg ?? null };
}

export async function records() {
  return run(sb.rpc("records_summary"), "Erro ao carregar os recordes.");
}

// ───────────────────────── Ranqueada (migração 018) ─────────────────────────
export async function rankedPlayer(uid) {
  const rows = await run(sb.from("ranked_players").select("*").eq("profile_id", uid), "Erro ao carregar a ranqueada.");
  return rows[0] || null;
}
export const rankedJoin = () => run(sb.rpc("ranked_join"), "Erro ao entrar na ranqueada.");
export const rankedBoard = () => run(sb.rpc("ranked_board"), "Erro ao carregar a turma.");
export async function rankedLastResult() { return (await run(sb.rpc("ranked_last_result"), "Erro."))[0] || null; }
export const rankedSettleWeek = (week) => run(sb.rpc("ranked_settle_week", { p_week: week }), "Erro ao fechar a semana.");
export const rankedSettleDuels = () => run(sb.rpc("ranked_settle_duels"), "Erro ao fechar os x1.");
export const rankedDuels = () => run(sb.rpc("ranked_my_duels"), "Erro ao carregar os x1.");
export const rankedDuelCreate = (opponent, ranked) => run(sb.rpc("ranked_duel_create", { p_opponent: opponent, p_ranked: ranked }), "Erro ao desafiar.");
export const rankedDuelRespond = (duel, accept) => run(sb.rpc("ranked_duel_respond", { p_duel: duel, p_accept: accept }), "Erro ao responder o x1.");

/** Todas as séries (para exportar), em páginas de 1000. */
export async function allSets() {
  const page = 1000; const all = [];
  for (let from = 0; ; from += page) {
    const chunk = await run(sb.from("workout_sets").select("*").order("completed_at", { ascending: true }).order("id", { ascending: true }).range(from, from + page - 1), "Erro ao carregar as séries.");
    all.push(...chunk);
    if (chunk.length < page) break;
  }
  return all;
}
export async function saveUsername(uid, username) {
  await run(sb.from("profiles").update({ username }).eq("id", uid), "Erro ao salvar o nome.");
}
