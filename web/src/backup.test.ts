import { describe, expect, it } from 'vitest'
import { backupFileName, buildBackup } from './backup'

describe('backup', () => {
  const e = (id: string, name: string) => ({ id, name, default_sets: 3, default_reps: 10 })
  it('junta exercícios repetidos e guarda os grupos de cada um', () => {
    const b = buildBackup('2026-11-22T10:00:00Z', [{ id: 'g1', name: 'A', color_hex: '#fff' }, { id: 'g2', name: 'B', color_hex: '#000' }],
      { g1: [e('x', 'Supino'), e('y', 'Agachamento')], g2: [e('x', 'Supino')] }, [], [])
    expect(b.exercises.map((x) => x.name)).toEqual(['Agachamento', 'Supino'])
    expect(b.exerciseGroups.x).toEqual(['g1', 'g2'])
    expect(b.exerciseGroups.y).toEqual(['g1'])
    expect(b.version).toBe(1)
  })
  it('nome do arquivo leva a data', () => {
    expect(backupFileName('2026-11-22T10:00:00Z')).toBe('gymtracker-backup-2026-11-22.json')
  })
})

import { parseBackup, planImport } from './backup'

describe('importar backup', () => {
  const file = JSON.stringify({
    app: 'GymTracker', version: 1, createdAt: 'x',
    groups: [{ id: 'g1', name: 'Peito', color_hex: '#111' }, { id: 'g2', name: 'Costas' }],
    exercises: [{ id: 'e1', name: 'Supino', default_sets: 4, default_reps: 8 }, { id: 'e2', name: 'Remada' }],
    exerciseGroups: { e1: ['g1'], e2: ['g2'] }, workouts: [{}], sets: [],
  })
  it('recusa arquivos que não são backup', () => {
    expect(() => parseBackup('nao e json')).toThrow(/JSON/)
    expect(() => parseBackup('{"app":"Outro"}')).toThrow(/não é um backup/)
    expect(() => parseBackup('{"app":"GymTracker","version":9,"groups":[],"exercises":[]}')).toThrow(/Versão/)
  })
  it('lê e normaliza valores faltantes', () => {
    const b = parseBackup(file)
    expect(b.groups[1].color_hex).toBe('#4CAF50')
    expect(b.exercises[1].default_reps).toBe(10)
  })
  it('só acrescenta o que falta, comparando sem acento nem caixa', () => {
    const p = planImport(parseBackup(file), { PEITO: ['supino'] })
    expect(p.newGroups.map((g) => g.name)).toEqual(['Costas'])
    expect(p.links.map((l) => l.exercise.name)).toEqual(['Remada'])
    expect(p.skippedGroups).toBe(1)
    expect(p.skippedLinks).toBe(1)
    expect(p.workoutsInFile).toBe(1)
  })
})
