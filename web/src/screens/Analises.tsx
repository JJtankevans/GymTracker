import { useEffect, useMemo, useState } from 'react'
import { loadHistory, type HistItem } from '../data'
import { loadWeights } from '../body'
import { bandRecords, compareWorkouts, estimateKcal, exerciseWeeklyVolume, heatmap, historyCsv, imbalances, plateaus, RM_BANDS, searchHistory } from '../insights'
import { formatKg, prettyGroupName, realDurationMinutes } from '../rules'
import { setsOf } from './Evolucao'

const LEVEL_OPACITY = [0.12, 0.35, 0.65, 1]
const dmy = (iso: string) => new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })
const label = (w: HistItem) => `${dmy(w.started_at)} · ${prettyGroupName(w.muscle_group_name ?? 'Treino livre')}`

function downloadCsv(items: HistItem[]) {
  const url = URL.createObjectURL(new Blob([historyCsv(items)], { type: 'text/csv;charset=utf-8' }))
  const a = Object.assign(document.createElement('a'), { href: url, download: 'gymtracker_historico.csv' })
  document.body.appendChild(a); a.click(); a.remove(); URL.revokeObjectURL(url)
}

export function Analises() {
  const [hist, setHist] = useState<HistItem[] | null>(null)
  const [bodyKg, setBodyKg] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [q, setQ] = useState('')
  const [exSel, setExSel] = useState('')
  const [pair, setPair] = useState<[string, string]>(['', ''])
  const load = () => { setError(null); Promise.all([loadHistory(), loadWeights().catch(() => [])]).then(([h, w]) => { setHist(h); setBodyKg(w.length ? w[w.length - 1].weight_kg : null) }).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  const v = useMemo(() => {
    if (!hist) return null
    const sets = setsOf(hist)
    const now = new Date()
    const month = hist.filter((w) => { const d = new Date(w.started_at); return d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth() })
    const minutes = month.reduce((a, w) => a + (realDurationMinutes(null, w.started_at, w.sets.map((s) => s.completed_at)) ?? 0), 0)
    return { bands: bandRecords(sets), imb: imbalances(sets), plat: plateaus(sets), heat: heatmap(hist, now), month: month.length, minutes }
  }, [hist])

  if (error) return <><div className="title">Análises</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!hist || !v) return <><div className="title">Análises</div><div className="skel" /><div className="skel" /></>

  const exNames = [...new Set(hist.flatMap((w) => w.sets.map((x) => x.exercise?.name ?? 'Exercício')))].sort((x, y) => x.localeCompare(y, 'pt-BR'))
  const exName = exSel || exNames[0] || ''
  const exVol = exName ? exerciseWeeklyVolume(hist, exName) : []
  const exMax = Math.max(1, ...exVol)
  const hits = searchHistory(hist, q)
  const a = hist.find((w) => w.id === pair[0]), b = hist.find((w) => w.id === pair[1])
  const byExercise = new Map<string, typeof v.bands>()
  for (const r of v.bands) byExercise.set(r.exercise, [...(byExercise.get(r.exercise) ?? []), r])

  return (
    <>
      <div className="title">Análises</div>

      <div className="card">
        <div className="label">Constância (26 semanas)</div>
        <div role="img" aria-label="Mapa de calor dos treinos nas últimas 26 semanas" style={{ display: 'flex', gap: 3, margin: '10px 0', overflowX: 'auto' }}>
          {v.heat.map((col, i) => (
            <div key={i} style={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              {col.map((c) => <i key={c.day} title={`${c.day}: ${c.count} séries`} style={{ width: 11, height: 11, borderRadius: 3, background: 'var(--accent)', opacity: LEVEL_OPACITY[c.level] }} />)}
            </div>
          ))}
        </div>
        <div className="muted">Mais forte = mais séries no dia.</div>
      </div>

      <div className="card">
        <div className="label">Calorias do mês (estimativa)</div>
        {bodyKg
          ? <><div className="big num">{estimateKcal(v.minutes, bodyKg).toLocaleString('pt-BR')} kcal</div><div className="muted">{v.month} treinos, {Math.round(v.minutes)} min, peso {formatKg(bodyKg)} kg. Conta aproximada (musculação ≈ 5 MET); não é medição.</div></>
          : <div className="muted">Registre seu peso em Evolução para ver a estimativa.</div>}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div className="label">Equilíbrio (últimos 28 dias)</div>
        {v.imb.map((i) => (
          <div key={i.label}>
            <div className="row"><b className="grow">{i.label}</b><span className="num">{i.setsA} × {i.setsB}</span></div>
            <div className="muted">{i.warn ? `Atenção: ${i.setsA > i.setsB ? i.a : i.b} tem mais que o dobro de séries. Considere equilibrar.` : 'Dentro do esperado.'}</div>
          </div>
        ))}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div className="label">Platôs</div>
        {v.plat.length === 0 && <div className="muted">Nenhum exercício parado há 4 semanas ou mais. Bom sinal.</div>}
        {v.plat.map((p) => (
          <div key={p.exercise}><b>{p.exercise}</b><div className="muted">Sem novo recorde há {p.weeks} semanas (1RM {formatKg(p.bestOneRm)} kg). Tente uma semana leve (deload) ou uma variação.</div></div>
        ))}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Recordes por faixa (1, 5 e 10 repetições)</div>
        {byExercise.size === 0 && <div className="muted">Sem dados ainda.</div>}
        {[...byExercise].slice(0, 25).map(([ex, rs]) => (
          <div key={ex} className="row"><b className="grow" style={{ minWidth: 0 }}>{ex}</b>
            {RM_BANDS.map((n) => { const r = rs.find((x) => x.band === n); return <span key={n} className="num muted" style={{ minWidth: 64, textAlign: 'right' }}>{n}RM {r ? formatKg(r.kg) : '–'}</span> })}
          </div>
        ))}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Volume por exercício (8 semanas)</div>
        <select aria-label="Exercício" value={exName} onChange={(e) => setExSel(e.target.value)}>{exNames.map((n) => <option key={n}>{n}</option>)}</select>
        <div className="bars" role="img" aria-label={`Volume semanal de ${exName}`}>
          {exVol.map((vol, i) => (
            <div key={i} className={'bar' + (i === exVol.length - 1 ? ' cur' : '')}>
              {vol > 0 && <b className="num">{vol >= 1000 ? `${Math.round(vol / 100) / 10}t` : Math.round(vol)}</b>}
              <span style={{ height: `${(vol / exMax) * 80}%` }} />
              <small className="muted num">{i === exVol.length - 1 ? 'atual' : `-${exVol.length - 1 - i}`}</small>
            </div>
          ))}
        </div>
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Comparar dois treinos</div>
        {[0, 1].map((i) => (
          <select key={i} aria-label={`Treino ${i + 1}`} value={pair[i]} onChange={(e) => setPair((p) => (i === 0 ? [e.target.value, p[1]] : [p[0], e.target.value]))}>
            <option value="">Escolher treino {i + 1}</option>
            {hist.slice(0, 40).map((w) => <option key={w.id} value={w.id}>{label(w)}</option>)}
          </select>
        ))}
        {a && b && compareWorkouts(a, b).map((d) => (
          <div key={d.exercise} className="row"><span className="grow" style={{ minWidth: 0 }}>{d.exercise}</span>
            <span className="num muted">{d.a ? `${formatKg(d.a.kg)}×${d.a.reps}` : '–'}</span><span aria-hidden="true">→</span><b className="num">{d.b ? `${formatKg(d.b.kg)}×${d.b.reps}` : '–'}</b></div>
        ))}
      </div>

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Pesquisar no histórico</div>
        <input aria-label="Pesquisar" placeholder="Exercício ou carga (ex.: rosca, 80kg)" value={q} onChange={(e) => setQ(e.target.value)} />
        {q && hits.length === 0 && <div className="muted">Nada encontrado.</div>}
        {hits.map((h, i) => <div key={i} className="row"><span className="muted num">{dmy(h.date)}</span><span className="grow" style={{ minWidth: 0 }}>{h.exercise}</span><b className="num">{formatKg(h.kg)} kg × {h.reps}</b></div>)}
      </div>

      <div className="card row">
        <div className="grow"><b>Exportar histórico</b><div className="muted">CSV com uma linha por série (abre no Excel)</div></div>
        <button className="btn" onClick={() => downloadCsv(hist)}>Baixar CSV</button>
      </div>
    </>
  )
}
