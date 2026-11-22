import { describe, expect, it } from 'vitest'
import { estimateOneRepMax, loadForReps, prettyGroupName, realDurationMinutes, staleEndsAt, streakDays, suggestNextGroup, weeklyHistory, weekStart, type WorkoutLite } from './rules'

const w = (id: string, g: string | null, started: string, finished: string | null = started): WorkoutLite => ({ id, muscle_group_id: g, started_at: started, finished_at: finished })
const groups = [{ id: 'a', name: 'A' }, { id: 'b', name: 'B' }, { id: 'c', name: 'C' }]

describe('1RM (Epley)', () => {
  it('1 rep é a própria carga', () => expect(estimateOneRepMax(100, 1)).toBe(100))
  it('10 reps', () => expect(estimateOneRepMax(60, 10)).toBeCloseTo(80))
  it('inverso', () => expect(loadForReps(80, 10)).toBeCloseTo(60))
})

describe('suggestNextGroup', () => {
  it('sem histórico ou com 1 grupo: nada', () => {
    expect(suggestNextGroup(groups, [])).toBeNull()
    expect(suggestNextGroup([groups[0]], [w('1', 'a', '2026-11-01T10:00:00Z')])).toBeNull()
  })
  it('prefere um grupo nunca treinado', () => expect(suggestNextGroup(groups, [w('1', 'a', '2026-11-01T10:00:00Z')])).toBe('b'))
  it('se todos já foram, o mais antigo', () => {
    const h = [w('1', 'a', '2026-11-03T10:00:00Z'), w('2', 'b', '2026-11-01T10:00:00Z'), w('3', 'c', '2026-11-02T10:00:00Z')]
    expect(suggestNextGroup(groups, h)).toBe('b')
  })
})

describe('realDurationMinutes', () => {
  it('usa a gravada se plausível', () => expect(realDurationMinutes(45, '2026-11-01T10:00:00Z', [])).toBe(45))
  it('calcula pelas séries e corta na pausa > 1 h', () => {
    const sets = ['2026-11-01T10:10:00Z', '2026-11-01T10:40:00Z', '2026-11-01T13:00:00Z']
    expect(realDurationMinutes(null, '2026-11-01T10:00:00Z', sets)).toBe(40)
  })
  it('gravada absurda e sem séries devolve a gravada', () => expect(realDurationMinutes(900, '2026-11-01T10:00:00Z', [])).toBe(900))
})

describe('staleEndsAt', () => {
  const now = Date.parse('2026-11-01T14:00:00Z')
  it('ativo recentemente: null', () => expect(staleEndsAt('2026-11-01T13:00:00Z', ['2026-11-01T13:30:00Z'], now)).toBeNull())
  it('parado > 1 h: encerra na última série', () => expect(staleEndsAt('2026-11-01T10:00:00Z', ['2026-11-01T11:00:00Z'], now)).toBe('2026-11-01T11:00:00.000Z'))
  it('sem séries: encerra no início', () => expect(staleEndsAt('2026-11-01T10:00:00Z', [], now)).toBe('2026-11-01T10:00:00.000Z'))
})

describe('datas', () => {
  it('semana começa na segunda', () => {
    expect(weekStart(new Date(2026, 10, 22)).getDate()).toBe(16) // domingo 22/11 -> segunda 16/11
    expect(weekStart(new Date(2026, 10, 16)).getDate()).toBe(16)
  })
  it('histórico semanal e sequência', () => {
    const now = new Date(2026, 10, 22, 12)
    const ws = [w('1', 'a', new Date(2026, 10, 21, 10).toISOString()), w('2', 'a', new Date(2026, 10, 20, 10).toISOString()), w('3', 'b', new Date(2026, 10, 10, 10).toISOString())]
    const h = weeklyHistory(ws, now)
    expect(h).toHaveLength(8)
    expect(h[7].count).toBe(2)
    expect(h[6].count).toBe(1)
    expect(streakDays(ws, now)).toBe(2) // 21 e 20; hoje ainda não treinou
  })
})

describe('prettyGroupName', () => {
  it('normaliza CAIXA ALTA', () => expect(prettyGroupName('PEITO E TRICEPS')).toBe('Peito e Triceps'))
})

import { clampGoal } from './data'
describe('meta semanal', () => {
  it('fica entre 1 e 7 e arredonda', () => {
    expect(clampGoal(0)).toBe(1)
    expect(clampGoal(9)).toBe(7)
    expect(clampGoal(3.6)).toBe(4)
  })
})
