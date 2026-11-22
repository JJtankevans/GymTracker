import { CATALOG } from './catalog'
import { fold } from './catalogSearch'
import type { SetLite } from './achievements'

export interface MuscleSets { muscle: string | null; sets: number }
/** Faixa comum de séries semanais por músculo para hipertrofia (igual ao app Android). */
export const WEEKLY_SETS_RANGE = [10, 20] as const
export const ALL_MUSCLES = ['Peito', 'Costas', 'Ombros', 'Bíceps', 'Tríceps', 'Quadríceps', 'Posterior', 'Glúteos', 'Panturrilha', 'Abdômen']

const byName = new Map<string, string>()
for (const e of CATALOG) byName.set(fold(e.name), e.muscle)
for (const e of CATALOG) for (const a of e.aliases ?? []) if (!byName.has(fold(a))) byName.set(fold(a), e.muscle)

/** Músculo de um exercício pelo nome ou apelido do catálogo (null = fora do catálogo). */
export const muscleOf = (name: string): string | null => byName.get(fold(name)) ?? null

/** Porte de weeklySetsByMuscle: conta séries (reps ≥ 1) dos últimos [days] dias; todos os músculos aparecem, mesmo com 0. Mais séries primeiro. */
export function weeklySetsByMuscle(sets: SetLite[], now = Date.now(), days = 7): MuscleSets[] {
  const from = now - days * 86_400_000
  const counts = new Map<string | null, number>()
  for (const s of sets) {
    if (s.reps <= 0) continue
    const t = new Date(s.at).getTime()
    if (!(t >= from && t <= now)) continue
    const m = muscleOf(s.exercise)
    counts.set(m, (counts.get(m) ?? 0) + 1)
  }
  const rows: MuscleSets[] = ALL_MUSCLES.map((m) => ({ muscle: m, sets: counts.get(m) ?? 0 }))
  const others = counts.get(null)
  if (others) rows.push({ muscle: null, sets: others })
  return rows.sort((a, b) => b.sets - a.sets || (a.muscle ?? 'Outros').localeCompare(b.muscle ?? 'Outros', 'pt-BR'))
}
