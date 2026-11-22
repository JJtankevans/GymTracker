import { MOCK, supabase } from './supabase'
import type { GroupLite, WorkoutLite } from './rules'
import { mockStore } from './mock'

export interface Group extends GroupLite { color_hex: string }
export interface Workout extends WorkoutLite {}

export interface HomeData { groups: Group[]; workouts: Workout[]; goal: number }

async function must<T>(p: PromiseLike<{ data: unknown; error: { message: string } | null }>): Promise<T> {
  const { data, error } = await p
  if (error) throw new Error(error.message)
  return data as T
}

export async function loadHome(): Promise<HomeData> {
  if (MOCK) return mockStore.home()
  const [groups, workouts, profile] = await Promise.all([
    must<Group[]>(supabase.from('muscle_groups').select('id,name,color_hex').order('name')),
    must<Workout[]>(
      supabase.from('workouts').select('id,muscle_group_id,muscle_group_name,started_at,finished_at').order('started_at', { ascending: false }).limit(600),
    ),
    supabase.from('profiles').select('weekly_goal').maybeSingle(),
  ])
  return { groups: groups ?? [], workouts: workouts ?? [], goal: (profile.data as { weekly_goal?: number } | null)?.weekly_goal ?? 3 }
}

// ───────────── treino ativo ─────────────
export interface Exercise { id: string; name: string; default_sets: number; default_reps: number }
export interface SetRow { id: string; workout_id: string; exercise_id: string; set_number: number; reps: number; weight_kg: number; rest_seconds: number | null; completed_at: string }
export interface LastPerf { reps: number; weight_kg: number }

async function uid(): Promise<string> {
  const { data } = await supabase.auth.getSession()
  const id = data.session?.user.id
  if (!id) throw new Error('Sessão expirada. Entre de novo.')
  return id
}

export async function groupExercises(groupId: string): Promise<Exercise[]> {
  if (MOCK) return mockStore.groupExercises(groupId)
  const rows = await must<{ exercise: Exercise | null }[]>(
    supabase.from('group_exercises').select('exercise:exercises(id,name,default_sets,default_reps)').eq('group_id', groupId),
  )
  return rows.map((r) => r.exercise).filter((e): e is Exercise => !!e).sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'))
}

/** Última série registrada de cada exercício (para pré-preencher carga e reps). */
export async function lastPerformance(exerciseIds: string[]): Promise<Record<string, LastPerf>> {
  if (MOCK) return mockStore.lastPerformance(exerciseIds)
  if (exerciseIds.length === 0) return {}
  const rows = await must<{ exercise_id: string; reps: number; weight_kg: number }[]>(
    supabase.from('workout_sets').select('exercise_id,reps,weight_kg').in('exercise_id', exerciseIds).order('completed_at', { ascending: false }).limit(400),
  )
  const out: Record<string, LastPerf> = {}
  for (const r of rows) if (!out[r.exercise_id]) out[r.exercise_id] = { reps: r.reps, weight_kg: Number(r.weight_kg) }
  return out
}

export async function activeWorkout(): Promise<Workout | null> {
  if (MOCK) return mockStore.active()
  const rows = await must<Workout[]>(
    supabase.from('workouts').select('id,muscle_group_id,muscle_group_name,started_at,finished_at').is('finished_at', null).order('started_at', { ascending: false }).limit(1),
  )
  return rows[0] ?? null
}

export async function startWorkout(groupId: string | null): Promise<Workout> {
  if (MOCK) return mockStore.start(groupId)
  return must<Workout>(
    supabase.from('workouts').insert({ profile_id: await uid(), muscle_group_id: groupId }).select('id,muscle_group_id,muscle_group_name,started_at,finished_at').single(),
  )
}

export async function workoutSets(workoutId: string): Promise<SetRow[]> {
  if (MOCK) return mockStore.sets(workoutId)
  return must<SetRow[]>(supabase.from('workout_sets').select('*').eq('workout_id', workoutId).order('completed_at'))
}

export async function addSet(workoutId: string, exerciseId: string, setNumber: number, reps: number, weightKg: number): Promise<SetRow> {
  if (MOCK) return mockStore.addSet(workoutId, exerciseId, setNumber, reps, weightKg)
  return must<SetRow>(
    supabase.from('workout_sets').insert({ workout_id: workoutId, exercise_id: exerciseId, set_number: setNumber, reps, weight_kg: weightKg }).select('*').single(),
  )
}

export async function deleteSet(id: string): Promise<void> {
  if (MOCK) return mockStore.deleteSet(id)
  await must(supabase.from('workout_sets').delete().eq('id', id).select('id'))
}

export async function finishWorkout(id: string, atIso?: string): Promise<void> {
  if (MOCK) return mockStore.finish(id)
  await must(supabase.from('workouts').update({ finished_at: atIso ?? new Date().toISOString() }).eq('id', id).select('id'))
}

export async function deleteWorkout(id: string): Promise<void> {
  if (MOCK) return mockStore.deleteWorkout(id)
  await must(supabase.from('workouts').delete().eq('id', id).select('id'))
}

// ───────────── histórico e grupos ─────────────
export interface HistSet { id: string; reps: number; weight_kg: number; set_number: number; completed_at: string; exercise: { name: string } | null }
export interface HistItem extends Workout { sets: HistSet[] }

export async function loadHistory(): Promise<HistItem[]> {
  if (MOCK) return mockStore.history()
  const rows = await must<(Workout & { workout_sets: (Omit<HistSet, 'exercise'> & { exercise: { name: string } | null })[] })[]>(
    supabase
      .from('workouts')
      .select('id,muscle_group_id,muscle_group_name,started_at,finished_at,workout_sets(id,reps,weight_kg,set_number,completed_at,exercise:exercises(name))')
      .not('finished_at', 'is', null)
      .order('started_at', { ascending: false })
      .limit(300),
  )
  return rows.map(({ workout_sets, ...w }) => ({ ...w, sets: workout_sets ?? [] }))
}

/** Quantos exercícios há em cada grupo. */
export async function groupCounts(): Promise<Record<string, number>> {
  if (MOCK) return mockStore.counts()
  const rows = await must<{ group_id: string }[]>(supabase.from('group_exercises').select('group_id'))
  const out: Record<string, number> = {}
  for (const r of rows) out[r.group_id] = (out[r.group_id] ?? 0) + 1
  return out
}

export async function createGroup(name: string, color: string): Promise<Group> {
  if (MOCK) return mockStore.createGroup(name, color)
  // O id nasce no cliente: INSERT ... RETURNING pode ser barrado pela RLS (mesma regra do app Android).
  const id = crypto.randomUUID()
  await must(supabase.from('muscle_groups').insert({ id, profile_id: await uid(), name: name.trim(), color_hex: color }))
  return { id, name: name.trim(), color_hex: color }
}

export async function deleteGroup(id: string): Promise<void> {
  if (MOCK) return mockStore.deleteGroup(id)
  await must(supabase.from('muscle_groups').delete().eq('id', id).select('id'))
}

/** Liga um exercício ao grupo; reaproveita o que já existe com o mesmo nome (nome é único por usuário). */
export async function addExerciseToGroup(groupId: string, name: string, sets: number, reps: number): Promise<void> {
  if (MOCK) return mockStore.addExercise(groupId, name, sets, reps)
  const profile_id = await uid()
  const clean = name.trim()
  const found = await must<{ id: string }[]>(supabase.from('exercises').select('id').eq('name', clean).limit(1))
  let exerciseId = found[0]?.id
  if (!exerciseId) {
    exerciseId = crypto.randomUUID()
    await must(supabase.from('exercises').insert({ id: exerciseId, profile_id, name: clean, default_sets: sets, default_reps: reps }))
  }
  await must(supabase.from('group_exercises').upsert({ group_id: groupId, exercise_id: exerciseId, profile_id }, { onConflict: 'group_id,exercise_id', ignoreDuplicates: true }))
}

export async function removeFromGroup(groupId: string, exerciseId: string): Promise<void> {
  if (MOCK) return mockStore.removeFromGroup(groupId, exerciseId)
  await must(supabase.from('group_exercises').delete().eq('group_id', groupId).eq('exercise_id', exerciseId).select('group_id'))
}

export const GOAL_MIN = 1
export const GOAL_MAX = 7
export const clampGoal = (n: number) => Math.min(GOAL_MAX, Math.max(GOAL_MIN, Math.round(n)))

/** Meta semanal de treinos (1 a 7), a mesma do app Android (profiles.weekly_goal). */
export async function setWeeklyGoal(goal: number): Promise<number> {
  const g = clampGoal(goal)
  if (MOCK) { mockStore.setGoal(g); return g }
  await must(supabase.from('profiles').update({ weekly_goal: g }).eq("id", await uid()).select("id"))
  return g
}
