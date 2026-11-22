// Conquistas, recordes e evolução de carga (porte de Achievements.kt e do cálculo de recordes do app Android).
import { dayKey, estimateOneRepMax, weekStart } from './rules'

export type AchievementFamily = 'WORKOUTS' | 'STREAK' | 'WEEKS' | 'VOLUME' | 'RECORDS' | 'SPECIAL'
export const FAMILY_LABEL: Record<AchievementFamily, string> = {
  WORKOUTS: 'Treinos', STREAK: 'Dias seguidos', WEEKS: 'Semanas com a meta', VOLUME: 'Volume total', RECORDS: 'Recordes', SPECIAL: 'Especiais',
}
export interface Achievement { id: string; family: AchievementFamily; title: string; description: string; progress: number; target: number }
export interface SessionInfo { start: Date; minutes: number | null }

export const longestStreak = (dates: Date[]): number => {
  const days = [...new Set(dates.map(dayKey))].sort()
  if (days.length === 0) return 0
  let best = 1
  let run = 1
  for (let i = 1; i < days.length; i++) {
    const prev = new Date(days[i - 1] + 'T12:00:00')
    prev.setDate(prev.getDate() + 1)
    run = dayKey(prev) === days[i] ? run + 1 : 1
    best = Math.max(best, run)
  }
  return best
}

export const weeksMeetingGoal = (dates: Date[], goal: number): number => {
  const per = new Map<number, number>()
  for (const d of dates) { const k = weekStart(d).getTime(); per.set(k, (per.get(k) ?? 0) + 1) }
  return [...per.values()].filter((n) => n >= Math.max(1, goal)).length
}

export function computeAchievements(i: { sessions: SessionInfo[]; weeklyGoal: number; totalVolumeKg: number | null; exercisesWithRecord: number | null }): Achievement[] {
  const dates = i.sessions.map((s) => s.start)
  const fam = (f: AchievementFamily, tiers: [number, string][], progress: number, describe: (t: number) => string): Achievement[] =>
    tiers.map(([target, title]) => ({ id: `${f}_${target}`, family: f, title, description: describe(target), progress, target }))
  const out: Achievement[] = [
    ...fam('WORKOUTS', [[1, 'Primeiro passo'], [10, 'Frequentador'], [50, 'Rato de academia'], [100, 'Morador da academia'], [250, 'Lenda viva']], dates.length, (t) => (t === 1 ? 'Termine seu primeiro treino' : `Termine ${t} treinos`)),
    ...fam('STREAK', [[3, 'Embalado'], [7, 'Semana perfeita'], [14, 'Imparável'], [30, 'Mês de ferro']], longestStreak(dates), (t) => `Treine ${t} dias seguidos`),
    ...fam('WEEKS', [[4, 'Constância'], [12, 'Trimestre firme'], [26, 'Meio ano no foco'], [52, 'Ano de ferro']], weeksMeetingGoal(dates, i.weeklyGoal), (t) => `Bata sua meta semanal (${i.weeklyGoal} treinos) em ${t} semanas`),
  ]
  if (i.totalVolumeKg != null) out.push(...fam('VOLUME', [[10, '10 toneladas'], [100, '100 toneladas'], [500, '500 toneladas'], [1000, 'Mil toneladas']], Math.floor(i.totalVolumeKg / 1000), (t) => `Levante ${t} toneladas somando todas as séries`))
  if (i.exercisesWithRecord != null) out.push(...fam('RECORDS', [[1, 'Primeiro recorde'], [10, 'Colecionador de recordes'], [25, 'Máquina de recordes']], i.exercisesWithRecord, (t) => (t === 1 ? 'Tenha um recorde de carga registrado' : `Tenha recorde de carga em ${t} exercícios`)))
  if (i.sessions.length > 0) {
    const sp = (id: string, title: string, description: string, target: number, f: (s: SessionInfo) => boolean): Achievement =>
      ({ id: `SPECIAL_${id}`, family: 'SPECIAL', title, description, progress: i.sessions.filter(f).length, target })
    out.push(
      sp('EARLY', 'Madrugador', 'Comece 5 treinos antes das 7h', 5, (s) => s.start.getHours() < 7),
      sp('NIGHT', 'Coruja de ferro', 'Comece 5 treinos depois das 21h', 5, (s) => s.start.getHours() >= 21),
      sp('LONG', 'Maratonista', 'Faça 3 treinos de 90 minutos ou mais', 3, (s) => (s.minutes ?? 0) >= 90),
      sp('MONDAY', 'Segunda sem desculpa', 'Treine em 10 segundas-feiras', 10, (s) => s.start.getDay() === 1),
      sp('WEEKEND', 'Fim de semana sagrado', 'Treine 10 vezes no sábado ou domingo', 10, (s) => s.start.getDay() === 0 || s.start.getDay() === 6),
    )
  }
  return out
}

export interface SetLite { exercise: string; reps: number; kg: number; at: string }
export interface RecordRow { exercise: string; bestKg: number; bestOneRm: number; at: string }

/** Melhor carga e melhor 1RM estimado (limitado a 12 reps, como no app) por exercício. */
export function recordsOf(sets: SetLite[]): RecordRow[] {
  const m = new Map<string, RecordRow>()
  for (const s of sets) {
    const r = m.get(s.exercise) ?? { exercise: s.exercise, bestKg: 0, bestOneRm: 0, at: s.at }
    if (s.kg > r.bestKg) { r.bestKg = s.kg; r.at = s.at }
    r.bestOneRm = Math.max(r.bestOneRm, estimateOneRepMax(s.kg, Math.min(s.reps, 12)))
    m.set(s.exercise, r)
  }
  return [...m.values()].filter((r) => r.bestKg > 0).sort((a, b) => b.bestOneRm - a.bestOneRm)
}

/** Melhor 1RM por dia de treino, em ordem cronológica, para o gráfico de evolução. */
export function oneRmSeries(sets: SetLite[], exercise: string): { day: string; value: number }[] {
  const byDay = new Map<string, number>()
  for (const s of sets) {
    if (s.exercise !== exercise) continue
    const k = dayKey(new Date(s.at))
    byDay.set(k, Math.max(byDay.get(k) ?? 0, estimateOneRepMax(s.kg, Math.min(s.reps, 12))))
  }
  return [...byDay.entries()].sort((a, b) => a[0].localeCompare(b[0])).map(([day, value]) => ({ day, value }))
}

/** Série nova bate o recorde de carga? Só conta se já havia histórico (a primeira vez de um exercício não é recorde). */
export const isNewRecord = (bestKg: number | undefined, kg: number, reps: number): boolean => !!bestKg && bestKg > 0 && reps >= 1 && kg > bestKg
