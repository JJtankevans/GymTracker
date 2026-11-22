import { useState } from 'react'
import { MOCK, supabase } from '../supabase'
import { Evolucao } from './Evolucao'
import { Recordes } from './Recordes'
import { Conquistas } from './Conquistas'

type Sub = null | 'evolucao' | 'recordes' | 'conquistas'
type Mode = 'auto' | 'dark' | 'light'

function applyMode(m: Mode) {
  const root = document.documentElement
  if (m === 'auto') root.removeAttribute('data-theme'); else root.setAttribute('data-theme', m)
  try { localStorage.setItem('gt-mode', m) } catch { /* sem armazenamento */ }
  document.querySelector('meta[name="theme-color"]')?.setAttribute('content', m === 'light' ? '#f5f6f3' : '#0c0d0f')
}
export function restoreMode() {
  try { const m = localStorage.getItem('gt-mode') as Mode | null; if (m) applyMode(m) } catch { /* sem armazenamento */ }
}

export function More() {
  const [sub, setSub] = useState<Sub>(null)
  const [mode, setMode] = useState<Mode>(() => (localStorage.getItem('gt-mode') as Mode | null) ?? 'auto')

  if (sub) {
    return (
      <>
        <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={() => setSub(null)}>‹ Mais</button>
        {sub === 'evolucao' && <Evolucao />}
        {sub === 'recordes' && <Recordes />}
        {sub === 'conquistas' && <Conquistas />}
      </>
    )
  }
  const item = (label: string, hint: string, to: Exclude<Sub, null>) => (
    <button className="card row" style={{ textAlign: 'left', width: '100%' }} onClick={() => setSub(to)}>
      <span className="grow"><b style={{ fontSize: 17 }}>{label}</b><span className="muted" style={{ display: 'block' }}>{hint}</span></span><span aria-hidden="true" className="muted">›</span>
    </button>
  )
  return (
    <>
      <div className="title">Mais</div>
      {item('Evolução', 'Peso corporal e cargas por exercício', 'evolucao')}
      {item('Recordes', 'Sua melhor carga em cada exercício', 'recordes')}
      {item('Conquistas', 'Selos por treinos, sequência e volume', 'conquistas')}
      <div className="card">
        <div className="label" style={{ marginBottom: 8 }}>Aparência</div>
        <div className="chips">
          {([['auto', 'Automático'], ['dark', 'Escuro'], ['light', 'Claro']] as const).map(([m, t]) => (
            <button key={m} className={'chip' + (mode === m ? ' on' : '')} onClick={() => { setMode(m); applyMode(m) }}>{t}</button>
          ))}
        </div>
      </div>
      <div className="card row">
        <div className="grow"><b>Conta</b><div className="muted">{MOCK ? 'Modo demonstração (dados falsos)' : 'Conectado ao Supabase'}</div></div>
        {!MOCK && <button className="btn" onClick={() => supabase.auth.signOut()}>Sair</button>}
      </div>
    </>
  )
}
