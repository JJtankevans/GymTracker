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

/**
 * Foto do treino com os números por cima, no estilo dos apps de corrida (mesmo desenho do Android):
 * a foto ocupa o cartão 1080×1350 (corte central), degradê escuro embaixo e leve no topo, texto sempre branco.
 */
export async function photoCardImage(file: Blob, spec: CardSpec): Promise<Blob> {
  const W = 1080, H = 1350
  const bmp = await createImageBitmap(file, { imageOrientation: 'from-image' })
  const accent = getComputedStyle(document.documentElement).getPropertyValue('--accent').trim() || '#c6f24e'
  const onAccent = getComputedStyle(document.documentElement).getPropertyValue('--on-accent').trim() || '#10130a'
  const c = document.createElement('canvas'); c.width = W; c.height = H
  const g = c.getContext('2d')!
  const s = Math.max(W / bmp.width, H / bmp.height)
  g.drawImage(bmp, (W - bmp.width * s) / 2, (H - bmp.height * s) / 2, bmp.width * s, bmp.height * s)
  bmp.close?.()
  const top = g.createLinearGradient(0, 0, 0, 360); top.addColorStop(0, 'rgba(0,0,0,0.67)'); top.addColorStop(1, 'rgba(0,0,0,0)')
  g.fillStyle = top; g.fillRect(0, 0, W, 360)
  const bottom = g.createLinearGradient(0, 560, 0, H); bottom.addColorStop(0, 'rgba(0,0,0,0)'); bottom.addColorStop(1, 'rgba(0,0,0,0.93)')
  g.fillStyle = bottom; g.fillRect(0, 560, W, H - 560)
  const pad = 72
  g.shadowColor = 'rgba(0,0,0,0.6)'; g.shadowBlur = 8; g.shadowOffsetY = 3
  g.fillStyle = accent; g.fillRect(pad, 76, 64, 8)
  g.fillStyle = '#fff'; g.font = '700 34px system-ui, sans-serif'; g.fillText('GYMTRACKER', pad, 132)
  if (spec.subtitle) { g.fillStyle = 'rgba(255,255,255,0.8)'; g.font = '500 34px system-ui, sans-serif'; g.textAlign = 'right'; g.fillText(spec.subtitle, W - pad, 132); g.textAlign = 'left' }
  g.fillStyle = '#fff'; g.font = '800 104px system-ui, sans-serif'
  let title = spec.title
  while (title.length > 1 && g.measureText(title).width > W - 2 * pad) title = title.slice(0, -1)
  if (title !== spec.title) title = title.trimEnd() + '…'
  g.fillText(title, pad, 1010)
  const stats = spec.stats.slice(0, 3)
  const colW = (W - 2 * pad) / Math.max(stats.length, 1)
  stats.forEach(([val, lab], i) => {
    const x = pad + i * colW
    let size = 96
    g.font = `800 ${size}px system-ui, sans-serif`
    while (g.measureText(val).width > colW - 44 && size > 40) { size -= 6; g.font = `800 ${size}px system-ui, sans-serif` }
    g.fillStyle = '#fff'; g.fillText(val, x, 1130)
    g.fillStyle = 'rgba(255,255,255,0.8)'; g.font = '600 28px system-ui, sans-serif'; g.fillText(lab.toUpperCase(), x, 1180)
  })
  if (spec.footer) {
    g.shadowColor = 'transparent'
    const txt = `${spec.footerLabel ? spec.footerLabel + ': ' : ''}${spec.footer}`
    g.font = '700 36px system-ui, sans-serif'
    const tw = Math.min(g.measureText(txt).width, W - 2 * pad - 64)
    g.fillStyle = accent; g.beginPath(); g.roundRect(pad, 1214, tw + 64, 86, 43); g.fill()
    g.fillStyle = onAccent; g.fillText(txt, pad + 32, 1271, W - 2 * pad - 64)
  }
  return new Promise((res, rej) => c.toBlob((b) => (b ? res(b) : rej(new Error('Não foi possível gerar a imagem.'))), 'image/png'))
}

/** Compartilha um PNG pronto (folha do sistema ou download). */
export async function shareBlob(blob: Blob, fileName: string, title: string): Promise<void> {
  const file = new File([blob], fileName, { type: 'image/png' })
  if (navigator.canShare?.({ files: [file] })) { await navigator.share({ files: [file], title }); return }
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a'); a.href = url; a.download = fileName; document.body.appendChild(a); a.click(); a.remove()
  setTimeout(() => URL.revokeObjectURL(url), 10_000)
}
