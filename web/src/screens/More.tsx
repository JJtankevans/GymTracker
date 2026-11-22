import { useState } from 'react'
import { MOCK, supabase } from '../supabase'
import { applyTheme, savedAppearance, THEMES, type Mode } from '../themes'
import { Evolucao } from './Evolucao'
import { Recordes } from './Recordes'
import { Conquistas } from './Conquistas'

type Sub = null | 'evolucao' | 'recordes' | 'conquistas'

export function More() {
  const [sub, setSub] = useState<Sub>(null)
  const [look, setLook] = useState(savedAppearance)

  const choose = (theme: string, mode: Mode) => { setLook({ theme, mode }); applyTheme(theme, mode) }

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

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="label">Aparência</div>
        <div className="chips" aria-label="Modo">
          {([['auto', 'Automático'], ['dark', 'Escuro'], ['light', 'Claro']] as const).map(([m, t]) => (
            <button key={m} className={'chip' + (look.mode === m ? ' on' : '')} aria-pressed={look.mode === m} onClick={() => choose(look.theme, m)}>{t}</button>
          ))}
        </div>
        <div className="themes">
          {THEMES.map((t) => {
            const preview = t[look.mode === 'light' ? 'light' : 'dark']
            return (
              <button key={t.id} className={'theme' + (look.theme === t.id ? ' on' : '')} aria-pressed={look.theme === t.id} onClick={() => choose(t.id, look.mode)}>
                <span className="swatchbox" style={{ background: preview.bg, borderColor: preview.line }}>
                  <i style={{ background: preview.surface }} /><i style={{ background: preview.accent }} />
                </span>
                <b>{t.name}</b>
                <small className="muted">{t.tagline}</small>
              </button>
            )
          })}
        </div>
      </div>

      <div className="card row">
        <div className="grow"><b>Conta</b><div className="muted">{MOCK ? 'Modo demonstração (dados falsos)' : 'Conectado ao Supabase'}</div></div>
        {!MOCK && <button className="btn" onClick={() => supabase.auth.signOut()}>Sair</button>}
      </div>
    </>
  )
}
