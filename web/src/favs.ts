const KEY = 'gt-fav-groups'

/** Grupos favoritos (atalhos na Home), guardados só neste aparelho. */
export function loadFavs(): string[] {
  try { const v = JSON.parse(localStorage.getItem(KEY) ?? '[]'); return Array.isArray(v) ? v.filter((x): x is string => typeof x === 'string') : [] } catch { return [] }
}

export const toggleFav = (favs: string[], id: string): string[] => (favs.includes(id) ? favs.filter((f) => f !== id) : [...favs, id])

export function saveFavs(favs: string[]) {
  try { localStorage.setItem(KEY, JSON.stringify(favs)) } catch { /* sem armazenamento */ }
}
