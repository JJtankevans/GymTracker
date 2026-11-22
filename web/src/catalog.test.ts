import { describe, expect, it } from 'vitest'
import { CATALOG, TEMPLATES } from './catalog'
import { fold, MUSCLES, searchCatalog } from './catalogSearch'

describe('catálogo gerado do app Android', () => {
  it('tem 70 exercícios únicos e 9 treinos prontos', () => {
    expect(CATALOG).toHaveLength(70)
    expect(new Set(CATALOG.map((e) => e.name)).size).toBe(70)
    expect(TEMPLATES).toHaveLength(9)
  })
  it('todo exercício dos treinos prontos existe no catálogo', () => {
    const names = new Set(CATALOG.map((e) => e.name))
    for (const t of TEMPLATES) for (const n of t.exercises) expect(names.has(n), `${t.name}: ${n}`).toBe(true)
  })
  it('dados plausíveis', () => {
    for (const e of CATALOG) { expect(e.sets).toBeGreaterThan(0); expect(e.reps).toBeGreaterThan(0); expect(e.muscle).toBeTruthy(); expect(e.equipment).toBeTruthy() }
    for (const t of TEMPLATES) expect(t.color).toMatch(/^#[0-9A-Fa-f]{6}$/)
  })
})

describe('busca', () => {
  it('ignora acento e maiúscula', () => { expect(fold('Tríceps')).toBe('triceps'); expect(searchCatalog('TRICEPS').length).toBeGreaterThan(2) })
  it('vários termos', () => expect(searchCatalog('supino barra').every((e) => /supino/i.test(e.name))).toBe(true))
  it('apelido acha o exercício', () => expect(searchCatalog('peck deck').some((e) => e.name.includes('Peck Deck'))).toBe(true))
  it('filtra por músculo', () => { const r = searchCatalog('', MUSCLES[0]); expect(r.length).toBeGreaterThan(0); expect(r.every((e) => e.muscle === MUSCLES[0])).toBe(true) })
  it('sem resultado', () => expect(searchCatalog('zzzzz')).toEqual([]))
})
