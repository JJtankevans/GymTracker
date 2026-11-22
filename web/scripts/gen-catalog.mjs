// Gera web/src/catalog.ts a partir do ExerciseCatalog.kt e WorkoutTemplates.kt do app (fonte única da verdade).
// Uso: node scripts/gen-catalog.mjs
import { readFileSync, writeFileSync } from 'node:fs'

const base = '../shared/src/commonMain/kotlin/com/vitoroliveira/gymtracker/data/catalog/'
const cat = readFileSync(base + 'ExerciseCatalog.kt', 'utf8')
const tpl = readFileSync(base + 'WorkoutTemplates.kt', 'utf8')

const muscles = {}
const muscleBlock = cat.match(/enum class CatalogMuscle[^{]*\{([\s\S]*?)\n\}/)[1]
for (const m of muscleBlock.matchAll(/([A-Z_]+)\("([^"]+)"\)/g)) muscles[m[1]] = m[2]
const equip = {}
for (const m of cat.matchAll(/(BARRA|HALTERES|MAQUINA|POLIA|PESO_CORPO|SMITH)\("([^"]+)"\)/g)) equip[m[1]] = m[2]

const exercises = []
for (const line of cat.split('\n')) {
  const m = line.match(/^\s*e\("([^"]+)",\s*M\.([A-Z_]+),\s*Q\.([A-Z_]+),\s*(\d+),\s*(\d+)(.*)\),?\s*$/)
  if (!m) continue
  const rest = m[6]
  const hint = rest.match(/hint\s*=\s*"([^"]*)"/)?.[1] ?? rest.match(/^,\s*"([^"]*)"/)?.[1]
  const al = rest.match(/aliases\s*=\s*listOf\(([^)]*)\)/)?.[1]
  const aliases = al ? [...al.matchAll(/"([^"]+)"/g)].map((x) => x[1]) : []
  exercises.push({ name: m[1], muscle: muscles[m[2]], equipment: equip[m[3]], sets: +m[4], reps: +m[5], ...(hint ? { hint } : {}), ...(aliases.length ? { aliases } : {}) })
}

const templates = []
for (const m of tpl.matchAll(/WorkoutTemplate\(\s*"([^"]+)",\s*"([^"]+)",\s*"(#[0-9A-Fa-f]{6})",\s*listOf\(([\s\S]*?)\)\s*\)/g)) {
  templates.push({ name: m[1], description: m[2], color: m[3], exercises: [...m[4].matchAll(/"([^"]+)"/g)].map((x) => x[1]) })
}

if (exercises.length < 50 || templates.length < 5) throw new Error(`Leitura suspeita: ${exercises.length} exercícios, ${templates.length} treinos`)
const out = `// GERADO por scripts/gen-catalog.mjs a partir do app Android. Não edite à mão: rode \`node scripts/gen-catalog.mjs\`.
export interface CatalogExercise { name: string; muscle: string; equipment: string; sets: number; reps: number; hint?: string; aliases?: string[] }
export interface CatalogTemplate { name: string; description: string; color: string; exercises: string[] }

export const CATALOG: CatalogExercise[] = ${JSON.stringify(exercises, null, 1)}

export const TEMPLATES: CatalogTemplate[] = ${JSON.stringify(templates, null, 1)}
`
writeFileSync('src/catalog.ts', out)
console.log(`catalog.ts: ${exercises.length} exercícios, ${templates.length} treinos prontos`)
