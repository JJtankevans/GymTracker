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

import { buildYearReview } from './recap'
describe('meu ano', () => {
  const items = [
    w('a', '2026-03-02T12:00:00', 'Peito', []), w('b', '2026-03-03T12:00:00', 'Peito', []), w('c', '2026-03-04T12:00:00', 'Costas', []),
    w('c2', '2026-03-04T20:00:00', 'Costas', []), w('d', '2026-03-09T12:00:00', 'Peito', []), w('e', '2025-03-09T12:00:00', 'Peito', []),
  ]
  it('barras por mês, melhor mês e grupos', () => {
    const r = buildYearReview(items, 2026)
    expect(r.perMonth[2]).toBe(5)
    expect(r.perMonth.length).toBe(12)
    expect(r.bestMonth).toBe(3)
    expect(r.topGroups[0]).toEqual({ name: 'Peito', workouts: 3 })
  })
  it('dia favorito e maior sequência de dias seguidos', () => {
    const r = buildYearReview(items, 2026)
    expect(r.favoriteWeekday).toBe(1) // segundas: 02 e 09
    expect(r.longestStreakDays).toBe(3) // 02, 03, 04
  })
  it('ano vazio', () => {
    const r = buildYearReview([], 2026)
    expect(r.bestMonth).toBeNull()
    expect(r.favoriteWeekday).toBeNull()
    expect(r.longestStreakDays).toBe(0)
  })
})

import { compareWeeks } from './recap'
describe('compareWeeks', () => {
  it('separa semana atual e anterior', () => {
    const mk = (iso: string, reps: number, kg: number) => ({ id: iso, started_at: iso, finished_at: iso, muscle_group_id: null, muscle_group_name: null, sets: [{ reps, weight_kg: kg }] }) as never
    const now = new Date(2026, 10, 18, 12) // quarta
    const r = compareWeeks([mk(new Date(2026, 10, 17, 9).toISOString(), 10, 20), mk(new Date(2026, 10, 11, 9).toISOString(), 5, 40), mk(new Date(2026, 9, 1).toISOString(), 1, 1)], now)
    expect(r.workouts).toEqual([1, 1]); expect(r.volumeKg).toEqual([200, 200]); expect(r.sets).toEqual([1, 1])
  })
})
