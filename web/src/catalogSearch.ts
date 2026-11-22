import { CATALOG, type CatalogExercise } from './catalog'

/** Minúsculas e sem acento, para busca tolerante ("triceps" acha "Tríceps"). */
export const fold = (s: string) => s.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().trim()

export const MUSCLES = [...new Set(CATALOG.map((e) => e.muscle))]

/** Busca por nome, apelido, músculo ou equipamento; [muscle] restringe o grupo muscular. */
export function searchCatalog(query: string, muscle: string | null = null): CatalogExercise[] {
  const words = fold(query).split(/\s+/).filter(Boolean)
  return CATALOG.filter((e) => {
    if (muscle && e.muscle !== muscle) return false
    const hay = fold([e.name, e.muscle, e.equipment, ...(e.aliases ?? [])].join(' '))
    return words.every((w) => hay.includes(w))
  })
}
