import { describe, expect, it } from 'vitest'
import { groupLabel, groupOf, nextInRound, supersetGroups, toggleLink } from './superset'
import { formatTut, parseTempo, tutSeconds } from './tempo'

describe('supersérie', () => {
  const ids = ['a', 'b', 'c', 'd']
  it('agrupa exercícios consecutivos ligados', () => {
    expect(supersetGroups(ids, { a: true, c: true })).toEqual([['a', 'b'], ['c', 'd']])
    expect(supersetGroups(ids, { a: true, b: true })).toEqual([['a', 'b', 'c'], ['d']])
    expect(supersetGroups(ids, {})).toEqual([['a'], ['b'], ['c'], ['d']])
  })
  it('nomeia o grupo', () => {
    expect([1, 2, 3, 4].map(groupLabel)).toEqual([null, 'Bi-set', 'Tri-set', 'Circuito'])
  })
  it('depois da série, indica o próximo da rodada; o último descansa', () => {
    const g = supersetGroups(ids, { a: true, b: true })
    expect(nextInRound(g, 'a')).toBe('b')
    expect(nextInRound(g, 'b')).toBe('c')
    expect(nextInRound(g, 'c')).toBeNull()
    expect(nextInRound(g, 'd')).toBeNull()
    expect(groupOf(g, 'b')).toEqual(['a', 'b', 'c'])
  })
  it('o último da lista não liga a nada', () => {
    expect(toggleLink(ids, {}, 'd')).toEqual({})
    expect(toggleLink(ids, {}, 'a')).toEqual({ a: true })
    expect(toggleLink(ids, { a: true }, 'a')).toEqual({ a: false })
  })
})

describe('cadência e tempo sob tensão', () => {
  it('aceita formatos comuns e rejeita inválidos', () => {
    expect(parseTempo('3-1-2-0')).toEqual([3, 1, 2, 0])
    expect(parseTempo('3 1 2 0')).toEqual([3, 1, 2, 0])
    expect(parseTempo('3120')).toEqual([3, 1, 2, 0])
    expect(parseTempo('3-1-2')).toBeNull()
    expect(parseTempo('a-b-c-d')).toBeNull()
    expect(parseTempo('')).toBeNull()
    expect(parseTempo('10-1-2-0')).toBeNull()
  })
  it('calcula o TUT da série', () => {
    expect(tutSeconds(10, [3, 1, 2, 0])).toBe(60)
    expect(tutSeconds(0, [3, 1, 2, 0])).toBe(0)
    expect(formatTut(45)).toBe('45s')
    expect(formatTut(75)).toBe('1min15')
  })
})
