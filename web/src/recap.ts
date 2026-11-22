import type { HistItem } from './data'
import { shareCard, type CardSpec } from './shareCard'
import { dayKey, formatKg, prettyGroupName } from './rules'

/** Retrospectiva de um mês (month 1–12) ou do ano inteiro (month 0): porte do MonthlyRecap do Android. */
export interface Recap {
  year: number
  month: number
  workouts: number
  daysTrained: number
  sets: number
  reps: number
  volumeKg: number
  topGroup: string | null
  topGroupWorkouts: number
}

export const MONTHS = ['janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho', 'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro']

export function buildRecap(items: HistItem[], year: number, month: number): Recap {
  const ws = items.filter((w) => {
    const d = new Date(w.started_at)
    return d.getFullYear() === year && (month === 0 || d.getMonth() + 1 === month)
  })
  const days = new Set(ws.map((w) => dayKey(new Date(w.started_at))))
  const sets = ws.flatMap((w) => w.sets)
  const counts = new Map<string, number>()
  for (const w of ws) {
    const g = w.muscle_group_name ? prettyGroupName(w.muscle_group_name) : 'Treino livre'
    counts.set(g, (counts.get(g) ?? 0) + 1)
  }
  let top: [string, number] | null = null
  for (const e of counts) if (!top || e[1] > top[1]) top = e
  return {
    year, month, workouts: ws.length, daysTrained: days.size, sets: sets.length,
    reps: sets.reduce((a, s) => a + s.reps, 0),
    volumeKg: sets.reduce((a, s) => a + s.reps * Number(s.weight_kg), 0),
    topGroup: top ? top[0] : null, topGroupWorkouts: top ? top[1] : 0,
  }
}

export const recapTitle = (r: Recap) => (r.month === 0 ? `Retrospectiva de ${r.year}` : `Retrospectiva de ${MONTHS[r.month - 1]}`)

/** 12 345 kg → '12,3 t' a partir de 1 000 kg (mesma ideia do formatVolume do Android). */
export const formatVolume = (kg: number) => (kg >= 1000 ? `${formatKg(kg / 1000)} t` : `${formatKg(kg)} kg`)

/** Especificação do cartão de compartilhar da retrospectiva. */
export function recapCard(r: Recap): CardSpec {
  return {
    title: recapTitle(r),
    stats: [[String(r.workouts), r.workouts === 1 ? 'treino' : 'treinos'], [String(r.daysTrained), r.daysTrained === 1 ? 'dia' : 'dias'], [String(r.sets), 'séries'], [formatVolume(r.volumeKg), 'volume']],
    footerLabel: r.topGroup ? 'Mais treinado' : undefined,
    footer: r.topGroup ? `${r.topGroup} · ${r.topGroupWorkouts}x` : undefined,
  }
}

export const shareRecap = (r: Recap) => shareCard(recapCard(r), 'gymtracker_retrospectiva.png')
