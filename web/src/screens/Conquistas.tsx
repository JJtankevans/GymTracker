import { useEffect, useState } from 'react'
import { loadHome, loadHistory } from '../data'
import { computeAchievements, recordsOf, FAMILY_LABEL, type Achievement, type AchievementFamily } from '../achievements'
import { realDurationMinutes } from '../rules'
import { setsOf } from './Evolucao'
import { monthlyBadges } from '../insights'

const ICON: Record<AchievementFamily, string> = {
  WORKOUTS: 'M6 6v12M3 9v6M18 6v12M21 9v6M6 12h12',
  STREAK: 'M12 3c1 4 5 5 5 10a5 5 0 0 1-10 0c0-2 1-3 2-4 0 2 1 3 2 3 0-3-1-5 1-9z',
  WEEKS: 'M4 6h16v14H4zM4 10h16M8 3v4M16 3v4',
  VOLUME: 'M6 20h12l-1.5-9h-9zM9 11a3 3 0 1 1 6 0',
  RECORDS: 'M8 4h8v5a4 4 0 0 1-8 0zM8 6H4v2a3 3 0 0 0 4 3M16 6h4v2a3 3 0 0 1-4 3M12 13v4M8 20h8',
  SPECIAL: 'M12 3l2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9z',
}

function Badge({ a }: { a: Achievement }) {
  const [flipped, setFlipped] = useState(false)
  const on = a.progress >= a.target
  return (
    <div className={'card row ach' + (on ? ' on' : '')}>
      <button className={'medal' + (flipped ? ' flip' : '')} disabled={!on} aria-label={on ? 'Virar selo' : 'Bloqueada'} onClick={() => setFlipped((f) => !f)}>
        <span className="face front"><svg viewBox="0 0 24 24" aria-hidden="true"><path d={on ? ICON[a.family] : 'M7 11V8a5 5 0 0 1 10 0v3M6 11h12v9H6z'} /></svg></span>
        <span className="face back" aria-hidden="true"><svg viewBox="0 0 24 24"><path d={ICON[a.family]} /></svg></span>
      </button>
      <div className="grow">
        <b>{a.title}</b>
        <div className="muted">{a.description}</div>
        {!on && <div className="row" style={{ marginTop: 6 }}><div className="prog grow"><i style={{ width: `${Math.min(100, (a.progress / a.target) * 100)}%` }} /></div><small className="num">{Math.min(a.progress, a.target)}/{a.target}</small></div>}
      </div>
    </div>
  )
}

export function Conquistas() {
  const [list, setList] = useState<Achievement[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const load = () => {
    setError(null)
    Promise.all([loadHistory(), loadHome()]).then(([h, home]) => {
      const sessions = h.map((w) => ({
        start: new Date(w.started_at),
        minutes: realDurationMinutes(w.finished_at ? Math.round((Date.parse(w.finished_at) - Date.parse(w.started_at)) / 60000) : null, w.started_at, w.sets.map((s) => s.completed_at)),
      }))
      const sets = setsOf(h)
      setList([...monthlyBadges(h, home.goal), ...computeAchievements({ sessions, weeklyGoal: home.goal, totalVolumeKg: sets.reduce((a, s) => a + s.reps * s.kg, 0), exercisesWithRecord: recordsOf(sets).length })])
    }).catch((e: Error) => setError(e.message))
  }
  useEffect(load, [])

  if (error) return <><div className="title">Conquistas</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!list) return <><div className="title">Conquistas</div><div className="skel" /><div className="skel" /></>
  const families = [...new Set(list.map((a) => a.family))]
  return (
    <>
      <div className="title">Conquistas</div>
      <div className="big num" style={{ fontSize: 22 }}>{list.filter((a) => a.progress >= a.target).length} de {list.length} desbloqueadas</div>
      {families.map((f) => (
        <div key={f} style={{ display: 'contents' }}>
          <div className="label" style={{ marginTop: 8 }}>{FAMILY_LABEL[f]}</div>
          {list.filter((a) => a.family === f).map((a) => <Badge key={a.id} a={a} />)}
        </div>
      ))}
      <p className="muted" style={{ fontSize: 12 }}>Volume e recordes consideram os últimos 300 treinos.</p>
    </>
  )
}
