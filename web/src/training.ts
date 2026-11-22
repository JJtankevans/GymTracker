import type { HistItem } from './data'
import { muscleOf } from './muscle'
import { dayKey, estimateOneRepMax, weekStart } from './rules'
import type { Tempo } from './tempo'
import type { WellnessMap } from './wellness'

const DAY = 86_400_000
const read = <T,>(key: string, fallback: T): T => { try { const v = JSON.parse(localStorage.getItem(key) ?? 'null'); return v ?? fallback } catch { return fallback } }
const write = (key: string, v: unknown) => { try { localStorage.setItem(key, JSON.stringify(v)) } catch { /* sem armazenamento */ } }

// ───────── #603 / #604 marcas de série (aquecimento, drop-set, rest-pause, falha) ─────────
export type SetFlag = 'warmup' | 'drop' | 'restpause' | 'failure'
export const FLAG_LABEL: Record<SetFlag, string> = { warmup: 'Aquecimento', drop: 'Drop-set', restpause: 'Rest-pause', failure: 'Falha' }
export type FlagMap = Record<string, SetFlag>
export const loadFlags = (): FlagMap => read<FlagMap>('gt-setflag', {})
export function setFlag(setId: string, flag: SetFlag | null): FlagMap {
  const m = { ...loadFlags() }
  if (flag) m[setId] = flag; else delete m[setId]
  write('gt-setflag', m)
  return m
}
/** Séries de aquecimento não contam no volume nem nos recordes. */
export const countsForStats = (setId: string, flags: FlagMap): boolean => flags[setId] !== 'warmup'

// ───────── #599 timer de série ao vivo ─────────
export interface Phase { label: string; seconds: number; rep: number }
const PHASE_NAMES = ['Descendo', 'Pausa embaixo', 'Subindo', 'Pausa em cima']

/** Fases em ordem para [reps] repetições com a cadência dada (fases de 0 s são puladas). */
export function liveSchedule(tempo: Tempo, reps: number): Phase[] {
  const out: Phase[] = []
  for (let r = 1; r <= Math.max(0, Math.round(reps)); r++) tempo.forEach((s, i) => { if (s > 0) out.push({ label: PHASE_NAMES[i], seconds: s, rep: r }) })
  return out
}

/** Fase em andamento após [elapsed] s (null = série terminou) e quantos segundos faltam nela. */
export function phaseAt(schedule: Phase[], elapsed: number): { phase: Phase; left: number; index: number } | null {
  let t = 0
  for (let i = 0; i < schedule.length; i++) {
    if (elapsed < t + schedule[i].seconds) return { phase: schedule[i], left: Math.ceil(t + schedule[i].seconds - elapsed), index: i }
    t += schedule[i].seconds
  }
  return null
}
export const scheduleSeconds = (s: Phase[]): number => s.reduce((a, p) => a + p.seconds, 0)

// ───────── #600 TUT semanal por músculo ─────────
export interface MuscleTut { muscle: string | null; seconds: number }

/** TUT (reps × soma da cadência) dos últimos [days] dias por músculo; só entram séries com cadência registrada. */
export function weeklyTutByMuscle(items: HistItem[], tempoOf: (setId: string) => Tempo | null, flags: FlagMap = {}, now = Date.now(), days = 7): MuscleTut[] {
  const from = now - days * DAY
  const by = new Map<string | null, number>()
  for (const w of items) for (const s of w.sets) {
    const t = new Date(s.completed_at).getTime()
    if (!(t >= from && t <= now) || !countsForStats(s.id, flags)) continue
    const tempo = tempoOf(s.id)
    if (!tempo) continue
    const m = s.exercise ? muscleOf(s.exercise.name) : null
    by.set(m, (by.get(m) ?? 0) + Math.max(0, s.reps) * tempo.reduce((a, b) => a + b, 0))
  }
  return [...by].map(([muscle, seconds]) => ({ muscle, seconds })).sort((a, b) => b.seconds - a.seconds)
}

// ───────── #605 1RM estimado ao vivo ─────────
/** 1RM estimado da série digitada (Epley); null se faltar dado. */
export const liveOneRm = (kg: number, reps: number): number | null => (kg > 0 && reps >= 1 ? estimateOneRepMax(kg, reps) : null)
/** Recorde estimado: a série vale mais 1RM que o melhor 1RM estimado do exercício (e já havia um melhor). */
export const isEstimatedRecord = (bestOneRm: number | undefined, kg: number, reps: number): boolean => {
  const e = liveOneRm(kg, reps)
  return !!bestOneRm && bestOneRm > 0 && e !== null && e > bestOneRm + 0.05
}

// ───────── #606 fadiga semanal ─────────
export interface FatigueWeek { start: Date; avgRpe: number | null; avgSleep: number | null; avgEnergy: number | null; score: number | null }

/** Por semana (a atual por último): RPE médio, sono e energia; [score] 0–100 de fadiga (mais alto = mais cansado). */
export function fatigueWeeks(items: HistItem[], rpe: Record<string, number>, wellness: WellnessMap, now = new Date(), n = 6): FatigueWeek[] {
  const cur = weekStart(now).getTime()
  const weeks = Array.from({ length: n }, (_, i) => ({ start: new Date(cur - (n - 1 - i) * 7 * DAY), rpe: [] as number[], sleep: [] as number[], energy: [] as number[] }))
  const idx = (d: Date) => n - 1 - Math.round((cur - weekStart(d).getTime()) / (7 * DAY))
  for (const w of items) {
    const i = idx(new Date(w.started_at))
    if (i < 0 || i >= n) continue
    for (const s of w.sets) if (rpe[s.id]) weeks[i].rpe.push(rpe[s.id])
  }
  for (const [day, v] of Object.entries(wellness)) {
    const d = new Date(day + 'T12:00:00')
    const i = idx(d)
    if (i < 0 || i >= n) continue
    if (v.sleepH) weeks[i].sleep.push(v.sleepH)
    if (v.energy) weeks[i].energy.push(v.energy)
  }
  const avg = (xs: number[]) => (xs.length ? xs.reduce((a, b) => a + b, 0) / xs.length : null)
  return weeks.map((w) => {
    const r = avg(w.rpe), s = avg(w.sleep), e = avg(w.energy)
    const parts: number[] = []
    if (r !== null) parts.push(((r - 6) / 4) * 100) // RPE 6→0, 10→100
    if (s !== null) parts.push(((9 - Math.min(9, Math.max(5, s))) / 4) * 100) // sono 9 h→0, 5 h→100
    if (e !== null) parts.push(((5 - e) / 4) * 100) // energia 5→0, 1→100
    return { start: w.start, avgRpe: r, avgSleep: s, avgEnergy: e, score: parts.length ? Math.round(parts.reduce((a, b) => a + b, 0) / parts.length) : null }
  })
}

// ───────── #609 metas por fase ─────────
export type PhaseId = 'auto' | 'volume' | 'strength' | 'cut'
export const PHASES: Record<PhaseId, { name: string; reps: [number, number]; tip: string }> = {
  auto: { name: 'Automático', reps: [0, 0], tip: 'Usa as repetições planejadas de cada exercício.' },
  volume: { name: 'Volume', reps: [8, 12], tip: 'Hipertrofia: 8 a 12 repetições, 10 a 20 séries por músculo na semana.' },
  strength: { name: 'Força', reps: [3, 6], tip: 'Força: 3 a 6 repetições com cargas altas e descanso longo (2 a 4 min).' },
  cut: { name: 'Definição', reps: [12, 15], tip: 'Definição: manter a carga, 12 a 15 repetições e mais cardio.' },
}
export const loadPhase = (): PhaseId => { const v = read<string>('gt-phase', 'auto'); return v in PHASES ? (v as PhaseId) : 'auto' }
export const savePhase = (p: PhaseId) => write('gt-phase', p)
/** Repetições-alvo da fase; mantém o alvo do exercício quando ele já cabe na faixa da fase. */
export const phaseTargetReps = (phase: PhaseId, exerciseDefault: number): number => {
  if (phase === 'auto') return exerciseDefault
  const [lo, hi] = PHASES[phase].reps
  return exerciseDefault >= lo && exerciseDefault <= hi ? exerciseDefault : hi
}

// ───────── #610 lembrete inteligente ─────────
export interface ReminderInput { now: Date; weeklyDone: number; goal: number; trainedToday: boolean; nextGroup: string | null }
/** Mensagem quando a meta da semana ainda não foi batida e ainda há dias para cumprir; null caso contrário. */
export function smartReminder({ now, weeklyDone, goal, trainedToday, nextGroup }: ReminderInput): string | null {
  const missing = goal - weeklyDone
  if (missing <= 0 || trainedToday) return null
  const daysLeft = 7 - ((now.getDay() + 6) % 7) // hoje conta (segunda = 7 dias restantes, domingo = 1)
  if (daysLeft < missing) return `Faltam ${missing} treinos e só restam ${daysLeft} ${daysLeft === 1 ? 'dia' : 'dias'}: hoje é dia de treinar${nextGroup ? ` (${nextGroup})` : ''}.`
  if (daysLeft === missing) return `Para bater a meta, treine em todos os dias que restam, inclusive hoje${nextGroup ? ` (${nextGroup})` : ''}.`
  return `Faltam ${missing} ${missing === 1 ? 'treino' : 'treinos'} para a meta e ${daysLeft} dias pela frente. Que tal hoje${nextGroup ? `: ${nextGroup}` : ''}?`
}

// ───────── #601 cardio ─────────
export interface Cardio { id: string; day: string; kind: string; minutes: number; km?: number }
export const loadCardio = (): Cardio[] => read<Cardio[]>('gt-cardio', [])
export function addCardio(c: Omit<Cardio, 'id'>): Cardio[] {
  const all = [...loadCardio(), { ...c, id: `c${Date.now()}` }]
  write('gt-cardio', all)
  return all
}
export function removeCardio(id: string): Cardio[] {
  const all = loadCardio().filter((c) => c.id !== id)
  write('gt-cardio', all)
  return all
}
/** Minutos e km de cardio na semana corrente (segunda a domingo). */
export function weeklyCardio(all: Cardio[], now = new Date()): { minutes: number; km: number; sessions: number } {
  const from = dayKey(weekStart(now))
  const w = all.filter((c) => c.day >= from && c.day <= dayKey(now))
  return { minutes: w.reduce((a, c) => a + c.minutes, 0), km: Math.round(w.reduce((a, c) => a + (c.km ?? 0), 0) * 10) / 10, sessions: w.length }
}

// ───────── #602 modelos de treino ─────────
export interface TemplateEntry { kg?: number; reps?: number; tempo?: string }
export interface Template { id: string; name: string; groupId: string; links: Record<string, boolean>; entries: Record<string, TemplateEntry>; savedAt: string }
export const loadTemplates = (): Template[] => read<Template[]>('gt-templates', [])
export function saveTemplate(t: Omit<Template, 'id' | 'savedAt'>): Template[] {
  const all = [...loadTemplates().filter((x) => x.name !== t.name), { ...t, id: `t${Date.now()}`, savedAt: new Date().toISOString() }]
  write('gt-templates', all)
  return all
}
export function deleteTemplate(id: string): Template[] {
  const all = loadTemplates().filter((t) => t.id !== id)
  write('gt-templates', all)
  return all
}
