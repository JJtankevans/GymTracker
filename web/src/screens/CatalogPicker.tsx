import { useMemo, useState } from 'react'
import { addExerciseToGroup, createGroup, deleteGroup } from '../data'
import { CATALOG, TEMPLATES, type CatalogTemplate } from '../catalog'
import { MUSCLES, searchCatalog } from '../catalogSearch'

/** Catálogo de 70 exercícios (o mesmo do app Android) com busca e filtro por músculo. */
export function CatalogPicker({ groupId, existing, onChanged, onClose }: { groupId: string; existing: string[]; onChanged: () => void; onClose: () => void }) {
  const [q, setQ] = useState('')
  const [muscle, setMuscle] = useState<string | null>(null)
  const [added, setAdded] = useState<string[]>([])
  const [busy, setBusy] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)
  const results = useMemo(() => searchCatalog(q, muscle), [q, muscle])
  const have = new Set([...existing, ...added].map((n) => n.toLowerCase()))

  async function add(name: string, sets: number, reps: number) {
    setBusy(name); setErr(null)
    try { await addExerciseToGroup(groupId, name, sets, reps); setAdded((p) => [...p, name]); onChanged() } catch (e) { setErr((e as Error).message) } finally { setBusy(null) }
  }

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }} role="dialog" aria-label="Catálogo de exercícios">
      <div className="row"><b className="grow">Catálogo ({CATALOG.length} exercícios)</b><button className="btn" onClick={onClose}>Fechar</button></div>
      <label className="field"><span className="label">Buscar</span><input type="search" value={q} onChange={(e) => setQ(e.target.value)} placeholder="supino, polia, tríceps…" /></label>
      <div className="chips" aria-label="Músculo">
        <button className={'chip' + (muscle === null ? ' on' : '')} onClick={() => setMuscle(null)}>Todos</button>
        {MUSCLES.map((m) => <button key={m} className={'chip' + (muscle === m ? ' on' : '')} onClick={() => setMuscle(muscle === m ? null : m)}>{m}</button>)}
      </div>
      {err && <div className="error">{err}</div>}
      {results.length === 0 && <div className="muted">Nada encontrado. Você ainda pode criar um exercício pelo nome.</div>}
      {results.map((e) => {
        const has = have.has(e.name.toLowerCase())
        return (
          <div key={e.name} className="row">
            <span className="grow" style={{ minWidth: 0 }}><b style={{ display: 'block' }}>{e.name}</b><span className="muted">{e.muscle} · {e.equipment} · {e.sets}×{e.reps}</span></span>
            <button className={'btn' + (has ? '' : ' primary')} style={{ minWidth: 48, padding: '0 14px' }} disabled={has || busy === e.name} aria-label={has ? `${e.name} já está no grupo` : `Adicionar ${e.name}`} onClick={() => add(e.name, e.sets, e.reps)}>{has ? '✓' : busy === e.name ? '…' : '+'}</button>
          </div>
        )
      })}
    </div>
  )
}

/** Treinos prontos: criam um grupo já com os exercícios. Se algo falha, o grupo é desfeito (como no app). */
export function TemplatePicker({ onCreated, onClose }: { onCreated: () => void; onClose: () => void }) {
  const [busy, setBusy] = useState<string | null>(null)
  const [err, setErr] = useState<string | null>(null)

  async function make(t: CatalogTemplate) {
    setBusy(t.name); setErr(null)
    let groupId: string | null = null
    try {
      groupId = (await createGroup(t.name, t.color)).id
      for (const n of t.exercises) {
        const ex = CATALOG.find((c) => c.name === n)
        if (ex) await addExerciseToGroup(groupId, ex.name, ex.sets, ex.reps)
      }
      onCreated()
    } catch (e) {
      const msg = (e as Error).message
      if (groupId) await deleteGroup(groupId).catch(() => {})
      setErr(msg.includes('duplicate') || msg.includes('unique') ? `Você já tem um grupo chamado “${t.name}”.` : msg)
    } finally { setBusy(null) }
  }

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }} role="dialog" aria-label="Treinos prontos">
      <div className="row"><b className="grow">Treinos prontos</b><button className="btn" onClick={onClose}>Fechar</button></div>
      {err && <div className="error">{err}</div>}
      {TEMPLATES.map((t) => (
        <button key={t.name} className="card row" style={{ textAlign: 'left', width: '100%' }} disabled={busy !== null} onClick={() => make(t)}>
          <span style={{ width: 14, height: 14, borderRadius: 7, background: t.color, flex: 'none' }} />
          <span className="grow"><b style={{ display: 'block' }}>{t.name}</b><span className="muted">{t.description} · {t.exercises.length} exercícios</span></span>
          <span aria-hidden="true">{busy === t.name ? '…' : '+'}</span>
        </button>
      ))}
    </div>
  )
}
