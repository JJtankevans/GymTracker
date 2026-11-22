/** Cartão para compartilhar (1080×1350) desenhado em canvas com as cores do tema atual. Usado na retrospectiva e no resumo do treino. */
export interface CardSpec {
  title: string
  subtitle?: string
  stats: [string, string][] // [valor, rótulo], até 4 (grade 2×2)
  footerLabel?: string
  footer?: string
}

export function cardImage(spec: CardSpec): Promise<Blob> {
  const css = getComputedStyle(document.documentElement)
  const v = (n: string, fb: string) => css.getPropertyValue(n).trim() || fb
  const bg = v('--bg', '#0c0d0f'), fg = v('--text', '#f4f5f2'), muted = v('--muted', '#8e948f'), accent = v('--accent', '#c6f24e'), surface = v('--surface', '#16181b')
  const c = document.createElement('canvas')
  c.width = 1080; c.height = 1350
  const g = c.getContext('2d')!
  g.fillStyle = bg; g.fillRect(0, 0, 1080, 1350)
  g.fillStyle = accent; g.fillRect(80, 110, 120, 12)
  g.fillStyle = muted; g.font = '700 34px system-ui, sans-serif'; g.fillText('GYMTRACKER', 80, 190)
  g.fillStyle = fg; g.font = '800 92px system-ui, sans-serif'
  // quebra por palavras (nunca no meio de uma palavra)
  const lines: string[] = []
  for (const word of spec.title.split(' ')) {
    const cur = lines[lines.length - 1]
    if (cur !== undefined && g.measureText(cur + ' ' + word).width <= 920) lines[lines.length - 1] = cur + ' ' + word
    else lines.push(word)
  }
  lines.slice(0, 2).forEach((l, i) => g.fillText(l, 80, 320 + i * 100))
  if (spec.subtitle) { g.fillStyle = muted; g.font = '600 40px system-ui, sans-serif'; g.fillText(spec.subtitle, 80, 320 + Math.min(lines.length, 2) * 100 - 10) }
  spec.stats.slice(0, 4).forEach(([val, lab], i) => {
    const x = 80 + (i % 2) * 480, y = 560 + Math.floor(i / 2) * 270
    g.fillStyle = surface; g.beginPath(); g.roundRect(x, y, 440, 230, 36); g.fill()
    g.fillStyle = accent; g.font = '800 96px system-ui, sans-serif'; g.fillText(val, x + 36, y + 120)
    g.fillStyle = muted; g.font = '600 38px system-ui, sans-serif'; g.fillText(lab, x + 36, y + 180)
  })
  if (spec.footer) {
    g.fillStyle = muted; g.font = '600 38px system-ui, sans-serif'; g.fillText(spec.footerLabel ?? '', 80, 1180)
    g.fillStyle = fg; g.font = '800 60px system-ui, sans-serif'; g.fillText(spec.footer, 80, 1250)
  }
  return new Promise((res, rej) => c.toBlob((b) => (b ? res(b) : rej(new Error('Não foi possível gerar a imagem.'))), 'image/png'))
}

/** Compartilha (iPhone: folha de compartilhamento) ou, sem suporte, baixa o PNG. */
export async function shareCard(spec: CardSpec, fileName: string): Promise<void> {
  const blob = await cardImage(spec)
  const file = new File([blob], fileName, { type: 'image/png' })
  if (navigator.canShare?.({ files: [file] })) { await navigator.share({ files: [file], title: spec.title }); return }
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = fileName; document.body.appendChild(a); a.click(); a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}
