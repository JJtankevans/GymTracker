import { describe, expect, it } from 'vitest'
import { platesFor, roundTo, warmupFor } from './tools'

describe('calculadora de anilhas', () => {
  it('100 kg com barra de 20: 40 de cada lado = 25 + 15', () => {
    const p = platesFor(100)
    expect(p.perSide).toEqual([{ plate: 25, count: 1 }, { plate: 15, count: 1 }])
    expect(p.achieved).toBe(100)
    expect(p.missing).toBe(0)
  })
  it('usa as pequenas e soma certo (72,5 = 20 + 2 x 26,25)', () => {
    const p = platesFor(72.5)
    expect(p.perSide).toEqual([{ plate: 25, count: 1 }, { plate: 1.25, count: 1 }])
    expect(p.achieved).toBe(72.5)
  })
  it('só a barra, ou menos que a barra', () => {
    expect(platesFor(20).perSide).toEqual([])
    expect(platesFor(15).achieved).toBe(20)
  })
  it('quando não dá exato, diz quanto falta', () => {
    const p = platesFor(101) // lado = 40,5: sobra 0,5 que não há anilha
    expect(p.achieved).toBe(100)
    expect(p.missing).toBe(1)
  })
  it('barra de 15 e vários pares da mesma anilha', () => {
    const p = platesFor(215, 15)
    expect(p.perSide).toEqual([{ plate: 25, count: 4 }])
    expect(p.achieved).toBe(215)
  })
})

describe('aquecimento', () => {
  it('rampa até a carga de trabalho sem repetir degrau', () => {
    expect(warmupFor(100)).toEqual([
      { kg: 20, reps: 10, label: 'Barra' }, { kg: 50, reps: 8, label: '50%' }, { kg: 70, reps: 5, label: '70%' }, { kg: 85, reps: 3, label: '85%' },
    ])
  })
  it('carga leve não pede aquecimento', () => {
    expect(warmupFor(22)).toEqual([])
    expect(warmupFor(20)).toEqual([])
  })
  it('carga média pula degraus que colidem', () => {
    const w = warmupFor(30) // 50% = 15 -> barra (20) já existe; 70% = 21 -> 20; 85% = 25,5 -> 25
    expect(w.map((s) => s.kg)).toEqual([20, 25])
  })
  it('arredonda para o degrau', () => {
    expect(roundTo(51.2)).toBe(50)
    expect(roundTo(52)).toBe(52.5)
  })
})

import { repMaxTable } from './tools'
describe('tabela de repetições', () => {
  it('1 rep é o próprio 1RM e as cargas caem com as reps', () => {
    const t = repMaxTable(100)
    expect(t[0]).toEqual({ reps: 1, kg: 100, percent: 100 })
    expect(t.find((r) => r.reps === 10)).toEqual({ reps: 10, kg: 75, percent: 75 })
    expect(t.map((r) => r.kg)).toEqual([...t.map((r) => r.kg)].sort((a, b) => b - a))
  })
  it('1RM inválido não gera tabela', () => {
    expect(repMaxTable(0)).toEqual([])
    expect(repMaxTable(NaN)).toEqual([])
  })
})
