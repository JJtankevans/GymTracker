import { MOCK, supabase } from './supabase'

export interface Weight { id: string; measured_on: string; weight_kg: number }

let mockWeights: Weight[] = [
  ['2026-10-27', 130.2], ['2026-10-28', 129.8], ['2026-10-29', 129.5], ['2026-10-30', 129.3], ['2026-11-03', 129.3], ['2026-11-04', 128.9], ['2026-11-05', 128.1], ['2026-11-07', 128.1], ['2026-11-21', 128.1],
].map(([d, kg], i) => ({ id: `bw${i}`, measured_on: d as string, weight_kg: kg as number }))

export async function loadWeights(): Promise<Weight[]> {
  if (MOCK) return [...mockWeights]
  const { data, error } = await supabase.from('body_weights').select('id,measured_on,weight_kg').order('measured_on', { ascending: true }).limit(1000)
  if (error) throw new Error(error.message)
  return (data ?? []).map((w) => ({ ...w, weight_kg: Number(w.weight_kg) }))
}

/** Um registro por dia: gravar de novo no mesmo dia substitui. */
export async function logWeight(day: string, kg: number): Promise<void> {
  if (MOCK) {
    mockWeights = [...mockWeights.filter((w) => w.measured_on !== day), { id: `bw${Date.now()}`, measured_on: day, weight_kg: kg }].sort((a, b) => a.measured_on.localeCompare(b.measured_on))
    return
  }
  const { data: s } = await supabase.auth.getSession()
  const profile_id = s.session?.user.id
  if (!profile_id) throw new Error('Sessão expirada. Entre de novo.')
  const { error } = await supabase.from('body_weights').upsert({ profile_id, measured_on: day, weight_kg: kg }, { onConflict: 'profile_id,measured_on' })
  if (error) throw new Error(error.message)
}

export async function deleteWeight(id: string): Promise<void> {
  if (MOCK) { mockWeights = mockWeights.filter((w) => w.id !== id); return }
  const { error } = await supabase.from('body_weights').delete().eq('id', id)
  if (error) throw new Error(error.message)
}

/** Sexo informado no perfil (usado só nas referências de força). */
export async function loadSex(): Promise<'MALE' | 'FEMALE' | null> {
  if (MOCK) return 'MALE'
  const { data } = await supabase.from('profiles').select('sex').maybeSingle()
  const s = (data as { sex?: string } | null)?.sex
  return s === 'MALE' || s === 'FEMALE' ? s : null
}
