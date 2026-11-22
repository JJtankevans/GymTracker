import { addExerciseToGroup, createGroup, groupExercises, loadHistory, loadHome } from './data'

/** Backup em um arquivo JSON (mesmo desenho do app Android: grupos, exercícios, vínculos, treinos, séries). Sem senha nem token. */
export interface BackupFile {
  app: 'GymTracker'
  version: 1
  createdAt: string
  groups: { id: string; name: string; color_hex: string }[]
  exercises: { id: string; name: string; default_sets: number; default_reps: number }[]
  exerciseGroups: Record<string, string[]>
  workouts: { id: string; muscle_group_id: string | null; muscle_group_name: string | null; started_at: string; finished_at: string | null }[]
  sets: { id: string; workout_id: string; exercise: string | null; set_number: number; reps: number; weight_kg: number; completed_at: string }[]
}

type G = BackupFile['groups'][number]
type E = BackupFile['exercises'][number]
type W = BackupFile['workouts'][number]
type S = BackupFile['sets'][number]

export function buildBackup(createdAt: string, groups: G[], exercisesByGroup: Record<string, E[]>, workouts: W[], sets: S[]): BackupFile {
  const exercises = new Map<string, E>()
  const exerciseGroups: Record<string, string[]> = {}
  for (const [gid, list] of Object.entries(exercisesByGroup)) {
    for (const e of list) {
      exercises.set(e.id, e)
      ;(exerciseGroups[e.id] ??= []).push(gid)
    }
  }
  return {
    app: 'GymTracker', version: 1, createdAt, groups,
    exercises: [...exercises.values()].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR')),
    exerciseGroups, workouts, sets,
  }
}

export async function collectBackup(): Promise<BackupFile> {
  const [home, hist] = await Promise.all([loadHome(), loadHistory({ keepWarmups: true })])
  const byGroup: Record<string, E[]> = {}
  await Promise.all(home.groups.map(async (g) => { byGroup[g.id] = await groupExercises(g.id) }))
  const workouts: W[] = []
  const sets: S[] = []
  for (const h of hist) {
    workouts.push({ id: h.id, muscle_group_id: h.muscle_group_id ?? null, muscle_group_name: h.muscle_group_name ?? null, started_at: h.started_at, finished_at: h.finished_at ?? null })
    for (const s of h.sets) sets.push({ id: s.id, workout_id: h.id, exercise: s.exercise?.name ?? null, set_number: s.set_number, reps: s.reps, weight_kg: Number(s.weight_kg), completed_at: s.completed_at })
  }
  return buildBackup(new Date().toISOString(), home.groups.map(({ id, name, color_hex }) => ({ id, name, color_hex })), byGroup, workouts, sets)
}

export function backupFileName(iso: string): string {
  return `gymtracker-backup-${iso.slice(0, 10)}.json`
}

export async function downloadBackup(): Promise<{ workouts: number; sets: number }> {
  const b = await collectBackup()
  const blob = new Blob([JSON.stringify(b, null, 2)], { type: 'application/json' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = backupFileName(b.createdAt)
  document.body.appendChild(a)
  a.click()
  a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
  return { workouts: b.workouts.length, sets: b.sets.length }
}

// ───────────── importar (só o catálogo: grupos, exercícios e vínculos) ─────────────
/** Lê e valida o texto de um backup (do Android ou da web). Lança Error com mensagem em português. */
export function parseBackup(text: string): BackupFile {
  let raw: unknown
  try { raw = JSON.parse(text) } catch { throw new Error('O arquivo não é um JSON válido.') }
  const b = raw as Partial<BackupFile> | null
  if (!b || b.app !== 'GymTracker') throw new Error('Este arquivo não é um backup do GymTracker.')
  if (typeof b.version !== 'number' || b.version > 1) throw new Error('Versão de backup desconhecida. Atualize o app.')
  if (!Array.isArray(b.groups) || !Array.isArray(b.exercises)) throw new Error('Backup incompleto (faltam grupos ou exercícios).')
  return {
    app: 'GymTracker', version: 1, createdAt: String(b.createdAt ?? ''),
    groups: b.groups.filter((g) => g && typeof g.name === 'string' && g.name.trim()).map((g) => ({ id: String(g.id), name: g.name.trim(), color_hex: g.color_hex || '#4CAF50' })),
    exercises: b.exercises.filter((e) => e && typeof e.name === 'string' && e.name.trim()).map((e) => ({ id: String(e.id), name: e.name.trim(), default_sets: Number(e.default_sets) || 3, default_reps: Number(e.default_reps) || 10 })),
    exerciseGroups: b.exerciseGroups && typeof b.exerciseGroups === 'object' ? b.exerciseGroups : {},
    workouts: Array.isArray(b.workouts) ? b.workouts : [],
    sets: Array.isArray(b.sets) ? b.sets : [],
  }
}

const norm = (s: string) => s.normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase()

export interface ImportPlan {
  newGroups: G[]
  /** exercício a ligar a um grupo (por nome; o grupo pode ser novo ou já existir). */
  links: { groupName: string; exercise: E }[]
  skippedGroups: number
  skippedLinks: number
  workoutsInFile: number
}

/** Calcula o que falta: nada existente é alterado, só se acrescenta (grupos e vínculos que não existem, comparando por nome). */
export function planImport(b: BackupFile, existing: Record<string, string[]>): ImportPlan {
  const have = new Map(Object.entries(existing).map(([name, ex]) => [norm(name), new Set(ex.map(norm))]))
  const idToName = new Map(b.groups.map((g) => [g.id, g.name]))
  const newGroups = b.groups.filter((g) => !have.has(norm(g.name)))
  const links: ImportPlan['links'] = []
  let skippedLinks = 0
  for (const e of b.exercises) {
    for (const gid of b.exerciseGroups[e.id] ?? []) {
      const gname = idToName.get(gid)
      if (!gname) continue
      if (have.get(norm(gname))?.has(norm(e.name))) { skippedLinks++; continue }
      links.push({ groupName: gname, exercise: e })
    }
  }
  return { newGroups, links, skippedGroups: b.groups.length - newGroups.length, skippedLinks, workoutsInFile: b.workouts.length }
}

export async function applyImport(b: BackupFile): Promise<ImportPlan> {
  const home = await loadHome()
  const existing: Record<string, string[]> = {}
  for (const g of home.groups) existing[g.name] = (await groupExercises(g.id)).map((x) => x.name)
  const plan = planImport(b, existing)
  const ids = new Map(home.groups.map((g) => [norm(g.name), g.id]))
  for (const g of plan.newGroups) ids.set(norm(g.name), (await createGroup(g.name, g.color_hex)).id)
  for (const l of plan.links) {
    const id = ids.get(norm(l.groupName))
    if (id) await addExerciseToGroup(id, l.exercise.name, l.exercise.default_sets, l.exercise.default_reps)
  }
  return plan
}
