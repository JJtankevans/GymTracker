import { useEffect, useRef, useState } from 'react'
import { MOCK, supabase } from '../supabase'
import { applyTheme, savedAppearance, THEMES, type Mode } from '../themes'
import { Evolucao } from './Evolucao'
import { Recordes } from './Recordes'
import { Conquistas } from './Conquistas'
import { clampGoal, loadHome, setWeeklyGoal } from '../data'
import { setSoundEnabled, soundEnabled } from '../alerts'
import { applyImport, downloadBackup, parseBackup } from '../backup'

type Sub = null | 'evolucao' | 'recordes' | 'conquistas'

export function More() {
  const [sub, setSub] = useState<Sub>(null)
  const [look, setLook] = useState(savedAppearance)
  const [bk, setBk] = useState<{ busy: boolean; msg: string }>({ busy: false, msg: '' })
  const doBackup = async () => {
    setBk({ busy: true, msg: '' })
    try { const r = await downloadBackup(); setBk({ busy: false, msg: `Backup salvo: ${r.workouts} treinos e ${r.sets} séries.` }) }
    catch (e) { setBk({ busy: false, msg: 'Não foi possível: ' + (e as Error).message }) }
  }

  const [goal, setGoal] = useState<number | null>(null)
  const [goalErr, setGoalErr] = useState('')
  useEffect(() => { loadHome().then((h) => setGoal(h.goal)).catch(() => {}) }, [])
  const changeGoal = async (delta: number) => {
    if (goal === null) return
    const next = clampGoal(goal + delta)
    if (next === goal) return
    setGoal(next); setGoalErr('')
    try { setGoal(await setWeeklyGoal(next)) } catch (e) { setGoal(goal); setGoalErr('Não foi possível salvar: ' + (e as Error).message) }
  }
  const [sound, setSound] = useState(soundEnabled)
  const fileRef = useRef<HTMLInputElement>(null)
  const doImport = async (f: File | undefined) => {
    if (!f) return
    setBk({ busy: true, msg: '' })
    try {
      const p = await applyImport(parseBackup(await f.text()))
      setBk({ busy: false, msg: `Importado: ${p.newGroups.length} grupos novos e ${p.links.length} exercícios ligados. O histórico (${p.workoutsInFile} treinos) não é importado pela web.` })
    } catch (e) { setBk({ busy: false, msg: 'Não foi possível: ' + (e as Error).message }) }
    if (fileRef.current) fileRef.current.value = ''
  }

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

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
        <div className="row">
          <div className="grow"><b>Meta semanal</b><div className="muted">Treinos por semana (1 a 7)</div></div>
          <button className="btn" aria-label="Diminuir meta" disabled={goal === null || goal <= 1} onClick={() => changeGoal(-1)}>−</button>
          <b className="num" style={{ minWidth: 24, textAlign: 'center', fontSize: 22 }}>{goal ?? '–'}</b>
          <button className="btn" aria-label="Aumentar meta" disabled={goal === null || goal >= 7} onClick={() => changeGoal(1)}>+</button>
        </div>
        {goalErr && <p className="error">{goalErr}</p>}
      </div>

      <div className="card row">
        <div className="grow"><b>Aviso do descanso</b><div className="muted">Bip (e vibração, onde houver) quando o descanso acaba</div></div>
        <button className={'chip' + (sound ? ' on' : '')} aria-pressed={sound} onClick={() => { setSound(!sound); setSoundEnabled(!sound) }}>{sound ? 'Ligado' : 'Desligado'}</button>
      </div>

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

      <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        <div className="grow"><b>Backup</b><div className="muted">{bk.msg || 'Baixa grupos, exercícios, treinos e séries em um arquivo JSON; importar acrescenta o que falta'}</div></div>
        <div className="row" style={{ gap: 8 }}>
        <button className="btn" disabled={bk.busy} onClick={doBackup}>{bk.busy ? 'Aguarde…' : 'Baixar'}</button>
        <button className="btn" disabled={bk.busy} onClick={() => fileRef.current?.click()}>Importar</button>
        </div>
        <input ref={fileRef} type="file" accept="application/json,.json" hidden onChange={(e) => doImport(e.target.files?.[0])} />
      </div>

      <div className="card row">
        <div className="grow"><b>Conta</b><div className="muted">{MOCK ? 'Modo demonstração (dados falsos)' : 'Conectado ao Supabase'}</div></div>
        {!MOCK && <button className="btn" onClick={() => supabase.auth.signOut()}>Sair</button>}
      </div>
    </>
  )
}
