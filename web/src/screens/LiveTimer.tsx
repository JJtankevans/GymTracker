import { useEffect, useRef, useState } from 'react'
import { liveSchedule, phaseAt, scheduleSeconds } from '../training'
import type { Tempo } from '../tempo'
import { phaseBeep, restOverAlert, unlockAudio } from '../alerts'

const LEAD_MS = 3000

/** Série cronometrada (#599): conta as fases da cadência, com bip a cada mudança, e avisa quando a série acaba. */
export function LiveTimer({ tempo, reps, onFinish }: { tempo: Tempo; reps: number; onFinish: () => void }) {
  const [startAt, setStartAt] = useState<number | null>(null)
  const [now, setNow] = useState(Date.now())
  const lastIdx = useRef(-2)
  const finished = useRef(false)
  const schedule = liveSchedule(tempo, reps)
  const total = scheduleSeconds(schedule)

  useEffect(() => {
    if (startAt === null) return
    const id = setInterval(() => setNow(Date.now()), 100)
    return () => clearInterval(id)
  }, [startAt])

  const elapsed = startAt === null ? 0 : (now - startAt) / 1000
  const cur = startAt !== null && elapsed >= 0 ? phaseAt(schedule, elapsed) : null
  const done = startAt !== null && elapsed >= total

  useEffect(() => {
    if (!cur || cur.index === lastIdx.current) return
    lastIdx.current = cur.index
    phaseBeep()
  }, [cur])
  useEffect(() => {
    if (done && !finished.current) { finished.current = true; restOverAlert(); onFinish() }
  }, [done, onFinish])

  if (schedule.length === 0) return null
  if (startAt === null || done) {
    return (
      <button className="btn" onClick={() => { unlockAudio(); lastIdx.current = -2; finished.current = false; setStartAt(Date.now() + LEAD_MS); setNow(Date.now()) }}>
        {done ? 'Série completa! Cronometrar de novo' : `Série cronometrada (${reps} reps · ${total}s sob tensão)`}
      </button>
    )
  }
  return (
    <div className="card hero" role="timer" aria-live="polite" style={{ cursor: 'default', textAlign: 'center' }}>
      {elapsed < 0
        ? <><div className="label" style={{ color: 'inherit' }}>Prepare-se</div><b className="num" style={{ fontSize: 40 }}>{Math.ceil(-elapsed)}</b></>
        : cur && <><div className="label" style={{ color: 'inherit' }}>Repetição {cur.phase.rep} de {reps}</div><b style={{ fontSize: 26 }}>{cur.phase.label}</b><div className="num" style={{ fontSize: 40 }}>{cur.left}</div></>}
      <button className="chip" onClick={() => setStartAt(null)}>Parar</button>
    </div>
  )
}
