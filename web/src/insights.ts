import type { HistItem } from './data'
import type { SetLite } from './achievements'
import { muscleOf } from './muscle'
import { dayKey, loadForReps } from './rules'

const DAY = 86_400_000

// ───────── #555 recordes por faixa de repetições ─────────
export const RM_BANDS = [1, 5, 10] as const
export interface BandRecord { exercise: string; band: number; kg: number; at: string }

/** Maior carga feita com pelo menos [band] repetições (nRM) em cada exercício. */
export function bandRecords(sets: SetLite[], bands: readonly number[] = RM_BANDS): BandRecord[] {
  const best = new Map<string, BandRecord>()
  for (const s of sets) {
    if (s.kg <= 0 || s.reps < 1) continue
    for (const b of bands) {
      if (s.reps < b) continue
      const k = `${s.exercise}|${b}`
      const cur = best.get(k)
      if (!cur || s.kg > cur.kg || (s.kg === cur.kg && s.at < cur.at)) best.set(k, { exercise: s.exercise, band: b, kg: s.kg, at: s.at })
    }
  }
  return [...best.values()].sort((a, b) => a.exercise.localeCompare(b.exercise, 'pt-BR') || a.band - b.band)
}

// ───────── #556 calorias ─────────
/** Estimativa grosseira: musculação ≈ 5 MET (compêndio de atividades físicas). kcal = MET × kg × horas. */
export const MET_WEIGHT_TRAINING = 5
export const estimateKcal = (minutes: number, bodyKg: number): number => Math.round(MET_WEIGHT_TRAINING * bodyKg * (Math.max(0, minutes) / 60))

// ───────── #557 desequilíbrio muscular ─────────
const PUSH = ['Peito', 'Ombros', 'Tríceps']
const PULL = ['Costas', 'Bíceps']
export interface Imbalance { label: string; a: string; b: string; setsA: number; setsB: number; ratio: number | null; warn: boolean }

/** Compara séries de empurrar × puxar e quadríceps × posterior. Aviso quando um lado é mais que o dobro do outro. */
export function imbalances(sets: SetLite[], now = Date.now(), days = 28): Imbalance[] {
  const from = now - days * DAY
  const by = new Map<string, number>()
  for (const s of sets) {
    const t = new Date(s.at).getTime()
    if (s.reps <= 0 || !(t >= from && t <= now)) continue
    const m = muscleOf(s.exercise)
    if (m) by.set(m, (by.get(m) ?? 0) + 1)
  }
  const sum = (ms: string[]) => ms.reduce((a, m) => a + (by.get(m) ?? 0), 0)
  const mk = (label: string, a: string, b: string, setsA: number, setsB: number): Imbalance => {
    const hi = Math.max(setsA, setsB), lo = Math.min(setsA, setsB)
    return { label, a, b, setsA, setsB, ratio: lo ? hi / lo : null, warn: hi >= 8 && hi > lo * 2 }
  }
  return [
    mk('Empurrar × puxar', 'Empurrar', 'Puxar', sum(PUSH), sum(PULL)),
    mk('Quadríceps × posterior', 'Quadríceps', 'Posterior', by.get('Quadríceps') ?? 0, by.get('Posterior') ?? 0),
  ]
}

// ───────── #558 platô ─────────
export interface Plateau { exercise: string; weeks: number; bestOneRm: number }

/** Exercícios cujo melhor 1RM estimado não sobe há [minWeeks] semanas (e que foram treinados nesse período). */
export function plateaus(sets: SetLite[], now = Date.now(), minWeeks = 4): Plateau[] {
  const by = new Map<string, SetLite[]>()
  for (const s of sets) if (s.kg > 0 && s.reps >= 1) (by.get(s.exercise) ?? by.set(s.exercise, []).get(s.exercise)!).push(s)
  const out: Plateau[] = []
  for (const [exercise, list] of by) {
    let best = 0, bestAt = 0, lastAt = 0
    for (const s of list) {
      const t = new Date(s.at).getTime()
      const rm = s.reps <= 1 ? s.kg : s.kg * (1 + s.reps / 30)
      if (rm > best + 1e-9) { best = rm; bestAt = t }
      lastAt = Math.max(lastAt, t)
    }
    const weeks = Math.floor((now - bestAt) / (7 * DAY))
    if (weeks >= minWeeks && now - lastAt <= weeks * 7 * DAY && now - lastAt < 21 * DAY) out.push({ exercise, weeks, bestOneRm: best })
  }
  return out.sort((a, b) => b.weeks - a.weeks)
}

// ───────── #574 mapa de calor ─────────
export interface HeatCell { day: string; count: number; level: 0 | 1 | 2 | 3 }

/** Células dos últimos [weeks] semanas (coluna = semana, de segunda a domingo), com nível 0–3 pelo nº de séries. */
export function heatmap(items: HistItem[], now = new Date(), weeks = 26): HeatCell[][] {
  const sets = new Map<string, number>()
  for (const w of items) sets.set(dayKey(new Date(w.started_at)), (sets.get(dayKey(new Date(w.started_at))) ?? 0) + Math.max(1, w.sets.length))
  const monday = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7) - 7 * (weeks - 1))
  const cols: HeatCell[][] = []
  for (let c = 0; c < weeks; c++) {
    const col: HeatCell[] = []
    for (let r = 0; r < 7; r++) {
      const d = new Date(monday); d.setDate(monday.getDate() + c * 7 + r)
      const n = d > now ? 0 : sets.get(dayKey(d)) ?? 0
      col.push({ day: dayKey(d), count: n, level: n === 0 ? 0 : n < 12 ? 1 : n < 20 ? 2 : 3 })
    }
    cols.push(col)
  }
  return cols
}

// ───────── #565 CSV ─────────
const cell = (v: string | number) => { const s = String(v); return /[;"\n]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s }

/** CSV (separador ;, abre direto no Excel em pt-BR) com uma linha por série. */
export function historyCsv(items: HistItem[]): string {
  const rows = [['data', 'grupo', 'exercicio', 'serie', 'repeticoes', 'carga_kg'].join(';')]
  for (const w of items) for (const s of w.sets) {
    rows.push([new Date(w.started_at).toISOString().slice(0, 10), w.muscle_group_name ?? '', s.exercise?.name ?? '', s.set_number, s.reps, String(s.weight_kg).replace('.', ',')].map(cell).join(';'))
  }
  return '﻿' + rows.join('\r\n')
}

// ───────── #573 comparar dois treinos ─────────
export interface ExerciseDiff { exercise: string; a: { kg: number; reps: number } | null; b: { kg: number; reps: number } | null }

/** Melhor série (maior 1RM estimado) de cada exercício nos dois treinos. */
export function compareWorkouts(a: HistItem, b: HistItem): ExerciseDiff[] {
  const top = (w: HistItem) => {
    const m = new Map<string, { kg: number; reps: number }>()
    for (const s of w.sets) {
      const name = s.exercise?.name ?? 'Exercício'
      const kg = Number(s.weight_kg)
      const cur = m.get(name)
      const rm = (x: { kg: number; reps: number }) => (x.reps <= 1 ? x.kg : x.kg * (1 + x.reps / 30))
      if (!cur || rm({ kg, reps: s.reps }) > rm(cur)) m.set(name, { kg, reps: s.reps })
    }
    return m
  }
  const ma = top(a), mb = top(b)
  return [...new Set([...ma.keys(), ...mb.keys()])].sort((x, y) => x.localeCompare(y, 'pt-BR')).map((exercise) => ({ exercise, a: ma.get(exercise) ?? null, b: mb.get(exercise) ?? null }))
}

// ───────── #577 pesquisa no histórico ─────────
export interface SearchHit { date: string; workoutId: string; exercise: string; reps: number; kg: number }
const fold = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase()

/** Procura por nome de exercício (sem acento) ou carga ("80", "80kg"); mais recentes primeiro. */
export function searchHistory(items: HistItem[], query: string, limit = 60): SearchHit[] {
  const q = fold(query.trim())
  if (!q) return []
  const kgQ = /^\d+([.,]\d+)?\s*(kg)?$/.test(q) ? Number(q.replace(/kg/, '').trim().replace(',', '.')) : null
  const hits: SearchHit[] = []
  for (const w of items) for (const s of w.sets) {
    const name = s.exercise?.name ?? 'Exercício'
    if (kgQ !== null ? Number(s.weight_kg) === kgQ : fold(name).includes(q)) hits.push({ date: w.started_at, workoutId: w.id, exercise: name, reps: s.reps, kg: Number(s.weight_kg) })
  }
  return hits.sort((a, b) => b.date.localeCompare(a.date)).slice(0, limit)
}

export { loadForReps }

// ───────── #559 sobrecarga progressiva ─────────
export interface Progression { kg: number; reps: number; text: string }

/** Sugestão para a próxima vez: bateu as repetições-alvo → sobe a carga (+2,5 kg, ou +5 kg se a carga passa de 60); senão mantém a carga e pede +1 repetição. */
export function nextProgression(last: { kg: number; reps: number } | undefined, targetReps: number): Progression | null {
  if (!last || last.kg <= 0 || last.reps < 1) return null
  if (last.reps >= targetReps) {
    const step = last.kg >= 60 ? 5 : 2.5
    return { kg: last.kg + step, reps: targetReps, text: `Sugestão: ${String(last.kg + step).replace('.', ',')} kg × ${targetReps}` }
  }
  return { kg: last.kg, reps: last.reps + 1, text: `Sugestão: ${String(last.kg).replace('.', ',')} kg × ${last.reps + 1}` }
}

// ───────── #560 semana de descarga ─────────
/** Volume (kg) por semana nas últimas [n] semanas (a atual por último). */
export function weeklyVolumes(items: HistItem[], now = new Date(), n = 12): number[] {
  const mondayOf = (d: Date) => { const x = new Date(d.getFullYear(), d.getMonth(), d.getDate()); x.setDate(x.getDate() - ((x.getDay() + 6) % 7)); return x.getTime() }
  const cur = mondayOf(now)
  const out = Array<number>(n).fill(0)
  for (const w of items) {
    const i = n - 1 - Math.round((cur - mondayOf(new Date(w.started_at))) / (7 * DAY))
    if (i >= 0 && i < n) out[i] += w.sets.reduce((a, s) => a + s.reps * Number(s.weight_kg), 0)
  }
  return out
}

/** Sugere descarga quando as 4 últimas semanas (a atual conta só se já passou do volume) ficaram todas acima da mediana de 12 semanas e a semana atual não é de descarga. */
export function needsDeload(volumes: number[]): boolean {
  if (volumes.length < 8) return false
  const sorted = [...volumes].filter((v) => v > 0).sort((a, b) => a - b)
  if (sorted.length < 6) return false
  const median = sorted[Math.floor(sorted.length / 2)]
  const last4 = volumes.slice(-4)
  return last4.every((v) => v >= median * 1.05)
}

// ───────── #564 metas do mês (selos sazonais) ─────────
import type { Achievement } from './achievements'
const MONTH_NAMES = ['Janeiro', 'Fevereiro', 'Março', 'Abril', 'Maio', 'Junho', 'Julho', 'Agosto', 'Setembro', 'Outubro', 'Novembro', 'Dezembro']

/** Três selos que valem só para o mês corrente: treinos (meta semanal × 4), dias diferentes e volume. */
export function monthlyBadges(items: HistItem[], weeklyGoal: number, now = new Date()): Achievement[] {
  const ws = items.filter((w) => { const d = new Date(w.started_at); return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() })
  const days = new Set(ws.map((w) => dayKey(new Date(w.started_at)))).size
  const volume = ws.reduce((a, w) => a + w.sets.reduce((b, s) => b + s.reps * Number(s.weight_kg), 0), 0)
  const mk = (suffix: string, title: string, description: string, progress: number, target: number): Achievement => ({
    id: `month-${now.getFullYear()}-${now.getMonth() + 1}-${suffix}`, family: 'SPECIAL', title, description, progress, target,
  })
  const name = MONTH_NAMES[now.getMonth()]
  const target = Math.max(4, weeklyGoal * 4)
  return [
    mk('workouts', `${name} em dia`, `Faça ${target} treinos em ${name.toLowerCase()}`, ws.length, target),
    mk('days', `${name} constante`, 'Treine em 12 dias diferentes no mês', days, 12),
    mk('volume', `${name} pesado`, 'Levante 50 toneladas no mês', Math.round(volume / 1000), 50),
  ]
}
