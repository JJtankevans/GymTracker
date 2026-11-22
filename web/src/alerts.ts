// Aviso de fim do descanso: bip curto (WebAudio) e vibração onde o aparelho tiver (o iPhone não vibra pelo navegador).
// O som só toca se o áudio foi "destravado" por um toque do usuário (regra dos navegadores): chamamos unlockAudio() ao registrar uma série.

const KEY = 'gt-sound'
let ctx: AudioContext | null = null

export function soundEnabled(): boolean {
  try { return localStorage.getItem(KEY) !== '0' } catch { return true }
}
export function setSoundEnabled(on: boolean): void {
  try { localStorage.setItem(KEY, on ? '1' : '0') } catch { /* sem armazenamento */ }
}

/** Cria/retoma o contexto de áudio dentro de um gesto do usuário. */
export function unlockAudio(): void {
  try {
    const AC = window.AudioContext ?? (window as unknown as { webkitAudioContext?: typeof AudioContext }).webkitAudioContext
    if (!AC) return
    ctx ??= new AC()
    if (ctx.state === 'suspended') void ctx.resume()
  } catch { /* sem áudio */ }
}

/** Dois bips curtos (880 Hz e 1175 Hz), volume baixo. Silencioso se o som estiver desligado ou o áudio não destravado. */
export function restOverAlert(): void {
  if (!soundEnabled()) return
  try { navigator.vibrate?.([120, 80, 120]) } catch { /* sem vibração */ }
  if (!ctx || ctx.state !== 'running') return
  const t0 = ctx.currentTime
  ;[[880, 0], [1175, 0.16]].forEach(([f, dt]) => {
    const o = ctx!.createOscillator(); const g = ctx!.createGain()
    o.type = 'sine'; o.frequency.value = f
    g.gain.setValueAtTime(0.0001, t0 + dt)
    g.gain.exponentialRampToValueAtTime(0.18, t0 + dt + 0.02)
    g.gain.exponentialRampToValueAtTime(0.0001, t0 + dt + 0.14)
    o.connect(g).connect(ctx!.destination)
    o.start(t0 + dt); o.stop(t0 + dt + 0.16)
  })
}
