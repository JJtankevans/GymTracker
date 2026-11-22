/**
 * Supersérie / bi-set (#575). O banco não tem coluna para isso, então a ligação fica no aparelho, por treino:
 * [links] = ids dos exercícios que estão ligados ao PRÓXIMO da lista. Exercícios consecutivos ligados formam um grupo.
 */
export type Links = Record<string, boolean>

const key = (workoutId: string) => `gt-ss-${workoutId}`

export function loadLinks(workoutId: string): Links {
  try { const v = JSON.parse(localStorage.getItem(key(workoutId)) ?? '{}'); return v && typeof v === 'object' ? v : {} } catch { return {} }
}
export function saveLinks(workoutId: string, links: Links) {
  try { localStorage.setItem(key(workoutId), JSON.stringify(links)) } catch { /* sem armazenamento */ }
}

/** Grupos de exercícios em ordem; os não ligados ficam em grupos de 1. */
export function supersetGroups(ids: string[], links: Links): string[][] {
  const groups: string[][] = []
  ids.forEach((id, i) => {
    if (i > 0 && links[ids[i - 1]]) groups[groups.length - 1].push(id)
    else groups.push([id])
  })
  return groups
}

export const groupOf = (groups: string[][], id: string): string[] => groups.find((g) => g.includes(id)) ?? [id]

/** 2 exercícios = bi-set, 3 = tri-set, 4+ = circuito; 1 = nenhum. */
export function groupLabel(size: number): string | null {
  return size < 2 ? null : size === 2 ? 'Bi-set' : size === 3 ? 'Tri-set' : 'Circuito'
}

/**
 * Depois de registrar uma série de [id], quem vem agora? Em grupo, o próximo exercício da rodada
 * (null se foi o último do grupo → hora de descansar). Fora de grupo, sempre null.
 */
export function nextInRound(groups: string[][], id: string): string | null {
  const g = groupOf(groups, id)
  const i = g.indexOf(id)
  return g.length > 1 && i < g.length - 1 ? g[i + 1] : null
}

/** Liga/desliga o exercício [id] ao próximo. O último da lista não pode ligar a nada. */
export function toggleLink(ids: string[], links: Links, id: string): Links {
  const i = ids.indexOf(id)
  if (i < 0 || i === ids.length - 1) return links
  return { ...links, [id]: !links[id] }
}
