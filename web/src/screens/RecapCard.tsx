import { useMemo, useState } from 'react'
import type { HistItem } from '../data'
import { buildRecap, buildYearReview, formatVolume, MONTHS, recapTitle, shareRecap, WEEKDAYS } from '../recap'

/** Retrospectiva do mês ou do ano (#354 na web), com botão para compartilhar como imagem. */
export function RecapCard({ items }: { items: HistItem[] }) {
  const now = new Date()
  const [scope, setScope] = useState<'month' | 'year'>('month')
  const [msg, setMsg] = useState('')
  const r = useMemo(() => buildRecap(items, now.getFullYear(), scope === 'month' ? now.getMonth() + 1 : 0), [items, scope])
  const review = useMemo(() => buildYearReview(items, now.getFullYear()), [items])
  const maxM = Math.max(1, ...review.perMonth)
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
          {scope === 'year' && (
            <>
              <div className="label">Treinos por mês</div>
              <div role="img" aria-label="Treinos por mês" style={{ display: 'flex', gap: 4, alignItems: 'flex-end', height: 110 }}>
                {review.perMonth.map((n, i) => (
                  <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', height: '100%', gap: 2 }}>
                    {n > 0 && <small className="muted num">{n}</small>}
                    <div style={{ width: '100%', height: n === 0 ? 3 : 6 + (80 * n) / maxM, borderRadius: 4, background: n === 0 ? 'color-mix(in srgb, var(--text) 10%, transparent)' : review.bestMonth === i + 1 ? 'var(--accent)' : 'color-mix(in srgb, var(--accent) 40%, transparent)' }} />
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', gap: 4 }}>{'JFMAMJJASOND'.split('').map((c, i) => <small key={i} className="muted" style={{ flex: 1, textAlign: 'center' }}>{c}</small>)}</div>
              {review.bestMonth && <div className="muted">Melhor mês: <b style={{ color: 'var(--text)' }}>{MONTHS[review.bestMonth - 1]}</b> ({review.perMonth[review.bestMonth - 1]} treinos)</div>}
              {review.favoriteWeekday !== null && <div className="muted">Dia favorito: <b style={{ color: 'var(--text)' }}>{WEEKDAYS[review.favoriteWeekday]}</b></div>}
              {review.longestStreakDays > 1 && <div className="muted">Maior sequência: <b style={{ color: 'var(--text)' }}>{review.longestStreakDays} dias seguidos</b></div>}
            </>
          )}
          <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={share}>Compartilhar imagem</button>
          {msg && <p className="error">{msg}</p>}
        </>
      )}
    </div>
  )
}
