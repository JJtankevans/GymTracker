import { MOCK, supabase } from './supabase'
import type { GroupLite, WorkoutLite } from './rules'
import { mockStore } from './mock'

export interface Group extends GroupLite { color_hex: string }
export interface Workout extends WorkoutLite {}

export interface HomeData { groups: Group[]; workouts: Workout[]; goal: number }

async function must<T>(p: PromiseLike<{ data: T | null; error: { message: string } | null }>): Promise<T> {
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
