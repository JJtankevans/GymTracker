// Ferramentas de academia: calculadora de anilhas e aquecimento progressivo. Funções puras (testadas em tools.test.ts).

export const DEFAULT_PLATES = [25, 20, 15, 10, 5, 2.5, 1.25]

export interface PlatePlan {
  perSide: { plate: number; count: number }[]
  /** Peso realmente montado (barra + anilhas), pode ficar abaixo do pedido se faltar anilha pequena. */
  achieved: number
  /** Quanto faltou para o pedido (0 se exato). */
  missing: number
}

const r2 = (n: number) => Math.round(n * 100) / 100

/** Anilhas de cada lado para chegar em [target] kg com uma barra de [bar] kg (guloso: maiores primeiro, só pares iguais dos dois lados). */
export function platesFor(target: number, bar = 20, plates: number[] = DEFAULT_PLATES): PlatePlan {
  const sorted = [...plates].sort((a, b) => b - a)
  let side = Math.max(0, (target - bar) / 2)
  const perSide: PlatePlan['perSide'] = []
  let used = 0
  for (const p of sorted) {
    const n = Math.floor(r2(side / p) + 1e-9)
    if (n > 0) { perSide.push({ plate: p, count: n }); side = r2(side - n * p); used += n * p }
  }
  const achieved = r2(bar + 2 * used)
  return { perSide, achieved, missing: Math.max(0, r2(target - achieved)) }
}

export interface WarmupSet { kg: number; reps: number; label: string }

/** Arredonda para o degrau de [step] kg mais próximo. */
export const roundTo = (kg: number, step = 2.5) => Math.round(kg / step) * step

/**
 * Aquecimento progressivo até a carga de trabalho [work]: barra vazia, 50 %, 70 % e 85 % (reps caindo).
 * Pula degraus que ficam iguais ao anterior ou abaixo da barra; sem aquecimento se a carga é leve (≤ 1,2 × a barra).
 */
export function warmupFor(work: number, bar = 20, step = 2.5): WarmupSet[] {
  if (!(work > bar * 1.2)) return []
  const plan: [number, number, string][] = [[bar, 10, 'Barra'], [work * 0.5, 8, '50%'], [work * 0.7, 5, '70%'], [work * 0.85, 3, '85%']]
  const out: WarmupSet[] = []
  for (const [raw, reps, label] of plan) {
    const kg = Math.max(bar, roundTo(raw, step))
    if (kg >= work) continue
    if (out.length > 0 && kg <= out[out.length - 1].kg) continue
    out.push({ kg, reps, label })
  }
  return out
}

export interface RepMaxRow { reps: number; kg: number; percent: number }

/** Cargas estimadas para cada nº de repetições a partir do 1RM (Epley invertido), arredondadas ao degrau de [step] kg. Estimativa, não prescrição. */
export function repMaxTable(oneRm: number, reps = [1, 2, 3, 4, 5, 6, 8, 10, 12, 15], step = 2.5): RepMaxRow[] {
  if (!(oneRm > 0)) return []
  return reps.map((r) => {
    const raw = r <= 1 ? oneRm : oneRm / (1 + r / 30)
    return { reps: r, kg: Math.round(raw / step) * step, percent: Math.round((raw / oneRm) * 100) }
  })
}
