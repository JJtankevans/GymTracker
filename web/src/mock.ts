// Dados falsos para ver a interface sem login (?mock). Determinísticos, relativos a "hoje"; escrevem só em memória.
import type { Exercise, Group, HistItem, HomeData, LastPerf, SetRow, Workout } from './data'

const iso = (daysAgo: number, hour = 18) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  d.setHours(hour, 0, 0, 0)
  return d.toISOString()
}

let mockGoal = 5
let groups: Group[] = [
  { id: 'g1', name: 'PEITO E TRICEPS', color_hex: '#4CAF50' },
  { id: 'g2', name: 'Biceps Costa', color_hex: '#2196F3' },
  { id: 'g3', name: 'Quadriceps e Panturrilha', color_hex: '#3F51B5' },
  { id: 'g4', name: 'Ombro', color_hex: '#E91E63' },
  { id: 'g5', name: 'Glúteos e Posterior', color_hex: '#4CAF50' },
]

let exercises: Record<string, Exercise[]> = {
  g1: [
    { id: 'e1', name: 'Supino Reto', default_sets: 4, default_reps: 10 },
    { id: 'e2', name: 'Tríceps Corda', default_sets: 3, default_reps: 12 },
    { id: 'e3', name: 'Crucifixo Inclinado', default_sets: 3, default_reps: 12 },
  ],
  g5: [
    { id: 'e4', name: 'Stiff', default_sets: 4, default_reps: 10 },
    { id: 'e5', name: 'Mesa Flexora', default_sets: 3, default_reps: 12 },
  ],
}

let workouts: Workout[] = [1, 2, 4, 6, 9, 13, 16, 18, 21, 25, 30, 33].map((ago, i) => {
  const g = groups[i % 4]
  const s = iso(ago)
  return { id: `w${i}`, muscle_group_id: g.id, muscle_group_name: g.name, started_at: s, finished_at: new Date(Date.parse(s) + 55 * 60000).toISOString() }
})
let sets: SetRow[] = []
let n = 0

const names = ['Supino Reto', 'Tríceps Corda', 'Crucifixo Inclinado']
const histSets = (w: Workout, i: number) => names.flatMap((nm, e) => [1, 2, 3].map((k) => ({
  id: `hs${i}-${e}-${k}`, reps: 12 - k, weight_kg: 20 + e * 10 + i, set_number: k,
  completed_at: new Date(Date.parse(w.started_at) + (e * 3 + k) * 4 * 60000).toISOString(), exercise: { name: nm },
})))

export const mockStore = {
  async history(): Promise<HistItem[]> { return workouts.filter((w) => w.finished_at).map((w, i) => ({ ...w, sets: histSets(w, i) })) },
  async counts(): Promise<Record<string, number>> { return Object.fromEntries(groups.map((g) => [g.id, (exercises[g.id] ?? []).length])) },
  async createGroup(name: string, color: string): Promise<Group> { const g = { id: `ng${++n}`, name, color_hex: color }; groups = [...groups, g]; return g },
  async deleteGroup(id: string) { groups = groups.filter((g) => g.id !== id) },
  async addExercise(groupId: string, name: string, sets: number, reps: number) {
    exercises = { ...exercises, [groupId]: [...(exercises[groupId] ?? []), { id: `ne${++n}`, name, default_sets: sets, default_reps: reps }] }
  },
  async removeFromGroup(groupId: string, exerciseId: string) { exercises = { ...exercises, [groupId]: (exercises[groupId] ?? []).filter((e) => e.id !== exerciseId) } },
  async home(): Promise<HomeData> { return { groups, workouts: [...workouts], goal: mockGoal } },
  setGoal(n: number) { mockGoal = n },
  async groupExercises(groupId: string): Promise<Exercise[]> { return exercises[groupId] ?? [] },
  async lastPerformance(ids: string[]): Promise<Record<string, LastPerf>> {
    return Object.fromEntries(ids.map((id, i) => [id, { reps: 10 + (i % 3), weight_kg: 20 + i * 12.5 }]))
  },
  async active(): Promise<Workout | null> { return workouts.find((w) => !w.finished_at) ?? null },
  async start(groupId: string | null): Promise<Workout> {
    const g = groups.find((x) => x.id === groupId)
    const w: Workout = { id: `n${++n}`, muscle_group_id: groupId, muscle_group_name: g?.name ?? null, started_at: new Date().toISOString(), finished_at: null }
    workouts = [w, ...workouts]
    return w
  },
  async sets(workoutId: string): Promise<SetRow[]> { return sets.filter((s) => s.workout_id === workoutId) },
  async addSet(workout_id: string, exercise_id: string, set_number: number, reps: number, weight_kg: number): Promise<SetRow> {
    const s: SetRow = { id: `s${++n}`, workout_id, exercise_id, set_number, reps, weight_kg, rest_seconds: null, completed_at: new Date().toISOString() }
    sets = [...sets, s]
    return s
  },
  async deleteSet(id: string) { sets = sets.filter((s) => s.id !== id) },
  async finish(id: string) { workouts = workouts.map((w) => (w.id === id ? { ...w, finished_at: new Date().toISOString() } : w)) },
  async deleteWorkout(id: string) { workouts = workouts.filter((w) => w.id !== id); sets = sets.filter((s) => s.workout_id !== id) },
}
