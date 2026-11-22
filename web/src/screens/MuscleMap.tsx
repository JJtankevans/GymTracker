import type { MuscleSets } from '../muscle'

// Porte do MuscleMap.kt (Android): silhuetas em "unidades de figura" 100 × 200, cada músculo pintado pelas séries da semana.
type P = [number, number, number, number, number] // x, y, largura, altura, raio
const SIL: P[] = [[45, 24, 10, 8, 3], [27, 32, 46, 84, 14], [12, 40, 15, 60, 7], [73, 40, 15, 60, 7], [10, 98, 11, 34, 5], [79, 98, 11, 34, 5], [31, 114, 38, 24, 10], [30, 130, 18, 68, 8], [52, 130, 18, 68, 8]]
const FRONT: [string, P[]][] = [
  ['Ombros', [[17, 34, 18, 18, 9], [65, 34, 18, 18, 9]]],
  ['Peito', [[34, 40, 15, 18, 7], [51, 40, 15, 18, 7]]],
  ['Bíceps', [[12, 54, 13, 26, 6], [75, 54, 13, 26, 6]]],
  ['Abdômen', [[38, 62, 24, 40, 8]]],
  ['Quadríceps', [[30, 120, 18, 46, 8], [52, 120, 18, 46, 8]]],
  ['Panturrilha', [[31, 170, 15, 28, 6], [54, 170, 15, 28, 6]]],
]
const BACK: [string, P[]][] = [
  ['Ombros', [[17, 34, 18, 18, 9], [65, 34, 18, 18, 9]]],
  ['Costas', [[34, 36, 32, 44, 10]]],
  ['Tríceps', [[12, 54, 13, 26, 6], [75, 54, 13, 26, 6]]],
  ['Glúteos', [[33, 100, 17, 20, 8], [50, 100, 17, 20, 8]]],
  ['Posterior', [[30, 122, 18, 44, 8], [52, 122, 18, 44, 8]]],
  ['Panturrilha', [[31, 170, 15, 28, 6], [54, 170, 15, 28, 6]]],
]
const BASE = 'color-mix(in srgb, var(--text) 6%, transparent)'

export function MuscleMap({ rows, fullAt }: { rows: MuscleSets[]; fullAt: number }) {
  const by = new Map(rows.filter((r) => r.muscle).map((r) => [r.muscle as string, r.sets]))
  const tint = (m: string) => {
    const n = by.get(m) ?? 0
    if (n <= 0) return 'color-mix(in srgb, var(--text) 10%, transparent)'
    const t = Math.min(1, n / fullAt)
    return `color-mix(in srgb, var(--accent) ${Math.round(30 + 70 * t)}%, transparent)`
  }
  const rect = ([x, y, w, h, r]: P, fill: string, k: string) => <rect key={k} x={x} y={y} width={w} height={h} rx={r} fill={fill} />
  const figure = (parts: [string, P[]][], dx: number) => (
    <g transform={`translate(${dx} 0)`}>
      <circle cx={50} cy={14} r={11} fill={BASE} />
      {SIL.map((p, i) => rect(p, BASE, 's' + i))}
      {parts.flatMap(([m, ps]) => ps.map((p, i) => rect(p, tint(m), m + i)))}
    </g>
  )
  return (
    <figure style={{ margin: 0 }}>
      <svg viewBox="0 0 220 200" role="img" aria-label="Mapa muscular: séries por músculo nos últimos 7 dias" style={{ width: '100%', height: 250 }}>
        {figure(FRONT, 0)}{figure(BACK, 120)}
      </svg>
      <div className="row muted" style={{ justifyContent: 'space-around', fontSize: 13 }}><span>Frente</span><span>Costas</span></div>
      <figcaption className="muted" style={{ fontSize: 12, textAlign: 'center', marginTop: 6 }}>Cinza = nenhuma série · cor cheia = {fullAt} séries ou mais</figcaption>
    </figure>
  )
}
