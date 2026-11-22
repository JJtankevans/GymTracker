// Porte de StrengthStandards.kt (tabelas StrengthLog, múltiplos do peso corporal). Referência, não diagnóstico.

export type Lift = 'BENCH' | 'SQUAT' | 'DEADLIFT' | 'OVERHEAD'
export type Sex = 'MALE' | 'FEMALE' | null
export const LIFT_LABEL: Record<Lift, string> = { BENCH: 'Supino', SQUAT: 'Agachamento', DEADLIFT: 'Levantamento terra', OVERHEAD: 'Desenvolvimento' }

export const LEVELS = [
  { id: 'BEGINNER', label: 'Iniciante', percentile: 5 },
  { id: 'NOVICE', label: 'Novato', percentile: 25 },
  { id: 'INTERMEDIATE', label: 'Intermediário', percentile: 50 },
  { id: 'ADVANCED', label: 'Avançado', percentile: 75 },
  { id: 'ELITE', label: 'Elite', percentile: 95 },
] as const
export type Level = (typeof LEVELS)[number]

const male: Record<Lift, number[]> = {
  BENCH: [0.68, 0.93, 1.13, 1.36, 1.79], SQUAT: [0.71, 1.11, 1.4, 1.71, 2.25], DEADLIFT: [0.91, 1.39, 1.78, 2.15, 2.7], OVERHEAD: [0.4, 0.56, 0.66, 0.78, 0.99],
}
const female: Record<Lift, number[]> = {
  BENCH: [0.37, 0.55, 0.7, 0.85, 1.25], SQUAT: [0.4, 0.8, 1.12, 1.42, 1.95], DEADLIFT: [0.63, 1.07, 1.35, 1.72, 2.25], OVERHEAD: [0.23, 0.38, 0.47, 0.57, 0.72],
}

/** Sem sexo informado, usa a média dos dois (aproximação). */
const ratios = (lift: Lift, sex: Sex): number[] => (sex === 'MALE' ? male[lift] : sex === 'FEMALE' ? female[lift] : male[lift].map((m, i) => (m + female[lift][i]) / 2))

/** 1RM de referência (kg) para cada nível. */
export const oneRepMaxByLevel = (lift: Lift, bodyKg: number, sex: Sex): number[] => ratios(lift, sex).map((r) => r * bodyKg)

/** Maior nível já alcançado; null se abaixo de Iniciante. */
export function levelFor(lift: Lift, oneRm: number, bodyKg: number, sex: Sex): Level | null {
  const refs = oneRepMaxByLevel(lift, bodyKg, sex)
  let idx = -1
  refs.forEach((v, i) => { if (oneRm >= v) idx = i })
  return idx >= 0 ? LEVELS[idx] : null
}
export const nextLevel = (level: Level | null): Level | null => (level === null ? LEVELS[0] : LEVELS[LEVELS.findIndex((l) => l.id === level.id) + 1] ?? null)

/** Reconhece o exercício pelo nome (pt-BR e inglês). Só os 4 básicos têm padrão. */
export function liftFor(exerciseName: string): Lift | null {
  const n = exerciseName.toLowerCase()
  const has = (...k: string[]) => k.some((x) => n.includes(x))
  if (has('halter', 'dumbbell', 'máquina', 'maquina', 'machine', 'smith') && !has('barra')) return null
  if (has('inclinado', 'declinado', 'incline', 'decline', 'crucifixo', 'fly')) return null
  if (has('supino', 'bench press')) return 'BENCH'
  if (has('agachamento', 'squat') && !has('búlgaro', 'bulgaro', 'bulgarian', 'hack', 'frontal', 'front')) return 'SQUAT'
  if (has('levantamento terra', 'terra', 'deadlift') && !has('romeno', 'stiff', 'sumô', 'sumo', 'romanian')) return 'DEADLIFT'
  if (has('desenvolvimento', 'overhead press', 'militar', 'shoulder press')) return 'OVERHEAD'
  return null
}
