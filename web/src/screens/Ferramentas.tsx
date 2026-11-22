import { useMemo, useState } from 'react'
const formatKg = (n: number) => String(Math.round(n * 100) / 100).replace('.', ',')
import { platesFor, warmupFor } from '../tools'

const num = (s: string) => { const n = parseFloat(s.replace(',', '.')); return Number.isFinite(n) ? n : NaN }
const BARS = [20, 15, 10]

/** Calculadora de anilhas e aquecimento progressivo, para usar na hora do treino. */
export function Ferramentas() {
  const [target, setTarget] = useState('')
  const [bar, setBar] = useState(20)
  const t = num(target)
  const plan = useMemo(() => (t > 0 ? platesFor(t, bar) : null), [t, bar])
  const warm = useMemo(() => (t > 0 ? warmupFor(t, bar) : []), [t, bar])

  return (
    <>
      <div className="title">Ferramentas</div>
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <label className="label" htmlFor="alvo">Carga total (kg)</label>
        <input id="alvo" className="field" inputMode="decimal" placeholder="Ex.: 100" value={target} onChange={(e) => setTarget(e.target.value)}
          style={{ fontSize: 22, padding: 12, borderRadius: 'var(--radius)', border: '1px solid var(--line)', background: 'var(--surface-2)', color: 'var(--text)' }} />
        <div className="label">Barra</div>
        <div className="chips" aria-label="Peso da barra">
          {BARS.map((b) => <button key={b} className={'chip' + (bar === b ? ' on' : '')} aria-pressed={bar === b} onClick={() => setBar(b)}>{b} kg</button>)}
        </div>
      </div>

      {plan && (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div className="label">Anilhas de cada lado</div>
          {plan.perSide.length === 0 ? <div className="muted">Só a barra.</div> : plan.perSide.map((p) => (
            <div key={p.plate} className="row"><b className="num" style={{ fontSize: 22, minWidth: 64 }}>{formatKg(p.plate)} kg</b><span className="muted">× {p.count}</span></div>
          ))}
          <div className="muted">Total montado: <b style={{ color: 'var(--text)' }}>{formatKg(plan.achieved)} kg</b>{plan.missing > 0 && <> · faltam {formatKg(plan.missing)} kg (sem anilha tão pequena)</>}</div>
        </div>
      )}

      {t > 0 && (
        <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <div className="label">Aquecimento até {formatKg(t)} kg</div>
          {warm.length === 0 ? <div className="muted">Carga leve: dispensa aquecimento.</div> : warm.map((w) => (
            <div key={w.label} className="row"><span className="grow muted">{w.label}</span><b className="num">{formatKg(w.kg)} kg × {w.reps}</b></div>
          ))}
        </div>
      )}
    </>
  )
}
