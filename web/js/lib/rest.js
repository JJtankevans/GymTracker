// Descanso entre séries: contagem por relógio (não por tique), bipe pelo WebAudio e tela acesa.
// O iPhone não tem navigator.vibrate no Safari, então o aviso é o bipe + a barra; a tela é mantida acesa durante o treino.

let ctx = null;
let wake = null;

function audio() {
  try { ctx = ctx || new (window.AudioContext || window.webkitAudioContext)(); if (ctx.state === "suspended") ctx.resume(); } catch { ctx = null; }
  return ctx;
}
/** Chamar dentro de um toque do usuário para o iOS liberar o som. */
export function unlockAudio() { audio(); }

export function beep(times = 3) {
  const c = audio();
  if (!c) return;
  for (let i = 0; i < times; i++) {
    const t = c.currentTime + i * 0.22;
    const o = c.createOscillator(); const g = c.createGain();
    o.type = "sine"; o.frequency.value = i === times - 1 ? 1175 : 880;
    g.gain.setValueAtTime(0.0001, t); g.gain.exponentialRampToValueAtTime(0.35, t + 0.02); g.gain.exponentialRampToValueAtTime(0.0001, t + 0.18);
    o.connect(g).connect(c.destination); o.start(t); o.stop(t + 0.2);
  }
  try { navigator.vibrate?.([200, 100, 200]); } catch { /* sem vibração */ }
}

export async function keepAwake(on) {
  try {
    if (on && "wakeLock" in navigator && !wake) {
      wake = await navigator.wakeLock.request("screen");
      wake.addEventListener("release", () => { wake = null; });
    } else if (!on && wake) { await wake.release(); wake = null; }
  } catch { /* sem suporte ou negado */ }
}
document.addEventListener("visibilitychange", () => { if (document.visibilityState === "visible" && window.__gtWantsAwake) keepAwake(true); });

/** Cronômetro de descanso: guarda o instante final; quem desenha só pergunta "quanto falta". */
export class RestTimer {
  constructor(onTick, onDone) { this.onTick = onTick; this.onDone = onDone; this.endAt = 0; this.total = 0; this.iv = null; }
  start(seconds) { this.total = seconds; this.endAt = Date.now() + seconds * 1000; this.#run(); }
  add(seconds) { if (!this.active) return; this.endAt += seconds * 1000; this.total += seconds; this.#tick(); }
  stop() { clearInterval(this.iv); this.iv = null; this.endAt = 0; }
  get active() { return this.endAt > 0; }
  get left() { return Math.max(0, Math.ceil((this.endAt - Date.now()) / 1000)); }
  #run() { clearInterval(this.iv); this.iv = setInterval(() => this.#tick(), 250); this.#tick(); document.addEventListener("visibilitychange", this.#vis); }
  #vis = () => { if (document.visibilityState === "visible" && this.active) this.#tick(); };
  #tick() {
    const left = this.left;
    this.onTick(left, this.total);
    if (left <= 0) { const cb = this.onDone; this.stop(); document.removeEventListener("visibilitychange", this.#vis); cb?.(); }
  }
}
