import { useEffect, useMemo, useState } from 'react'
import { loadHistory, loadHome, type HistItem, type HomeData } from '../data'
import { needsDeload, weeklyVolumes } from '../insights'
import { compareWeeks, formatVolume } from '../recap'
import { prettyGroupName, streakDays, suggestNextGroup, weeklyHistory, weekStart } from '../rules'
import type { Tab } from '../App'
import { loadFavs } from '../favs'
import { loadWellness, saveWellness } from '../wellness'
import { addCardio, loadCardio, loadPhase, PHASES, removeCardio, smartReminder, weeklyCardio, type Cardio } from '../training'
import { dayKey } from '../rules'

export function Home({ go }: { go: (t: Tab, groupId?: string) => void }) {
  const [data, setData] = useState<HomeData | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [hist, setHist] = useState<HistItem[] | null>(null)
  const [cardio, setCardio] = useState<Cardio[]>(() => loadCardio())
  const [cMin, setCMin] = useState('')
  const [cKm, setCKm] = useState('')
  const [cKind, setCKind] = useState('Corrida')
  const [well, setWell] = useState(() => loadWellness()[dayKey(new Date())] ?? {})
  useEffect(() => { loadHistory().then(setHist).catch(() => {}) }, [])
  const load = () => { setError(null); loadHome().then(setData).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  const view = useMemo(() => {
    if (!data) return null
    const now = new Date()
    const done = data.workouts.filter((w) => w.finished_at)
    const active = data.workouts.find((w) => !w.finished_at) ?? null
    const week = weekStart(now).getTime()
    const weekly = done.filter((w) => new Date(w.started_at).getTime() >= week).length
    const nextId = suggestNextGroup(data.groups, data.workouts)
    const next = data.groups.find((g) => g.id === nextId) ?? null
    const last = done[0] ?? null
    return { active, weekly, next, last, streak: streakDays(data.workouts, now), bars: weeklyHistory(data.workouts, now), total: done.length }
  }, [data])

  if (error) return <><div className="title">Início</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!data || !view) return <><div className="title">Início</div><div className="skel" /><div className="skel" /><div className="skel" style={{ height: 160 }} /></>

  const goal = data.goal
  const max = Math.max(1, ...view.bars.map((b) => b.count), goal)
  const lastName = view.last ? prettyGroupName(view.last.muscle_group_name ?? data.groups.find((g) => g.id === view.last!.muscle_group_id)?.name ?? 'Treino livre') : null
  const lastDate = view.last ? new Date(view.last.started_at).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }) : '—'
  const missing = Math.max(0, goal - view.weekly)

  return (
    <>
      <div className="title">Início</div>

      {view.active && (
        <div className="banner" role="button" tabIndex={0} onClick={() => go('treino')} onKeyDown={(e) => e.key === 'Enter' && go('treino')}>
          <div className="grow">
            <div style={{ fontWeight: 700 }}>Treino em andamento</div>
            <div className="muted">{prettyGroupName(view.active.muscle_group_name ?? 'Treino livre')} · iniciado {new Date(view.active.started_at).toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' })}</div>
          </div>
          <strong>Retomar →</strong>
        </div>
      )}

      <div className="card hero" role="button" tabIndex={0} onClick={() => go('treino', view.next?.id)} onKeyDown={(e) => e.key === 'Enter' && go('treino', view.next?.id)}>
        <div className="label">{view.next ? 'Próximo treino' : 'Hora de treinar'}</div>
        <div className="big">{view.next ? prettyGroupName(view.next.name) : 'Escolher treino'}</div>
        <small>{view.next ? 'Sugerido pela sua rotação · toque para começar' : 'Toque para escolher o grupo'}</small>
      </div>

      {(() => {
        const favs = loadFavs().map((id) => data.groups.find((g) => g.id === id)).filter((g): g is NonNullable<typeof g> => !!g)
        return favs.length > 0 && (
          <div className="card">
            <div className="label">Atalhos</div>
            <div className="chips" style={{ marginTop: 8 }}>{favs.map((g) => <button key={g.id} className="chip" onClick={() => go('treino', g.id)}>{prettyGroupName(g.name)}</button>)}</div>
          </div>
        )
      })()}

      {(() => {
        const reminder = smartReminder({ now: new Date(), weeklyDone: view.weekly, goal, trainedToday: !!view.last && dayKey(new Date(view.last.started_at)) === dayKey(new Date()), nextGroup: view.next ? prettyGroupName(view.next.name) : null })
        return reminder ? <div className="card" role="status"><div className="label">Lembrete</div><div>{reminder}</div></div> : null
      })()}

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Cardio da semana</div>
        {(() => { const w = weeklyCardio(cardio); return <div className="muted">{w.sessions === 0 ? 'Nenhum cardio registrado esta semana.' : `${w.sessions} ${w.sessions === 1 ? 'sessão' : 'sessões'} · ${w.minutes} min${w.km ? ` · ${String(w.km).replace('.', ',')} km` : ''}`}</div> })()}
        <div className="chips" role="group" aria-label="Tipo de cardio">{['Corrida', 'Bike', 'Caminhada', 'Aquecimento'].map((k) => <button key={k} className={'chip' + (cKind === k ? ' on' : '')} aria-pressed={cKind === k} onClick={() => setCKind(k)}>{k}</button>)}</div>
        <form className="row" onSubmit={(e) => { e.preventDefault(); const m = parseInt(cMin, 10); if (!(m > 0 && m < 600)) return; const km = parseFloat(cKm.replace(',', '.')); setCardio(addCardio({ day: dayKey(new Date()), kind: cKind, minutes: m, ...(km > 0 ? { km } : {}) })); setCMin(''); setCKm('') }}>
          <label className="field grow"><span className="label">Minutos</span><input inputMode="numeric" value={cMin} onChange={(e) => setCMin(e.target.value.replace(/\D/g, '').slice(0, 3))} /></label>
          <label className="field grow"><span className="label">Km (opcional)</span><input inputMode="decimal" value={cKm} onChange={(e) => setCKm(e.target.value.replace(/[^0-9,.]/g, '').slice(0, 5))} /></label>
          <button className="btn primary" style={{ alignSelf: 'flex-end' }} disabled={!cMin}>Registrar</button>
        </form>
        {cardio.slice(-3).reverse().map((c) => <div key={c.id} className="row"><span className="grow muted num">{c.day.slice(8)}/{c.day.slice(5, 7)} · {c.kind} · {c.minutes} min{c.km ? ` · ${String(c.km).replace('.', ',')} km` : ''}</span><button className="icon" aria-label={`Apagar cardio de ${c.day}`} onClick={() => setCardio(removeCardio(c.id))}>×</button></div>)}
        <div className="muted" style={{ fontSize: 12 }}>Fase atual: {PHASES[loadPhase()].name}. {PHASES[loadPhase()].tip} Cardio fica só neste aparelho.</div>
      </div>

      {hist && (() => {
        const v = weeklyVolumes(hist)
        const cur = v[v.length - 1], prev = v[v.length - 2]
        const lastDay = view.last ? Math.floor((Date.now() - new Date(view.last.started_at).getTime()) / 86400000) : null
        return (
          <div className="card">
            <div className="label">Hoje</div>
            <div className="row"><span className="grow">Carga da semana</span><b className="num">{formatVolume(cur)}</b><span className="muted num" style={{ marginLeft: 10 }}>{prev > 0 ? `${cur >= prev ? '+' : '−'}${Math.round(Math.abs(cur - prev) / prev * 100)}% vs. anterior` : ''}</span></div>
            {lastDay !== null && <div className="row"><span className="grow">Descanso</span><b className="num">{lastDay === 0 ? 'treinou hoje' : `${lastDay} ${lastDay === 1 ? 'dia' : 'dias'} sem treinar`}</b></div>}
            {needsDeload(v) && <div className="muted" style={{ marginTop: 6 }}>Quatro semanas pesadas seguidas: considere uma semana de descarga (cerca de 60% do volume).</div>}
          </div>
        )
      })()}

      {hist && [0, 5, 6].includes(new Date().getDay()) && (() => {
        const c = compareWeeks(hist)
        return (
          <div className="card">
            <div className="label">Resumo da semana</div>
            <div className="muted">{c.workouts[0]} {c.workouts[0] === 1 ? 'treino' : 'treinos'}, {c.sets[0]} séries, {formatVolume(c.volumeKg[0])}{c.workouts[1] ? ` (semana passada: ${c.workouts[1]} treinos)` : ''}.</div>
            <div>{view.weekly >= goal ? 'Meta batida: aproveite para planejar a próxima semana.' : `Ainda dá tempo: faltam ${goal - view.weekly} para a meta.`}{view.next ? ` Sugestão para começar: ${prettyGroupName(view.next.name)}.` : ''}</div>
          </div>
        )
      })()}

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        <div className="label">Como você está hoje?</div>
        <div className="muted">Sono da noite (horas)</div>
        <div className="chips" role="group" aria-label="Sono">{[5, 6, 7, 8, 9].map((h) => <button key={h} className={'chip' + (well.sleepH === h ? ' on' : '')} aria-pressed={well.sleepH === h} onClick={() => { saveWellness(dayKey(new Date()), { sleepH: h }); setWell({ ...well, sleepH: h }) }}>{h === 5 ? '≤5' : h === 9 ? '9+' : h}</button>)}</div>
        <div className="muted">Energia (1 baixa, 5 alta)</div>
        <div className="chips" role="group" aria-label="Energia">{[1, 2, 3, 4, 5].map((e) => <button key={e} className={'chip' + (well.energy === e ? ' on' : '')} aria-pressed={well.energy === e} onClick={() => { saveWellness(dayKey(new Date()), { energy: e }); setWell({ ...well, energy: e }) }}>{e}</button>)}</div>
      </div>

      <div className="card">
        <div className="label">Meta semanal</div>
        <div className="big num">{view.weekly} de {goal} {goal === 1 ? 'treino' : 'treinos'}</div>
        <div className="segs" style={{ margin: '10px 0 8px' }} aria-hidden="true">{Array.from({ length: goal }, (_, i) => <i key={i} className={i < view.weekly ? 'on' : ''} />)}</div>
        <div className="muted">{missing === 0 ? 'Meta batida. Bom trabalho!' : `Faltam ${missing} ${missing === 1 ? 'treino' : 'treinos'} para a meta.`}</div>
      </div>

      <div className="grid2">
        <div className="card"><div className="label">Sequência</div><div className="big num">{view.streak}</div><div className="muted">{view.streak === 1 ? 'dia seguido' : 'dias seguidos'}</div></div>
        <div className="card"><div className="label">Último</div><div className="big num">{lastDate}</div><div className="muted" style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{lastName ?? 'sem treinos'}</div></div>
      </div>

      <div className="card">
        <div className="row"><div className="label grow">Frequência</div><div className="muted num">{view.total} treinos no total</div></div>
        <div className="bars" style={{ marginTop: 10 }} role="img" aria-label="Treinos por semana nas últimas 8 semanas">
          {view.bars.map((b, i) => (
            <div key={b.start.getTime()} className={'bar' + (i === view.bars.length - 1 ? ' cur' : '')}>
              {b.count > 0 && <b className="num">{b.count}</b>}
              <span style={{ height: `${(b.count / max) * 80}%` }} />
              <small className="muted num">{b.start.getDate()}/{b.start.getMonth() + 1}</small>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
