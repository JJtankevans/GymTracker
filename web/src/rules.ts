// Regras portadas do :shared do app Android (mesmos nomes e comportamento). Testadas em rules.test.ts.

export const IDLE_LIMIT_MS = 60 * 60 * 1000 // treino esquecido: > 1 h sem série (StaleWorkoutPolicy)
export const MAX_PLAUSIBLE_WORKOUT_MINUTES = 240

export interface GroupLite { id: string; name: string; color_hex?: string }
export interface WorkoutLite {
  id: string
  muscle_group_id: string | null
  muscle_group_name?: string | null
  started_at: string
  finished_at: string | null
}

/** Estimativa de 1RM (Epley). Para 1 rep é a própria carga. */
export const estimateOneRepMax = (kg: number, reps: number) => (reps <= 1 ? kg : kg * (1 + reps / 30))
/** Inverso de Epley: carga para N reps dado o 1RM. */
export const loadForReps = (oneRm: number, reps: number) => (reps <= 1 ? oneRm : oneRm / (1 + reps / 30))

/** Próximo grupo da rotação: um que nunca foi treinado ou, se todos já foram, o treinado há mais tempo. */
export function suggestNextGroup(groups: GroupLite[], history: WorkoutLite[]): string | null {
  const ids = groups.map((g) => g.id)
  if (ids.length < 2) return null
  const last = new Map<string, string>()
  for (const w of history) {
    if (!w.muscle_group_id || !ids.includes(w.muscle_group_id)) continue
    const prev = last.get(w.muscle_group_id)
    if (!prev || w.started_at > prev) last.set(w.muscle_group_id, w.started_at)
  }
  if (last.size === 0) return null
  const never = ids.find((id) => !last.has(id))
  if (never) return never
  let best: string | null = null
  for (const [id, t] of last) if (best === null || t < last.get(best)!) best = id
  return best
}

/** Duração para exibir: a gravada (se plausível) ou a calculada pelas séries (corta numa pausa > 1 h). */
export function realDurationMinutes(recorded: number | null, start: string | null, setTimes: (string | null)[]): number | null {
  if (recorded != null && recorded >= 1 && recorded <= MAX_PLAUSIBLE_WORKOUT_MINUTES) return recorded
  if (!start) return recorded
  const times = setTimes.filter((t): t is string => !!t).map((t) => Date.parse(t)).filter((n) => !Number.isNaN(n)).sort((a, b) => a - b)
  if (times.length === 0) return recorded
  let last = times[0]
  for (const t of times.slice(1)) { if (t - last > IDLE_LIMIT_MS) break; last = t }
  const mins = Math.floor((last - Date.parse(start)) / 60000)
  return mins >= 1 && mins <= MAX_PLAUSIBLE_WORKOUT_MINUTES ? mins : recorded
}

/** Treino aberto há mais de 1 h sem série? Devolve o instante em que deve ser encerrado (última série) ou null. */
export function staleEndsAt(startedAt: string, setTimes: string[], now = Date.now()): string | null {
  const times = setTimes.map((t) => Date.parse(t)).filter((n) => !Number.isNaN(n)).sort((a, b) => a - b)
  const lastActivity = times.length ? times[times.length - 1] : Date.parse(startedAt)
  if (now - lastActivity <= IDLE_LIMIT_MS) return null
  return new Date(lastActivity).toISOString()
}

// ---------- datas (semana começa na segunda, fuso do aparelho) ----------
export const dayKey = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
export function weekStart(d: Date): Date {
  const x = new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const dow = (x.getDay() + 6) % 7 // segunda = 0
  x.setDate(x.getDate() - dow)
  return x
}

export interface WeekBar { start: Date; count: number }
/** Treinos concluídos por semana nas últimas [n] semanas (a atual por último). */
export function weeklyHistory(workouts: WorkoutLite[], now = new Date(), n = 8): WeekBar[] {
  const cur = weekStart(now)
  const bars: WeekBar[] = []
  for (let i = n - 1; i >= 0; i--) { const s = new Date(cur); s.setDate(s.getDate() - 7 * i); bars.push({ start: s, count: 0 }) }
  for (const w of workouts) {
    if (!w.finished_at) continue
    const ws = weekStart(new Date(w.started_at)).getTime()
    const bar = bars.find((b) => b.start.getTime() === ws)
    if (bar) bar.count++
  }
  return bars
}

/** Dias seguidos com treino, contando até hoje ou ontem. */
export function streakDays(workouts: WorkoutLite[], now = new Date()): number {
  const days = new Set(workouts.filter((w) => w.finished_at).map((w) => dayKey(new Date(w.started_at))))
  const d = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  if (!days.has(dayKey(d))) d.setDate(d.getDate() - 1)
  let n = 0
  while (days.has(dayKey(d))) { n++; d.setDate(d.getDate() - 1) }
  return n
}

/** Nome de grupo em caixa de título, mesmo que venha em CAIXA ALTA. */
export function prettyGroupName(name: string): string {
  const small = new Set(['e', 'de', 'da', 'do', 'das', 'dos'])
  return name.trim().toLowerCase().split(/\s+/).map((w, i) => (i > 0 && small.has(w) ? w : w.charAt(0).toUpperCase() + w.slice(1))).join(' ')
}

export const formatKg = (kg: number) => (Math.round(kg * 10) / 10).toString().replace('.', ',')
