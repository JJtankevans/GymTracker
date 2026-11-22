import type { HistItem } from './data'
import { dayKey } from './rules'

const KEY = 'gt-wellness'
export interface Wellness { sleepH?: number; energy?: number } // energy 1–5
export type WellnessMap = Record<string, Wellness> // chave = dia (aaaa-mm-dd)

export function loadWellness(): WellnessMap {
  try { const v = JSON.parse(localStorage.getItem(KEY) ?? '{}'); return v && typeof v === 'object' ? v : {} } catch { return {} }
}

export function saveWellness(day: string, patch: Wellness): WellnessMap {
  const all = { ...loadWellness(), [day]: { ...loadWellness()[day], ...patch } }
  try { localStorage.setItem(KEY, JSON.stringify(all)) } catch { /* sem armazenamento */ }
  return all
}

export interface EnergyCompare { highDays: number; lowDays: number; highVolumeKg: number; lowVolumeKg: number }

/** Volume médio por treino nos dias de energia alta (≥ 4) e baixa (≤ 2). Precisa de pelo menos 2 treinos em cada grupo. */
export function energyCompare(items: HistItem[], w: WellnessMap): EnergyCompare | null {
  const hi: number[] = [], lo: number[] = []
  for (const it of items) {
    const e = w[dayKey(new Date(it.started_at))]?.energy
    if (!e) continue
    const vol = it.sets.reduce((a, s) => a + s.reps * Number(s.weight_kg), 0)
    if (e >= 4) hi.push(vol); else if (e <= 2) lo.push(vol)
  }
  if (hi.length < 2 || lo.length < 2) return null
  const avg = (xs: number[]) => xs.reduce((a, b) => a + b, 0) / xs.length
  return { highDays: hi.length, lowDays: lo.length, highVolumeKg: avg(hi), lowVolumeKg: avg(lo) }
}
