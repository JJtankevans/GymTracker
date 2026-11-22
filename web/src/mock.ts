// Dados falsos para ver a interface sem login (?mock). Determinísticos, relativos a "hoje".
import type { Group, HomeData, Workout } from './data'

const iso = (daysAgo: number, hour = 18) => {
  const d = new Date()
  d.setDate(d.getDate() - daysAgo)
  d.setHours(hour, 0, 0, 0)
  return d.toISOString()
}

const groups: Group[] = [
  { id: 'g1', name: 'PEITO E TRICEPS', color_hex: '#4CAF50' },
  { id: 'g2', name: 'Biceps Costa', color_hex: '#2196F3' },
  { id: 'g3', name: 'Quadriceps e Panturrilha', color_hex: '#3F51B5' },
  { id: 'g4', name: 'Ombro', color_hex: '#E91E63' },
  { id: 'g5', name: 'Glúteos e Posterior', color_hex: '#4CAF50' },
]

const workouts: Workout[] = [1, 2, 4, 6, 9, 13, 16, 18, 21, 25, 30, 33].map((ago, i) => {
  const g = groups[i % 4]
  const s = iso(ago)
  return { id: `w${i}`, muscle_group_id: g.id, muscle_group_name: g.name, started_at: s, finished_at: new Date(Date.parse(s) + 55 * 60000).toISOString() }
})

export const mockStore = {
  async home(): Promise<HomeData> {
    return { groups, workouts, goal: 5 }
  },
}
