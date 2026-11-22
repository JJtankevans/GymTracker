import { useEffect, useState } from 'react'
import { loadFeed, reactionSummary, REACTIONS, setCaption, setReaction, statsLine, type FeedData } from '../social'
import { prettyGroupName } from '../rules'

export function Feed() {
  const [data, setData] = useState<FeedData | null>(null)
  const [error, setError] = useState<string | null>(null)
  const load = () => { setError(null); loadFeed().then(setData).catch((e: Error) => setError(e.message)) }
  useEffect(load, [])

  if (!data) {
    if (error) return <><div className="title">Feed</div><div className="card"><p className="error">{error}</p><button className="btn" onClick={load}>Tentar de novo</button></div></>
    return <><div className="title">Feed</div><div className="skel" /><div className="skel" /></>
  }

  const toggle = async (postId: string, kind: string, on: boolean) => {
    const prev = data
    setError(null)
    setData({ ...data, reactions: on ? [...data.reactions, { post_id: postId, profile_id: data.me, kind }] : data.reactions.filter((r) => !(r.post_id === postId && r.profile_id === data.me && r.kind === kind)) })
    try { await setReaction(postId, data.me, kind, on) } catch (e) { setData(prev); setError((e as Error).message) }
  }
  const saveCaption = async (postId: string, text: string) => {
    const caption = text.trim().slice(0, 280) || null
    try { await setCaption(postId, caption); setData((d) => d && { ...d, posts: d.posts.map((p) => (p.id === postId ? { ...p, caption } : p)) }) } catch (e) { setError((e as Error).message) }
  }

  return (
    <>
      <div className="title">Feed</div>
      {error && <p className="error">{error}</p>}
      {data.posts.length === 0 && <div className="card muted">Nada por aqui ainda. Treinos compartilhados por você e por quem você segue aparecem neste feed.</div>}
      {data.posts.map((p) => {
        const own = p.profile_id === data.me
        const { counts, mine } = reactionSummary(data.reactions, p.id, data.me)
        return (
          <div key={p.id} className="card" style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            <div className="row"><b className="grow">{data.names[p.profile_id] ?? 'Alguém'}</b><span className="muted num">{new Date(p.created_at).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit' })}</span></div>
            <div><b>{p.stats.group ? prettyGroupName(p.stats.group) : 'Treino livre'}</b><div className="muted num">{statsLine(p.stats)}</div></div>
            {own
              ? <input aria-label="Legenda" maxLength={280} placeholder="Adicionar legenda" defaultValue={p.caption ?? ''} onBlur={(e) => e.target.value.trim() !== (p.caption ?? '') && saveCaption(p.id, e.target.value)} />
              : p.caption && <div>{p.caption}</div>}
            <div className="chips" role="group" aria-label="Reações">
              {REACTIONS.map(([k, label]) => (
                <button key={k} className={'chip' + (mine.has(k) ? ' on' : '')} aria-pressed={mine.has(k)} onClick={() => toggle(p.id, k, !mine.has(k))}>{label}{counts[k] ? ` ${counts[k]}` : ''}</button>
              ))}
            </div>
          </div>
        )
      })}
    </>
  )
}
