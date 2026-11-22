import { MOCK, supabase } from './supabase'
import { formatKg } from './rules'

export const REACTIONS = [['flex', 'Força'], ['fire', 'Fogo'], ['clap', 'Palmas'], ['headphones', 'Música']] as const

export interface PostStats { minutes?: number; sets?: number; reps?: number; volume_kg?: number; exercises?: number; group?: string | null }
export interface FeedPost { id: string; profile_id: string; caption: string | null; visibility: string; stats: PostStats; created_at: string }
export interface Reaction { post_id: string; profile_id: string; kind: string }
export interface FeedData { me: string; posts: FeedPost[]; names: Record<string, string>; reactions: Reaction[] }

/** Contagem por tipo e quais tipos o usuário já marcou, para um post. */
export function reactionSummary(rs: Reaction[], postId: string, me: string) {
  const counts: Record<string, number> = {}
  const mine = new Set<string>()
  for (const r of rs) {
    if (r.post_id !== postId) continue
    counts[r.kind] = (counts[r.kind] ?? 0) + 1
    if (r.profile_id === me) mine.add(r.kind)
  }
  return { counts, mine }
}

/** Resumo em uma linha do treino do post. */
export function statsLine(s: PostStats): string {
  const p: string[] = []
  if (s.minutes) p.push(`${s.minutes} min`)
  if (s.sets) p.push(`${s.sets} séries`)
  if (s.volume_kg) p.push(`${formatKg(Math.round(s.volume_kg))} kg`)
  return p.join(' · ')
}

const mockFeed: FeedData = {
  me: 'me',
  posts: [
    { id: 'p1', profile_id: 'u1', caption: 'Dia de pernas pesado!', visibility: 'public', stats: { minutes: 62, sets: 20, volume_kg: 9400, group: 'Quadríceps' }, created_at: new Date(Date.now() - 3600e3).toISOString() },
    { id: 'p2', profile_id: 'me', caption: null, visibility: 'followers', stats: { minutes: 48, sets: 16, volume_kg: 5200, group: 'Peito e Tríceps' }, created_at: new Date(Date.now() - 86400e3).toISOString() },
  ],
  names: { u1: 'Marina', me: 'Você' },
  reactions: [{ post_id: 'p1', profile_id: 'me', kind: 'fire' }, { post_id: 'p1', profile_id: 'u2', kind: 'fire' }, { post_id: 'p2', profile_id: 'u1', kind: 'clap' }],
}

export async function loadFeed(): Promise<FeedData> {
  if (MOCK) return structuredClone(mockFeed)
  const { data: u } = await supabase.auth.getUser()
  const me = u.user?.id ?? ''
  const { data: posts, error } = await supabase.from('feed_posts').select('id,profile_id,caption,visibility,stats,created_at').order('created_at', { ascending: false }).limit(40)
  if (error) throw new Error(error.message)
  const list = (posts ?? []) as FeedPost[]
  const ids = list.map((p) => p.id)
  const [rx, pf] = await Promise.all([
    ids.length ? supabase.from('post_reactions').select('post_id,profile_id,kind').in('post_id', ids) : Promise.resolve({ data: [], error: null }),
    supabase.rpc('public_profiles'),
  ])
  if (rx.error) throw new Error(rx.error.message)
  const names: Record<string, string> = { [me]: 'Você' }
  for (const p of (pf.data ?? []) as { id: string; username: string | null; full_name: string | null }[]) if (p.id !== me) names[p.id] = p.full_name || p.username || 'Alguém'
  return { me, posts: list, names, reactions: (rx.data ?? []) as Reaction[] }
}

export async function setReaction(postId: string, me: string, kind: string, on: boolean) {
  if (MOCK) return
  const q = on
    ? supabase.from('post_reactions').insert({ post_id: postId, profile_id: me, kind })
    : supabase.from('post_reactions').delete().eq('post_id', postId).eq('profile_id', me).eq('kind', kind)
  const { error } = await q
  if (error) throw new Error(error.message)
}

export async function setCaption(postId: string, caption: string | null) {
  if (MOCK) return
  const { error } = await supabase.from('feed_posts').update({ caption }).eq('id', postId)
  if (error) throw new Error(error.message)
}
