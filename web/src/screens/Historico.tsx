import { useEffect, useMemo, useRef, useState } from 'react'
import { deleteWorkout, loadHistory, loadWorkoutNote, NOTES_MAX, saveWorkoutNote, type HistItem } from '../data'
import { dayKey, formatKg, prettyGroupName, realDurationMinutes, weekStart } from '../rules'

const fmtDate = (iso: string) => new Date(iso).toLocaleDateString('pt-BR')
const fmtVol = (kg: number) => (kg >= 1000 ? `${formatKg(kg / 1000)} t` : `${formatKg(kg)} kg`)

function durationOf(w: HistItem): number | null {
  const recorded = w.finished_at ? Math.round((Date.parse(w.finished_at) - Date.parse(w.started_at)) / 60000) : null
  return realDurationMinutes(recorded, w.started_at, w.sets.map((s) => s.completed_at))
}
const volumeOf = (w: HistItem) => w.sets.reduce((a, s) => a + s.reps * Number(s.weight_kg), 0)
const nameOf = (w: HistItem) => prettyGroupName(w.muscle_group_name ?? 'Treino livre')

function Heatmap({ items }: { items: HistItem[] }) {
  const weeks = 18
  const counts = new Map<string, number>()
  for (const w of items) counts.set(dayKey(new Date(w.started_at)), (counts.get(dayKey(new Date(w.started_at))) ?? 0) + 1)
  const start = weekStart(new Date())
  start.setDate(start.getDate() - 7 * (weeks - 1))
  const days = new Set(counts.keys()).size
  const cols = Array.from({ length: weeks }, (_, c) => Array.from({ length: 7 }, (_, r) => { const d = new Date(start); d.setDate(d.getDate() + c * 7 + r); return { d, n: counts.get(dayKey(d)) ?? 0, future: d > new Date() } }))
  return (
    <div className="card">
      <div className="label">Últimas {weeks} semanas</div>
      <div className="muted num" style={{ margin: '2px 0 10px' }}>{days} dias treinados · {items.length} treinos</div>
      <div className="heat" role="img" aria-label="Mapa de calor dos treinos">
        {cols.map((col, c) => <div key={c}>{col.map(({ d, n, future }) => <i key={d.getTime()} className={n >= 2 ? 'h2' : n === 1 ? 'h1' : ''} style={future ? { opacity: 0.35 } : undefined} />)}</div>)}
      </div>
    </div>
  )
}

/** Anotação do treino: carrega à parte e salva ao sair do campo. */
function NoteCard({ id }: { id: string }) {
  const [text, setText] = useState<string | null>(null)
  const [saved, setSaved] = useState('')
  const [msg, setMsg] = useState('')
  useEffect(() => { loadWorkoutNote(id).then((n) => { setText(n); setSaved(n) }).catch(() => { setText(''); setSaved('') }) }, [id])
  if (text === null) return null
  const save = async () => {
    if (text.trim() === saved.trim()) return
    try { await saveWorkoutNote(id, text); setSaved(text.trim()); setMsg('Anotação salva.') } catch (e) { setMsg('Não foi possível salvar: ' + (e as Error).message) }
  }
  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
      <label className="label" htmlFor="nota">Anotação do treino</label>
      <textarea id="nota" rows={3} maxLength={NOTES_MAX} placeholder="Como foi o treino? Dores, sensações, ajustes…" value={text} onChange={(e) => { setText(e.target.value); setMsg('') }} onBlur={save}
        style={{ fontSize: 16, padding: 10, borderRadius: 'var(--radius)', border: '1px solid var(--line)', background: 'var(--surface-2)', color: 'var(--text)', resize: 'vertical', fontFamily: 'inherit' }} />
      {msg && <small className="muted">{msg}</small>}
    </div>
  )
}

export function Historico() {
  const [items, setItems] = useState<HistItem[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [picked, setPicked] = useState<string[]>([])
  const toggle = (g: string) => setPicked((p) => (p.includes(g) ? p.filter((x) => x !== g) : [...p, g]))
  const [open, setOpen] = useState<HistItem | null>(null)
  const [undo, setUndo] = useState<{ item: HistItem } | null>(null)
  const timer = useRef<number | undefined>(undefined)
  const pendingDelete = useRef<HistItem | null>(null)

  const load = () => { setError(null); loadHistory().then(setItems).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])
  // Ao sair da tela com um apagar pendente, conclui o apagar.
  useEffect(() => () => { window.clearTimeout(timer.current); if (pendingDelete.current) void deleteWorkout(pendingDelete.current.id) }, [])

  const groups = useMemo(() => {
    const m = new Map<string, number>()
    for (const w of items ?? []) m.set(nameOf(w), (m.get(nameOf(w)) ?? 0) + 1)
    return [...m.entries()]
  }, [items])

  function remove(w: HistItem) {
    // Apaga de verdade depois de 6 s, a menos que a pessoa toque em Desfazer.
    if (pendingDelete.current) void deleteWorkout(pendingDelete.current.id)
    window.clearTimeout(timer.current)
    pendingDelete.current = w
    setItems((p) => (p ?? []).filter((x) => x.id !== w.id))
    setOpen(null)
    setUndo({ item: w })
    timer.current = window.setTimeout(() => { void deleteWorkout(w.id); pendingDelete.current = null; setUndo(null) }, 6000)
  }
  function undoDelete() {
    window.clearTimeout(timer.current)
    const w = undo?.item
    pendingDelete.current = null
    setUndo(null)
    if (w) setItems((p) => [...(p ?? []), w].sort((a, b) => b.started_at.localeCompare(a.started_at)))
  }

  if (error) return <><div className="title">Histórico</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!items) return <><div className="title">Histórico</div><div className="skel" style={{ height: 180 }} /><div className="skel" /></>

  if (open) {
    const byEx = new Map<string, HistItem['sets']>()
    for (const s of [...open.sets].sort((a, b) => a.completed_at.localeCompare(b.completed_at))) {
      const k = s.exercise?.name ?? 'Exercício'
      byEx.set(k, [...(byEx.get(k) ?? []), s])
    }
    return (
      <>
        <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={() => setOpen(null)}>‹ Histórico</button>
        <div className="title">{nameOf(open)}</div>
        <div className="muted num">{fmtDate(open.started_at)} · {durationOf(open) ?? '—'} min · {open.sets.length} séries · {fmtVol(volumeOf(open))}</div>
        {[...byEx.entries()].map(([nm, ss]) => (
          <div key={nm} className="card">
            <b>{nm}</b>
            {ss.map((s, i) => <div key={s.id} className="row" style={{ padding: '4px 0' }}><span className="muted grow">Série {i + 1}</span><span className="num">{s.reps} reps × {formatKg(Number(s.weight_kg))} kg</span></div>)}
          </div>
        ))}
        {byEx.size === 0 && <div className="card muted">Este treino não tem séries.</div>}
        <NoteCard id={open.id} />
        <button className="btn block danger" onClick={() => remove(open)}>Apagar treino</button>
      </>
    )
  }

  const shown = picked.length === 0 ? items : items.filter((w) => picked.includes(nameOf(w)))
  return (
    <>
      <div className="title">Histórico</div>
      <Heatmap items={items} />
      <div className="chips" aria-label="Filtrar por grupo (pode escolher vários)">
        <button className={'chip' + (picked.length === 0 ? ' on' : '')} aria-pressed={picked.length === 0} onClick={() => setPicked([])}>Todos ({items.length})</button>
        {groups.map(([g, n]) => <button key={g} className={'chip' + (picked.includes(g) ? ' on' : '')} aria-pressed={picked.includes(g)} onClick={() => toggle(g)}>{g} ({n})</button>)}
      </div>
      {shown.length === 0 && <div className="card muted">Nenhum treino por aqui ainda.</div>}
      {shown.map((w) => (
        <button key={w.id} className="card row" style={{ textAlign: 'left', width: '100%' }} onClick={() => setOpen(w)}>
          <span className="grow" style={{ minWidth: 0 }}>
            <b style={{ display: 'block' }}>{nameOf(w)}</b>
            <span className="muted num">{fmtDate(w.started_at)} · {w.sets.length} séries · {fmtVol(volumeOf(w))}</span>
          </span>
          <b className="num">{durationOf(w) ? `${durationOf(w)}min` : '—'}</b>
        </button>
      ))}
      {undo && (
        <div className="toast" role="status">
          <span className="grow">Treino apagado: {nameOf(undo.item)}</span>
          <button className="chip on" onClick={undoDelete}>Desfazer</button>
        </div>
      )}
    </>
  )
}
