/**
 * Cadência e tempo sob tensão (#581). Cadência "3-1-2-0" = descida 3 s, pausa embaixo 1 s, subida 2 s, pausa em cima 0 s.
 * TUT (time under tension) da série = repetições × soma da cadência. Guardado no aparelho, por série.
 */
export type Tempo = [number, number, number, number]

/** Aceita "3-1-2-0", "3 1 2 0" ou "3120" (um dígito 0–9 por fase). Devolve null se inválido. */
export function parseTempo(text: string): Tempo | null {
  const t = text.trim()
  if (!t) return null
  const parts = /^\d{4}$/.test(t) ? t.split('') : t.split(/[\s\-x.]+/)
  if (parts.length !== 4 || !parts.every((p) => /^\d$/.test(p))) return null
  return parts.map(Number) as Tempo
}

export const tempoText = (t: Tempo): string => t.join('-')

/** Segundos sob tensão de uma série. */
export const tutSeconds = (reps: number, t: Tempo): number => Math.max(0, Math.round(reps)) * t.reduce((a, b) => a + b, 0)

export const formatTut = (s: number): string => (s >= 60 ? `${Math.floor(s / 60)}min${String(s % 60).padStart(2, '0')}` : `${s}s`)

const MAP = 'gt-tempo' // série → "3-1-2-0"
const DEF = 'gt-tempo-ex' // exercício → cadência padrão

export function tempoOfSet(setId: string): Tempo | null {
  try { return parseTempo(JSON.parse(localStorage.getItem(MAP) ?? '{}')[setId] ?? '') } catch { return null }
}
export function saveTempoOfSet(setId: string, t: Tempo) {
  try { const m = JSON.parse(localStorage.getItem(MAP) ?? '{}'); m[setId] = tempoText(t); localStorage.setItem(MAP, JSON.stringify(m)) } catch { /* sem armazenamento */ }
}
export function defaultTempo(exerciseId: string): string {
  try { return JSON.parse(localStorage.getItem(DEF) ?? '{}')[exerciseId] ?? '' } catch { return '' }
}
export function saveDefaultTempo(exerciseId: string, text: string) {
  try { const m = JSON.parse(localStorage.getItem(DEF) ?? '{}'); text ? (m[exerciseId] = text) : delete m[exerciseId]; localStorage.setItem(DEF, JSON.stringify(m)) } catch { /* sem armazenamento */ }
}
