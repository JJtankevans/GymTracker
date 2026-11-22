import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  activeWorkout, addSet, deleteSet, deleteWorkout, finishWorkout, groupExercises, lastPerformance, loadHome, startWorkout, workoutSets,
  type Exercise, type Group, type LastPerf, type SetRow, type Workout,
} from '../data'
import { formatKg, prettyGroupName, staleEndsAt, suggestNextGroup } from '../rules'

const REST_OPTIONS = [30, 45, 60, 90, 120]
const restLabel = (s: number) => (s < 60 ? `${s}s` : s % 60 === 0 ? `${s / 60}min` : `${Math.floor(s / 60)}min${s % 60}`)
const clock = (ms: number) => { const t = Math.max(0, Math.floor(ms / 1000)); return `${String(Math.floor(t / 60)).padStart(2, '0')}:${String(t % 60).padStart(2, '0')}` }
const parseNum = (s: string) => { const n = parseFloat(s.replace(',', '.')); return Number.isFinite(n) ? n : NaN }

// RPE por série fica só neste aparelho (a coluna `rpe` ainda não existe no banco), como no app Android.
const rpeGet = (id: string): number => { try { return Number(JSON.parse(localStorage.getItem('gt-rpe') ?? '{}')[id] ?? 0) } catch { return 0 } }
const rpePut = (id: string, v: number) => { try { const m = JSON.parse(localStorage.getItem('gt-rpe') ?? '{}'); m[id] = v; localStorage.setItem('gt-rpe', JSON.stringify(m)) } catch { /* sem armazenamento */ } }
const RPE_TEXT: Record<number, string> = { 6: 'leve', 7: 'moderada', 8: 'forte', 9: 'muito forte', 10: 'no limite' }

function useNow(ms = 1000) {
  const [now, setNow] = useState(Date.now())
  useEffect(() => { const t = setInterval(() => setNow(Date.now()), ms); return () => clearInterval(t) }, [ms])
  return now
}

interface Summary { minutes: number; exercises: number; sets: number; reps: number; volume: number }

export function Treino({ pending, clearPending, onDone }: { pending: string | null; clearPending: () => void; onDone: () => void }) {
  const [state, setState] = useState<'loading' | 'pick' | 'active' | 'summary'>('loading')
  const [workout, setWorkout] = useState<Workout | null>(null)
  const [groups, setGroups] = useState<Group[]>([])
  const [nextId, setNextId] = useState<string | null>(null)
  const [exercises, setExercises] = useState<Exercise[]>([])
  const [last, setLast] = useState<Record<string, LastPerf>>({})
  const [sets, setSets] = useState<SetRow[]>([])
  const [summary, setSummary] = useState<Summary | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [note, setNote] = useState<string | null>(null)
  const [restEnd, setRestEnd] = useState<number>(() => Number(localStorage.getItem('gt-rest-end') ?? 0))
  const [restSec, setRestSec] = useState<number>(() => Number(localStorage.getItem('gt-rest') ?? 90))
  const [confirmEnd, setConfirmEnd] = useState(false)
  const now = useNow()

  const open = useCallback(async (w: Workout) => {
    setWorkout(w)
    const [ex, s] = await Promise.all([w.muscle_group_id ? groupExercises(w.muscle_group_id) : Promise.resolve([]), workoutSets(w.id)])
    setExercises(ex)
    setSets(s)
    setLast(await lastPerformance(ex.map((e) => e.id)))
    setState('active')
  }, [])

  const boot = useCallback(async () => {
    setError(null)
    try {
      const w = await activeWorkout()
      if (w) {
        // Treino esquecido (> 1 h sem série): encerra na última série (ou apaga se não houve série).
        const s = await workoutSets(w.id)
        const ends = staleEndsAt(w.started_at, s.map((x) => x.completed_at))
        if (ends) {
          if (s.length === 0) { await deleteWorkout(w.id); setNote('Um treino esquecido, sem séries, foi descartado.') }
          else { await finishWorkout(w.id, ends); setNote('Um treino esquecido foi encerrado no horário da última série.') }
        } else { await open(w); return }
      }
      const home = await loadHome()
      setGroups(home.groups)
      setNextId(suggestNextGroup(home.groups, home.workouts))
      if (pending) { const g = pending; clearPending(); await start(g); return }
      setState('pick')
    } catch (e) { setError((e as Error).message) }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  useEffect(() => { void boot() }, [boot])

  async function start(groupId: string | null) {
    setError(null)
    try { await open(await startWorkout(groupId)) } catch (e) { setError((e as Error).message) }
  }

  async function register(ex: Exercise, reps: number, kg: number): Promise<SetRow> {
    const n = sets.filter((s) => s.exercise_id === ex.id).length + 1
    const row = await addSet(workout!.id, ex.id, n, reps, kg)
    setSets((p) => [...p, row])
    const end = Date.now() + restSec * 1000
    setRestEnd(end)
    localStorage.setItem('gt-rest-end', String(end))
    return row
  }

  async function removeSet(id: string) { await deleteSet(id); setSets((p) => p.filter((s) => s.id !== id)) }

  async function finish() {
    if (!workout) return
    try {
      if (sets.length === 0) { await deleteWorkout(workout.id); setConfirmEnd(false); setWorkout(null); onDone(); return }
      await finishWorkout(workout.id)
      setSummary({
        minutes: Math.max(1, Math.round((Date.now() - Date.parse(workout.started_at)) / 60000)),
        exercises: new Set(sets.map((s) => s.exercise_id)).size,
        sets: sets.length, reps: sets.reduce((a, s) => a + s.reps, 0), volume: sets.reduce((a, s) => a + s.reps * Number(s.weight_kg), 0),
      })
      setConfirmEnd(false)
      setState('summary')
    } catch (e) { setError((e as Error).message) }
  }

  const resting = restEnd > now
  const restLeft = restEnd - now

  if (state === 'loading') return <><div className="title">Treino</div>{error ? <div className="card"><p className="error">{error}</p><button className="btn" onClick={boot}>Tentar de novo</button></div> : <div className="skel" />}</>

  if (state === 'pick') return (
    <>
      <div className="title">Treino</div>
      {note && <div className="card muted">{note}</div>}
      {error && <div className="card error">{error}</div>}
      <div className="label">Escolha o grupo</div>
      {groups.length === 0 && <div className="card muted">Crie grupos no app para começar.</div>}
      {[...groups].sort((a, b) => (a.id === nextId ? -1 : b.id === nextId ? 1 : 0)).map((g) => (
        <button key={g.id} className={'card row' + (g.id === nextId ? ' hero' : '')} style={{ textAlign: 'left', width: '100%' }} onClick={() => start(g.id)}>
          <span style={{ width: 14, height: 14, borderRadius: 7, background: g.color_hex, flex: 'none' }} />
          <span className="grow"><b>{prettyGroupName(g.name)}</b>{g.id === nextId && <small style={{ display: 'block' }}>Sugerido pela rotação</small>}</span>
          <span aria-hidden="true">›</span>
        </button>
      ))}
      <button className="btn block" onClick={() => start(null)}>Treino livre (sem grupo)</button>
    </>
  )

  if (state === 'summary' && summary) return (
    <>
      <div className="title">Treino concluído!</div>
      <div className="card">
        {([['Duração', `${summary.minutes} min`], ['Exercícios', summary.exercises], ['Séries', summary.sets], ['Repetições', summary.reps], ['Volume total', `${formatKg(summary.volume)} kg`]] as const).map(([k, v]) => (
          <div key={k} className="row" style={{ padding: '6px 0' }}><span className="grow muted">{k}</span><b className="num">{v}</b></div>
        ))}
      </div>
      <button className="btn primary block" onClick={() => { setWorkout(null); onDone() }}>Concluir</button>
    </>
  )

  return (
    <>
      <div className="row">
        <div className="grow">
          <div className="title" style={{ margin: 0 }}>{prettyGroupName(workout?.muscle_group_name ?? 'Treino livre')}</div>
          <div className="muted num">{clock(now - Date.parse(workout!.started_at))}</div>
        </div>
        <button className="btn primary" onClick={() => setConfirmEnd(true)}>Finalizar</button>
      </div>
      {note && <div className="card muted">{note}</div>}
      {error && <div className="card error">{error}</div>}
      {exercises.length === 0 && <div className="card muted">Este treino não tem exercícios. Adicione exercícios ao grupo no app.</div>}
      {exercises.map((ex) => (
        <ExerciseCard key={ex.id} ex={ex} last={last[ex.id]} sets={sets.filter((s) => s.exercise_id === ex.id)} onRegister={register} onRemove={removeSet} />
      ))}

      {confirmEnd && (
        <div className="card" role="dialog" aria-label="Finalizar treino">
          <b>{sets.length === 0 ? 'Você ainda não registrou nenhuma série.' : 'Finalizar o treino?'}</b>
          <p className="muted" style={{ margin: '6px 0 12px' }}>{sets.length === 0 ? 'Ao finalizar agora o treino vazio será descartado.' : `${sets.length} ${sets.length === 1 ? 'série registrada' : 'séries registradas'}.`}</p>
          <div className="row"><button className="btn grow" onClick={() => setConfirmEnd(false)}>Continuar treinando</button><button className="btn primary grow" onClick={finish}>{sets.length === 0 ? 'Descartar' : 'Finalizar'}</button></div>
        </div>
      )}

      {restEnd > 0 && now - restEnd < 20000 && (
        <div className="island" role="timer" aria-live="off">
          <div className="top">
            <div className="grow"><div className="label" style={{ color: 'inherit', opacity: 0.7 }}>{resting ? 'Descanso' : 'Descanso acabou'}</div><b className="num" style={{ fontSize: 22 }}>{resting ? clock(restLeft) : 'Bora!'}</b></div>
            <button className="chip" onClick={() => setRestEnd(0)} aria-label="Fechar descanso">×</button>
          </div>
          <div className="opts">
            {REST_OPTIONS.map((s) => (
              <button key={s} className={'chip' + (s === restSec ? ' on' : '')} onClick={() => { setRestSec(s); localStorage.setItem('gt-rest', String(s)); const end = Date.now() + s * 1000; setRestEnd(end); localStorage.setItem('gt-rest-end', String(end)) }}>{restLabel(s)}</button>
            ))}
          </div>
        </div>
      )}
    </>
  )
}

function ExerciseCard({ ex, last, sets, onRegister, onRemove }: {
  ex: Exercise; last?: LastPerf; sets: SetRow[]
  onRegister: (ex: Exercise, reps: number, kg: number) => Promise<SetRow>; onRemove: (id: string) => Promise<void>
}) {
  const [reps, setReps] = useState(String(last?.reps ?? ex.default_reps))
  const [kg, setKg] = useState(last ? formatKg(last.weight_kg) : '')
  const [busy, setBusy] = useState(false)
  const [err, setErr] = useState<string | null>(null)
  const [, bump] = useState(0)
  const lastSet = sets[sets.length - 1]

  const step = (field: 'reps' | 'kg', d: number) => {
    if (field === 'reps') setReps(String(Math.min(999, Math.max(0, (Number.isFinite(parseNum(reps)) ? parseNum(reps) : 0) + d))))
    else setKg(formatKg(Math.max(0, (Number.isFinite(parseNum(kg)) ? parseNum(kg) : 0) + d)))
  }
  const repsN = parseNum(reps), kgN = parseNum(kg)
  const valid = Number.isFinite(repsN) && repsN >= 1 && Number.isFinite(kgN) && kgN >= 0

  async function go() {
    if (!valid || busy) return
    setBusy(true); setErr(null)
    try { await onRegister(ex, Math.round(repsN), kgN) } catch (e) { setErr((e as Error).message) } finally { setBusy(false) }
  }
  const sugg = useMemo(() => (last ? `Última vez: ${formatKg(last.weight_kg)} kg × ${last.reps}` : null), [last])

  return (
    <div className="card" style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <div><b style={{ fontSize: 18 }}>{ex.name}</b><div className="muted">{ex.default_sets} séries × {ex.default_reps} reps{sugg ? ` · ${sugg}` : ''}</div></div>
      {sets.map((s) => (
        <div key={s.id} className="row">
          <span className="muted grow">Série {s.set_number}</span>
          <span className="num">{s.reps} reps × {formatKg(Number(s.weight_kg))} kg{rpeGet(s.id) ? ` · RPE ${rpeGet(s.id)}` : ''}</span>
          <button className="icon" aria-label={`Apagar série ${s.set_number}`} onClick={() => onRemove(s.id)}>×</button>
        </div>
      ))}
      <div className="grid2">
        <label className="field"><span className="label">Reps</span><input inputMode="numeric" value={reps} onChange={(e) => setReps(e.target.value.replace(/\D/g, '').slice(0, 3))} /></label>
        <label className="field"><span className="label">Peso (kg)</span><input inputMode="decimal" value={kg} onChange={(e) => setKg(e.target.value.replace(/[^0-9,.]/g, '').slice(0, 6))} /></label>
      </div>
      <div className="steps">
        <button className="btn" onClick={() => step('reps', -1)}>−1</button>
        <button className="btn" onClick={() => step('reps', 1)}>+1</button>
        <button className="btn" onClick={() => step('kg', -2.5)}>−2,5</button>
        <button className="btn" onClick={() => step('kg', 2.5)}>+2,5</button>
      </div>
      <button className="btn primary block" disabled={!valid || busy} onClick={go}>{busy ? 'Registrando…' : `+ Registrar série ${sets.length + 1}`}</button>
      {err && <div className="error">{err}</div>}
      {lastSet && (
        <div>
          <div className="label" style={{ marginBottom: 6 }}>{rpeGet(lastSet.id) ? `Esforço da série ${lastSet.set_number}: ${RPE_TEXT[rpeGet(lastSet.id)]}` : `Quanto esforço foi a série ${lastSet.set_number}? (opcional)`}</div>
          <div className="steps five">{[6, 7, 8, 9, 10].map((v) => <button key={v} className={'chip' + (rpeGet(lastSet.id) === v ? ' on' : '')} onClick={() => { rpePut(lastSet.id, v); bump((n) => n + 1) }}>{v}</button>)}</div>
        </div>
      )}
    </div>
  )
}
