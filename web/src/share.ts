import { addExerciseToGroup, createGroup, type Exercise, type Group } from './data'
import { MOCK, supabase } from './supabase'

/** Compartilhar um grupo de treino por código (#608): mesma tabela e função do app (`shared_routines`, migração 011). */
export interface SharedExercise { name: string; sets: number; reps: number; notes?: string }
export interface SharedRoutine { code: string; name: string; color_hex: string; exercises: SharedExercise[]; owner_username: string | null; imports: number }

/** Código em maiúsculas e sem espaços (como a função do banco). */
export const normalizeCode = (s: string): string => s.replace(/\s+/g, '').toUpperCase()

/** Cópia (snapshot) do grupo para publicar: máx. 40 exercícios, nomes e séries/reps saneados. */
export function buildSnapshot(exercises: Exercise[]): SharedExercise[] {
  return exercises.slice(0, 40).map((e) => ({ name: e.name.trim().slice(0, 80), sets: Math.min(20, Math.max(1, e.default_sets)), reps: Math.min(100, Math.max(1, e.default_reps)) })).filter((e) => e.name)
}

/** Valida o que veio do banco antes de criar exercícios (nunca confia no formato). */
export function parseSharedExercises(raw: unknown): SharedExercise[] {
  if (!Array.isArray(raw)) return []
  const out: SharedExercise[] = []
  for (const r of raw.slice(0, 40)) {
    if (!r || typeof r !== 'object') continue
    const o = r as Record<string, unknown>
    const name = typeof o.name === 'string' ? o.name.trim().slice(0, 80) : ''
    if (!name) continue
    const sets = Number(o.sets), reps = Number(o.reps)
    out.push({ name, sets: Number.isFinite(sets) ? Math.min(20, Math.max(1, Math.round(sets))) : 3, reps: Number.isFinite(reps) ? Math.min(100, Math.max(1, Math.round(reps))) : 12 })
  }
  return out
}

export async function publishGroup(group: Group, exercises: Exercise[]): Promise<string> {
  const snapshot = buildSnapshot(exercises)
  if (snapshot.length === 0) throw new Error('Adicione exercícios ao grupo antes de compartilhar.')
  if (MOCK) return 'DEMO1234'
  const { data, error } = await supabase.from('shared_routines').insert({ name: group.name.slice(0, 60), color_hex: group.color_hex, exercises: snapshot }).select('code').single()
  if (error) throw new Error(error.message)
  return (data as { code: string }).code
}

export async function lookupCode(code: string): Promise<SharedRoutine> {
  const c = normalizeCode(code)
  if (!c) throw new Error('Digite o código.')
  if (MOCK) return { code: c, name: 'Costas (demonstração)', color_hex: '#2196F3', exercises: [{ name: 'Remada Curvada', sets: 4, reps: 10 }, { name: 'Puxada Frontal', sets: 3, reps: 12 }], owner_username: 'Marina', imports: 3 }
  const { data, error } = await supabase.rpc('get_shared_routine', { p_code: c, p_count_import: false })
  if (error) throw new Error(error.message)
  const row = (data as { code: string; name: string; color_hex: string; exercises: unknown; owner_username: string | null; imports: number }[] | null)?.[0]
  if (!row) throw new Error('Código não encontrado.')
  const exercises = parseSharedExercises(row.exercises)
  if (exercises.length === 0) throw new Error('Este compartilhamento não tem exercícios válidos.')
  return { code: row.code, name: row.name, color_hex: /^#[0-9A-Fa-f]{6}$/.test(row.color_hex) ? row.color_hex : '#6200EE', exercises, owner_username: row.owner_username, imports: row.imports }
}

/** Cria um grupo seu com os exercícios do compartilhamento e conta a importação. */
export async function importRoutine(r: SharedRoutine): Promise<Group> {
  const g = await createGroup(r.name, r.color_hex)
  for (const e of r.exercises) await addExerciseToGroup(g.id, e.name, e.sets, e.reps)
  if (!MOCK) await supabase.rpc('get_shared_routine', { p_code: r.code, p_count_import: true }).then(() => undefined, () => undefined)
  return g
}
