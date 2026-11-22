import { describe, expect, it } from 'vitest'
import { buildSnapshot, normalizeCode, parseSharedExercises } from './share'

describe('compartilhar treino', () => {
  it('normaliza o código', () => {
    expect(normalizeCode(' ab12 cd34 ')).toBe('AB12CD34')
  })
  it('monta o snapshot com limites', () => {
    const many = Array.from({ length: 60 }, (_, i) => ({ id: String(i), name: `Ex ${i}`, default_sets: 3, default_reps: 10 }))
    expect(buildSnapshot(many)).toHaveLength(40)
    expect(buildSnapshot([{ id: 'a', name: ' Supino ', default_sets: 99, default_reps: 0 }])).toEqual([{ name: 'Supino', sets: 20, reps: 1 }])
    expect(buildSnapshot([{ id: 'a', name: '   ', default_sets: 3, default_reps: 10 }])).toEqual([])
  })
  it('valida o que vem do banco', () => {
    expect(parseSharedExercises('lixo')).toEqual([])
    expect(parseSharedExercises([{ name: 'Remada', sets: '4', reps: 10 }, { sets: 3 }, null, { name: 'Rosca', sets: 'x', reps: 500 }])).toEqual([
      { name: 'Remada', sets: 4, reps: 10 },
      { name: 'Rosca', sets: 3, reps: 100 },
    ])
  })
})
