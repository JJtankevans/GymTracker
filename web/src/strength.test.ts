import { describe, expect, it } from 'vitest'
import { levelFor, liftFor, nextLevel, oneRepMaxByLevel } from './strength'

describe('padrões de força', () => {
  it('1RM de referência homem 80 kg no supino', () => {
    const r = oneRepMaxByLevel('BENCH', 80, 'MALE')
    expect(r[0]).toBeCloseTo(54.4)
    expect(r[4]).toBeCloseTo(143.2)
  })
  it('sem sexo usa a média', () => expect(oneRepMaxByLevel('BENCH', 100, null)[2]).toBeCloseTo((1.13 + 0.7) / 2 * 100))
  it('nível alcançado e próximo', () => {
    expect(levelFor('BENCH', 50, 80, 'MALE')).toBeNull()
    expect(levelFor('BENCH', 80, 80, 'MALE')?.id).toBe('NOVICE')
    expect(nextLevel(null)?.id).toBe('BEGINNER')
    expect(nextLevel(levelFor('BENCH', 80, 80, 'MALE'))?.id).toBe('INTERMEDIATE')
    expect(nextLevel(levelFor('BENCH', 200, 80, 'MALE'))).toBeNull()
  })
  it('reconhece só os 4 básicos', () => {
    expect(liftFor('Supino Reto com Barra')).toBe('BENCH')
    expect(liftFor('Supino Inclinado com Halteres')).toBeNull()
    expect(liftFor('Agachamento Livre')).toBe('SQUAT')
    expect(liftFor('Agachamento Búlgaro')).toBeNull()
    expect(liftFor('Levantamento Terra')).toBe('DEADLIFT')
    expect(liftFor('Stiff com Barra')).toBeNull()
    expect(liftFor('Desenvolvimento com Barra')).toBe('OVERHEAD')
    expect(liftFor('Rosca Direta')).toBeNull()
  })
})
