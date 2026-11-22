import { describe, expect, it } from 'vitest'
import { muscleOf, weeklySetsByMuscle } from './muscle'

const NOW = new Date('2026-11-22T12:00:00Z').getTime()
const set = (exercise: string, daysAgo: number, reps = 10) => ({ exercise, reps, kg: 50, at: new Date(NOW - daysAgo * 86_400_000).toISOString() })

describe('mapa muscular', () => {
  it('acha o músculo pelo nome, sem acento nem caixa', () => {
    expect(muscleOf('supino reto com barra')).toBe('Peito')
    expect(muscleOf('Exercício inventado')).toBeNull()
  })
  it('conta só os últimos 7 dias, ignora reps 0 e lista todos os músculos', () => {
    const rows = weeklySetsByMuscle([set('Supino Reto com Barra', 1), set('Supino Reto com Barra', 2), set('Supino Reto com Barra', 9), set('Supino Reto com Barra', 1, 0), set('Xyz', 1)], NOW)
    expect(rows[0]).toEqual({ muscle: 'Peito', sets: 2 })
    expect(rows.find((r) => r.muscle === null)?.sets).toBe(1)
    expect(rows.filter((r) => r.muscle && r.sets === 0).length).toBe(9)
  })
})
