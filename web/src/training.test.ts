import { describe, expect, it } from 'vitest'
import {
  countsForStats, fatigueWeeks, isEstimatedRecord, liveOneRm, liveSchedule, phaseAt, phaseTargetReps, scheduleSeconds, smartReminder, weeklyCardio, weeklyTutByMuscle,
} from './training'
import type { HistItem } from './data'

const now = new Date(2026, 10, 22, 12) // domingo
const iso = (daysAgo: number) => new Date(now.getTime() - daysAgo * 86_400_000).toISOString()
const item = (id: string, daysAgo: number, sets: [string, string, number][]): HistItem => ({
  id, muscle_group_id: null, muscle_group_name: null, started_at: iso(daysAgo), finished_at: iso(daysAgo),
  sets: sets.map(([sid, name, reps], i) => ({ id: sid, reps, weight_kg: 50, set_number: i + 1, completed_at: iso(daysAgo), exercise: { name } })),
}) as HistItem

describe('marcas de série', () => {
  it('aquecimento não conta nas estatísticas', () => {
    expect(countsForStats('a', { a: 'warmup' })).toBe(false)
    expect(countsForStats('a', { a: 'drop' })).toBe(true)
    expect(countsForStats('b', { a: 'warmup' })).toBe(true)
  })
})

describe('timer de série ao vivo', () => {
  it('monta as fases e pula as de 0 s', () => {
    const s = liveSchedule([3, 1, 2, 0], 2)
    expect(s.map((p) => p.label)).toEqual(['Descendo', 'Pausa embaixo', 'Subindo', 'Descendo', 'Pausa embaixo', 'Subindo'])
    expect(scheduleSeconds(s)).toBe(12)
  })
  it('acha a fase pelo tempo decorrido', () => {
    const s = liveSchedule([3, 1, 2, 0], 1)
    expect(phaseAt(s, 0)).toMatchObject({ phase: { label: 'Descendo' }, left: 3 })
    expect(phaseAt(s, 3.5)).toMatchObject({ phase: { label: 'Pausa embaixo' } })
    expect(phaseAt(s, 4.5)).toMatchObject({ phase: { label: 'Subindo' }, left: 2 })
    expect(phaseAt(s, 6)).toBeNull()
  })
})

describe('TUT semanal', () => {
  it('soma por músculo e ignora série sem cadência ou de aquecimento', () => {
    const items = [item('w', 1, [['s1', 'Supino Reto com Barra', 10], ['s2', 'Supino Reto com Barra', 10], ['s3', 'Supino Reto com Barra', 10]])]
    const tempoOf = (id: string) => (id === 's3' ? null : ([3, 1, 2, 0] as [number, number, number, number]))
    const r = weeklyTutByMuscle(items, tempoOf, { s2: 'warmup' }, now.getTime())
    expect(r).toEqual([{ muscle: 'Peito', seconds: 60 }])
  })
})

describe('1RM ao vivo', () => {
  it('estima e detecta recorde estimado', () => {
    expect(liveOneRm(60, 10)).toBe(80)
    expect(liveOneRm(0, 10)).toBeNull()
    expect(isEstimatedRecord(75, 60, 10)).toBe(true)
    expect(isEstimatedRecord(85, 60, 10)).toBe(false)
    expect(isEstimatedRecord(undefined, 60, 10)).toBe(false)
  })
})

describe('fadiga semanal', () => {
  it('combina RPE, sono e energia numa nota 0–100', () => {
    const w = fatigueWeeks([item('w', 1, [['s1', 'X', 10]])], { s1: 10 }, { '2026-11-22': { sleepH: 5, energy: 1 } }, now, 2)
    expect(w[1].avgRpe).toBe(10)
    expect(w[1].score).toBe(100)
    expect(w[0].score).toBeNull()
  })
})

describe('fases', () => {
  it('mantém o alvo do exercício se cabe na faixa; senão usa o topo', () => {
    expect(phaseTargetReps('volume', 10)).toBe(10)
    expect(phaseTargetReps('strength', 10)).toBe(6)
    expect(phaseTargetReps('cut', 8)).toBe(15)
  })
})

describe('lembrete inteligente', () => {
  const base = { now, weeklyDone: 2, goal: 4, trainedToday: false, nextGroup: 'Pernas' }
  it('sem lembrete se a meta foi batida ou já treinou hoje', () => {
    expect(smartReminder({ ...base, weeklyDone: 4 })).toBeNull()
    expect(smartReminder({ ...base, trainedToday: true })).toBeNull()
  })
  it('urgente quando faltam mais treinos que dias (domingo = 1 dia)', () => {
    expect(smartReminder(base)).toContain('só restam 1 dias')
  })
  it('tom leve quando sobra tempo (segunda)', () => {
    expect(smartReminder({ ...base, now: new Date(2026, 10, 16, 9) })).toContain('Que tal hoje')
  })
})

describe('cardio', () => {
  it('soma minutos e km da semana', () => {
    const r = weeklyCardio([
      { id: '1', day: '2026-11-21', kind: 'Corrida', minutes: 30, km: 4.5 },
      { id: '2', day: '2026-11-22', kind: 'Bike', minutes: 20 },
      { id: '3', day: '2026-11-10', kind: 'Corrida', minutes: 60, km: 9 },
    ], now)
    expect(r).toEqual({ minutes: 50, km: 4.5, sessions: 2 })
  })
})
