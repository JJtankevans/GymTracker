import { describe, expect, it } from 'vitest'
import { toggleFav } from './favs'

describe('favoritos', () => {
  it('liga e desliga', () => {
    expect(toggleFav([], 'a')).toEqual(['a'])
    expect(toggleFav(['a', 'b'], 'a')).toEqual(['b'])
  })
})
