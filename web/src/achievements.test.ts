import { describe, expect, it } from 'vitest'
import { computeAchievements, longestStreak, oneRmSeries, recordsOf, weeksMeetingGoal } from './achievements'

const d = (y: number, m: number, day: number, h = 18) => new Date(y, m - 1, day, h)

describe('conquistas', () => {
  it('maior sequência', () => expect(longestStreak([d(2026, 11, 1), d(2026, 11, 2), d(2026, 11, 3), d(2026, 11, 5)])).toBe(3))
  it('sem datas: 0', () => expect(longestStreak([])).toBe(0))
  it('semanas com a meta', () => expect(weeksMeetingGoal([d(2026, 11, 16), d(2026, 11, 17), d(2026, 11, 18), d(2026, 11, 9)], 3)).toBe(1))
  it('níveis e especiais', () => {
    const sessions = [{ start: d(2026, 11, 16, 6), minutes: 100 }, { start: d(2026, 11, 22, 22), minutes: 50 }]
    const a = computeAchievements({ sessions, weeklyGoal: 3, totalVolumeKg: 25000, exercisesWithRecord: 2 })
    expect(a.find((x) => x.id === 'WORKOUTS_1')!.progress).toBe(2)
    expect(a.find((x) => x.id === 'VOLUME_10')!.progress).toBe(25)
    expect(a.find((x) => x.id === 'SPECIAL_EARLY')!.progress).toBe(1)
    expect(a.find((x) => x.id === 'SPECIAL_MONDAY')!.progress).toBe(1)
    expect(a.find((x) => x.id === 'SPECIAL_WEEKEND')!.progress).toBe(1)
    expect(a.find((x) => x.id === 'SPECIAL_LONG')!.progress).toBe(1)
  })
  it('sem sessões, sem especiais; sem volume, a família some', () => {
    const a = computeAchievements({ sessions: [], weeklyGoal: 3, totalVolumeKg: null, exercisesWithRecord: null })
    expect(a.some((x) => x.family === 'SPECIAL' || x.family === 'VOLUME' || x.family === 'RECORDS')).toBe(false)
  })
})

describe('recordes', () => {
  const sets = [
    { exercise: 'Supino', reps: 10, kg: 60, at: '2026-11-01T10:00:00Z' },
    { exercise: 'Supino', reps: 5, kg: 80, at: '2026-11-08T10:00:00Z' },
    { exercise: 'Remada', reps: 12, kg: 0, at: '2026-11-01T10:00:00Z' },
  ]
  it('melhor carga e 1RM; peso zero não conta', () => {
    const r = recordsOf(sets)
    expect(r).toHaveLength(1)
    expect(r[0].bestKg).toBe(80)
    expect(r[0].bestOneRm).toBeCloseTo(80 * (1 + 5 / 30))
  })
  it('série de 1RM por dia', () => expect(oneRmSeries(sets, 'Supino')).toHaveLength(2))
})

import { isNewRecord } from './achievements'
describe('novo recorde ao registrar série', () => {
  it('só quando a carga passa do melhor anterior', () => {
    expect(isNewRecord(80, 82.5, 5)).toBe(true)
    expect(isNewRecord(80, 80, 5)).toBe(false)
    expect(isNewRecord(80, 70, 12)).toBe(false)
  })
  it('primeira vez do exercício ou série sem repetições não é recorde', () => {
    expect(isNewRecord(undefined, 50, 8)).toBe(false)
    expect(isNewRecord(0, 50, 8)).toBe(false)
    expect(isNewRecord(40, 50, 0)).toBe(false)
  })
})
