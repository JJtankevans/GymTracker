import { useEffect, useMemo, useState } from 'react'
import { loadHistory, loadHome, type HistItem, type HomeData } from '../data'
import { needsDeload, weeklyVolumes } from '../insights'
import { compareWeeks, formatVolume } from '../recap'
import { prettyGroupName, streakDays, suggestNextGroup, weeklyHistory, weekStart } from '../rules'
import type { Tab } from '../App'

export function Home({ go }: { go: (t: Tab, groupId?: string) => void }) {
  const [data, setData] = useState<HomeData | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [hist, setHist] = useState<HistItem[] | null>(null)
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
