import { useEffect, useMemo, useState, type FormEvent } from 'react'
import { deleteWeight, loadSex, loadWeights, logWeight, type Weight } from '../body'
import { levelFor, liftFor, nextLevel, oneRepMaxByLevel, type Sex } from '../strength'
import { loadHistory, type HistItem } from '../data'
import { oneRmSeries, type SetLite } from '../achievements'
import { dayKey, formatKg } from '../rules'
import { weeklySetsByMuscle, WEEKLY_SETS_RANGE } from '../muscle'
import { MuscleMap } from './MuscleMap'
import { RecapCard } from './RecapCard'
import { LineChart } from './LineChart'
import { compareWeeks, formatVolume } from '../recap'
import { projectWeight } from '../insights'

const short = (iso: string) => { const [, m, d] = iso.slice(0, 10).split('-'); return `${d}/${m}` }
const signed = (v: number) => { const r = Math.round(v * 10) / 10; return (r > 0 ? '+' : '') + formatKg(r) + ' kg' }

const LEVELS_IDX: Record<string, number> = { BEGINNER: 0, NOVICE: 1, INTERMEDIATE: 2, ADVANCED: 3, ELITE: 4 }

export function setsOf(items: HistItem[]): SetLite[] {
  return items.flatMap((w) => w.sets.map((s) => ({ exercise: s.exercise?.name ?? 'Exercício', reps: s.reps, kg: Number(s.weight_kg), at: s.completed_at })))
}

function Stats({ rows }: { rows: [string, string][] }) {
  return <div className="stats">{rows.map(([k, v]) => <div key={k}><div className="label">{k}</div><b className="num">{v}</b></div>)}</div>
}

export function Evolucao() {
  const [goalKg, setGoalKg] = useState<number>(() => { try { return Number(localStorage.getItem('gt-weight-goal')) || 0 } catch { return 0 } })
  const [weights, setWeights] = useState<Weight[] | null>(null)
  const [hist, setHist] = useState<HistItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [sex, setSex] = useState<Sex>(null)
  useEffect(() => { loadSex().then(setSex).catch(() => {}) }, [])

  const load = () => { setError(null); Promise.all([loadWeights(), loadHistory()]).then(([w, h]) => { setWeights(w); setHist(h) }).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  const muscles = useMemo(() => weeklySetsByMuscle(setsOf(hist ?? [])), [hist])

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
  const proj = goalKg ? projectWeight(weights.map((w) => ({ day: w.measured_on, kg: w.weight_kg })), goalKg) : null
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
        <div className="row">
          <label className="field grow"><span className="label">Meta de peso (kg)</span><input inputMode="decimal" placeholder="opcional" defaultValue={goalKg ? formatKg(goalKg) : ''} onBlur={(e) => { const n = parseFloat(e.target.value.replace(',', '.')); const v = Number.isFinite(n) && n > 20 && n < 400 ? n : 0; try { v ? localStorage.setItem('gt-weight-goal', String(v)) : localStorage.removeItem('gt-weight-goal') } catch { /* sem armazenamento */ } setGoalKg(v) }} /></label>
        </div>
        {goalKg > 0 && (proj
          ? <div className="muted">{proj.date ? `Ritmo de ${signed(proj.perWeek).replace(' kg', '')} kg por semana: meta por volta de ${proj.date.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })}.` : 'A tendência atual não leva à meta (ou o peso está estável).'}</div>
          : <div className="muted">Registre pelo menos 3 pesos nas últimas 8 semanas para projetar a data.</div>)}
        {error && <div className="error">{error}</div>}
        {weights.slice(-5).reverse().map((w) => (
          <div key={w.id} className="row"><span className="grow num">{short(w.measured_on)} · {formatKg(w.weight_kg)} kg</span><button className="icon" aria-label={`Apagar ${short(w.measured_on)}`} onClick={async () => { await deleteWeight(w.id); setWeights(await loadWeights()) }}>×</button></div>
        ))}
      </div>

      {hist && (() => {
        const c = compareWeeks(hist)
        const rows: [string, [number, number], (n: number) => string][] = [['Treinos', c.workouts, String], ['Séries', c.sets, String], ['Volume', c.volumeKg, formatVolume]]
        return (
          <div className="card">
            <div className="label">Esta semana vs. anterior</div>
            {rows.map(([name, [a, b], fmt]) => {
              const d = a - b
              return <div key={name} className="row"><span className="grow">{name}</span><span className="num">{fmt(a)}</span><span className="muted num" style={{ marginLeft: 10 }}>{b === 0 ? '—' : `${d >= 0 ? '+' : '−'}${Math.round(Math.abs(d) / b * 100)}%`}</span></div>
            })}
          </div>
        )
      })()}

      <RecapCard items={hist} />

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="label">Séries por músculo (7 dias)</div>
        <MuscleMap rows={muscles} fullAt={WEEKLY_SETS_RANGE[1]} />
        {muscles.filter((r) => r.sets > 0).map((r) => {
          const ok = r.sets >= WEEKLY_SETS_RANGE[0] && r.sets <= WEEKLY_SETS_RANGE[1]
          return (
            <div key={r.muscle ?? 'outros'} className="row">
              <span className="grow">{r.muscle ?? 'Outros'}</span><b className="num">{r.sets}</b>
              <span className="muted" style={{ width: 120, textAlign: 'right' }}>{r.muscle === null ? 'fora do catálogo' : ok ? 'na faixa' : r.sets < WEEKLY_SETS_RANGE[0] ? 'abaixo' : 'acima'}</span>
            </div>
          )
        })}
        <div className="muted" style={{ fontSize: 12 }}>Faixa comum para hipertrofia: {WEEKLY_SETS_RANGE[0]} a {WEEKLY_SETS_RANGE[1]} séries por semana.</div>
      </div>

      <div className="title" style={{ fontSize: 22 }}>Cargas por exercício</div>
      {lifts.length === 0 && <div className="card muted">Quando você treinar um exercício em pelo menos dois dias, o 1RM estimado aparece aqui.</div>}
      {lifts.map((l) => {
        const v = l.series.map((p) => p.value)
        const lift = liftFor(l.name)
        const bw = latest
        const lvl = lift && bw ? levelFor(lift, Math.max(...v), bw, sex) : null
        const nxt = lift && bw ? nextLevel(lvl) : null
        const missing = lift && bw && nxt ? oneRepMaxByLevel(lift, bw, sex)[LEVELS_IDX[nxt.id]] - Math.max(...v) : null
        return (
          <div key={l.name} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div className="row"><b className="grow" style={{ fontSize: 17 }}>{l.name}</b>{lift && bw ? <span className="chip on" style={{ minHeight: 32, display: 'inline-flex', alignItems: 'center' }}>{lvl?.label ?? 'Abaixo de Iniciante'}</span> : null}</div>
            <Stats rows={[['Atual', `${formatKg(v[v.length - 1])} kg`], ['Melhor', `${formatKg(Math.max(...v))} kg`], ['Variação', signed(v[v.length - 1] - v[0])], ['Sessões', String(v.length)]]} />
            <LineChart values={v} labels={l.series.map((p) => short(p.day))} height={170} />
            {lift && !bw && <div className="muted" style={{ fontSize: 13 }}>Registre seu peso corporal para ver as referências de nível.</div>}
            {nxt && missing != null && <div className="muted" style={{ fontSize: 13 }}>Para {nxt.label}: faltam ~{formatKg(Math.max(0, missing))} kg de 1RM ({nxt.percentile}% das pessoas ficam abaixo desse nível).</div>}
            {lift && bw && !nxt && <div style={{ fontSize: 13 }}>Nível Elite: mais forte que ~95% dos usuários.</div>}
          </div>
        )
      })}
      <p className="muted" style={{ fontSize: 12 }}>O 1RM é estimado (Epley, até 12 reps): use como guia, não como diagnóstico.</p>
    </>
  )
}
