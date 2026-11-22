import { describe, expect, it } from 'vitest'
import { reactionSummary, statsLine } from './social'

describe('social', () => {
  it('conta reações e marca as do usuário', () => {
    const r = reactionSummary([
      { post_id: 'a', profile_id: 'me', kind: 'fire' }, { post_id: 'a', profile_id: 'x', kind: 'fire' }, { post_id: 'b', profile_id: 'x', kind: 'clap' },
    ], 'a', 'me')
    expect(r.counts).toEqual({ fire: 2 })
    expect([...r.mine]).toEqual(['fire'])
  })
  it('resume o treino', () => {
    expect(statsLine({ minutes: 50, sets: 12, volume_kg: 3000 })).toBe('50 min · 12 séries · 3000 kg')
  })
})
