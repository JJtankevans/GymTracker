// Emblema do elo (SVG): escudo na cor do elo, gema no centro, marcas de divisão, asas e, na Lenda, coroa e raios.
import { TIERS } from "./rankedRules.js";

function shade(hex, f) {
  const n = parseInt(hex.slice(1), 16);
  const c = [(n >> 16) & 255, (n >> 8) & 255, n & 255].map((v) => Math.max(0, Math.min(255, Math.round(v * f))));
  return `rgb(${c[0]},${c[1]},${c[2]})`;
}

let seq = 0;

/** Texto SVG pronto (só números e cores nossas, sem texto do usuário). */
export function emblemSvg(tier, division) {
  const t = TIERS[tier] || TIERS[0];
  const id = `em${++seq}`;
  const c = t.color;
  const d = shade(c, 0.4);
  const wings = Math.max(0, Math.min(4, tier - 1));
  let wingPaths = "";
  for (const side of [-1, 1]) {
    for (let k = 0; k < wings; k++) {
      const y = 34 + 11 * k;
      const reach = 22 - 2 * k;
      const x0 = 56 + side * 40;
      wingPaths += `<path d="M${x0} ${y} L${x0 + side * reach} ${y - 7} L${x0 + side * reach * 0.7} ${y + 3} L${x0} ${y + 6} Z" fill="${c}" opacity=".75"/>`;
    }
  }
  const rays = tier === 6
    ? `<g opacity=".6" stroke="${c}" stroke-width="3" stroke-linecap="round"><animateTransform attributeName="transform" type="rotate" from="0 56 56" to="360 56 56" dur="16s" repeatCount="indefinite"/>${Array.from({ length: 12 }, (_, i) => {
        const a = (i * 30 * Math.PI) / 180;
        return `<line x1="${56 + Math.cos(a) * 50}" y1="${56 + Math.sin(a) * 50}" x2="${56 + Math.cos(a) * 58}" y2="${56 + Math.sin(a) * 58}"/>`;
      }).join("")}</g>`
    : "";
  const marks = tier === 6
    ? `<path d="M38 33 L44 20 L56 30 L68 20 L74 33 Z" fill="#fff" opacity=".95"/>`
    : (() => {
        const n = 4 - Math.max(1, Math.min(3, division));
        return Array.from({ length: n }, (_, m) => {
          const x = 56 + (m - (n - 1) / 2) * 10;
          return `<path d="M${x - 4} 33 L${x} 27 L${x + 4} 33" fill="none" stroke="#fff" stroke-width="2.6" stroke-linecap="round" stroke-linejoin="round"/>`;
        }).join("");
      })();
  return `<svg class="emblem" viewBox="0 0 112 112" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="${t.label}">
  <defs>
    <radialGradient id="${id}a"><stop offset="0" stop-color="${c}" stop-opacity=".4"/><stop offset="1" stop-color="${c}" stop-opacity="0"/></radialGradient>
    <linearGradient id="${id}s" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="${c}"/><stop offset="1" stop-color="${d}"/></linearGradient>
    <linearGradient id="${id}g" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fff" stop-opacity=".92"/><stop offset="1" stop-color="${c}"/></linearGradient>
  </defs>
  <circle cx="56" cy="56" r="56" fill="url(#${id}a)"/>
  ${rays}${wingPaths}
  <path d="M56 9 L96 22 L92 65 Q86 92 56 105 Q26 92 20 65 L16 22 Z" fill="url(#${id}s)" stroke="#fff" stroke-opacity=".35" stroke-width="2"/>
  <path d="M56 17 L86 26 L83 63 Q79 84 56 95 Q33 84 29 63 L26 26 Z" fill="${d}" opacity=".88"/>
  <path d="M56 38 L72 58 L56 80 L40 58 Z" fill="url(#${id}g)" stroke="#fff" stroke-opacity=".6" stroke-width="1.5"/>
  ${marks}
</svg>`;
}
