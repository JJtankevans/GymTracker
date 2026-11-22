import { describe, expect, it } from 'vitest'
import { buildRecap, formatVolume, recapTitle } from './recap'
import type { HistItem } from './data'

const w = (id: string, iso: string, group: string | null, sets: [number, number][]): HistItem => ({
  id, muscle_group_id: null, muscle_group_name: group, started_at: iso, finished_at: iso,
  sets: sets.map(([reps, kg], i) => ({ id: id + i, reps, weight_kg: kg, set_number: i + 1, completed_at: iso, exercise: { name: 'X' } })),
})

describe('retrospectiva', () => {
  const items = [
    w('a', '2026-11-03T15:00:00', 'Peito', [[10, 50], [10, 50]]),
    w('b', '2026-11-03T19:00:00', 'Peito', [[8, 100]]),
    w('c', '2026-11-10T15:00:00', 'Costas', [[10, 40]]),
    w('d', '2026-10-10T15:00:00', 'Ombro', [[10, 20]]),
    w('e', '2025-11-10T15:00:00', null, [[5, 5]]),
  ]
  it('soma o mês: treinos, dias, séries, volume e grupo mais frequente', () => {
    const r = buildRecap(items, 2026, 11)
    expect(r).toMatchObject({ workouts: 3, daysTrained: 2, sets: 4, reps: 38, volumeKg: 2200, topGroup: 'Peito', topGroupWorkouts: 2 })
  })
  it('mês 0 soma o ano inteiro e ignora outros anos', () => {
    const r = buildRecap(items, 2026, 0)
    expect(r.workouts).toBe(4)
    expect(r.volumeKg).toBe(2400)
  })
  it('mês vazio não tem grupo', () => {
    expect(buildRecap(items, 2026, 1)).toMatchObject({ workouts: 0, topGroup: null })
  })
  it('título e volume', () => {
    expect(recapTitle(buildRecap(items, 2026, 11))).toBe('Retrospectiva de novembro')
    expect(recapTitle(buildRecap(items, 2026, 0))).toBe('Retrospectiva de 2026')
    expect(formatVolume(2200)).toBe('2,2 t')
    expect(formatVolume(850)).toBe('850 kg')
  })
})
