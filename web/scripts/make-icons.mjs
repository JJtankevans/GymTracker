// Gera os PNGs do ícone (halter lima sobre grafite) sem dependências: rasteriza retângulos arredondados e grava PNG.
import { deflateSync } from 'node:zlib'
import { writeFileSync } from 'node:fs'

const crcTable = new Uint32Array(256).map((_, n) => { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; return c >>> 0 })
const crc = (buf) => { let c = 0xffffffff; for (const b of buf) c = crcTable[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0 }
const chunk = (type, data) => { const len = Buffer.alloc(4); len.writeUInt32BE(data.length); const td = Buffer.concat([Buffer.from(type), data]); const c = Buffer.alloc(4); c.writeUInt32BE(crc(td)); return Buffer.concat([len, td, c]) }

function png(size, full) {
  const s = size / 512
  const px = Buffer.alloc(size * size * 4)
  const rr = (x, y, rx, ry, w, h, r) => { // dentro de retângulo arredondado (coords em 512)
    const dx = Math.max(rx - x, 0, x - (rx + w)), dy = Math.max(ry - y, 0, y - (ry + h))
    if (dx === 0 && dy === 0) { const cx = Math.min(Math.max(x, rx + r), rx + w - r), cy = Math.min(Math.max(y, ry + r), ry + h - r); return Math.hypot(x - cx, y - cy) <= r }
    return false
  }
  const bars = [[96, 196, 40, 120, 12], [144, 164, 40, 184, 12], [176, 240, 160, 32, 8], [328, 164, 40, 184, 12], [376, 196, 40, 120, 12]]
  for (let j = 0; j < size; j++) for (let i = 0; i < size; i++) {
    const x = (i + 0.5) / s, y = (j + 0.5) / s
    const o = (j * size + i) * 4
    const inBg = full ? true : rr(x, y, 0, 0, 512, 512, 112)
    if (!inBg) continue
    let c = [12, 13, 15]
    for (const [bx, by, bw, bh, br] of bars) if (rr(x, y, bx, by, bw, bh, br)) c = [198, 242, 78]
    px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255
  }
  const raw = Buffer.alloc((size * 4 + 1) * size)
  for (let j = 0; j < size; j++) { raw[j * (size * 4 + 1)] = 0; px.copy(raw, j * (size * 4 + 1) + 1, j * size * 4, (j + 1) * size * 4) }
  const ihdr = Buffer.alloc(13); ihdr.writeUInt32BE(size, 0); ihdr.writeUInt32BE(size, 4); ihdr[8] = 8; ihdr[9] = 6
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr), chunk('IDAT', deflateSync(raw)), chunk('IEND', Buffer.alloc(0))])
}

// apple-touch-icon e maskable ocupam o quadrado todo (o sistema arredonda); o ícone comum já vem arredondado.
writeFileSync('public/apple-touch-icon.png', png(180, true))
writeFileSync('public/icon-192.png', png(192, false))
writeFileSync('public/icon-512.png', png(512, true))
console.log('ícones gerados')
