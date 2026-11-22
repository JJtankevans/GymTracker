// Temas da web (subconjunto dos 31 do app Android). Cada tema tem tokens por modo; o teste de contraste (themes.test.ts)
// garante: texto >= 4,5:1, texto de apoio >= 3:1 e texto do botão de destaque >= 4,5:1.

export interface Tokens {
  bg: string; surface: string; surface2: string; line: string; text: string; muted: string
  accent: string; onAccent: string; danger: string
  radius: string; font: string; fontH: string
}
export interface Theme { id: string; name: string; tagline: string; dark: Tokens; light: Tokens }

const SANS = "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Inter, system-ui, sans-serif"
const SERIF = "'Iowan Old Style', 'Palatino Linotype', Palatino, Georgia, 'Times New Roman', serif"
const MONO = "ui-monospace, 'SF Mono', Menlo, Consolas, 'Courier New', monospace"

const carbonDark: Tokens = { bg: '#0c0d0f', surface: '#16181b', surface2: '#1e2125', line: '#2a2d30', text: '#f4f5f2', muted: '#8e948f', accent: '#c6f24e', onAccent: '#10130a', danger: '#ff6b5e', radius: '18px', font: SANS, fontH: SANS }
const carbonLight: Tokens = { bg: '#f5f6f3', surface: '#ffffff', surface2: '#ecefe9', line: '#dde0d8', text: '#15180f', muted: '#5b6157', accent: '#5b7f00', onAccent: '#ffffff', danger: '#b3261e', radius: '18px', font: SANS, fontH: SANS }

export const THEMES: Theme[] = [
  { id: 'carbon', name: 'Carbon', tagline: 'Grafite com verde-limão. Cara de academia', dark: carbonDark, light: carbonLight },
  {
    id: 'studio', name: 'Studio', tagline: 'Limpo, índigo',
    light: { bg: '#f6f7fb', surface: '#ffffff', surface2: '#eceef7', line: '#dcdfee', text: '#11142b', muted: '#565b78', accent: '#3f51b5', onAccent: '#ffffff', danger: '#b3261e', radius: '14px', font: SANS, fontH: SANS },
    dark: { bg: '#0f1226', surface: '#171a33', surface2: '#1f2342', line: '#2b3052', text: '#f1f2fa', muted: '#9aa0c4', accent: '#8c9bff', onAccent: '#0b0e24', danger: '#ff7b70', radius: '14px', font: SANS, fontH: SANS },
  },
  {
    id: 'papel', name: 'Papel', tagline: 'Editorial, serifado, tinta vermelha',
    light: { bg: '#f2ecdf', surface: '#f8f4ea', surface2: '#e9e2d1', line: '#d6cdb8', text: '#1b1a17', muted: '#625d50', accent: '#b3261e', onAccent: '#ffffff', danger: '#8f1d17', radius: '4px', font: SANS, fontH: SERIF },
    dark: { bg: '#1b1a17', surface: '#24221e', surface2: '#2d2a25', line: '#3d3932', text: '#f2ecdf', muted: '#a9a28f', accent: '#e8695f', onAccent: '#1b1a17', danger: '#ff8a80', radius: '4px', font: SANS, fontH: SERIF },
  },
  {
    id: 'terminal', name: 'Terminal', tagline: 'Fósforo verde num monitor',
    dark: { bg: '#000000', surface: '#06130a', surface2: '#0b2013', line: '#145a2a', text: '#39ff6e', muted: '#1fa846', accent: '#ffb000', onAccent: '#000000', danger: '#ff5a4d', radius: '2px', font: MONO, fontH: MONO },
    light: { bg: '#000000', surface: '#06130a', surface2: '#0b2013', line: '#145a2a', text: '#39ff6e', muted: '#1fa846', accent: '#ffb000', onAccent: '#000000', danger: '#ff5a4d', radius: '2px', font: MONO, fontH: MONO },
  },
]

export type Mode = 'auto' | 'dark' | 'light'

export function resolveMode(mode: Mode, prefersDark: boolean): 'dark' | 'light' { return mode === 'auto' ? (prefersDark ? 'dark' : 'light') : mode }

/** Aplica tokens como variáveis CSS na raiz (vencem o CSS padrão). */
export function applyTheme(themeId: string, mode: Mode): void {
  const theme = THEMES.find((t) => t.id === themeId) ?? THEMES[0]
  const m = resolveMode(mode, typeof matchMedia !== 'undefined' && matchMedia('(prefers-color-scheme: dark)').matches)
  const t = theme[m]
  const root = document.documentElement
  const map: Record<string, string> = {
    '--bg': t.bg, '--surface': t.surface, '--surface-2': t.surface2, '--line': t.line, '--text': t.text, '--muted': t.muted,
    '--accent': t.accent, '--on-accent': t.onAccent, '--danger': t.danger, '--radius': t.radius, '--font': t.font, '--font-h': t.fontH,
  }
  for (const [k, v] of Object.entries(map)) root.style.setProperty(k, v)
  root.style.colorScheme = m
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', t.bg)
  try { localStorage.setItem('gt-theme', themeId); localStorage.setItem('gt-mode', mode) } catch { /* sem armazenamento */ }
}

export function savedAppearance(): { theme: string; mode: Mode } {
  try { return { theme: localStorage.getItem('gt-theme') ?? 'carbon', mode: (localStorage.getItem('gt-mode') as Mode | null) ?? 'auto' } } catch { return { theme: 'carbon', mode: 'auto' } }
}
export function restoreAppearance(): void { const a = savedAppearance(); applyTheme(a.theme, a.mode) }

// ---------- contraste WCAG (usado pelo teste e disponível para ferramentas) ----------
const lin = (c: number) => { const s = c / 255; return s <= 0.03928 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4 }
export function luminance(hex: string): number {
  const n = parseInt(hex.slice(1), 16)
  return 0.2126 * lin((n >> 16) & 255) + 0.7152 * lin((n >> 8) & 255) + 0.0722 * lin(n & 255)
}
export function contrast(a: string, b: string): number {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x)
  return (hi + 0.05) / (lo + 0.05)
}
