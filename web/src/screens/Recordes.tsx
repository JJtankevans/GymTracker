import { useEffect, useState } from 'react'
import { loadHistory } from '../data'
import { recordsOf, type RecordRow } from '../achievements'
import { formatKg } from '../rules'
import { setsOf } from './Evolucao'

export function Recordes() {
  const [rows, setRows] = useState<RecordRow[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const load = () => { setError(null); loadHistory().then((h) => setRows(recordsOf(setsOf(h)))).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  if (error) return <><div className="title">Recordes</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
  if (!rows) return <><div className="title">Recordes</div><div className="skel" /><div className="skel" /></>
  const recent = rows.filter((r) => Date.now() - Date.parse(r.at) < 30 * 86400000).length
  return (
    <>
      <div className="title">Recordes</div>
      <div className="card row"><div className="big num">{rows.length}</div><div className="grow"><b>recordes</b><div className="muted">{recent} batidos nos últimos 30 dias</div></div></div>
      {rows.length === 0 && <div className="card muted">Registre séries com carga para ver seus recordes.</div>}
      {rows.map((r) => (
        <div key={r.exercise} className="card row">
          <div className="grow" style={{ minWidth: 0 }}>
            <b>{r.exercise}</b>
            <div className="muted num">1RM estimado {formatKg(r.bestOneRm)} kg · {new Date(r.at).toLocaleDateString('pt-BR')}</div>
          </div>
          <b className="num" style={{ fontSize: 22, color: 'var(--accent)' }}>{formatKg(r.bestKg)} kg</b>
        </div>
      ))}
      <p className="muted" style={{ fontSize: 12 }}>Calculado sobre os últimos 300 treinos.</p>
    </>
  )
}
