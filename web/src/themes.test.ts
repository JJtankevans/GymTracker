import { describe, expect, it } from 'vitest'
import { contrast, resolveMode, THEMES } from './themes'

describe('contraste dos temas (WCAG)', () => {
  for (const theme of THEMES) {
    for (const mode of ['dark', 'light'] as const) {
      const t = theme[mode]
      const name = `${theme.name} (${mode})`
      it(`${name}: texto sobre fundo e cartão >= 4,5:1`, () => {
        expect(contrast(t.text, t.bg)).toBeGreaterThanOrEqual(4.5)
        expect(contrast(t.text, t.surface)).toBeGreaterThanOrEqual(4.5)
        expect(contrast(t.text, t.surface2)).toBeGreaterThanOrEqual(4.5)
      })
      it(`${name}: texto de apoio >= 3:1 no fundo e no cartão`, () => {
        expect(contrast(t.muted, t.bg)).toBeGreaterThanOrEqual(3)
        expect(contrast(t.muted, t.surface)).toBeGreaterThanOrEqual(3)
      })
      it(`${name}: texto do botão de destaque >= 4,5:1 e destaque visível (>= 3:1)`, () => {
        expect(contrast(t.onAccent, t.accent)).toBeGreaterThanOrEqual(4.5)
        expect(contrast(t.accent, t.bg)).toBeGreaterThanOrEqual(3)
        expect(contrast(t.accent, t.surface)).toBeGreaterThanOrEqual(3)
      })
      it(`${name}: erro legível >= 3:1`, () => expect(contrast(t.danger, t.surface)).toBeGreaterThanOrEqual(3))
    }
  }
  it('ids únicos e modo automático', () => {
    expect(new Set(THEMES.map((t) => t.id)).size).toBe(THEMES.length)
    expect(resolveMode('auto', true)).toBe('dark')
    expect(resolveMode('light', true)).toBe('light')
  })
})
