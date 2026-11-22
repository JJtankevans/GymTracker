import { useMemo, useState } from 'react'
import type { HistItem } from '../data'
import { buildRecap, formatVolume, recapTitle, shareRecap } from '../recap'

/** Retrospectiva do mês ou do ano (#354 na web), com botão para compartilhar como imagem. */
export function RecapCard({ items }: { items: HistItem[] }) {
  const now = new Date()
  const [scope, setScope] = useState<'month' | 'year'>('month')
  const [msg, setMsg] = useState('')
  const r = useMemo(() => buildRecap(items, now.getFullYear(), scope === 'month' ? now.getMonth() + 1 : 0), [items, scope])
  const stat = (v: string, l: string) => <div key={l}><b className="num" style={{ fontSize: 26 }}>{v}</b><div className="muted">{l}</div></div>
  const share = async () => {
    setMsg('')
    try { await shareRecap(r) } catch (e) { if ((e as Error).name !== 'AbortError') setMsg((e as Error).message) }
  }
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      <div className="row">
        <div className="label grow">{recapTitle(r)}</div>
        <div className="chips" aria-label="Período">
          {([['month', 'Mês'], ['year', 'Ano']] as const).map(([k, t]) => (
            <button key={k} className={'chip' + (scope === k ? ' on' : '')} aria-pressed={scope === k} onClick={() => setScope(k)}>{t}</button>
          ))}
        </div>
      </div>
      {r.workouts === 0 ? <div className="muted">Nenhum treino neste período ainda.</div> : (
        <>
          <div className="stats">
            {stat(String(r.workouts), r.workouts === 1 ? 'treino' : 'treinos')}
            {stat(String(r.daysTrained), r.daysTrained === 1 ? 'dia' : 'dias')}
            {stat(String(r.sets), 'séries')}
            {stat(formatVolume(r.volumeKg), 'volume')}
          </div>
          {r.topGroup && <div className="muted">Mais treinado: <b style={{ color: 'var(--text)' }}>{r.topGroup}</b> ({r.topGroupWorkouts}x)</div>}
          <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={share}>Compartilhar imagem</button>
          {msg && <p className="error">{msg}</p>}
        </>
      )}
    </div>
  )
}
