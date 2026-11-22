import { describe, expect, it } from 'vitest'
import { energyCompare } from './wellness'
import type { HistItem } from './data'

const w = (iso: string, kg: number): HistItem => ({
  id: iso, muscle_group_id: null, muscle_group_name: null, started_at: iso, finished_at: iso,
  sets: [{ id: iso + 's', reps: 10, weight_kg: kg, set_number: 1, completed_at: iso, exercise: { name: 'X' } }],
}) as HistItem
const d = (day: number) => new Date(2026, 10, day, 10).toISOString()
const key = (day: number) => `2026-11-${String(day).padStart(2, '0')}`

describe('energia x desempenho', () => {
  it('compara o volume médio por nível de energia', () => {
    const items = [w(d(1), 100), w(d(2), 120), w(d(3), 50), w(d(4), 70)]
    const map = { [key(1)]: { energy: 5 }, [key(2)]: { energy: 4 }, [key(3)]: { energy: 1 }, [key(4)]: { energy: 2 } }
    const r = energyCompare(items, map)!
    expect(r.highVolumeKg).toBe(1100); expect(r.lowVolumeKg).toBe(600)
  })
  it('sem dados suficientes devolve null', () => {
    expect(energyCompare([w(d(1), 100)], { [key(1)]: { energy: 5 } })).toBeNull()
  })
})
