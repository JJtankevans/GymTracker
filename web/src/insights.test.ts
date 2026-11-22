import { describe, expect, it } from 'vitest'
import { bandRecords, compareWorkouts, estimateKcal, heatmap, historyCsv, imbalances, plateaus, searchHistory } from './insights'
import type { HistItem } from './data'

const DAY = 86_400_000
const now = new Date(2026, 10, 22, 12).getTime()
const at = (daysAgo: number) => new Date(now - daysAgo * DAY).toISOString()
const set = (exercise: string, reps: number, kg: number, daysAgo: number) => ({ exercise, reps, kg, at: at(daysAgo) })
const hist = (id: string, daysAgo: number, sets: [string, number, number][]): HistItem => ({
  id, muscle_group_id: null, muscle_group_name: 'Peito', started_at: at(daysAgo), finished_at: at(daysAgo),
  sets: sets.map(([n, reps, kg], i) => ({ id: `${id}${i}`, reps, weight_kg: kg, set_number: i + 1, completed_at: at(daysAgo), exercise: { name: n } })),
}) as HistItem

describe('insights', () => {
  it('recorde por faixa usa a maior carga com pelo menos N reps', () => {
    const r = bandRecords([set('Supino', 10, 60, 5), set('Supino', 5, 80, 4), set('Supino', 1, 100, 3)])
    const kg = (b: number) => r.find((x) => x.band === b)!.kg
    expect([kg(1), kg(5), kg(10)]).toEqual([100, 80, 60])
  })
  it('calorias', () => { expect(estimateKcal(60, 80)).toBe(400); expect(estimateKcal(-5, 80)).toBe(0) })
  it('desequilíbrio avisa quando um lado passa do dobro', () => {
    const s = Array.from({ length: 12 }, () => set('Supino Reto com Barra', 10, 50, 2))
    const r = imbalances(s, now)
    expect(r[0].setsA).toBe(12); expect(r[0].setsB).toBe(0); expect(r[0].warn).toBe(true)
    expect(r[1].warn).toBe(false)
  })
  it('platô: sem recorde há 5 semanas mas treinado recentemente', () => {
    const s = [set('Agachamento', 5, 100, 40), set('Agachamento', 5, 90, 3)]
    const p = plateaus(s, now)
    expect(p).toHaveLength(1); expect(p[0].weeks).toBe(5)
    expect(plateaus([set('Agachamento', 5, 90, 40), set('Agachamento', 5, 100, 3)], now)).toHaveLength(0)
  })
  it('mapa de calor: 26 colunas de 7 dias e dia futuro zerado', () => {
    const h = heatmap([hist('a', 0, [['X', 10, 10]])], new Date(now))
    expect(h).toHaveLength(26); expect(h[0]).toHaveLength(7)
    expect(h.flat().filter((c) => c.count > 0)).toHaveLength(1)
  })
  it('CSV com BOM, ; e vírgula decimal', () => {
    const csv = historyCsv([hist('a', 1, [['Supino', 10, 52.5]])])
    expect(csv.startsWith('﻿data;grupo')).toBe(true)
    expect(csv).toContain(';Supino;1;10;52,5')
  })
  it('compara treinos pelo melhor 1RM de cada exercício', () => {
    const d = compareWorkouts(hist('a', 9, [['Supino', 10, 50]]), hist('b', 2, [['Supino', 10, 55], ['Remada', 8, 40]]))
    expect(d.find((x) => x.exercise === 'Supino')).toMatchObject({ a: { kg: 50 }, b: { kg: 55 } })
    expect(d.find((x) => x.exercise === 'Remada')!.a).toBeNull()
  })
  it('pesquisa por nome sem acento e por carga', () => {
    const h = [hist('a', 5, [['Rosca direta', 10, 20], ['Supino', 8, 80]])]
    expect(searchHistory(h, 'ROSCA')).toHaveLength(1)
    expect(searchHistory(h, '80kg')).toHaveLength(1)
    expect(searchHistory(h, '')).toHaveLength(0)
  })
})

import { needsDeload, nextProgression, weeklyVolumes } from './insights'
describe('progressão e descarga', () => {
  it('sobe a carga ao bater as reps, senão pede +1 rep', () => {
    expect(nextProgression({ kg: 40, reps: 12 }, 12)).toMatchObject({ kg: 42.5, reps: 12 })
    expect(nextProgression({ kg: 80, reps: 8 }, 8)).toMatchObject({ kg: 85 })
    expect(nextProgression({ kg: 40, reps: 9 }, 12)).toMatchObject({ kg: 40, reps: 10 })
    expect(nextProgression(undefined, 10)).toBeNull()
  })
  it('descarga: 4 semanas acima da mediana', () => {
    expect(needsDeload([5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 9, 9])).toBe(true)
    expect(needsDeload([5, 5, 5, 5, 5, 5, 5, 5, 9, 9, 5, 9])).toBe(false)
    expect(needsDeload([9, 9, 9])).toBe(false)
  })
  it('volume por semana', () => {
    const v = weeklyVolumes([hist('a', 0, [['X', 10, 10]]), hist('b', 8, [['X', 10, 20]])], new Date(now))
    expect(v).toHaveLength(12); expect(v[11]).toBe(100); expect(v[10]).toBe(200)
  })
})

import { monthlyBadges } from './insights'
describe('metas do mês', () => {
  it('conta treinos, dias e toneladas só do mês corrente', () => {
    const b = monthlyBadges([hist('a', 1, [['X', 10, 100]]), hist('b', 40, [['X', 10, 100]])], 3, new Date(now))
    expect(b).toHaveLength(3); expect(b[0].target).toBe(12); expect(b[0].progress).toBe(1); expect(b[1].progress).toBe(1)
  })
})

import { exerciseWeeklyVolume } from './insights'
it('volume semanal de um exercício', () => {
  const v = exerciseWeeklyVolume([hist('a', 0, [['X', 10, 10], ['Y', 5, 5]])], 'X', new Date(now), 4)
  expect(v).toEqual([0, 0, 0, 100])
})

import { equipmentSets, projectWeight, variationsFor } from './insights'
describe('variações, projeção e equipamento', () => {
  it('variações: mesmo músculo, outro equipamento primeiro', () => {
    const v = variationsFor('Supino Reto com Barra')
    expect(v.length).toBeGreaterThan(0); expect(v).not.toContain('Supino Reto com Barra')
    expect(variationsFor('Exercício que não existe')).toEqual([])
  })
  it('projeção de peso: perda de 0,5 kg/semana', () => {
    const base = new Date(2026, 10, 22)
    const pts = [0, 1, 2, 3].map((w) => { const d = new Date(base); d.setDate(d.getDate() - 7 * (3 - w)); return { day: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`, kg: 80 - 0.5 * w } })
    const p = projectWeight(pts, 75, new Date(base.getTime() + 12 * 3600e3))!
    expect(p.perWeek).toBeCloseTo(-0.5, 1); expect(p.weeksLeft).toBeCloseTo(7, 0)
    expect(projectWeight(pts, 85, base)!.date).toBeNull() // meta na direção oposta
    expect(projectWeight(pts.slice(0, 2), 75, base)).toBeNull()
  })
  it('séries por equipamento', () => {
    const r = equipmentSets([set('Supino Reto com Barra', 5, 50, 1), set('Desconhecido', 5, 5, 1), set('Desconhecido', 5, 5, 1)])
    expect(r[0]).toEqual({ equipment: 'Outros', sets: 2 })
  })
})

import { challengeStatus } from './insights'
describe('desafios pessoais', () => {
  it('conta só treinos dentro dos 30 dias', () => {
    const items = [hist('a', 2, [['X', 10, 10]]), hist('b', 5, [['X', 10, 10]]), hist('c', 50, [['X', 10, 10]])]
    const s = challengeStatus({ kind: 'workouts12', start: at(10) }, items, new Date(now))
    expect(s.progress).toBe(2); expect(s.target).toBe(12); expect(s.daysLeft).toBe(20); expect(s.done).toBe(false)
    expect(challengeStatus({ kind: 'workouts12', start: at(40) }, items, new Date(now)).expired).toBe(true)
  })
})
