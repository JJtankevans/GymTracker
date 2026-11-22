import { useEffect, useState, type FormEvent } from 'react'
import { addExerciseToGroup, createGroup, deleteGroup, groupCounts, groupExercises, loadHome, removeFromGroup, type Exercise, type Group, type Workout } from '../data'
import { prettyGroupName } from '../rules'
import { loadFavs, saveFavs, toggleFav } from '../favs'
import { CatalogPicker, TemplatePicker } from './CatalogPicker'

const COLORS = ['#4CAF50', '#2196F3', '#3F51B5', '#E91E63', '#FF9800', '#9C27B0', '#00BCD4', '#F44336']

function lastTrained(groupId: string, workouts: Workout[]): string | null {
  const w = workouts.find((x) => x.muscle_group_id === groupId && x.finished_at)
  return w ? new Date(w.started_at).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' }) : null
}

export function Grupos() {
  const [groups, setGroups] = useState<Group[] | null>(null)
  const [workouts, setWorkouts] = useState<Workout[]>([])
  const [counts, setCounts] = useState<Record<string, number>>({})
  const [error, setError] = useState<string | null>(null)
  const [open, setOpen] = useState<Group | null>(null)
  const [creating, setCreating] = useState(false)
  const [templates, setTemplates] = useState(false)

  const load = () => {
    setError(null)
    Promise.all([loadHome(), groupCounts()]).then(([h, c]) => { setGroups(h.groups); setWorkouts(h.workouts); setCounts(c) }).catch((e: Error) => setError(e.message))
  }
  useEffect(load, [])

  if (error) return <><div className="title">Grupos</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!groups) return <><div className="title">Grupos</div><div className="skel" /><div className="skel" /></>
  if (open) return <GroupDetail group={open} onBack={() => { setOpen(null); load() }} onDeleted={() => { setOpen(null); load() }} />

  return (
    <>
      <div className="row"><div className="title grow">Grupos</div><button className="btn" onClick={() => setTemplates(true)}>Prontos</button><button className="btn primary" onClick={() => setCreating(true)}>+ Novo</button></div>
      {templates && <TemplatePicker onClose={() => setTemplates(false)} onCreated={() => { setTemplates(false); load() }} />}
      {creating && <NewGroup onCancel={() => setCreating(false)} onCreated={() => { setCreating(false); load() }} />}
      {groups.length === 0 && !creating && <div className="card muted">Nenhum grupo ainda. Toque em “+ Novo” para criar o primeiro.</div>}
      {groups.map((g) => {
        const n = counts[g.id] ?? 0
        const last = lastTrained(g.id, workouts)
        return (
          <button key={g.id} className="card row" style={{ textAlign: 'left', width: '100%' }} onClick={() => setOpen(g)}>
            <span style={{ width: 40, height: 40, borderRadius: 20, background: g.color_hex, flex: 'none' }} />
            <span className="grow" style={{ minWidth: 0 }}>
              <b style={{ display: 'block', fontSize: 17 }}>{prettyGroupName(g.name)}</b>
              <span className="muted">{n} {n === 1 ? 'exercício' : 'exercícios'} · {last ? `último treino ${last}` : 'sem treino ainda'}</span>
            </span>
            <span aria-hidden="true" className="muted">›</span>
          </button>
        )
      })}
    </>
  )
}

function NewGroup({ onCancel, onCreated }: { onCancel: () => void; onCreated: () => void }) {
  const [name, setName] = useState('')
  const [color, setColor] = useState(COLORS[0])
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    setBusy(true); setErr(null)
    try { await createGroup(name, color); onCreated() } catch (x) { setErr((x as Error).message.includes('duplicate') ? 'Já existe um grupo com esse nome.' : (x as Error).message) } finally { setBusy(false) }
  }
  return (
    <form className="card" onSubmit={submit} style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <label className="field"><span className="label">Nome do grupo</span><input value={name} onChange={(e) => setName(e.target.value)} maxLength={40} autoFocus placeholder="Ex.: Peito e Tríceps" /></label>
      <div className="chips" aria-label="Cor">{COLORS.map((c) => <button type="button" key={c} aria-label={`Cor ${c}`} aria-pressed={c === color} className="swatch" style={{ background: c, outline: c === color ? '3px solid var(--text)' : 'none' }} onClick={() => setColor(c)} />)}</div>
      {err && <div className="error">{err}</div>}
      <div className="row"><button type="button" className="btn grow" onClick={onCancel}>Cancelar</button><button className="btn primary grow" disabled={busy || !name.trim()}>Criar</button></div>
    </form>
  )
}

function GroupDetail({ group, onBack, onDeleted }: { group: Group; onBack: () => void; onDeleted: () => void }) {
  const [list, setList] = useState<Exercise[] | null>(null)
  const [name, setName] = useState('')
  const [err, setErr] = useState<string | null>(null)
  const [confirm, setConfirm] = useState(false)
  const [catalog, setCatalog] = useState(false)
  const load = () => groupExercises(group.id).then(setList).catch((e: Error) => setErr(e.message))
  useEffect(() => { void load() }, [group.id])

  async function add(e: FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    try { await addExerciseToGroup(group.id, name, 3, 12); setName(''); await load() } catch (x) { setErr((x as Error).message) }
  }
  async function remove(id: string) { await removeFromGroup(group.id, id); await load() }
  const [fav, setFav] = useState(() => loadFavs().includes(group.id))

  return (
    <>
      <button className="btn" style={{ alignSelf: 'flex-start' }} onClick={onBack}>‹ Grupos</button>
      <div className="row"><span style={{ width: 28, height: 28, borderRadius: 14, background: group.color_hex }} /><div className="title grow" style={{ margin: 0 }}>{prettyGroupName(group.name)}</div><button className={'chip' + (fav ? ' on' : '')} aria-pressed={fav} onClick={() => { const n = toggleFav(loadFavs(), group.id); saveFavs(n); setFav(n.includes(group.id)) }}>{fav ? '★ Atalho' : '☆ Atalho'}</button></div>
      {err && <div className="card error">{err}</div>}
      {!list && <div className="skel" />}
      {list?.length === 0 && <div className="card muted">Nenhum exercício neste grupo ainda.</div>}
      {list?.map((ex) => (
        <div key={ex.id} className="card row">
          <span className="grow"><b>{ex.name}</b><span className="muted" style={{ display: 'block' }}>{ex.default_sets} séries × {ex.default_reps} reps</span></span>
          <button className="icon" aria-label={`Tirar ${ex.name} do grupo`} onClick={() => remove(ex.id)}>×</button>
        </div>
      ))}
      {catalog ? <CatalogPicker groupId={group.id} existing={(list ?? []).map((e) => e.name)} onChanged={load} onClose={() => setCatalog(false)} /> : <button className="btn primary block" onClick={() => setCatalog(true)}>+ Adicionar do catálogo</button>}
      <form className="card row" onSubmit={add}>
        <label className="field grow"><span className="label">Ou criar pelo nome</span><input value={name} onChange={(e) => setName(e.target.value)} placeholder="Ex.: Supino Reto" maxLength={60} /></label>
        <button className="btn primary" style={{ alignSelf: 'flex-end' }} disabled={!name.trim()}>Adicionar</button>
      </form>
      {!confirm ? <button className="btn block danger" onClick={() => setConfirm(true)}>Remover grupo</button> : (
        <div className="card">
          <b>Remover “{prettyGroupName(group.name)}”?</b>
          <p className="muted" style={{ margin: '6px 0 12px' }}>Os exercícios e o histórico continuam guardados; só o grupo some.</p>
          <div className="row"><button className="btn grow" onClick={() => setConfirm(false)}>Cancelar</button><button className="btn danger grow" onClick={async () => { await deleteGroup(group.id); onDeleted() }}>Remover</button></div>
        </div>
      )}
    </>
  )
}
