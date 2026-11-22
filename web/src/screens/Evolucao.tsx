import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { deleteWeight, loadWeights, logWeight, type Weight } from '../body'
import { loadHistory, type HistItem } from '../data'
import { oneRmSeries, type SetLite } from '../achievements'
import { dayKey, formatKg } from '../rules'
import { LineChart } from './LineChart'

const short = (iso: string) => { const [, m, d] = iso.slice(0, 10).split('-'); return `${d}/${m}` }
const signed = (v: number) => { const r = Math.round(v * 10) / 10; return (r > 0 ? '+' : '') + formatKg(r) + ' kg' }

export function setsOf(items: HistItem[]): SetLite[] {
  return items.flatMap((w) => w.sets.map((s) => ({ exercise: s.exercise?.name ?? 'Exercício', reps: s.reps, kg: Number(s.weight_kg), at: s.completed_at })))
}

function Stats({ rows }: { rows: [string, string][] }) {
  return <div className="stats">{rows.map(([k, v]) => <div key={k}><div className="label">{k}</div><b className="num">{v}</b></div>)}</div>
}

export function Evolucao() {
  const [weights, setWeights] = useState<Weight[] | null>(null)
  const [hist, setHist] = useState<HistItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)

  const load = () => { setError(null); Promise.all([loadWeights(), loadHistory()]).then(([w, h]) => { setWeights(w); setHist(h) }).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  const lifts = useMemo(() => {
    const sets = setsOf(hist ?? [])
    const names = new Map<string, Set<string>>()
    for (const s of sets) names.set(s.exercise, (names.get(s.exercise) ?? new Set()).add(dayKey(new Date(s.at))))
    return [...names.entries()].sort((a, b) => b[1].size - a[1].size).slice(0, 4).map(([n]) => ({ name: n, series: oneRmSeries(sets, n) })).filter((l) => l.series.length >= 2)
  }, [hist])

  async function submit(e: FormEvent) {
    e.preventDefault()
    const kg = parseFloat(input.replace(',', '.'))
    if (!(kg >= 20 && kg <= 400)) { setError('Informe um peso entre 20 e 400 kg.'); return }
    setBusy(true); setError(null)
    try { await logWeight(dayKey(new Date()), Math.round(kg * 10) / 10); setInput(''); setWeights(await loadWeights()) } catch (x) { setError((x as Error).message) } finally { setBusy(false) }
  }

  if (!weights || !hist) return <><div className="title">Evolução</div>{error ? <div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div> : <div className="skel" style={{ height: 260 }} />}</>

  const kgs = weights.map((w) => w.weight_kg)
  const latest = kgs[kgs.length - 1]
  return (
    <>
      <div className="title">Evolução</div>
      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="label">Peso corporal</div>
        {latest != null ? <div className="big num">{formatKg(latest)} kg</div> : <div className="muted">Registre seu peso para acompanhar a evolução.</div>}
        {kgs.length >= 2 && (
          <>
            <Stats rows={[['Variação', signed(latest - kgs[0])], ['Mínimo', `${formatKg(Math.min(...kgs))} kg`], ['Máximo', `${formatKg(Math.max(...kgs))} kg`], ['Registros', String(kgs.length)]]} />
            <LineChart values={kgs} labels={weights.map((w) => short(w.measured_on))} height={200} />
          </>
        )}
        <form className="row" onSubmit={submit}>
          <label className="field grow"><span className="label">Peso de hoje (kg)</span><input inputMode="decimal" value={input} onChange={(e) => setInput(e.target.value.replace(/[^0-9,.]/g, '').slice(0, 6))} /></label>
          <button className="btn primary" style={{ alignSelf: 'flex-end' }} disabled={busy || !input}>Registrar</button>
        </form>
        {error && <div className="error">{error}</div>}
        {weights.slice(-5).reverse().map((w) => (
          <div key={w.id} className="row"><span className="grow num">{short(w.measured_on)} · {formatKg(w.weight_kg)} kg</span><button className="icon" aria-label={`Apagar ${short(w.measured_on)}`} onClick={async () => { await deleteWeight(w.id); setWeights(await loadWeights()) }}>×</button></div>
        ))}
      </div>

      <div className="title" style={{ fontSize: 22 }}>Cargas por exercício</div>
      {lifts.length === 0 && <div className="card muted">Quando você treinar um exercício em pelo menos dois dias, o 1RM estimado aparece aqui.</div>}
      {lifts.map((l) => {
        const v = l.series.map((p) => p.value)
        return (
          <div key={l.name} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <b style={{ fontSize: 17 }}>{l.name}</b>
            <Stats rows={[['Atual', `${formatKg(v[v.length - 1])} kg`], ['Melhor', `${formatKg(Math.max(...v))} kg`], ['Variação', signed(v[v.length - 1] - v[0])], ['Sessões', String(v.length)]]} />
            <LineChart values={v} labels={l.series.map((p) => short(p.day))} height={170} />
          </div>
        )
      })}
      <p className="muted" style={{ fontSize: 12 }}>O 1RM é estimado (Epley, até 12 reps): use como guia, não como diagnóstico.</p>
    </>
  )
}
