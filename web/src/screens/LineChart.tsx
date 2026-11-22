/** Gráfico de linha em SVG, sem biblioteca. O último ponto é destacado; eixos só com os extremos. */
export function LineChart({ values, labels, height = 190, unit = 'kg' }: { values: number[]; labels: string[]; height?: number; unit?: string }) {
  const W = 340
  const padL = 40, padR = 12, padT = 14, padB = 26
  const min = Math.min(...values), max = Math.max(...values)
  const span = max - min || 1
  const lo = min - span * 0.15, hi = max + span * 0.15
  const x = (i: number) => padL + (values.length === 1 ? (W - padL - padR) / 2 : (i * (W - padL - padR)) / (values.length - 1))
  const y = (v: number) => padT + (1 - (v - lo) / (hi - lo)) * (height - padT - padB)
  const d = values.map((v, i) => `${i ? 'L' : 'M'}${x(i).toFixed(1)} ${y(v).toFixed(1)}`).join(' ')
  const area = `${d} L${x(values.length - 1).toFixed(1)} ${height - padB} L${x(0).toFixed(1)} ${height - padB} Z`
  const fmt = (v: number) => (Math.round(v * 10) / 10).toString().replace('.', ',')
  const ticks = [hi - (hi - lo) * 0.1, (hi + lo) / 2, lo + (hi - lo) * 0.1]
  return (
    <svg viewBox={`0 0 ${W} ${height}`} width="100%" role="img" aria-label={`Gráfico de ${labels[0]} a ${labels[labels.length - 1]}, de ${fmt(min)} a ${fmt(max)} ${unit}`} style={{ display: 'block' }}>
      {ticks.map((t) => <g key={t}><line x1={padL} x2={W - padR} y1={y(t)} y2={y(t)} stroke="var(--line)" strokeDasharray="3 4" /><text x={padL - 6} y={y(t) + 4} textAnchor="end" fontSize="10" fill="var(--muted)">{fmt(t)}</text></g>)}
      <path d={area} fill="var(--accent)" opacity="0.12" />
      <path d={d} fill="none" stroke="var(--accent)" strokeWidth="2.5" strokeLinejoin="round" strokeLinecap="round" />
      {values.length <= 24 && values.map((v, i) => <circle key={i} cx={x(i)} cy={y(v)} r="3" fill="var(--accent)" />)}
      <circle cx={x(values.length - 1)} cy={y(values[values.length - 1])} r="6" fill="none" stroke="var(--accent)" strokeWidth="2" />
      <text x={padL} y={height - 6} fontSize="10" fill="var(--muted)">{labels[0]}</text>
      <text x={W - padR} y={height - 6} fontSize="10" fill="var(--muted)" textAnchor="end">{labels[labels.length - 1]}</text>
    </svg>
  )
}
